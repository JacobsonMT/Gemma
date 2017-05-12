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
package ubic.gemma.persistence.service.common.measurement;

import ubic.gemma.model.common.measurement.Unit;

import java.util.Collection;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.measurement.Unit</code>.
 * </p>
 * 
 * @see ubic.gemma.model.common.measurement.Unit
 */
public abstract class UnitDaoBase extends org.springframework.orm.hibernate3.support.HibernateDaoSupport implements
        UnitDao {

    /**
     * @see UnitDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends Unit> create( final java.util.Collection<? extends Unit> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Unit.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends Unit> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    @Override
    public Collection<? extends Unit> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from UnitImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see UnitDao#create(int transform, ubic.gemma.model.common.measurement.Unit)
     */
    @Override
    public Unit create( final Unit unit ) {
        if ( unit == null ) {
            throw new IllegalArgumentException( "Unit.create - 'unit' can not be null" );
        }
        this.getHibernateTemplate().save( unit );
        return unit;
    }

    /**
     * @see UnitDao#load(int, java.lang.Long)
     */
    @Override
    public Unit load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Unit.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.common.measurement.UnitImpl.class, id );
        return ( Unit ) entity;
    }

    /**
     * @see UnitDao#loadAll(int)
     */
    @Override
    public java.util.Collection<? extends Unit> loadAll() {
        final java.util.Collection<? extends Unit> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.measurement.UnitImpl.class );
        return results;
    }

    /**
     * @see UnitDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Unit.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.measurement.Unit entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see UnitDao#remove(java.util.Collection)
     */
    @Override
    public void remove( java.util.Collection<? extends Unit> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Unit.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see UnitDao#remove(ubic.gemma.model.common.measurement.Unit)
     */
    @Override
    public void remove( ubic.gemma.model.common.measurement.Unit unit ) {
        if ( unit == null ) {
            throw new IllegalArgumentException( "Unit.remove - 'unit' can not be null" );
        }
        this.getHibernateTemplate().delete( unit );
    }

    /**
     * @see UnitDao#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection<? extends Unit> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Unit.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends Unit> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see UnitDao#update(ubic.gemma.model.common.measurement.Unit)
     */
    @Override
    public void update( ubic.gemma.model.common.measurement.Unit unit ) {
        if ( unit == null ) {
            throw new IllegalArgumentException( "Unit.update - 'unit' can not be null" );
        }
        this.getHibernateTemplate().update( unit );
    }

}