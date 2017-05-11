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
package ubic.gemma.persistence.service.association;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.association.Gene2GOAssociationImpl;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.Gene;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.association.Gene2GOAssociation</code>.
 * 
 * @see ubic.gemma.model.association.Gene2GOAssociation
 */
public abstract class Gene2GOAssociationDaoBase extends HibernateDaoSupport implements Gene2GOAssociationDao {

    /**
     * @see Gene2GOAssociationDao#create(int, Collection)
     */
    @Override
    public Collection<? extends Gene2GOAssociation> create( final Collection<? extends Gene2GOAssociation> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Gene2GOAssociation.create - 'entities' can not be null" );
        }

        for ( Iterator<? extends Gene2GOAssociation> entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
            create( entityIterator.next() );
        }

        return entities;
    }

    /**
     * @see Gene2GOAssociationDao#create(int transform,
     *      ubic.gemma.model.association.Gene2GOAssociation)
     */
    @Override
    public Gene2GOAssociation create( final ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) {
        if ( gene2GOAssociation == null ) {
            throw new IllegalArgumentException( "Gene2GOAssociation.create - 'gene2GOAssociation' can not be null" );
        }
        this.getHibernateTemplate().save( gene2GOAssociation );
        return gene2GOAssociation;
    }

    /**
     * @see Gene2GOAssociationDao#find(int, java.lang.String,
     *      ubic.gemma.model.association.Gene2GOAssociation)
     */

    public Gene2GOAssociation find( final java.lang.String queryString,
            final ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) {
        List<String> argNames = new ArrayList<String>();
        List<Object> args = new ArrayList<Object>();
        args.add( gene2GOAssociation );
        argNames.add( "gene2GOAssociation" );
        Set<? extends Gene2GOAssociation> results = new LinkedHashSet<Gene2GOAssociation>( this.getHibernateTemplate()
                .findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.association.Gene2GOAssociation"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return ( Gene2GOAssociation ) result;
    }

    /**
     * @see Gene2GOAssociationDao#find(int,
     *      ubic.gemma.model.association.Gene2GOAssociation)
     */

    @Override
    public Gene2GOAssociation find( final ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) {
        return this
                .find( "from ubic.gemma.model.association.Gene2GOAssociation as gene2GOAssociation where gene2GOAssociation.gene2GOAssociation = :gene2GOAssociation",
                        gene2GOAssociation );
    }

    /**
     * @see Gene2GOAssociationDao#findAssociationByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    public Collection<Gene2GOAssociation> findAssociationByGene( final ubic.gemma.model.genome.Gene gene ) {
        return this.handleFindAssociationByGene( gene );

    }

    /**
     * @see Gene2GOAssociationDao#findByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    public Collection<VocabCharacteristic> findByGene( final ubic.gemma.model.genome.Gene gene ) {
        return this.handleFindByGene( gene );

    }

    @Override
    public Collection<Gene> findByGoTerm( final java.lang.String goId ) {
        return this.handleFindByGoTerm( goId );

    }

    /**
     * @see Gene2GOAssociationDao#findByGoTerm(java.lang.String,
     *      ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Collection<Gene> findByGoTerm( final java.lang.String goId, final ubic.gemma.model.genome.Taxon taxon ) {
        return this.handleFindByGoTerm( goId, taxon );

    }

    /**
     * @see Gene2GOAssociationDao#findByGOTerm(Collection, ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Collection<Gene> findByGOTerm( final Collection<String> goTerms, final ubic.gemma.model.genome.Taxon taxon ) {
        return this.handleFindByGOTerm( goTerms, taxon );

    }

    /**
     * @see Gene2GOAssociationDao#findOrCreate(int, java.lang.String,
     *      ubic.gemma.model.association.Gene2GOAssociation)
     */

    public Gene2GOAssociation findOrCreate( final java.lang.String queryString,
            final ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) {
        List<String> argNames = new ArrayList<String>();
        List<Object> args = new ArrayList<Object>();
        args.add( gene2GOAssociation );
        argNames.add( "gene2GOAssociation" );
        Set<? extends Gene2GOAssociation> results = new LinkedHashSet<Gene2GOAssociation>( this.getHibernateTemplate()
                .findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Gene2GOAssociation result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.association.Gene2GOAssociation"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return result;
    }

    /**
     * @see Gene2GOAssociationDao#findOrCreate(int,
     *      ubic.gemma.model.association.Gene2GOAssociation)
     */

    @Override
    public Gene2GOAssociation findOrCreate( final ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) {
        return this
                .findOrCreate(

                        "from ubic.gemma.model.association.Gene2GOAssociation as gene2GOAssociation where gene2GOAssociation.gene2GOAssociation = :gene2GOAssociation",
                        gene2GOAssociation );
    }

    @Override
    public Collection<? extends Gene2GOAssociation> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from Gene2GOAssociationImpl where id in (:ids)", "ids",
                ids );
    }

    /**
     * @see Gene2GOAssociationDao#load(int, java.lang.Long)
     */
    @Override
    public Gene2GOAssociation load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Gene2GOAssociation.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.association.Gene2GOAssociationImpl.class, id );
        return ( ubic.gemma.model.association.Gene2GOAssociation ) entity;
    }

    /**
     * @see Gene2GOAssociationDao#loadAll(int)
     */
    @Override
    public Collection<? extends Gene2GOAssociation> loadAll() {
        final Collection<? extends Gene2GOAssociation> results = this.getHibernateTemplate().loadAll(
                Gene2GOAssociationImpl.class );

        return results;
    }

    /**
     * @see ubic.gemma.model.association.RelationshipDao#remove(Collection)
     */
    @Override
    public void remove( Collection<? extends Gene2GOAssociation> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Gene2GOAssociation.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see Gene2GOAssociationDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Gene2GOAssociation.remove - 'id' can not be null" );
        }
        ubic.gemma.model.association.Gene2GOAssociation entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see Gene2GOAssociationDao#remove(ubic.gemma.model.association.Gene2GOAssociation)
     */
    @Override
    public void remove( ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) {
        if ( gene2GOAssociation == null ) {
            throw new IllegalArgumentException( "Gene2GOAssociation.remove - 'gene2GOAssociation' can not be null" );
        }
        this.getHibernateTemplate().delete( gene2GOAssociation );
    }

    /**
     * @see Gene2GOAssociationDao#removeAll()
     */
    @Override
    public void removeAll() {
        try {
            this.handleRemoveAll();
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'Gene2GOAssociationDao.removeAll()' --> " + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.association.RelationshipDao#update(Collection)
     */
    @Override
    public void update( final Collection<? extends Gene2GOAssociation> entities ) {
        throw new UnsupportedOperationException( "Immutable, update not supported" );
    }

    /**
     * @see Gene2GOAssociationDao#update(ubic.gemma.model.association.Gene2GOAssociation)
     */
    @Override
    public void update( ubic.gemma.model.association.Gene2GOAssociation gene2GOAssociation ) {
        throw new UnsupportedOperationException( "Immutable, update not supported" );
    }

    /**
     * Performs the core logic for {@link #findAssociationByGene(ubic.gemma.model.genome.Gene)}
     */
    protected abstract Collection<Gene2GOAssociation> handleFindAssociationByGene( ubic.gemma.model.genome.Gene gene );

    /**
     * Performs the core logic for {@link #findByGene(ubic.gemma.model.genome.Gene)}
     */
    protected abstract Collection<VocabCharacteristic> handleFindByGene( ubic.gemma.model.genome.Gene gene );

    protected abstract Collection<Gene> handleFindByGoTerm( java.lang.String goId );

    /**
     * Performs the core logic for {@link #findByGoTerm(java.lang.String, ubic.gemma.model.genome.Taxon)}
     */
    protected abstract Collection<Gene> handleFindByGoTerm( java.lang.String goId, ubic.gemma.model.genome.Taxon taxon );

    /**
     * Performs the core logic for {@link #findByGOTerm(Collection, ubic.gemma.model.genome.Taxon)}
     */
    protected abstract Collection<Gene> handleFindByGOTerm( Collection<String> goTerms,
            ubic.gemma.model.genome.Taxon taxon );

    /**
     * Performs the core logic for {@link #removeAll()}
     */
    protected abstract void handleRemoveAll();

}