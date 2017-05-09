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
import java.util.Iterator;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import ubic.gemma.model.genome.gene.GeneAlias;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.gene.GeneAlias</code>.
 * 
 * @see ubic.gemma.model.genome.gene.GeneAlias
 */
public abstract class GeneAliasDaoBase extends HibernateDaoSupport implements GeneAliasDao {

    /**
     * @see GeneAliasDao#create(int, Collection)
     */
    @Override
    public Collection<? extends GeneAlias> create( final Collection<? extends GeneAlias> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneAlias.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession( new HibernateCallback<Object>() {
            @Override
            public Object doInHibernate( Session session ) throws HibernateException {
                for ( Iterator<? extends GeneAlias> entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                    create( entityIterator.next() );
                }
                return null;
            }
        } );
        return entities;
    }

    /**
     * @see GeneAliasDao#create(int transform, ubic.gemma.model.genome.gene.GeneAlias)
     */
    @Override
    public GeneAlias create( final ubic.gemma.model.genome.gene.GeneAlias geneAlias ) {
        if ( geneAlias == null ) {
            throw new IllegalArgumentException( "GeneAlias.create - 'geneAlias' can not be null" );
        }
        this.getHibernateTemplate().save( geneAlias );
        return geneAlias;
    }

    @Override
    public Collection<? extends GeneAlias> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from GeneAliasImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see GeneAliasDao#load(int, java.lang.Long)
     */
    @Override
    public GeneAlias load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "GeneAlias.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.genome.gene.GeneAliasImpl.class, id );
        return ( ubic.gemma.model.genome.gene.GeneAlias ) entity;
    }

    /**
     * @see GeneAliasDao#loadAll(int)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<GeneAlias> loadAll() {
        final Collection<?> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.gene.GeneAliasImpl.class );
        return ( Collection<GeneAlias> ) results;
    }

    /**
     * @see GeneAliasDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "GeneAlias.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.gene.GeneAlias entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see GeneAliasDao#remove(Collection)
     */
    @Override
    public void remove( Collection<? extends GeneAlias> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneAlias.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see GeneAliasDao#remove(ubic.gemma.model.genome.gene.GeneAlias)
     */
    @Override
    public void remove( ubic.gemma.model.genome.gene.GeneAlias geneAlias ) {
        if ( geneAlias == null ) {
            throw new IllegalArgumentException( "GeneAlias.remove - 'geneAlias' can not be null" );
        }
        this.getHibernateTemplate().delete( geneAlias );
    }

    /**
     * @see GeneAliasDao#update(Collection)
     */
    @Override
    public void update( final Collection<? extends GeneAlias> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneAlias.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( Session session ) throws HibernateException {
                        for ( Iterator<?> entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.genome.gene.GeneAlias ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see GeneAliasDao#update(ubic.gemma.model.genome.gene.GeneAlias)
     */
    @Override
    public void update( ubic.gemma.model.genome.gene.GeneAlias geneAlias ) {
        if ( geneAlias == null ) {
            throw new IllegalArgumentException( "GeneAlias.update - 'geneAlias' can not be null" );
        }
        this.getHibernateTemplate().update( geneAlias );
    }

}