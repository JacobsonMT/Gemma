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
package ubic.gemma.javaspaces.gigaspaces;

import java.util.Collection;

import org.acegisecurity.userdetails.UserDetailsService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.service.GeoDatasetService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.SecurityUtil;

/**
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentTaskImpl implements ExpressionExperimentTask {
    private Log log = LogFactory.getLog( this.getClass() );

    private long counter = 0;
    private ExpressionExperimentService expressionExperimentService = null;
    private GeoDatasetService geoDatasetService = null;
    private UserDetailsService userDetailsService = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.javaspaces.gigaspaces.ExpressionExperimentTask#execute(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    public Result execute( ExpressionExperiment expressionExperiment ) {

        ExpressionExperiment persistedExpressionExperiment = expressionExperimentService.create( expressionExperiment );
        Long id = persistedExpressionExperiment.getId();
        counter++;
        Result result = new Result();
        result.setTaskID( counter );
        result.setAnswer( id );

        return result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.javaspaces.gigaspaces.ExpressionExperimentTask#execute(java.lang.String, boolean, boolean)
     */
    public Result execute( String geoAccession, boolean loadPlatformOnly, boolean doSampleMatching ) {

        log.info( "executing task " + this.getClass().getName() );

        SecurityUtil.populateAuthenticationIfEmpty( userDetailsService, "administrator" );

        Collection datasets = geoDatasetService.fetchAndLoad( geoAccession, loadPlatformOnly, doSampleMatching );

        SecurityUtil.flushAuthentication();

        // TODO figure out what to store in the result for collections
        counter++;
        Result result = new Result();
        result.setAnswer( datasets.size() );
        result.setTaskID( counter );

        return result;
    }

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param geoDatasetService
     */
    public void setGeoDatasetService( GeoDatasetService geoDatasetService ) {
        this.geoDatasetService = geoDatasetService;
        this.geoDatasetService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
    }

    /**
     * @param userDetailsService
     */
    public void setUserDetailsService( UserDetailsService userDetailsService ) {
        this.userDetailsService = userDetailsService;
    }

}
