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
package ubic.gemma.persistence.service.association;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.util.BusinessKey;
import ubic.gemma.persistence.util.EntityUtils;

/**
 * @see ubic.gemma.model.association.Gene2GOAssociation
 * @author pavlidis
 * @version $Id$
 */
@Repository
public class Gene2GOAssociationDaoImpl extends Gene2GOAssociationDaoBase {

    private static Log log = LogFactory.getLog( Gene2GOAssociationDaoImpl.class );

    @Autowired
    public Gene2GOAssociationDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see Gene2GOAssociationDaoBase#find(ubic.gemma.model.association.Gene2GOAssociation)
     */
    @Override
    public Gene2GOAssociation find( Gene2GOAssociation gene2GOAssociation ) {
        try {

            BusinessKey.checkValidKey( gene2GOAssociation );
            Criteria queryObject = super.getSessionFactory().getCurrentSession()
                    .createCriteria( Gene2GOAssociation.class );
            BusinessKey.addRestrictions( queryObject, gene2GOAssociation );

            java.util.List<?> results = queryObject.list();
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
     * @see Gene2GOAssociationDao#findByGenes(java.util.Collection)
     */
    @Override
    public Map<Gene, Collection<VocabCharacteristic>> findByGenes( Collection<Gene> needToFind ) {
        Map<Gene, Collection<VocabCharacteristic>> result = new HashMap<Gene, Collection<VocabCharacteristic>>();
        StopWatch timer = new StopWatch();
        timer.start();
        int batchSize = 200;
        Set<Gene> batch = new HashSet<Gene>();
        int i = 0;
        for ( Gene gene : needToFind ) {
            batch.add( gene );
            if ( batch.size() == batchSize ) {
                result.putAll( fetchBatch( batch ) );
                batch.clear();
            }
            if ( ++i % 1000 == 0 ) {
                log.info( "Fetched GO associations for " + i + "/" + needToFind.size() + " genes" );
            }
        }
        if ( !batch.isEmpty() ) result.putAll( fetchBatch( batch ) );

        if ( timer.getTime() > 1000 ) {
            log.info( "Fetched GO annotations for " + needToFind.size() + " genes in " + timer.getTime() + "ms" );
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Gene2GOAssociationDao#findByGoTermsPerTaxon(java.util.Collection)
     */
    @Override
    public Map<Taxon, Collection<Gene>> findByGoTermsPerTaxon( Collection<String> termsToFetch ) {
        Collection<Gene> genes = this.getGenes( termsToFetch );
        Map<Taxon, Collection<Gene>> results = new HashMap<>();

        for ( Gene g : genes ) {

            if ( !results.containsKey( g.getTaxon() ) ) {

                results.put( g.getTaxon(), new HashSet<Gene>() );

            }

            results.get( g.getTaxon() ).add( g );

        }

        return results;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * Gene2GOAssociationDaoBase#findOrCreate(ubic.gemma.model.association.Gene2GOAssociation
     * )
     */
    @Override
    public Gene2GOAssociation findOrCreate( Gene2GOAssociation gene2GOAssociation ) {
        Gene2GOAssociation existing = this.find( gene2GOAssociation );
        if ( existing != null ) {
            assert existing.getId() != null;
            return existing;
        }
        return create( gene2GOAssociation );
    }

    @Override
    public Collection<Gene> getGenes( Collection<String> ids ) {
        final String queryString = "select distinct geneAss.gene from Gene2GOAssociationImpl as geneAss  "
                + "where geneAss.ontologyEntry.value in ( :goIDs)";

        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameterList( "goIDs", ids )
                .list();

    }

    /*
     * (non-Javadoc)
     * 
     * @see Gene2GOAssociationDao#getGenes(java.util.Collection,
     * ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Collection<Gene> getGenes( Collection<String> ids, Taxon taxon ) {
        if ( taxon == null ) return getGenes( ids );

        final String queryString = "select distinct  "
                + "  gene from Gene2GOAssociationImpl as geneAss join geneAss.gene as gene "
                + "where geneAss.ontologyEntry.value in ( :goIDs) and gene.taxon = :tax";

        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameterList( "goIDs", ids )
                .setParameter( "tax", taxon ).list();
    }

    /*
     * (non-Javadoc)
     * 
     * @see Gene2GOAssociationDao#getSets(java.util.Collection)
     */
    @Override
    public Map<String, Collection<Gene>> getSets( Collection<String> ids ) {
        final String queryString = "select distinct geneAss.ontologyEntry.value, "
                + "geneAss.gene from Gene2GOAssociationImpl as geneAss  "
                + "where geneAss.ontologyEntry.value in ( :goIDs)";

        Map<String, Collection<Gene>> result = new HashMap<>();
        List<?> list = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "goIDs", ids ).list();

        for ( Object o : list ) {
            Object[] oa = ( Object[] ) o;
            if ( !result.containsKey( oa[0] ) ) {
                result.put( ( String ) oa[0], new HashSet<Gene>() );
            }
            result.get( oa[0] ).add( ( Gene ) oa[1] );
        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * Gene2GOAssociationDaoBase#handleFindAssociationByGene(ubic.gemma.model.genome.Gene)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<Gene2GOAssociation> handleFindAssociationByGene( Gene gene ) {

        final String queryString = "from Gene2GOAssociationImpl where gene = :gene";
        List<?> g2go = this.getHibernateTemplate().findByNamedParam( queryString, "gene", gene );
        return ( Collection<Gene2GOAssociation> ) g2go;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Gene2GOAssociationDaoBase#handleFindByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection<VocabCharacteristic> handleFindByGene( Gene gene ) {

        final String queryString = "select distinct geneAss.ontologyEntry from Gene2GOAssociationImpl as geneAss  where geneAss.gene = :gene";

        List<?> vo = this.getHibernateTemplate().findByNamedParam( queryString, "gene", gene );

        @SuppressWarnings("unchecked")
        Collection<VocabCharacteristic> result = ( Collection<VocabCharacteristic> ) vo;

        return result;
    }

    @Override
    protected Collection<Gene> handleFindByGoTerm( String goId ) {

        final String queryString = "select distinct geneAss.gene from Gene2GOAssociationImpl as geneAss  "
                + "where geneAss.ontologyEntry.value = :goID";

        // need to turn the collection of goTerms into a collection of GOId's

        Collection<Gene> results;

        org.hibernate.Query queryObject = super.getSessionFactory().getCurrentSession().createQuery( queryString );
        queryObject.setParameter( "goID", goId.replaceFirst( ":", "_" ) );

        results = queryObject.list();

        return results;
    }

    @Override
    protected Collection<Gene> handleFindByGoTerm( String goId, Taxon taxon ) {

        final String queryString = "select distinct geneAss.gene from Gene2GOAssociationImpl as geneAss  "
                + "where geneAss.ontologyEntry.value = :goID and geneAss.gene.taxon = :taxon";

        // need to turn the collection of goTerms into a collection of GOId's

        Collection<Gene> results;

        org.hibernate.Query queryObject = super.getSessionFactory().getCurrentSession().createQuery( queryString );
        queryObject.setParameter( "goID", goId.replaceFirst( ":", "_" ) );
        queryObject.setParameter( "taxon", taxon );

        results = queryObject.list();

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Gene2GOAssociationDaoBase#handleFindByGOTerm(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection<Gene> handleFindByGOTerm( Collection<String> goTerms, Taxon taxon ) {

        if ( goTerms.size() == 0 ) return new HashSet<Gene>();

        final String queryString = "select distinct geneAss.gene from Gene2GOAssociationImpl as geneAss"
                + "  where geneAss.ontologyEntry.valueUri in (:goIDs) and geneAss.gene.taxon = :taxon";

        return this.getHibernateTemplate().findByNamedParam( queryString, new String[] { "goIDs", "taxon" },
                new Object[] { goTerms, taxon } );
    }

    /*
     * (non-Javadoc)
     * 
     * @see Gene2GOAssociationDaoBase#handleRemoveAll()
     */
    @Override
    protected void handleRemoveAll() {

        int total = 0;
        Session sess = getSessionFactory().getCurrentSession();

        // this should do the deletion, right? -- Confirmed. (PP)
        while ( true ) {
            Query q = sess.createQuery( "from Gene2GOAssociationImpl" );
            q.setMaxResults( 10000 );
            List<?> list = q.list();
            if ( list.isEmpty() ) break;

            total += list.size();

            this.getHibernateTemplate().deleteAll( list );
            log.info( "Deleted " + total + " so far..." );
        }

        log.info( "Deleted: " + total );

    }

    /**
     * @param batch
     * @return
     */
    private Map<? extends Gene, ? extends Collection<VocabCharacteristic>> fetchBatch( Set<Gene> batch ) {
        Map<Long, Gene> gimap = EntityUtils.getIdMap( batch );
        final String queryString = "select g.id, geneAss.ontologyEntry from Gene2GOAssociationImpl as geneAss join geneAss.gene g where g.id in (:genes)";
        Map<Gene, Collection<VocabCharacteristic>> results = new HashMap<Gene, Collection<VocabCharacteristic>>();
        Query query = this.getHibernateTemplate().getSessionFactory().getCurrentSession().createQuery( queryString );
        query.setFetchSize( batch.size() );
        query.setParameterList( "genes", gimap.keySet() );
        List<?> o = query.list();

        for ( Object object : o ) {
            Object[] oa = ( Object[] ) object;
            Long g = ( Long ) oa[0];
            VocabCharacteristic vc = ( VocabCharacteristic ) oa[1];
            Gene gene = gimap.get( g );
            assert gene != null;
            if ( !results.containsKey( gene ) ) {
                results.put( gene, new HashSet<VocabCharacteristic>() );
            }
            results.get( gene ).add( vc );
        }

        return results;
    }

}