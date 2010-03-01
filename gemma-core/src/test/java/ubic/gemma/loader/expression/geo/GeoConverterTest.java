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
package ubic.gemma.loader.expression.geo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.loader.expression.geo.model.GeoPlatform;
import ubic.gemma.loader.expression.geo.model.GeoSeries;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Unit test for GeoConversion Added extension BaseSpringContextTest as want Taxon Service to be called
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoConverterTest extends BaseSpringContextTest {

    @Autowired
    GeoConverter gc;

    ByteArrayConverter bac = new ByteArrayConverter();

    /**
     * GSE2388 is an example of where the array and sample taxon do not match. This test checks that the biomaterial and
     * array taxons are set correctly.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    @Transactional
    public void testArrayTaxonDifferentToSampleTaxon() throws Exception {

        Taxon t = taxonService.findByScientificName( "Salmonidae" );

        if ( t == null ) {
            super.executeSqlScript( "/script/sql/add-fish-taxa.sql", false );
        }

        Taxon rainbowTrout = taxonService.findByAbbreviation( "omyk" );
        assertNotNull( rainbowTrout );
        Taxon atlanticSalm = taxonService.findByAbbreviation( "ssal" );
        assertNotNull( atlanticSalm );
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE2388.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );

        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE2388" );
        GeoPlatform platform = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatformMap().get(
                "GPL966" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        // assert that the biomaterials have been set as one taxon
        Object seriesResult = gc.convert( series );
        assertNotNull( seriesResult );
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) seriesResult;
        ExpressionExperiment exper = ees.iterator().next();
        Collection<BioAssay> bioassays = exper.getBioAssays();
        Collection<BioMaterial> materials = bioassays.iterator().next().getSamplesUsed();
        Taxon taxon = materials.iterator().next().getSourceTaxon();
        assertEquals( "Oncorhynchus kisutch", taxon.getScientificName() );

        // assert that the platform is another taxon

        Object resultPlatForm = gc.convert( platform );
        ArrayDesign ad = ( ArrayDesign ) resultPlatForm;
        assertNotNull( ad );
        Set<Taxon> taxa = new HashSet<Taxon>();
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {

            BioSequence bs = cs.getBiologicalCharacteristic();
            if ( bs != null ) {
                assertNotNull( bs.getTaxon() );
                log.info( bs.getTaxon() );
                taxa.add( bs.getTaxon() );
            }
        }
        // can be empty taxon if the probe does not have a sequence which is why taxon size is 3.
        assertEquals( 2, taxa.size() );
        assertTrue( taxa.contains( rainbowTrout ) );
        assertTrue( taxa.contains( atlanticSalm ) );

    }

    @Test
    public void testConvertDataDoubles() {
        List<Object> testList = new ArrayList<Object>();
        testList.add( "1.1" );
        testList.add( "2929202e-4" );
        testList.add( "-394949.44422" );
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setRepresentation( PrimitiveType.DOUBLE );
        byte[] actualResult = gc.convertData( testList, qt );
        double[] revertedResult = bac.byteArrayToDoubles( actualResult );
        assertEquals( revertedResult[0], 1.1, 0.00001 );
        assertEquals( revertedResult[1], 2929202e-4, 0.00001 );
        assertEquals( revertedResult[2], -394949.44422, 0.00001 );
    }

    /*
     * @see TestCase#setUp()
     */
    // protected void setUp() throws Exception {
    // super.setUp();
    // }
    /*
     * @see TestCase#tearDown()
     */
    // @Override
    // protected void tearDown() throws Exception {
    // super.tearDown();
    // }
    /*
     * Test method for 'ubic.gemma.loader.expression.geo.GeoConverter.convertData(List<String>)'
     */
    @Test
    public void testConvertDataIntegers() {
        List<Object> testList = new ArrayList<Object>();
        testList.add( "1" );
        testList.add( "2929202" );
        testList.add( "-394949" );
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setRepresentation( PrimitiveType.INT );
        byte[] actualResult = gc.convertData( testList, qt );
        int[] revertedResult = bac.byteArrayToInts( actualResult );
        assertEquals( revertedResult[0], 1 );
        assertEquals( revertedResult[1], 2929202 );
        assertEquals( revertedResult[2], -394949 );
    }

    @Test
    public void testConvertGenePix() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/shortGenePix/GSE2221_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE2221" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
    }

    /**
     * caused "GSM3059 had no platform assigned"
     * 
     * @throws Exception
     */
    @Test
    public void testConvertGSE106() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse106Short/GSE106.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE106" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConvertGSE2982() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse2982Short/GSE2982_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE2982" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;
        assertEquals( 1, ees.size() );
        ExpressionExperiment ee = ees.iterator().next();
        boolean ok = false;
        for ( DesignElementDataVector dedv : ee.getRawExpressionDataVectors() ) {
            QuantitationType qt = dedv.getQuantitationType();
            if ( qt.getIsPreferred() ) {
                ok = true;
                assertEquals( "VALUE", qt.getName() );
                assertEquals( StandardQuantitationType.AMOUNT, qt.getType() );
                assertTrue( qt.getIsRatio() );
            }
        }
        assertTrue( ok );
    }

    /**
     * Note: this series has some samples that don't have all the quantitation types.
     * 
     * @throws Exception
     */
    @Test
    public void testConvertGSE360() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE360Short/GSE360_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE360" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
    }

    /**
     * Not all quantitation types are found in all samples, and not all in the same order. This is a broken GSE for us.
     */
    // public void testConvertGSE4345() throws Exception {
    // InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
    // "/data/loader/expression/geo/gse4345Short/GSE4345.soft.gz" ) );
    // GeoFamilyParser parser = new GeoFamilyParser();
    // parser.parse( is );
    // GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE4345" );
    // DatasetCombiner datasetCombiner = new DatasetCombiner();
    // GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
    // series.setSampleCorrespondence( correspondence );
    // Object result = this.gc.convert( series );
    // assertNotNull( result );
    // }
    /**
     * Has two species.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConvertGSE3791() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse3791Short/GSE3791.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE3791" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;
        assertEquals( 2, ees.size() );
    }

    /**
     * Gets no 'preferred' quantitation type. - it should find one.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConvertGSE404() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse404Short/GSE404.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE404" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );

        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;

        ExpressionExperiment ee = ees.iterator().next();
        boolean ok = false;
        for ( DesignElementDataVector dedv : ee.getRawExpressionDataVectors() ) {
            QuantitationType qt = dedv.getQuantitationType();
            if ( qt.getIsPreferred() ) {
                ok = true;
                assertEquals( "VALUE", qt.getName() );
            }
        }

        assertTrue( ok );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConvertGse59() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE59Short/GSE59_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE59" );
        DatasetCombiner datasetCombiner = new DatasetCombiner( false );
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        // GeoValues values = series.getValues();
        // System.err.print( values );
        Object result = this.gc.convert( series );
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;
        ExpressionExperiment ee = ees.iterator().next();
        boolean ok = false;
        for ( DesignElementDataVector dedv : ee.getRawExpressionDataVectors() ) {
            QuantitationType qt = dedv.getQuantitationType();

            if ( qt.getIsPreferred() ) {
                ok = true;
                assertEquals( "VALUE", qt.getName() );
                // ExpressionDataDoubleMatrix mat = new ExpressionDataDoubleMatrix( ee, dedv.getBioAssayDimension(), qt
                // );
                // System.err.println( qt );
                // System.err.print( mat );
            }
        }

        assertTrue( ok );
    }

    /**
     * Was yielding a 'ArrayDesigns must be converted before datasets'.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConvertGSE60() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse60Short/GSE60_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse60Short/GDS75.soft.gz" ) );
        parser.parse( is );

        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE60" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;
        assertEquals( 1, ees.size() );
    }

    /**
     * NPE in converter, not reproduced here.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConvertGSE8134() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE8134_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );

        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE8134" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;
        assertEquals( 1, ees.size() );
    }

    /**
     * Problem with QT being interpreted as String instead of Double.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testConvertGSE5091() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE5091_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );

        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE5091" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;
        assertEquals( 1, ees.size() );

        ExpressionExperiment ee = ees.iterator().next();

        Collection<QuantitationType> quantitationTypes = ee.getQuantitationTypes();

        boolean found = false;
        for ( QuantitationType quantitationType : quantitationTypes ) {
            // log.info(quantitationType);
            if ( quantitationType.getName().equals( "VALUE" ) ) {
                /*
                 * Here's the problem. Of course it works fine...
                 */
                assertEquals( PrimitiveType.DOUBLE, quantitationType.getRepresentation() );
                assertTrue( quantitationType.getIsPreferred() );
                found = true;
            }
        }

        assertTrue( found );
    }

    /**
     * Case where the same sample can be in multiple series, we had problems with it.
     * 
     * @throws Exception
     */
    @Test
    public void testConvertMultiSeriesPerSample() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE3193Short/GSE3193_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE3193" );
        DatasetCombiner datasetCombiner = new DatasetCombiner( false );
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
        ExpressionExperiment ee = ( ExpressionExperiment ) ( ( Collection<?> ) result ).iterator().next();
        // assertEquals( 85, ee.getBioAssays().size() );
        Map<ArrayDesign, Integer> ads = new HashMap<ArrayDesign, Integer>();
        for ( BioAssay b : ee.getBioAssays() ) {
            if ( ads.containsKey( b.getArrayDesignUsed() ) ) {
                ads.put( b.getArrayDesignUsed(), ads.get( b.getArrayDesignUsed() ) + 1 );
            } else {
                ads.put( b.getArrayDesignUsed(), new Integer( 1 ) );
            }
        }
        assertEquals( 4, ads.size() );
        for ( ArrayDesign ad : ads.keySet() ) {
            Integer count = ads.get( ad );
            if ( ad.getName().equals( "SHAC" ) ) {
                assertEquals( 8, count.intValue() ); // ok
            } else if ( ad.getName().equals( "SVJ" ) ) {
                assertEquals( 1, count.intValue() );// ok
            } else if ( ad.getName().equals( "SVL_SVM_SVN_SVO" ) ) {
                assertEquals( 32, count.intValue() );
            } else if ( ad.getName().equals( "SVC" ) ) {
                assertEquals( 44, count.intValue() );
            } else {
                fail( "Name was " + ad.getName() );
            }
        }
    }

    @Test
    public void testConvertWithLotsOfPlatforms() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse3500Short/GSE3500_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE3500" );
        DatasetCombiner datasetCombiner = new DatasetCombiner( false );
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
    }

    /**
     * Ends up with too few vectors, because of a problem with the quantitation type guesser.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testFetchAndLoadGSE8294() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE8294_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE8294" );
        DatasetCombiner datasetCombiner = new DatasetCombiner( false );
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Set result = ( Set ) this.gc.convert( series );
        ExpressionExperiment e = ( ExpressionExperiment ) result.iterator().next();
        assertEquals( 66, e.getRawExpressionDataVectors().size() );
    }

    /**
     * GSE4047 is an example of where some of the samples used have channel 1 and channel 2 taxon different. And thus an
     * exception should be thrown
     * 
     * @throws Exception
     */
    @Test
    public void testIllegalArgumentExceptionChannel1Channel2taxonDifferent() throws Exception {
        try {
            InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                    "/data/loader/expression/geo/GSE4047.soft.gz" ) );
            GeoFamilyParser parser = new GeoFamilyParser();
            parser.parse( is );
            GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get(
                    "GSE4047" );
            DatasetCombiner datasetCombiner = new DatasetCombiner();
            GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
            series.setSampleCorrespondence( correspondence );
            this.gc.convert( series );
            fail();

        } catch ( IllegalArgumentException e ) {
            assertEquals(
                    "Channel 1 taxon is Danio rerio Channel 2 taxon is Pomacentrus moluccensis Check that is expected for sample GSM104737",
                    e.getMessage() );
        }
    }

    /**
     * Tests that if platform is defined as having multiple organisms but no column can be found that defines the taxon
     * at the probe level then an Exception is thrown
     * 
     * @throws Exception
     */
    @Test
    public void testIllegalArgumentExceptionMultipleTaxaOnArrayWithNoOrganismColumn() throws Exception {
        try {
            InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                    "/data/loader/expression/geo/GPL226_family.soft.gz" ) );
            GeoFamilyParser parser = new GeoFamilyParser();
            parser.setProcessPlatformsOnly( true );
            parser.parse( is );
            GeoPlatform platform = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatformMap().get(
                    "GPL226" );
            // add an extra organism to the platform to make a pretend 2 orgnaism array
            platform.addToOrganisms( "Rattus norvegicus" );
            this.gc.convert( platform );

            // thrown an error
            fail();
        } catch ( IllegalArgumentException e ) {
            // assertEquals(
            // "2 taxon found on platform: Mus musculus: Rattus norvegicus but there is no probe specific taxon Column found for platform GPL226",
            // e.getMessage() );
        }

    }

    @Test
    public void testImageClones() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GPL226_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoPlatform platform = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatformMap().get(
                "GPL226" );
        Object result = this.gc.convert( platform );
        ArrayDesign ad = ( ArrayDesign ) result;

        assertNotNull( ad );
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            BioSequence bs = cs.getBiologicalCharacteristic();
            if ( bs != null && bs.getName().startsWith( "IMAGE" ) ) {
                return;
            }

        }
        fail( "No IMAGE clones!" );
    }

    // We are more relaxed about number format errors; invalid values are put in as Missing (NaN)
    // public void testConvertDataMixed() {
    // List<String> testList = new ArrayList<String>();
    // testList.add( "1" ); // should trigger use of integers
    // testList.add( "2929202e-4" ); // uh-oh
    // testList.add( "-394949.44422" );
    // QuantitationType qt = QuantitationType.Factory.newInstance();
    // qt.setRepresentation( PrimitiveType.INT );
    // try {
    // gc.convertData( testList, qt );
    // fail( "Should have gotten an exception" );
    // } catch ( RuntimeException e ) {
    //
    // }
    // }

    /**
     * Method to test that an array design can have multiple taxa stored against it and that if abbreviations used as
     * probe names mapped to the scientific names correctly if the abbreviation is stored in DB.
     * 
     * @throws Exception
     */
    @Test
    @Transactional
    public void testMultipleTaxaIdentifiedBYAbbreviationsOnArrayWithOrganismColumn() throws Exception {

        Taxon t = taxonService.findByScientificName( "Salmonidae" );

        if ( t == null ) {
            super.executeSqlScript( "/script/sql/add-fish-taxa.sql", false );
        }

        Taxon rainbowTroat = taxonService.findByAbbreviation( "omyk" );
        Taxon whiteFish = taxonService.findByAbbreviation( "cclu" );
        Taxon rainbowSmelt = taxonService.findByAbbreviation( "omor" );
        Taxon atlanticSalm = taxonService.findByAbbreviation( "ssal" );

        assertNotNull( atlanticSalm );

        gc = ( GeoConverter ) this.getBean( "geoConverter" );
        // gc.clear();

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GPL2899.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        // parse only the plaform
        parser.setProcessPlatformsOnly( true );
        parser.parse( is );

        GeoPlatform platform = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatformMap().get(
                "GPL2899" );
        Object result = gc.convert( platform );
        ArrayDesign ad = ( ArrayDesign ) result;

        assertNotNull( ad );
        Set<Taxon> taxa = new HashSet<Taxon>();
        BioSequence bs = null;
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {

            bs = cs.getBiologicalCharacteristic();
            if ( bs != null ) {
                assertNotNull( bs.getTaxon() );
                taxa.add( bs.getTaxon() );
            }
        }
        assertEquals( 4, taxa.size() );

        log.info( taxa.toArray() );

        // original file has five taxa, test file just kept four.
        assertTrue( taxa.contains( atlanticSalm ) );
        assertTrue( taxa.contains( rainbowTroat ) );
        assertTrue( taxa.contains( whiteFish ) );
        assertTrue( taxa.contains( rainbowSmelt ) );
    }

    /**
     * Ensure that if platform has one taxon then taxon is still set correctly
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSingleTaxonOnArrayWithNoOrganismColumn() throws Exception {

        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GPL226_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.setProcessPlatformsOnly( true );
        parser.parse( is );
        GeoPlatform platform = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatformMap().get(
                "GPL226" );
        Object result = this.gc.convert( platform );
        ArrayDesign ad = ( ArrayDesign ) result;

        assertNotNull( ad );
        Set listPossibleTaxonValues = new HashSet();
        BioSequence bs = null;
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            bs = cs.getBiologicalCharacteristic();
            if ( bs != null ) {
                listPossibleTaxonValues.add( bs.getTaxon() );
            }
        }
        assertEquals( 1, listPossibleTaxonValues.size() );
    }

    /**
     * Test splitting
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSplitByPlatform() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE493Short/GSE493_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE493" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        this.gc.setSplitIncompatiblePlatforms( true );
        Object result = this.gc.convert( series );
        assertNotNull( result );

        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;
        assertEquals( 2, ees.size() );
        for ( ExpressionExperiment ee : ees ) {
            // 4 on one platform, 10 on the other. This is a bit sloppy but good enuf for now.
            assertTrue( ee.getBioAssays().size() == 4 || ee.getBioAssays().size() == 10 );
        }
    }

    /**
     * Platform has IMAGE:CCCCC in CLONE_ID column, no genbank accessions anywhere.
     * 
     * @throws Exception
     */
    @Test
    public final void testWithImageNoGenbank() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GPL222_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.setProcessPlatformsOnly( true );
        parser.parse( is );
        GeoPlatform platform = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatformMap().get(
                "GPL222" );
        Object result = this.gc.convert( platform );
        ArrayDesign ad = ( ArrayDesign ) result;
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            BioSequence bs = cs.getBiologicalCharacteristic();
            assertTrue( bs.getName().startsWith( "IMAGE" ) );
        }

    }

    /**
     * Has image clones.
     * 
     * @throws Exception
     */
    @Test
    public final void testWithImages() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GPL890_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.setProcessPlatformsOnly( true );
        parser.parse( is );
        GeoPlatform platform = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getPlatformMap().get(
                "GPL890" );
        Object result = this.gc.convert( platform );
        ArrayDesign ad = ( ArrayDesign ) result;
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            BioSequence bs = cs.getBiologicalCharacteristic();
            if ( bs != null && bs.getSequence() != null ) {
                return;
            }

        }
        fail( "No sequences!" );
    }

    /**
     * Test logic to evaluate a primary array taxon Either from platform taxon, common parent taxon or probe taxon.
     * 
     * @throws Exception
     */
    @Test
    public final void testGetPrimaryArrayTaxon() throws Exception {
        Collection<Taxon> platformTaxa = new HashSet<Taxon>();
        Collection<String> probeTaxa = new ArrayList<String>();
        Taxon salmonid = taxonService.findByCommonName( "salmonid" );
        Taxon rainbowTroat = taxonService.findByAbbreviation( "omyk" );
        Taxon atlanticSalm = taxonService.findByAbbreviation( "ssal" );
        atlanticSalm.setParentTaxon( salmonid );
        rainbowTroat.setParentTaxon( salmonid );
        Taxon human = taxonService.findByCommonName( "human" );

        platformTaxa.add( atlanticSalm );
        probeTaxa.add( "ssal" );
        probeTaxa.add( "omyk" );
        probeTaxa.add( "ssal" );
        // test get primary taxon from the array design platform if only one
        Taxon primaryTaxon = this.gc.getPrimaryArrayTaxon( platformTaxa, probeTaxa );
        assertEquals( "atlantic salmon", primaryTaxon.getCommonName() );
        // test that can work out parent taxon
        platformTaxa.add( rainbowTroat );
        Taxon primaryTaxonTwo = this.gc.getPrimaryArrayTaxon( platformTaxa, probeTaxa );
        assertEquals( "salmonid", primaryTaxonTwo.getCommonName() );

        // test that if no common parent taxon take most common taxon on probe
        platformTaxa.add( human );
        Taxon primaryTaxonThree = this.gc.getPrimaryArrayTaxon( platformTaxa, probeTaxa );
        assertEquals( "atlantic salmon", primaryTaxonThree.getCommonName() );

    }

    /**
     * Has only one GDS in GEOs when there really should be two. Bug 1829.
     * @throws Exception
     */
    @Test
    public final void testGSE8872() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse8872short/GSE8872_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );

        is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/gse8872Short/GDS2942.soft.gz" ) );
        parser.parse( is );

        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE8872" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
        Collection<ExpressionExperiment> ees = ( Collection<ExpressionExperiment> ) result;
        assertEquals( 1, ees.size() );
    }

}
