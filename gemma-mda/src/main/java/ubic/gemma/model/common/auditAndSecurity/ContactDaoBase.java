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
package ubic.gemma.model.common.auditAndSecurity;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.auditAndSecurity.Contact</code>.
 * </p>
 * 
 * @see ubic.gemma.model.common.auditAndSecurity.Contact
 */
public abstract class ContactDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.common.auditAndSecurity.ContactDao {

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.ContactDao#create(int, java.util.Collection)
     */
    public java.util.Collection<Contact> create( final int transform, final java.util.Collection<Contact> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Contact.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<Contact> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( transform, entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.ContactDao#create(int transform,
     *      ubic.gemma.model.common.auditAndSecurity.Contact)
     */
    public Contact create( final int transform, final ubic.gemma.model.common.auditAndSecurity.Contact contact ) {
        if ( contact == null ) {
            throw new IllegalArgumentException( "Contact.create - 'contact' can not be null" );
        }
        this.getHibernateTemplate().save( contact );
        return ( Contact ) this.transformEntity( transform, contact );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.ContactDao#create(java.util.Collection)
     */
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection<Contact> create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.ContactDao#create(ubic.gemma.model.common.auditAndSecurity.Contact)
     */
    public Contact create( ubic.gemma.model.common.auditAndSecurity.Contact contact ) {
        return this.create( TRANSFORM_NONE, contact );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.ContactDao#findByEmail(int, java.lang.String)
     */
    public Contact findByEmail( final int transform, final java.lang.String email ) {
        return this.findByEmail( transform, "from ContactImpl c where c.email = :email", email );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.ContactDao#findByEmail(int, java.lang.String, java.lang.String)
     */
    @SuppressWarnings( { "unchecked" })
    public Contact findByEmail( final int transform, final java.lang.String queryString, final java.lang.String email ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( email );
        argNames.add( "email" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.common.auditAndSecurity.Contact"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, ( ubic.gemma.model.common.auditAndSecurity.Contact ) result );
        return ( Contact ) result;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.ContactDao#findByEmail(java.lang.String)
     */
    public ubic.gemma.model.common.auditAndSecurity.Contact findByEmail( java.lang.String email ) {
        return this.findByEmail( TRANSFORM_NONE, email );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.ContactDao#findByEmail(java.lang.String, java.lang.String)
     */
    public ubic.gemma.model.common.auditAndSecurity.Contact findByEmail( final java.lang.String queryString,
            final java.lang.String email ) {
        return this.findByEmail( TRANSFORM_NONE, queryString, email );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.ContactDao#load(int, java.lang.Long)
     */

    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Contact.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.common.auditAndSecurity.ContactImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.common.auditAndSecurity.Contact ) entity );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.ContactDao#load(java.lang.Long)
     */

    public Contact load( java.lang.Long id ) {
        return ( ubic.gemma.model.common.auditAndSecurity.Contact ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.ContactDao#loadAll()
     */
    public java.util.Collection<Contact> loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.ContactDao#loadAll(int)
     */

    @SuppressWarnings("unchecked")
    public java.util.Collection<Contact> loadAll( final int transform ) {
        final java.util.Collection<Contact> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.auditAndSecurity.ContactImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.ContactDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Contact.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.auditAndSecurity.Contact entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection<Contact> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Contact.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.ContactDao#remove(ubic.gemma.model.common.auditAndSecurity.Contact)
     */
    public void remove( ubic.gemma.model.common.auditAndSecurity.Contact contact ) {
        if ( contact == null ) {
            throw new IllegalArgumentException( "Contact.remove - 'contact' can not be null" );
        }
        this.getHibernateTemplate().delete( contact );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection<Contact> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Contact.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<Contact> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.ContactDao#update(ubic.gemma.model.common.auditAndSecurity.Contact)
     */
    public void update( ubic.gemma.model.common.auditAndSecurity.Contact contact ) {
        if ( contact == null ) {
            throw new IllegalArgumentException( "Contact.update - 'contact' can not be null" );
        }
        this.getHibernateTemplate().update( contact );
    }

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.common.auditAndSecurity.Contact)} method. This method does not
     * instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in
     *        <code>ubic.gemma.model.common.auditAndSecurity.ContactDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.common.auditAndSecurity.Contact)
     */

    protected void transformEntities( final int transform, final java.util.Collection<Contact> entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.common.auditAndSecurity.ContactDao</code>, please note that the {@link #TRANSFORM_NONE}
     * constant denotes no transformation, so the entity itself will be returned. If the integer argument value is
     * unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.common.auditAndSecurity.ContactDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform, final ubic.gemma.model.common.auditAndSecurity.Contact entity ) {
        Object target = null;
        if ( entity != null ) {
            switch ( transform ) {
                case TRANSFORM_NONE: // fall-through
                default:
                    target = entity;
            }
        }
        return target;
    }

}