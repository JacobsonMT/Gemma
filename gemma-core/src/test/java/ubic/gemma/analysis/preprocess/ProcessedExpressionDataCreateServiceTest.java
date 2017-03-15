/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.analysis.preprocess;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.*;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author pavlidis
 */
public class ProcessedExpressionDataCreateServiceTest extends AbstractGeoServiceTest {

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private GeoService geoService;

    @Autowired
    private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    @Autowired
    private ExperimentalFactorService experimentalFactorService;

    @Autowired
    private ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private ExpressionExperimentReportService experimentReportService;

    @Autowired
    private BioMaterialService bioMaterialService;

    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;

    private ExpressionExperiment ee = null;

    @SuppressWarnings("unchecked")
    @Test
    public void testComputeDevRankForExpressionExperimentB() throws Exception {

        try {
            geoService.setGeoDomainObjectGenerator(
                    new GeoDomainObjectGeneratorLocal( getTestFileBasePath( "GSE5949short" ) ) );
            Collection<ExpressionExperiment> results = ( Collection<ExpressionExperiment> ) geoService
                    .fetchAndLoad( "GSE5949", false, true, false, false );
            this.ee = results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            this.ee = ( ( Collection<ExpressionExperiment> ) e.getData() ).iterator().next();
        }

        ee = eeService.thawLite( ee );

        processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );
        Collection<ProcessedExpressionDataVector> preferredVectors = this.processedExpressionDataVectorService
                .getProcessedDataVectors( ee );
        ee = eeService.load( ee.getId() );
        ee = eeService.thawLite( ee );
        int numQts = ee.getQuantitationTypes().size();
        for ( ProcessedExpressionDataVector d : preferredVectors ) {
            assertTrue( d.getQuantitationType().getIsMaskedPreferred() );
            assertTrue( ee.getQuantitationTypes().contains( d.getQuantitationType() ) );
            assertNotNull( d.getRankByMean() );
            assertNotNull( d.getRankByMax() );
        }

        assertNotNull( ee.getNumberOfDataVectors() );

        ExpressionExperimentValueObject s = experimentReportService.generateSummary( ee.getId() );
        assertEquals( ee.getNumberOfDataVectors(), s.getProcessedExpressionVectorCount() );

        processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );
        // repeat, make sure deleted old QTs.
        ee = eeService.load( ee.getId() );
        ee = eeService.thawLite( ee );
        assertEquals( numQts, ee.getQuantitationTypes().size() );
    }

    /**
     * Three platforms, one sample was not run on GPL81. It's 'Norm-1a', but the name we use for the sample is random.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testComputeDevRankForExpressionExperimentMultiArrayWithGaps() throws Exception {

        try {
            geoService.setGeoDomainObjectGenerator(
                    new GeoDomainObjectGeneratorLocal( getTestFileBasePath( "gse482short" ) ) );
            Collection<ExpressionExperiment> results = ( Collection<ExpressionExperiment> ) geoService
                    .fetchAndLoad( "GSE482", false, true, false, false );
            this.ee = results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            this.ee = ( ( Collection<ExpressionExperiment> ) e.getData() ).iterator().next();
        }

        ee = eeService.thawLite( ee );

        processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );
        Collection<ProcessedExpressionDataVector> preferredVectors = this.processedExpressionDataVectorService
                .getProcessedDataVectors( ee );
        ee = eeService.load( ee.getId() );
        ee = eeService.thawLite( ee );
        processedExpressionDataVectorService.thaw( preferredVectors );

        ExpressionDataDoubleMatrix mat = new ExpressionDataDoubleMatrix( preferredVectors );
        assertEquals( 10, mat.columns() );

        boolean found = false;
        for ( int i = 0; i < mat.rows(); i++ ) {
            Double[] row = mat.getRow( i );

            // debugging
            if ( i == 0 ) {
                for ( int j = 0; j < row.length; j++ ) {
                    BioAssay ba = mat.getBioAssaysForColumn( j ).iterator().next();
                    System.err.println( ba.getName() );
                }
            }
            System.err.print( mat.getRowElement( i ).getDesignElement().getName() + "\t" );
            for ( double d : row ) {
                System.err.print( String.format( "%4.2f\t", d ) );
            }
            System.err.print( "\n" );

            CompositeSequence el = mat.getDesignElementForRow( i );
            for ( int j = 0; j < row.length; j++ ) {
                BioAssay ba = mat.getBioAssaysForColumn( j ).iterator().next();
                if ( ba.getName().matches( "PGA-MurLungHyper-Norm-1a[ABC]v2-s2" ) && (
                        el.getName().equals( "100001_at" ) || el.getName().equals( "100002_at" ) || el.getName()
                                .equals( "100003_at" ) || el.getName().equals( "100004_at" ) || el.getName()
                                .equals( "100005_at" ) || el.getName().equals( "100006_at" ) || el.getName()
                                .equals( "100007_at" ) || el.getName().equals( "100009_r_at" ) || el.getName()
                                .equals( "100010_at" ) || el.getName().equals( "100011_at" ) ) ) {
                    assertEquals( Double.NaN, row[j], 0.0001 );
                    found = true;
                } else {
                    assertTrue( "Got unexpected NA value for " + ba.getName() + " for " + el.getName(),
                            !Double.isNaN( row[j] ) );
                }
            }
        }
        assertTrue( found );

        /*
         * Now do this through the processedExpressionDataVectorService
         */
        Collection<DoubleVectorValueObject> da = this.processedExpressionDataVectorService.getProcessedDataArrays( ee );
        assertEquals( 30, da.size() );
        found = false;
        boolean first = true;
        for ( DoubleVectorValueObject v : da ) {
            CompositeSequenceValueObject el = v.getDesignElement();
            double[] row = v.getData();

            // debugging
            if ( first ) {
                for ( int j = 0; j < row.length; j++ ) {
                    BioAssayValueObject ba = v.getBioAssays().get( j );
                    System.err.println( ba.getName() );
                }
                first = false;
            }
            System.err.print( el.getName() + "\t" );
            for ( double d : row ) {
                System.err.print( String.format( "%4.2f\t", d ) );
            }
            System.err.print( "\n" );

            assertEquals( 10, row.length );
            for ( int j = 0; j < row.length; j++ ) {
                assertNotNull( v.getBioAssays() );
                BioAssayValueObject ba = v.getBioAssays().get( j );
                if ( ba.getName().startsWith( "Missing bioassay for biomaterial" ) && (
                        el.getName().equals( "100001_at" ) || el.getName().equals( "100002_at" ) || el.getName()
                                .equals( "100003_at" ) || el.getName().equals( "100004_at" ) || el.getName()
                                .equals( "100005_at" ) || el.getName().equals( "100006_at" ) || el.getName()
                                .equals( "100007_at" ) || el.getName().equals( "100009_r_at" ) || el.getName()
                                .equals( "100010_at" ) || el.getName().equals( "100011_at" ) ) ) {
                    assertEquals( Double.NaN, row[j], 0.0001 );
                    found = true;
                } else {
                    assertTrue( "Got unexpected NA value for " + ba.getName() + " for " + el.getName(),
                            !Double.isNaN( row[j] ) );
                }
            }
        }
        assertTrue( found );
    }

    @Test
    public void testReorder() throws Exception {

        ExpressionExperiment old = eeService.findByShortName( "GSE404" );
        if ( old != null ) {
            eeService.delete( old );
        }

        try {
            geoService.setGeoDomainObjectGenerator(
                    new GeoDomainObjectGeneratorLocal( getTestFileBasePath( "gse404Short" ) ) );
            @SuppressWarnings("unchecked") Collection<ExpressionExperiment> results = ( Collection<ExpressionExperiment> ) geoService
                    .fetchAndLoad( "GSE404", false, true, false, false );
            this.ee = results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            this.ee = ( ExpressionExperiment ) e.getData();
        }

        ee = eeService.thawLite( ee );
        processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );

        ExperimentalFactor factor = ExperimentalFactor.Factory.newInstance();
        factor.setType( FactorType.CATEGORICAL );
        factor.setName( ee.getShortName() + " design" );
        factor.setExperimentalDesign( ee.getExperimentalDesign() );
        factor = eeService.addFactor( ee, factor );

        FactorValue fv1 = FactorValue.Factory.newInstance();
        FactorValue fv2 = FactorValue.Factory.newInstance();
        fv1.setValue( "foo" );
        fv1.setExperimentalFactor( factor );
        fv2.setValue( "bar" );
        fv2.setIsBaseline( true );
        fv2.setExperimentalFactor( factor );

        eeService.addFactorValue( ee, fv1 );
        eeService.addFactorValue( ee, fv2 );

        List<BioAssay> basInOrder = new ArrayList<>( ee.getBioAssays() );

        Collections.sort( basInOrder, new Comparator<BioAssay>() {

            @Override
            public int compare( BioAssay o1, BioAssay o2 ) {
                return o1.getId().compareTo( o2.getId() );
            }
        } );

        int i = 0;
        for ( BioAssay ba : basInOrder ) {
            // bioAssayService.thaw( ba );
            BioMaterial bm = ba.getSampleUsed();
            assert fv1.getId() != null;
            if ( !bm.getFactorValues().isEmpty() ) {
                continue;
            }
            if ( i % 2 == 0 ) {
                bm.getFactorValues().add( fv1 );
                // log.info( bm + " " + bm.getId() + " => " + fv1 );
            } else {
                bm.getFactorValues().add( fv2 );
                // log.info( bm + " " + bm.getId() + " => " + fv2 );
            }

            bioMaterialService.update( bm );

            i++;
        }

        factor = this.experimentalFactorService.load( factor.getId() );
        assertEquals( 2, factor.getFactorValues().size() );

        /*
         * All that was setup. Now do the interesting bit
         */

        processedExpressionDataVectorCreateService.reorderByDesign( ee.getId() );

        /*
         * Now check the vectors...
         */
        Collection<ProcessedExpressionDataVector> resortedVectors = processedExpressionDataVectorService
                .getProcessedDataVectors( ee );

        // ExpressionDataDoubleMatrix newMat = new ExpressionDataDoubleMatrix( resortedVectors );
        // log.info( newMat );

        boolean foundVector = false;

        assertTrue( resortedVectors.size() > 0 );

        for ( ProcessedExpressionDataVector vector : resortedVectors ) {
            i = 0;
            log.debug( vector.getDesignElement().getName() + " ........................." );

            // thawingto avoid lazy error because we are outside of transaction in this test. All references in code run
            // inside a transaction
            BioAssayDimension bioAssayDimension = bioAssayDimensionService.thawLite( vector.getBioAssayDimension() );

            Collection<BioAssay> bioAssays = bioAssayDimension.getBioAssays();

            for ( BioAssay ba : bioAssays ) {

                BioMaterial bm = ba.getSampleUsed();
                assertEquals( 1, bm.getFactorValues().size() );

                FactorValue fv = bm.getFactorValues().iterator().next();

                assertNotNull( fv.getId() );
                log.debug( ba.getId() + " " + fv.getId() + " " + fv );
                if ( i < 10 ) {
                    assertEquals( fv2, fv ); // first because it is baseline;
                }

                i++;

            }

            /*
             * spot check the data, same place as before.
             */
            if ( vector.getDesignElement().getName().equals( "40" ) ) {
                foundVector = true;
                ByteArrayConverter conv = new ByteArrayConverter();
                Double[] d = ArrayUtils.toObject( conv.byteArrayToDoubles( vector.getData() ) );
                assertEquals( 20, d.length );
                assertEquals( -0.08, d[1], 0.001 );
                assertEquals( 0.45, d[10], 0.001 );
                assertEquals( Double.NaN, d[19], 0.001 );
            }
        }

        assertTrue( "test vector not found", foundVector );

    }
}
