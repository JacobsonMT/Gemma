/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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

package ubic.gemma.analysis.expression.diff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.measurement.MeasurementType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * Tests of ANCOVA: using linear models with mixtures of fixed level and continuous parameters. See
 * test/data/stat-tests/README.txt for R code.
 * 
 * @author paul
 * @version $Id$
 */
public class AncovaTest extends BaseAnalyzerConfigurationTest {

    @Autowired
    private GenericAncovaAnalyzer analyzer;

    /**
     * Two fixed-level parameters
     * 
     * @throws Exception
     */
    @Test
    public void testAncovaTwoway() throws Exception {

        if ( !connected ) {
            log.warn( "Could not establish R connection.  Skipping test ..." );
            return;
        }

        configureMocks();

        DifferentialExpressionAnalysis expressionAnalysis = analyzer.run( expressionExperiment,
                super.experimentalFactors );

        assertNotNull( expressionAnalysis );

        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();

        assertEquals( 2, resultSets.size() );

        for ( ExpressionAnalysisResultSet resultSet : resultSets ) {

            Collection<ExperimentalFactor> factors = resultSet.getExperimentalFactors();

            assertEquals( 1, factors.size() );

            for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {

                ProbeAnalysisResult probeAnalysisResult = ( ProbeAnalysisResult ) r;
                CompositeSequence probe = probeAnalysisResult.getProbe();
                Double pvalue = probeAnalysisResult.getPvalue();
                Double stat = probeAnalysisResult.getEffectSize();

                if ( pvalue != null ) assertNotNull( stat );
                assertNotNull( probe );

                log.debug( "probe: " + probe + "; p-value: " + pvalue + "; T=" + stat );

                ExperimentalFactor f = factors.iterator().next();

                if ( f.equals( super.experimentalFactorA_Area ) ) {
                    if ( probe.getName().equals( "probe_98" ) ) {
                        assertEquals( 0.8572, pvalue, 0.001 );
                    } else if ( probe.getName().equals( "probe_10" ) ) {
                        assertEquals( 4.69e-11, pvalue, 1e-12 );
                    } else if ( probe.getName().equals( "probe_4" ) ) {
                        assertEquals( 0.0048, pvalue, 0.0001 );
                        assertEquals( -125.746, stat, 0.001 );
                    }

                } else {
                    if ( probe.getName().equals( "probe_98" ) ) {
                        assertEquals( 0.6417, pvalue, 0.001 );
                    } else if ( probe.getName().equals( "probe_10" ) ) {
                        assertEquals( 0.196, pvalue, 0.001 );
                    }
                }

            }
        }
    }

    /**
     * With a continuous covariate + two categorical
     * 
     * @throws Exception
     */
    @Test
    public void testAncovaCovariate() throws Exception {
        if ( !connected ) {
            log.warn( "Could not establish R connection.  Skipping test ..." );
            return;
        }

        configureMocks();

        /*
         * Add a continuous factor
         */
        ExperimentalFactor experimentalFactorC = ExperimentalFactor.Factory.newInstance();
        experimentalFactorC.setName( "confabulatiliationity" );
        experimentalFactorC.setId( 5399424551L );
        experimentalFactorC.setType( FactorType.CONTINUOUS );
        for ( int i = 1; i <= 8; i++ ) {

            FactorValue factorValueC = FactorValue.Factory.newInstance();
            factorValueC.setId( 2000L + i );

            factorValueC.setMeasurement( Measurement.Factory.newInstance( MeasurementType.ABSOLUTE, "" + i,
                    PrimitiveType.DOUBLE ) );

            factorValueC.setExperimentalFactor( experimentalFactorC );

            assert !biomaterials.get( i - 1 ).getFactorValues().contains( factorValueC );
            super.biomaterials.get( i - 1 ).getFactorValues().add( factorValueC );

            experimentalFactorC.getFactorValues().add( factorValueC );
        }
        expressionExperiment.getExperimentalDesign().getExperimentalFactors().add( experimentalFactorC );

        DifferentialExpressionAnalysis expressionAnalysis = analyzer.run( expressionExperiment, expressionExperiment
                .getExperimentalDesign().getExperimentalFactors() );

        assertNotNull( expressionAnalysis );

        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();

        assertEquals( 3, resultSets.size() );

        for ( ExpressionAnalysisResultSet resultSet : resultSets ) {

            Collection<ExperimentalFactor> factors = resultSet.getExperimentalFactors();

            assertEquals( 1, factors.size() );

            for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {

                ProbeAnalysisResult probeAnalysisResult = ( ProbeAnalysisResult ) r;
                CompositeSequence probe = probeAnalysisResult.getProbe();
                Double pvalue = probeAnalysisResult.getPvalue();
                Double stat = probeAnalysisResult.getEffectSize();

                if ( pvalue != null ) assertNotNull( stat );
                assertNotNull( probe );

                log.debug( "probe: " + probe + "; Factor="
                        + resultSet.getExperimentalFactors().iterator().next().getName() + "; p-value: " + pvalue
                        + "; T=" + stat );

                ExperimentalFactor f = factors.iterator().next();

                if ( f.equals( super.experimentalFactorA_Area ) ) {

                    if ( probe.getName().equals( "probe_98" ) ) {
                        assertEquals( 0.8673, pvalue, 0.001 );
                    } else if ( probe.getName().equals( "probe_10" ) ) {
                        assertEquals( 4.062e-09, pvalue, 1e-10 );
                    } else if ( probe.getName().equals( "probe_4" ) ) {
                        // too few samples
                        assertEquals( null, pvalue );
                        assertEquals( null, stat );
                    }

                } else if ( f.equals( super.experimentalFactorB ) ) {
                    if ( probe.getName().equals( "probe_98" ) ) {
                        assertEquals( 0.6665, pvalue, 0.001 );
                    } else if ( probe.getName().equals( "probe_10" ) ) {
                        assertEquals( 0.2356, pvalue, 0.001 );
                        assertEquals( 0.076, stat, 0.001 );
                    }
                } else {
                    // our new one.
                    if ( probe.getName().equals( "probe_98" ) ) {
                        assertEquals( 0.4977, pvalue, 0.001 );
                    } else if ( probe.getName().equals( "probe_10" ) ) {
                        assertEquals( 0.57347, pvalue, 0.001 );
                    }
                }

            }
        }
    }

