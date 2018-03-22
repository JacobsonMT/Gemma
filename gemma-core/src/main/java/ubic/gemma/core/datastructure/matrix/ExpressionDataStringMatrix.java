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
package ubic.gemma.core.datastructure.matrix;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.basecode.dataStructure.matrix.StringMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author pavlidis
 */
public class ExpressionDataStringMatrix extends BaseExpressionDataMatrix<String> {

    private static final long serialVersionUID = 1L;
    private static final Log log = LogFactory.getLog( ExpressionDataStringMatrix.class.getName() );
    private StringMatrix<Integer, Integer> matrix;

    public ExpressionDataStringMatrix( Collection<? extends DesignElementDataVector> vectors ) {
        this.init();
        this.selectVectors( vectors );
        this.vectorsToMatrix( vectors );
    }

    @SuppressWarnings("unused")
    public ExpressionDataStringMatrix( Collection<? extends DesignElementDataVector> dataVectors,
            QuantitationType quantitationType ) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    public ExpressionDataStringMatrix( ExpressionExperiment expressionExperiment,
            Collection<CompositeSequence> designElements, QuantitationType quantitationType ) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unused")
    public ExpressionDataStringMatrix( ExpressionExperiment expressionExperiment, QuantitationType quantitationType ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int columns() {
        return matrix.columns();
    }

    @Override
    public String get( CompositeSequence designElement, BioAssay bioAssay ) {
        int i = this.rowElementMap.get( designElement );
        int j = this.columnAssayMap.get( bioAssay );
        return this.matrix.get( i, j );
    }

    @Override
    public String get( int row, int column ) {
        return matrix.get( row, column );
    }

    @Override
    public String[][] get( List<CompositeSequence> designElements, List<BioAssay> bioAssays ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getColumn( BioAssay bioAssay ) {
        int index = this.columnAssayMap.get( bioAssay );
        return this.getColumn( index );
    }

    @Override
    public String[] getColumn( Integer index ) {
        return this.matrix.getColumn( index );
    }

    @Override
    public String[][] getColumns( List<BioAssay> bioAssays ) {
        String[][] res = new String[bioAssays.size()][];
        for ( int i = 0; i < bioAssays.size(); i++ ) {
            res[i] = this.getColumn( bioAssays.get( i ) );
        }
        return res;
    }

    @Override
    public String[][] getRawMatrix() {
        String[][] res = new String[this.rows()][];
        for ( int i = 0; i < this.rows(); i++ ) {
            res[i] = this.matrix.getRow( i );
        }
        return res;
    }

    @Override
    public String[] getRow( CompositeSequence designElement ) {
        return this.matrix.getRow( this.getRowIndex( designElement ) );
    }

    @Override
    public String[] getRow( Integer index ) {
        return matrix.getRow( index );
    }

    @Override
    public String[][] getRows( List<CompositeSequence> designElements ) {
        String[][] res = new String[this.rows()][];
        for ( int i = 0; i < designElements.size(); i++ ) {
            res[i] = this.matrix.getRow( this.getRowIndex( designElements.get( i ) ) );
        }
        return res;
    }

    @Override
    public boolean hasMissingValues() {
        for ( int i = 0; i < matrix.rows(); i++ ) {
            for ( int j = 0; j < matrix.columns(); j++ ) {
                // Note that blank strings don't count as missing.
                if ( matrix.get( i, j ) == null )
                    return true;
            }
        }
        return false;
    }

    @Override
    public int rows() {
        return matrix.rows();
    }

    @Override
    public void set( int row, int column, String value ) {
        matrix.set( row, column, value );
    }

    @Override
    protected void vectorsToMatrix( Collection<? extends DesignElementDataVector> vectors ) {
        if ( vectors == null || vectors.size() == 0 ) {
            throw new IllegalArgumentException( "No vectors!" );
        }

        int maxSize = this.setUpColumnElements();

        this.matrix = this.createMatrix( vectors, maxSize );
    }

    private StringMatrix<Integer, Integer> createMatrix( Collection<? extends DesignElementDataVector> vectors,
            int maxSize ) {

        int numRows = this.rowDesignElementMapByInteger.keySet().size();

        StringMatrix<Integer, Integer> mat = new StringMatrix<>( numRows, maxSize );

        for ( int j = 0; j < mat.columns(); j++ ) {
            mat.addColumnName( j );
        }

        // initialize the matrix to "";
        for ( int i = 0; i < mat.rows(); i++ ) {
            for ( int j = 0; j < mat.columns(); j++ ) {
                mat.set( i, j, "" );
            }
        }

        ByteArrayConverter bac = new ByteArrayConverter();
        for ( DesignElementDataVector vector : vectors ) {

            CompositeSequence designElement = vector.getDesignElement();
            assert designElement != null : "No designelement for " + vector;

            Integer rowIndex = this.rowElementMap.get( designElement );
            assert rowIndex != null;

            mat.addRowName( rowIndex );

            byte[] bytes = vector.getData();
            String[] vals = bac.byteArrayToStrings( bytes );

            BioAssayDimension dimension = vector.getBioAssayDimension();
            Collection<BioAssay> bioAssays = dimension.getBioAssays();
            assert bioAssays.size() == vals.length : "Expected " + vals.length + " got " + bioAssays.size();

            Iterator<BioAssay> it = bioAssays.iterator();

            for ( int j = 0; j < bioAssays.size(); j++ ) {

                BioAssay bioAssay = it.next();
                Integer column = this.columnAssayMap.get( bioAssay );

                assert column != null;

                mat.setByKeys( rowIndex, column, vals[j] );
            }

        }

        ExpressionDataStringMatrix.log.debug( "Created a " + mat.rows() + " x " + mat.columns() + " matrix" );
        return mat;
    }

}
