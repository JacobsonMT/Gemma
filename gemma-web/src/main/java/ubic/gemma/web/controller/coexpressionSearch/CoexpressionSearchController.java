/*
 * The Gemma project
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
package ubic.gemma.web.controller.coexpressionSearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.expression.coexpression.CoexpressionMetaValueObject;
import ubic.gemma.analysis.expression.coexpression.CoexpressionValueObjectExt;
import ubic.gemma.analysis.expression.coexpression.GeneCoexpressionService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetService;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.search.SearchService;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.view.TextView;

/**
 * @author luke
 * @version $Id$
 */
@Controller
public class CoexpressionSearchController extends BaseFormController {

    private static final int DEFAULT_STRINGENCY = 2;

    private static final int MAX_GENES_PER_QUERY = 20;

    private static final int MAX_RESULTS = 200;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    private GeneCoexpressionAnalysisService geneCoexpressionAnalysisService;

    @Autowired
    private GeneCoexpressionService geneCoexpressionService;

    @Autowired
    private GeneService geneService = null;

    @Autowired
    private SearchService searchService = null;

    /**
     * @param searchOptions
     * @return
     */
    public CoexpressionMetaValueObject doQuickSearch( CoexpressionSearchCommand searchOptions ) {

        CoexpressionMetaValueObject result = new CoexpressionMetaValueObject();

        if ( searchOptions.getGeneIds().size() != 1 ) {
            result.setErrorState( "Too many genes selected, please limit searches to one" );
            return result;
        }

        Gene gene = geneService.load( searchOptions.getGeneIds().iterator().next() );

        if ( gene == null ) {
            result.setErrorState( "Invalid gene id(s) - no genes found" );
            return result;

        }

        log.info( "Coexpression search: " + searchOptions );

        this.geneService.thawLite( gene ); // need to thaw externalDB in taxon for marshling back to client...

        Long eeSetId = getEESet( searchOptions, gene );

        if ( eeSetId == null ) {
            result.setErrorState( "No coexpression results available" );
            log.info( "No expression experiment set results for query: " + searchOptions );
            return result;
        }

        List<Gene> genes = new ArrayList<Gene>();
        genes.add( gene );
        result.setQueryGenes( genes );

        Collection<CoexpressionValueObjectExt> geneResults = geneCoexpressionService.coexpressionSearchQuick( eeSetId,
                genes, 2, 20, false, true );
        result.setKnownGeneResults( geneResults );

        if ( result.getKnownGeneResults() == null || result.getKnownGeneResults().isEmpty() ) {
            result.setErrorState( "Sorry, No genes are currently coexpressed under the selected search conditions " );
            log.info( "No search results for query: " + searchOptions );
        }

        return result;

    }

    /**
     * Main AJAX entry point
     * 
     * @param searchOptions
     * @return
     */
    public CoexpressionMetaValueObject doSearch( CoexpressionSearchCommand searchOptions ) {

        CoexpressionMetaValueObject result = new CoexpressionMetaValueObject();
        Collection<ExpressionExperiment> myEE = null;

        if ( searchOptions.getGeneIds() == null || searchOptions.getGeneIds().isEmpty() ) {
            return getEmptyResult();
        }

        if ( searchOptions.isQuick() ) {
            return doQuickSearch( searchOptions );
        }

        if ( searchOptions.getGeneIds().size() > MAX_GENES_PER_QUERY ) {
            result.setErrorState( "Too many genes selected, please limit searches to " + MAX_GENES_PER_QUERY );
            return result;
        }

        Collection<Gene> genes = geneService.loadMultiple( searchOptions.getGeneIds() );

        if ( genes.size() == 0 ) {
            result.setErrorState( "Invalid gene id(s) - no genes found" );
            return result;

        }

        this.geneService.thawLite( genes ); // need to thaw externalDB in taxon for marshling back to client...

        /*
         * Validation ...
         */
        if ( searchOptions.getTaxonId() != null ) {
            for ( Gene gene : genes ) {
                if ( !gene.getTaxon().getId().equals( searchOptions.getTaxonId() ) ) {
                    result
                            .setErrorState( "Search for gene from wrong taxon. Please check the genes match the selected taxon" );
                    return result;
                }
            }
        }

        // Add the users datasets to the selected datasets
        if ( searchOptions.isUseMyDatasets() ) {
            myEE = expressionExperimentService.loadMyExpressionExperiments();
            if ( myEE != null && !myEE.isEmpty() ) {
                for ( ExpressionExperiment ee : myEE )
                    searchOptions.getEeIds().add( ee.getId() );

                searchOptions.setForceProbeLevelSearch( true );
            } else
                log.info( "No user data to add" );
        }

        log.info( "Coexpression search: " + searchOptions );

        Long eeSetId = searchOptions.getEeSetId();

        if ( ( eeSetId == null || eeSetId < 0 ) && StringUtils.isNotBlank( searchOptions.getEeSetName() ) ) {
            Collection<ExpressionExperimentSet> eeSets = expressionExperimentSetService.findByName( searchOptions
                    .getEeSetName() );
            if ( eeSets.size() == 1 ) {
                eeSetId = eeSets.iterator().next().getId();
            } else {
                result.setErrorState( "Unknown or ambiguous set name: " + searchOptions.getEeSetName() );
                return result;

            }
        }

        if ( eeSetId != null && !searchOptions.isForceProbeLevelSearch() && ( eeSetId >= 0 && !searchOptions.isDirty() ) ) {
            result = geneCoexpressionService.coexpressionSearch( eeSetId, genes, searchOptions.getStringency(),
                    MAX_RESULTS, searchOptions.getQueryGenesOnly() );
        } else {
            assert ( searchOptions.getEeIds() != null && searchOptions.getEeIds().size() > 0 );

            result = geneCoexpressionService.coexpressionSearch( searchOptions.getEeIds(), genes, searchOptions
                    .getStringency(), MAX_RESULTS, searchOptions.getQueryGenesOnly(), searchOptions
                    .isForceProbeLevelSearch() );

        }

        if ( searchOptions.isUseMyDatasets() ) {
            addMyDataFlag( result, myEE );
        }

        if ( result.getKnownGeneResults() == null || result.getKnownGeneResults().isEmpty() ) {
            result
                    .setErrorState( "<b> Sorry, No genes are currently coexpressed under the selected search conditions </b>" );
            log.info( "No search results for query: " + searchOptions );
        }
        return result;

    }

