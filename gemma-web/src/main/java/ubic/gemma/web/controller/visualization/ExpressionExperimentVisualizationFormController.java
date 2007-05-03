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
package ubic.gemma.web.controller.visualization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.analysis.service.CompositeSequenceGeneMapperService;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.ontology.CharacteristicUtils;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.propertyeditor.QuantitationTypePropertyEditor;
import ubic.gemma.web.util.ConfigurationCookie;

/**
 * A <link>SimpleFormController<link> providing search functionality of genes or design elements (probe sets). The
 * success view returns either a visual representation of the result set or a downloadable data file.
 * <p>
 * {@link viewSampling} sets whether or not just some randomly selected vectors will be shown, and {@link species} sets
 * the type of species to search. {@link keywords} restrict the search.
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="expressionExperimentVisualizationFormController"
 * @spring.property name = "commandName" value="expressionExperimentVisualizationCommand"
 * @spring.property name = "commandClass"
 *                  value="ubic.gemma.web.controller.visualization.ExpressionExperimentVisualizationCommand"
 * @spring.property name = "formView" value="expressionExperimentVisualizationForm"
 * @spring.property name = "successView" value="showExpressionExperimentVisualization"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "compositeSequenceService" ref="compositeSequenceService"
 * @spring.property name = "designElementDataVectorService" ref="designElementDataVectorService"
 * @spring.property name = "compositeSequenceGeneMapperService" ref="compositeSequenceGeneMapperService"
 * @spring.property name = "validator" ref="genericBeanValidator"
 */
public class ExpressionExperimentVisualizationFormController extends BaseFormController {

    private static Log log = LogFactory.getLog( ExpressionExperimentVisualizationFormController.class.getName() );

    public static final String SEARCH_BY_PROBE = "probe set id";
    public static final String SEARCH_BY_GENE = "gene symbol";
    private static final String COOKIE_NAME = "expressionExperimentVisualizationCookie";
    private static final int MAX_ELEMENTS_TO_VISUALIZE = 70;

    protected ExpressionExperimentService expressionExperimentService = null;
    protected CompositeSequenceService compositeSequenceService = null;
    protected DesignElementDataVectorService designElementDataVectorService;
    protected CompositeSequenceGeneMapperService compositeSequenceGeneMapperService = null;

    public ExpressionExperimentVisualizationFormController() {
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

        Long id = null;
        try {
            id = Long.parseLong( request.getParameter( "id" ) );
        } catch ( NumberFormatException e ) {
            throw new RuntimeException( "Id was not valid Long integer", e );
        }

        ExpressionExperiment ee = null;
        ExpressionExperimentVisualizationCommand eevc = new ExpressionExperimentVisualizationCommand();

        if ( id != null && StringUtils.isNotBlank( id.toString() ) ) {
            ee = expressionExperimentService.findById( id );
        } else {
            ee = ExpressionExperiment.Factory.newInstance();
        }

        eevc.setExpressionExperimentId( ee.getId() );
        eevc.setName( ee.getName() );

        if ( StringUtils.isBlank( request.getParameter( "searchString" ) ) ) {
            eevc = loadCookie( request, eevc );
        }

        return eevc;

    }

    /**
     * A cookie to store the user preferences.
     * 
     * @param request
     * @param eevc
     * @return ExpressionExperimentVisualizationCommand
     */
    private ExpressionExperimentVisualizationCommand loadCookie( HttpServletRequest request,
            ExpressionExperimentVisualizationCommand eevc ) {

        Collection<QuantitationType> quantitationTypes = getContinuousQuantitationTypes( request );

        /*
         * If we don't have any cookies, just return. We probably won't get this situation as we'll always have at least
         * one cookie (the one with the JSESSION ID).
         */
        if ( request == null || request.getCookies() == null ) {
            return null;
        }

        for ( Cookie cook : request.getCookies() ) {
            if ( cook.getName().equals( COOKIE_NAME ) ) {
                try {
                    ConfigurationCookie cookie = new ConfigurationCookie( cook );
                    eevc.setSearchString( cookie.getString( "searchString" ) );
                    eevc.setSearchCriteria( cookie.getString( "searchCriteria" ) );
                    eevc.setViewSampling( cookie.getBoolean( "viewSampling" ) );

                    /* determine which quantitation type was previously selected */
                    String qtName = cookie.getString( "quantitationTypeName" );
                    for ( QuantitationType qt : quantitationTypes ) {
                        if ( StringUtils.equals( qtName, qt.getName() ) ) {
                            eevc.setQuantitationType( qt );
                            return eevc;
                        }
                    }
                } catch ( Exception e ) {
                    log.warn( "Cookie could not be loaded: " + e.getMessage() );
                    // that's okay, we just don't get a cookie.
                }
            }
        }

        /* If we've come this far, we have a cookie but not one that matches COOKIE_NAME. Provide friendly defaults. */
        if ( quantitationTypes.size() > 0 ) {
            QuantitationType qt = quantitationTypes.iterator().next();
            eevc.setQuantitationType( qt );
        } else {
            throw new RuntimeException( "No continuous-valued quantitation types" );
        }

        eevc.setSearchString( "gene symbol 1, gene symbol 2" );

        eevc.setViewSampling( true );
        return eevc;
    }

