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
package ubic.gemma.visualization;

import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix2DNamed;
import ubic.basecode.dataStructure.matrix.DoubleMatrixNamed;
import ubic.basecode.gui.ColorMatrix;
import ubic.basecode.gui.JMatrixDisplay;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import cern.colt.list.DoubleArrayList;

/**
 * A service to generate visualizations. Can be used to generate a color mosaic and x y line charts.
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean name="expressionDataMatrixVisualizationService"
 */
public class ExpressionDataMatrixVisualizationService {

    private Log log = LogFactory.getLog( this.getClass() );

    private static final int NUM_PROFILES_TO_DISPLAY = 3;

    /**
     * Generates a color mosaic (also known as a heat map).
     * 
     * @param expressionDataMatrix
     * @return JMatrixDisplay
     */
    public JMatrixDisplay createHeatMap( ExpressionDataMatrix expressionDataMatrix ) {

        if ( expressionDataMatrix == null )
            throw new RuntimeException( "Cannot create color matrix due to null ExpressionDataMatrix" );

        ColorMatrix colorMatrix = createColorMatrix( expressionDataMatrix );

        JMatrixDisplay display = new JMatrixDisplay( colorMatrix );
        display.setMaxColumnLength( 20 );

        display.setCellSize( new Dimension( 10, 10 ) );

        return display;
    }

    /**
     * Generates an x y line chart (also known as a profile).
     * 
     * @param title
     * @param dataCol
     * @param numProfiles
     * @return JFreeChart
     */
    public JFreeChart createXYLineChart( String title, Collection<double[]> dataCol, int numProfiles ) {
        if ( dataCol == null ) throw new RuntimeException( "dataCol cannot be " + null );

        if ( dataCol.size() < numProfiles ) {
            log.info( "Collection smaller than number of elements.  Will display first " + NUM_PROFILES_TO_DISPLAY
                    + " profiles." );
            numProfiles = NUM_PROFILES_TO_DISPLAY;
        }

        XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
        Iterator iter = dataCol.iterator();
        for ( int j = 0; j < numProfiles; j++ ) {
            double[] data = ( double[] ) iter.next();
            XYSeries series = new XYSeries( j, true, true );
            for ( int i = 0; i < data.length; i++ ) {
                series.add( i, data[i] );
            }
            xySeriesCollection.addSeries( series );
        }

        JFreeChart chart = ChartFactory.createXYLineChart( title, "Microarray", "Expression Value", xySeriesCollection,
                PlotOrientation.VERTICAL, false, false, false );
        chart.addSubtitle( new TextTitle( "(Raw data values)", new Font( "SansSerif", Font.BOLD, 14 ) ) );

        return chart;
    }

    /**
     * @param expressionDataMatrix
     * @return ColorMatrix
     */
    @SuppressWarnings("unchecked")
    private ColorMatrix createColorMatrix( ExpressionDataMatrix expressionDataMatrix ) {

        Collection<BioAssay> colElements = new LinkedHashSet<BioAssay>();

        if ( expressionDataMatrix == null || expressionDataMatrix.rows() == 0 ) {
            throw new IllegalArgumentException( "ExpressionDataMatrix apparently has no data" );
        }

        double[][] data = new double[expressionDataMatrix.rows()][];
        for ( int i = 0; i < expressionDataMatrix.rows(); i++ ) {
            Double[] row = ( Double[] ) expressionDataMatrix.getRow( i );
            data[i] = ArrayUtils.toPrimitive( row );
        }

        for ( int j = 0; j < data[0].length; j++ ) {
            Collection<BioAssay> bas = expressionDataMatrix.getBioAssaysForColumn( j );
            colElements.add( bas.iterator().next() );// this is temporary.
        }

        return createColorMatrix( data, expressionDataMatrix.getRowElements(), colElements );
    }

    /**
     * @param data
     * @param rowElements
     * @param colElements
     * @return ColorMatrix
     */
    private ColorMatrix createColorMatrix( double[][] data, List<ExpressionDataMatrixRowElement> rowElements,
            Collection<BioAssay> colElements ) {

        assert rowElements != null && colElements != null : "Labels cannot be set";

        List<String> rowLabels = new ArrayList<String>();

        List<String> colLabels = new ArrayList<String>();

        for ( ExpressionDataMatrixRowElement de : rowElements ) {
            rowLabels.add( de.toString() );
        }

        for ( BioAssay ba : colElements ) {
            if ( colLabels.contains( ba.getName() ) ) {
                ba.setName( ba.getName() + " _" + RandomUtils.nextInt() );
            }
            colLabels.add( ba.getName() );
        }

        DoubleMatrixNamed matrix = new DenseDoubleMatrix2DNamed( data );

        matrix.setRowNames( rowLabels );

        matrix.setColumnNames( colLabels );

        ColorMatrix colorMatrix = new ColorMatrix( matrix );

        return colorMatrix;
    }

    /**
     * Normalize (centered on the mean) and clip the data. That is, a z-score is calculated for each data point. If the
     * threshold is null, the data will not be clipped.
     * <p>
     * More information on z-scores can be found at http://en.wikipedia.org/wiki/Z_score.
     * </p>
     * 
     * @param expressionDataDoubleMatrix
     * @param threshold The threhold at which the data will be clipped (ie. 2 clips the data at -2 and +2).
     * @return ExpressionDataMatrix
     */
    public ExpressionDataMatrix standardizeExpressionDataDoubleMatrix( ExpressionDataMatrix expressionDataMatrix,
            Double threshold ) {

        ExpressionDataMatrix normalizedExpressionDataMatrix = expressionDataMatrix;

        Object[][] matrix = normalizedExpressionDataMatrix.getMatrix();

        for ( int i = 0; i < matrix.length; i++ ) {
            Object[] vector = matrix[i];

            double[] ddata = new double[vector.length];

            /* first, we convert each Object to a Double */
            for ( int j = 0; j < ddata.length; j++ ) {
                ddata[j] = ( Double ) vector[j];
            }

            /* calculate mean and variance for row */
            double mean = DescriptiveWithMissing.mean( new DoubleArrayList( ddata ) );

            double variance = DescriptiveWithMissing.variance( new DoubleArrayList( ddata ) );

            /* normalize the data */
            Double[] ndata = new Double[ddata.length];
            for ( int j = 0; j < ddata.length; j++ ) {
                ndata[j] = ddata[j];

                if ( Double.isNaN( ndata[j] ) ) {
                    continue;
                }
                ndata[j] = ( ndata[j] - mean ) / Math.sqrt( variance );
            }

            if ( threshold != null ) ndata = clipData( ndata, threshold );

            ( ( ExpressionDataDoubleMatrix ) normalizedExpressionDataMatrix ).setRow( i, ndata );
        }

        return normalizedExpressionDataMatrix;

    }

    /**
     * Clips the data at the positive and negative values of the threshold.
     * 
     * @param data
     * @param threshold
     * @return Double[]
     */
    private Double[] clipData( Double[] data, double threshold ) {

        threshold = Math.abs( threshold );

        double upperLimit = threshold;

        double lowerLimit = -1 * threshold;

        for ( int i = 0; i < data.length; i++ ) {

            if ( Double.isNaN( data[i] ) ) continue;

            if ( data[i] > upperLimit ) {
                data[i] = upperLimit;
            } else if ( data[i] < lowerLimit ) {
                data[i] = lowerLimit;
            }
        }
        return data;
    }
}
