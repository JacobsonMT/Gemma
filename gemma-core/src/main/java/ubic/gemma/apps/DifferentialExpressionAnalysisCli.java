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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService.AnalysisType;
import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * A command line interface to the {@link DifferentialExpressionAnalysis}.
 * 
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisCli extends ExpressionExperimentManipulatingCLI {

    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService = null;

    private AnalysisType type = null;

    private List<Long> factorIds = new ArrayList<Long>();

    private List<String> factorNames = new ArrayList<String>();

    /**
     * @param args
     */
    public static void main( String[] args ) {
        DifferentialExpressionAnalysisCli analysisCli = new DifferentialExpressionAnalysisCli();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = analysisCli.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( "Elapsed time: " + watch.getTime() / 1000 + " seconds" );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "Differential Expression Analysis", args );
        if ( err != null ) {
            return err;
        }

        this.differentialExpressionAnalyzerService = ( DifferentialExpressionAnalyzerService ) this
                .getBean( "differentialExpressionAnalyzerService" );

        // this.expressionExperimentReportService = ( ExpressionExperimentReportService ) this
        // .getBean( "expressionExperimentReportService" );

        for ( BioAssaySet ee : expressionExperiments ) {
            if ( !( ee instanceof ExpressionExperiment ) ) {
                continue;
            }
            processExperiment( ( ExpressionExperiment ) ee );
        }

        summarizeProcessing();

        return null;
    }

    /**
     * @param ee
     */
    private void processExperiment( ExpressionExperiment ee ) {

        Collection<ExperimentalFactor> factors = new HashSet<ExperimentalFactor>();

        ExperimentalFactorService efs = ( ExperimentalFactorService ) this.getBean( "experimentalFactorService" );

        try {
            DifferentialExpressionAnalysis results = null;
            if ( this.factorNames.size() > 0 ) {
                if ( this.factorIds.size() > 0 ) {
                    throw new IllegalArgumentException( "Please provide factor names or ids, not a mixture of each" );
                }
                Collection<ExperimentalFactor> experimentalFactors = ee.getExperimentalDesign()
                        .getExperimentalFactors();
                for ( ExperimentalFactor experimentalFactor : experimentalFactors ) {
                    if ( factorNames.contains( experimentalFactor.getName() ) ) {
                        factors.add( experimentalFactor );
                    }
                }

                if ( factors.size() != factorNames.size() ) {
                    throw new IllegalArgumentException( "Didn't find factors for all the provided factor names" );
                }

            } else if ( this.factorIds.size() > 0 ) {
                for ( Long factorId : factorIds ) {
                    if ( this.factorNames.size() > 0 ) {
                        throw new IllegalArgumentException( "Please provide factor names or ids, not a mixture of each" );
                    }
                    ExperimentalFactor factor = efs.load( factorId );
                    if ( factor == null ) {
                        throw new IllegalArgumentException( "No factor for id=" + factorId );
                    }
                    if ( !factor.getExperimentalDesign().equals( ee.getExperimentalDesign() ) ) {
                        throw new IllegalArgumentException( "Factor with id=" + factorId + " does not belong to " + ee );
                    }
                    factors.add( factor );
                }
            }

            if ( factors.size() > 0 ) {
                if ( this.type != null ) {
                    results = this.differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ee,
                            factors, type );
                } else {
                    results = this.differentialExpressionAnalyzerService
                            .runDifferentialExpressionAnalyses( ee, factors );
                }
            } else {
                results = this.differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ee );
            }

            if ( results == null ) {
                throw new Exception( "Failed to process differential expression for experiment " + ee.getShortName() );
            }

            if ( log.isDebugEnabled() ) logProcessing( results );

            successObjects.add( ee.toString() );

        } catch ( Exception e ) {
            log.error( "Error while processing " + e + ": " + e.getMessage() );
            log.error( e, e );
            errorObjects.add( ee + ": " + e.getMessage() );
        }
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.apps.AbstractGeneExpressionExperimentManipulatingCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {

        /*
         * These options from the super class support: running on one or more data sets from the command line, running
         * on list of data sets from a file, running on all data sets.
         */
        super.buildOptions();

        /* Supports: running on all data sets that have not been run since a given date. */
        super.addDateOption();

        Option topOpt = OptionBuilder.withLongOpt( "top" ).hasArg( true ).withDescription(
                "The top (most significant) results to display." ).create();
        super.addOption( topOpt );

        // Option forceAnalysisOpt = OptionBuilder.hasArg( false ).withDescription( "Force the run." ).create( 'r' );
        // super.addOption( forceAnalysisOpt );

        Option factors = OptionBuilder.hasArg().withDescription(
                "ID numbers or names of the factor(s) to use, comma-delimited" ).create( "factors" );

        super.addOption( factors );

        Option analysisType = OptionBuilder
                .hasArg()
                .withDescription(
                        "Type of analysis to perform. If omitted, the system will try to guess based on the experimental design. "
                                + "Choices are : TWA (two-way anova), TWIA (two-way ANOVA with interactions), OWA (one-way ANOVA), TTEST" )
                .create( "type" );

        super.addOption( analysisType );

    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.apps.AbstractGeneExpressionExperimentManipulatingCLI#processOptions()
     */
    @Override
    protected void processOptions() {
        super.processOptions();

        if ( hasOption( "type" ) ) {
            if ( !hasOption( "factors" ) ) {
                throw new IllegalArgumentException( "Please specify the factor(s) when specifying the analysis type." );
            }
            this.type = AnalysisType.valueOf( getOptionValue( "type" ) );
        }

        if ( hasOption( "factors" ) ) {
            String rawfactors = getOptionValue( "factors" );
            String[] factorIDst = StringUtils.split( rawfactors, "," );
            if ( factorIDst != null && factorIDst.length > 0 ) {
                for ( String string : factorIDst ) {
                    try {
                        Long factorId = Long.parseLong( string );
                        this.factorIds.add( factorId );
                    } catch ( NumberFormatException e ) {
                        this.factorNames.add( string );
                    }
                }
            }
        }
    }

    /**
     * @param expressionAnalysis
     */
    private void logProcessing( DifferentialExpressionAnalysis expressionAnalysis ) {

        log.debug( "Summarizing results for expression analysis of type: " + expressionAnalysis.getName() );
        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();

        log.debug( resultSets.size() + " result set(s) to process." );
        for ( ExpressionAnalysisResultSet resultSet : resultSets ) {
            log.debug( "*** Result set ***" );
            Collection<DifferentialExpressionAnalysisResult> results = resultSet.getResults();

            for ( DifferentialExpressionAnalysisResult result : results ) {
                ProbeAnalysisResult probeResult = ( ProbeAnalysisResult ) result;
                log.debug( "probe: " + probeResult.getProbe().getName() + ", p-value: " + probeResult.getPvalue()
                        + ", score: " + probeResult.getScore() );
            }
            log.debug( "Result set processed with " + results.size() + " results." );
        }
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.util.AbstractSpringAwareCLI#getShortDesc()
     */
    @Override
    public String getShortDesc() {
        return "Analyze expression data sets for differentially expressed genes.";
    }

}