    /**
     * Two fixed-level parameters, one of which has three levels
     * 
     * @throws Exception
     */
    @Test
    public void testAncovaTriLevel() throws Exception {

        if ( !connected ) {
            log.warn( "Could not establish R connection.  Skipping test ..." );
            return;
        }

        configureMocks();

        /*
         * Add a factor with three levels (same one used in onewayanovaanalyzertest)
         */
        ExperimentalFactor experimentalFactorC = ExperimentalFactor.Factory.newInstance();
        experimentalFactorC.setName( "threeLevFactor" );
        experimentalFactorC.setId( 5003L );
        experimentalFactorC.setType( FactorType.CATEGORICAL );
        expressionExperiment.getExperimentalDesign().getExperimentalFactors().add( experimentalFactorC );

        FactorValue fcbase = null;
        for ( int i = 1; i <= 3; i++ ) {
            FactorValue factorValueC = FactorValue.Factory.newInstance();
            factorValueC.setId( 2000L + i );
            if ( i == 3 ) {
                factorValueC.setValue( "control_group" );
                fcbase = factorValueC;
            } else {
                factorValueC.setValue( i + "_group" );
            }
            factorValueC.setExperimentalFactor( experimentalFactorC );
            experimentalFactorC.getFactorValues().add( factorValueC );
        }

        List<FactorValue> facV = new ArrayList<FactorValue>( experimentalFactorC.getFactorValues() );
        for ( int i = 0; i < 8; i++ ) {
            super.biomaterials.get( i ).getFactorValues().add( facV.get( i % 3 ) );
        }

        List<ExperimentalFactor> factors = new ArrayList<ExperimentalFactor>();

        factors.add( experimentalFactorA_Area );
        factors.add( experimentalFactorC );

        DifferentialExpressionAnalysis expressionAnalysis = analyzer.run( expressionExperiment, factors );

        assertNotNull( expressionAnalysis );

        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();

        assertEquals( 2, resultSets.size() );
        boolean found14 = false;
        boolean found198 = false;
        boolean found3 = false;
        for ( ExpressionAnalysisResultSet resultSet : resultSets ) {
            assertEquals( 1, resultSet.getExperimentalFactors().size() );

            log.info( resultSet.getBaselineGroup() );

            assertTrue( resultSet.getBaselineGroup().equals( factorValueA2 )
                    || resultSet.getBaselineGroup().equals( fcbase ) );

            ExperimentalFactor f = resultSet.getExperimentalFactors().iterator().next();

            for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {

                ProbeAnalysisResult probeAnalysisResult = ( ProbeAnalysisResult ) r;
                CompositeSequence probe = probeAnalysisResult.getProbe();
                Double pvalue = probeAnalysisResult.getPvalue();
                Double stat = probeAnalysisResult.getEffectSize();

                if ( pvalue != null ) assertNotNull( stat );
                assertNotNull( probe );

                log.debug( "probe: " + probe + "; p-value: " + pvalue + "; T=" + stat );

                if ( f.equals( super.experimentalFactorA_Area ) ) {
                    if ( probe.getName().equals( "probe_98" ) ) {
                        assertEquals( 0.8060, pvalue, 0.001 );
                        found198 = true;
                    } else if ( probe.getName().equals( "probe_10" ) ) {
                        assertEquals( 9.215e-09, pvalue, 1e-11 );
                        assertEquals( -152.812, stat, 0.001 );
                    } else if ( probe.getName().equals( "probe_4" ) ) {
                        assertEquals( 0.006278, pvalue, 0.0001 );
                        assertEquals( -95.118, stat, 0.001 );
                        found14 = true;
                    }

                } else { // factor C
                    if ( probe.getName().equals( "probe_98" ) ) {
                        assertEquals( 0.2171, pvalue, 0.001 );
                    } else if ( probe.getName().equals( "probe_10" ) ) {
                        assertEquals( 0.9088, pvalue, 0.001 );
                        assertEquals( -0.03028, stat, 0.001 );
                        found3 = true;
                    }
                }

            }
        }

        assertTrue( "Didn't find results", found14 && found198 && found3 );

    }