    /**
     * 
     */
    @Override
    protected void initBinder( HttpServletRequest request, ServletRequestDataBinder binder ) {
        super.initBinder( request, binder );
        binder.registerCustomEditor( QuantitationType.class, new QuantitationTypePropertyEditor(
                getContinuousQuantitationTypes( request ) ) );
    }

    /**
     * Populates drop downs.
     * 
     * @param request
     * @return Map
     */
    @Override
    protected Map referenceData( HttpServletRequest request ) {

        Map<String, List<? extends Object>> searchByMap = new HashMap<String, List<? extends Object>>();
        List<String> searchCategories = new ArrayList<String>();
        searchCategories.add( SEARCH_BY_GENE );
        searchCategories.add( SEARCH_BY_PROBE );
        searchByMap.put( "searchCategories", searchCategories );

        Collection<QuantitationType> types = getContinuousQuantitationTypes( request );
        List<QuantitationType> listedTypes = new ArrayList<QuantitationType>();
        listedTypes.addAll( types );

        searchByMap.put( "quantitationTypes", listedTypes );

        return searchByMap;
    }

    /**
     * @param request
     * @param response
     * @param command
     * @param errors
     * @return ModelAndView
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {

        ExpressionExperimentVisualizationCommand eevc = ( ( ExpressionExperimentVisualizationCommand ) command );
        Long id = eevc.getExpressionExperimentId();

        if ( request.getParameter( "cancel" ) != null ) {
            log.info( "Cancelled" );

            if ( id != null ) {
                return new ModelAndView( new RedirectView(
                        "/Gemma/expressionExperiment/showExpressionExperiment.html?id=" + id ) );
            }

            log.warn( "Cannot find details view due to null id.  Redirecting to overview" );
            return new ModelAndView( new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html" ) );

        }

        return super.processFormSubmission( request, response, command, errors );
    }

    /**
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

        ExpressionExperimentVisualizationCommand eevc = ( ( ExpressionExperimentVisualizationCommand ) command );

        /* store user choices from command object in a cookie. */
        Cookie cookie = new ExpressionExperimentVisualizationCookie( eevc );
        response.addCookie( cookie );

        Long id = eevc.getExpressionExperimentId();

        ExpressionExperiment expressionExperiment = this.expressionExperimentService.findById( id );
        if ( expressionExperiment == null ) {
            return processErrors( request, response, command, errors, "No expression experiment with id " + id
                    + " found" );
        }

        QuantitationType quantitationType = eevc.getQuantitationType();
        if ( quantitationType == null ) {
            return processErrors( request, response, command, errors, "Quantitation type must be provided" );
        }

        Collection<DesignElementDataVector> dataVectors = getVectors( command, errors, eevc, expressionExperiment,
                quantitationType );

        if ( errors.hasErrors() ) {
            return processErrors( request, response, command, errors, null );
        }

        designElementDataVectorService.thaw( dataVectors );

        /*
         * Determine ordering we want to use.
         */
        if ( false ) {
            // need to thaw
            Collection<BioMaterial> bioMaterials = new HashSet<BioMaterial>();
            Map<FactorValue, Collection<BioMaterial>> fvMap = new HashMap<FactorValue, Collection<BioMaterial>>();
            Map<FactorValue, Integer> fvSizes = new HashMap<FactorValue, Integer>();
            Map<ExperimentalFactor, Integer> facNumVals = new HashMap<ExperimentalFactor, Integer>();
            for ( BioAssay assay : expressionExperiment.getBioAssays() ) {
                bioMaterials.addAll( assay.getSamplesUsed() );
            }
            for ( BioMaterial bm : bioMaterials ) {
                for ( FactorValue fv : bm.getFactorValues() ) {
                    ExperimentalFactor factor = fv.getExperimentalFactor();

                    if ( !facNumVals.containsKey( factor ) ) {
                        facNumVals.put( factor, factor.getFactorValues().size() );
                    }

                    if ( !fvMap.containsKey( fv ) ) {
                        fvMap.put( fv, new HashSet<BioMaterial>() );
                        fvSizes.put( fv, 0 );
                    }
                    fvMap.get( fv ).add( bm );
                    fvSizes.put( fv, fvSizes.get( fv ) + 1 );
                }
            }

            if ( fvMap.size() > 0 ) {
                // find the factor with the fewest values.

                // sort biomaterials by that; anybody who doesn't have a value for it gets put at the end
                
                // impose the ordering on the samples.

            }
        }

