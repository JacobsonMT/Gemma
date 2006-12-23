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
package ubic.gemma.model.genome;

import java.util.Collection;

import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

/**
 * @see ubic.gemma.model.genome.ProbeAlignedRegion
 * @author paul
 * @version $Id$
 */
public class ProbeAlignedRegionDaoImpl extends ubic.gemma.model.genome.ProbeAlignedRegionDaoBase {

    @SuppressWarnings("unchecked")
    @Override
    public Collection<ProbeAlignedRegion> find( BlatResult blatResult ) {

        Chromosome chrom = blatResult.getTargetChromosome();
        final Long targetStart = blatResult.getTargetStart();
        final Long targetEnd = blatResult.getTargetEnd();
        final String strand = blatResult.getStrand();

        return findByPosition( chrom, targetStart, targetEnd, strand );

    }

    @Override
    public Collection<ProbeAlignedRegion> findByPhysicalLocation( PhysicalLocation location ) {
        Chromosome chrom = location.getChromosome();
        final Long targetStart = location.getNucleotide();
        final Long targetEnd = location.getNucleotide() + location.getNucleotideLength();
        final String strand = location.getStrand();

        return findByPosition( chrom, targetStart, targetEnd, strand );
    }

    @SuppressWarnings("unchecked")
    private Collection<ProbeAlignedRegion> findByPosition( Chromosome chrom, final Long targetStart,
            final Long targetEnd, final String strand ) {
        Collection<ProbeAlignedRegion> result = null;
        // the 'fetch'es are so we don't get lazy loads (typical applications of this method)
        String query = "select par from ProbeAlignedRegionImpl as par inner join fetch par.physicalLocation pl "
                + "inner join fetch par.products inner join fetch pl.chromosome "
                + "where ((pl.nucleotide >= :start AND (pl.nucleotide + pl.nucleotideLength) <= :end) "
                + "OR (pl.nucleotide <= :start AND (pl.nucleotide + pl.nucleotideLength) >= :end) OR "
                + "(pl.nucleotide >= :start  AND pl.nucleotide <= :end) "
                + "OR  ((pl.nucleotide + pl.nucleotideLength) >= :start AND (pl.nucleotide + pl.nucleotideLength) <= :end )) "
                + "and pl.chromosome = :chromosome ";

        if ( strand != null ) {
            query = query + " and pl.strand = :strand ";
        }

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( query );
            queryObject.setParameter( "chromosome", chrom );
            queryObject.setParameter( "start", targetStart );
            queryObject.setParameter( "end", targetEnd );
            if ( strand != null ) queryObject.setParameter( "strand", strand );

            result = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return result;
    }

}