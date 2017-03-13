/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */
package ubic.gemma.expression.experiment.service;

import gemma.gsec.SecurityService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.common.search.SearchSettingsImpl;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.expression.experiment.FreeTextExpressionExperimentResultsValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchResultDisplayObject;
import ubic.gemma.search.SearchService;
import ubic.gemma.util.EntityUtils;

import java.util.*;

/**
 * Handles searching for experiments and experiment sets
 *
 * @author tvrossum
 */
@Component
public class ExpressionExperimentSearchServiceImpl implements ExpressionExperimentSearchService {

    private final Log log = LogFactory.getLog( this.getClass() );
    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;
    @Autowired
    private CoexpressionAnalysisService coexpressionAnalysisService;
    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private TaxonService taxonService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Override
    public List<SearchResultDisplayObject> getAllTaxonExperimentGroup( Long taxonId ) {

        List<SearchResultDisplayObject> setResults = new LinkedList<>();

        Taxon taxon = taxonService.load( taxonId );

        Collection<ExpressionExperimentSet> sets = expressionExperimentSetService
                .findByName( "Master set for " + taxon.getCommonName().toLowerCase() );
        SearchResultDisplayObject newSRDO;
        for ( ExpressionExperimentSet set : sets ) {
            expressionExperimentSetService.thaw( set );
            if ( set.getTaxon().getId().equals( taxonId ) ) {
                ExpressionExperimentSetValueObject eevo = expressionExperimentSetService.loadValueObject( set.getId() );
                newSRDO = new SearchResultDisplayObject( eevo );
                newSRDO.setUserOwned( securityService.isPrivate( set ) );
                ( ( ExpressionExperimentSetValueObject ) newSRDO.getResultValueObject() )
                        .setIsPublic( securityService.isPublic( set ) );
                setResults.add( newSRDO );
            }
        }

        Collections.sort( setResults );

        return setResults;
    }

