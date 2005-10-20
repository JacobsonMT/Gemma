/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.web.flow.expression.arrayDesign;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.DataBinder;
import org.springframework.webflow.Event;
import org.springframework.webflow.RequestContext;
import org.springframework.webflow.ScopeType;

import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignImpl;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;
import edu.columbia.gemma.web.flow.AbstractFlowFormAction;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @spring.bean name="arrayDesign.Edit.formAction"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignFormEditAction extends AbstractFlowFormAction {
    protected final transient Log log = LogFactory.getLog( getClass() );
    private ArrayDesignService arrayDesignService;
    private ArrayDesign ad = null;

    /**
     * 
     */
    public ArrayDesignFormEditAction() {
        setFormObjectName( "arrayDesign" );
        setFormObjectClass( ArrayDesignImpl.class );
        setFormObjectScope( ScopeType.FLOW );
    }

    /**
     * flowScope - attributes in the flowScope are available for the duration of the flow requestScope - attributes in
     * the requestScope are available for the duration of the request sourceEvent - this is the event that originated
     * the request. SourceEvent contains parameters provided as input by the client.
     * 
     * @param context
     */
    @Override
    public Object createFormObject( RequestContext context ) {

        // todo: can't we just pass the arraydesign into here.
        String name = ( String ) context.getSourceEvent().getAttribute( "name" );
        context.getFlowScope().setAttribute( "name", name );

        ad = getArrayDesignService().findArrayDesignByName( name );

        if ( ad != null ) context.getRequestScope().setAttribute( "arrayDesign", ad );

        return ad;
    }

    /**
     * This is the webflow equivalent of mvc's formBackingObject
     * 
     * @param context
     * @param binder
     */
    @Override
    @SuppressWarnings( { "unused" })
    protected void initBinder( RequestContext context, DataBinder binder ) {

        this.setBindOnSetupForm( true );

        log.info( new Boolean( this.isBindOnSetupForm() ) );

    }

    /**
     * @param context
     * @return
     * @throws Exception
     */
    public Event save( RequestContext context ) throws Exception {

        ad.setName( ( String ) context.getFlowScope().getAttribute( "name", String.class ) );
        ad.setDescription( ( String ) context.getSourceEvent().getAttribute( "description" ) );

        // TODO: need to use contact service to find or create this before setting it.
        // ad.setDesignProvider( ( Contact ) context.getSourceEvent().getAttribute( "provider" ) );

        log.info( "updating ArrayDesign " + ad.getName() );

        getArrayDesignService().updateArrayDesign( ad );

        addMessage( context, "arrayDesign.update", null );
        return success();
    }

    /**
     * @param arrayDesignService The arrayDesignService to set.
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @return Returns the arrayDesignService.
     */
    public ArrayDesignService getArrayDesignService() {
        return arrayDesignService;
    }
}