/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.analysis.expression.diff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.util.FileTools;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerServiceImpl.AnalysisType;
import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.loader.expression.simple.ExperimentalDesignImporter;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Test based on GSE19480, see bug 3177
 * 
 * @author paul
 * @version $Id$
 */
public class LowVarianceDataTest extends AbstractGeoServiceTest {

    @Autowired
    private GeoService geoService;

    @Autowired
    ExperimentalDesignImporter designImporter;

    @Autowired
    ExperimentalFactorService experimentalFactorService;

    @Autowired
    ExpressionExperimentService expressionExperimentService;

    @Autowired
    DiffExAnalyzer analyzer;

    @Autowired
    AnalysisSelectionAndExecutionService analysisService = null;

    @Autowired
    DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService;

    @Autowired
    DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    ExpressionExperiment ee;

    @Before
    public void setup() throws Exception {

        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( FileTools
                .resourceToPath( "/data/analysis/expression" ) ) );

        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE19420", false, true, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            ee = ( ExpressionExperiment ) ( ( Collection<?> ) e.getData() ).iterator().next();

        }

        ee = expressionExperimentService.thawLite( ee );

        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            experimentalFactorService.delete( ef );
        }

        processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );

        ee = expressionExperimentService.thaw( ee );

        designImporter.importDesign( ee,
                this.getClass().getResourceAsStream( "/data/analysis/expression/2976_GSE19420_expdesign.data.txt" ) );

    }

    /**
     * @throws Exception
     */
    @Test
    public void test() throws Exception {

        AnalysisType aa = analysisService.determineAnalysis( ee, ee.getExperimentalDesign().getExperimentalFactors(),
                null, true );

        assertEquals( AnalysisType.TWANI, aa );

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();

        Collection<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors();

        assertEquals( 2, factors.size() );
        config.setAnalysisType( aa );
        config.setFactorsToInclude( factors );
        config.setQvalueThreshold( null );

        analyzer = this.getBean( DiffExAnalyzer.class );
        Collection<DifferentialExpressionAnalysis> result = analyzer.run( ee, config );
        assertEquals( 1, result.size() );

        DifferentialExpressionAnalysis analysis = result.iterator().next();

        checkResults( analysis );

    }

    /**
     * @param analysis
     */
    public void checkResults( DifferentialExpressionAnalysis analysis ) {
        Collection<ExpressionAnalysisResultSet> resultSets = analysis.getResultSets();

        assertEquals( 2, resultSets.size() );

        boolean found1 = false, found2 = false;

        for ( ExpressionAnalysisResultSet rs : resultSets ) {
            Collection<DifferentialExpressionAnalysisResult> results = rs.getResults();

            assertEquals( 100, results.size() );

            for ( DifferentialExpressionAnalysisResult r : results ) {
                CompositeSequence probe = r.getProbe();
                Double pvalue = r.getPvalue();

                // log.info( probe.getName() + " " + pvalue );

                if ( probe.getName().equals( "1552338_at" ) ) {
                    found1 = true;
                    assertEquals( null, pvalue );

                } else if ( probe.getName().equals( "1552335_at" ) ) {
                    found2 = true;
                    assertTrue( pvalue != null );
                }
            }

        }

        assertTrue( found1 && found2 );
    }
}
