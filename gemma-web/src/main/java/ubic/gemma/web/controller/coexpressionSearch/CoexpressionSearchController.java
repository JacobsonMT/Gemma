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
package ubic.gemma.web.controller.coexpressionSearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.ontology.GeneOntologyService;
import ubic.gemma.loader.genome.taxon.SupportedTaxa;
import ubic.gemma.model.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.coexpression.CoexpressionValueObject;
import ubic.gemma.model.common.description.OntologyEntry;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.search.SearchService;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BackgroundProcessingFormBindController;
import ubic.gemma.web.propertyeditor.TaxonPropertyEditor;
import ubic.gemma.web.util.ConfigurationCookie;
import ubic.gemma.web.util.MessageUtil;

/**
 * A <link>SimpleFormController<link> providing search functionality of genes or design elements (probe sets). The
 * success view returns either a visual representation of the result set or a downloadable data file.
 * <p>
 * {@link stringency} sets the number of data sets the link must be seen in before it is listed in the results, and
 * {@link species} sets the type of species to search. {@link keywords} restrict the search.
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="coexpressionSearchController"
 * @spring.property name = "commandName" value="coexpressionSearchCommand"
 * @spring.property name = "commandClass" value="ubic.gemma.web.controller.coexpressionSearch.CoexpressionSearchCommand"
 * @spring.property name = "formView" value="searchCoexpression"
 * @spring.property name = "successView" value="searchCoexpression"
 * @spring.property name = "geneService" ref="geneService"
 * @spring.property name = "taxonService" ref="taxonService"
 * @spring.property name = "searchService" ref="searchService"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "geneOntologyService" ref="geneOntologyService"
 * @spring.property name = "validator" ref="genericBeanValidator"
 */
public class CoexpressionSearchController extends BackgroundProcessingFormBindController {
    private static Log log = LogFactory.getLog( CoexpressionSearchController.class.getName() );

    private int MAX_GENES_TO_RETURN = 50;
    private int DEFAULT_STRINGENCY = 3;

    private static final String COOKIE_NAME = "coexpressionSearchCookie";
    private static final int MAX_OVERLAP = 40;

    private GeneService geneService = null;
    private TaxonService taxonService = null;
    private SearchService searchService = null;
    private ExpressionExperimentService expressionExperimentService = null;
    private GeneOntologyService geneOntologyService;

    public CoexpressionSearchController() {
        /*
         * if true, reuses the same command object across the edit-submit-process (get-post-process).
         */
        setSessionForm( true );
    }

    /**
     * @param request
     * @return Object
     * @throws ServletException
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) {

        CoexpressionSearchCommand csc = new CoexpressionSearchCommand();
        
        if (request.getParameter( "id" ) != null) {
            loadGETParameters( request, csc );
        }
        else if ( request.getParameter( "searchString" ) != null  ) {
            loadGETParameters( request, csc );
        } else {
            loadCookie( request, csc );
        }
        return csc;

    }

    /**
     * Mock function - do not use.
     * 
     * @param request
     * @param response
     * @param command
     * @param errors
     * @return ModelAndView
     * @throws Exception
     */
    @SuppressWarnings( { "unused", "unchecked" })
    @Override
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        CoexpressionSearchCommand csc = ( ( CoexpressionSearchCommand ) command );

        Collection<Gene> genesFound = new HashSet<Gene>();

        // find the genes specified by the search
        // if an id is specified, find the identified gene
        // if there is no exact search specified, do an inexact search
        // if exact search is on, find only by official symbol
        // if exact search is auto (usually from the front page), check if there is an exact search match. If there is
        // none, do inexact search.
        if (csc.getId() != null) {
            Gene g = geneService.load( Long.parseLong( csc.getId() ) );
            genesFound.add( g);
            csc.setExactSearch( g.getOfficialName() );
        }
        else if ( csc.getExactSearch() == null ) {
            genesFound.addAll( searchService.geneDbSearch( csc.getSearchString() ) );
            genesFound.addAll( searchService.compassGeneSearch( csc.getSearchString() ) );
        } else if ( csc.getGeneIdSearch().equalsIgnoreCase( "true" ) ) {
            String geneId = csc.getSearchString();
            Long id = Long.parseLong( geneId );
            Collection<Long> ids = new ArrayList<Long>();
            ids.add( id );
            genesFound.addAll( geneService.load( ids ) );
        } else if ( csc.getExactSearch().equalsIgnoreCase( "on" ) ) {
            genesFound.addAll( geneService.findByOfficialSymbol( csc.getSearchString() ) );
        } else {
            genesFound.addAll( geneService.findByOfficialSymbol( csc.getSearchString() ) );
            if ( genesFound.size() == 0 ) {
                genesFound.addAll( searchService.geneDbSearch( csc.getSearchString() ) );
                genesFound.addAll( searchService.compassGeneSearch( csc.getSearchString() ) );
            }
        }
        
