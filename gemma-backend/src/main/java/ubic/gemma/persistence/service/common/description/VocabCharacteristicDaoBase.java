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
package ubic.gemma.persistence.service.common.description;

import java.util.Collection;
import java.util.Map;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.description.VocabCharacteristic</code>.
 * 
 * @see ubic.gemma.model.common.description.VocabCharacteristic
 */
public abstract class VocabCharacteristicDaoBase extends HibernateDaoSupport implements VocabCharacteristicDao {

    /**
     * @see VocabCharacteristicDao#create(int, java.util.Collection)
     */

    @Override
    public java.util.Collection<? extends VocabCharacteristic> create(
            final java.util.Collection<? extends VocabCharacteristic> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "VocabCharacteristic.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends VocabCharacteristic> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see VocabCharacteristicDao#create(int transform,
     *      ubic.gemma.model.common.description.VocabCharacteristic)
     */
    @Override
    public VocabCharacteristic create( final ubic.gemma.model.common.description.VocabCharacteristic vocabCharacteristic ) {
        if ( vocabCharacteristic == null ) {
            throw new IllegalArgumentException( "VocabCharacteristic.create - 'vocabCharacteristic' can not be null" );
        }
        this.getHibernateTemplate().save( vocabCharacteristic );
        return vocabCharacteristic;
    }

    /**
     * @see CharacteristicDao#findByParentClass(java.lang.Class)
     */
    @Override
    public java.util.Map<Characteristic, Object> findByParentClass( final java.lang.Class<?> parentClass ) {
        return this.handleFindByParentClass( parentClass );

    }

    /**
     * @see CharacteristicDao#findByUri(java.lang.String)
     */
    @Override
    public java.util.Collection<Characteristic> findByUri( final java.lang.String searchString ) {

        return this.handleFindByUri( searchString );

    }

    /**
     * @see CharacteristicDao#findByUri(java.util.Collection)
     */
    @Override
    public java.util.Collection<Characteristic> findByUri( final java.util.Collection<String> uris ) {
        return this.handleFindByUri( uris );

    }

    /**
     * @see CharacteristicDao#findByValue(java.lang.String)
     */
    @Override
    public java.util.Collection<Characteristic> findByValue( final java.lang.String search ) {
        return this.handleFindByValue( search );

    }

    /**
     * @see CharacteristicDao#getParents(java.lang.Class, java.util.Collection)
     */
    @Override
    public Map<Characteristic, Object> getParents( final java.lang.Class<?> parentClass,
            final java.util.Collection<Characteristic> characteristics ) {
        return this.handleGetParents( parentClass, characteristics );

    }

    /**
     * @see VocabCharacteristicDao#load(int, java.lang.Long)
     */

    @Override
    public VocabCharacteristic load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "VocabCharacteristic.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.common.description.VocabCharacteristicImpl.class, id );
        return ( ubic.gemma.model.common.description.VocabCharacteristic ) entity;
    }

    /**
     * @see VocabCharacteristicDao#loadAll(int)
     */

    @SuppressWarnings("unchecked")
    @Override
    public java.util.Collection<? extends VocabCharacteristic> loadAll() {
        final java.util.Collection<?> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.description.VocabCharacteristicImpl.class );
        return ( Collection<? extends VocabCharacteristic> ) results;
    }

    /**
     * @see VocabCharacteristicDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "VocabCharacteristic.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.description.VocabCharacteristic entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    @Override
    public void remove( java.util.Collection<? extends VocabCharacteristic> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "VocabCharacteristic.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see VocabCharacteristicDao#remove(ubic.gemma.model.common.description.VocabCharacteristic)
     */
    @Override
    public void remove( ubic.gemma.model.common.description.VocabCharacteristic vocabCharacteristic ) {
        if ( vocabCharacteristic == null ) {
            throw new IllegalArgumentException( "VocabCharacteristic.remove - 'vocabCharacteristic' can not be null" );
        }
        this.getHibernateTemplate().delete( vocabCharacteristic );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    @Override
    public void update( final java.util.Collection<? extends VocabCharacteristic> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "VocabCharacteristic.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends VocabCharacteristic> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see VocabCharacteristicDao#update(ubic.gemma.model.common.description.VocabCharacteristic)
     */
    @Override
    public void update( ubic.gemma.model.common.description.VocabCharacteristic vocabCharacteristic ) {
        if ( vocabCharacteristic == null ) {
            throw new IllegalArgumentException( "VocabCharacteristic.update - 'vocabCharacteristic' can not be null" );
        }
        this.getHibernateTemplate().update( vocabCharacteristic );
    }

    /**
     * Performs the core logic for {@link #findByParentClass(java.lang.Class)}
     */
    protected abstract java.util.Map<Characteristic, Object> handleFindByParentClass( java.lang.Class<?> parentClass );

    /**
     * Performs the core logic for {@link #findByUri(java.lang.String)}
     */
    protected abstract java.util.Collection<Characteristic> handleFindByUri( java.lang.String searchString );

    /**
     * Performs the core logic for {@link #findByUri(java.util.Collection)}
     */
    protected abstract java.util.Collection<Characteristic> handleFindByUri( java.util.Collection<String> uris );

    /**
     * Performs the core logic for {@link #findByValue(java.lang.String)}
     */
    protected abstract java.util.Collection<Characteristic> handleFindByValue( java.lang.String search );

    /**
     * Performs the core logic for {@link #getParents(java.lang.Class, java.util.Collection)}
     */
    protected abstract java.util.Map<Characteristic, Object> handleGetParents( java.lang.Class<?> parentClass,
            java.util.Collection<Characteristic> characteristics );

}