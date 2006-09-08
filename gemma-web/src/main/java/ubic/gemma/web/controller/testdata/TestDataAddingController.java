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
package ubic.gemma.web.controller.testdata;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.testing.TestPersistentObjectHelper;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;

/**
 * Add test data to the system at the press of a button. .
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="testDataAddingController"
 * @spring.property name="persisterHelper" ref="persisterHelper"
 * @spring.property name="externalDatabaseService" ref="externalDatabaseService"
 * @spring.property name="formView" value="testDataInsert"
 * @spring.property name="successView" value="expressionExperiment.detail"
 */
public class TestDataAddingController extends SimpleFormController {

    private PersisterHelper persisterHelper;
    private ExternalDatabaseService externalDatabaseService;

    /**
     * @param externalDatabaseService the externalDatabaseService to set
     */
    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.AbstractController#handleRequestInternal(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    @Override
    @SuppressWarnings("unused")
    protected ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException error ) throws Exception {
        TestPersistentObjectHelper helper = new TestPersistentObjectHelper();
        helper.setPersisterHelper( this.persisterHelper );
        helper.setExternalDatabaseService( externalDatabaseService );

        // ProgressJob job = ProgressManager.createProgressJob( request.getRemoteUser(),
        // "Test data adding to the database" );
        ExpressionExperiment ee = helper.getTestExpressionExperimentWithAllDependencies();
        // ProgressManager.destroyProgressJob( job );

        Map<Object, Object> model = new HashMap<Object, Object>();
        model.put( "expressionExperiment", ee );
        return new ModelAndView( "expressionExperiment.detail", model );
    }

    /**
     * @param persisterHelper the persisterHelper to set
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
     */
    @Override
    @SuppressWarnings("unused")
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        return ExpressionExperiment.Factory.newInstance();
    }

}
