/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2008 University of British Columbia
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

package ubic.gemma.model.genome.gene;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;

import ubic.gemma.model.analysis.expression.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.PredictedGene;
import ubic.gemma.model.genome.ProbeAlignedRegion;
import ubic.gemma.model.genome.RelativeLocationData;
import ubic.gemma.model.genome.Taxon;

/**
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see gene.GeneService
 */
@Service
public class GeneServiceImpl extends GeneServiceBase {

    private static Log log = LogFactory.getLog( GeneServiceImpl.class.getName() );

    public Collection<Gene> find( PhysicalLocation physicalLocation ) {
        return this.getGeneDao().find( physicalLocation );
    }

    public RelativeLocationData findNearest( PhysicalLocation physicalLocation, boolean useStrand ) {
        return this.getGeneDao().findNearest( physicalLocation, useStrand );
    }

    @Override
    public Map<Gene, CoexpressionCollectionValueObject> handleGetCoexpressedGenes( Collection<Gene> genes,
            Collection<? extends BioAssaySet> ees, Integer stringency, boolean knownGenesOnly, boolean interGenesOnly ) {
        return this.getGeneDao().getCoexpressedGenes( genes, ees, stringency, knownGenesOnly, interGenesOnly );
    }

    @Override
    public void handleThawLite( Collection<Gene> genes ) {
        this.getGeneDao().thawLite( genes );
    }

    public void thawLite( Gene gene ) {
        this.getGeneDao().thawLite( gene );
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        return this.getGeneDao().countAll();
    }

    /*
     * (non-Javadoc)
     * @see gene.GeneServiceBase#handleCreate(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<Gene> handleCreate( Collection<Gene> genes ) throws Exception {
        return ( Collection<Gene> ) this.getGeneDao().create( genes );

    }

    @Override
    protected Gene handleCreate( Gene gene ) throws Exception {
        return this.getGeneDao().create( gene );
    }

    /*
     * (non-Javadoc)
     * @see gene.GeneServiceBase#handleFind(Gene)
     */
    @Override
    protected Gene handleFind( Gene gene ) throws Exception {
        return this.getGeneDao().find( gene );
    }

    @Override
    protected Gene handleFindByAccession( String accession, ExternalDatabase source ) throws Exception {
        return this.getGeneDao().findByAccession( accession, source );
    }

    /*
     * (non-Javadoc)
     * @see gene.GeneServiceBase#handleGetByGeneAlias(java.lang.String)
     */
    @Override
    protected Collection<Gene> handleFindByAlias( String search ) throws Exception {
        return this.getGeneDao().findByAlias( search );
    }

    /**
     * @see gene.GeneService#handleFindByID(java.lang.long)
     */
    protected Gene handleFindByID( Long id ) throws java.lang.Exception {
        return this.getGeneDao().load( id );
    }

    @Override
    protected Gene handleFindByNCBIId( String accession ) throws Exception {
        Collection<Gene> genes = this.getGeneDao().findByNcbiId( accession );
        if ( genes.size() > 1 ) {
            log.warn( "More than one gene with accession=" + accession );
        } else if ( genes.size() == 1 ) {
            return genes.iterator().next();
        }
        return null;

    }

    /**
     * @see gene.GeneService#findByOfficialName(java.lang.String)
     */
    @Override
    protected java.util.Collection<Gene> handleFindByOfficialName( java.lang.String officialName )
            throws java.lang.Exception {
        return this.getGeneDao().findByOfficialName( officialName );
    }

    /**
     * @see gene.GeneService#findByOfficialSymbol(java.lang.String)
     */
    @Override
    protected java.util.Collection<Gene> handleFindByOfficialSymbol( java.lang.String officialSymbol )
            throws java.lang.Exception {
        return this.getGeneDao().findByOfficalSymbol( officialSymbol );
    }

    @Override
    protected Gene handleFindByOfficialSymbol( String symbol, Taxon taxon ) throws Exception {
        return this.getGeneDao().findByOfficialSymbol( symbol, taxon );
    }

    /**
     * @see gene.GeneService#handleFindByOfficialSymbolInexact(java.lang.String)
     */
    @Override
    protected java.util.Collection<Gene> handleFindByOfficialSymbolInexact( java.lang.String officialSymbol )
            throws java.lang.Exception {
        return this.getGeneDao().findByOfficialSymbolInexact( officialSymbol );
    }

    @Override
    protected Gene handleFindOrCreate( Gene gene ) throws Exception {
        return this.getGeneDao().findOrCreate( gene );
    }

    /*
     * (non-Javadoc)
     * @see gene.GeneServiceBase#handleGetCoexpressedGenes(Gene)
     */
    @Override
    protected CoexpressionCollectionValueObject handleGetCoexpressedGenes( Gene gene,
            Collection<? extends BioAssaySet> ees, Integer stringency, boolean knownGenesOnly ) throws Exception {
        return this.getGeneDao().getCoexpressedGenes( gene, ees, stringency, knownGenesOnly );
    }

    @Override
    protected long handleGetCompositeSequenceCountById( Long id ) throws Exception {
        return this.getGeneDao().getCompositeSequenceCountById( id );
    }

