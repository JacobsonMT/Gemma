/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.genome;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import edu.columbia.gemma.common.protocol.ProtocolDaoImpl;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.genome.Taxon
 */
public class TaxonDaoImpl extends edu.columbia.gemma.genome.TaxonDaoBase {

    private static Log log = LogFactory.getLog( ProtocolDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.genome.TaxonDaoBase#find(edu.columbia.gemma.genome.Taxon)
     */
    @SuppressWarnings("boxing")
    @Override
    public Taxon find( Taxon taxon ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( Taxon.class );
            if ( taxon.getNcbiId() != null ) {
                queryObject.add( Restrictions.eq( "ncbiId", taxon.getNcbiId() ) );
            } else if ( taxon.getScientificName() != null ) {
                queryObject.add( Restrictions.eq( "scientificName", taxon.getScientificName() ) );
            } else if ( taxon.getCommonName() != null ) {
                queryObject.add( Restrictions.eq( "commonName", taxon.getCommonName() ) );
            } else {
                throw new IllegalArgumentException(
                        "No valid fields filled in for finding. Must supply NCBI Id, scientific name, or common name." );
            }

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + taxon.getClass().getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = ( Taxon ) results.iterator().next();
                }
            }
            return ( Taxon ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.genome.TaxonDaoBase#findOrCreate(edu.columbia.gemma.genome.Taxon)
     */
    @Override
    public Taxon findOrCreate( Taxon taxon ) {
        if ( taxon.getScientificName() == null && taxon.getCommonName() == null && taxon.getNcbiId() == null ) {
            log.warn( "taxon had no testable fields filled in : " + taxon );
            return null;
        }
        Taxon newTaxon = find( taxon );
        if ( newTaxon != null ) {
            log.debug( "Found existing taxon: " + taxon );
            return newTaxon;
        }
        log.debug( "Creating new taxon: " + taxon );
        return create( taxon );

    }

}