        Cookie cookie = new CoexpressionSearchCookie( csc );
        response.addCookie( cookie );
        // filter genes by Taxon

        Collection<Gene> genesToRemove = new ArrayList<Gene>();
        if ( !( csc.getGeneIdSearch().equalsIgnoreCase( "true" ) ) && ( csc.getTaxon() != null )
                && ( csc.getTaxon().getId() != null ) ) {
            for ( Gene gene : genesFound ) {
                if ( gene.getTaxon().getId().longValue() != csc.getTaxon().getId().longValue() ) {
                    genesToRemove.add( gene );
                }
            }
            genesFound.removeAll( genesToRemove );
        }

        // if no genes found
        // return error
        if ( genesFound.size() == 0 ) {
            saveMessage( request, "No genes found based on criteria." );
            return super.showForm( request, response, errors );
        }

        // check if more than 1 gene found
        // if yes, then query user for gene to be used
        if ( genesFound.size() > 1 ) {
            // check if more than 50 genes have been found.
            // if there are more, then warn the user and truncate list
            if ( genesFound.size() > MAX_GENES_TO_RETURN ) {
                genesToRemove = new ArrayList<Gene>();
                int count = 0;
                for ( Gene gene : genesFound ) {
                    if ( count >= MAX_GENES_TO_RETURN ) {
                        genesToRemove.add( gene );
                    }
                    count++;
                }
                genesFound.removeAll( genesToRemove );
                saveMessage( request, "Found " + count + " genes. Truncating list to first " + MAX_GENES_TO_RETURN
                        + "." );
            }

            saveMessage( request, "Multiple genes matched. Choose which gene to use." );

            // set to exact search
            csc.setExactSearch( "on" );
            ModelAndView mav = super.showForm( request, errors, getFormView() );
            mav.addObject( "genes", genesFound );
            return mav;
        }

        // At this point, only one gene has been found
        // set command object to reflect this
        csc.setSearchString( genesFound.iterator().next().getOfficialSymbol() );
        csc.setGeneIdSearch( "false" );

        // find coexpressed genes

        // find expressionExperiments via lucene if the query is eestring-constrained
        Collection<ExpressionExperiment> ees;
        if ( StringUtils.isNotBlank( csc.getEeSearchString() ) ) {
            ees = searchService.compassExpressionSearch( csc.getEeSearchString() );
            if ( ees.size() == 0 ) {
                saveMessage( request, "No datasets matched - defaulting to all datasets" );
            }
        } else {
            ees = new ArrayList<ExpressionExperiment>();
        }

        Gene sourceGene = ( Gene ) ( genesFound.toArray() )[0];
        csc.setSourceGene( sourceGene );

        Integer numExpressionExperiments = 0;
        Collection<Long> possibleEEs = expressionExperimentService.findByGene( sourceGene );

        if ( ( possibleEEs == null ) || ( possibleEEs.isEmpty() ) ) {
            ModelAndView mav = super.showForm( request, errors, getFormView() );
            saveMessage( request, "There are no " + csc.getTaxon().getScientificName()
                    + " arrays in the system that assay for the gene " + csc.getSourceGene().getOfficialSymbol() );
            return mav;
        }

