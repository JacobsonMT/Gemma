/*
 * The Gemma project.
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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.expression.experiment;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;

/**
 * @author pavlidis
 * @version $Id$
 */
public class FactorValueImpl extends ubic.gemma.model.expression.experiment.FactorValue {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -5395878022298281346L;

    @Override
    public boolean equals( Object object ) {
        if ( object == null ) return false;
        if ( this == object ) return true;
        if ( !( object instanceof FactorValue ) ) return false;
        FactorValue that = ( FactorValue ) object;
        if ( this.getId() != null && that.getId() != null ) return this.getId().equals( that.getId() );

        /*
         * at this point, we know we have two FactorValues, at least one of which is transient, so we have to look at
         * the fields; to do this, just compare the hashcodes, which already incorporate all of the important fields...
         */
        return ObjectUtils.equals( this.getExperimentalFactor(), that.getExperimentalFactor() )
                && ObjectUtils.equals( this.getMeasurement(), that.getMeasurement() )
                && ObjectUtils.equals( this.getCharacteristics(), that.getCharacteristics() );
    }

    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder( 17, 7 ).append( this.getId() ).append(
                this.getExperimentalFactor() ).append( this.getMeasurement() );
        if ( this.getCharacteristics() != null ) {
            for ( Characteristic c : this.getCharacteristics() ) {
                if ( c instanceof VocabCharacteristic )
                    builder.append( ( ( VocabCharacteristic ) c ).hashCode() );
                else
                    builder.append( c.hashCode() );
            }
        }
        return builder.toHashCode();
    }

    /**
     * @see ubic.gemma.model.expression.experiment.FactorValue#toString()
     */
    @Override
    public java.lang.String toString() {
        StringBuilder buf = new StringBuilder();
        // this can be null in tests or with half-setup transient objects
        if ( this.getExperimentalFactor() != null ) buf.append( this.getExperimentalFactor().getName() + ":" );
        if ( this.getCharacteristics().size() > 0 ) {
            for ( Characteristic c : this.getCharacteristics() ) {
                buf.append( c.getValue() );
                if ( this.getCharacteristics().size() > 1 ) buf.append( " | " );
            }
        } else if ( this.getMeasurement() != null ) {
            buf.append( this.getMeasurement().getValue() );
        } else if ( StringUtils.isNotBlank( this.getValue() ) ) {
            buf.append( this.getValue() );
        }
        return buf.toString();
    }
}