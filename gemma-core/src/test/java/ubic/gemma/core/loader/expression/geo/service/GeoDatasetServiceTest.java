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
package ubic.gemma.core.loader.expression.geo.service;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.core.analysis.preprocess.TwoChannelMissingValues;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.core.security.authorization.acl.AclTestUtils;
import ubic.gemma.core.tasks.analysis.expression.ExpressionExperimentLoadTask;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.File;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Test full procedure of loading GEO data, focus on corner cases. Tests deletion of data sets as well.
 *
 * @author pavlidis
 */
public class GeoDatasetServiceTest extends AbstractGeoServiceTest {

    @Autowired
    ProcessedExpressionDataVectorService processedExpressionDataVectorService;
    @Autowired
    TwoChannelMissingValues twoChannelMissingValues;
    @Autowired
    ExpressionExperimentLoadTask expressionExperimentLoadTask;
    @Autowired
    ExpressionDataFileService dataFileService;
    @Autowired
    AclTestUtils aclTestUtils;
    @Autowired
    private GeoService geoService;
    @Autowired
    private ExpressionExperimentService eeService;
    private ExpressionExperiment ee;
    @Autowired
    private RawExpressionDataVectorService rawExpressionDataVectorService;

    /*
     * Has multiple species (mouse and human, one and two platforms respectively), also test publication entry.
     */
    @Test
    public void testFetchAndLoadGSE1133() throws Exception {

        geoService.setGeoDomainObjectGenerator(
                new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath( "gse1133Short" ) ) );
        Collection<?> results;

        try {
            results = geoService.fetchAndLoad( "GSE1133", false, true, false );
        } catch ( AlreadyExistsInSystemException e ) {
            log.warn( "Test skipped because GSE1133 was not removed from the system prior to test" );
            return;
        }

        assertEquals( 2, results.size() );