        if ( ees.size() > 0 ) {
            // if there are matches, fihter the expression experiments first by taxon

            Collection<ExpressionExperiment> eeToRemove = new HashSet<ExpressionExperiment>();
            for ( ExpressionExperiment ee : ees ) {
                Taxon t = expressionExperimentService.getTaxon( ee.getId() );
                if ( t.getId().longValue() != csc.getTaxon().getId().longValue() ) eeToRemove.add( ee );

                if ( !possibleEEs.contains( ee.getId() ) ) eeToRemove.add( ee );
            }
            ees.removeAll( eeToRemove );

            if ( ees.isEmpty() ) {
                ModelAndView mav = super.showForm( request, errors, getFormView() );
                saveMessage( request, "There are no " + csc.getTaxon().getScientificName()
                        + " arrays in the system that assay for the gene " + csc.getSourceGene().getOfficialSymbol()
                        + " matching search criteria " + csc.getEeSearchString() );
                return mav;
            }

        } else {
        //    ees = expressionExperimentService.load( possibleEEs );
        }
        csc.setToUseEE( ees );
        numExpressionExperiments = ees.size();

        // stringency. Cannot be less than 1; set to one if it is
        Integer stringency = csc.getStringency();
        if ( stringency == null ) {
            stringency = DEFAULT_STRINGENCY;
        } else if ( stringency < 1 ) {
            stringency = DEFAULT_STRINGENCY;
        }
        csc.setStringency( stringency );

        // ===================================================================================
        // Taking out progress for the mean time while we figure out the back button problem
        // return startJob( command, request, response, errors );
        // ===================================================================================
        CoexpressionCollectionValueObject coexpressions = ( CoexpressionCollectionValueObject ) geneService
                .getCoexpressedGenes( csc.getSourceGene(), csc.getToUseEE(), csc.getStringency() );

        StopWatch watch = new StopWatch();

        watch.start();

        // get all the coexpressed genes and sort them by dataset count
        List<CoexpressionValueObject> coexpressedGenes = new ArrayList<CoexpressionValueObject>();
        coexpressedGenes.addAll( coexpressions.getCoexpressionData() );

        // sort coexpressed genes by dataset count
        Collections.sort( coexpressedGenes, new CoexpressionComparator() );

        // load expression experiment value objects
        Collection<Long> eeIds = new HashSet<Long>();
        Collection<ExpressionExperimentValueObject> origEeVos = coexpressions.getExpressionExperiments();
        for ( ExpressionExperimentValueObject eeVo : origEeVos ) {
            eeIds.add( eeVo.getId() );
        }

        Collection<ExpressionExperimentValueObject> eeVos = expressionExperimentService.loadValueObjects( eeIds );

        for ( ExpressionExperimentValueObject eeVo : eeVos ) {
            eeVo.setCoexpressionLinkCount( coexpressions.getLinkCountForEE( eeVo.getId() ) );
            eeVo.setRawCoexpressionLinkCount( coexpressions.getRawLinkCountForEE( eeVo.getId() ) );
            eeVo.setSpecific( coexpressions.getExpressionExperiment( eeVo.getId() ).isSpecific() );
        }

        // new ModelAndView(getSuccessView());

        // no genes are coexpressed
        // return error
        if ( coexpressedGenes.size() == 0 ) {
            this.saveMessage( request, "No genes are coexpressed with the given stringency." );
        }
        int numSourceGeneGoTerms = computeGoOverlap( csc, coexpressedGenes );

        
        // still get the number of EEs that are used even if it is not given as a keyword search.
        if ( ees.size() == 0 ) {
            ees = expressionExperimentService.load( possibleEEs );
        }
        csc.setToUseEE( ees );
        numExpressionExperiments = ees.size();
        
        Long numUsedExpressionExperiments = new Long( coexpressions.getNumberOfUsedExpressonExperiments() );
        Long numPositiveCoexpressedGenes = new Long( coexpressions.getPositiveStringencyLinkCount() );
        Long numNegativeCoexpressedGenes = new Long( coexpressions.getNegativeStringencyLinkCount() );
        Long numGenes = new Long( coexpressions.getNumGenes() );
        Long numPredictedGenes = new Long( coexpressions.getNumPredictedGenes() );
        Long numProbeAlignedRegions = new Long( coexpressions.getNumProbeAlignedRegions() );
        Long numStringencyGenes = new Long( coexpressions.getNumStringencyGenes() );
        Long numStringencyPredictedGenes = new Long( coexpressions.getNumStringencyPredictedGenes() );
        Long numStringencyProbeAlignedRegions = new Long( coexpressions.getNumStringencyProbeAlignedRegions() );
        Integer numMatchedLinks = coexpressions.getLinkCount();

