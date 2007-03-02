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
package ubic.gemma.model.genome;

import org.apache.commons.lang.builder.CompareToBuilder;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.genome.PhysicalLocation
 */
public class PhysicalLocationImpl extends ubic.gemma.model.genome.PhysicalLocation implements Comparable {

    /**
     * 
     */
    private static final long serialVersionUID = -6580769809003779541L;

    /**
     * @see java.lang.Comparable#compareTo(Object)
     */
    @Override
    public int compareTo( Object object ) {
        PhysicalLocationImpl other = ( PhysicalLocationImpl ) object;
        return new CompareToBuilder().append( this.getChromosome().getName(), other.getChromosome().getName() ).append(
                this.getNucleotide(), other.getNucleotide() ).toComparison();
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof PhysicalLocation ) ) {
            return false;
        }
        final PhysicalLocation that = ( PhysicalLocation ) object;

        if ( this.getId() == null || that.getId() == null || !this.getId().equals( that.getId() ) ) {

            if ( !this.getChromosome().equals( that.getChromosome() ) ) {
                return false;
            }

            if ( this.getStrand() != null && that.getStrand() != null ) {
                if ( !this.getStrand().equals( that.getStrand() ) ) {
                    return false;
                }
            }

            if ( this.getNucleotide() != null && that.getNucleotide() != null ) {
                if ( !this.getNucleotide().equals( that.getNucleotide() ) ) {
                    return false;
                }
            }

            if ( this.getNucleotideLength() != null && that.getNucleotideLength() != null ) {
                if ( !this.getNucleotideLength().equals( that.getNucleotideLength() ) ) {
                    return false;
                }
            }

            return true;
        }
        return true;
    }

    /**
     * @see ubic.gemma.model.genome.PhysicalLocation#nearlyEquals(java.lang.Object)
     * @deprecated
     */
    @Override
    @Deprecated
    public boolean nearlyEquals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof PhysicalLocation ) ) {
            return false;
        }
        final PhysicalLocation that = ( PhysicalLocation ) object;

        if ( this.getId() == null || that.getId() == null || !this.getId().equals( that.getId() ) ) {
            if ( !this.getChromosome().equals( that.getChromosome() ) ) return false;

            if ( this.getStrand() != null && that.getStrand() != null && !this.getStrand().equals( that.getStrand() ) ) {
                return false;
            }

            if ( this.getNucleotide() != null && that.getNucleotide() != null && this.getNucleotideLength() != null
                    && that.getNucleotideLength() != null ) {
                long starta = this.getNucleotide();
                long enda = starta + this.getNucleotideLength();
                long startb = that.getNucleotide();
                long endb = startb + that.getNucleotideLength();

                int overlap = computeOverlap( starta, enda, startb, endb );

                if ( overlap == 0 ) {
                    return false;
                }
            }

            if ( this.getNucleotide() != null && that.getNucleotide() != null
                    && Math.abs( this.getNucleotide() - that.getNucleotide() ) > 1000L ) return false;

            return true;
        }
        return true;
    }

    private static int computeOverlap( long starta, long enda, long startb, long endb ) {
        if ( starta > enda ) throw new IllegalArgumentException( "Start " + starta + " must be before end " + enda );
        if ( startb > endb ) throw new IllegalArgumentException( "Start " + startb + " must be before end " + endb );

        long overlap = 0;
        if ( endb < starta || enda < startb ) {
            overlap = 0;
        } else if ( starta <= startb ) {
            if ( enda < endb ) {
                overlap = enda - startb; // overhang on the left
            } else {
                overlap = endb - startb; // includes entire target
            }
        } else if ( enda < endb ) { // entirely contained within target.
            overlap = enda - starta; // length of our test sequence.
        } else {
            overlap = endb - starta; // overhang on the right
        }

        assert overlap >= 0 : "Negative overlap";
        assert ( double ) overlap / ( double ) ( enda - starta ) <= 1.0 : "Overlap longer than sequence";
        // if ( log.isTraceEnabled() ) log.trace( "Overlap=" + overlap );
        return ( int ) overlap;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29;
        if ( this.getId() != null ) {
            hashCode += this.getId().hashCode();
            return hashCode;
        }

        assert this.getChromosome() != null;
        hashCode += this.getChromosome().hashCode();

        if ( this.getNucleotide() != null ) hashCode += this.getNucleotide().hashCode();

        if ( this.getNucleotideLength() != null ) hashCode += this.getNucleotideLength().hashCode();

        return hashCode;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append( this.getChromosome().getTaxon().getScientificName() + " chr " + this.getChromosome().getName() );

        if ( this.getNucleotide() != null ) buf.append( ":" + this.getNucleotide() );
        buf.append( " on " + this.getStrand() + " strand" );

        return buf.toString();
    }

    @Override
    public int computeOverlap( PhysicalLocation other ) {

        if ( this.getId() == null || other.getId() == null || !this.getId().equals( other.getId() ) ) {
            if ( !this.getChromosome().equals( other.getChromosome() ) ) return 0;

            if ( this.getStrand() != null && other.getStrand() != null && !this.getStrand().equals( other.getStrand() ) ) {
                return 0;
            }

            if ( this.getNucleotide() != null && other.getNucleotide() != null && this.getNucleotideLength() != null
                    && other.getNucleotideLength() != null ) {
                long starta = this.getNucleotide();
                long enda = starta + this.getNucleotideLength();
                long startb = other.getNucleotide();
                long endb = startb + other.getNucleotideLength();

                return computeOverlap( starta, enda, startb, endb );

            }
            return 0;
        }
        return other.getNucleotideLength(); // The two locations are the same object.
    }

    private static int _binFirstShift = 17; /* How much to shift to get to finest bin. */
    private static int _binNextShift = 3; /* How much to shift to get to next larger bin. */

    private static int binOffsetsExtended[] = { 4096 + 512 + 64 + 8 + 1, 512 + 64 + 8 + 1, 64 + 8 + 1, 8 + 1, 1, 0 };

    private static int binOffsets[] = { 512 + 64 + 8 + 1, 64 + 8 + 1, 8 + 1, 1, 0 };
    private static int BINRANGE_MAXEND_512M = ( 512 * 1024 * 1024 );
    private static int _binOffsetOldToExtended = 4681;

    private int binFromRangeStandard( int start, int end )

    /*
     * Given start,end in chromosome coordinates assign it a bin. There's a bin for each 128k segment, for each 1M
     * segment, for each 8M segment, for each 64M segment, and for each chromosome (which is assumed to be less than
     * 512M.) A range goes into the smallest bin it will fit in.
     */
    {
        int startBin = start, endBin = end - 1, i;
        startBin >>= _binFirstShift;
        endBin >>= _binFirstShift;
        for ( i = 0; i < binOffsets.length; ++i ) {
            if ( startBin == endBin ) return binOffsets[i] + startBin;
            startBin >>= _binNextShift;
            endBin >>= _binNextShift;
        }
        throw new IllegalArgumentException( "start " + start + ", end " + end
                + " out of range in findBin (max is 512M)" );
    }

    private int binFromRangeExtended( int start, int end )
    /*
     * Given start,end in chromosome coordinates assign it a bin. There's a bin for each 128k segment, for each 1M
     * segment, for each 8M segment, for each 64M segment, for each 512M segment, and one top level bin for 4Gb. Note,
     * since start and end are int's, the practical limit is up to 2Gb-1, and thus, only four result bins on the second
     * level. A range goes into the smallest bin it will fit in.
     */
    {
        int startBin = start, endBin = end - 1, i;
        startBin >>= _binFirstShift;
        endBin >>= _binFirstShift;
        for ( i = 0; i < binOffsetsExtended.length; ++i ) {
            if ( startBin == endBin ) return _binOffsetOldToExtended + binOffsetsExtended[i] + startBin;
            startBin >>= _binNextShift;
            endBin >>= _binNextShift;
        }
        throw new IllegalArgumentException( "start " + start + ", end " + end
                + " out of range in findBin (max is 512M)" );
    }

    public int binFromRange( int start, int end )
    /* return bin that this start-end segment is in */
    {
        if ( end <= BINRANGE_MAXEND_512M )
            return binFromRangeStandard( start, end );
        else
            return binFromRangeExtended( start, end );
    }

}