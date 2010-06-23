/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.web.controller.genome.gene;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;

/**
 * For 'live searches' from the web interface.
 * 
 * @author luke
 * @version $Id$
 */
@Controller
public class GenePickerController {

    private static Log log = LogFactory.getLog( GenePickerController.class );

    @Autowired
    private GeneService geneService = null;

    @Autowired
    private TaxonService taxonService = null;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private SearchService searchService = null;

    @Autowired
    private ArrayDesignService arrayDesignService;

    private static final int MAX_GENES_PER_QUERY = 1000;

    private static Comparator<Taxon> TAXON_COMPARATOR = new Comparator<Taxon>() {
        public int compare( Taxon o1, Taxon o2 ) {
            return ( o1 ).getScientificName().compareTo( ( o2 ).getScientificName() );
        }
    };

    /**
     * AJAX
     * 
     * @param collection of <long> geneIds
     * @return collection of gene entity objects
     */
    public Collection<GeneValueObject> getGenes( Collection<Long> geneIds ) {
        if ( geneIds == null || geneIds.isEmpty() ) {
            return new HashSet<GeneValueObject>();
        }
        return GeneValueObject.convert2ValueObjects( geneService.thawLite( geneService.loadMultiple( geneIds ) ) );
    }

    /**
     * AJAX
     * 
     * @return a collection of the taxa in gemma (whether usable or not)
     */
    public Collection<Taxon> getTaxa() {
        SortedSet<Taxon> taxa = new TreeSet<Taxon>( TAXON_COMPARATOR );
        for ( Taxon taxon : taxonService.loadAll() ) {
            taxonService.thaw( taxon );
            taxa.add( taxon );
        }
        return taxa;
    }

    /**
     * AJAX
     * 
     * @return Taxon that are species. (only returns usable taxa)
     */
    public Collection<Taxon> getTaxaSpecies() {
        SortedSet<Taxon> taxaSpecies = new TreeSet<Taxon>( TAXON_COMPARATOR );
        for ( Taxon taxon : taxonService.loadAll() ) {
            if ( taxon.getIsSpecies() ) {
                taxonService.thaw( taxon );
                taxaSpecies.add( taxon );
            }
        }
        return taxaSpecies;
    }

    /**
     * AJAX
     * 
     * @return Taxon that have genes loaded into Gemma and that should be used
     */
    public Collection<Taxon> getTaxaWithGenes() {
        SortedSet<Taxon> taxaWithGenes = new TreeSet<Taxon>( TAXON_COMPARATOR );
        for ( Taxon taxon : taxonService.loadAll() ) {
            if ( taxon.getIsGenesUsable() ) {
                taxonService.thaw( taxon );
                taxaWithGenes.add( taxon );
            }
        }
        return taxaWithGenes;
    }

    /**
     * AJAX
     * 
     * @return collection of taxa that have expression experiments available.
     */
    public Collection<Taxon> getTaxaWithDatasets() {
        Set<Taxon> taxaWithDatasets = new TreeSet<Taxon>( TAXON_COMPARATOR );

        Map<Taxon, Long> perTaxonCount = expressionExperimentService.getPerTaxonCount();

        for ( Taxon taxon : taxonService.loadAll() ) {
            if ( perTaxonCount.containsKey( taxon ) && perTaxonCount.get( taxon ) > 0 ) {
                taxonService.thaw( taxon );
                taxaWithDatasets.add( taxon );
            }
        }
        return taxaWithDatasets;
    }

    /**
     * AJAX
     * 
     * @return List of taxa with array designs in gemma
     */
    public Collection<Taxon> getTaxaWithArrays() {
        Set<Taxon> taxaWithDatasets = new TreeSet<Taxon>( TAXON_COMPARATOR );

        taxaWithDatasets.addAll( arrayDesignService.getPerTaxonCount().keySet() );

        return taxaWithDatasets;
    }

