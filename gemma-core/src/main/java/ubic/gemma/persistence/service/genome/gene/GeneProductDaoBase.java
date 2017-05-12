/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.genome.gene;

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneProduct;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.gene.GeneProduct</code>.
 * 
 * @see ubic.gemma.model.genome.gene.GeneProduct
 */
public abstract class GeneProductDaoBase extends HibernateDaoSupport implements GeneProductDao {

    /**
     * @see GeneProductDao#countAll()
     */
    @Override
    public java.lang.Integer countAll() {
        try {
            return this.handleCountAll();
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'GeneProductDao.countAll()' --> " + th, th );
        }
    }

    /**
     * @see GeneProductDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends GeneProduct> create(
            final java.util.Collection<? extends GeneProduct> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneProduct.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends GeneProduct> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see GeneProductDao#create(int transform, ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    public GeneProduct create( final ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        if ( geneProduct == null ) {
            throw new IllegalArgumentException( "GeneProduct.create - 'geneProduct' can not be null" );
        }
        this.getHibernateTemplate().save( geneProduct );
        return geneProduct;
    }

    /**
     * @see GeneProductDao#find(int, java.lang.String,
     *      ubic.gemma.model.genome.gene.GeneProduct)
     */

    public GeneProduct find( final java.lang.String queryString,
            final ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( geneProduct );
        argNames.add( "geneProduct" );
        java.util.Set<GeneProduct> results = new java.util.LinkedHashSet<GeneProduct>( this.getHibernateTemplate()
                .findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.genome.gene.GeneProduct"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }
        return ( GeneProduct ) result;
    }

    /**
     * @see GeneProductDao#findByNcbiId(int, java.lang.String)
     */

    @Override
    public GeneProduct findByNcbiId( final String ncbiId ) {
        return this.findByNcbiId( "from GeneProductImpl g where g.ncbiGi = :ncbiId", ncbiId );
    }

    /**
     * @see GeneProductDao#findByNcbiId(int, java.lang.String, java.lang.String)
     */

    public GeneProduct findByNcbiId( final java.lang.String queryString, final java.lang.String ncbiId ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( ncbiId );
        argNames.add( "ncbiId" );
        java.util.List<?> results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );

        if ( results.isEmpty() ) return null;
        return ( GeneProduct ) results.iterator().next();
    }

    /**
     * @see GeneProductDao#findOrCreate(int, java.lang.String,
     *      ubic.gemma.model.genome.gene.GeneProduct)
     */

    public GeneProduct findOrCreate( final java.lang.String queryString,
            final ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( geneProduct );
        argNames.add( "geneProduct" );
        java.util.Set<GeneProduct> results = new java.util.LinkedHashSet<GeneProduct>( this.getHibernateTemplate()
                .findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.genome.gene.GeneProduct"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }
        return ( GeneProduct ) result;
    }

    /**
     * @see GeneProductDao#findOrCreate(int, ubic.gemma.model.genome.gene.GeneProduct)
     */

    @Override
    public GeneProduct findOrCreate( final ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        return this
                .findOrCreate(
                        "from ubic.gemma.model.genome.gene.GeneProductImpl as geneProduct where geneProduct.geneProduct = :geneProduct",
                        geneProduct );
    }

    /**
     * @see GeneProductDao#geneProductValueObjectToEntity(ubic.gemma.model.genome.gene.GeneProductValueObject,
     *      ubic.gemma.model.genome.gene.GeneProduct)
     */
    public void geneProductValueObjectToEntity( ubic.gemma.model.genome.gene.GeneProductValueObject source,
            ubic.gemma.model.genome.gene.GeneProduct target, boolean copyIfNull ) {
        // No conversion for target.type (can't convert source.getType():java.lang.String to
        // ubic.gemma.model.genome.gene.GeneProductType)
        if ( copyIfNull || source.getNcbiId() != null ) {
            target.setNcbiGi( source.getNcbiId() );
        }
        if ( copyIfNull || source.getName() != null ) {
            target.setName( source.getName() );
        }
    }

    /**
     * @see GeneProductDao#getGenesByName(java.lang.String)
     */
    @Override
    public java.util.Collection<Gene> getGenesByName( final java.lang.String search ) {
        try {
            return this.handleGetGenesByName( search );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'GeneProductDao.getGenesByName(java.lang.String search)' --> "
                            + th, th );
        }
    }

    /**
     * @see GeneProductDao#getGenesByNcbiId(java.lang.String)
     */
    @Override
    public java.util.Collection<Gene> getGenesByNcbiId( final java.lang.String search ) {
        try {
            return this.handleGetGenesByNcbiId( search );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'GeneProductDao.getGenesByNcbiId(java.lang.String search)' --> "
                            + th, th );
        }
    }

    /**
     * @see GeneProductDao#load(int, java.lang.Long)
     */

    @Override
    public GeneProduct load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "GeneProduct.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.genome.gene.GeneProductImpl.class, id );
        return ( GeneProduct ) entity;
    }

    /**
     * @see GeneProductDao#load(java.util.Collection)
     */
    @Override
    public java.util.Collection<GeneProduct> load( final java.util.Collection<Long> ids ) {
        try {
            return this.handleLoad( ids );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'GeneProductDao.load(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see GeneProductDao#loadAll(int)
     */

    @Override
    @SuppressWarnings("unchecked")
    public java.util.Collection<? extends GeneProduct> loadAll() {
        final java.util.Collection<?> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.gene.GeneProductImpl.class );

        return ( Collection<? extends GeneProduct> ) results;
    }

    /**
     * @see GeneProductDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "GeneProduct.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.gene.GeneProduct entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    @Override
    public void remove( java.util.Collection<? extends GeneProduct> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneProduct.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see GeneProductDao#remove(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    public void remove( ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        if ( geneProduct == null ) {
            throw new IllegalArgumentException( "GeneProduct.remove - 'geneProduct' can not be null" );
        }
        this.getHibernateTemplate().delete( geneProduct );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    @Override
    public void update( final java.util.Collection<? extends GeneProduct> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneProduct.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends GeneProduct> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see GeneProductDao#update(ubic.gemma.model.genome.gene.GeneProduct)
     */
    @Override
    public void update( ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        if ( geneProduct == null ) {
            throw new IllegalArgumentException( "GeneProduct.update - 'geneProduct' can not be null" );
        }
        this.getHibernateTemplate().update( geneProduct );
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getGenesByName(java.lang.String)}
     */
    protected abstract java.util.Collection<Gene> handleGetGenesByName( java.lang.String search )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getGenesByNcbiId(java.lang.String)}
     */
    protected abstract java.util.Collection<Gene> handleGetGenesByNcbiId( java.lang.String search )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.util.Collection)}
     */
    protected abstract java.util.Collection<GeneProduct> handleLoad( java.util.Collection<Long> ids )
            throws java.lang.Exception;

}