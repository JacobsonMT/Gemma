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
package ubic.gemma.web.controller.expression.designElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.analysis.sequence.ArrayDesignMapResultService;
import ubic.gemma.analysis.sequence.GeneMappingSummary;
import ubic.gemma.analysis.sequence.CompositeSequenceMapValueObject;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultService;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import ubic.gemma.web.controller.BaseController;
import ubic.gemma.web.propertyeditor.SequenceTypePropertyEditor;
import ubic.gemma.web.remote.EntityDelegator;

/**
 * @author joseph
 * @author paul
 * @version $Id$
 */
@Controller
@RequestMapping("/compositeSequence")
public class CompositeSequenceController extends BaseController {

    @Autowired
    private ArrayDesignMapResultService arrayDesignMapResultService = null;
    @Autowired
    private ArrayDesignService arrayDesignService = null;
    @Autowired
    private BlatResultService blatResultService = null;
    @Autowired
    private CompositeSequenceService compositeSequenceService = null;
    @Autowired
    private SearchService searchService;

    /**
     * Search for probes.
     * 
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/filter")
    public ModelAndView filter( HttpServletRequest request, HttpServletResponse response ) {
        String filter = request.getParameter( "filter" );
        String arid = request.getParameter( "arid" );

        ModelAndView mav = new ModelAndView( "compositeSequences.geneMap" );

        // Validate the filtering search criteria.
        if ( StringUtils.isBlank( filter ) ) {
            mav.getModel().put( "message", "No search critera provided" );
            // return showAll( request, response );
        } else {
            Collection<CompositeSequenceMapValueObject> compositeSequenceSummary = search( filter, arid );

            if ( ( compositeSequenceSummary == null ) || ( compositeSequenceSummary.size() == 0 ) ) {
                mav.getModel().put( "message", "Your search yielded no results" );
                compositeSequenceSummary = new ArrayList<CompositeSequenceMapValueObject>();
            } else {
                mav.getModel().put( "message", compositeSequenceSummary.size() + " probes matched your search." );
            }
            mav.addObject( "arrayDesign", loadArrayDesign( arid ) );
            mav.addObject( "sequenceData", compositeSequenceSummary );
            mav.addObject( "numCompositeSequences", compositeSequenceSummary.size() );
        }

        return mav;
    }

    /**
     * Exposed for AJAX calls (Probe browser)
     * 
     * @param ids
     * @return
     */
    public Collection<CompositeSequenceMapValueObject> getCsSummaries( Collection<Long> ids ) {

        if ( ids == null || ids.size() == 0 ) {
            return new HashSet<CompositeSequenceMapValueObject>();
        }

        Collection<CompositeSequence> compositeSequences = compositeSequenceService.loadMultiple( ids );
        Collection<Object[]> rawSummaries = compositeSequenceService.getRawSummary( compositeSequences, 0 );
        return arrayDesignMapResultService.getSummaryMapValueObjects( rawSummaries );
    }

    /**
     * Exposed for AJAX calls.
     * 
     * @param csd
     * @return
     */
    public Collection<GeneMappingSummary> getGeneMappingSummary( EntityDelegator csd ) {
        if ( csd == null || csd.getId() == null ) {
            return new HashSet<GeneMappingSummary>();
        }
        CompositeSequence cs = compositeSequenceService.load( csd.getId() );

        return this.getGeneMappingSummary( cs );
    }

    @InitBinder
    public void initBinder( WebDataBinder binder ) throws Exception {
        binder.registerCustomEditor( SequenceType.class, new SequenceTypePropertyEditor() );
    }

    /**
     * @param searchString
     * @param arrayDesign
     * @return
     */
    public Collection<CompositeSequenceMapValueObject> search( String searchString, String arrayDesignId ) {

        if ( StringUtils.isBlank( searchString ) ) {
            return new HashSet<CompositeSequenceMapValueObject>();
        }

        /*
         * There have to be a few ways of searching: - by ID, by bioSequence, by Gene name. An array design may or may
         * not be given.
         */
        ArrayDesign arrayDesign = loadArrayDesign( arrayDesignId );

        Map<Class<?>, List<SearchResult>> search = searchService.search( SearchSettings.compositeSequenceSearch(
                searchString, arrayDesign ) );

        Collection<CompositeSequence> css = new HashSet<CompositeSequence>();
        if ( search.containsKey( CompositeSequence.class ) ) {

            Collection<SearchResult> searchResults = search.get( CompositeSequence.class );

            for ( SearchResult sr : searchResults ) {
                CompositeSequence cs = ( CompositeSequence ) sr.getResultObject();
                if ( arrayDesign == null || cs.getArrayDesign().equals( arrayDesign ) ) {
                    css.add( cs );
                }
            }
        }

        return getSummaries( css );
    }

