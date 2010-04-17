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
package ubic.gemma.web.controller.expression.arrayDesign;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.web.controller.BaseFormController;

/**
 * Controller for editing basic information about array designs.
 * 
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignFormController extends BaseFormController {

    ArrayDesignService arrayDesignService = null;

    /**
     * Case = POST: Step 5 - Custom logic is here. For instance, this is where you would actually save or delete the
     * object.
     * 
     * @param request
     * @param response
     * @param command
     * @param errors
     * @return ModelAndView
     * @throws Exception
     */
    @Override
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {
        ArrayDesignValueObject ad = ( ArrayDesignValueObject ) command;

        ArrayDesign existing = arrayDesignService.load( ad.getId() );

        if ( existing == null ) {
            errors.addError( new ObjectError( command.toString(), null, null, "No such array design with id="
                    + ad.getId() ) );
            return processFormSubmission( request, response, command, errors );
        }

        // existing = arrayDesignService.thawLite( existing );
        existing.setDescription( ad.getDescription() );
        existing.setName( ad.getName() );
        existing.setShortName( ad.getShortName() );
        existing.setTechnologyType( ad.getTechnologyType() );

        arrayDesignService.update( existing );

        saveMessage( request, "object.updated", new Object[] {
                ad.getClass().getSimpleName().replaceFirst( "Impl", "" ), ad.getName() }, "Saved" );

        // go back to the aray we just edited.
        return new ModelAndView( new RedirectView( "/Gemma/arrays/showArrayDesign.html?id=" + ad.getId() ) );
    }

    /**
     * Case = POST: Step 5 - Used to process the form action (ie. clicking on the 'save' button or the 'cancel' button.
     * 
     * @param request
     * @param response
     * @param command
     * @param errors
     * @return ModelAndView
     * @throws Exception
     */
    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            Object command, BindException errors ) throws Exception {

        log.debug( "entering processFormSubmission" );

        return super.processFormSubmission( request, response, command, errors );
    }

    /**
     * @param arrayDesignService The arrayDesignService to set.
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * Case = GET: Step 1 - return instance of command class (from database). This is not called in the POST case
     * because the sessionForm is set to 'true' in the constructor. This means the command object was already bound to
     * the session in the GET case.
     * 
     * @param request
     * @return Object
     * @throws ServletException
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) {

        String idString = request.getParameter( "id" );

        Long id;
        ArrayDesignValueObject arrayDesign = null;

        // should be caught by validation.
        if ( idString != null ) {
            try {
                id = Long.parseLong( idString );
            } catch ( NumberFormatException e ) {
                throw new IllegalArgumentException();
            }
            Collection<Long> ids = new HashSet<Long>();
            ids.add( id );
            Collection<ArrayDesignValueObject> arrayDesigns = arrayDesignService.loadValueObjects( ids );
            if ( arrayDesigns.size() > 0 ) arrayDesign = arrayDesigns.iterator().next();

        }

        if ( arrayDesign == null ) {
            return new ArrayDesignValueObject();
        }
        return arrayDesign;
    }

    @Override
    protected ModelAndView getCancelView( HttpServletRequest request ) {
        // go back to the aray we just edited.
        return new ModelAndView( new RedirectView( "/Gemma/arrays/showArrayDesign.html?id="
                + request.getParameter( "id" ) ) );
    }

    @Override
    protected Map referenceData( HttpServletRequest request ) throws Exception {
        Map<String, List<? extends Object>> mapping = new HashMap<String, List<? extends Object>>();
        mapping.put( "technologyTypes", new ArrayList<String>( TechnologyType.literals() ) );
        return mapping;
    }

}
