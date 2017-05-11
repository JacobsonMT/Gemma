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
package ubic.gemma.persistence.service.common.quantitationtype;

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import ubic.gemma.model.common.quantitationtype.QuantitationType;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.quantitationtype.QuantitationType</code>.
 * </p>
 * 
 * @see ubic.gemma.model.common.quantitationtype.QuantitationType
 */
public abstract class QuantitationTypeDaoBase extends HibernateDaoSupport implements QuantitationTypeDao {

    /**
     * @see QuantitationTypeDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends QuantitationType> create(
            final java.util.Collection<? extends QuantitationType> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "QuantitationType.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends QuantitationType> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    @Override
    public Collection<? extends QuantitationType> load( Collection<Long> ids ) {
        return this.getHibernateTemplate()
                .findByNamedParam( "from QuantitationTypeImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see QuantitationTypeDao#create(int transform,
     *      ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    public QuantitationType create( final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        if ( quantitationType == null ) {
            throw new IllegalArgumentException( "QuantitationType.create - 'quantitationType' can not be null" );
        }
        this.getHibernateTemplate().save( quantitationType );
        return quantitationType;
    }

    /**
     * @see QuantitationTypeDao#load(int, java.lang.Long)
     */

    @Override
    public QuantitationType load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "QuantitationType.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.common.quantitationtype.QuantitationTypeImpl.class, id );
        return ( ubic.gemma.model.common.quantitationtype.QuantitationType ) entity;
    }

    /**
     * @see QuantitationTypeDao#loadAll(int)
     */

    @Override
    public java.util.Collection<? extends QuantitationType> loadAll() {
        final java.util.Collection<? extends QuantitationType> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.quantitationtype.QuantitationTypeImpl.class );
        return results;
    }

    /**
     * @see QuantitationTypeDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "QuantitationType.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.quantitationtype.QuantitationType entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    @Override
    public void remove( java.util.Collection<? extends QuantitationType> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "QuantitationType.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see QuantitationTypeDao#remove(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    public void remove( ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        if ( quantitationType == null ) {
            throw new IllegalArgumentException( "QuantitationType.remove - 'quantitationType' can not be null" );
        }
        this.getHibernateTemplate().delete( quantitationType );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    @Override
    public void update( final java.util.Collection<? extends QuantitationType> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "QuantitationType.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends QuantitationType> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see QuantitationTypeDao#update(ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @Override
    public void update( ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        if ( quantitationType == null ) {
            throw new IllegalArgumentException( "QuantitationType.update - 'quantitationType' can not be null" );
        }
        this.getHibernateTemplate().update( quantitationType );
    }

}