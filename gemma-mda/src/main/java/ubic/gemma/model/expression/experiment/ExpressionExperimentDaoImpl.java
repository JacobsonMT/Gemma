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
package ubic.gemma.model.expression.experiment;


import java.math.BigInteger;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.BlobType;
import org.hibernate.type.DoubleType;
import org.hibernate.type.LongType;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.BusinessKey;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.experiment.ExpressionExperiment
 */
public class ExpressionExperimentDaoImpl extends ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase {

    static Log log = LogFactory.getLog( ExpressionExperimentDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#find(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public ExpressionExperiment find( ExpressionExperiment expressionExperiment ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( ExpressionExperiment.class );

            if ( expressionExperiment.getAccession() != null ) {
                queryObject.add( Restrictions.eq( "accession", expressionExperiment.getAccession() ) );
            } else {
                queryObject.add( Restrictions.eq( "name", expressionExperiment.getName() ) );
            }

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + ExpressionExperiment.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( ExpressionExperiment ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#findOrCreate(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public ExpressionExperiment findOrCreate( ExpressionExperiment expressionExperiment ) {
        if ( expressionExperiment.getName() == null && expressionExperiment.getAccession() == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment must have name or external accession." );
        }
        ExpressionExperiment newExpressionExperiment = this.find( expressionExperiment );
        if ( newExpressionExperiment != null ) {

            return newExpressionExperiment;
        }
        log.debug( "Creating new expressionExperiment: " + expressionExperiment.getName() );
        newExpressionExperiment = ( ExpressionExperiment ) create( expressionExperiment );
        return newExpressionExperiment;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#getQuantitationTypeCountById(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public Map handleGetQuantitationTypeCountById( Long Id ) {
        HashMap<QuantitationType, Integer> qtCounts = new HashMap<QuantitationType, Integer>();

        final String queryString = "select quantType,count(*) as count "
                + "from ubic.gemma.model.expression.experiment.ExpressionExperimentImpl ee "
                + "inner join ee.designElementDataVectors as vectors "
                + "inner join  vectors.quantitationType as quantType " + "where ee.id = :id GROUP BY quantType.name";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "id", Id );
            ScrollableResults list = queryObject.scroll();
            while ( list.next() ) {
                qtCounts.put( ( QuantitationType ) list.get( 0 ), list.getInteger( 1 ) );
            }
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        return qtCounts;
    }

    @Override
    public Collection handleGetQuantitationTypes( ExpressionExperiment expressionExperiment ) {
        final String queryString = "select distinct quantType "
                + "from ubic.gemma.model.expression.experiment.ExpressionExperimentImpl ee "
                + "inner join ee.quantitationTypes as quantType " + "where ee  = :ee ";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "ee", expressionExperiment );
            List results = queryObject.list();
            return results;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    public Collection handleGetQuantitationTypes( ExpressionExperiment expressionExperiment, ArrayDesign arrayDesign ) {
        if ( arrayDesign == null ) {
            return handleGetQuantitationTypes( expressionExperiment );
        }

        /*
         * final String queryString = "select distinct quantType " + "from
         * ubic.gemma.model.expression.experiment.ExpressionExperimentImpl ee " + "inner join
         * ee.designElementDataVectors as vector " + "inner join vector.quantitationType as quantType " + "inner join
         * vector.bioAssayDimension bad " + "inner join bad.bioAssays ba inner join ba.arrayDesignUsed ad " + "where ee =
         * :ee and ad = :ad";
         */
        final String queryString = "select distinct quantType "
                + "from ubic.gemma.model.expression.experiment.ExpressionExperimentImpl ee "
                + "inner join ee.quantitationTypes as quantType " + "inner join ee.bioAssays as ba "
                + "inner join ba.arrayDesignUsed ad " + "where ee = :ee and ad = :ad";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "ee", expressionExperiment );
            queryObject.setParameter( "ad", arrayDesign );
            List results = queryObject.list();
            return results;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#getQuantitationTypeCountById(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public long handleGetDesignElementDataVectorCountById( long Id ) {
        long count = 0;

