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
package ubic.gemma.persistence.service.expression.designElement;

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Gene;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.designElement.CompositeSequence</code>.
 * </p>
 * 
 * @see ubic.gemma.model.expression.designElement.CompositeSequence
 */
public abstract class CompositeSequenceDaoBase extends HibernateDaoSupport implements CompositeSequenceDao {

    /**
     * @see CompositeSequenceDao#countAll()
     */
    @Override
    public java.lang.Integer countAll() {
        return this.handleCountAll();

    }

    /**
     * @see CompositeSequenceDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends CompositeSequence> create(
            final java.util.Collection<? extends CompositeSequence> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "CompositeSequence.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends CompositeSequence> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see CompositeSequenceDao#create(int
     *      ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    @Override
    public CompositeSequence create( final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        if ( compositeSequence == null ) {
            throw new IllegalArgumentException( "CompositeSequence.create - 'compositeSequence' can not be null" );
        }
        this.getHibernateTemplate().save( compositeSequence );
        return compositeSequence;
    }

    /**
     * @see CompositeSequenceDao#findByBioSequence(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    public java.util.Collection<CompositeSequence> findByBioSequence(
            final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        return this.handleFindByBioSequence( bioSequence );

    }

    /**
     * @see CompositeSequenceDao#findByBioSequenceName(java.lang.String)
     */
    @Override
    public java.util.Collection<CompositeSequence> findByBioSequenceName( final java.lang.String name ) {
        return this.handleFindByBioSequenceName( name );

    }

    /**
     * @see CompositeSequenceDao#findByName(int, java.lang.String)
     */
    @Override
    public java.util.Collection<CompositeSequence> findByName( final java.lang.String name ) {
        return this
                .findByName(
                        "from ubic.gemma.model.expression.designElement.CompositeSequence as compositeSequence where compositeSequence.name = :name",
                        name );
    }

    /**
     * @see CompositeSequenceDao#findByName(int, java.lang.String,
     *      java.lang.String)
     */

    public java.util.Collection<CompositeSequence> findByName( final java.lang.String queryString,
            final java.lang.String name ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( name );
        argNames.add( "name" );
        java.util.List<CompositeSequence> results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return results;
    }

    /**
     * @see CompositeSequenceDao#findByName(int, java.lang.String,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign, java.lang.String)
     */

    public CompositeSequence findByName( final java.lang.String queryString,
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, final java.lang.String name ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( arrayDesign );
        argNames.add( "arrayDesign" );
        args.add( name );
        argNames.add( "name" );
        java.util.Set<CompositeSequence> results = new java.util.LinkedHashSet<CompositeSequence>( this
                .getHibernateTemplate().findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ),
                        args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.expression.designElement.CompositeSequence"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return ( CompositeSequence ) result;
    }

    /**
     * @see CompositeSequenceDao#findByName(int,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign, java.lang.String)
     */
    @Override
    public CompositeSequence findByName( final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign,
            final java.lang.String name ) {
        return this
                .findByName(

                        "from ubic.gemma.model.expression.designElement.CompositeSequence as compositeSequence where compositeSequence.arrayDesign = :arrayDesign and compositeSequence.name = :name",
                        arrayDesign, name );
    }

    /**
     * @see CompositeSequenceDao#getGenes(java.util.Collection)
     */
    @Override
    public java.util.Map<CompositeSequence, Collection<Gene>> getGenes(
            final java.util.Collection<CompositeSequence> compositeSequences ) {
        return this.handleGetGenes( compositeSequences );

    }

    /**
     * @see CompositeSequenceDao#getGenes(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    @Override
    public java.util.Collection<Gene> getGenes(
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        return this.handleGetGenes( compositeSequence );

    }

    /**
     * @see CompositeSequenceDao#getGenesWithSpecificity(java.util.Collection)
     */
    @Override
    public java.util.Map<CompositeSequence, Collection<BioSequence2GeneProduct>> getGenesWithSpecificity(
            final java.util.Collection<CompositeSequence> compositeSequences ) {
        return this.handleGetGenesWithSpecificity( compositeSequences );

    }

    /**
     * @see CompositeSequenceDao#getRawSummary(java.util.Collection,
     *      java.lang.Integer)
     */
    @Override
    public java.util.Collection<Object[]> getRawSummary(
            final java.util.Collection<CompositeSequence> compositeSequences, final java.lang.Integer numResults ) {
        return this.handleGetRawSummary( compositeSequences, numResults );

    }

    /**
     * @see CompositeSequenceDao#getRawSummary(ubic.gemma.model.expression.arrayDesign.ArrayDesign,
     *      java.lang.Integer)
     */
    @Override
    public java.util.Collection<Object[]> getRawSummary(
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, final java.lang.Integer numResults ) {
        return this.handleGetRawSummary( arrayDesign, numResults );

    }