        ModelAndView mav = super.showForm( request, response, errors );

        mav.addObject( "coexpressedGenes", coexpressedGenes );
        mav.addObject( "numPositiveCoexpressedGenes", numPositiveCoexpressedGenes );
        mav.addObject( "numNegativeCoexpressedGenes", numNegativeCoexpressedGenes );
        mav.addObject( "numSearchedExpressionExperiments", csc.getToUseEE().size() );
        mav.addObject( "numUsedExpressionExperiments", numUsedExpressionExperiments );

        mav.addObject( "numGenes", numGenes );
        mav.addObject( "numPredictedGenes", numPredictedGenes );
        mav.addObject( "numProbeAlignedRegions", numProbeAlignedRegions );

        mav.addObject( "numStringencyGenes", numStringencyGenes );
        mav.addObject( "numStringencyPredictedGenes", numStringencyPredictedGenes );
        mav.addObject( "numStringencyProbeAlignedRegions", numStringencyProbeAlignedRegions );
        mav.addObject( "numSourceGeneGoTerms", numSourceGeneGoTerms );

        mav.addObject( "numMatchedLinks", numMatchedLinks );
        mav.addObject( "sourceGene", csc.getSourceGene() );
        mav.addObject( "expressionExperiments", eeVos );
        mav.addObject( "numLinkedExpressionExperiments", new Integer( eeVos.size() ) );

        // binding objects
        mav.addObject( "coexpressionSearchCommand", csc );
        populateTaxonReferenceData( mav.getModel() );
        mav.addAllObjects( errors.getModel() );

        Long elapsed = watch.getTime();
        watch.stop();
        log.info( "Processing after DAO call (elapsed time): " + elapsed );

        this.saveMessage( request, "Coexpression query took: " + coexpressions.getElapsedWallSeconds() );

