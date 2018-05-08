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
package ubic.gemma.persistence.service.expression.experiment;

import gemma.gsec.util.SecurityUtil;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.time.StopWatch;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.LongType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.AbstractVoEnabledDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.AbstractCuratableDao;
import ubic.gemma.persistence.util.*;

import java.util.*;

/**
 * @author pavlidis
 * @see ubic.gemma.model.expression.experiment.ExpressionExperiment
 */
@Repository
public class ExpressionExperimentDaoImpl
        extends AbstractCuratableDao<ExpressionExperiment, ExpressionExperimentValueObject>
        implements ExpressionExperimentDao {

    private static final int BATCH_SIZE = 1000;
    private static final int NON_ADMIN_QUERY_FILTER_COUNT = 2;

    @Autowired
    public ExpressionExperimentDaoImpl( SessionFactory sessionFactory ) {
        super( ExpressionExperiment.class, sessionFactory );
    }

    @Override
    public Integer countNotTroubled() {
        return ( ( Long ) this.getSessionFactory().getCurrentSession().createQuery(
                "select count( distinct ee ) from ExpressionExperiment as ee left join "
                        + " ee.bioAssays as ba left join ba.arrayDesignUsed as ad"
                        + " where ee.curationDetails.troubled = false and ad.curationDetails.troubled = false" )
                .uniqueResult() ).intValue();
    }

    @Override
    public ExpressionExperiment thaw( final ExpressionExperiment expressionExperiment ) {
        return this.thaw( expressionExperiment, true );
    }

    @Override
    public ExpressionExperiment thawWithoutVectors( final ExpressionExperiment expressionExperiment ) {
        return this.thaw( expressionExperiment, false );
    }

    @Override
    public ExpressionExperiment thawForFrontEnd( final ExpressionExperiment expressionExperiment ) {
        return this.thawLiter( expressionExperiment );
    }

    @Override
    public ExpressionExperiment thawBioAssays( ExpressionExperiment expressionExperiment ) {
        String thawQuery = "select distinct e from ExpressionExperiment e "
                + " left join fetch e.accession acc left join fetch acc.externalDatabase where e.id=:eeId";

        List res = this.getSessionFactory().getCurrentSession().createQuery( thawQuery )
                .setParameter( "eeId", expressionExperiment.getId() ).list();

        ExpressionExperiment result = ( ExpressionExperiment ) res.iterator().next();

        Hibernate.initialize( result.getBioAssays() );

        for ( BioAssay ba : result.getBioAssays() ) {
            Hibernate.initialize( ba.getArrayDesignUsed() );
            Hibernate.initialize( ba.getSampleUsed() );
        }

        return result;
    }

    /**
     * Special method for front-end access
     *
     * @param orderBy    the field to order the results by.
     * @param descending whether the ordering by the orderField should be descending.
     * @param ids        only list specific ids.
     * @param taxon      only list experiments within specific taxon.
     * @return a list of EE details VOs representing experiments matching the given arguments.
     */
    @Override
    public List<ExpressionExperimentDetailsValueObject> loadDetailsValueObjects( String orderBy, boolean descending,
            Collection<Long> ids, Taxon taxon, int limit, int start ) {
        final ObjectFilter[] filters = new ObjectFilter[taxon != null ? 2 : 1];
        if ( ids != null ) {
            if ( ids.isEmpty() )
                return new ArrayList<>();
            List<Long> idList = new ArrayList<>( ids );
            Collections.sort( idList );
            filters[0] = new ObjectFilter( "id", idList, ObjectFilter.in, ObjectFilter.DAO_EE_ALIAS );
        }
        if ( taxon != null ) {
            filters[1] = new ObjectFilter( "id", taxon.getId(), ObjectFilter.is, ObjectFilter.DAO_TAXON_ALIAS );
        }

        // Compose query
        Query query = this.getLoadValueObjectsQueryString( new ArrayList<ObjectFilter[]>() {
            {
                this.add( filters );
            }
        }, this.getOrderByProperty( orderBy ), descending );

        query.setCacheable( true );
        if ( limit > 0 ) {
            query.setMaxResults( limit );
        }
        query.setFirstResult( start );

        //noinspection unchecked
        List<Object[]> list = query.list();
        List<ExpressionExperimentDetailsValueObject> vos = new ArrayList<>( list.size() );
        for ( Object[] row : list ) {
            ExpressionExperimentDetailsValueObject vo = new ExpressionExperimentDetailsValueObject( row );

            // Add array designs
            Collection<ArrayDesignValueObject> adVos = CommonQueries
                    .getArrayDesignsUsedVOs( ( Long ) row[0], this.getSessionFactory().getCurrentSession() );
            vo.setArrayDesigns( adVos );

            // QTs
            if ( row[24] != null && vo.getTechnologyType() != null ) {
                this.fillQuantitationTypeInfo( ( QuantitationType ) row[24], vo );
            }

            vos.add( vo );
        }

        this.populateAnalysisInformation( vos );

        return vos;
    }

    @Override
    public List<ExpressionExperiment> browse( Integer start, Integer limit ) {
        Query query = this.getSessionFactory().getCurrentSession().createQuery( "from ExpressionExperiment" );
        if ( limit > 0 )
            query.setMaxResults( limit );
        query.setFirstResult( start );

        //noinspection unchecked
        return query.list();
    }

    @Override
    public Collection<ExpressionExperiment> findByAccession( DatabaseEntry accession ) {
        Criteria criteria = this.getSessionFactory().getCurrentSession().createCriteria( ExpressionExperiment.class );

        BusinessKey.checkKey( accession );
        BusinessKey.attachCriteria( criteria, accession, "accession" );

        //noinspection unchecked
        return criteria.list();
    }

    @Override
    public Collection<ExpressionExperiment> findByAccession( String accession ) {
        Query query = this.getSessionFactory().getCurrentSession().createQuery(
                "select e from ExpressionExperiment e inner join e.accession a where a.accession = :accession" )
                .setParameter( "accession", accession );

        //noinspection unchecked
        return query.list();
    }

    @Override
    public List<ExpressionExperiment> findByTaxon( Taxon taxon, Integer limit ) {
        //language=HQL
        final String queryString =
                "select distinct ee from ExpressionExperiment as ee " + "inner join ee.bioAssays as ba "
                        + "inner join ba.sampleUsed as sample join ee.curationDetails s where sample.sourceTaxon = :taxon"
                        + " or sample.sourceTaxon.parentTaxon = :taxon order by s.lastUpdated desc";
        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "taxon", taxon );

        if ( limit != null ) {
            query.setMaxResults( limit );
        }

        //noinspection unchecked
        return query.list();
    }

    @Override
    public List<ExpressionExperiment> findByUpdatedLimit( Collection<Long> ids, Integer limit ) {
        if ( ids.isEmpty() || limit <= 0 )
            return new ArrayList<>();

        Session s = this.getSessionFactory().getCurrentSession();

        String queryString = "select e from ExpressionExperiment e join e.curationDetails s where e.id in (:ids) order by s.lastUpdated desc ";

        Query q = s.createQuery( queryString );
        q.setParameterList( "ids", ids );
        q.setMaxResults( limit );

        //noinspection unchecked
        return q.list();

    }

    @Override
    public List<ExpressionExperiment> findByUpdatedLimit( Integer limit ) {
        if ( limit == 0 )
            return new ArrayList<>();
        Session s = this.getSessionFactory().getCurrentSession();
        String queryString = "select e from ExpressionExperiment e join e.curationDetails s order by s.lastUpdated " + (
                limit < 0 ?
                        "asc" :
                        "desc" );
        Query q = s.createQuery( queryString );
        q.setMaxResults( Math.abs( limit ) );

        //noinspection unchecked
        return q.list();
    }

    @Override
    public Collection<ArrayDesign> getArrayDesignsUsed( BioAssaySet bas ) {

        ExpressionExperiment ee;
        if ( bas instanceof ExpressionExperimentSubSet ) {
            ee = ( ( ExpressionExperimentSubSet ) bas ).getSourceExperiment();
        } else {
            ee = ( ExpressionExperiment ) bas;
        }

        assert ee != null;

        return CommonQueries.getArrayDesignsUsed( ee, this.getSessionFactory().getCurrentSession() );
    }

    @Override
    public Map<ArrayDesign, Collection<Long>> getArrayDesignsUsed( Collection<Long> eeids ) {
        return CommonQueries.getArrayDesignsUsed( eeids, this.getSessionFactory().getCurrentSession() );
    }

    @Override
    public Collection<BioAssayDimension> getBioAssayDimensions( ExpressionExperiment expressionExperiment ) {
        String queryString = "select distinct b from BioAssayDimension b, ExpressionExperiment e "
                + "inner join b.bioAssays bba inner join e.bioAssays eb where eb = bba and e = :ee ";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ee", expressionExperiment ).list();
    }

    @Override
    public Collection<ExpressionExperiment> getExperimentsWithOutliers() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                "select distinct e from ExpressionExperiment e join e.bioAssays b where b.isOutlier = true" ).list();
    }

    @Override
    public Collection<ExpressionExperimentValueObject> loadAllValueObjectsOrdered( String orderField,
            boolean descending ) {
        return this.loadValueObjectsPreFilter( 0, -1, orderField, !descending, null, true );
    }

    @Override
    public Collection<ExpressionExperimentValueObject> loadAllValueObjectsTaxon( final Taxon taxon ) {
        ObjectFilter[] filter = new ObjectFilter[] {
                new ObjectFilter( "id", taxon.getId(), ObjectFilter.is, ObjectFilter.DAO_TAXON_ALIAS ),
                new ObjectFilter( "parentTaxon.id", taxon.getId(), ObjectFilter.is, ObjectFilter.DAO_TAXON_ALIAS ) };

        return this.loadValueObjectsPreFilter( 0, -1, null, true, filter, true );
    }

    @Override
    public Collection<ExpressionExperimentValueObject> loadValueObjectsOrdered( String orderField, boolean descending,
            Collection<Long> ids ) {
        if ( ids.isEmpty() )
            return Collections.emptyList();

        ObjectFilter[] filter = new ObjectFilter[] {
                new ObjectFilter( "id", ids, ObjectFilter.in, ObjectFilter.DAO_EE_ALIAS ) };

        return this.loadValueObjectsPreFilter( 0, -1, orderField, !descending, filter, true );
    }

    @Override
    public Collection<ExpressionExperimentValueObject> loadAllValueObjectsTaxonOrdered( String orderField,
            boolean descending, Taxon taxon ) {

        final ObjectFilter[] filter = new ObjectFilter[] {
                new ObjectFilter( "id", taxon.getId(), ObjectFilter.is, ObjectFilter.DAO_TAXON_ALIAS ),
                new ObjectFilter( "parentTaxon.id", taxon.getId(), ObjectFilter.is, ObjectFilter.DAO_TAXON_ALIAS ) };

        return this.loadValueObjectsPreFilter( 0, -1, orderField, !descending, filter, true );
    }

    @Override
    public Collection<ExpressionExperiment> loadLackingFactors() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                "select e from ExpressionExperiment e join e.experimentalDesign d where d.experimentalFactors.size =  0" )
                .list();
    }

    @Override
    public Collection<ExpressionExperiment> loadLackingTags() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select e from ExpressionExperiment e where e.characteristics.size = 0" ).list();
    }

    @Override
    public ExpressionExperimentValueObject loadValueObject( Long eeId ) {
        Collection<ExpressionExperimentValueObject> r = this.loadValueObjects( Collections.singleton( eeId ), false );
        if ( r.isEmpty() )
            return null;
        return r.iterator().next();
    }

    @Override
    public Collection<ExpressionExperimentValueObject> loadValueObjects( Collection<Long> ids, boolean maintainOrder ) {
        boolean isList = ( ids instanceof List );
        if ( ids == null || ids.size() == 0 ) {
            if ( isList ) {
                return Collections.emptyList();
            }
            return Collections.emptySet();
        }

        List<Long> idl = new ArrayList<>( ids );
        Collections.sort( idl ); // so it's consistent and therefore cacheable.

        final ObjectFilter[] filter = new ObjectFilter[] {
                new ObjectFilter( "id", idl, ObjectFilter.in, ObjectFilter.DAO_EE_ALIAS ) };

        Collection<ExpressionExperimentValueObject> vos = this
                .loadValueObjectsPreFilter( 0, -1, null, true, filter, true );

        Collection<ExpressionExperimentValueObject> finalValues = new LinkedHashSet<>();
        if ( maintainOrder ) {
            Map<Long, ExpressionExperimentValueObject> map = this.getExpressionExperimentValueObjectMap( vos );
            for ( Long id : ids ) {
                if ( map.get( id ) != null ) {
                    finalValues.add( map.get( id ) );
                }
            }
        } else {
            finalValues = vos;
        }

        if ( finalValues.isEmpty() ) {
            AbstractDao.log.error( "No values were retrieved for the ids provided" );
        }

        if ( isList ) {
            return new ArrayList<>( finalValues );
        }

        return finalValues;
    }

    @Override
    public Collection<ExpressionExperiment> findByBibliographicReference( Long bibRefID ) {
        //language=HQL
        final String queryString =
                "select distinct ee FROM ExpressionExperiment as ee left join ee.otherRelevantPublications as eeO"
                        + " WHERE ee.primaryPublication.id = :bibID OR (eeO.id = :bibID) ";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "bibID", bibRefID )
                .list();
    }

    @Override
    public ExpressionExperiment findByBioAssay( BioAssay ba ) {

        //language=HQL
        final String queryString =
                "select distinct ee from ExpressionExperiment as ee inner join ee.bioAssays as ba " + "where ba = :ba";
        List list = this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ba", ba )
                .list();

        if ( list.size() == 0 ) {
            AbstractDao.log.warn( "No expression experiment for " + ba );
            return null;
        }

        if ( list.size() > 1 ) {
            /*
             * This really shouldn't happen!
             */
            AbstractDao.log.warn( "Found " + list.size() + " expression experiment for the given bio assay: " + ba
                    + " Only 1 returned." );
        }
        return ( ExpressionExperiment ) list.iterator().next();
    }

    @Override
    public ExpressionExperiment findByBioMaterial( BioMaterial bm ) {

        //language=HQL
        final String queryString = "select distinct ee from ExpressionExperiment as ee "
                + "inner join ee.bioAssays as ba inner join ba.sampleUsed as sample where sample = :bm";

        List list = this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "bm", bm )
                .list();

        if ( list.size() == 0 ) {
            AbstractDao.log.warn( "No expression experiment for " + bm );
            return null;
        }
        if ( list.size() > 1 ) {
            /*
             * This really shouldn't happen!
             */
            AbstractDao.log.warn( "Found " + list.size() + " expression experiment for the given bm: " + bm
                    + " Only 1 returned." );
        }
        return ( ExpressionExperiment ) list.iterator().next();
    }

    @Override
    public Collection<ExpressionExperiment> findByBioMaterials( Collection<BioMaterial> bms ) {
        if ( bms == null || bms.size() == 0 ) {
            return new HashSet<>();
        }
        //language=HQL
        final String queryString = "select distinct ee from ExpressionExperiment as ee "
                + "inner join ee.bioAssays as ba inner join ba.sampleUsed as sample where sample in (:bms)";

        Collection<ExpressionExperiment> results = new HashSet<>();
        Collection<BioMaterial> batch = new HashSet<>();

        for ( BioMaterial o : bms ) {
            batch.add( o );
            if ( batch.size() == ExpressionExperimentDaoImpl.BATCH_SIZE ) {

                //noinspection unchecked
                results.addAll( this.getSessionFactory().getCurrentSession().createQuery( queryString )
                        .setParameterList( "bms", batch ).list() );
                batch.clear();
            }
        }

        if ( batch.size() > 0 ) {

            //noinspection unchecked
            results.addAll( this.getSessionFactory().getCurrentSession().createQuery( queryString )
                    .setParameterList( "bms", batch ).list() );
        }

        return results;
    }

    @Override
    public Collection<ExpressionExperiment> findByExpressedGene( Gene gene, Double rank ) {

        //language=MySQL
        final String queryString = "SELECT DISTINCT ee.ID AS eeID FROM "
                + "GENE2CS g2s, COMPOSITE_SEQUENCE cs, PROCESSED_EXPRESSION_DATA_VECTOR dedv, INVESTIGATION ee "
                + "WHERE g2s.CS = cs.ID AND cs.ID = dedv.DESIGN_ELEMENT_FK AND dedv.EXPRESSION_EXPERIMENT_FK = ee.ID"
                + " AND g2s.gene = :geneID AND dedv.RANK_BY_MEAN >= :rank";

        Collection<Long> eeIds;

        try {
            Session session = this.getSessionFactory().getCurrentSession();
            org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
            queryObject.setLong( "geneID", gene.getId() );
            queryObject.setDouble( "rank", rank );
            queryObject.addScalar( "eeID", new LongType() );
            ScrollableResults results = queryObject.scroll();

            eeIds = new HashSet<>();

            // Post Processing
            while ( results.next() )
                eeIds.add( results.getLong( 0 ) );

            session.clear();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        return this.load( eeIds );
    }

    @Override
    public ExpressionExperiment findByFactor( ExperimentalFactor ef ) {
        //language=HQL
        final String queryString =
                "select distinct ee from ExpressionExperiment as ee inner join ee.experimentalDesign ed "
                        + "inner join ed.experimentalFactors ef where ef = :ef ";

        List results = this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ef", ef )
                .list();

        if ( results.size() == 0 ) {
            AbstractDao.log.info( "There is no expression experiment that has factor = " + ef );
            return null;
        }
        return ( ExpressionExperiment ) results.iterator().next();
    }

    @Override
    public ExpressionExperiment findByFactorValue( FactorValue fv ) {
        return this.findByFactorValue( fv.getId() );
    }

    @Override
    public ExpressionExperiment findByFactorValue( Long factorValueId ) {
        //language=HQL
        final String queryString =
                "select distinct ee from ExpressionExperiment as ee inner join ee.experimentalDesign ed "
                        + "inner join ed.experimentalFactors ef inner join ef.factorValues fv where fv.id = :fvId ";

        //noinspection unchecked
        List<ExpressionExperiment> results = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "fvId", factorValueId ).list();

        if ( results.size() == 0 ) {
            AbstractDao.log.info( "There is no expression experiment that has factorValue ID= " + factorValueId );
            return null;
        }

        return results.get( 0 );
    }

    @Override
    public Collection<ExpressionExperiment> findByFactorValues( Collection<FactorValue> fvs ) {

        if ( fvs.isEmpty() )
            return new HashSet<>();

        // Thaw the factor values.
        //noinspection unchecked
        Collection<ExperimentalDesign> eds = this.getSessionFactory().getCurrentSession().createQuery(
                "select ed from FactorValue f join f.experimentalFactor ef "
                        + " join ef.experimentalDesign ed where f.id in (:ids)" )
                .setParameterList( "ids", EntityUtils.getIds( fvs ) ).list();

        if ( eds.isEmpty() ) {
            return new HashSet<>();
        }

        //language=HQL
        final String queryString = "select distinct ee from ExpressionExperiment as ee where ee.experimentalDesign in (:eds) ";
        Collection<ExpressionExperiment> results = new HashSet<>();
        Collection<ExperimentalDesign> batch = new HashSet<>();
        for ( ExperimentalDesign o : eds ) {
            batch.add( o );
            if ( batch.size() == ExpressionExperimentDaoImpl.BATCH_SIZE ) {

                //noinspection unchecked
                results.addAll( this.getSessionFactory().getCurrentSession().createQuery( queryString )
                        .setParameterList( "eds", batch ).list() );
                batch.clear();
            }
        }

        if ( batch.size() > 0 ) {

            //noinspection unchecked
            results.addAll( this.getSessionFactory().getCurrentSession().createQuery( queryString )
                    .setParameterList( "eds", batch ).list() );
        }

        return results;

    }

    @Override
    public Collection<ExpressionExperiment> findByGene( Gene gene ) {

        /*
         * uses GENE2CS table.
         */
        //language=MySQL
        final String queryString = "SELECT DISTINCT ee.ID AS eeID FROM "
                + "GENE2CS g2s, COMPOSITE_SEQUENCE cs, ARRAY_DESIGN ad, BIO_ASSAY ba, INVESTIGATION ee "
                + "WHERE g2s.CS = cs.ID AND ad.ID = cs.ARRAY_DESIGN_FK AND ba.ARRAY_DESIGN_USED_FK = ad.ID AND"
                + " ba.EXPRESSION_EXPERIMENT_FK = ee.ID AND g2s.GENE = :geneID";

        Collection<Long> eeIds;

        Session session = this.getSessionFactory().getCurrentSession();
        org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
        queryObject.setLong( "geneID", gene.getId() );
        queryObject.addScalar( "eeID", new LongType() );
        ScrollableResults results = queryObject.scroll();

        eeIds = new HashSet<>();

        while ( results.next() ) {
            eeIds.add( results.getLong( 0 ) );
        }

        return this.load( eeIds );
    }

    @Override
    public Collection<ExpressionExperiment> findByParentTaxon( Taxon taxon ) {
        //language=HQL
        final String queryString =
                "select distinct ee from ExpressionExperiment as ee " + "inner join ee.bioAssays as ba "
                        + "inner join ba.sampleUsed as sample "
                        + "inner join sample.sourceTaxon as childtaxon where childtaxon.parentTaxon  = :taxon ";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "taxon", taxon )
                .list();
    }

    @Override
    public ExpressionExperiment findByQuantitationType( QuantitationType quantitationType ) {
        //language=HQL
        final String queryString =
                "select ee from ExpressionExperiment as ee " + "inner join ee.quantitationTypes qt where qt = :qt ";

        List results = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "qt", quantitationType ).list();

        if ( results.size() == 1 ) {
            return ( ExpressionExperiment ) results.iterator().next();
        } else if ( results.size() == 0 ) {
            return null;
        }

        throw new IllegalStateException( "More than one ExpressionExperiment associated with " + quantitationType );
    }

    @Override
    public Collection<ExpressionExperiment> findByTaxon( Taxon taxon ) {
        //language=HQL
        final String queryString =
                "select distinct ee from ExpressionExperiment as ee " + "inner join ee.bioAssays as ba "
                        + "inner join ba.sampleUsed as sample where sample.sourceTaxon = :taxon or sample.sourceTaxon.parentTaxon = :taxon";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "taxon", taxon )
                .list();
    }

    @Override
    public Map<Long, Integer> getAnnotationCounts( Collection<Long> ids ) {
        Map<Long, Integer> results = new HashMap<>();
        for ( Long id : ids ) {
            results.put( id, 0 );
        }
        if ( ids.size() == 0 ) {
            return results;
        }
        String queryString = "select e.id,count(c.id) from ExpressionExperiment e inner join e.characteristics c where e.id in (:ids) group by e.id";
        List res = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list();

        this.addIdsToResults( results, res );
        return results;
    }

    @Override
    @Deprecated
    public Map<Long, Map<Long, Collection<AuditEvent>>> getArrayDesignAuditEvents( Collection<Long> ids ) {
        //language=HQL
        final String queryString =
                "select ee.id, ad.id, event " + "from ExpressionExperiment ee " + "inner join ee.bioAssays b "
                        + "inner join b.arrayDesignUsed ad " + "inner join ad.auditTrail trail "
                        + "inner join trail.events event " + "where ee.id in (:ids) ";

        List result = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list();

        Map<Long, Map<Long, Collection<AuditEvent>>> eventMap = new HashMap<>();
        // process list of expression experiment ids that have events
        for ( Object o : result ) {
            Object[] row = ( Object[] ) o;
            Long eeId = ( Long ) row[0];
            Long adId = ( Long ) row[1];
            AuditEvent event = ( AuditEvent ) row[2];

            Map<Long, Collection<AuditEvent>> adEventMap = eventMap.get( eeId );
            if ( adEventMap == null ) {
                adEventMap = new HashMap<>();
                eventMap.put( eeId, adEventMap );
            }

            Collection<AuditEvent> events = adEventMap.get( adId );
            if ( events == null ) {
                events = new ArrayList<>();
                adEventMap.put( adId, events );
            }

            events.add( event );
        }
        return eventMap;

    }

    @Override
    public Map<Long, Collection<AuditEvent>> getAuditEvents( Collection<Long> ids ) {
        //language=HQL
        final String queryString =
                "select ee.id, auditEvent from ExpressionExperiment ee inner join ee.auditTrail as auditTrail inner join auditTrail.events as auditEvent "
                        + " where ee.id in (:ids) ";

        List result = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list();

        Map<Long, Collection<AuditEvent>> eventMap = new HashMap<>();

        for ( Object o : result ) {
            Object[] row = ( Object[] ) o;
            Long id = ( Long ) row[0];
            AuditEvent event = ( AuditEvent ) row[1];

            this.addEventsToMap( eventMap, id, event );
        }
        // add in expression experiment ids that do not have events. Set
        // their values to null.
        for ( Object object : ids ) {
            Long id = ( Long ) object;
            if ( !eventMap.containsKey( id ) ) {
                eventMap.put( id, null );
            }
        }
        return eventMap;

    }

    @Override
    public Integer getBioAssayCountById( long Id ) {
        //language=HQL
        final String queryString =
                "select count(ba) from ExpressionExperiment ee " + "inner join ee.bioAssays ba where ee.id = :ee";

        List list = this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ee", Id )
                .list();

        if ( list.size() == 0 ) {
            AbstractDao.log.warn( "No vectors for experiment with id " + Id );
            return 0;
        }

        return ( ( Long ) list.iterator().next() ).intValue();
    }

    @Override
    public Integer getBioMaterialCount( ExpressionExperiment expressionExperiment ) {
        //language=HQL
        final String queryString =
                "select count(distinct sample) from ExpressionExperiment as ee " + "inner join ee.bioAssays as ba "
                        + "inner join ba.sampleUsed as sample where ee.id = :eeId ";

        List result = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "eeId", expressionExperiment.getId() ).list();

        return ( ( Long ) result.iterator().next() ).intValue();
    }

    /**
     * @param Id if of the expression experiment
     * @return count of RAW vectors.
     */
    @Override
    public Integer getDesignElementDataVectorCountById( long Id ) {

        //language=HQL
        final String queryString = "select count(dedv) from ExpressionExperiment ee "
                + "inner join ee.rawExpressionDataVectors dedv where ee.id = :ee";

        List list = this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ee", Id )
                .list();
        if ( list.size() == 0 ) {
            AbstractDao.log.warn( "No vectors for experiment with id " + Id );
            return 0;
        }
        return ( ( Long ) list.iterator().next() ).intValue();

    }

    @Override
    public Map<Long, Date> getLastArrayDesignUpdate( Collection<ExpressionExperiment> expressionExperiments ) {
        //language=HQL
        final String queryString = "select ee.id, max(s.lastUpdated) from ExpressionExperiment as ee inner join "
                + "ee.bioAssays b inner join b.arrayDesignUsed a join a.curationDetails s "
                + " where ee in (:ees) group by ee.id ";

        List res = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ees", expressionExperiments ).list();

        assert ( !res.isEmpty() );

        Map<Long, Date> result = new HashMap<>();
        for ( Object o : res ) {
            Object[] oa = ( Object[] ) o;
            Long id = ( Long ) oa[0];
            Date d = ( Date ) oa[1];
            result.put( id, d );
        }
        return result;
    }

    @Override
    public Date getLastArrayDesignUpdate( ExpressionExperiment ee ) {

        //language=HQL
        final String queryString = "select max(s.lastUpdated) from ExpressionExperiment as ee inner join "
                + "ee.bioAssays b inner join b.arrayDesignUsed a join a.curationDetails s " + " where ee = :ee ";

        List res = this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ee", ee )
                .list();

        assert ( !res.isEmpty() );

        return ( Date ) res.iterator().next();
    }

    @Override
    public QuantitationType getMaskedPreferredQuantitationType( ExpressionExperiment ee ) {
        String queryString = "select q from ExpressionExperiment e inner join e.quantitationTypes q where e = :ee and q.isMaskedPreferred = true";
        List k = this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ee", ee )
                .list();

        if ( k.size() == 1 ) {
            return ( QuantitationType ) k.iterator().next();
        } else if ( k.size() > 1 ) {
            throw new IllegalStateException(
                    "There should only be one masked preferred quantitationType per expressionExperiment (" + ee
                            + ")" );
        }
        return null;
    }

    @Override
    public Map<Taxon, Long> getPerTaxonCount() {

        Map<Taxon, Taxon> taxonParents = new HashMap<>();

        //noinspection unchecked
        List<Object[]> tp = this.getSessionFactory().getCurrentSession()
                .createQuery( "select t, p from Taxon t left outer join t.parentTaxon p" ).list();
        for ( Object[] o : tp ) {
            taxonParents.put( ( Taxon ) o[0], ( Taxon ) o[1] );
        }

        Map<Taxon, Long> taxonCount = new LinkedHashMap<>();
        String queryString = "select t, count(distinct ee) from ExpressionExperiment "
                + "ee inner join ee.bioAssays as ba inner join ba.sampleUsed su "
                + "inner join su.sourceTaxon t group by t order by t.scientificName ";

        // it is important to cache this, as it gets called on the home page. Though it's actually fast.
        org.hibernate.Query queryObject = this.getSessionFactory().getCurrentSession().createQuery( queryString );
        queryObject.setCacheable( true );
        ScrollableResults list = queryObject.scroll();
        while ( list.next() ) {
            Taxon taxon = ( Taxon ) list.get( 0 );
            Taxon parent = taxonParents.get( taxon );
            Long count = list.getLong( 1 );

            if ( parent != null ) {
                if ( !taxonCount.containsKey( parent ) ) {
                    taxonCount.put( parent, 0L );
                }

                taxonCount.put( parent, taxonCount.get( parent ) + count );

            } else {
                taxonCount.put( taxon, count );
            }
        }
        return taxonCount;

    }

    @Override
    public Map<Long, Integer> getPopulatedFactorCounts( Collection<Long> ids ) {
        Map<Long, Integer> results = new HashMap<>();
        if ( ids.size() == 0 ) {
            return results;
        }

        for ( Long id : ids ) {
            results.put( id, 0 );
        }

        String queryString = "select e.id,count(distinct ef.id) from ExpressionExperiment e inner join e.bioAssays ba"
                + " inner join ba.sampleUsed bm inner join bm.factorValues fv inner join fv.experimentalFactor "
                + "ef where e.id in (:ids) group by e.id";
        List res = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ).list();

        this.addIdsToResults( results, res );
        return results;
    }

    @Override
    public Map<Long, Integer> getPopulatedFactorCountsExcludeBatch( Collection<Long> ids ) {
        Map<Long, Integer> results = new HashMap<>();
        if ( ids.size() == 0 ) {
            return results;
        }

        for ( Long id : ids ) {
            results.put( id, 0 );
        }

        String queryString = "select e.id,count(distinct ef.id) from ExpressionExperiment e inner join e.bioAssays ba"
                + " inner join ba.sampleUsed bm inner join bm.factorValues fv inner join fv.experimentalFactor ef "
                + " inner join ef.category cat where e.id in (:ids) and cat.category != (:category) and ef.name != (:name) group by e.id";

        List res = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ids", ids ) // Set ids
                .setParameter( "category", ExperimentalFactorService.BATCH_FACTOR_CATEGORY_NAME ) // Set batch category
                .setParameter( "name", ExperimentalFactorService.BATCH_FACTOR_NAME ) // set batch name
                .list();

        this.addIdsToResults( results, res );
        return results;
    }

    @Override
    public Map<QuantitationType, Integer> getQuantitationTypeCountById( Long id ) {

        //language=HQL
        final String queryString = "select quantType,count(*) as count "
                + "from ubic.gemma.model.expression.experiment.ExpressionExperiment ee "
                + "inner join ee.rawExpressionDataVectors as vectors "
                + "inner join vectors.quantitationType as quantType " + "where ee.id = :id GROUP BY quantType.name";

        List list = this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "id", id )
                .list();

        Map<QuantitationType, Integer> qtCounts = new HashMap<>();

        //noinspection unchecked
        for ( Object[] tuple : ( List<Object[]> ) list ) {
            qtCounts.put( ( QuantitationType ) tuple[0], ( ( Long ) tuple[1] ).intValue() );
        }

        return qtCounts;
    }

    @Override
    public Collection<QuantitationType> getQuantitationTypes( final ExpressionExperiment expressionExperiment ) {
        //language=HQL
        final String queryString = "select distinct quantType " + "from ExpressionExperiment ee "
                + "inner join ee.quantitationTypes as quantType fetch all properties where ee  = :ee ";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ee", expressionExperiment ).list();
    }

    @Override
    public Collection<QuantitationType> getQuantitationTypes( ExpressionExperiment expressionExperiment,
            ArrayDesign arrayDesign ) {
        if ( arrayDesign == null ) {
            return this.getQuantitationTypes( expressionExperiment );
        }

        //language=HQL
        final String queryString =
                "select distinct quantType " + "from ubic.gemma.model.expression.experiment.ExpressionExperiment ee "
                        + "inner join  ee.quantitationTypes as quantType " + "inner join ee.bioAssays as ba "
                        + "inner join ba.arrayDesignUsed ad " + "where ee = :ee and ad = :ad";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ee", expressionExperiment ) // Set the EE
                .setParameter( "ad", arrayDesign ) // Set the AD
                .list();
    }

    @Override
    public Map<ExpressionExperiment, Collection<AuditEvent>> getSampleRemovalEvents(
            Collection<ExpressionExperiment> expressionExperiments ) {
        //language=HQL
        final String queryString = "select ee,ev from ExpressionExperiment ee inner join ee.auditTrail trail inner join"
                + " trail.events ev inner join ev.eventType et "
                + "inner join fetch ev.performer where ee in (:ees) and et.class = 'SampleRemovalEvent'";

        Map<ExpressionExperiment, Collection<AuditEvent>> result = new HashMap<>();
        List r = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "ees", expressionExperiments ).list();

        for ( Object o : r ) {
            Object[] ol = ( Object[] ) o;
            ExpressionExperiment e = ( ExpressionExperiment ) ol[0];
            if ( !result.containsKey( e ) ) {
                result.put( e, new HashSet<AuditEvent>() );
            }
            AuditEvent ae = ( AuditEvent ) ol[1];
            result.get( e ).add( ae );
        }
        return result;
    }

    @Override
    public Collection<ExpressionExperimentSubSet> getSubSets( ExpressionExperiment expressionExperiment ) {
        String queryString = "select eess from ExpressionExperimentSubSet eess inner join eess.sourceExperiment ee where ee = :ee";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ee", expressionExperiment ).list();
    }

    @Override
    public Taxon getTaxon( BioAssaySet ee ) {

        if ( ee instanceof ExpressionExperiment ) {
            String queryString = "select SU.sourceTaxon from ExpressionExperiment as EE "
                    + "inner join EE.bioAssays as BA inner join BA.sampleUsed as SU where EE = :ee";
            List list = this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ee", ee )
                    .list();
            if ( list.size() > 0 )
                return ( Taxon ) list.iterator().next();
        } else if ( ee instanceof ExpressionExperimentSubSet ) {
            String queryString =
                    "select su.sourceTaxon from ExpressionExperimentSubSet eess inner join eess.sourceExperiment ee"
                            + " inner join ee.bioAssays as BA inner join BA.sampleUsed as su where eess = :ee";
            List list = this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ee", ee )
                    .list();
            if ( list.size() > 0 )
                return ( Taxon ) list.iterator().next();
        } else {
            throw new UnsupportedOperationException(
                    "Can't get taxon of BioAssaySet of class " + ee.getClass().getName() );
        }

        return null;
    }

    @Override
    public <T extends BioAssaySet> Map<T, Taxon> getTaxa( Collection<T> bioAssaySets ) {
        Map<T, Taxon> result = new HashMap<>();

        if ( bioAssaySets.isEmpty() )
            return result;

        // is this going to run into problems if there are too many ees given? Need to batch?
        T example = bioAssaySets.iterator().next();
        List list;
        if ( ExpressionExperiment.class.isAssignableFrom( example.getClass() ) ) {
            String queryString = "select EE, SU.sourceTaxon from ExpressionExperiment as EE "
                    + "inner join EE.bioAssays as BA inner join BA.sampleUsed as SU where EE in (:ees)";
            list = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                    .setParameterList( "ees", bioAssaySets ).list();
        } else if ( ExpressionExperimentSubSet.class.isAssignableFrom( example.getClass() ) ) {
            String queryString =
                    "select eess, su.sourceTaxon from ExpressionExperimentSubSet eess inner join eess.sourceExperiment ee"
                            + " inner join ee.bioAssays as BA inner join BA.sampleUsed as su where eess in (:ees)";
            list = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                    .setParameterList( "ees", bioAssaySets ).list();
        } else {
            throw new UnsupportedOperationException(
                    "Can't get taxon of BioAssaySet of class " + example.getClass().getName() );
        }

        for ( Object o : list ) {
            Object[] oa = ( Object[] ) o;

            @SuppressWarnings("unchecked") T e = ( T ) oa[0];
            Taxon t = ( Taxon ) oa[1];
            result.put( e, t );

        }

        return result;
    }

    @Override
    public Collection<ExpressionExperiment> findUpdatedAfter( Date date ) {
        if ( date == null )
            return Collections.emptyList();
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                "select e from ExpressionExperiment e join e.curationDetails cd where cd.lastUpdated > :date" )
                .setDate( "date", date ).list();
    }

    /**
     * @deprecated use the service layer ({@link ExpressionExperimentService}) for EE removal. There is mandatory house
     * keeping before you can
     * remove the experiment. Attempting to call this method directly will likely result in
     * org.hibernate.exception.ConstraintViolationException
     */
    @Override
    @Deprecated
    public void remove( Long id ) {
        throw new NotImplementedException(
                "Use the EEService.remove(ExpressionExperiment) instead, this method would not do what you want it to." );
    }

    @Override
    public void remove( final ExpressionExperiment ee ) {

        if ( ee == null )
            throw new IllegalArgumentException();

        Session session = this.getSessionFactory().getCurrentSession();

        try {
            // Note that links and analyses are deleted separately - see the ExpressionExperimentService.

            // At this point, the ee is probably still in the session, as the service already has gotten it
            // in this transaction.
            session.flush();
            session.clear();

            session.buildLockRequest( LockOptions.NONE ).lock( ee );

            Hibernate.initialize( ee.getAuditTrail() );

            Set<BioAssayDimension> dims = new HashSet<>();
            Set<QuantitationType> qts = new HashSet<>();
            Collection<RawExpressionDataVector> designElementDataVectors = ee.getRawExpressionDataVectors();
            Hibernate.initialize( designElementDataVectors );
            ee.setRawExpressionDataVectors( null );

            int count = 0;
            if ( designElementDataVectors != null ) {
                count = this.removeDataVectors( session, dims, qts, designElementDataVectors, count );
            }

            Collection<ProcessedExpressionDataVector> processedVectors = ee.getProcessedExpressionDataVectors();

            Hibernate.initialize( processedVectors );
            if ( processedVectors != null && processedVectors.size() > 0 ) {
                ee.setProcessedExpressionDataVectors( null );
                this.removeProcessedVectors( session, dims, qts, count, processedVectors );
            }

            session.flush();
            session.clear();
            session.update( ee );

            AbstractDao.log.info( "Removing BioAssay Dimensions ..." );
            for ( BioAssayDimension dim : dims ) {
                dim.getBioAssays().clear();
                session.update( dim );
                session.delete( dim );
            }
            dims.clear();
            session.flush();

            AbstractDao.log.info( "Removing Bioassays and biomaterials ..." );

            // keep to put back in the object.
            Map<BioAssay, BioMaterial> copyOfRelations = new HashMap<>();

            Collection<BioMaterial> bioMaterialsToDelete = new HashSet<>();
            Collection<BioAssay> bioAssays = ee.getBioAssays();
            this.removeBioAssays( session, copyOfRelations, bioMaterialsToDelete, bioAssays );

            AbstractDao.log.info( "Last bits ..." );

            // We remove them here in case they are associated to more than one bioassay-- no cascade is possible.
            for ( BioMaterial bm : bioMaterialsToDelete ) {
                session.delete( bm );
            }

            for ( QuantitationType qt : qts ) {
                session.delete( qt );
            }

            session.flush();
            session.delete( ee );

            /*
             * Put transient instances back. This is possibly useful for clearing ACLS.
             */
            ee.setProcessedExpressionDataVectors( processedVectors );
            ee.setRawExpressionDataVectors( designElementDataVectors );
            for ( BioAssay ba : ee.getBioAssays() ) {
                ba.setSampleUsed( copyOfRelations.get( ba ) );
            }

            AbstractDao.log.info( "Deleted " + ee );
        } catch ( Exception e ) {
            AbstractDao.log.error( e );
        } finally {
            AbstractDao.log.info( "Finalising remove method." );
        }
    }

    @Override
    public ExpressionExperiment find( ExpressionExperiment entity ) {

        Criteria criteria = this.getSessionFactory().getCurrentSession().createCriteria( ExpressionExperiment.class );

        if ( entity.getAccession() != null ) {
            criteria.add( Restrictions.eq( "accession", entity.getAccession() ) );
        } else if ( entity.getShortName() != null ) {
            criteria.add( Restrictions.eq( "shortName", entity.getShortName() ) );
        } else {
            criteria.add( Restrictions.eq( "name", entity.getName() ) );
        }

        return ( ExpressionExperiment ) criteria.uniqueResult();
    }

    @Override
    public ExpressionExperiment findOrCreate( ExpressionExperiment entity ) {
        if ( entity.getShortName() == null && entity.getName() == null && entity.getAccession() == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment must have name or external accession." );
        }
        return super.findOrCreate( entity );
    }

    @Override
    public ExpressionExperimentValueObject loadValueObject( ExpressionExperiment entity ) {
        return this.loadValueObject( entity.getId() );
    }

    @Override
    public Collection<ExpressionExperimentValueObject> loadValueObjects( Collection<ExpressionExperiment> entities ) {
        return this.loadValueObjects( EntityUtils.getIds( entities ), false );
    }

    @Override
    public Collection<ExpressionExperimentValueObject> loadAllValueObjects() {
        return this.loadValueObjectsPreFilter( 0, -1, null, true, null );
    }

    /**
     * Loads value objects of experiments matching the given criteria using a query that pre-filters for EEs
     * that the currently logged-in user can access. This way the returned amount and offset is always guaranteed
     * to be correct, since the ACL interceptors will not remove any more objects from the returned collection.
     *
     * @param offset  amount of EEs to skip.
     * @param limit   maximum amount of EEs to retrieve.
     * @param orderBy the field to order the EEs by. Has to be a valid identifier, or exception is thrown. Can either
     *                be a property of EE itself, or any nested property that hibernate can reach.
     *                E.g. "curationDetails.lastUpdated". Works for multi-level nesting as well.
     * @param asc     true, to order by the {@code orderBy} in ascending, or false for descending order.
     * @param filter  see this#formRestrictionClause(ArrayList) filters argument for description.
     * @return list of value objects representing the EEs that matched the criteria.
     */
    @Override
    public Collection<ExpressionExperimentValueObject> loadValueObjectsPreFilter( int offset, int limit, String orderBy,
            boolean asc, ArrayList<ObjectFilter[]> filter ) {

        String orderByProperty = this.getOrderByProperty( orderBy );

        // Compose query
        Query query = this.getLoadValueObjectsQueryString( filter, orderByProperty, !asc );
        query.setCacheable( true );
        query.setMaxResults( limit > 0 ? limit : -1 );
        query.setFirstResult( offset );

        //noinspection unchecked
        List<Object[]> list = query.list();
        List<ExpressionExperimentValueObject> vos = new ArrayList<>( list.size() );

        for ( Object[] row : list ) {
            ExpressionExperimentValueObject vo = new ExpressionExperimentValueObject( row );
            vos.add( vo );
        }

        return vos;
    }

    private void removeBioAssays( Session session, Map<BioAssay, BioMaterial> copyOfRelations,
            Collection<BioMaterial> bioMaterialsToDelete, Collection<BioAssay> bioAssays ) {
        for ( BioAssay ba : bioAssays ) {
            // relations to files cascade, so we only have to worry about biomaterials, which aren't cascaded from
            // anywhere. BioAssay -> BioMaterial is many-to-one, but bioassaySet (experiment) owns the bioAssay.
            BioMaterial biomaterial = ba.getSampleUsed();

            if ( biomaterial == null )
                continue; // shouldn't...

            bioMaterialsToDelete.add( biomaterial );

            copyOfRelations.put( ba, biomaterial );

            session.buildLockRequest( LockOptions.NONE ).lock( biomaterial );

            Hibernate.initialize( biomaterial );

            // this can easily end up with an unattached object.
            Hibernate.initialize( biomaterial.getBioAssaysUsedIn() );

            biomaterial.getFactorValues().clear();
            biomaterial.getBioAssaysUsedIn().clear();

            ba.setSampleUsed( null );
        }
    }

    private void removeProcessedVectors( Session session, Set<BioAssayDimension> dims, Set<QuantitationType> qts,
            int count, Collection<ProcessedExpressionDataVector> processedVectors ) {
        for ( ProcessedExpressionDataVector dv : processedVectors ) {
            BioAssayDimension bad = dv.getBioAssayDimension();
            dims.add( bad );
            QuantitationType qt = dv.getQuantitationType();
            qts.add( qt );
            dv.setBioAssayDimension( null );
            dv.setQuantitationType( null );
            session.delete( dv );
            if ( ++count % 1000 == 0 ) {
                session.flush();
            }
            if ( count % 20000 == 0 ) {
                AbstractDao.log.info( count + " processed design Element data vectors deleted" );
            }

            // put back..
            dv.setBioAssayDimension( bad );
            dv.setQuantitationType( qt );
        }
    }

    private int removeDataVectors( Session session, Set<BioAssayDimension> dims, Set<QuantitationType> qts,
            Collection<RawExpressionDataVector> designElementDataVectors, int count ) {
        AbstractDao.log.info( "Removing Design Element Data Vectors ..." );
        for ( RawExpressionDataVector dv : designElementDataVectors ) {
            BioAssayDimension bad = dv.getBioAssayDimension();
            dims.add( bad );
            QuantitationType qt = dv.getQuantitationType();
            qts.add( qt );
            dv.setBioAssayDimension( null );
            dv.setQuantitationType( null );
            session.delete( dv );
            if ( ++count % 1000 == 0 ) {
                session.flush();
            }
            // put back...
            dv.setBioAssayDimension( bad );
            dv.setQuantitationType( qt );

            if ( count % 20000 == 0 ) {
                AbstractDao.log.info( count + " design Element data vectors deleted" );
            }
        }
        count = 0;
        return count;
    }

    /**
     * @param offset      amount of EEs to skip.
     * @param limit       maximum amount of EEs to retrieve.
     * @param orderBy     the property to order by.
     * @param asc         whether the ordering is ascending or descending.
     * @param filters     An array representing either a conjunction (AND) or disjunction (OR) of filters.
     * @param disjunction true to signal that the filters property is a disjunction (OR). False will cause the
     *                    filters property to be treated as a conjunction (AND).
     *                    If you are passing a single filter, using <code>false</code> is slightly more effective;
     * @return a hibernate Query object ready to be used for EEVO retrieval.
     */
    @SuppressWarnings("SameParameterValue") // Better reusability
    private Collection<ExpressionExperimentValueObject> loadValueObjectsPreFilter( int offset, int limit,
            String orderBy, boolean asc, ObjectFilter[] filters, boolean disjunction ) {
        if ( filters == null ) {
            return this.loadValueObjectsPreFilter( offset, limit, orderBy, asc, null );
        }

        ArrayList<ObjectFilter[]> filterList = new ArrayList<>( disjunction ? filters.length : 1 );
        if ( disjunction ) {
            ObjectFilter[] filterArray = new ObjectFilter[filters.length];
            int i = 0;
            for ( ObjectFilter filter : filters ) {
                filterArray[i++] = filter;
            }
            filterList.add( filterArray );
        } else {
            for ( ObjectFilter filter : filters ) {
                filterList.add( new ObjectFilter[] { filter } );
            }
        }

        return this.loadValueObjectsPreFilter( offset, limit, orderBy, asc, filterList );
    }

    /**
     * Creates an order by parameter. Expecting either one of the options from the ExtJS frontend (taxon, bioAssayCount,
     * lastUpdated,troubled or needsAttention), or a property of an {@link ExpressionExperiment}. Nested properties
     * (even
     * multiple levels) are allowed. E.g: "accession", "curationDetails.lastUpdated",
     * "curationDetails.lastTroubledEvent.date"
     *
     * @param orderBy the order field requested by front end or API.
     * @return a string that can be used as the orderByProperty param in
     * {@link this#getLoadValueObjectsQueryString(ArrayList, String, boolean)}.
     */
    private String getOrderByProperty( String orderBy ) {
        if ( orderBy == null )
            return ObjectFilter.DAO_EE_ALIAS + ".id";
        String orderByField;
        switch ( orderBy ) {
            case "taxon":
                orderByField = "taxon.id";
                break;
            case "bioAssayCount":
                orderByField = "count(BA)";
                break;
            case "lastUpdated":
                orderByField = "s.lastUpdated";
                break;
            case "troubled":
                orderByField = "s.troubled";
                break;
            case "needsAttention":
                orderByField = "s.needsAttention";
                break;
            default:
                orderByField = ObjectFilter.DAO_EE_ALIAS + "." + orderBy;
                break;
        }
        return orderByField;
    }

    private void addIdsToResults( Map<Long, Integer> results, List res ) {
        for ( Object r : res ) {
            Object[] ro = ( Object[] ) r;
            Long id = ( Long ) ro[0];
            Integer count = ( ( Long ) ro[1] ).intValue();
            results.put( id, count );
        }
    }

    private ExpressionExperiment thaw( ExpressionExperiment ee, boolean vectorsAlso ) {
        if ( ee == null ) {
            return null;
        }

        if ( ee.getId() == null )
            throw new IllegalArgumentException( "id cannot be null, cannot be thawed: " + ee );

        /*
         * Trying to do everything fails miserably, so we still need a hybrid approach. But returning the thawed object,
         * as opposed to thawing the one passed in, solves problems.
         */
        String thawQuery = "select distinct e from ExpressionExperiment e "
                + " left join fetch e.accession acc left join fetch acc.externalDatabase where e.id=:eeId";

        List res = this.getSessionFactory().getCurrentSession().createQuery( thawQuery )
                .setParameter( "eeId", ee.getId() ).list();

        if ( res.size() == 0 ) {
            throw new IllegalArgumentException( "No experiment with id=" + ee.getId() + " could be loaded." );
        }
        ExpressionExperiment result = ( ExpressionExperiment ) res.iterator().next();
        Hibernate.initialize( result.getMeanVarianceRelation() );
        Hibernate.initialize( result.getQuantitationTypes() );
        Hibernate.initialize( result.getCharacteristics() );
        Hibernate.initialize( result.getPrimaryPublication() );
        Hibernate.initialize( result.getOtherRelevantPublications() );
        Hibernate.initialize( result.getBioAssays() );
        Hibernate.initialize( result.getAuditTrail() );
        Hibernate.initialize( result.getGeeq() );
        if ( result.getAuditTrail() != null )
            Hibernate.initialize( result.getAuditTrail().getEvents() );
        Hibernate.initialize( result.getCurationDetails() );

        for ( BioAssay ba : result.getBioAssays() ) {
            Hibernate.initialize( ba.getArrayDesignUsed() );
            Hibernate.initialize( ba.getArrayDesignUsed().getDesignProvider() );
            Hibernate.initialize( ba.getSampleUsed() );
            BioMaterial bm = ba.getSampleUsed();
            if ( bm != null ) {
                Hibernate.initialize( bm.getFactorValues() );
                Hibernate.initialize( bm.getTreatments() );
            }
        }

        ExperimentalDesign experimentalDesign = result.getExperimentalDesign();
        if ( experimentalDesign != null ) {
            Hibernate.initialize( experimentalDesign );
            Hibernate.initialize( experimentalDesign.getExperimentalFactors() );
            experimentalDesign.getTypes().size();
            for ( ExperimentalFactor factor : experimentalDesign.getExperimentalFactors() ) {
                Hibernate.initialize( factor.getAnnotations() );
                for ( FactorValue f : factor.getFactorValues() ) {
                    Hibernate.initialize( f.getCharacteristics() );
                    if ( f.getMeasurement() != null ) {
                        Hibernate.initialize( f.getMeasurement() );
                        if ( f.getMeasurement().getUnit() != null ) {
                            Hibernate.initialize( f.getMeasurement().getUnit() );
                        }
                    }
                }
            }
        }

        this.thawReferences( result );
        this.thawMeanVariance( result );

        if ( vectorsAlso ) {
            /*
             * Optional because this could be slow.
             */
            Hibernate.initialize( result.getRawExpressionDataVectors() );
            Hibernate.initialize( result.getProcessedExpressionDataVectors() );

        }

        return result;
    }

    /**
     * Method for the front end display
     *
     * @param ee expression experiment to be thawed
     * @return thawed expression experiment.
     */
    private ExpressionExperiment thawLiter( ExpressionExperiment ee ) {
        if ( ee == null ) {
            return null;
        }

        if ( ee.getId() == null )
            throw new IllegalArgumentException( "id cannot be null, cannot be thawed: " + ee );

        /*
         * Trying to do everything fails miserably, so we still need a hybrid approach. But returning the thawed object,
         * as opposed to thawing the one passed in, solves problems.
         */
        String thawQuery = "select distinct e from ExpressionExperiment e "
                + " left join fetch e.accession acc left join fetch acc.externalDatabase " + "where e.id=:eeId";

        List res = this.getSessionFactory().getCurrentSession().createQuery( thawQuery )
                .setParameter( "eeId", ee.getId() ).list();

        if ( res.size() == 0 ) {
            throw new IllegalArgumentException( "No experiment with id=" + ee.getId() + " could be loaded." );
        }
        ExpressionExperiment result = ( ExpressionExperiment ) res.iterator().next();
        Hibernate.initialize( result.getPrimaryPublication() );
        Hibernate.initialize( result.getCurationDetails() );
        Hibernate.initialize( result.getGeeq() );

        ExperimentalDesign experimentalDesign = result.getExperimentalDesign();
        if ( experimentalDesign != null ) {
            Hibernate.initialize( experimentalDesign );
            Hibernate.initialize( experimentalDesign.getExperimentalFactors() );
        }

        this.thawReferences( result );
        this.thawMeanVariance( result );

        return result;
    }

    /**
     * @param filters         see {@link this#formRestrictionClause(ArrayList)} filters argument for
     *                        description.
     * @param orderByProperty the property to order by.
     * @param orderDesc       whether the ordering is ascending or descending.
     * @return a hibernate Query object ready to be used for EEVO retrieval.
     */
    private Query getLoadValueObjectsQueryString( ArrayList<ObjectFilter[]> filters, String orderByProperty,
            boolean orderDesc ) {

        // Restrict to non-troubled EEs for non-administrators
        if ( !SecurityUtil.isUserAdmin() ) {
            if ( filters == null ) {
                filters = new ArrayList<>( ExpressionExperimentDaoImpl.NON_ADMIN_QUERY_FILTER_COUNT );
            } else {
                filters.ensureCapacity( filters.size() + ExpressionExperimentDaoImpl.NON_ADMIN_QUERY_FILTER_COUNT );
            }
            // Both restrictions have to be met (AND) therefore they have to be added as separate arrays.
            filters.add( new ObjectFilter[] { new ObjectFilter( "curationDetails.troubled", false, ObjectFilter.is,
                    ObjectFilter.DAO_EE_ALIAS ) } );
            filters.add( new ObjectFilter[] { new ObjectFilter( "curationDetails.troubled", false, ObjectFilter.is,
                    ObjectFilter.DAO_AD_ALIAS ) } );
        }

        //noinspection JpaQlInspection // the constants for aliases is messing with the inspector
        String queryString = "select " + ObjectFilter.DAO_EE_ALIAS + ".id as id, " // 0
                + ObjectFilter.DAO_EE_ALIAS + ".name, " // 1
                + ObjectFilter.DAO_EE_ALIAS + ".source, " // 2
                + ObjectFilter.DAO_EE_ALIAS + ".shortName, " // 3
                + ObjectFilter.DAO_EE_ALIAS + ".metadata, " // 4
                + ObjectFilter.DAO_EE_ALIAS + ".numberOfDataVectors, " // 5
                + "acc.accession, " // 6
                + "ED.name, " // 7
                + "ED.webUri, " // 8
                + ObjectFilter.DAO_EE_ALIAS + ".description, " // 9
                + ObjectFilter.DAO_AD_ALIAS + ".technologyType, "// 10
                + "taxon.commonName, " // 11
                + "taxon.id, " // 12
                + "s.lastUpdated, " // 13
                + "s.troubled, " // 14
                + "s.needsAttention, " // 15
                + "s.curationNote, " // 16
                + "count(distinct BA), " // 17
                + "count(distinct " + ObjectFilter.DAO_AD_ALIAS + "), " // 18
                + "count(distinct SU), " // 19
                + "EDES.id,  " // 20
                + "ptax.id, " // 21
                + "aoi, " // 22
                + "sid, " // 23
                + "qts, " // 24
                + ObjectFilter.DAO_EE_ALIAS + ".batchEffect, " // 25
                + ObjectFilter.DAO_EE_ALIAS + ".batchConfound, " // 26
                + "eNote, " //27
                + "eAttn, " //28
                + "eTrbl, " //29
                + ObjectFilter.DAO_GEEQ_ALIAS + " " //30
                + "from ExpressionExperiment as " + ObjectFilter.DAO_EE_ALIAS + " " + "inner join "
                + ObjectFilter.DAO_EE_ALIAS + ".bioAssays as BA  " + "left join " + ObjectFilter.DAO_EE_ALIAS
                + ".quantitationTypes as qts left join BA.sampleUsed as SU left join BA.arrayDesignUsed as "
                + ObjectFilter.DAO_AD_ALIAS + " left join SU.sourceTaxon as taxon left join "
                + ObjectFilter.DAO_EE_ALIAS + ".accession acc "
                + "left join acc.externalDatabase as ED left join taxon.parentTaxon as ptax " + "left join "
                + ObjectFilter.DAO_EE_ALIAS + ".experimentalDesign as EDES " + "join " + ObjectFilter.DAO_EE_ALIAS
                + ".curationDetails as s left join s.lastNeedsAttentionEvent as eAttn " + "left join "
                + ObjectFilter.DAO_EE_ALIAS + ".geeq as " + ObjectFilter.DAO_GEEQ_ALIAS + " "
                + "left join s.lastNoteUpdateEvent as eNote left join s.lastTroubledEvent as eTrbl ";

        queryString += AbstractVoEnabledDao.formAclSelectClause( ObjectFilter.DAO_EE_ALIAS,
                "ubic.gemma.model.expression.experiment.ExpressionExperiment" );
        queryString += AbstractVoEnabledDao.formRestrictionClause( filters );
        queryString += "group by " + ObjectFilter.DAO_EE_ALIAS + ".id ";
        queryString += AbstractVoEnabledDao.formOrderByProperty( orderByProperty, orderDesc );

        Query query = this.getSessionFactory().getCurrentSession().createQuery( queryString );

        AbstractVoEnabledDao.addRestrictionParameters( query, filters );

        return query;
    }

    private <C extends ExpressionExperimentValueObject> Map<Long, C> getExpressionExperimentValueObjectMap(
            Collection<C> vos ) {

        Map<Long, C> voMap = new LinkedHashMap<>( vos.size() );

        for ( C vo : vos ) {
            voMap.put( vo.getId(), vo );
        }

        return voMap;
    }

    /**
     * Filling 'hasDifferentialExpressionAnalysis' and 'hasCoexpressionAnalysis'
     */
    private void populateAnalysisInformation( Collection<ExpressionExperimentDetailsValueObject> vos ) {

        Map<Long, ExpressionExperimentDetailsValueObject> voIdMap = this.getExpressionExperimentValueObjectMap( vos );

        if ( voIdMap.isEmpty() ) {
            return;
        }

        StopWatch timer = new StopWatch();
        timer.start();

        //noinspection unchecked
        List<Long> withCoexpression = this.getSessionFactory().getCurrentSession().createQuery(
                "select experimentAnalyzed.id from CoexpressionAnalysis where experimentAnalyzed.id in (:ids)" )
                .setParameterList( "ids", voIdMap.keySet() ).list();

        for ( Long id : withCoexpression ) {
            voIdMap.get( id ).setHasCoexpressionAnalysis( true );
        }

        //noinspection unchecked
        List<Long> withDiffEx = this.getSessionFactory().getCurrentSession().createQuery(
                "select experimentAnalyzed.id from DifferentialExpressionAnalysis where experimentAnalyzed.id in (:ids)" )
                .setParameterList( "ids", voIdMap.keySet() ).list();

        for ( Long id : withDiffEx ) {
            voIdMap.get( id ).setHasDifferentialExpressionAnalysis( true );
        }

        if ( timer.getTime() > 200 ) {
            AbstractDao.log
                    .info( "Populate analysis info for " + voIdMap.size() + " eevos: " + timer.getTime() + "ms" );
        }

    }

    private void thawReferences( final ExpressionExperiment expressionExperiment ) {
        if ( expressionExperiment.getPrimaryPublication() != null ) {
            Hibernate.initialize( expressionExperiment.getPrimaryPublication() );
            Hibernate.initialize( expressionExperiment.getPrimaryPublication().getPubAccession() );
            Hibernate
                    .initialize( expressionExperiment.getPrimaryPublication().getPubAccession().getExternalDatabase() );
            //   Hibernate.initialize( expressionExperiment.getPrimaryPublication().getPublicationTypes() );
        }
        if ( expressionExperiment.getOtherRelevantPublications() != null ) {
            Hibernate.initialize( expressionExperiment.getOtherRelevantPublications() );
            for ( BibliographicReference bf : expressionExperiment.getOtherRelevantPublications() ) {
                Hibernate.initialize( bf.getPubAccession() );
                Hibernate.initialize( bf.getPubAccession().getExternalDatabase() );
                //     Hibernate.initialize( bf.getPublicationTypes() );
            }
        }
    }

    private void thawMeanVariance( final ExpressionExperiment expressionExperiment ) {
        if ( expressionExperiment.getMeanVarianceRelation() != null ) {
            Hibernate.initialize( expressionExperiment.getMeanVarianceRelation() );
            Hibernate.initialize( expressionExperiment.getMeanVarianceRelation().getMeans() );
            Hibernate.initialize( expressionExperiment.getMeanVarianceRelation().getVariances() );
        }
    }

    private void fillQuantitationTypeInfo( QuantitationType qt, ExpressionExperimentDetailsValueObject vo ) {

        if ( !vo.getTechnologyType().equals( TechnologyType.ONECOLOR.toString() ) && !vo.getTechnologyType()
                .equals( TechnologyType.NONE.toString() ) ) {

            if ( qt == null ) {
                return;
            }

            boolean hasIntensityA = false;
            boolean hasIntensityB = false;
            boolean mayBeOneChannel = false;

            if ( qt.getIsPreferred() && !qt.getIsRatio() ) {
                /*
                 * This could be a dual-mode array, or it could be mis-labeled as two-color; or this might actually
                 * be ratios. In either case, we should flag it; as it stands we shouldn't use two-channel missing
                 * value analysis on it.
                 */
                mayBeOneChannel = true;
            }
            if ( ChannelUtils.isSignalChannelA( qt.getName() ) ) {
                hasIntensityA = true;
            }
            if ( ChannelUtils.isSignalChannelB( qt.getName() ) ) {
                hasIntensityB = true;
            }

            vo.setHasBothIntensities( hasIntensityA && hasIntensityB && !mayBeOneChannel );
            vo.setHasEitherIntensity( hasIntensityA || hasIntensityB );
        }
    }

}