    @Override
    public List<SearchResultDisplayObject> searchExperimentsAndExperimentGroups( String query, Long taxonId ) {

        List<SearchResultDisplayObject> displayResults = new LinkedList<>();

        // if query is blank, return list of public sets, user-owned sets (if logged in) and user's recent
        // session-bound sets (not autogen sets until handling of large searches is fixed)
        if ( StringUtils.isBlank( query ) ) {
            return this.searchExperimentsAndExperimentGroupBlankQuery( taxonId );
        }

        Map<Class<?>, List<SearchResult>> results = initialSearch( query, taxonId );

        List<SearchResultDisplayObject> experimentSets = getExpressionExperimentSetResults( results );
        List<SearchResultDisplayObject> experiments = getExpressionExperimentResults( results );

        if ( experimentSets.isEmpty() && experiments.isEmpty() ) {
            return displayResults;
        }

        /*
         * ALL RESULTS BY TAXON GROUPS
         */

        // if >1 result, add a group whose members are all experiments returned from search

        Map<Long, Set<Long>> eeIdsByTaxonId = new HashMap<>();

        // add every individual experiment to the set, grouped by taxon and also altogether.
        for ( SearchResultDisplayObject srdo : experiments ) {

            // group by the Parent Taxon, for things like salmonid - see bug 3286
            Long taxId;
            if ( srdo.getParentTaxonId() != null ) {
                taxId = srdo.getParentTaxonId();
            } else {
                taxId = srdo.getTaxonId();
            }

            if ( !eeIdsByTaxonId.containsKey( taxId ) ) {
                eeIdsByTaxonId.put( taxId, new HashSet<Long>() );
            }
            ExpressionExperimentValueObject eevo = ( ExpressionExperimentValueObject ) srdo.getResultValueObject();
            eeIdsByTaxonId.get( taxId ).add( eevo.getId() );
        }

        // if there's a group, get the number of members
        // assuming the taxon of the members is the same as that of the group

        // for each group
        for ( SearchResultDisplayObject eesSRO : experimentSets ) {
            ExpressionExperimentSetValueObject set = ( ExpressionExperimentSetValueObject ) eesSRO
                    .getResultValueObject();

            /*
             * This is security filtered.
             */
            Collection<Long> ids = EntityUtils
                    .getIds( expressionExperimentSetService.getExperimentValueObjectsInSet( set.getId() ) );

            set.setSize( ids.size() ); // to account for security filtering.

            if ( !eeIdsByTaxonId.containsKey( set.getTaxonId() ) ) {
                eeIdsByTaxonId.put( set.getTaxonId(), new HashSet<Long>() );
            }
            eeIdsByTaxonId.get( set.getTaxonId() ).addAll( ids );
        }

        // make an entry for each taxon

        Long taxonId2;
        for ( Map.Entry<Long, Set<Long>> entry : eeIdsByTaxonId.entrySet() ) {
            taxonId2 = entry.getKey();
            Taxon taxon = taxonService.load( taxonId2 );
            if ( taxon != null && entry.getValue().size() > 0 ) {

                FreeTextExpressionExperimentResultsValueObject ftvo = new FreeTextExpressionExperimentResultsValueObject(
                        "All " + taxon.getCommonName() + " results for '" + query + "'",
                        "All " + taxon.getCommonName() + " experiments found for your query", taxon.getId(),
                        taxon.getCommonName(), entry.getValue(), query );

                int numWithDifferentialExpressionAnalysis = differentialExpressionAnalysisService
                        .getExperimentsWithAnalysis( entry.getValue() ).size();

                assert numWithDifferentialExpressionAnalysis <= entry.getValue().size();

                int numWithCoexpressionAnalysis = coexpressionAnalysisService
                        .getExperimentsWithAnalysis( entry.getValue() ).size();

                ftvo.setNumWithCoexpressionAnalysis( numWithCoexpressionAnalysis );
                ftvo.setNumWithDifferentialExpressionAnalysis( numWithDifferentialExpressionAnalysis );
                displayResults.add( new SearchResultDisplayObject( ftvo ) );
            }
        }

        displayResults.addAll( experimentSets );
        displayResults.addAll( experiments );

        if ( displayResults.isEmpty() ) {
            log.info( "No results for search: " + query );
        } else {
            log.info( "Results for search: " + query + " size=" + displayResults.size() + " entry0: "
                    + ( ( SearchResultDisplayObject ) ( displayResults.toArray() )[0] ).getName() + " valueObject:"
                    + ( ( SearchResultDisplayObject ) ( displayResults.toArray() )[0] ).getResultValueObject()
                    .toString() );
        }
        return displayResults;
    }

    @Override
    public Collection<ExpressionExperimentValueObject> searchExpressionExperiments( String query ) {

        SearchSettings settings = SearchSettingsImpl.expressionExperimentSearch( query );
        List<SearchResult> experimentSearchResults = searchService.search( settings ).get( ExpressionExperiment.class );

        if ( experimentSearchResults == null || experimentSearchResults.isEmpty() ) {
            log.info( "No experiments for search: " + query );
            return new HashSet<>();
        }

        log.info( "Experiment search: " + query + ", " + experimentSearchResults.size() + " found" );
        Collection<ExpressionExperimentValueObject> experimentValueObjects = expressionExperimentService
                .loadValueObjects( EntityUtils.getIds( experimentSearchResults ), true );
        log.info( "Experiment search: " + experimentValueObjects.size() + " value objects returned." );
        return experimentValueObjects;
    }

    private List<SearchResultDisplayObject> getExpressionExperimentResults(
            Map<Class<?>, List<SearchResult>> results ) {
        // get all expressionExperiment results and convert result object into a value object
        List<SearchResult> srEEs = results.get( ExpressionExperiment.class );
        if ( srEEs == null ) {
            srEEs = new ArrayList<>();
        }

        List<Long> eeIds = new ArrayList<>();
        for ( SearchResult sr : srEEs ) {
            eeIds.add( sr.getId() );
        }

        Collection<ExpressionExperimentValueObject> eevos = expressionExperimentService.loadValueObjects( eeIds, true );
        List<SearchResultDisplayObject> experiments = new ArrayList<>();
        for ( ExpressionExperimentValueObject eevo : eevos ) {
            experiments.add( new SearchResultDisplayObject( eevo ) );
        }
        return experiments;
    }

