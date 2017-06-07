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
package ubic.gemma.core.analysis.preprocess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.DataUpdater;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.core.security.authorization.acl.AclTestUtils;

/**
 * @author ptan
 * @version $Id$
 */
public class MeanVarianceServiceTest extends AbstractGeoServiceTest {

    @Autowired
    private MeanVarianceService meanVarianceService;

    @Autowired
    private GeoService geoService;

    @Autowired
    private DataUpdater dataUpdater;

    @Autowired
    private ExpressionExperimentService eeService;

    private ExpressionExperiment ee;

    private QuantitationType qt;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private AclTestUtils aclTestUtils;

    private static ByteArrayConverter bac = new ByteArrayConverter();

    @Before
    public void setUp() {
        ee = eeService.findByShortName( "GSE2982" );
        if ( ee != null ) {
            eeService.remove( ee ); // might work, but array designs might be in the way.
        }

        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal(
                    getTestFileBasePath( "gse2982Short" ) ) );
        } catch ( URISyntaxException e1 ) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        Collection<?> results = geoService.fetchAndLoad( "GSE2982", false, false, true, false );

        ee = ( ExpressionExperiment ) results.iterator().next();

        try {
            qt = createOrUpdateQt( ScaleType.LINEAR );
        } catch ( Exception e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        qt.setIsNormalized( true );
        quantitationTypeService.update( qt );

        // important bit, need to createProcessedVectors manually before using it
        ee = processedExpressionDataVectorService.createProcessedDataVectors( ee );
    }

    @After
    public void after() {
        try {
            eeService.remove( ee );
        } catch ( Exception e ) {

        }
    }

    @Test
    final public void testServiceLinearNormalized() throws Exception {

        assertEquals( 97, ee.getProcessedExpressionDataVectors().size() );

        MeanVarianceRelation mvr = meanVarianceService.create( ee, true );

        // convert byte[] to array[]
        // warning: order may have changed
        double[] means = bac.byteArrayToDoubles( mvr.getMeans() );
        double[] variances = bac.byteArrayToDoubles( mvr.getVariances() );
        Arrays.sort( means );
        Arrays.sort( variances );

        int expectedLength = 97; // after filtering
        assertEquals( expectedLength, means.length );
        assertEquals( expectedLength, variances.length );
        expectedLength = 95; // duplicate rows removed

        int idx = 0;
        assertEquals( -1.9858, means[idx], 0.0001 );
        assertEquals( 0, variances[idx], 0.0001 );

        idx = expectedLength - 1;
        assertEquals( 0.02509, means[idx], 0.0001 );
        assertEquals( 0.09943, variances[idx], 0.0001 );

    }

    @Test
    final public void testServiceCreateTwoColor() throws Exception {

        qt = createOrUpdateQt( ScaleType.LOG2 );
        qt.setIsNormalized( false );
        quantitationTypeService.update( qt );

        // update ArrayDesign to TWOCOLOR
        Collection<ArrayDesign> aas = eeService.getArrayDesignsUsed( ee );
        assertEquals( 1, aas.size() );
        ArrayDesign des = aas.iterator().next();
        des.setTechnologyType( TechnologyType.TWOCOLOR );
        arrayDesignService.update( des );

        aclTestUtils.checkHasAcl( des );

        // check that ArrayDesign is the right TechnologyType
        aas = eeService.getArrayDesignsUsed( ee );
        assertEquals( 1, aas.size() );
        des = aas.iterator().next();
        assertEquals( TechnologyType.TWOCOLOR, des.getTechnologyType() );

        MeanVarianceRelation mvr = meanVarianceService.create( ee, true );

        aclTestUtils.checkEEAcls( ee );

        assertEquals( 97, ee.getProcessedExpressionDataVectors().size() );

        // convert byte[] to array[]
        // warning: order may have changed
        double[] means = bac.byteArrayToDoubles( mvr.getMeans() );
        double[] variances = bac.byteArrayToDoubles( mvr.getVariances() );
        Arrays.sort( means );
        Arrays.sort( variances );

        int expectedLength = 75; // after filtering
        assertEquals( expectedLength, means.length );
        assertEquals( expectedLength, variances.length );

        int idx = 0;
        assertEquals( -0.34836, means[idx], 0.0001 );
        assertEquals( 0.001569, variances[idx], 0.0001 );

        idx = expectedLength - 1;
        assertEquals( 0.05115, means[idx], 0.0001 );
        assertEquals( 0.12014, variances[idx], 0.0001 );

    }

    private QuantitationType createOrUpdateQt( ScaleType scale ) throws Exception {

        Collection<QuantitationType> qtList = eeService.getPreferredQuantitationType( ee );
        if ( qtList.size() == 0 ) {
            qt = QuantitationType.Factory.newInstance();
            qt.setName( "testQt" );
            qt.setScale( scale );
            qt.setIsPreferred( true );
            qt.setRepresentation( PrimitiveType.DOUBLE );
            qt.setIsMaskedPreferred( false );
            qt.setIsRatio( false );
            qt.setIsNormalized( false );
            qt.setIsBackground( false );
            qt.setGeneralType( GeneralType.QUANTITATIVE );
            qt.setType( StandardQuantitationType.AMOUNT );
            qt.setIsBackgroundSubtracted( false );
            qt.setIsBatchCorrected( false );
            qt.setIsRecomputedFromRawData( false );
            quantitationTypeService.create( qt );
        } else {
            qt = qtList.iterator().next();
            qt.setScale( scale );
            quantitationTypeService.update( qt );
        }

        return qt;
    }

    @Test
    final public void testServiceCreateOneColor() throws Exception {

        qt = createOrUpdateQt( ScaleType.LOG2 );
        qt.setIsNormalized( false );
        quantitationTypeService.update( qt );

        // update ArrayDesign to ONECOLOR
        Collection<ArrayDesign> aas = eeService.getArrayDesignsUsed( ee );
        assertEquals( 1, aas.size() );
        ArrayDesign des = aas.iterator().next();
        des.setTechnologyType( TechnologyType.ONECOLOR );
        arrayDesignService.update( des );

        // check that ArrayDesign is the right TechnologyType
        aas = eeService.getArrayDesignsUsed( ee );
        assertEquals( 1, aas.size() );
        des = aas.iterator().next();
        assertEquals( TechnologyType.ONECOLOR, des.getTechnologyType() );

        MeanVarianceRelation mvr = meanVarianceService.create( ee, true );

        // convert byte[] to array[]
        // warning: order may have changed
        double[] means = bac.byteArrayToDoubles( mvr.getMeans() );
        double[] variances = bac.byteArrayToDoubles( mvr.getVariances() );
        Arrays.sort( means );
        Arrays.sort( variances );

        // check sizes
        int expectedMeanVarianceLength = 75;
        int expectedLowessLength = 75; // NAs removed
        assertEquals( expectedMeanVarianceLength, means.length );
        assertEquals( expectedMeanVarianceLength, variances.length );

        // check results
        int idx = 0;
        assertEquals( -0.3484, means[idx], 0.0001 );
        assertEquals( 0.001569, variances[idx], 0.0001 );

        idx = expectedLowessLength - 1;
        assertEquals( 0.05115, means[idx], 0.0001 );
        assertEquals( 0.12014, variances[idx], 0.0001 );

    }

    @Test
    final public void testServiceCreateCountData() throws Exception {

        // so it doesn't look for soft files
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );

        ee = eeService.findByShortName( "GSE29006" );
        if ( ee != null ) {
            eeService.remove( ee );
        }

        assertNull( eeService.findByShortName( "GSE29006" ) );

        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE29006", false, false, false, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            throw new IllegalStateException( "Need to remove this data set before test is run" );
        }

        eeService.thaw( ee );

        qt = createOrUpdateQt( ScaleType.COUNT );

        // Load the data from a text file.
        DoubleMatrixReader reader = new DoubleMatrixReader();

        try (InputStream countData = this.getClass()
                .getResourceAsStream( "/data/loader/expression/flatfileload/GSE29006_expression_count.test.txt" );

                InputStream rpkmData = this.getClass().getResourceAsStream(
                        "/data/loader/expression/flatfileload/GSE29006_expression_RPKM.test.txt" );) {
            DoubleMatrix<String, String> countMatrix = reader.read( countData );
            DoubleMatrix<String, String> rpkmMatrix = reader.read( rpkmData );

            List<String> probeNames = countMatrix.getRowNames();

            // we have to find the right generic platform to use.
            ArrayDesign targetArrayDesign = this.getTestPersistentArrayDesign( probeNames,
                    taxonService.findByCommonName( "human" ) );
            arrayDesignService.thaw( targetArrayDesign );

            try {
                dataUpdater.addCountData( ee, targetArrayDesign, countMatrix, rpkmMatrix, 36, true, false );
                fail( "Should have gotten an exception" );
            } catch ( IllegalArgumentException e ) {
                // Expected
            }
            dataUpdater.addCountData( ee, targetArrayDesign, countMatrix, rpkmMatrix, 36, true, true );
        }

        eeService.thaw( this.ee );

        assertNotNull( ee.getId() );

        MeanVarianceRelation mvr = meanVarianceService.create( ee, true );

        // convert byte[] to array[]
        // warning: order may have changed
        double[] means = bac.byteArrayToDoubles( mvr.getMeans() );
        double[] variances = bac.byteArrayToDoubles( mvr.getVariances() );
        Arrays.sort( means );
        Arrays.sort( variances );

        // check sizes
        int expectedMeanVarianceLength = 199;
        int expectedLowessLength = 197; // NAs removed
        assertEquals( expectedMeanVarianceLength, means.length );
        assertEquals( expectedMeanVarianceLength, variances.length );

        int idx = 0;
        assertEquals( 1.037011, means[idx], 0.0001 );
        assertEquals( 0.00023724336, variances[idx], 0.000001 );

        idx = expectedLowessLength - 1;
        assertEquals( 15.23313, means[idx], 0.0001 );
        assertEquals( 4.84529, variances[idx], 0.0001 );
    }

    @Test
    final public void testServiceCreateExistingEe() throws Exception {

        // no MeanVarianceRelation exists yet
        ee = eeService.load( ee.getId() );
        assertNotNull( ee.getId() );
        MeanVarianceRelation oldMvr = ee.getMeanVarianceRelation();
        assertNull( oldMvr );
        Long oldEeId = ee.getId();

        // first time we create a MeanVarianceRelation
        ee = eeService.load( ee.getId() );
        MeanVarianceRelation mvr = meanVarianceService.create( ee, true );
        assertEquals( oldEeId, ee.getId() );
        assertNotNull( mvr );
        oldMvr = mvr;

        // now that the MeanVarianceRelation exists
        // try loading ee again by just using an eeId
        // and see if we get a no Session error
        ee = eeService.load( ee.getId() );
        mvr = meanVarianceService.create( ee, true );
        assertEquals( oldEeId, ee.getId() );
        assertTrue( oldMvr != mvr );

    }
}