    /**
     * Two factors with interactions.
     * 
     * @throws Exception
     */
    @Test
    public void testAncovaWithInteraction() throws Exception {

        if ( !connected ) {
            log.warn( "Could not establish R connection.  Skipping test ..." );
            return;
        }

        configureMocks();

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.getFactorsToInclude().add( this.experimentalFactorA_Area );
        config.getFactorsToInclude().add( this.experimentalFactorB );
        config.getInteractionsToInclude().add( config.getFactorsToInclude() );

        DifferentialExpressionAnalysis expressionAnalysis = analyzer.run( expressionExperiment, config );

        assertNotNull( expressionAnalysis );

        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();

        assertEquals( 3, resultSets.size() );
        boolean foundInteractions = false;

        for ( ExpressionAnalysisResultSet resultSet : resultSets ) {

            Collection<ExperimentalFactor> factors = resultSet.getExperimentalFactors();

            for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {

                ProbeAnalysisResult probeAnalysisResult = ( ProbeAnalysisResult ) r;
                CompositeSequence probe = probeAnalysisResult.getProbe();
                Double pvalue = probeAnalysisResult.getPvalue();
                Double stat = probeAnalysisResult.getEffectSize();

                // if ( pvalue != null ) assertNotNull( stat );
                assertNotNull( probe );

                log.debug( "probe: " + probe + "; p-value: " + pvalue + "; T=" + stat );
                if ( factors.size() == 2 ) { // interaction
                    foundInteractions = true;

                    if ( probe.getName().equals( "probe_98" ) ) {
                        assertEquals( 0.7893, pvalue, 0.001 );
                    } else if ( probe.getName().equals( "probe_10" ) ) {
                        assertEquals( 0.04514, pvalue, 0.0001 );
                    } else if ( probe.getName().equals( "probe_4" ) ) {
                        assertEquals( null, pvalue );
                        assertEquals( null, stat );
                    }

                } else {

                    ExperimentalFactor f = factors.iterator().next();

                    assertNotNull( f );

                    if ( f.equals( super.experimentalFactorA_Area ) ) {
                        if ( probe.getName().equals( "probe_98" ) ) {
                            assertEquals( 0.8769, pvalue, 0.001 );
                        } else if ( probe.getName().equals( "probe_10" ) ) {
                            assertEquals( 5.158e-10, pvalue, 1e-12 );
                        } else if ( probe.getName().equals( "probe_4" ) ) {
                            assertEquals( 0.0048, pvalue, 0.0001 );
                            assertEquals( -125.746, stat, 0.001 );
                        }

                    } else {
                        if ( probe.getName().equals( "probe_98" ) ) {
                            assertEquals( 0.6888, pvalue, 0.001 );
                        } else if ( probe.getName().equals( "probe_10" ) ) {
                            assertEquals( 0.07970, pvalue, 0.001 );
                        }
                    }
                }

            }
            assertTrue( foundInteractions );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.analysis.expression.diff.BaseAnalyzerConfigurationTest#configureMocks()
     */
    @Override
    protected void configureMocks() throws Exception {

        configureMockAnalysisServiceHelper( 1 );

        analyzer.setExpressionDataMatrixService( expressionDataMatrixService );

    }

}
