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
package ubic.gemma.analysis.preprocess;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang.RandomStringUtils;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.loader.expression.geo.DatasetCombiner;
import ubic.gemma.loader.expression.geo.GeoConverter;
import ubic.gemma.loader.expression.geo.GeoFamilyParser;
import ubic.gemma.loader.expression.geo.GeoParseResult;
import ubic.gemma.loader.expression.geo.GeoSampleCorrespondence;
import ubic.gemma.loader.expression.geo.model.GeoSeries;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class TwoChannelMissingValuesTest extends BaseSpringContextTest {

    GeoConverter gc = new GeoConverter();
    TwoChannelMissingValues tcmv;

    ExpressionExperimentService eeService;

    DesignElementDataVectorService dedvService;

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        tcmv = ( TwoChannelMissingValues ) this.getBean( "twoChannelMissingValues" );
        dedvService = ( DesignElementDataVectorService ) this.getBean( "designElementDataVectorService" );
        eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        endTransaction();
    }

    /**
     * GSE56 is corrupt: there is no Channel 1 signal value in the data file.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testMissingValueGSE56() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE56Short/GSE56_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE56" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
        ExpressionExperiment expExp = ( ExpressionExperiment ) ( ( Collection ) result ).iterator().next();

        // make sure we don't run into bad data that already is in the DB
        expExp.setShortName( RandomStringUtils.randomAlphabetic( 20 ) );
        expExp.setName( RandomStringUtils.randomAlphabetic( 200 ) );
        expExp.setAccession( null );

        expExp = ( ExpressionExperiment ) persisterHelper.persist( expExp );
        Collection<RawExpressionDataVector> calls = tcmv.computeMissingValues( expExp, 2.0, new ArrayList<Double>() );

        /*
         * There is one array design and it has 10 rows.
         */
        assertEquals( 10, calls.size() );
    }

    @SuppressWarnings("unchecked")
    public void testMissingValueGSE523() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSE523_family.soft.gz" ) );
        GeoFamilyParser parser = new GeoFamilyParser();
        parser.parse( is );
        GeoSeries series = ( ( GeoParseResult ) parser.getResults().iterator().next() ).getSeriesMap().get( "GSE523" );
        DatasetCombiner datasetCombiner = new DatasetCombiner();
        GeoSampleCorrespondence correspondence = datasetCombiner.findGSECorrespondence( series );
        series.setSampleCorrespondence( correspondence );
        Object result = this.gc.convert( series );
        assertNotNull( result );
        ExpressionExperiment expExp = ( ExpressionExperiment ) ( ( Collection ) result ).iterator().next();
        // make sure we don't run into bad data that already is in the DB
        expExp.setShortName( RandomStringUtils.randomAlphabetic( 20 ) );
        expExp.setName( RandomStringUtils.randomAlphabetic( 200 ) );
        expExp.setAccession( null );
        expExp = ( ExpressionExperiment ) persisterHelper.persist( expExp );
        Collection<RawExpressionDataVector> calls = tcmv.computeMissingValues( expExp, 2.0, new ArrayList<Double>() );

        /*
         * The expected number of rows is 30, because there are two platforms, one with 20 features and one with 10 (in
         * this contrived example)
         */
        assertEquals( 30, calls.size() );
    }

    /**
     * @throws Exception
     */
    public void testMissingValue() throws Exception {
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
        ExpressionExperiment expExp = ( ExpressionExperiment ) ( ( Collection<?> ) result ).iterator().next();
        // make sure we don't run into bad data that already is in the DB
        expExp.setShortName( RandomStringUtils.randomAlphabetic( 20 ) );
        expExp.setName( RandomStringUtils.randomAlphabetic( 200 ) );
        expExp.setAccession( null );
        expExp = ( ExpressionExperiment ) persisterHelper.persist( expExp );

        Collection<RawExpressionDataVector> calls = tcmv.computeMissingValues( expExp, 2.0, new ArrayList<Double>() );

        assertEquals( 500, calls.size() );

        ByteArrayConverter bac = new ByteArrayConverter();

        BioAssayDimension dim = calls.iterator().next().getBioAssayDimension();

        if ( log.isDebugEnabled() ) {
            System.err.print( "\n" );
            for ( BioAssay bas : dim.getBioAssays() ) {
                System.err.print( "\t" + bas );
            }
            System.err.print( "\n" );
            for ( DesignElementDataVector vector : calls ) {
                System.err.print( vector.getDesignElement() );
                byte[] dat = vector.getData();
                boolean[] row = bac.byteArrayToBooleans( dat );
                for ( boolean b : row ) {
                    System.err.print( "\t" + b );
                }
                System.err.print( "\n" );
            }
        }

        /*
         * Spot check the results. For sample ME-TMZ, ID #27 should be 'true' and 26 should be false.
         */
        boolean foundA = false;
        boolean foundB = false;
        for ( DesignElementDataVector vector : calls ) {
            if ( vector.getDesignElement().getName().equals( "26" ) ) {
                byte[] dat = vector.getData();
                boolean[] row = bac.byteArrayToBooleans( dat );
                int i = 0;
                for ( BioAssay bas : dim.getBioAssays() ) {
                    if ( bas.getName().equals( "expression array ME-TMZ" ) ) {
                        assertTrue( !row[i] );
                        foundA = true;
                    }
                    i++;
                }
            }
            if ( vector.getDesignElement().getName().equals( "27" ) ) {
                byte[] dat = vector.getData();
                boolean[] row = bac.byteArrayToBooleans( dat );
                int i = 0;
                for ( BioAssay bas : dim.getBioAssays() ) {
                    if ( bas.getName().equals( "expression array ME-TMZ" ) ) {
                        assertTrue( row[i] );
                        foundB = true;
                    }
                    i++;
                }
            }
        }

        assertTrue( foundA && foundB );

    }

}
