/*
 * The Gemma project Copyright (c) 2009 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */

package ubic.gemma.model.expression.bioAssayData;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.BusinessKey;

/**
 * @author paul
 * @version $Id$
 */
@Repository
public class RawExpressionDataVectorDaoImpl extends DesignElementDataVectorDaoImpl<RawExpressionDataVector> implements
        RawExpressionDataVectorDao {

    private static Log log = LogFactory.getLog( RawExpressionDataVectorDaoImpl.class.getName() );

    @Autowired
    public RawExpressionDataVectorDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDao#find(ubic.gemma.model.expression.arrayDesign
     * .ArrayDesign, ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    @SuppressWarnings("unchecked")
    public Collection<RawExpressionDataVector> find( ArrayDesign arrayDesign, QuantitationType quantitationType ) {
        final String queryString = "select dev from RawExpressionDataVectorImpl dev  inner join fetch dev.bioAssayDimension bd "
                + " inner join fetch dev.designElement de inner join fetch dev.quantitationType where dev.designElement in (:desEls) "
                + "and dev.quantitationType = :quantitationType ";
        try {
            org.hibernate.Query queryObject = super.getSession().createQuery( queryString );
            queryObject.setParameter( "quantitationType", quantitationType );

            Collection<DesignElement> batch = new HashSet<DesignElement>();
            Collection<RawExpressionDataVector> result = new HashSet<RawExpressionDataVector>();
            int batchSize = 2000;
            for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
                batch.add( cs );

                if ( batch.size() >= batchSize ) {
                    queryObject.setParameterList( "desEls", batch );
                    result.addAll( queryObject.list() );
                    batch.clear();
                }
            }

            if ( batch.size() > 0 ) {
                queryObject.setParameterList( "desEls", batch );
                result.addAll( queryObject.list() );
            }

            return result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<RawExpressionDataVector> find( Collection<QuantitationType> quantitationTypes ) {
        final String queryString = "select dev from RawExpressionDataVectorImpl dev  where  "
                + "  dev.quantitationType in ( :quantitationTypes) ";
        try {
            org.hibernate.Query queryObject = super.getSession().createQuery( queryString );
            queryObject.setParameterList( "quantitationTypes", quantitationTypes );
            return queryObject.list();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @SuppressWarnings("unchecked")
    public Collection<RawExpressionDataVector> find( QuantitationType quantitationType ) {
        final String queryString = "select dev from RawExpressionDataVectorImpl dev   where  "
                + "  dev.quantitationType = :quantitationType ";
        try {
            org.hibernate.Query queryObject = super.getSession().createQuery( queryString );
            queryObject.setParameter( "quantitationType", quantitationType );
            return queryObject.list();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDao#find(ubic.gemma.model.expression.bioAssayData
     * .DesignElementDataVector)
     */
    @SuppressWarnings("unchecked")
    public RawExpressionDataVector find( RawExpressionDataVector designElementDataVector ) {

        BusinessKey.checkKey( designElementDataVector );

        DetachedCriteria crit = DetachedCriteria.forClass( RawExpressionDataVector.class );

        crit.createCriteria( "designElement" ).add(
                Restrictions.eq( "name", designElementDataVector.getDesignElement().getName() ) ).createCriteria(
                "arrayDesign" ).add(
                Restrictions.eq( "name", designElementDataVector.getDesignElement().getArrayDesign().getName() ) );

        crit.createCriteria( "quantitationType" ).add(
                Restrictions.eq( "name", designElementDataVector.getQuantitationType().getName() ) );

        crit.createCriteria( "expressionExperiment" ).add(
                Restrictions.eq( "name", designElementDataVector.getExpressionExperiment().getName() ) );

        List results = this.getHibernateTemplate().findByCriteria( crit );
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of '" + DesignElementDataVector.class.getName()
                                + "' was found when executing query" );

            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        return ( RawExpressionDataVector ) result;

    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDao#load(int, java.lang.Long)
     */

    public RawExpressionDataVector load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "RawExpressionDataVector.load - 'id' can not be null" );
        }
        return this.getHibernateTemplate().get( RawExpressionDataVectorImpl.class, id );

    }

    /**
     * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDao#loadAll()
     */
    public java.util.Collection<? extends RawExpressionDataVector> loadAll() {
        return this.getHibernateTemplate().loadAll( RawExpressionDataVectorImpl.class );
    }

    @Override
    public void remove( RawExpressionDataVector designElementDataVector ) {
        this.getHibernateTemplate().delete( designElementDataVector );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDaoBase#handleRemoveDataForCompositeSequence(
     * ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    public void removeDataForCompositeSequence( final CompositeSequence compositeSequence ) {
        // rarely used.
        String[] probeCoexpTypes = new String[] { "Mouse", "Human", "Rat", "Other" };

        for ( String type : probeCoexpTypes ) {

            final String dedvRemovalQuery = "delete dedv from RawExpressionDataVectorImpl dedv where dedv.designElement = ?";

            final String ppcRemoveFirstQuery = "delete d from " + type
                    + "ProbeCoExpressionImpl as p inner join p.firstVector d where d.designElement = ?";
            final String ppcRemoveSecondQuery = "delete d from " + type
                    + "ProbeCoExpressionImpl as p inner join p.secondVector d where d.designElement = ?";

            int deleted = getHibernateTemplate().bulkUpdate( ppcRemoveFirstQuery, compositeSequence );
            deleted += getHibernateTemplate().bulkUpdate( ppcRemoveSecondQuery, compositeSequence );
            getHibernateTemplate().bulkUpdate( dedvRemovalQuery, compositeSequence );
        }
    }

    public void removeDataForQuantitationType( final QuantitationType quantitationType ) {
        final String dedvRemovalQuery = "delete from RawExpressionDataVectorImpl as dedv where dedv.quantitationType = ?";
        int deleted = getHibernateTemplate().bulkUpdate( dedvRemovalQuery, quantitationType );
        log.info( "Deleted " + deleted + " data vector elements" );
    }

    /**
     * @param ees
     * @param cs2gene
     * @return
     */
    protected Map<RawExpressionDataVector, Collection<Gene>> getPreferredVectorsForProbes(
            Collection<ExpressionExperiment> ees, Map<CompositeSequence, Collection<Gene>> cs2gene ) {

        final String queryString;
        if ( ees == null || ees.size() == 0 ) {
            queryString = "select distinct dedv, dedv.designElement from RawExpressionDataVectorImpl dedv "
                    + " inner join fetch dedv.bioAssayDimension bd "
                    + " inner join dedv.designElement de inner join fetch dedv.quantitationType "
                    + " where dedv.designElement in ( :cs ) and dedv.quantitationType.isPreferred = true";
        } else {
            queryString = "select distinct dedv, dedv.designElement from RawExpressionDataVectorImpl dedv"
                    + " inner join fetch dedv.bioAssayDimension bd "
                    + " inner join dedv.designElement de inner join fetch dedv.quantitationType "
                    + " where dedv.designElement in (:cs ) and dedv.quantitationType.isPreferred = true"
                    + " and dedv.expressionExperiment in ( :ees )";
        }
        return getVectorsForProbesInExperiments( ees, cs2gene, queryString );
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from RawExpressionDataVectorImpl";
        try {
            org.hibernate.Query queryObject = super.getSession().createQuery( query );

            return ( Integer ) queryObject.iterate().next();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

}
