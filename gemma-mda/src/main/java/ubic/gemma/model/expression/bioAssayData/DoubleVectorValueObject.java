/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.model.expression.bioAssayData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import ubic.basecode.math.DescriptiveWithMissing;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import cern.colt.list.DoubleArrayList;

/**
 * Simple wrapper for a double[] that is derived from a DesignElementDataVector.
 * 
 * @author paul
 * @version $Id$
 */
public class DoubleVectorValueObject extends DataVectorValueObject {

    @Override
    public int hashCode() {
        final int prime = 31;
        if ( super.getId() != null ) return super.hashCode();

        int result = prime * ( ( sourceVectorId == null ) ? 0 : sourceVectorId.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( id != null ) {
            return super.equals( obj );
        }

        if ( this == obj ) return true;
        if ( !super.equals( obj ) ) return false;
        if ( getClass() != obj.getClass() ) return false;

        DoubleVectorValueObject other = ( DoubleVectorValueObject ) obj;
        if ( sourceVectorId == null ) {
            if ( other.sourceVectorId != null ) return false;
        } else if ( !sourceVectorId.equals( other.sourceVectorId ) ) return false;
        return true;
    }

    private static final long serialVersionUID = -5116242513725297615L;
    private double[] data = null;
    private boolean masked = false;

    private boolean reorganized = false;
    private boolean sliced = false;

    public boolean isSliced() {
        return sliced;
    }

    private Double pvalue;
    private Double rank;

    private Double rankByMax;

    private Double rankByMean;

    /**
     * Will only be non-null if the id is null, when we have a 'slice' of the data
     */
    private Long sourceVectorId = null;

    /**
     * @param dedv
     */
    public DoubleVectorValueObject( DesignElementDataVector dedv ) {
        this( dedv, null );
    }

    /**
     * @param dedv
     * @param genes
     */
    public DoubleVectorValueObject( DesignElementDataVector dedv, Collection<Long> genes ) {
        super( dedv, genes );
        QuantitationType qt = dedv.getQuantitationType();
        if ( !qt.getRepresentation().equals( PrimitiveType.DOUBLE ) ) {
            throw new IllegalArgumentException( "Can only store double vectors, got " + qt + " "
                    + qt.getRepresentation() );
        }
        if ( qt.getIsMaskedPreferred() ) {
            this.masked = true;
        }
        this.data = byteArrayConverter.byteArrayToDoubles( dedv.getData() );
        if ( dedv instanceof ProcessedExpressionDataVector ) {
            this.rankByMax = ( ( ProcessedExpressionDataVector ) dedv ).getRankByMax();
            this.rankByMean = ( ( ProcessedExpressionDataVector ) dedv ).getRankByMean();
        }
    }

    /**
     * Create a vector where we expect to have to create one or more gaps to match other vectors, defined by dimToMatch.
     * 
     * @param dedv
     * @param genes
     * @param dimToMatch ensure that the vector missing values to match the locations of any bioassays in dimToMatch
     *        that aren't in the dedv's bioAssayDimension.
     */
    public DoubleVectorValueObject( DesignElementDataVector dedv, Collection<Long> genes, BioAssayDimension dimToMatch ) {
        this( dedv, genes );

        if ( dimToMatch.getBioAssays().size() != this.data.length ) {
            addGaps( dimToMatch );
        }

    }

    /**
     * Create a vector that is a slice of another one. The bioassays chosen are as given in the supplied
     * bioassaydimension.
     * 
     * @param bioassayset, possibly a subset, which we are going to slice.
     * @param vec
     * @param bad all we nee is the id, the name and the list of bioassays from this.S
     */
    public DoubleVectorValueObject( BioAssaySet bioassayset, DoubleVectorValueObject vec,
            BioAssayDimensionValueObject bad ) {
        this.masked = vec.masked;
        this.rankByMax = vec.rankByMax;
        this.rankByMean = vec.rankByMean;
        this.setGenes( vec.getGenes() );
        this.setDesignElement( vec.getDesignElement() );

        if ( !bioassayset.getId().equals( vec.getExpressionExperiment().getId() ) ) {
            this.expressionExperiment = new ExpressionExperimentValueObject( bioassayset );
        } else {
            this.expressionExperiment = vec.getExpressionExperiment();
        }

        this.setId( null ); // because this is a 'slice', not a persistent one.
        this.setQuantitationType( vec.getQuantitationType() );

        this.setBioAssayDimension( bad );

        this.sourceVectorId = vec.getId(); // so we can track this!
        this.sliced = true;
        this.data = new double[bad.getBioAssays().size()];

        Collection<Double> values = new ArrayList<Double>();
        int i = 0;
        for ( BioAssayValueObject ba : vec.getBioAssays() ) {
            if ( this.getBioAssays().contains( ba ) ) {
                values.add( vec.getData()[i] );
            }
            i++;
        }

        this.data = ArrayUtils.toPrimitive( values.toArray( new Double[] {} ) );
    }

    public double[] getData() {
        return data;
    }

    public Double getPvalue() {
        return pvalue;
    }

    public Double getRank() {
        return rank;
    }

    /**
     * If this returns non-null, it means the vector is a slice of another vector identified by the return value.
     * 
     * @return
     */
    public Long getSourceVectorId() {
        return sourceVectorId;
    }

    public boolean isMasked() {
        return masked;
    }

    /**
     * @return true if the data has been rearranged relative to the bioassaydimension (as a matter of practice the
     *         bioassaydimension should be nulled if it is not valid; this boolean is an additional check)
     */
    public boolean isReorganized() {
        return reorganized;
    }

    public void setMasked( boolean masked ) {
        this.masked = masked;
    }

    public void setPvalue( Double pvalue ) {
        this.pvalue = pvalue;
    }

    public void setRank( Double rank ) {
        this.rank = rank;
    }

    public void setReorganized( boolean reorganized ) {
        this.reorganized = reorganized;
    }

    /**
     * @return data adjusted to mean 0, variance 1.
     */

    public double[] standardize() {
        /*
         * FIXME If the values are all equal, variance == 0 and we get nothing back. So we should fill in zeros instead.
         */

        /*
         * DoubleArrayList constructor does not make a copy, so we have to make one.
         */
        double[] copy = new double[this.data.length];
        for ( int i = 0; i < data.length; i++ ) {
            copy[i] = data[i];
        }

        DescriptiveWithMissing.standardize( new DoubleArrayList( copy ) );
        return copy;

    }

    /**
     * @param ee
     * @param cs required
     * @param updatedQuantitationType required because this might be changed.
     * @return
     */
    public DesignElementDataVector toDesignElementDataVector( ExpressionExperiment ee, CompositeSequence cs,
            QuantitationType updatedQuantitationType ) {
        DesignElementDataVector result;
        if ( updatedQuantitationType == null ) throw new IllegalArgumentException();
        if ( this.masked ) {
            result = ProcessedExpressionDataVector.Factory.newInstance();
            ( ( ProcessedExpressionDataVector ) result ).setRankByMax( rankByMax );
            ( ( ProcessedExpressionDataVector ) result ).setRankByMean( rankByMean );
        } else {
            result = RawExpressionDataVector.Factory.newInstance();
        }
        result.setExpressionExperiment( ee );

        result.setBioAssayDimension( this.getBioAssayDimension().getEntity() );
        assert this.getBioAssays().size() > 0;

        result.setQuantitationType( updatedQuantitationType );

        result.setDesignElement( cs );
        result.setData( byteArrayConverter.doubleArrayToBytes( this.data ) );
        return result;
    }

    /**
     * @param dimToMatch
     */
    private void addGaps( BioAssayDimension dimToMatch ) {

        List<BioAssayValueObject> dimToMatchBioAssays = new BioAssayDimensionValueObject( dimToMatch ).getBioAssays();

        double[] expandedData = new double[dimToMatch.getBioAssays().size()];
        BioAssayDimension expandedDim = BioAssayDimension.Factory.newInstance();
        expandedDim.setDescription( "Expanded bioassay dimension based on " + this.getBioAssayDimension().getName() );
        expandedDim.setName( "Expanded bioassay dimension based on " + this.getBioAssayDimension().getName() );

        Map<BioMaterialValueObject, BioAssayValueObject> bmap = new HashMap<BioMaterialValueObject, BioAssayValueObject>();
        ArrayDesignValueObject arrayDesign = null;
        for ( BioAssayValueObject b : this.getBioAssays() ) {

            bmap.put( b.getSample(), b );
            arrayDesign = b.getArrayDesign();
        }

        List<BioAssayValueObject> expandedBioAssays = new ArrayList<BioAssayValueObject>();
        int i = 0;
        int indexInUngappedData = 0;
        for ( BioAssayValueObject b : dimToMatchBioAssays ) {

            BioMaterialValueObject bm = b.getSample();

            if ( !bmap.containsKey( bm ) ) {
                /*
                 * This is one where we have to put in a gap.
                 */
                expandedData[i] = Double.NaN;
                BioAssayValueObject placeholder = new BioAssayValueObject();
                placeholder.setName( "Missing bioassay for biomaterial=" + bm + " that was not run on " + arrayDesign );
                placeholder
                        .setDescription( "This is to represent a biomaterial that was not run on the platform for the rest of the bioassaydimension." );
                placeholder.setArrayDesign( arrayDesign );
                placeholder.setSample( bm );
                expandedBioAssays.add( placeholder );
            } else {
                expandedBioAssays.add( this.getBioAssays().get( indexInUngappedData ) );
                expandedData[i] = data[indexInUngappedData];
                indexInUngappedData++;
            }
            i++;
        }

        this.data = expandedData;
        this.setBioAssayDimension( new BioAssayDimensionValueObject() );
        this.getBioAssayDimension().setBioAssays( expandedBioAssays );
        this.getBioAssayDimension().setId( null );
        this.getBioAssayDimension().setName(
                "Expanded bioassay dimension based on " + this.getBioAssayDimension().getName() );
        assert this.getBioAssays() != null;
    }
}
