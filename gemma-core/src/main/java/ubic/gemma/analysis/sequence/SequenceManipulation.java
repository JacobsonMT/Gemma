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
package ubic.gemma.analysis.sequence;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.Reporter;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;

/**
 * Convenient methods for manipulating BioSequences and PhysicalLocations
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SequenceManipulation {
    protected static final Log log = LogFactory.getLog( SequenceManipulation.class );

    /**
     * Puts "chr" prefix on the chromosome name, if need be.
     * 
     * @param chromosome
     * @return
     */
    public static String blatFormatChromosomeName( String chromosome ) {
        String searchChrom = chromosome;
        if ( !searchChrom.startsWith( "chr" ) ) searchChrom = "chr" + searchChrom;
        return searchChrom;
    }

    /**
     * Convert a psl-formatted list (comma-delimited) to an int[].
     * 
     * @param blatLocations
     * @return
     */
    public static int[] blatLocationsToIntArray( String blatLocations ) {
        if ( blatLocations == null ) return null;
        String[] strings = blatLocations.split( "," );
        int[] result = new int[strings.length];
        for ( int i = 0; i < strings.length; i++ ) {
            try {
                result[i] = Integer.parseInt( strings[i] );
            } catch ( NumberFormatException e ) {
                throw new RuntimeException( "Could not parse integer blat location from " + strings[i], e );
            }
        }
        return result;
    }

    /**
     * @param sequences
     * @return BioSequence. Not all fields are filled in and must be set by the caller.
     */
    public static BioSequence collapse( Collection<Reporter> sequences ) {
        Collection<Reporter> copyOfSequences = copyReporters( sequences );
        BioSequence collapsed = BioSequence.Factory.newInstance();
        collapsed.setSequence( "" );
        while ( !copyOfSequences.isEmpty() ) {
            Reporter next = findLeftMostProbe( copyOfSequences );
            int ol = SequenceManipulation.rightHandOverlap( collapsed, next.getImmobilizedCharacteristic() );
            String nextSeqStr = next.getImmobilizedCharacteristic().getSequence();
            collapsed.setSequence( collapsed.getSequence() + nextSeqStr.substring( ol ) );
            copyOfSequences.remove( next );
        }
        collapsed.setIsCircular( false );
        collapsed.setIsApproximateLength( false );
        collapsed.setLength( new Long( collapsed.getSequence().length() ) );
        collapsed.setDescription( "Collapsed from " + sequences.size() + " reporter sequences" );
        return collapsed;
    }

    /**
     * Convert a CompositeSequence's immobilizedCharacteristics into a single sequence, using a simple merge-join
     * strategy.
     * 
     * @return
     */
    public static BioSequence collapse( CompositeSequence compositeSequence ) {
        return collapse( compositeSequence.getComponentReporters() );
    }

    /**
     * Removes "chr" prefix from the chromosome name, if it is there.
     * 
     * @param chromosome
     * @return
     */
    public static String deBlatFormatChromosomeName( String chromosome ) {
        String searchChrom = chromosome;
        if ( searchChrom.startsWith( "chr" ) ) searchChrom = searchChrom.substring( 3 );
        return searchChrom;
    }

    /**
     * Find where the center of a query location is in a gene. This is defined as the location of the center base of the
     * query sequence relative to the 3' end of the gene.
     * 
     * @param starts
     * @param sizes
     * @param gene
     * @return
     */
    public static int findCenter( String starts, String sizes ) {

        int[] startArray = blatLocationsToIntArray( starts );
        int[] sizesArray = blatLocationsToIntArray( sizes );

        return findCenterExonCenterBase( startArray, sizesArray );

    }

    /**
     * Given a gene, find out how much of it overlaps with exons provided as starts and sizes. This could involve more
     * than one exon.
     * <p>
     * 
     * @param chromosome, as "chrX" or "X".
     * @param starts of the locations we are testing.
     * @param sizes of the locations we are testing.
     * @param strand to consider. If null, strandedness is ignored.
     * @param gene Gene we are testing
     * @return Number of bases which overlap with exons of the gene. A value of zero indicates that the location is
     *         entirely within an intron. If multiple GeneProducts are associated with this gene, the best (highest)
     *         overlap is reported).
     */
    public static int getGeneExonOverlaps( String chromosome, String starts, String sizes, String strand, Gene gene ) {
        if ( gene == null ) {
            log.warn( "Null gene" );
            return 0;
        }

        if ( gene.getPhysicalLocation().getChromosome() != null
                && !gene.getPhysicalLocation().getChromosome().getName().equals(
                        deBlatFormatChromosomeName( chromosome ) ) ) return 0;

        int bestOverlap = 0;
        for ( GeneProduct geneProduct : gene.getProducts() ) {
            int overlap = getGeneProductExonOverlap( starts, sizes, strand, geneProduct );
            if ( overlap > bestOverlap ) {
                bestOverlap = overlap;
            }
        }
        return bestOverlap;
    }

    /**
     * Compute the overlap of a physical location with a transcript (gene product).
     * 
     * @param starts of the locations we are testing.
     * @param sizes of the locations we are testing.
     * @param strand the strand to look on. If null, strand is ignored.
     * @param geneProduct GeneProduct we are testing. If strand of PhysicalLocation is null, we ignore strand.
     * @return Total number of bases which overlap with exons of the transcript. A value of zero indicates that the
     *         location is entirely within an intron.
     */
    public static int getGeneProductExonOverlap( String starts, String sizes, String strand, GeneProduct geneProduct ) {

        if ( starts == null || sizes == null || geneProduct == null )
            throw new IllegalArgumentException( "Null data" );

        // If strand is null we don't bother looking at it.
        if ( strand != null && geneProduct.getPhysicalLocation().getStrand() != null
                && geneProduct.getPhysicalLocation().getStrand() != strand ) {
            return 0;
        }

        Collection<PhysicalLocation> exons = geneProduct.getExons();

        int[] startArray = blatLocationsToIntArray( starts );
        int[] sizesArray = blatLocationsToIntArray( sizes );

        // this was happening when data was truncated by the database!
        assert startArray.length == sizesArray.length : startArray.length + " starts and " + sizesArray.length
                + " sizes (expected equal numbers)";

        int totalOverlap = 0;
        int totalLength = 0;
        for ( int i = 0; i < sizesArray.length; i++ ) {
            int start = startArray[i];
            int end = start + sizesArray[i];
            for ( PhysicalLocation exonLocation : exons ) {
                int exonStart = exonLocation.getNucleotide().intValue();
                int exonEnd = exonLocation.getNucleotide().intValue() + exonLocation.getNucleotideLength().intValue();
                totalOverlap += computeOverlap( start, end, exonStart, exonEnd );
            }
            totalLength += end - start;
        }

        if ( totalOverlap > totalLength )
            log.warn( "More overlap than length of sequence, trimming because " + totalOverlap + " > " + totalLength );

        return Math.min( totalOverlap, totalLength );
    }

    /**
     * Compute just any overlap the compare sequence has with the target on the right side.
     * 
     * @param query
     * @param target
     * @return The index of the end of the overlap. If zero, there is no overlap. In other words, this is the amount
     *         that needs to be trimmed off the compare sequence if we are going to join it on to the target without
     *         generating redundancy.
     */
    public static int rightHandOverlap( BioSequence target, BioSequence query ) {

        if ( target == null || query == null ) throw new IllegalArgumentException( "Null parameters" );

        String targetString = target.getSequence();
        String queryString = query.getSequence();

        if ( targetString == null ) throw new IllegalArgumentException( "Target sequence was empty" );
        if ( queryString == null ) throw new IllegalArgumentException( "Query sequence was empty" );

        // match the end of the target with the beginning of the query. We start with the whole thing
        for ( int i = 0; i < targetString.length(); i++ ) {
            String targetSub = targetString.substring( i );

            if ( queryString.indexOf( targetSub ) == 0 ) {
                return targetSub.length();
            }
        }

        return 0;
    }

    /**
     * @param sizes Blat-formatted string of sizes (comma-delimited)
     * @return
     */
    public static int totalSize( String sizes ) {
        return totalSize( blatLocationsToIntArray( sizes ) );
    }

    /**
     * 1. exon ---------------<br>
     * &nbsp; &nbsp; &nbsp;input&nbsp;&nbsp;&nbsp; -------<br>
     * 2. exon &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;-----------<br>
     * &nbsp; &nbsp; &nbsp;input -----------<br>
     * 3. exon ---------------<br>
     * &nbsp; &nbsp; &nbsp; input &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;---------<br>
     * 4. exon&nbsp;&nbsp;&nbsp;&nbsp; -------<br>
     * &nbsp; &nbsp; &nbsp;input ---------------- <br>
     * 
     * @param starta
     * @param enda
     * @param startb
     * @param endb
     * @return
     */
    private static int computeOverlap( long starta, long enda, long startb, long endb ) {
        if ( starta > enda ) throw new IllegalArgumentException( "Start " + starta + " must be before end " + enda );
        if ( startb > endb ) throw new IllegalArgumentException( "Start " + startb + " must be before end " + endb );

        if ( log.isTraceEnabled()) {
//            log.trace( "Comparing query length " + ( enda - starta ) + ", location: " + starta + "-->" + enda
//                    + " to target length " + ( endb - startb ) + ", location: " + startb + "--->" + endb );
        }

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
        //if ( log.isTraceEnabled() ) log.trace( "Overlap=" + overlap );
        return ( int ) overlap;
    }

    /**
     * Compute the overlap between two physical locations. If both do not have a length the overlap is zero unless they
     * point to exactly the same nucleotide location, in which case the overlap is 1.
     * 
     * @param a
     * @param b
     * @return
     */
    public static int computeOverlap( PhysicalLocation a, PhysicalLocation b ) {
        if ( !a.getChromosome().equals( b.getChromosome() ) ) {
            return 0;
        }

        if ( a.getNucleotideLength() == null && b.getNucleotideLength() == null ) {
            if ( a.getNucleotide() == b.getNucleotide() ) {
                return 1;
            }
            return 0;
        }

        return computeOverlap( a.getNucleotide(), a.getNucleotide() + a.getNucleotideLength(), b.getNucleotide(), b
                .getNucleotide()
                + b.getNucleotideLength() );

    }

    /**
     * @param reporters
     * @return
     */
    private static Collection<Reporter> copyReporters( Collection<Reporter> reporters ) {
        Collection<Reporter> copyReporters = new HashSet<Reporter>();
        for ( Reporter next : reporters ) {
            Reporter copy = Reporter.Factory.newInstance();
            copy.setImmobilizedCharacteristic( next.getImmobilizedCharacteristic() );
            copy.setStartInBioChar( next.getStartInBioChar() );
            copyReporters.add( copy );
        }
        return copyReporters;
    }

    /**
     * Find the index of the aligned base in the center exon that is the center of the query.
     * 
     * @param centerExon
     * @param startArray
     * @param sizesArray
     * @return
     */
    private static int findCenterExonCenterBase( int[] startArray, int[] sizesArray ) {
        int middle = middleBase( totalSize( sizesArray ) );
        int totalSize = 0;
        for ( int i = 0; i < sizesArray.length; i++ ) {
            totalSize += sizesArray[i];
            if ( totalSize >= middle ) {
                totalSize -= sizesArray[i];
                int diff = middle - totalSize;
                return startArray[i] + diff;
            }
        }
        assert false : "Failed to find center!";
        throw new IllegalStateException( "Should not be here!" );
    }

    /**
     * From a set of Reporters, find the one with the left-most location.
     * 
     * @param copyOfProbes
     * @return
     */
    private static Reporter findLeftMostProbe( Collection<Reporter> copyOfProbes ) {
        Long minLocation = new Long( Integer.MAX_VALUE );
        Reporter next = null;
        for ( Reporter probe : copyOfProbes ) {
            Long loc = probe.getStartInBioChar();
            if ( loc <= minLocation ) {
                minLocation = loc;
                next = probe;
            }
        }
        return next;
    }

    /**
     * @param totalSize
     * @return
     */
    private static int middleBase( int totalSize ) {
        int middle = ( int ) Math.floor( totalSize / 2.0 );
        return middle;
    }

    /**
     * @param sizesArray
     * @return
     */
    private static int totalSize( int[] sizesArray ) {
        int totalSize = 0;
        for ( int i = 0; i < sizesArray.length; i++ ) {
            totalSize += sizesArray[i];
        }
        return totalSize;
    }

}
