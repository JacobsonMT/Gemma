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
package ubic.gemma.apps;

import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.basecode.util.FileTools;
import ubic.gemma.core.datastructure.matrix.ExperimentalDesignWriter;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Writes out the experimental design for a given experiment. This can be directly read into R.
 * 
 * @author keshav
 * @version $Id$
 */
public class ExperimentalDesignWriterCLI extends ExpressionExperimentManipulatingCLI {

    /**
     * @param args
     */
    public static void main( String[] args ) {
        ExperimentalDesignWriterCLI cli = new ExperimentalDesignWriterCLI();
        Exception exc = cli.doWork( args );
        if ( exc != null ) {
            log.error( exc.getMessage() );
        }
    }

    private String outFileName;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.AbstractCLI#getCommandName()
     */
    @Override
    public String getCommandName() {
       return "printExperimentalDesign"; 
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.AbstractSpringAwareCLI#getShortDesc()
     */
    @Override
    public String getShortDesc() {
        return "Prints experimental design to a file in a R-friendly format";
    }

    /*
     * (non-Javadoc)
     * 
     * @see ExpressionExperimentManipulatingCLI#buildOptions()
     */
    @Override
    @SuppressWarnings("static-access")
    protected void buildOptions() {
        super.buildOptions();
        Option outputFileOption = OptionBuilder.hasArg().isRequired().withArgName( "outFilePrefix" )
                .withDescription( "File prefix for saving the output (short name will be appended)" )
                .withLongOpt( "outFilePrefix" ).create( 'o' );
        addOption( outputFileOption );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        processCommandLine( args );

        for ( BioAssaySet ee : expressionExperiments ) {

            if ( ee instanceof ExpressionExperiment ) {
                ExperimentalDesignWriter edWriter = new ExperimentalDesignWriter();

                try (PrintWriter writer = new PrintWriter( outFileName + "_"
                        + FileTools.cleanForFileName( ( ( ExpressionExperiment ) ee ).getShortName() ) + ".txt" );) {

                    edWriter.write( writer, ( ExpressionExperiment ) ee, true, true );
                    writer.flush();
                    writer.close();
                } catch ( IOException e ) {
                    return e;
                }
            } else {
                throw new UnsupportedOperationException( "Can't handle non-EE BioAssaySets yet" );
            }
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ExpressionExperimentManipulatingCLI#processOptions()
     */
    @Override
    protected void processOptions() {
        super.processOptions();
        outFileName = getOptionValue( 'o' );
    }
}
