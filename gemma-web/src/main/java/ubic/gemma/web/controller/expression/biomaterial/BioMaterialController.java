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
package ubic.gemma.web.controller.expression.biomaterial;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.controller.BaseMultiActionController;
import ubic.gemma.web.util.EntityNotFoundException;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="bioMaterialController"
 * @spring.property name = "bioMaterialService" ref="bioMaterialService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="methodNameResolver" ref="bioMaterialActions"
 */
public class BioMaterialController extends BaseMultiActionController {

    private static Log log = LogFactory.getLog( BioMaterialController.class.getName() );

    private BioMaterialService bioMaterialService = null;

    private ExpressionExperimentService expressionExperimentService;

    private boolean AJAX = true;

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param bioMaterialService
     */
    public void setBioMaterialService( BioMaterialService bioMaterialService ) {
        this.bioMaterialService = bioMaterialService;
    }

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {

        log.debug( request.getParameter( "id" ) );

        Long id = Long.parseLong( request.getParameter( "id" ) );

        if ( id == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( "Must provide a biomaterial id" );
        }

        BioMaterial bioMaterial = bioMaterialService.load( id );
        if ( bioMaterial == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }

        this.saveMessage( request, "biomaterial with id " + id + " found" );
        request.setAttribute( "id", id );
        return new ModelAndView( "bioMaterial.detail" ).addObject( "bioMaterial", bioMaterial );
    }

    /**
     * @param request
     * @param response
     * @return
     */
    public ModelAndView annot( HttpServletRequest request, HttpServletResponse response ) {

        log.debug( request.getParameter( "eeid" ) );

        Long id = Long.parseLong( request.getParameter( "eeid" ) );

        if ( id == null ) {
            // should be a validation error, on 'submit'.
            throw new EntityNotFoundException( "Must provide an expression experiment id" );
        }

        Collection<BioMaterial> bioMaterials = getBioMaterialsForEE( id );

        ModelAndView mav = new ModelAndView( "bioMaterialAnnotator" );
        if ( AJAX ) {
            StringBuilder buf = new StringBuilder();
            for ( BioMaterial bm : bioMaterials ) {
                buf.append( bm.getId() );
                buf.append( "," );
            }
            mav.addObject( "bioMaterialIdList", buf.toString().replaceAll( ",$", "" ) );
        }

        Long numBioMaterials = new Long( bioMaterials.size() );
        mav.addObject( "numBioMaterials", numBioMaterials );
        mav.addObject( "bioMaterials", bioMaterials );
        return mav;
    }

    /**
     * @param id of experiment
     * @return
     */
    public Collection<BioMaterial> getBioMaterialsForEE( Long id ) {
        ExpressionExperiment expressionExperiment = expressionExperimentService.load( id );
        if ( expressionExperiment == null ) {
            throw new EntityNotFoundException( "Expression experiment with id=" + id + " not found" );
        }

        expressionExperimentService.thawLite( expressionExperiment );
        Collection<BioAssay> bioAssays = expressionExperiment.getBioAssays();
        Collection<BioMaterial> bioMaterials = new ArrayList<BioMaterial>();
        for ( BioAssay assay : bioAssays ) {
            Collection<BioMaterial> materials = assay.getSamplesUsed();
            if ( materials != null ) {
                bioMaterials.addAll( materials );
            }
        }
        return bioMaterials;
    }

    public Collection<BioMaterial> getBioMaterials( Collection<Long> ids ) {
        return bioMaterialService.load( ids );
    }

    /**
     * @param request
     * @param response
     * @return
     */
    @SuppressWarnings("unused")
    public ModelAndView showAll( HttpServletRequest request, HttpServletResponse response ) {
        return new ModelAndView( "bioMaterials" ).addObject( "bioMaterials", bioMaterialService.loadAll() );
    }

}
