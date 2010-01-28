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
package ubic.gemma.loader.expression.geo.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Collection;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.ConfigUtils;

/**
 * Test full procedure of loading GEO data, focus on corner cases. Tests deletion of data sets as well.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoDatasetServiceIntegrationTest extends AbstractGeoServiceTest {

    @Autowired
    protected GeoDatasetService geoService;

    @Autowired
    ExpressionExperimentService eeService;

    ExpressionExperiment ee;

    @Autowired
    ArrayDesignService adService;

    @Autowired
    private DesignElementDataVectorService designElementDataVectorService;

    /**
     * Has multiple species (mouse and human, one and two platforms respectively), also test publication entry.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testFetchAndLoadGSE1133() throws Exception {
        String path = getTestFileBasePath();
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT
                + "gse1133Short" ) );
        Collection<ExpressionExperiment> results = geoService.fetchAndLoad( "GSE1133", false, true, false, false, true );
        ee = results.iterator().next(); // fixme, need to delete both.
        eeService.thawLite( ee );

        /*
         * Sometimes pubmed barfs on us..but this should be populated.
         */
        assertNotNull( ee.getPrimaryPublication() );
        assertEquals( "6062-7", ee.getPrimaryPublication().getPages() );
        assertEquals( 2, results.size() );

        eeService.delete( ee );

        assertNull( eeService.load( ee.getId() ) );

    }

    /**
     * Left out quantitation types due to bug in how quantitation types were cached during persisting, if the QTs didn't
     * have descriptions.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testFetchAndLoadGSE13657() throws Exception {
        String path = ConfigUtils.getString( "gemma.home" );
        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path
                    + AbstractGeoServiceTest.GEO_TEST_DATA_ROOT ) );
            Collection<ExpressionExperiment> results = geoService.fetchAndLoad( "GSE13657", false, true, false, false,
                    true );
            ee = results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            log.info( "Test skipped because GSE13657 was already loaded - clean the DB before running the test" );
            return;
        }
        eeService.thawLite( ee );
        Collection qts = eeService.getQuantitationTypes( ee );
        assertEquals( 17, qts.size() );

        eeService.delete( ee );

        assertNull( eeService.load( ee.getId() ) );

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFetchAndLoadGSE5949() throws Exception {
        String path = ConfigUtils.getString( "gemma.home" );
        try {
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path
                    + AbstractGeoServiceTest.GEO_TEST_DATA_ROOT + "GSE5949short" ) );
            Collection<ExpressionExperiment> results = geoService.fetchAndLoad( "GSE5949", false, true, false, false,
                    true );
            ee = results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            log.info( "Test skipped because GSE5949 was already loaded - clean the DB before running the test" );
            return;
        }
        eeService.thawLite( ee );
        Collection qts = eeService.getQuantitationTypes( ee );
        assertEquals( 3, qts.size() );

        eeService.delete( ee );

        assertNull( eeService.load( ee.getId() ) );

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFetchAndLoadMultiChipPerSeriesShort() throws Exception {
        String path = getTestFileBasePath();
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path + GEO_TEST_DATA_ROOT
                + "shortTest" ) );

        /*
         * HG-U133A. GDS473 is for the other chip (B). Series is GSE674. see
         * http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=gds&term=GSE674[Accession]&cmd=search
         */
        ExpressionExperiment newee;
        try {
            Collection<ExpressionExperiment> results = geoService.fetchAndLoad( "GDS472", false, true, false, false,
                    true );
            newee = results.iterator().next();

        } catch ( AlreadyExistsInSystemException e ) {
            log.info( "Skipping test, data already exists in db" );
            return;
        }
        assertNotNull( newee );
        eeService.thaw( newee );

        /*
         * Test for bug 468 (merging of subsets across GDS's)
         */
        ExperimentalFactor factor = newee.getExperimentalDesign().getExperimentalFactors().iterator().next();
        assertEquals( 2, factor.getFactorValues().size() ); // otherwise get 4.

        Collection<RawExpressionDataVector> vectors = newee.getRawExpressionDataVectors();

        designElementDataVectorService.thaw( vectors );

        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( vectors );

        ExpressionDataMatrix<Double> matrix = builder.getPreferredData();

        assertNotNull( matrix );

        assertEquals( 31, matrix.rows() );

        assertEquals( 15, matrix.columns() );

        // GSM10363 = D1-U133B
        testMatrixValue( newee, matrix, "200000_s_at", "GSM10363", 5722.0 );

        // GSM10380 = C7-U133A
        testMatrixValue( newee, matrix, "1007_s_at", "GSM10380", 1272.0 );

        eeService.delete( newee );

        assertNull( eeService.load( newee.getId() ) );

    }

    /*
     * Please leave this here, we use it to load data sets for chopping.
     */
    void fetchASeries( String accession ) throws Exception {
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
        geoService.fetchAndLoad( accession, false, false, false, false, true );
    }

    /**
     * @param matrix
     */
    @SuppressWarnings( { "unchecked", "unused" })
    private void printMatrix( DoubleMatrix matrix ) {
        StringBuilder buf = new StringBuilder();
        buf.append( "probe" );
        for ( Object columnName : ( Collection<Object> ) matrix.getColNames() ) {
            buf.append( "\t" + columnName );
        }
        buf.append( "\n" );
        for ( Object rowName : ( Collection<Object> ) matrix.getRowNames() ) {
            buf.append( rowName );
            double[] array = matrix.getRowByName( rowName );
            for ( int i = 0; i < array.length; i++ ) {
                double array_element = array[i];
                buf.append( "\t" + array_element );
            }
            buf.append( "\n" );
        }
        log.debug( buf.toString() );
    }

    /**
     * @param ee
     * @param matrix
     * @param probeToTest
     * @param sampleToTest
     * @param expectedValue
     */
    private void testMatrixValue( ExpressionExperiment exp, ExpressionDataMatrix<Double> matrix, String probeToTest,
            String sampleToTest, double expectedValue ) {

        DesignElement soughtDesignElement = null;
        BioAssay soughtBioAssay = null;
        Collection<RawExpressionDataVector> vectors = exp.getRawExpressionDataVectors();
        for ( DesignElementDataVector vector : vectors ) {
            DesignElement de = vector.getDesignElement();
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
        if ( soughtDesignElement == null || soughtBioAssay == null ) fail( "didn't find values for " + sampleToTest );

        Double actualValue = matrix.get( soughtDesignElement, soughtBioAssay );
        assertNotNull( "No value for " + soughtBioAssay, actualValue );
        assertEquals( expectedValue, actualValue, 0.00001 );

    }

}