    /**
     * @param query
     * @param taxonId
     * @return
     * @deprecated redundant with method in ExpressionExperimentController.
     */
    @Deprecated
    public Collection<Long> findExpressionExperiments( String query, Long taxonId ) {
        log.info( "Search: " + query + " taxon=" + taxonId );
        return searchService.searchExpressionExperiments( query, taxonId );
    }

    public CoexpressionMetaValueObject getEmptyResult() {
        return new CoexpressionMetaValueObject();
    }

    public void setGeneCoexpressionService( GeneCoexpressionService geneCoexpressionService ) {
        this.geneCoexpressionService = geneCoexpressionService;
    }

    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

    /*
     * Handle case of text export of the results.
     * @seeorg.springframework.web.servlet.mvc.AbstractFormController#handleRequestInternal(javax.servlet.http.
     * HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected ModelAndView handleRequestInternal( HttpServletRequest request, HttpServletResponse response )
            throws Exception {

        if ( request.getParameter( "export" ) != null ) {

            Collection<Long> geneIds = extractIds( request.getParameter( "g" ) );
            Collection<Gene> genes = geneService.loadMultiple( geneIds );

            boolean queryGenesOnly = request.getParameter( "q" ) != null;
            int stringency = DEFAULT_STRINGENCY;
            try {
                stringency = Integer.parseInt( request.getParameter( "s" ) );
            } catch ( Exception e ) {
                log.warn( "invalid stringency; using default " + stringency );
            }

            Long eeSetId = null;
            String eeSetIdString = request.getParameter( "a" );
            if ( StringUtils.isNotBlank( eeSetIdString ) ) {
                try {
                    eeSetId = Long.parseLong( eeSetIdString );
                } catch ( NumberFormatException e ) {
                    log.warn( "Invalid eeSet id: " + eeSetIdString );
                    return new ModelAndView( this.getFormView() );
                }
            }

            CoexpressionMetaValueObject result;
            if ( eeSetId != null ) {
                result = geneCoexpressionService.coexpressionSearch( eeSetId, genes, stringency, 500, queryGenesOnly );
            } else {
                Collection<Long> eeIds = extractIds( request.getParameter( "ee" ) );
                result = geneCoexpressionService.coexpressionSearch( eeIds, genes, stringency, MAX_RESULTS,
                        queryGenesOnly, false );
            }

            ModelAndView mav = new ModelAndView( new TextView() );
            String output = result.toString();
            mav.addObject( "text", output.length() > 0 ? output : "no results" );
            return mav;

        }
        return new ModelAndView( this.getFormView() );

    }

    private void addMyDataFlag( CoexpressionMetaValueObject vo, Collection<ExpressionExperiment> eesToFlag ) {

        Collection<Long> eesToFlagIds = new ArrayList<Long>();
        for ( ExpressionExperiment ee : eesToFlag ) {
            eesToFlagIds.add( ee.getId() );
        }

        if ( vo == null || vo.getKnownGeneResults() == null || vo.getKnownGeneResults().isEmpty() ) return;

        for ( CoexpressionValueObjectExt covo : vo.getKnownGeneResults() ) {
            for ( Long eeToFlag : eesToFlagIds ) {
                if ( covo.getSupportingExperiments().contains( eeToFlag ) ) {
                    covo.setContainsMyData( true );
                    break;
                }
            }
        }

    }

    /**
     * Locate an appropriate EESet to use.
     * 
     * @param searchOptions
     * @param gene
     * @return ID of the EESet, or null if none can be found.
     */
    private Long getEESet( CoexpressionSearchCommand searchOptions, Gene gene ) {
        ExpressionExperimentSet eeSet = null;
        Long eeSetId = null;

        if ( searchOptions.getEeSetId() != null ) {
            eeSet = expressionExperimentSetService.load( searchOptions.getEeSetId() );
            if ( eeSet != null ) eeSetId = eeSet.getId();

        } else {

            if ( searchOptions.getEeSetName() != null ) {
                Collection<ExpressionExperimentSet> eeSets = expressionExperimentSetService.findByName( searchOptions
                        .getEeSetName() );

                if ( eeSets == null || eeSets.size() == 0 ) {
                    return null;
                }
                if ( eeSets.size() > 1 ) {
                    log.warn( "more than one set found using 1st." );
                }

                eeSetId = eeSets.iterator().next().getId();

            } else {

                String analysisName = "All " + gene.getTaxon().getCommonName();
                Collection<GeneCoexpressionAnalysis> analyses = geneCoexpressionAnalysisService
                        .findByName( analysisName );

                if ( analyses.isEmpty() ) {
                    return null;
                } else if ( analyses.size() > 1 ) {
                    log.warn( "More than one analysis with name: " + analysisName );
                }

                GeneCoexpressionAnalysis analysis = analyses.iterator().next();

                eeSet = analysis.getExpressionExperimentSetAnalyzed();

                assert eeSet != null;

                eeSetId = eeSet.getId();

            }
        }
        return eeSetId;
    }

}