    /**
     * @param request
     * @param response
     * @return ModelAndView
     */
    @RequestMapping(value = "/show")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {
        Long id = Long.parseLong( request.getParameter( "id" ) );
        CompositeSequence cs = compositeSequenceService.load( id );
        if ( cs == null ) {
            addMessage( request, "object.notfound", new Object[] { "composite sequence " + id } );
        }

        ModelAndView mav = new ModelAndView( "compositeSequence.detail" );

        mav.addObject( "compositeSequence", cs );
        return mav;
    }

    /**
     * @param cs
     * @param blatResults
     */
    private void addBlatResultsLackingGenes( CompositeSequence cs, Map<BlatResult, GeneMappingSummary> blatResults ) {
        /*
         * Pick up blat results that didn't map to genes.
         */
        Collection<BlatResult> allBlatResultsForCs = blatResultService.findByBioSequence( cs
                .getBiologicalCharacteristic() );
        for ( BlatResult blatResult : allBlatResultsForCs ) {
            if ( !blatResults.containsKey( blatResult ) ) {
                GeneMappingSummary summary = new GeneMappingSummary();
                summary.setBlatResult( blatResult );
                // no gene...
                blatResults.put( blatResult, summary );
            }
        }
    }

    /**
     * @param cs
     * @return
     */
    private Collection<GeneMappingSummary> getGeneMappingSummary( CompositeSequence cs ) {
        BioSequence bs = cs.getBiologicalCharacteristic();

        Map<BlatResult, GeneMappingSummary> results = new HashMap<BlatResult, GeneMappingSummary>();
        if ( bs == null || bs.getBioSequence2GeneProduct() == null ) {
            return results.values();
        }

        Collection<BioSequence2GeneProduct> bs2gps = cs.getBiologicalCharacteristic().getBioSequence2GeneProduct();

        for ( BioSequence2GeneProduct bs2gp : bs2gps ) {
            GeneProduct geneProduct = bs2gp.getGeneProduct();
            Gene gene = geneProduct.getGene();
            BlatResult blatResult = null;

            if ( ( bs2gp instanceof BlatAssociation ) ) {
                BlatAssociation blatAssociation = ( BlatAssociation ) bs2gp;
                blatResult = blatAssociation.getBlatResult();
            } else if ( bs2gp instanceof AnnotationAssociation ) {
                /*
                 * Make a dummy blat result
                 */
                blatResult = BlatResult.Factory.newInstance();
                blatResult.setQuerySequence( bs );
                blatResult.setId( bs.getId() );
            }

            if ( blatResult == null ) {
                continue;
            }

            if ( results.containsKey( blatResult ) ) {
                results.get( blatResult ).addGene( geneProduct, gene );
            } else {
                GeneMappingSummary summary = new GeneMappingSummary();
                summary.addGene( geneProduct, gene );
                summary.setBlatResult( blatResult );
                summary.setCompositeSequence( cs );
                results.put( blatResult, summary );
            }

        }

        addBlatResultsLackingGenes( cs, results );

        if ( results.size() == 0 ) {
            // add a 'dummy' that at least contains the information about the CS. This is a bit of a hack...
            GeneMappingSummary summary = new GeneMappingSummary();
            summary.setCompositeSequence( cs );
            BlatResult newInstance = BlatResult.Factory.newInstance();
            newInstance.setQuerySequence( cs.getBiologicalCharacteristic() );
            newInstance.setId( -1L );
            summary.setBlatResult( newInstance );
            results.put( newInstance, summary );
        }

        return results.values();
    }

    /**
     * @param compositeSequences
     * @return
     */
    private Collection<CompositeSequenceMapValueObject> getSummaries( Collection<CompositeSequence> compositeSequences ) {
        Collection<CompositeSequenceMapValueObject> compositeSequenceSummary = new HashSet<CompositeSequenceMapValueObject>();
        if ( compositeSequences.size() > 0 ) {
            Collection<Object[]> rawSummaries = compositeSequenceService.getRawSummary( compositeSequences, 0 );
            compositeSequenceSummary = arrayDesignMapResultService.getSummaryMapValueObjects( rawSummaries );
        }
        return compositeSequenceSummary;
    }

    /**
     * @param arrayDesignId
     * @return
     */
    private ArrayDesign loadArrayDesign( String arrayDesignId ) {
        ArrayDesign arrayDesign = null;
        if ( arrayDesignId != null ) {
            try {
                arrayDesign = arrayDesignService.load( Long.parseLong( arrayDesignId ) );
            } catch ( NumberFormatException e ) {
                // Fail gracefully, please.
            }
        }
        return arrayDesign;
    }

}