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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.association;

import java.util.Collection;
import java.util.HashSet;

import org.hibernate.Criteria;

import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.BusinessKey;

/**
 * @see ubic.gemma.model.association.Gene2GOAssociation
 * @author pavlidis
 * @version $Id$
 */
public class Gene2GOAssociationDaoImpl extends ubic.gemma.model.association.Gene2GOAssociationDaoBase {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.Gene2GOAssociationDaoBase#handleFindByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection handleFindByGene( Gene gene ) throws Exception {
        final String queryString = "select distinct geneAss.ontologyEntry from Gene2GOAssociationImpl as geneAss  where geneAss.gene = :gene";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "gene", gene );
            return queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.Gene2GOAssociationDaoBase#handleFindByGOTerm(ubic.gemma.model.genome.Gene)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleFindByGOTerm( Collection goTerms, Taxon taxon ) throws Exception {
        Collection<String> goIDs = new HashSet<String>();
        if ( goTerms.size() == 0 ) return goIDs;

        final String queryString = "select distinct geneAss.gene from Gene2GOAssociationImpl as geneAss  where geneAss.ontologyEntry.valueUri in (:goIDs) and geneAss.gene.taxon = :taxon";

        // need to turn the collection of goTerms into a collection of GOId's

        for ( Object obj : goTerms ) {
            VocabCharacteristic oe = ( VocabCharacteristic ) obj;
            goIDs.add( oe.getValueUri() );
        }

        Collection<Gene> results;

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameterList( "goIDs", goIDs );
            queryObject.setParameter( "taxon", taxon );

            results = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return results;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.Gene2GOAssociationDaoBase#find(ubic.gemma.model.association.Gene2GOAssociation)
     */
    @Override
    public Gene2GOAssociation find( Gene2GOAssociation gene2GOAssociation ) {
        try {

            BusinessKey.checkValidKey( gene2GOAssociation );
            Criteria queryObject = super.getSession( false ).createCriteria( Gene2GOAssociation.class );
            BusinessKey.addRestrictions( queryObject, gene2GOAssociation );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {

                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException( results.size() + " "
                            + Gene2GOAssociation.class.getName() + "s were found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( Gene2GOAssociation ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.Gene2GOAssociationDaoBase#findOrCreate(ubic.gemma.model.association.Gene2GOAssociation)
     */
    @Override
    public Gene2GOAssociation findOrCreate( Gene2GOAssociation gene2GOAssociation ) {
        Gene2GOAssociation existing = this.find( gene2GOAssociation );
        if ( existing != null ) {
            assert existing.getId() != null;
            return existing;
        }
        return ( Gene2GOAssociation ) create( gene2GOAssociation );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleFindByGoTerm( String goId, Taxon taxon ) throws Exception {

        final String queryString = "select distinct geneAss.gene from Gene2GOAssociationImpl as geneAss  where geneAss.ontologyEntry.value = :goID and geneAss.gene.taxon = :taxon";

        // need to turn the collection of goTerms into a collection of GOId's

        Collection<Gene> results;

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "goID", goId.replaceFirst( ":", "_" ) );
            queryObject.setParameter( "taxon", taxon );

            results = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return results;
    }

    protected void handleRemoveAll() throws Exception {
        final String queryString = "delete from Gene2GOAssociationImpl";
        this.getHibernateTemplate().bulkUpdate( queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.Gene2GOAssociationDaoBase#handleFindAssociationByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection handleFindAssociationByGene( Gene gene ) throws Exception {
        final String queryString = "from Gene2GOAssociationImpl where gene = :gene";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "gene", gene );
            return queryObject.list();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

}