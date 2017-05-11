/*
 * The gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.analysis.expression.coexpression;

/**
 * TODO Document Me
 * 
 * @author Paul
 * @version $Id$
 */
public abstract class CoexpCorrelationDistribution {

    public static final class Factory {
        public static CoexpCorrelationDistribution newInstance() {
            return new CoexpCorrelationDistributionImpl();
        }
    }

    private byte[] binCounts;

    private Long id;

    private Integer numBins;

    /**
     * Returns <code>true</code> if the argument is an PvalueDistribution instance and all identifiers for this entity
     * equal the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof CoexpCorrelationDistribution ) ) {
            return false;
        }
        final CoexpCorrelationDistribution that = ( CoexpCorrelationDistribution ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    public byte[] getBinCounts() {
        return binCounts;
    }

    public Long getId() {
        return id;
    }

    public Integer getNumBins() {
        return numBins;
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    public void setBinCounts( byte[] binCounts ) {
        this.binCounts = binCounts;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setNumBins( Integer numBins ) {
        this.numBins = numBins;
    }
}