    /*
     * (non-Javadoc)
     * @see gene.GeneServiceBase#handleGetCompositeSequencesById(Gene,
     * ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection<CompositeSequence> handleGetCompositeSequences( Gene gene, ArrayDesign arrayDesign )
            throws Exception {
        return this.getGeneDao().getCompositeSequences( gene, arrayDesign );
    }

    @Override
    protected Collection<CompositeSequence> handleGetCompositeSequencesById( Long id ) throws Exception {
        return this.getGeneDao().getCompositeSequencesById( id );
    }

    @Override
    protected Collection<Gene> handleGetGenesByTaxon( Taxon taxon ) throws Exception {
        return this.getGeneDao().getGenesByTaxon( taxon );
    }

    /*
     * (non-Javadoc)
     * @see gene.GeneServiceBase#handleGetMicroRnaByTaxon(Taxon)
     */
    @Override
    protected Collection<Gene> handleGetMicroRnaByTaxon( Taxon taxon ) throws Exception {
        return this.getGeneDao().getMicroRnaByTaxon( taxon );
    }

    @Override
    protected Gene handleLoad( long id ) throws Exception {
        return this.getGeneDao().load( id );
    }

    /**
     * @see gene.GeneServiceBase#handleGetAllGenes()
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<Gene> handleLoadAll() throws Exception {
        return ( Collection<Gene> ) this.getGeneDao().loadAll();
    }

    @Override
    protected Collection<Gene> handleLoadKnownGenes( Taxon taxon ) throws Exception {
        return this.getGeneDao().loadKnownGenes( taxon );
    }

    /*
     * (non-Javadoc)
     * @see gene.GeneServiceBase#handleLoadMultiple(java.util.Collection)
     */
    @Override
    protected Collection<Gene> handleLoadMultiple( Collection<Long> ids ) throws Exception {
        return this.getGeneDao().load( ids );
    }

    @Override
    protected Collection<PredictedGene> handleLoadPredictedGenes( Taxon taxon ) throws Exception {
        return this.getGeneDao().loadPredictedGenes( taxon );
    }

    @Override
    protected Collection<ProbeAlignedRegion> handleLoadProbeAlignedRegions( Taxon taxon ) throws Exception {
        return this.getGeneDao().loadProbeAlignedRegions( taxon );
    }

    /*
     * (non-Javadoc)
     * @see gene.GeneServiceBase#handleRemove(java.util.Collection)
     */
    @Override
    protected void handleRemove( Collection<Gene> genes ) throws Exception {
        this.getGeneDao().remove( genes );

    }

    @Override
    protected void handleRemove( Gene gene ) throws Exception {
        this.getGeneDao().remove( gene );
    }

    /**
     * This was created because calling saveGene from Spring causes caching errors. I left saveGene in place on the
     * assumption that Kiran's loaders use it with success.
     * 
     * @see gene.GeneService#createGene(Gene)
     */
    protected Gene handleSaveGene( Gene gene ) throws java.lang.Exception {
        return this.getGeneDao().create( gene );
    }

    @Override
    protected void handleThaw( Gene gene ) throws Exception {
        this.getGeneDao().thaw( gene );
    }

    /**
     * This was created because calling saveGene with an existing gene actually causes a caching error in Spring.
     * 
     * @see gene.GeneService#updateGene(Gene)
     */
    @Override
    protected void handleUpdate( Gene gene ) throws java.lang.Exception {
        this.getGeneDao().update( gene );
    }

    @Override
    public PhysicalLocation getMaxPhysicalLength( Long geneId ) {
        if ( geneId == null ) return null;

        Gene gene = this.load( geneId );
        this.thaw( gene );

        Collection<GeneProduct> gpCollection = gene.getProducts();

        if ( gpCollection == null ) return null;

        Long minStartNt = null;
        Long maxEndNt = null;
        String strand = null;
        Chromosome chromosone = null;

        for ( GeneProduct gp : gpCollection ) {

            PhysicalLocation pLoc = gp.getPhysicalLocation();
            if ( pLoc == null ) {
                log.warn("No phyiscal location for Gene: " + gene.getOfficialSymbol() + "'s Gene Product: " + gp.getId() + ". Skipping.");
                continue;
            }

            String currentStrand = pLoc.toString();
            Chromosome currentChromosone = pLoc.getChromosome();
            Long currentStartNt = pLoc.getNucleotide();
            Long currentEndNt = currentStartNt + pLoc.getNucleotideLength();

            // 1st time through loop
            if ( minStartNt == null ) {
                minStartNt = currentStartNt;
                maxEndNt = currentEndNt;
                strand = currentStrand;
                chromosone = currentChromosone;
                continue;
            }

            // FIXME: This is defensive coding. Not sure if this will ever happen. If it does, will need to sort the
            // gene products in advance to remove the outliers. Currently this method is assuming the 1st gene product
            // is not the outlier.
            if ( !currentStrand.equalsIgnoreCase( strand ) ) {
                log
                        .warn( "Gene products for "
                                + gene.getOfficialSymbol()
                                + " , Id="
                                + gene.getId()
                                + " are on different strands. Unable to compute distance when products are on different strands. Skipping Gene product: "
                                + gp.getId() );
                continue;
            }

            if ( !currentChromosone.equals( chromosone ) ) {
                log
                        .warn( "Gene products for "
                                + gene.getOfficialSymbol()
                                + " , Id="
                                + gene.getId()
                                + " are on different chromosones. Unable to compute distance when gene products are on different chromosomes. Skipping Gene product: "
                                + gp.getId() );

                continue;
            }

            if ( currentStartNt < minStartNt ) minStartNt = currentStartNt;

            if ( currentEndNt > maxEndNt ) maxEndNt = currentEndNt;

        } // for each gene product

        Long length = maxEndNt - minStartNt;
        PhysicalLocation result = PhysicalLocation.Factory.newInstance(minStartNt, length.intValue(), strand, null, chromosone);
        return result;

    }

}