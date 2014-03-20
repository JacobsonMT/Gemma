/*
 * The Gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
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
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.HashSet;

import org.junit.After;
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
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public class TwoWayAnovaWithInteractionsTest2 extends AbstractGeoServiceTest {

    @Autowired
    private AnalysisSelectionAndExecutionService analysisService = null;

    @Autowired
    private DiffExAnalyzer analyzer;

    @Autowired
    private ExperimentalDesignImporter designImporter;

    private ExpressionExperiment ee;

    @Autowired
    private ExperimentalFactorService experimentalFactorService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    @Autowired
    private GeoService geoService;

    @Test
    public void test() throws Exception {

        ee = expressionExperimentService.thawLite( ee );
        Collection<ExperimentalFactor> factors = ee.getExperimentalDesign().getExperimentalFactors();

        assertEquals( 2, factors.size() );

        for ( BioAssay ba : ee.getBioAssays() ) {
            assertEquals( 2, ba.getSampleUsed().getFactorValues().size() );
        }

        AnalysisType aa = analysisService.determineAnalysis( ee, ee.getExperimentalDesign().getExperimentalFactors(),
                null, true );

        assertEquals( AnalysisType.TWO_WAY_ANOVA_WITH_INTERACTION, aa );

        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();
        config.setAnalysisType( aa );
        config.setFactorsToInclude( factors );
        config.addInteractionToInclude( factors );
        config.setQvalueThreshold( null );

        analyzer = this.getBean( DiffExAnalyzer.class );
        Collection<DifferentialExpressionAnalysis> result = analyzer.run( ee, config );
        assertEquals( 1, result.size() );

        DifferentialExpressionAnalysis analysis = result.iterator().next();

        assertNotNull( analysis );

        assertEquals( 3, analysis.getResultSets().size() );

    }

    @After
    public void teardown() throws Exception {
        if ( ee != null ) expressionExperimentService.delete( ee );
    }

    @Before
    public void setup() throws Exception {

        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( FileTools
                .resourceToPath( "/data/analysis/expression/gse18795short" ) ) );

        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE18795", false, true, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            ee = ( ExpressionExperiment ) ( ( Collection<?> ) e.getData() ).iterator().next();

        }

        ee = expressionExperimentService.thawLite( ee );

        Collection<ExperimentalFactor> toremove = new HashSet<ExperimentalFactor>();
        toremove.addAll( ee.getExperimentalDesign().getExperimentalFactors() );
        for ( ExperimentalFactor ef : toremove ) {
            experimentalFactorService.delete( ef );
            ee.getExperimentalDesign().getExperimentalFactors().remove( ef );

        }

        expressionExperimentService.update( ee );

        processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );

        ee = expressionExperimentService.thaw( ee );

        designImporter.importDesign(
                ee,
                this.getClass().getResourceAsStream(
                        "/data/analysis/expression/gse18795short/6290_GSE18795_expdesign.data.txt" ) );

    }

}