    /**
     * AJAX (used by GeneCombo.js)
     * 
     * @param query
     * @param taxonId
     * @return Collection of Gene entity objects
     */
    public Collection<GeneValueObject> searchGenes( String query, Long taxonId ) {

        Taxon taxon = null;
        if ( taxonId != null ) {
            taxon = taxonService.load( taxonId );
        }
        SearchSettings settings = SearchSettings.geneSearch( query, taxon );
        List<SearchResult> geneSearchResults = searchService.search( settings ).get( Gene.class );

        Collection<Gene> genes = new HashSet<Gene>();
        if ( geneSearchResults == null || geneSearchResults.isEmpty() ) {
            log.info( "No Genes for search: " + query + " taxon=" + taxonId );
            return new HashSet<GeneValueObject>();
        }
        for ( SearchResult sr : geneSearchResults ) {
            genes.add( ( Gene ) sr.getResultObject() );
        }
        log.info( "Gene search: " + query + " taxon=" + taxonId + ", " + genes.size() + " found" );
        return GeneValueObject.convert2ValueObjects( geneService.thawLite( genes ) );
    }

    /**
     * AJAX Search for multiple genes at once. This attempts to limit the number of genes per query to only one.
     * 
     * @param query A list of gene names (symbols), one per line.
     * @param taxonId
     * @return colleciton of gene entity objects
     * @throws IOException
     */
    public Collection<GeneValueObject> searchMultipleGenes( String query, Long taxonId ) throws IOException {
        Taxon taxon = taxonService.load( taxonId );

        BufferedReader reader = new BufferedReader( new StringReader( query ) );
        Collection<Gene> genes = new HashSet<Gene>();
        String line = null;

        while ( ( line = reader.readLine() ) != null ) {
            if ( StringUtils.isBlank( line ) ) continue;
            if ( genes.size() >= MAX_GENES_PER_QUERY ) {
                log.warn( "Too many genes, stopping" );
                break;
            }
            line = StringUtils.strip( line );
            SearchSettings settings = SearchSettings.geneSearch( line, taxon );
            List<SearchResult> geneSearchResults = searchService.search( settings ).get( Gene.class ); // drops
            // predicted gene
            // results....

            // FIXME inform the user (on the client!) if there are some that don't have results.
            if ( geneSearchResults == null || geneSearchResults.isEmpty() ) {
                log.warn( "No gene results for gene with id: " + line );
            } else if ( geneSearchResults.size() == 1 ) { // Just one result so add it
                genes.add( ( Gene ) geneSearchResults.iterator().next().getResultObject() );

            } else { // Many results need to find best if possible
                Collection<Gene> notExactMatch = new HashSet<Gene>();
                Collection<Gene> sameTaxonMatch = new HashSet<Gene>();

                Boolean foundMatch = false;

                // Usually if there is more than 1 results the search term was a official symbol and picked up matches
                // like grin1, grin2, grin3, grin (given the search term was grin)
                for ( SearchResult sr : geneSearchResults ) {
                    Gene srGene = ( Gene ) sr.getResultObject();
                    if ( srGene.getOfficialSymbol().equalsIgnoreCase( line ) ) {
                        genes.add( srGene );
                        foundMatch = true;
                        break; // found so return
                    } else if ( srGene.getTaxon().equals( taxon ) ) {
                        sameTaxonMatch.add( srGene );
                    } else
                        notExactMatch.add( srGene );
                }

                // if no exact match found add all of them of the same taxon and toss a warning
                if ( !foundMatch ) {

                    if ( !sameTaxonMatch.isEmpty() ) {
                        genes.addAll( sameTaxonMatch );
                        log.warn( sameTaxonMatch.size() + " genes found for query id = " + line + ". Genes found are: "
                                + sameTaxonMatch + ". Adding All" );
                    } else {
                        log.warn( notExactMatch.size() + " genes found for query id = " + line + ". Genes found are: "
                                + notExactMatch + ". Adding None" );
                    }
                }
            }

        }
        return GeneValueObject.convert2ValueObjects( geneService.thawLite( genes ) );
    }

}