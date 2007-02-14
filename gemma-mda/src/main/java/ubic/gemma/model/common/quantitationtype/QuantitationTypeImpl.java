/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.common.quantitationtype;

/**
 * @see ubic.gemma.model.common.quantitationtype.QuantitationType
 */
public class QuantitationTypeImpl extends ubic.gemma.model.common.quantitationtype.QuantitationType {

    /**
     * 
     */
    private static final long serialVersionUID = -352202738189491165L;

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof QuantitationType ) ) {
            return false;
        }
        final QuantitationType that = ( QuantitationType ) object;

        if ( that.getName() != null && this.getName() != null && !this.getName().equals( that.getName() ) ) {
            return false;
        }

        if ( this.getScale() != null && that.getScale() != null && !this.getScale().equals( that.getScale() ) ) {
            return false;
        }

        if ( !this.getIsPreferred().equals( that.getIsPreferred() ) ) {
            return false;
        }

        if ( !this.getIsRatio().equals( that.getIsRatio() ) ) {
            return false;
        }

        if ( !this.getIsNormalized().equals( that.getIsNormalized() ) ) {
            return false;
        }
        
        if ( !this.getIsBackground().equals( that.getIsBackground() ) ) {
            return false;
        }

        if ( !this.getIsBackgroundSubtracted().equals( that.getIsBackgroundSubtracted() ) ) {
            return false;
        }

        if ( this.getGeneralType() != null && that.getGeneralType() != null
                && !this.getGeneralType().equals( that.getGeneralType() ) ) {
            return false;
        }

        if ( this.getRepresentation() != null && that.getRepresentation() != null
                && !this.getRepresentation().equals( that.getRepresentation() ) ) {
            return false;
        }

        if ( this.getType() != null && that.getRepresentation() != null && !this.getType().equals( that.getType() ) ) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( this.getId() == null ? computeHashCode() : this.getId().hashCode() );
        return hashCode;
    }

    private int computeHashCode() {
        int hashCode = 0;
        if ( this.getName() != null ) {
            hashCode = hashCode + this.getName().hashCode();
        }
        if ( this.getType() != null ) {
            hashCode = hashCode + this.getType().hashCode();
        }
        if ( this.getRepresentation() != null ) {
            hashCode = hashCode + this.getRepresentation().hashCode();
        }
        if ( this.getGeneralType() != null ) {
            hashCode = hashCode + this.getGeneralType().hashCode();
        }
        if ( this.getScale() != null ) {
            hashCode = hashCode + this.getScale().hashCode();
        }
        hashCode += this.getIsBackground().hashCode();
        hashCode += this.getIsBackgroundSubtracted().hashCode();
        hashCode += this.getIsNormalized().hashCode();
        hashCode += this.getIsPreferred().hashCode();
        hashCode += this.getIsRatio().hashCode();

        return hashCode;
    }
}