    /**
     * @see CompositeSequenceDao#getRawSummary(ubic.gemma.model.expression.designElement.CompositeSequence,
     *      java.lang.Integer)
     */
    @Override
    public java.util.Collection<Object[]> getRawSummary(
            final ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence,
            final java.lang.Integer numResults ) {
        return this.handleGetRawSummary( compositeSequence, numResults );

    }

    /**
     * @see CompositeSequenceDao#load(int, java.lang.Long)
     */

    @Override
    public CompositeSequence load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "CompositeSequence.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.expression.designElement.CompositeSequenceImpl.class, id );
        return ( CompositeSequence ) entity;
    }

    /**
     * @see CompositeSequenceDao#load(java.util.Collection)
     */
    @Override
    public java.util.Collection<CompositeSequence> load( final java.util.Collection<Long> ids ) {

        return this.handleLoad( ids );

    }

    /**
     * @see CompositeSequenceDao#loadAll(int)
     */

    @Override
    public java.util.Collection<? extends CompositeSequence> loadAll() {
        return this.getHibernateTemplate().loadAll(
                ubic.gemma.model.expression.designElement.CompositeSequenceImpl.class );
    }

    /**
     * @see CompositeSequenceDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "CompositeSequence.remove - 'id' can not be null" );
        }
        ubic.gemma.model.expression.designElement.CompositeSequence entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    @Override
    public void remove( java.util.Collection<? extends CompositeSequence> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "CompositeSequence.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see CompositeSequenceDao#remove(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    @Override
    public void remove( ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        if ( compositeSequence == null ) {
            throw new IllegalArgumentException( "CompositeSequence.remove - 'compositeSequence' can not be null" );
        }
        this.getHibernateTemplate().delete( compositeSequence );
    }

    /**
     * @see CompositeSequenceDao#thaw(java.util.Collection)
     */
    @Override
    public void thaw( final java.util.Collection<CompositeSequence> compositeSequences ) {
        try {
            this.handleThaw( compositeSequences );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'CompositeSequenceDao.thaw(java.util.Collection compositeSequences)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    @Override
    public void update( final java.util.Collection<? extends CompositeSequence> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "CompositeSequence.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends CompositeSequence> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see CompositeSequenceDao#update(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    @Override
    public void update( ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence ) {
        if ( compositeSequence == null ) {
            throw new IllegalArgumentException( "CompositeSequence.update - 'compositeSequence' can not be null" );
        }
        this.getHibernateTemplate().update( compositeSequence );
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll();

    /**
     * Performs the core logic for {@link #findByBioSequence(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleFindByBioSequence(
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

    /**
     * Performs the core logic for {@link #findByBioSequenceName(java.lang.String)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleFindByBioSequenceName( java.lang.String name );

    /**
     * Performs the core logic for {@link #getGenes(java.util.Collection)}
     */
    protected abstract java.util.Map<CompositeSequence, Collection<Gene>> handleGetGenes(
            java.util.Collection<CompositeSequence> compositeSequences );

    /**
     * Performs the core logic for {@link #getGenes(ubic.gemma.model.expression.designElement.CompositeSequence)}
     */
    protected abstract java.util.Collection<Gene> handleGetGenes(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence );

    /**
     * Performs the core logic for {@link #getGenesWithSpecificity(java.util.Collection)}
     */
    protected abstract java.util.Map<CompositeSequence, Collection<BioSequence2GeneProduct>> handleGetGenesWithSpecificity(
            java.util.Collection<CompositeSequence> compositeSequences );

    /**
     * Performs the core logic for {@link #getRawSummary(java.util.Collection, java.lang.Integer)}
     */
    protected abstract java.util.Collection<Object[]> handleGetRawSummary(
            java.util.Collection<CompositeSequence> compositeSequences, java.lang.Integer numResults );

    /**
     * Performs the core logic for
     * {@link #getRawSummary(ubic.gemma.model.expression.arrayDesign.ArrayDesign, java.lang.Integer)}
     */
    protected abstract java.util.Collection<Object[]> handleGetRawSummary(
            ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign, java.lang.Integer numResults );

    /**
     * Performs the core logic for
     * {@link #getRawSummary(ubic.gemma.model.expression.designElement.CompositeSequence, java.lang.Integer)}
     */
    protected abstract java.util.Collection<Object[]> handleGetRawSummary(
            ubic.gemma.model.expression.designElement.CompositeSequence compositeSequence, java.lang.Integer numResults );

    /**
     * Performs the core logic for {@link #load(java.util.Collection)}
     */
    protected abstract java.util.Collection<CompositeSequence> handleLoad( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #thaw(java.util.Collection)}
     */
    protected abstract void handleThaw( java.util.Collection<CompositeSequence> compositeSequences );

}