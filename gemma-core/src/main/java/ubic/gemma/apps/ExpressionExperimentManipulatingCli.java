/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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
package ubic.gemma.apps;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Base class for CLIs that need an expression experiment as an input.
 * 
 * @author Paul
 * @version $Id$
 */
public abstract class ExpressionExperimentManipulatingCli extends AbstractSpringAwareCLI {

    ExpressionExperimentService expressionExperimentService;
    private String experimentShortName = null;

    @SuppressWarnings("static-access")
    protected void buildOptions() {
        Option expOption = OptionBuilder.hasArg().withArgName( "Expression experiment" ).withDescription(
                "Expression experiment short name" ).withLongOpt( "experiment" ).create( 'e' );

        addOption( expOption );

    }

    /**
     * @param short name of the experiment to find.
     * @return
     */
    protected ExpressionExperiment locateExpressionExperiment( String name ) {

        if ( name == null ) {
            throw new IllegalArgumentException( "Expression experiment name must be provided" );
        }

        ExpressionExperiment experiment = expressionExperimentService.findByShortName( name );

        if ( experiment == null ) {
            log.error( "No experiment " + name + " found" );
            bail( ErrorCode.INVALID_OPTION );
        }
        return experiment;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'e' ) ) {
            this.experimentShortName = this.getOptionValue( 'e' );
        }
        expressionExperimentService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
    }

    public String getExperimentShortName() {
        return experimentShortName;
    }

    public ExpressionExperimentService getExpressionExperimentService() {
        return expressionExperimentService;
    }

}
