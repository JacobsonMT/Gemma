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

import java.util.Collection;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * @author paul
 * @version $Id$
 */
public abstract class DataVectorValueObject {

    protected static ByteArrayConverter byteArrayConverter;

    static {
        byteArrayConverter = new ByteArrayConverter();
    }

    public DataVectorValueObject() {
    }

    protected Long id;
    protected DesignElement designElement;
    protected BioAssayDimension bioAssayDimension;
    protected QuantitationType quantitationType;
    protected ExpressionExperiment expressionExperiment;
    Collection<Gene> genes;

    public DataVectorValueObject( DesignElementDataVector dedv ) {
        this.bioAssayDimension = dedv.getBioAssayDimension();
        this.quantitationType = dedv.getQuantitationType();
        this.designElement = dedv.getDesignElement();
        this.expressionExperiment = dedv.getExpressionExperiment();
        this.id = dedv.getId();
    }

    public DataVectorValueObject( DesignElementDataVector dedv, Collection<Gene> genes ) {
        this( dedv );
        this.genes = genes;
    }

    public BioAssayDimension getBioAssayDimension() {
        return bioAssayDimension;
    }

    public void setBioAssayDimension( BioAssayDimension bioAssayDimension ) {
        this.bioAssayDimension = bioAssayDimension;
    }

    public QuantitationType getQuantitationType() {
        return quantitationType;
    }

    public void setQuantitationType( QuantitationType quantitationType ) {
        this.quantitationType = quantitationType;
    }

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        final DoubleVectorValueObject other = ( DoubleVectorValueObject ) obj;
        if ( id == null ) {
            return false;
        } else if ( !id.equals( other.id ) ) return false;
        return true;
    }

    public DesignElement getDesignElement() {
        return designElement;
    }

    public void setDesignElement( DesignElement designElement ) {
        this.designElement = designElement;
    }

    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

    /**
     * @return the genes
     */
    public Collection<Gene> getGenes() {
        return genes;
    }

    @Override
    public String toString() {
        return "EE=" + this.expressionExperiment.getId() + " Probe=" + this.designElement.getId();
    }

    /**
     * @param genes the genes to set
     */
    public void setGenes( Collection<Gene> genes ) {
        this.genes = genes;
    }

}