        for ( Object o : results ) {
            ExpressionExperiment e = ( ExpressionExperiment ) o;
            e = eeService.thawLite( e );

            aclTestUtils.checkEEAcls( e );

            assertNotNull( e.getPrimaryPublication() );
            assertEquals( "6062-7", e.getPrimaryPublication().getPages() );

            try {
                eeService.remove( e );
            } catch ( Exception ex ) {
                log.info( "Failed to remove EE after test" );
            }
        }

    }

    @Test
    public void testFetchAndLoadGSE37646RNASEQ() throws Exception {
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath() ) );
        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE37646", false, true, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
            aclTestUtils.checkEEAcls( ee );
        } catch ( AlreadyExistsInSystemException e ) {
            log.warn( "Test skipped because GSE1133 was not removed from the system prior to test" );
        }
    }

    @Test
    public void testFetchAndLoadGSE12135EXON() throws Exception {
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath() ) );
        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE12135", false, true, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
            aclTestUtils.checkEEAcls( ee );
        } catch ( AlreadyExistsInSystemException e ) {
            log.warn( "Test skipped because GSE1133 was not removed from the system prior to test" );
        }

    }

    /*
     * Left out quantitation types due to bug in how quantitation types were cached during persisting, if the QTs didn't
     * have descriptions.
     */
    @Test
    public void testFetchAndLoadGSE13657() throws Exception {
        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath() ) );
            Collection<?> results = geoService.fetchAndLoad( "GSE13657", false, true, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            log.info( "Test skipped because GSE13657 was already loaded - clean the DB before running the test" );
            return;
        }
        ee = this.eeService.thawLite( ee );
        aclTestUtils.checkEEAcls( ee );
        Collection<QuantitationType> qts = eeService.getQuantitationTypes( ee );
        assertEquals( 13, qts.size() );

        // make sure we got characteristics and treatments for both channels.
        for ( BioAssay ba : ee.getBioAssays() ) {

            BioMaterial bm = ba.getSampleUsed();

            assertNotNull( bm );

            log.info( bm + " " + bm.getDescription() );

            assertEquals( 9, bm.getCharacteristics().size() );

        }

    }

    @After
    public void tearDown() {
        if ( ee != null )
            try {
                eeService.remove( ee );
            } catch ( Exception e ) {
                log.info( "Failed to remove EE after test: " + e.getMessage() );
                throw e;
            }
    }

    @Test
    public void testFetchAndLoadGSE9048() throws Exception {
        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath() ) );

            Collection<?> results = geoService.fetchAndLoad( "GSE9048", false, true, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            log.info( "Test skipped because GSE9048 was already loaded - clean the DB before running the test" );
            return;
        }

        ee = eeService.load( ee.getId() );
        ee = this.eeService.thawLite( ee );
        aclTestUtils.checkEEAcls( ee );
        Collection<QuantitationType> qts = eeService.getQuantitationTypes( ee );
        assertEquals( 16, qts.size() );

        twoChannelMissingValues.computeMissingValues( ee );

        ee = eeService.load( ee.getId() );
        ee = this.eeService.thawLite( ee );
        qts = eeService.getQuantitationTypes( ee );
        assertEquals( 17, qts.size() ); // 16 that were imported plus the detection call we added.

        Collection<ProcessedExpressionDataVector> dataVectors = processedExpressionDataVectorService
                .computeProcessedExpressionData( ee );

        assertEquals( 10, dataVectors.size() );

        ee = eeService.load( ee.getId() );
        ee = this.eeService.thawLite( ee );
        qts = eeService.getQuantitationTypes( ee );
        assertEquals( 18, qts.size() );
        File f = dataFileService.writeOrLocateDataFile( ee, true, true );
        assertTrue( f.canRead() );
        assertTrue( f.length() > 0 );
    }

    /*
     * For bug 2312 - qts getting dropped.
     */
    @Test
    public void testFetchAndLoadGSE18707() throws Exception {
        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath() ) );

            Collection<?> results = geoService.fetchAndLoad( "GSE18707", false, true, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            log.info( "Test skipped because GSE18707 was already loaded - clean the DB before running the test" );
            return;
        }

        // Mouse430A_2.
        ee = eeService.findByShortName( "GSE18707" );
        aclTestUtils.checkEEAcls( ee );
        Collection<QuantitationType> qts = eeService.getQuantitationTypes( ee );

        assertEquals( 1, qts.size() );
        QuantitationType qt = qts.iterator().next();
        assertEquals( "Processed Affymetrix Rosetta intensity values", qt.getDescription() );

        Collection<ProcessedExpressionDataVector> dataVectors = processedExpressionDataVectorService
                .computeProcessedExpressionData( ee );
        assertEquals( 100, dataVectors.size() );

        ee = eeService.findByShortName( "GSE18707" );

        qts = eeService.getQuantitationTypes( ee );

        assertEquals( 2, qts.size() );

    }

    @Test
    public void testFetchAndLoadGSE5949() throws Exception {
        try {
            geoService.setGeoDomainObjectGenerator(
                    new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath( "GSE5949short" ) ) );
            Collection<?> results = geoService.fetchAndLoad( "GSE5949", false, true, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            log.info( "Test skipped because GSE5949 was already loaded - clean the DB before running the test" );
            return;
        }
        ee = this.eeService.thawLite( ee );
        Collection<QuantitationType> qts = eeService.getQuantitationTypes( ee );
        assertEquals( 1, qts.size() );

    }

    @Test
    public void testFetchAndLoadMultiChipPerSeriesShort() throws Exception {
        geoService.setGeoDomainObjectGenerator(
                new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath( "shortTest" ) ) );

        /*
         * HG-U133A. GDS473 is for the other chip (B). Series is GSE674. see
         * http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gds&term=GSE674[Accession]&cmd=search
         */
        ExpressionExperiment newee;
        try {
            Collection<?> results = geoService.fetchAndLoad( "GSE674", false, true, false );
            newee = ( ExpressionExperiment ) results.iterator().next();

        } catch ( AlreadyExistsInSystemException e ) {
            log.info( "Skipping test, data already exists in db" );
            return;
        }
        assertNotNull( newee );
        newee = eeService.thaw( newee );

        /*
         * Test for bug 468 (merging of subsets across GDS's)
         */
        ExperimentalFactor factor = newee.getExperimentalDesign().getExperimentalFactors().iterator().next();
        assertEquals( 2, factor.getFactorValues().size() ); // otherwise get 4.

        Collection<RawExpressionDataVector> vectors = newee.getRawExpressionDataVectors();

        rawExpressionDataVectorService.thaw( vectors );

        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( vectors );

        ExpressionDataMatrix<Double> matrix = builder.getPreferredData();

        assertNotNull( matrix );

        assertEquals( 31, matrix.rows() );

        assertEquals( 15, matrix.columns() );

        // GSM10363 = D1-U133B
        this.testMatrixValue( newee, matrix, "200000_s_at", "GSM10363", 5722.0 );

        // GSM10380 = C7-U133A
        this.testMatrixValue( newee, matrix, "1007_s_at", "GSM10380", 1272.0 );

    }

    @Test
    public void testLoadGSE30521ExonArray() throws Exception {
        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath() ) );
            Collection<?> results = geoService.fetchAndLoad( "GSE30521", false, true, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            log.info( "Test skipped because GSE30521 was already loaded - clean the DB before running the test" );
            return;
        }
        ee = this.eeService.thawLite( ee );

        /*
         * Should load okay, but should not load the data.
         */
        try {
            processedExpressionDataVectorService.computeProcessedExpressionData( ee );
            fail( "Should not have any data vectors for exon arrays on first loading" );
        } catch ( Exception e ) {
            // OK
        }

    }

    @Test
    public void testLoadGSE28383ExonArray() throws Exception {
        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath() ) );
            Collection<?> results = geoService.fetchAndLoad( "GSE28383", false, true, false );
            ee = ( ExpressionExperiment ) results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            log.info( "Test skipped because GSE28383 was already loaded - clean the DB before running the test" );
            return;
        }
        ee = this.eeService.thawLite( ee );

        /*
         * Should load okay, even though it has no data. See bug 3981.
         */
        try {
            processedExpressionDataVectorService.computeProcessedExpressionData( ee );
            fail( "Should not have any data vectors for exon arrays on first loading" );
        } catch ( Exception e ) {
            // OK
        }

    }

    @SuppressWarnings("unused")
        // !! Please leave this here, we use it to load data sets for chopping.
    void fetchASeries( String accession ) {
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
        geoService.fetchAndLoad( accession, false, false, false );
    }

    @SuppressWarnings("unused")
    private void printMatrix( DoubleMatrix<Object, Object> matrix ) {
        StringBuilder buf = new StringBuilder();
        buf.append( "probe" );
        for ( Object columnName : matrix.getColNames() ) {
            buf.append( "\t" ).append( columnName );
        }
        buf.append( "\n" );
        for ( Object rowName : matrix.getRowNames() ) {
            buf.append( rowName );
            double[] array = matrix.getRowByName( rowName );
            for ( double array_element : array ) {
                buf.append( "\t" ).append( array_element );
            }
            buf.append( "\n" );
        }
        log.debug( buf.toString() );
    }

    private void testMatrixValue( ExpressionExperiment exp, ExpressionDataMatrix<Double> matrix, String probeToTest,
            String sampleToTest, double expectedValue ) {

        CompositeSequence soughtDesignElement = null;
        BioAssay soughtBioAssay = null;
        Collection<RawExpressionDataVector> vectors = exp.getRawExpressionDataVectors();
        for ( DesignElementDataVector vector : vectors ) {
            CompositeSequence de = vector.getDesignElement();
            if ( de.getName().equals( probeToTest ) ) {
                soughtDesignElement = de;
            }

            BioAssayDimension bad = vector.getBioAssayDimension();
            for ( BioAssay ba : bad.getBioAssays() ) {
                if ( ba.getAccession().getAccession().equals( sampleToTest ) ) {
                    soughtBioAssay = ba;
                }
            }

        }
        if ( soughtDesignElement == null || soughtBioAssay == null )
            fail( "didn't find values for " + sampleToTest );

        Double actualValue = matrix.get( soughtDesignElement, soughtBioAssay );
        assertNotNull( "No value for " + soughtBioAssay, actualValue );
        assertEquals( expectedValue, actualValue, 0.00001 );

    }

    // @Test
    // public void test() {
    // fetchASeries( "GSE45405" );
    // }

}
