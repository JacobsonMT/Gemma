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

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;

import ubic.basecode.gui.JMatrixDisplay;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.visualization.ExpressionDataMatrixVisualizationService;
import ubic.gemma.web.controller.BaseMultiActionController;
import ubic.gemma.web.view.TextView;

/**
 * This controller allows images to be created in a web environment. Specifically, the image is written to the
 * {@link OutputStream} as a Portable Network Graphic (PNG).
 * 
 * @spring.bean id="expressionExperimentVisualizationController"
 * @spring.property name="methodNameResolver" ref="expressionExperimentVisualizationActions"
 * @spring.property name = "expressionDataMatrixVisualizationService" ref="expressionDataMatrixVisualizationService"
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentVisualizationController extends BaseMultiActionController {
    private Log log = LogFactory.getLog( ExpressionExperimentVisualizationController.class );

    private static final String DEFAULT_CONTENT_TYPE = "image/png";

    private static final String HEAT_MAP_IMAGE_TYPE = "heatmap";

    private static final String TEXT_TYPE = "text";

    private ExpressionDataMatrixVisualizationService expressionDataMatrixVisualizationService = null;

    // private static final String EXPRESSION_PROFILE_IMAGE_TYPE = "profile";

    /**
     * @param request
     * @param response
     * @param errors
     * @return ModelAndView
     */
    @SuppressWarnings("unused")
    public ModelAndView show( HttpServletRequest request, HttpServletResponse response ) {

        String type = request.getParameter( "type" );

        String id = request.getParameter( "id" );

        if ( id == null ) {
            log.warn( "No id!" );
            return null;
        }

        ExpressionDataMatrix expressionDataMatrix = ( ExpressionDataMatrix ) request.getSession().getAttribute( id );

        if ( expressionDataMatrix == null ) {
            log.warn( "No matrix" );
            return null;
        }

        if ( type.equalsIgnoreCase( HEAT_MAP_IMAGE_TYPE ) ) {
            OutputStream out = null;
            try {
                out = response.getOutputStream();
                JMatrixDisplay display = expressionDataMatrixVisualizationService.createHeatMap( expressionDataMatrix );
                if ( display != null ) {
                    response.setContentType( DEFAULT_CONTENT_TYPE );
                    display.saveImageToPng( display.getColorMatrix(), out, true, true );
                }

            } catch ( Exception e ) {
                log.error( "Error is: " );
                e.printStackTrace();
            } finally {
                if ( out != null ) {
                    try {
                        out.close();
                        request.getSession().removeAttribute( id );
                    } catch ( IOException e ) {
                        log.warn( "Problems closing output stream.  Issues were: " + e.toString() );
                    }
                }
            }
            request.getSession().setAttribute( id, expressionDataMatrix );
            return null; // nothing to return;
        } else if ( type.equalsIgnoreCase( TEXT_TYPE ) ) {
            // return model and view with text
            ModelAndView mav = new ModelAndView( new TextView() );
            mav.addObject( "text", expressionDataMatrix.toString() );
            return mav;
        } else {
            log.warn( "Don't know how to view data as " + type );
            return null;
        }

    }

    /**
     * @param expressionDataMatrixVisualizationService The expressionDataMatrixVisualizationService to set.
     */
    public void setExpressionDataMatrixVisualizationService(
            ExpressionDataMatrixVisualizationService expressionDataMatrixVisualizationService ) {
        this.expressionDataMatrixVisualizationService = expressionDataMatrixVisualizationService;
    }
}