        ExpressionDataDoubleMatrix expressionDataMatrix = new ExpressionDataDoubleMatrix( dataVectors );

        /* deals with the case where probes don't match for the given quantitation type. */
        if ( expressionDataMatrix.rows() == 0 ) {
            String message = "None of the probe sets match the given quantitation type "
                    + quantitationType.getType().getValue();

            return processErrors( request, response, command, errors, message );
        }

        Map<CompositeSequence, Collection<Gene>> genes = getGenes( expressionDataMatrix ); // this will slow things
        // down.

        /* return the model and view */
        ModelAndView mav = new ModelAndView( getSuccessView() );
        mav.addObject( "expressionDataMatrix", expressionDataMatrix );
        mav.addObject( "genes", genes );
        mav.addObject( "expressionExperiment", expressionExperiment );
        mav.addObject( "quantitationType", eevc.getQuantitationType() );
        mav.addObject( "searchCriteria", eevc.getSearchCriteria() );
        mav.addObject( "searchString", eevc.getSearchString() );
        mav.addObject( "viewSampling", new Boolean( eevc.isViewSampling() ) );
        return mav;
    }

    @SuppressWarnings("unchecked")
    private Map<CompositeSequence, Collection<Gene>> getGenes( ExpressionDataDoubleMatrix expressionDataMatrix ) {
        Collection<CompositeSequence> css = new HashSet<CompositeSequence>();
        for ( ExpressionDataMatrixRowElement el : expressionDataMatrix.getRowElements() ) {
            CompositeSequence cs = ( CompositeSequence ) el.getDesignElement();
            css.add( cs );
        }
        return compositeSequenceService.getGenes( css );

    }

    /**
     * @param command
     * @param errors
     * @param eevc
     * @param expressionExperiment
     * @param quantitationType
     * @return Collection<DesignElementDataVector>
     */
    @SuppressWarnings("unchecked")
    protected Collection<DesignElementDataVector> getVectors( Object command, BindException errors,
            ExpressionExperimentVisualizationCommand eevc, ExpressionExperiment expressionExperiment,
            QuantitationType quantitationType ) {

        Collection<DesignElementDataVector> vectors = null;

        Collection<CompositeSequence> compositeSequences = null;

        boolean viewSampling = ( ( ExpressionExperimentVisualizationCommand ) command ).isViewSampling();

        Collection<ArrayDesign> arrayDesigns = expressionExperimentService.getArrayDesignsUsed( expressionExperiment );

        /* check size if 'viewSampling' is set. */
        if ( viewSampling ) {
            vectors = expressionExperimentService.getSamplingOfVectors( quantitationType, MAX_ELEMENTS_TO_VISUALIZE );
        } else {
            String searchString = eevc.getSearchString();

            String[] searchIds = StringUtils.split( searchString, "," );
            if ( searchIds.length > MAX_ELEMENTS_TO_VISUALIZE ) {
                String message = "Max elements to search for is " + MAX_ELEMENTS_TO_VISUALIZE;
                log.error( message );
                errors.addError( new ObjectError( command.toString(), null, null, message ) );
                return null;
            }

            List<String> searchIdsAsList = Arrays.asList( searchIds );

            /* handle search by design element */
            if ( eevc.getSearchCriteria().equalsIgnoreCase( SEARCH_BY_PROBE ) ) {

                if ( arrayDesigns.size() == 0 ) {
                    String message = "No array designs found for " + expressionExperiment;
                    log.error( message );
                    errors.addError( new ObjectError( command.toString(), null, null, message ) );
                    return null;
                }

                compositeSequences = compositeSequenceService.findByNamesInArrayDesigns( searchIdsAsList, arrayDesigns );

            } else if ( eevc.getSearchCriteria().equalsIgnoreCase( SEARCH_BY_GENE ) ) {
                /* search by gene */
                if ( arrayDesigns.size() == 0 ) {
                    String message = "No array designs found for " + expressionExperiment;
                    log.error( message );
                    errors.addError( new ObjectError( command.toString(), null, null, message ) );
                    return null;
                }

                Map<Gene, Collection<CompositeSequence>> compositeSequencesForGene = compositeSequenceGeneMapperService
                        .getCompositeSequencesForGenesByOfficialSymbols( searchIdsAsList, arrayDesigns );

                Collection<Gene> geneKeySet = compositeSequencesForGene.keySet();

                for ( Gene g : geneKeySet ) {
                    compositeSequences = compositeSequencesForGene.get( g );
                    log.debug( "gene official symbol: " + g.getOfficialSymbol() + " has " + compositeSequences.size()
                            + " composite sequences associated with it." );
                }
            }

            if ( compositeSequences == null || compositeSequences.size() == 0 ) {
                String message = "Genes/Probes could not be found.";
                log.error( message );
                errors.addError( new ObjectError( command.toString(), null, null, message ) );
                return null;
            }

            vectors = expressionExperimentService.getDesignElementDataVectors( compositeSequences, quantitationType );
        }
        if ( vectors == null || vectors.size() == 0 ) {
            errors.addError( new ObjectError( command.toString(), null, null, "No data could be found." ) );
        }
        return vectors;
    }

    /**
     * @param request
     * @return Collection<QuantitationType>
     */
    @SuppressWarnings("unchecked")
    private Collection<QuantitationType> getContinuousQuantitationTypes( HttpServletRequest request ) {
        Long id = null;
        try {
            id = Long.parseLong( request.getParameter( "id" ) );
        } catch ( NumberFormatException e ) {
            throw new RuntimeException( "Id was not valid Long integer", e );
        }
        ExpressionExperiment expressionExperiment = this.expressionExperimentService.findById( id );
        Collection<QuantitationType> types = expressionExperimentService.getQuantitationTypes( expressionExperiment );
        Iterator iter = types.iterator();
        while ( iter.hasNext() ) {
            QuantitationType type = ( QuantitationType ) iter.next();
            if ( !type.getGeneralType().equals( GeneralType.QUANTITATIVE ) ) {
                iter.remove();
            }
        }
        return types;
    }

    /**
     * @param compositeSequenceService
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

    /**
     * @param designElementDataVectorService
     */
    public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
        this.designElementDataVectorService = designElementDataVectorService;
    }

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param compositeSequenceGeneMapperService The compositeSequenceGeneMapperService to set.
     */
    public void setCompositeSequenceGeneMapperService(
            CompositeSequenceGeneMapperService compositeSequenceGeneMapperService ) {
        this.compositeSequenceGeneMapperService = compositeSequenceGeneMapperService;
    }

    /**
     * @author keshav
     */
    class ExpressionExperimentVisualizationCookie extends ConfigurationCookie {

        public ExpressionExperimentVisualizationCookie( ExpressionExperimentVisualizationCommand command ) {

            super( COOKIE_NAME );

            log.debug( "creating cookie" );

            this.setProperty( "searchString", command.getSearchString() );
            this.setProperty( "viewSampling", command.isViewSampling() );
            this.setProperty( "searchCriteria", command.getSearchCriteria() );
            this.setProperty( "quantitationTypeName", command.getQuantitationType().getName() );

            /* set cookie to expire after 2 days. */
            this.setMaxAge( 172800 );
            this.setComment( "User selections for visualization form" );
        }

    }
}