        return mav;

    }

    /**
     * @param request
     * @return Map
     */
    @SuppressWarnings("unused")
    @Override
    protected Map referenceData( HttpServletRequest request ) {
        Map<String, List<? extends Object>> mapping = new HashMap<String, List<? extends Object>>();

        // add species
        populateTaxonReferenceData( mapping );

        return mapping;
    }

    /**
     * @param mapping
     */
    @SuppressWarnings("unchecked")
    private void populateTaxonReferenceData( Map mapping ) {
        List<Taxon> taxa = new ArrayList<Taxon>();
        for ( Taxon taxon : ( Collection<Taxon> ) taxonService.loadAll() ) {
            if ( !SupportedTaxa.contains( taxon ) ) {
                continue;
            }
            taxa.add( taxon );
        }
        Collections.sort( taxa, new Comparator<Taxon>() {
            public int compare( Taxon o1, Taxon o2 ) {
                return ( o1 ).getScientificName().compareTo( ( o2 ).getScientificName() );
            }
        } );
        mapping.put( "taxa", taxa );
    }

    @Override
    protected void initBinder( HttpServletRequest request, ServletRequestDataBinder binder ) {
        super.initBinder( request, binder );
        binder.registerCustomEditor( Taxon.class, new TaxonPropertyEditor( this.taxonService ) );
    }

    /**
     * @param request
     * @param csc
     */
    private void loadCookie( HttpServletRequest request, CoexpressionSearchCommand csc ) {

        // cookies aren't all that important, if they're missing we just go on.
        if ( request == null || request.getCookies() == null ) return;

        for ( Cookie cook : request.getCookies() ) {
            if ( cook.getName().equals( COOKIE_NAME ) ) {
                try {
                    ConfigurationCookie cookie = new ConfigurationCookie( cook );
                    csc.setEeSearchString( cookie.getString( "eeSearchString" ) );

                    csc.setStringency( cookie.getInt( "stringency" ) );
                    Taxon taxon = taxonService.findByScientificName( cookie.getString( "taxonScientificName" ) );
                    csc.setTaxon( taxon );

                    // save the gene name. If the gene id is on, then convert the ID to a gene first
                    String searchString = cookie.getString( "searchString" );
                    csc.setSearchString( searchString );
                    
                    String id = cookie.getString( "id" );
                    csc.setId( id );

                } catch ( Exception e ) {
                    log.warn( "Cookie could not be loaded: " + e.getMessage() );
                    // that's okay, we just don't get a cookie.
                }
            }
        }
    }

    /**
     * Fills in the command object in the case that GET parameters are passed in
     * 
     * @param request
     * @param csc
     */
    private void loadGETParameters( HttpServletRequest request, CoexpressionSearchCommand csc ) {
        if ( request == null) {
            return;
        }
        if ( ( request.getParameter( "searchString" ) == null ) && (request.getParameter( "id" ) == null) ) return;
        

        Map params = request.getParameterMap();

        if ( params.get( "eeSearchString" ) != null ) {
            csc.setEeSearchString( ( ( String[] ) params.get( "eeSearchString" ) )[0] );
        }
        if ( params.get( "geneIdSearch" ) != null ) {
            String[] geneIdSearch = ( String[] ) params.get( "geneIdSearch" );
            csc.setGeneIdSearch( geneIdSearch[0] );
        }
        if ( params.get( "stringency" ) != null ) {
            String[] stringency = ( String[] ) params.get( "stringency" );
            Integer num = Integer.parseInt( stringency[0] );
            csc.setStringency( num );
        }
        if ( params.get( "taxon" ) != null ) {
            Taxon taxon = taxonService.findByScientificName( ( ( String[] ) params.get( "taxon" ) )[0] );
            csc.setTaxon( taxon );
        }
        if ( params.get( "searchString" ) != null ) {

            String searchString = ( ( String[] ) params.get( "searchString" ) )[0];

            csc.setSearchString( searchString );
        }
        if (params.get( "id" ) != null)  {
            String id = ( ( String[] ) params.get( "id" ) )[0];
            csc.setId( id );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.SimpleFormController#showForm(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse, org.springframework.validation.BindException)
     */
    protected ModelAndView showForm( HttpServletRequest request, HttpServletResponse response, BindException errors )
            throws Exception {
        if ( request.getParameter( "searchString" ) != null || request.getParameter("id") != null ) {
            return this.onSubmit( request, response, this.formBackingObject( request ), errors );
        }

        return super.showForm( request, response, errors );
    }

    /**
     * @param geneService
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @param taxonService the taxonService to set
     */
    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    /**
     * @param searchService the searchService to set
     */
    public void setSearchService( SearchService searchService ) {
        this.searchService = searchService;
    }

    /**
     * @param expressionExperimentService the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    private int computeGoOverlap( CoexpressionSearchCommand csc, List<CoexpressionValueObject> coexpressedGenes )
            throws Exception {

        // streamlining: don't compute this if we aren't loading GO into memory.
        boolean loadOntology = ConfigUtils.getBoolean( "loadOntology" );
        if ( !loadOntology ) {
            log.warn( "Won't compute GO overlaps, it will take too long without GO cache." );
            return 0;
        }

        StopWatch overlapWatch = new StopWatch();
        overlapWatch.start();
        log.info( "Calculating go overlap...." );
        // calculate the goOverlap for the 1st 25 genes
        Collection<Long> overlapIds = new HashSet<Long>();
        int i = 0;
        for ( CoexpressionValueObject cvo : coexpressedGenes ) {
            overlapIds.add( cvo.getGeneId() );
            if ( i++ > MAX_OVERLAP ) break;
        }

        Map<Long, Collection<OntologyEntry>> overlap = geneOntologyService.calculateGoTermOverlap( csc.getSourceGene(),
                overlapIds );

        Integer numSourceGeneGoTerms;
        if ( overlap == null ) // query gene had no go terms
            numSourceGeneGoTerms = 0;
        else {
            numSourceGeneGoTerms = overlap.get( csc.getSourceGene().getId() ).size();

            if ( overlap.keySet().size() > 1 ) {
                for ( CoexpressionValueObject cvo : coexpressedGenes ) {
                    cvo.setGoOverlap( overlap.get( cvo.getGeneId() ) );
                    cvo.setPossibleOverlap( numSourceGeneGoTerms );
                }
            }
        }
        Long overlapTime = overlapWatch.getTime();
        overlapWatch.stop();
        log.info( "took " + overlapTime / 1000 + "s to calculate GO overlap" );
        return numSourceGeneGoTerms;
    }

    @Override
    protected BackgroundControllerJob<ModelAndView> getRunner( String taskId, SecurityContext securityContext,
            final HttpServletRequest request, final HttpServletResponse response, final Object command,
            final MessageUtil messenger, final BindException errors ) {

        return new BackgroundControllerJob<ModelAndView>( taskId, securityContext, request, response, command,
                messenger, errors ) {

            @SuppressWarnings("unchecked")
            public ModelAndView call() throws Exception {

                SecurityContextHolder.setContext( securityContext );
                CoexpressionSearchCommand csc = ( CoexpressionSearchCommand ) command;

                ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), securityContext
                        .getAuthentication().getName(), "Coexpression analysis for "
                        + csc.getSourceGene().getOfficialSymbol() );

                job.updateProgress( "Analyzing coexpresson for " + csc.getSourceGene().getOfficialSymbol() );
                CoexpressionCollectionValueObject coexpressions = ( CoexpressionCollectionValueObject ) geneService
                        .getCoexpressedGenes( csc.getSourceGene(), csc.getToUseEE(), csc.getStringency() );

                StopWatch watch = new StopWatch();

                watch.start();

                // get all the coexpressed genes and sort them by dataset count
                List<CoexpressionValueObject> coexpressedGenes = new ArrayList<CoexpressionValueObject>();
                coexpressedGenes.addAll( coexpressions.getCoexpressionData() );

                // sort coexpressed genes by dataset count
                Collections.sort( coexpressedGenes, new CoexpressionComparator() );

                computeGoOverlap( csc, coexpressedGenes );

                // load expression experiment value objects
                Collection<Long> eeIds = new HashSet<Long>();
                Collection<ExpressionExperimentValueObject> origEeVos = coexpressions.getExpressionExperiments();
                for ( ExpressionExperimentValueObject eeVo : origEeVos ) {
                    eeIds.add( eeVo.getId() );
                }

                Collection<ExpressionExperimentValueObject> eeVos = expressionExperimentService
                        .loadValueObjects( eeIds );
                // add link count information to ee value objects
                // coexpressions.calculateLinkCounts();
                // coexpressions.calculateRawLinkCounts();

                for ( ExpressionExperimentValueObject eeVo : eeVos ) {
                    eeVo.setCoexpressionLinkCount( coexpressions.getLinkCountForEE( eeVo.getId() ) );
                    eeVo.setRawCoexpressionLinkCount( coexpressions.getRawLinkCountForEE( eeVo.getId() ) );
                }

                // new ModelAndView(getSuccessView());

                // no genes are coexpressed
                // return error
                if ( coexpressedGenes.size() == 0 ) {
                    this.saveMessage( "No genes are coexpressed with the given stringency." );
                }

                Long numUsedExpressionExperiments = new Long( coexpressions.getNumberOfUsedExpressonExperiments() );
                Long numPositiveCoexpressedGenes = new Long( coexpressions.getPositiveStringencyLinkCount() );
                Long numNegativeCoexpressedGenes = new Long( coexpressions.getNegativeStringencyLinkCount() );
                Long numGenes = new Long( coexpressions.getNumGenes() );
                Long numPredictedGenes = new Long( coexpressions.getNumPredictedGenes() );
                Long numProbeAlignedRegions = new Long( coexpressions.getNumProbeAlignedRegions() );
                Long numStringencyGenes = new Long( coexpressions.getNumStringencyGenes() );
                Long numStringencyPredictedGenes = new Long( coexpressions.getNumStringencyPredictedGenes() );
                Long numStringencyProbeAlignedRegions = new Long( coexpressions.getNumStringencyProbeAlignedRegions() );
                Integer numMatchedLinks = coexpressions.getLinkCount();

                // addTimingInformation( request, coexpressions );
                job.updateProgress( "ending...." );
                ProgressManager.destroyProgressJob( job );

                // request.getParameterMap().remove( "searchString" );
                // request.setAttribute( "inner", "inner" );
                ModelAndView mav = new ModelAndView( getSuccessView() );
                // mav.setViewName( getSuccessView() );

                mav.addObject( "coexpressedGenes", coexpressedGenes );
                mav.addObject( "numPositiveCoexpressedGenes", numPositiveCoexpressedGenes );
                mav.addObject( "numNegativeCoexpressedGenes", numNegativeCoexpressedGenes );
                mav.addObject( "numSearchedExpressionExperiments", csc.getToUseEE().size() );
                mav.addObject( "numUsedExpressionExperiments", numUsedExpressionExperiments );

                mav.addObject( "numGenes", numGenes );
                mav.addObject( "numPredictedGenes", numPredictedGenes );
                mav.addObject( "numProbeAlignedRegions", numProbeAlignedRegions );

                mav.addObject( "numStringencyGenes", numStringencyGenes );
                mav.addObject( "numStringencyPredictedGenes", numStringencyPredictedGenes );
                mav.addObject( "numStringencyProbeAlignedRegions", numStringencyProbeAlignedRegions );
                // mav.addObject( "numSourceGeneGoTerms", numSourceGeneGoTerms );

                mav.addObject( "numMatchedLinks", numMatchedLinks );
                mav.addObject( "sourceGene", csc.getSourceGene() );
                mav.addObject( "expressionExperiments", eeVos );
                mav.addObject( "numLinkedExpressionExperiments", new Integer( eeVos.size() ) );

                // binding objects
                mav.addObject( "coexpressionSearchCommand", csc );
                populateTaxonReferenceData( mav.getModel() );
                mav.addAllObjects( errors.getModel() );

                Long elapsed = watch.getTime();
                watch.stop();
                log.info( "Processing after DAO call (elapsed time): " + elapsed );

                this.saveMessage( "Coexpression query took: " + coexpressions.getElapsedWallSeconds() );

                return mav;

            }

        };
    }

    class CoexpressionSearchCookie extends ConfigurationCookie {

        public CoexpressionSearchCookie( CoexpressionSearchCommand command ) {
            super( COOKIE_NAME );

            this.setProperty( "eeSearchString", command.getEeSearchString() );

            // save the gene name. If the gene id is on, then convert the ID to a gene first
            if ( !StringUtils.isBlank( command.getGeneIdSearch() ) ) {
                String geneId = command.getSearchString();
                Long id;
                try {
                    id = Long.parseLong( geneId );
                    Gene g = geneService.load( id );
                    this.setProperty( "searchString", g.getOfficialSymbol() );
                } catch ( NumberFormatException e ) {
                    this.setProperty( "searchString", command.getSearchString() );
                }
            } else {
                this.setProperty( "searchString", command.getSearchString() );
            }

            this.setProperty( "stringency", command.getStringency() );
            this.setProperty( "taxonScientificName", command.getTaxon().getScientificName() );

            this.setMaxAge( 100000 );
            this.setComment( "Information for coexpression search form" );
        }

    }

    /**
     * @author jsantos
     */
    class CoexpressionComparator implements Comparator {

        public int compare( Object o1, Object o2 ) {
            CoexpressionValueObject v1 = ( ( CoexpressionValueObject ) o1 );
            CoexpressionValueObject v2 = ( ( CoexpressionValueObject ) o2 );
            int o1Size = v1.getExpressionExperimentValueObjects().size();
            int o2Size = v2.getExpressionExperimentValueObjects().size();
            if ( o1Size > o2Size ) {
                return -1;
            } else if ( o1Size < o2Size ) {
                return 1;
            } else {
                return 0;
                // return v2.getGeneId().compareTo( v1.getGeneId() );
            }
        }
    }

    public void setGeneOntologyService( GeneOntologyService geneOntologyService ) {
        this.geneOntologyService = geneOntologyService;
    }
}