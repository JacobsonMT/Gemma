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
package ubic.gemma.model.genome.gene;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.BusinessKey;

/**
 * @see ubic.gemma.model.genome.gene.GeneProduct
 * @author pavlidis
 * @version $Id$
 */
public class GeneProductDaoImpl extends ubic.gemma.model.genome.gene.GeneProductDaoBase {

    private static Log log = LogFactory.getLog( GeneProductDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneProductDaoBase#find(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @SuppressWarnings("unchecked")
    @Override
    public GeneProduct find( GeneProduct geneProduct ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( GeneProduct.class );

            BusinessKey.checkValidKey( geneProduct );

            BusinessKey.createQueryObject( queryObject, geneProduct );

            log.debug( queryObject );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {

                    /*
                     * This happens in some cases where NCBI has the same RNA mapped to multiple genes. Example:
                     * BC016940 maps to OR2A20P and OR2A9P (both are pseudogenes in this case)
                     */

                    Gene gene = geneProduct.getGene();
                    Collections.sort( results, new Comparator<GeneProduct>() {
                        public int compare( GeneProduct arg0, GeneProduct arg1 ) {
                            return arg0.getId().compareTo( arg1.getId() );
                        }
                    } );
                    if ( gene != null ) {
                        GeneProduct keeper = null;
                        int numFound = 0;
                        for ( Object object : results ) {
                            GeneProduct candidateMatch = ( GeneProduct ) object;

                            if ( candidateMatch.getGene().equals( gene ) ) {
                                keeper = candidateMatch;
                                numFound++;
                            } else if ( candidateMatch.getPhysicalLocation() != null
                                    && geneProduct.getPhysicalLocation() != null
                                    && candidateMatch.getPhysicalLocation().nearlyEquals(
                                            geneProduct.getPhysicalLocation() ) ) {
                                keeper = candidateMatch;
                                numFound++;
                            }
                        }

                        if ( numFound == 1 ) {
                            // not so bad, we figured out a match.
                            log.warn( "Multiple gene products match " + geneProduct
                                    + ", but only one for the right gene (" + gene + "), returning " + keeper );
                            return keeper;
                        }

                        if ( numFound == 0 ) {
                            log.error( "Multiple gene products match " + geneProduct + ", but none with " + gene );
                            debug( results );
                            log.error( "Returning arbitrary match " + results.iterator().next() );
                            return ( GeneProduct ) results.iterator().next();
                        }

                        if ( numFound > 1 ) {
                            log.error( "Multiple gene products match " + geneProduct + ", and matches " + numFound
                                    + " genes" );
                            debug( results );
                            log.error( "Returning arbitrary match " + results.iterator().next() );
                            return ( GeneProduct ) results.iterator().next();
                        }
                    }

                    // throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    // "More than one instance of '" + geneProduct + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            log.debug( "Found: " + result );
            return ( GeneProduct ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneProductDaoBase#findOrCreate(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    public GeneProduct findOrCreate( GeneProduct geneProduct ) {
        GeneProduct existingGeneProduct = this.find( geneProduct );
        if ( existingGeneProduct != null ) {
            return existingGeneProduct;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new geneProduct: " + geneProduct.getName() );
        return ( GeneProduct ) create( geneProduct );
    }

    /*
     * (non-Javadoc)
     * @seeubic.gemma.model.genome.gene.GeneProductDao#geneProductValueObjectToEntity(ubic.gemma.model.genome.gene.
     * GeneProductValueObject)
     */
    public GeneProduct geneProductValueObjectToEntity( GeneProductValueObject geneProductValueObject ) {
        final String queryString = "select distinct gp from GeneProductImpl gp where gp.id = :id";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setLong( "id", geneProductValueObject.getId() );
            java.util.List results = queryObject.list();

            if ( ( results == null ) || ( results.size() == 0 ) ) return null;

            return ( GeneProduct ) results.iterator().next();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from GeneProductImpl";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( query );

            return ( Integer ) queryObject.iterate().next();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneProductDaoBase#handleGetGenesByName(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetGenesByName( String search ) throws Exception {
        Collection<Gene> genes = null;
        final String queryString = "select distinct gene from GeneImpl as gene inner join gene.products gp where  gp.name = :search";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setString( "search", search );
            genes = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        return genes;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneProductDaoBase#handleGetGenesByNcbiId(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetGenesByNcbiId( String search ) throws Exception {
        Collection<Gene> genes = null;
        final String queryString = "select distinct gene from GeneImpl as gene inner join gene.products gp where gp.ncbiId = :search";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setString( "search", search );
            genes = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        return genes;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneProductDaoBase#handleLoad(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleLoad( Collection ids ) throws Exception {
        Collection<GeneProduct> geneProducts = null;
        final String queryString = "select distinct gp from GeneProductImpl gp where gp.id in (:ids)";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameterList( "ids", ids );
            geneProducts = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return geneProducts;
    }

    /**
     * @param results
     */
    private void debug( List results ) {

        StringBuilder buf = new StringBuilder();
        buf.append( "\n" );
        for ( Object o : results ) {
            buf.append( o + "\n" );
        }
        log.error( buf );

    }
}