class FactorValueComparator implements Comparator<FactorValue> {

    public int compare( FactorValue arg0, FactorValue arg1 ) {
        if ( arg0.getMeasurement() != null && arg1.getMeasurement() != null ) {
            return ( new MeasurementComparator() ).compare( arg0.getMeasurement(), arg1.getMeasurement() );
        } else if ( arg0.getOntologyEntry() != null && arg1.getOntologyEntry() != null ) {
            return CharacteristicUtils.compare( arg0.getOntologyEntry(), arg1.getOntologyEntry() );
        } else if ( arg0.getValue() != null && arg1.getValue() != null ) {
            return arg0.getValue().compareTo( arg1.getValue() );
        } else {
            return arg0.getId().compareTo( arg1.getId() ); // fallback.
        }
    }
}

class MeasurementComparator implements Comparator<Measurement> {

    public int compare( Measurement o1, Measurement o2 ) {
        PrimitiveType ptype = o1.getRepresentation();
        if ( ptype.equals( PrimitiveType.STRING ) || ptype.equals( PrimitiveType.BOOLEAN ) ) {
            return o1.getValue().compareTo( o2.getValue() );
        } else if ( ptype.equals( PrimitiveType.DOUBLE ) ) {
            Double d1 = Double.parseDouble( o1.getValue() );
            Double d2 = Double.parseDouble( o2.getValue() );
            return d1.compareTo( d2 );
        } else if ( ptype.equals( PrimitiveType.INT ) ) {
            Integer d1 = Integer.parseInt( o1.getValue() );
            Integer d2 = Integer.parseInt( o2.getValue() );
            return d1.compareTo( d2 );
        } else {
            throw new UnsupportedOperationException( "Don't know how to compare " + ptype + "'s" );
        }
    }
}