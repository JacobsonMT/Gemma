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
package ubic.gemma.core.analysis.expression.diff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;

/**
 * Tests the two way anova analyzer. See test/data/stat-tests/README.txt for R code.
 * 
 * @author keshav
 * @version $Id$
 */
public class TwoWayAnovaWithoutInteractionsAnalyzerTest extends BaseAnalyzerConfigurationTest {

    @Autowired
    DiffExAnalyzer analyzer = null;

    /**
     * Tests the TwoWayAnova method.
     */
    @Test
    public void testTwoWayAnova() throws Exception {

        log.debug( "Testing getPValues method in " + DiffExAnalyzer.class.getName() );

        if ( !connected ) {
            log.warn( "Could not establish R connection.  Skipping test ..." );
            return;
        }

        configureMocks();

        List<ExperimentalFactor> factors = Arrays.asList( new ExperimentalFactor[] { experimentalFactorA_Area,
                experimentalFactorB } );
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.setFactorsToInclude( factors );
        config.setQvalueThreshold( null );

        Collection<DifferentialExpressionAnalysis> expressionAnalyses = analyzer.run( expressionExperiment, config );
        DifferentialExpressionAnalysis expressionAnalysis = expressionAnalyses.iterator().next();
        Collection<ExpressionAnalysisResultSet> resultSets = expressionAnalysis.getResultSets();

        assertEquals( 2, resultSets.size() );

        for ( ExpressionAnalysisResultSet resultSet : resultSets ) {
            checkResults( resultSet );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.analysis.diff.BaseAnalyzerConfigurationTest#configureMocks()
     */
    @Override
    protected void configureMocks() throws Exception {

        configureMockAnalysisServiceHelper( 1 );

        analyzer.setExpressionDataMatrixService( expressionDataMatrixService );

    }

    /**
     * @param resultSet
     */
    private void checkResults( ExpressionAnalysisResultSet resultSet ) {

        Collection<ExperimentalFactor> factors = resultSet.getExperimentalFactors();
        assertEquals( "Should not have an interaction term", 1, factors.size() );

        ExperimentalFactor f = factors.iterator().next();

        boolean found = false;
        for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {

            DifferentialExpressionAnalysisResult probeAnalysisResult = r;
            CompositeSequence probe = probeAnalysisResult.getProbe();
            Double pvalue = probeAnalysisResult.getPvalue();
            if ( f.equals( super.experimentalFactorB ) && probe.getName().equals( "probe_1" ) ) {
                assertEquals( 0.501040, pvalue, 0.001 );
                found = true;
            }
            Collection<ContrastResult> contrasts = probeAnalysisResult.getContrasts();
            Double stat = null;
            if ( contrasts.isEmpty() ) {
                continue;
            }

            stat = contrasts.iterator().next().getTstat();

            assertNotNull( probe );

            // log.debug( "probe: " + probe + "; p-value: " + pvalue + "; F=" + stat );

            if ( f.equals( super.experimentalFactorA_Area ) ) {

                assertEquals( factorValueA2, resultSet.getBaselineGroup() );

                if ( probe.getName().equals( "probe_1" ) ) { // id=1001
                    assertEquals( 0.001814, pvalue, 0.00001 );
                    assertNotNull( stat );
                    assertEquals( -287.061, stat, 0.001 );
                    found = true;
                } else if ( probe.getName().equals( "probe_97" ) ) { // id 1097
                    assertEquals( 0.3546, pvalue, 0.001 );
                } else if ( probe.getName().equals( "probe_0" ) ) {
                    assertEquals( 1.36e-12, pvalue, 1e-10 );
                    assertNotNull( stat );
                    assertEquals( -425.3, stat, 0.1 );
                }

            } else {

                assertEquals( factorValueB2, resultSet.getBaselineGroup() );

                if ( probe.getName().equals( "probe_97" ) ) {
                    assertEquals( 0.4449, pvalue, 0.001 );
                }

            }

        }
        assertTrue( "Did not find expected results for probe_1", found );
    }

}