    private List<SearchResultDisplayObject> getExpressionExperimentSetResults(
            Map<Class<?>, List<SearchResult>> results ) {
        List<SearchResultDisplayObject> experimentSets = new ArrayList<>();

        if ( results.get( ExpressionExperimentSet.class ) != null ) {
            List<Long> eeSetIds = new ArrayList<>();
            for ( SearchResult sr : results.get( ExpressionExperimentSet.class ) ) {
                eeSetIds.add( ( ( ExpressionExperimentSet ) sr.getResultObject() ).getId() );
            }

            if ( eeSetIds.isEmpty() ) {
                return experimentSets;
            }

            for ( ExpressionExperimentSetValueObject eesvo : expressionExperimentSetService
                    .loadValueObjects( eeSetIds ) ) {
                //
                // if ( !SecurityUtil.isUserAdmin() ) {
                // // have to security filter to get the accurate size. This is a performance hit so we don't do it.
                // eesvo.setSize( expressionExperimentSetService.getExperimentValueObjectsInSet( eesvo.getId() )
                // .size() );
                // }

                experimentSets.add( new SearchResultDisplayObject( eesvo ) );
            }
        }
        return experimentSets;
    }

    private Map<Class<?>, List<SearchResult>> initialSearch( String query, Long taxonId ) {
        SearchSettings settings = SearchSettingsImpl.expressionExperimentSearch( query );
        settings.setSearchExperimentSets( true ); // add searching for experimentSets
        Taxon taxonParam;
        if ( taxonId != null ) {
            taxonParam = taxonService.load( taxonId );
            settings.setTaxon( taxonParam );
        }
        return searchService.search( settings );
    }

    /**
     * if query is blank, return list of public sets, user-owned sets (if logged in) and user's recent session-bound
     * sets called by ubic.gemma.web.controller .expression.experiment.ExpressionExperimentController.
     * searchExperimentsAndExperimentGroup(String, Long) does not include session bound sets
     */
    private List<SearchResultDisplayObject> searchExperimentsAndExperimentGroupBlankQuery( Long taxonId ) {
        boolean taxonLimited = taxonId != null;

        List<SearchResultDisplayObject> displayResults = new LinkedList<>();

        // These are widely considered to be the most important results and
        // therefore need to be at the top
        List<SearchResultDisplayObject> masterResults = new LinkedList<>();

        Collection<ExpressionExperimentSetValueObject> evos = expressionExperimentSetService
                .loadAllExperimentSetValueObjects( true );

        for ( ExpressionExperimentSetValueObject evo : evos ) {

            if ( taxonLimited && !evo.getTaxonId().equals( taxonId ) ) {
                continue;
            }

            // if ( !SecurityUtil.isUserAdmin() ) {
            // // have to security filter the experiments in the set to get the accurate size. This is a performance
            // // hit so we are not doing it here.
            // evo.setSize( expressionExperimentSetService.getExperimentValueObjectsInSet( evo.getId() ).size() );
            // }

            SearchResultDisplayObject srdvo = new SearchResultDisplayObject( evo );

            // FIXME: could be spoofed by other users 'Master sets'
            String arbitraryMasterSetPrefix = "Master set for";
            if ( evo.getName().startsWith( arbitraryMasterSetPrefix ) ) {
                masterResults.add( srdvo );
            } else {
                displayResults.add( srdvo );
            }
        }

        Collections.sort( displayResults );

        // should we also sort by which species is most important(humans obviously) or is that not politically
        // correct???
        displayResults.addAll( 0, masterResults );

        return displayResults;
    }
}
