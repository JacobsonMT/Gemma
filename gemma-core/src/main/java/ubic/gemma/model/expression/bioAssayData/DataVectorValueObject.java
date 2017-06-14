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

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * @author paul
 */
public abstract class DataVectorValueObject extends IdentifiableValueObject<DataVector> implements Serializable {

    private static final long serialVersionUID = 4291090102921066018L;

    static ByteArrayConverter byteArrayConverter;

    static {
        byteArrayConverter = new ByteArrayConverter();
    }

    protected CompositeSequenceValueObject designElement;
    protected QuantitationTypeValueObject quantitationType;
    protected ExpressionExperimentValueObject expressionExperiment;

    private Collection<Long> genes;
    private BioAssayDimensionValueObject bioAssayDimension;

    /* ********************************
     * Constructors
     * ********************************/

    public DataVectorValueObject( Long id ) {
        super( id );
    }

    public DataVectorValueObject( DesignElementDataVector dedv, BioAssayDimensionValueObject badvo ) {
        super( dedv.getId() );
        if ( badvo == null ) {
            BioAssayDimension badim = dedv.getBioAssayDimension();
            this.bioAssayDimension = new BioAssayDimensionValueObject( badim );
        } else {
            this.bioAssayDimension = badvo;
        }
        assert !this.bioAssayDimension.getBioAssays().isEmpty();
        this.quantitationType = new QuantitationTypeValueObject( dedv.getQuantitationType() );
        this.designElement = new CompositeSequenceValueObject( dedv.getDesignElement() );
        this.expressionExperiment = new ExpressionExperimentValueObject( dedv.getExpressionExperiment(), true );
    }

    public DataVectorValueObject( DesignElementDataVector dedv, Collection<Long> genes,
            BioAssayDimensionValueObject badvo ) {
        this( dedv, badvo );
        this.genes = genes;
    }

    /* ********************************
     * Object override methods
     * ********************************/

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        final DoubleVectorValueObject other = ( DoubleVectorValueObject ) obj;
        if ( id == null ) {
            return false;
        } else if ( !id.equals( other.id ) )
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
    }

    @Override
    public String toString() {
        return "EE=" + this.expressionExperiment.getId() + " Probe=" + this.designElement.getId();
    }

    /* ********************************
     * Public methods
     * ********************************/

    /**
     * Represents the order of the bioassays for this. It might not be a real (persistent) BioAssayDimension: it might
     * be a subset, or a "padded" one.
     *
     * @return
     */
    public BioAssayDimensionValueObject getBioAssayDimension() {
        return this.bioAssayDimension;
    }

    public void setBioAssayDimension( BioAssayDimensionValueObject bioAssayDimension ) {
        this.bioAssayDimension = bioAssayDimension;
    }

    public List<BioAssayValueObject> getBioAssays() {
        assert bioAssayDimension != null;
        return bioAssayDimension.getBioAssays();
    }

    public CompositeSequenceValueObject getDesignElement() {
        return designElement;
    }

    public void setDesignElement( CompositeSequenceValueObject designElement ) {
        this.designElement = designElement;
    }

    public ExpressionExperimentValueObject getExpressionExperiment() {
        return expressionExperiment;
    }

    public void setExpressionExperiment( ExpressionExperimentValueObject expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    /**
     * @return the genes
     */
    public Collection<Long> getGenes() {
        return genes;
    }

    /**
     * @param genes the genes to set
     */
    public void setGenes( Collection<Long> genes ) {
        this.genes = genes;
    }

    public QuantitationTypeValueObject getQuantitationType() {
        return quantitationType;
    }

    public void setQuantitationType( QuantitationTypeValueObject quantitationType ) {
        this.quantitationType = quantitationType;
    }



}
