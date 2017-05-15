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
package ubic.gemma.persistence.service.genome;

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import ubic.gemma.model.genome.PhysicalLocation;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.PhysicalLocation</code>.
 * 
 * @see ubic.gemma.model.genome.PhysicalLocation
 */
public abstract class PhysicalLocationDaoBase extends HibernateDaoSupport implements PhysicalLocationDao {

    /**
     * @see PhysicalLocationDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends PhysicalLocation> create(
            final java.util.Collection<? extends PhysicalLocation> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "PhysicalLocation.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends PhysicalLocation> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see PhysicalLocationDao#create(int transform, ubic.gemma.model.genome.PhysicalLocation)
     */
    @Override
    public PhysicalLocation create( final ubic.gemma.model.genome.PhysicalLocation physicalLocation ) {
        if ( physicalLocation == null ) {
            throw new IllegalArgumentException( "PhysicalLocation.create - 'physicalLocation' can not be null" );
        }
        this.getHibernateTemplate().save( physicalLocation );
        return physicalLocation;
    }

    /**
     * @see PhysicalLocationDao#load(int, java.lang.Long)
     */
    @Override
    public PhysicalLocation load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "PhysicalLocation.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.genome.PhysicalLocationImpl.class, id );
        return ( ubic.gemma.model.genome.PhysicalLocation ) entity;
    }

    /**
     * @see PhysicalLocationDao#loadAll(int)
     */
    @SuppressWarnings("unchecked")
    @Override
    public java.util.Collection<PhysicalLocation> loadAll() {
        final java.util.Collection<?> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.PhysicalLocationImpl.class );
        return ( Collection<PhysicalLocation> ) results;
    }

    /**
     * @see PhysicalLocationDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "PhysicalLocation.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.PhysicalLocation entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeLocationDao#remove(java.util.Collection)
     */
    @Override
    public void remove( java.util.Collection<? extends PhysicalLocation> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "PhysicalLocation.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see PhysicalLocationDao#remove(ubic.gemma.model.genome.PhysicalLocation)
     */
    @Override
    public void remove( ubic.gemma.model.genome.PhysicalLocation physicalLocation ) {
        if ( physicalLocation == null ) {
            throw new IllegalArgumentException( "PhysicalLocation.remove - 'physicalLocation' can not be null" );
        }
        this.getHibernateTemplate().delete( physicalLocation );
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeLocationDao#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection<? extends PhysicalLocation> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "PhysicalLocation.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends PhysicalLocation> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see PhysicalLocationDao#update(ubic.gemma.model.genome.PhysicalLocation)
     */
    @Override
    public void update( ubic.gemma.model.genome.PhysicalLocation physicalLocation ) {
        if ( physicalLocation == null ) {
            throw new IllegalArgumentException( "PhysicalLocation.update - 'physicalLocation' can not be null" );
        }
        this.getHibernateTemplate().update( physicalLocation );
    }

}