        final String queryString = "select count(*) from EXPRESSION_EXPERIMENT ee "
                + "inner join DESIGN_ELEMENT_DATA_VECTOR dedv on dedv.EXPRESSION_EXPERIMENT_FK=ee.ID where ee.ID = :id";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createSQLQuery( queryString );
            queryObject.setLong( "id", Id );
            queryObject.setMaxResults( 1 );
            /*
             * org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
             * queryObject.setParameter( "id", Id ); queryObject.setMaxResults( 1 );
             */
            count = ( ( BigInteger ) queryObject.uniqueResult() ).longValue();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        return count;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#getQuantitationTypeCountById(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public long handleGetBioAssayCountById( long Id ) {
        long count = 0;

        final String queryString = "select count(*) from EXPRESSION_EXPERIMENT ee "
                + "inner join BIO_ASSAY ba on ba.EXPRESSION_EXPERIMENT_FK=ee.ID where ee.ID = :id";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createSQLQuery( queryString );
            queryObject.setLong( "id", Id );
            queryObject.setMaxResults( 1 );
            /*
             * org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
             * queryObject.setParameter( "id", Id ); queryObject.setMaxResults( 1 );
             */
            count = ( ( BigInteger ) queryObject.uniqueResult() ).longValue();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        return count;
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from ExpressionExperimentImpl";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( query );

