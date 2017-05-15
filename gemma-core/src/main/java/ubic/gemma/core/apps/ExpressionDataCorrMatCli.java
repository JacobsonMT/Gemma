/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.core.apps;

import ubic.gemma.core.analysis.preprocess.SampleCoexpressionMatrixService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Create correlation visualizations for expression experiments
 * 
 * @author paul
 * @version $Id$
 */
public class ExpressionDataCorrMatCli extends ExpressionExperimentManipulatingCLI {

    public static void main( String[] args ) {
        try {
            ExpressionDataCorrMatCli e = new ExpressionDataCorrMatCli();
            Exception ex = e.doWork( args );
            if ( ex != null ) log.info( ex, ex );
        } catch ( Exception e ) {
            log.info( e, e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.AbstractCLI#getCommandName()
     */
    @Override
    public String getCommandName() {
        return "corrMat";
    }

    @Override
    public String getShortDesc() {
        return "Create or update sample correlation matrices for expression experiments";
    }

    @Override
    protected void buildOptions() {
        super.buildOptions();
        super.addForceOption();
    }

    @Override
    protected Exception doWork( String[] args ) {
        this.processCommandLine( args );

        for ( BioAssaySet ee : expressionExperiments ) {
            try {
                if ( !( ee instanceof ExpressionExperiment ) ) {
                    errorObjects.add( ee );
                    continue;
                }
                processExperiment( ( ExpressionExperiment ) ee );
                successObjects.add( ee );
            } catch ( Exception e ) {
                log.error( "Error while processing " + ee, e );
                errorObjects.add( ee );
            }

        }
        summarizeProcessing();
        return null;
    }

    /**
     * @param arrayDesign
     */
    private void audit( ExpressionExperiment ee, AuditEventType eventType ) {
        auditTrailService.addUpdateEvent( ee, eventType, "Generated sample correlation matrix" );
        successObjects.add( ee.toString() );
    }

    /**
     * @param ee
     */
    private void processExperiment( ExpressionExperiment ee ) {
        if ( !force && !needToRun( ee, null ) ) {
            return;
        }

        SampleCoexpressionMatrixService sampleCoexpressionMatrixService = this
                .getBean( SampleCoexpressionMatrixService.class );

        sampleCoexpressionMatrixService.create( ee, this.force );
        audit( ee, null );

    }

}