            return ( Integer ) queryObject.iterate().next();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#remove(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public void remove( ExpressionExperiment expressionExperiment ) {
        final ExpressionExperiment toDelete = expressionExperiment;
        this.getHibernateTemplate().execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( Session session ) throws HibernateException {

                log.info( "Loading data for deletion..." );
                session.update( toDelete );
                toDelete.getBioAssayDataVectors().size();
                Set<BioAssayDimension> dims = new HashSet<BioAssayDimension>();
                Collection<DesignElementDataVector> designElementDataVectors = toDelete.getDesignElementDataVectors();

                int count = 0;
                log.info( "Removing  Design Element Data Vectors." );
                for ( DesignElementDataVector dv : designElementDataVectors ) {
                    BioAssayDimension dim = dv.getBioAssayDimension();
                    dims.add( dim );
                    session.delete( dv );
                    if ( ++count % 20000 == 0 ) {
                        log.info( count + " design Element data vectors deleted" );
                        // session.flush();
                    }

                }
                toDelete.getDesignElementDataVectors().clear();
                // session.flush();
                // session.clear();

                log.info( "Removing BioAssay Dimensions." );
                for ( BioAssayDimension dim : dims ) {
                    session.delete( dim );
                }

                Collection<BioMaterial> bioMaterialsToDelete = new HashSet<BioMaterial>();
                for ( BioAssay ba : toDelete.getBioAssays() ) {
                    ba.getArrayDesignUsed().getCompositeSequences().size();

                    // delete references to files on disk
                    for ( LocalFile lf : ba.getDerivedDataFiles() ) {
                        for ( LocalFile sf : lf.getSourceFiles() ) {
                            session.delete( sf );
                        }
                        lf.getSourceFiles().clear();
                        session.delete( lf );
                    }
                    // Delete raw data files
                    if ( ba.getRawDataFile() != null ) session.delete( ba.getRawDataFile() );

                    // remove the bioassay audit trail
                    // AuditTrail at = ba.getAuditTrail();
                    // ba.setAuditTrail( null );
                    // if ( at != null ) {
                    // for ( AuditEvent event : at.getEvents() ) {
                    // session.delete( event );
                    // }
                    // at.getEvents().clear();
                    // session.delete( at );
                    // }

                    Collection<BioMaterial> biomaterials = ba.getSamplesUsed();
                    bioMaterialsToDelete.addAll( biomaterials );
                    for ( BioMaterial bm : biomaterials ) {
                        bm.getBioAssaysUsedIn().clear();
                        session.saveOrUpdate( bm );
                    }
                    biomaterials.clear();
                    session.update( ba );
                    session.delete( ba );
                    log.info( "Removed BioAssay " + ba.getName() + " and its associations." );
                }

                for ( ExpressionExperimentSubSet subset : toDelete.getSubsets() ) {
                    session.delete( subset );
                }
                toDelete.getSubsets().clear();

                for ( BioMaterial bm : bioMaterialsToDelete ) {
                    session.delete( bm );
                }

                // session.flush();

                // Delete investigators
                // for ( Contact ct : toDelete.getInvestigators() ) {
                // session.delete( ct );
                // }

                // Remove audit information for ee from the db. We might want to keep this but......
                // AuditTrail at = toDelete.getAuditTrail();
                // if ( at != null ) {
                // for ( AuditEvent event : at.getEvents() ) {
                // session.delete( event );
                // }
                // at.getEvents().clear();

                // session.delete( at );
                // }

                // session.clear();
                session.delete( toDelete );
                session.flush();
                session.clear();

                // for ( BioMaterial bm : bioMaterialsToDelete ) {
                // session.delete( bm );
                // }
                // session.flush();

                return null;
            }
        }, true );

    }

    public ExpressionExperiment expressionExperimentValueObjectToEntity(
            ExpressionExperimentValueObject expressionExperimentValueObject ) {
        return ( ExpressionExperiment ) this.load( expressionExperimentValueObject.getId() );
    }

    @Override
    public ExpressionExperimentValueObject toExpressionExperimentValueObject( final ExpressionExperiment entity ) {
        ExpressionExperimentValueObject vo = new ExpressionExperimentValueObject();

        vo.setId( entity.getId() );

        if ( entity.getAccession() != null ) {
            vo.setAccession( entity.getAccession().getAccession() );
            vo.setExternalDatabase( entity.getAccession().getExternalDatabase().getName() );
            vo.setExternalUri( entity.getAccession().getExternalDatabase().getWebUri() );
        }

        vo.setName( entity.getName() );

        vo.setSource( entity.getSource() );

        vo.setBioAssayCount( 0 );
        vo.setTaxon( "test" );
        vo.setDesignElementDataVectorCount( 0 );

        // vo.setBioAssayCount( this.handleGetBioAssayCountById( entity.getId() ) );
        // vo.setTaxon( getTaxon( entity ) );
        // vo.setDesignElementDataVectorCount( this.handleGetDesignElementDataVectorCountById( entity.getId() ) );

        return vo;
    }

    public String getTaxon( ExpressionExperiment object ) {

        final String queryString = "select sample.sourceTaxon from ExpressionExperimentImpl ee "
                + "inner join ee.bioAssays as ba inner join ba.samplesUsed as sample "
                + "inner join sample.sourceTaxon where ee.id = :id";

        Taxon taxon = ( Taxon ) queryByIdReturnObject( object.getId(), queryString );

        if ( taxon == null || StringUtils.isBlank( taxon.getScientificName() ) ) {
            return "Taxon unavailable";
        }
        return taxon.getCommonName();

        // return ((Taxon) queryByIdReturnObject(object.getId(), queryString)).getScientificName();

        /*
         * if ( object == null ) { return "Taxon unavailable"; } Collection bioAssayCol = object.getBioAssays();
         * BioAssay bioAssay = null; Taxon taxon = null; if ( bioAssayCol != null && bioAssayCol.size() > 0 ) { bioAssay = (
         * BioAssay ) bioAssayCol.iterator().next(); } else { return "Taxon unavailable"; } Collection bioMaterialCol =
         * bioAssay.getSamplesUsed(); if ( bioMaterialCol != null && bioMaterialCol.size() != 0 ) { BioMaterial
         * bioMaterial = ( BioMaterial ) bioMaterialCol.iterator().next(); taxon = bioMaterial.getSourceTaxon(); } else {
         * return "Taxon unavailable"; } if ( taxon != null ) return taxon.getScientificName(); return "Taxon
         * unavailable";
         */
    }

    private Object queryByIdReturnObject( Long id, final String queryString ) {
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setFirstResult( 1 );
            queryObject.setMaxResults( 1 ); // this should gaurantee that there is only one or no element in the
            // collection returned
            queryObject.setParameter( "id", id );
            java.util.List results = queryObject.list();

            if ( ( results == null ) || ( results.size() == 0 ) ) return null;

            return results.iterator().next();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    protected void handleThaw( final ExpressionExperiment expressionExperiment ) throws Exception {
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.update( expressionExperiment );
                expressionExperiment.getDesignElementDataVectors().size();
                expressionExperiment.getBioAssays().size();
                expressionExperiment.getSubsets().size();
                if ( expressionExperiment.getAccession() != null )
                    expressionExperiment.getAccession().getExternalDatabase();
                thawPrimaryReference( expressionExperiment, session );
                for ( BioAssay ba : expressionExperiment.getBioAssays() ) {
                    ba.getSamplesUsed().size();
                    ba.getDerivedDataFiles().size();
                }
                return null;
            }

        }, true );
    }

    private void thawPrimaryReference( final ExpressionExperiment expressionExperiment, org.hibernate.Session session ) {
        if ( expressionExperiment.getPrimaryPublication() != null ) {
            session.update( expressionExperiment.getPrimaryPublication() );
            session.update( expressionExperiment.getPrimaryPublication().getPubAccession() );
            session.update( expressionExperiment.getPrimaryPublication().getPubAccession().getExternalDatabase() );
        }
    }

    // thaw lite.
    @Override
    protected void handleThawBioAssays( final ExpressionExperiment expressionExperiment ) {
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.update( expressionExperiment );
                Hibernate.initialize( expressionExperiment );
                expressionExperiment.getBioAssays().size();
                expressionExperiment.getAuditTrail().getEvents().size();
                thawPrimaryReference( expressionExperiment, session );
                if ( expressionExperiment.getAccession() != null )
                    expressionExperiment.getAccession().getExternalDatabase();
                for ( BioAssay ba : expressionExperiment.getBioAssays() ) {
                    ba.getSamplesUsed().size();
                    ba.getDerivedDataFiles().size();
                    Hibernate.initialize( ba.getArrayDesignUsed() );
                }
                for ( QuantitationType type : expressionExperiment.getQuantitationTypes() ) {
                    session.update( type );
                }
                return null;
            }
        }, true );
    }

    @Override
    protected Taxon handleGetTaxon( Long id ) throws Exception {

        final String queryString = "select SU.sourceTaxon from ExpressionExperimentImpl as EE "
                + "inner join EE.bioAssays as BA "
                + "inner join BA.samplesUsed as SU inner join SU.sourceTaxon where EE.id = :id";

        return ( Taxon ) queryByIdReturnObject( id, queryString );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#findByAccession(ubic.gemma.model.common.description.DatabaseEntry)
     */
    @Override
    public ExpressionExperiment findByAccession( DatabaseEntry accession ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( ExpressionExperiment.class );

            BusinessKey.checkKey( accession );
            BusinessKey.attachCriteria( queryObject, accession, "accession" );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + ExpressionExperiment.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( ExpressionExperiment ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    @Override
    protected Collection handleGetSamplingOfVectors( QuantitationType quantitationType, Integer limit )
            throws Exception {
        final String queryString = "select dev from DesignElementDataVectorImpl dev "
                + "inner join dev.quantitationType as qt where qt.id = :qtid";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setMaxResults( limit );
            queryObject.setParameter( "qtid", quantitationType.getId() );
            List results = queryObject.list();
            return results;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    // FIXME, EE is not needed as a parameter.
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetDesignElementDataVectors( Collection designElements, QuantitationType quantitationType )
            throws Exception {
        if ( designElements == null || designElements.size() == 0 ) return new HashSet();

        assert quantitationType.getId() != null;

        final String queryString = "select dev from DesignElementDataVectorImpl as dev inner join dev.designElement as de "
                + " where de in (:de) and dev.quantitationType = :qt";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameterList( "de", designElements );
            queryObject.setParameter( "qt", quantitationType );
            queryObject.setCacheable( true );
            return queryObject.list();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected Map handleGetDesignElementDataVectors( Map cs2gene, QuantitationType qt )
    throws Exception {

    	Map <DesignElementDataVector, Object> dedv2genes = new HashMap<DesignElementDataVector, Object>();

//  	String queryString = "SELECT * FROM DESIGN_ELEMENT_DATA_VECTOR WHERE " + 
//  	" DESIGN_ELEMENT_FK in (" +
//  	StringUtils.join( designElements.iterator(), "," ) + ") + AND QUANTITATION_TYPE_FK = " + quantitationType.getId(); 

    	String queryString = "SELECT ID as dedvId, DATA as dedvData, DESIGN_ELEMENT_FK as csId, RANK as dedvRank FROM DESIGN_ELEMENT_DATA_VECTOR WHERE " + 
    	" QUANTITATION_TYPE_FK = " + qt.getId(); 
    	Session session = getSessionFactory().openSession();
    	org.hibernate.SQLQuery queryObject = session.createSQLQuery( queryString );
    	
    	queryObject.addScalar( "dedvId", new LongType() );
    	queryObject.addScalar( "dedvData", new BlobType() );
    	queryObject.addScalar( "csId", new LongType() );
    	queryObject.addScalar( "dedvRank", new DoubleType() );

    	ScrollableResults scroll = queryObject.scroll( ScrollMode.FORWARD_ONLY );
    	Collection<Long> csIds = cs2gene.keySet();
    	while ( scroll.next() ) {
    		Long dedvId = scroll.getLong(0);
    		Blob dedvData = scroll.getBlob(1);
    		byte data[] = dedvData.getBytes((long)1, (int)dedvData.length());
    		Long csId = scroll.getLong( 2 );
    		Double rank = scroll.getDouble( 3 );

    		if(csIds.contains(csId)){
    			DesignElementDataVector vector = DesignElementDataVector.Factory.newInstance();
    			vector.setId(dedvId);
    			vector.setData( data );
    			//vector.setDesignElement( cs );
    			vector.setQuantitationType( qt );
    			vector.setRank(rank);
    			//vector.setExpressionExperiment( expressionExperiment );
    			//vector.setBioAssayDimension( bioAssayDimension );
    			dedv2genes.put(vector, cs2gene.get(csId) );
    		}
    		
    	}
    	session.clear();
    	session.close();
    	return dedv2genes;
    }


    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleGetPerTaxonCount()
     */
    @Override
    protected Map handleGetPerTaxonCount() throws Exception {
        final String queryString = "select SU.sourceTaxon, count(distinct EE.id) from ExpressionExperimentImpl as EE "
                + "inner join EE.bioAssays as BA inner join BA.samplesUsed as SU "
                + "inner join SU.sourceTaxon group by SU.sourceTaxon.scientificName";
        Map<Taxon, Long> taxonCount = new HashMap<Taxon, Long>();
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            ScrollableResults list = queryObject.scroll();
            while ( list.next() ) {
                taxonCount.put( ( Taxon ) list.get( 0 ), new Long( list.getInteger( 1 ) ) );
            }
            return taxonCount;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleLoadAllValueObjects()
     */
    @Override
    protected Collection handleLoadAllValueObjects() throws Exception {
        Collection<ExpressionExperimentValueObject> vo = new ArrayList<ExpressionExperimentValueObject>();
        final String queryString = "select ee.id as id, "
                + "ee.name as name, "
                + "ED.name as externalDatabaseName, "
                + "ED.webUri as externalDatabaseUri, "
                + "ee.source as source, "
                + "ee.accession.accession as accession, "
                + "taxon.commonName as taxonCommonName,"
                + "count(distinct BA) as bioAssayCount, "
                + "count(distinct AD) as arrayDesignCount, "
                + "ee.shortName as shortName, "
                + "eventCreated.date as createdDate "
                +
                // removed to speed up query
                // "count(distinct dedv) as dedvCount, " +
                // "count(distinct SU) as bioMaterialCount " +
                " from ExpressionExperimentImpl as ee inner join ee.bioAssays as BA inner join ee.auditTrail.events as eventCreated "
                + "inner join BA.samplesUsed as SU inner join BA.arrayDesignUsed as AD "
                + "inner join SU.sourceTaxon as taxon left join ee.accession.externalDatabase as ED "
                + "WHERE eventCreated.action='C'" + " group by ee order by ee.name";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            ScrollableResults list = queryObject.scroll( ScrollMode.FORWARD_ONLY );
            while ( list.next() ) {
                ExpressionExperimentValueObject v = new ExpressionExperimentValueObject();
                v.setId( list.getLong( 0 ) );
                v.setName( list.getString( 1 ) );
                v.setExternalDatabase( list.getString( 2 ) );
                v.setExternalUri( list.getString( 3 ) );
                v.setSource( list.getString( 4 ) );
                v.setAccession( list.getString( 5 ) );
                v.setTaxon( list.getString( 6 ) );
                v.setBioAssayCount( list.getInteger( 7 ) );
                v.setArrayDesignCount( list.getInteger( 8 ) );
                v.setShortName( list.getString( 9 ) );
                v.setDateCreated( list.getDate( 10 ).toString() );
                // removed to speed up query
                // v.setDesignElementDataVectorCount( list.getInteger( 10 ) );
                // v.setBioMaterialCount( list.getInteger( 11 ) );
                vo.add( v );
            }
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return vo;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleLoadValueObjects(java.util.Collection)
     */
    @Override
    protected Collection handleLoadValueObjects( Collection ids ) throws Exception {
        Collection<ExpressionExperimentValueObject> vo = new ArrayList<ExpressionExperimentValueObject>();
        // sanity check
        if ( ids == null || ids.size() == 0 ) {
            return vo;
        }
        final String queryString = "select ee.id as id, "
                + "ee.name as name, "
                + "ED.name as externalDatabaseName, "
                + "ED.webUri as externalDatabaseUri, "
                + "ee.source as source, "
                + "ee.accession.accession as accession, "
                + "taxon.commonName as taxonCommonName,"
                + "count(distinct BA) as bioAssayCount, "
                + "count(distinct AD) as arrayDesignCount, "
                + "ee.shortName as shortName, "
                + "eventCreated.date as createdDate "
                + " from ExpressionExperimentImpl as ee inner join ee.bioAssays as BA inner join ee.auditTrail.events as eventCreated "
                + "inner join BA.samplesUsed as SU inner join BA.arrayDesignUsed as AD "
                + "inner join SU.sourceTaxon as taxon left join ee.accession.externalDatabase as ED "
                + " where eventCreated.action='C' and ee.id in (:ids) " + " group by ee order by ee.name";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameterList( "ids", ids );
            queryObject.setCacheable( true );
            List list = queryObject.list();
            for ( Object object : list ) {

                Object[] res = ( Object[] ) object;
                ExpressionExperimentValueObject v = new ExpressionExperimentValueObject();
                v.setId( ( Long ) res[0] );
                v.setName( ( String ) res[1] );
                v.setExternalDatabase( ( String ) res[2] );
                v.setExternalUri( ( String ) res[3] );
                v.setSource( ( String ) res[4] );
                v.setAccession( ( String ) res[5] );
                v.setTaxon( ( String ) res[6] );
                v.setBioAssayCount( ( Integer ) res[7] );
                v.setArrayDesignCount( ( Integer ) res[8] );
                v.setShortName( ( String ) res[9] );
                v.setDateCreated( ( ( Date ) res[10] ).toString() );
                vo.add( v );
            }
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return vo;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleFindByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleFindByTaxon( Taxon taxon ) throws Exception {

        Collection<ExpressionExperiment> ee = null;
        final String queryString = "select distinct ee from ExpressionExperimentImpl as ee "
                + "inner join ee.bioAssays as ba "
                + "inner join ba.samplesUsed as sample where sample.sourceTaxon = :taxon ";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "taxon", taxon );
            ee = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return ee;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleGetBioMaterialCount(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected long handleGetBioMaterialCount( ExpressionExperiment expressionExperiment ) throws Exception {

        long count = 0;
        final String queryString = "select count(distinct sample) from ExpressionExperimentImpl as ee "
                + "inner join ee.bioAssays as ba " + "inner join ba.samplesUsed as sample where ee.id = :eeId ";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setLong( "eeId", expressionExperiment.getId() );

            queryObject.setMaxResults( 1 );

            count = ( ( Integer ) queryObject.uniqueResult() ).longValue();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return count;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleGetPreferredDesignElementDataVectorCount(ubic.gemma.model.expression.experiment.ExpressionExperiment)
     */
    @Override
    protected long handleGetPreferredDesignElementDataVectorCount( ExpressionExperiment expressionExperiment )
            throws Exception {
        long count = 0;
        final String queryString = "select count(distinct dedv) from ExpressionExperimentImpl as ee "
                + "inner join ee.designElementDataVectors as dedv "
                + "inner join dedv.quantitationType as qType where qType.isPreferred = true and ee.id = :eeId ";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setLong( "eeId", expressionExperiment.getId() );

            queryObject.setMaxResults( 1 );

            count = ( ( Integer ) queryObject.uniqueResult() ).longValue();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return count;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleLoad(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleLoad( Collection ids ) throws Exception {
        Collection<ExpressionExperiment> ee = null;
        final String queryString = "select ee from ExpressionExperimentImpl as ee " + " where ee.id in (:ids) ";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameterList( "ids", ids );

            ee = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return ee;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleGetDesignElementDataVectors(ubic.gemma.model.expression.experiment.ExpressionExperiment,
     *      java.util.Collection)
     */
    @Override
    protected Collection handleGetDesignElementDataVectors( ExpressionExperiment expressionExperiment,
            Collection quantitationTypes ) throws Exception {
        // FIXME: the experiment is no longer necessary,as each QT is soley owned by one EE.
        final String queryString = "select distinct dev from DesignElementDataVectorImpl dev inner join fetch dev.bioAssayDimension inner join fetch dev.designElement  where dev.quantitationType in (:qts) ";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameterList( "qts", quantitationTypes );
            List results = queryObject.list();
            return results;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }
    
     /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleGetLastLinkAnalysis(java.util.Collection)
     */
    @Override
    protected Map handleGetAuditEvents( Collection ids ) throws Exception {
        final String queryString = "select ee.id, auditEvent from ExpressionExperimentImpl ee inner join ee.auditTrail as auditTrail inner join auditTrail.events as auditEvent "
                + " where ee.id in (:ids) ";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameterList( "ids", ids );
            ScrollableResults list = queryObject.scroll();
            Map<Long, Collection<AuditEvent>> eventMap = new HashMap<Long, Collection<AuditEvent>>();
            // process list of expression experiment ids that have events
            while ( list.next() ) {
                Long id = list.getLong( 0 );
                AuditEvent event = ( AuditEvent ) list.get( 1 );

                if ( eventMap.containsKey( id ) ) {
                    Collection<AuditEvent> events = eventMap.get( id );
                    events.add( event );
                } else {
                    Collection<AuditEvent> events = new ArrayList<AuditEvent>();
                    events.add( event );
                    eventMap.put( id, events );
                }
            }
            // add in expression experiment ids that do not have events. Set their values to null.
            for ( Object object : ids ) {
                Long id = ( Long ) object;
                if ( !eventMap.containsKey( id ) ) {
                    eventMap.put( id, null );
                }
            }
            return eventMap;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleFindByExpressedGene(ubic.gemma.model.genome.Gene,
     *      java.lang.Double)
     */
    @Override
    protected Collection handleFindByExpressedGene( Gene gene, Double rank ) throws Exception {

        final String queryString = "select distinct ee.ID as eeID FROM "
                + "GENE2CS g2s, COMPOSITE_SEQUENCE cs, DESIGN_ELEMENT_DATA_VECTOR dedv, EXPRESSION_EXPERIMENT ee "
                + "WHERE g2s.CS = cs.ID AND cs.ID = dedv.DESIGN_ELEMENT_FK AND dedv.EXPRESSION_EXPERIMENT_FK = ee.ID AND g2s.gene = :geneID AND dedv.RANK >= :rank";

        Collection<Long> eeIds = null;

        try {
            org.hibernate.SQLQuery queryObject = super.getSession( false ).createSQLQuery( queryString );
            queryObject.setLong( "geneID", gene.getId() );
            queryObject.setDouble( "rank", rank );
            queryObject.addScalar( "eeID", new LongType() );
            ScrollableResults results = queryObject.scroll();

            eeIds = new HashSet<Long>();

            // Post Processing
            while ( results.next() )
                eeIds.add( results.getLong( 0 ) );

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        return eeIds;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleFindByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection handleFindByGene( Gene gene ) throws Exception {

        final String queryString = "select distinct ee.ID as eeID FROM "
                + "GENE2CS g2s, COMPOSITE_SEQUENCE cs, ARRAY_DESIGN ad, BIO_ASSAY ba, EXPRESSION_EXPERIMENT ee "
                + "WHERE g2s.CS = cs.ID AND ad.ID = cs.ARRAY_DESIGN_FK AND ba.ARRAY_DESIGN_USED_FK = ad.ID AND ba.EXPRESSION_EXPERIMENT_FK = ee.ID and g2s.gene = :geneID";

        Collection<Long> eeIds = null;

        try {
            org.hibernate.SQLQuery queryObject = super.getSession( false ).createSQLQuery( queryString );
            queryObject.setLong( "geneID", gene.getId() );
            queryObject.addScalar( "eeID", new LongType() );
            ScrollableResults results = queryObject.scroll();

            eeIds = new HashSet<Long>();

            // Post Processing
            while ( results.next() )
                eeIds.add( results.getLong( 0 ) );

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        return eeIds;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.experiment.ExpressionExperimentDaoBase#handleFindByBibliographicReference(java.lang.Long)
     */
    @Override
    protected Collection handleFindByBibliographicReference( Long bibRefID ) throws Exception {
        final String queryString = "select distinct ee FROM ExpressionExperimentImpl as ee left join ee.otherRelevantPublications as eeO"
                + " WHERE ee.primaryPublication.id = :bibID OR (eeO.id = :bibID) ";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "bibID", bibRefID );

            Collection results = queryObject.list();
            return results;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

}