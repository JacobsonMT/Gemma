/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.persistence.service.expression.bioAssayData;

import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.*;
import org.hibernate.type.LongType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.util.BatchIterator;
import ubic.gemma.core.analysis.preprocess.normalize.QuantileNormalizer;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeImpl;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.util.CommonQueries;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.*;

/**
 * @author Paul
 */
@Repository
public class ProcessedExpressionDataVectorDaoImpl extends DesignElementDataVectorDaoImpl<ProcessedExpressionDataVector>
        implements ProcessedExpressionDataVectorDao {

    /**
     * Don't attempt to renormalize data that is smaller than this. This avoids unnecessary normalization in tests, and
     * in data sets where normalization is more likely to harm than good.
     */
    private static final int MIN_SIZE_FOR_RENORMALIZATION = 4000;

    private final ProcessedDataVectorCache processedDataVectorCache;

    @Autowired
    public ProcessedExpressionDataVectorDaoImpl( SessionFactory sessionFactory,
            ProcessedDataVectorCache processedDataVectorCache ) {
        super( ProcessedExpressionDataVector.class, sessionFactory );
        this.processedDataVectorCache = processedDataVectorCache;
    }

    @Override
    public void clearCache() {
        processedDataVectorCache.clearCache();
    }

    @Override
    public ExpressionExperiment createProcessedDataVectors( ExpressionExperiment ee ) {
        if ( ee == null ) {
            throw new IllegalStateException( "ExpressionExperiment cannot be null" );
        }

        ExpressionExperiment expressionExperiment = ( ExpressionExperiment ) this.getSessionFactory()
                .getCurrentSession().get( ExpressionExperiment.class, ee.getId() );

        assert expressionExperiment != null;

        removeProcessedDataVectors( expressionExperiment );

        Hibernate.initialize( expressionExperiment );
        Hibernate.initialize( expressionExperiment.getQuantitationTypes() );
        Hibernate.initialize( expressionExperiment.getProcessedExpressionDataVectors() );

        expressionExperiment.getProcessedExpressionDataVectors().clear();

        log.info( "Computing processed expression vectors for " + expressionExperiment );

        boolean isTwoChannel = isTwoChannel( expressionExperiment );

        Collection<RawExpressionDataVector> missingValueVectors = new HashSet<>();
        if ( isTwoChannel ) {
            missingValueVectors = this.getMissingValueVectors( expressionExperiment );
        }

        Collection<RawExpressionDataVector> preferredDataVectors = this.getPreferredDataVectors( expressionExperiment );
        if ( preferredDataVectors.isEmpty() ) {
            throw new IllegalArgumentException( "No preferred data vectors for " + expressionExperiment );
        }

        Map<CompositeSequence, DoubleVectorValueObject> maskedVectorObjects = maskAndUnpack( preferredDataVectors,
                missingValueVectors );

        /*
         * Create the vectors. Do a sanity check that we don't have more than we should
         */
        Collection<CompositeSequence> seenDes = new HashSet<>();
        RawExpressionDataVector preferredDataVectorExemplar = preferredDataVectors.iterator().next();
        QuantitationType preferredMaskedDataQuantitationType = getPreferredMaskedDataQuantitationType(
                preferredDataVectorExemplar.getQuantitationType() );

        if ( !preferredMaskedDataQuantitationType.getType().equals( StandardQuantitationType.COUNT )
                && !preferredMaskedDataQuantitationType.getIsRatio()
                /* && !preferredMaskedDataQuantitationType.getIsNormalized() */
                && maskedVectorObjects.size() > MIN_SIZE_FOR_RENORMALIZATION ) {
            log.info( "Normalizing the data" );
            renormalize( maskedVectorObjects );
        } else {
            log.info( "Normalization skipped for this data set (not suitable, or already normalized)" );
        }

        int i = 0;

        for ( CompositeSequence cs : maskedVectorObjects.keySet() ) {

            DoubleVectorValueObject dvvo = maskedVectorObjects.get( cs );

            if ( seenDes.contains( cs ) ) {
                // defensive programming, this happens.
                throw new IllegalStateException( "Duplicated design element: " + cs
                        + "; make sure the experiment has only one 'preferred' quantitation type. "
                        + "Perhaps you need to run vector merging following an array desing switch?" );
            }

            ProcessedExpressionDataVector vec = ( ProcessedExpressionDataVector ) dvvo
                    .toDesignElementDataVector( ee, cs, preferredMaskedDataQuantitationType );

            expressionExperiment.getProcessedExpressionDataVectors().add( vec );
            seenDes.add( cs );
            if ( ++i % 5000 == 0 ) {
                log.info( i + " vectors built" );
            }
        }

        log.info( "Persisting " + expressionExperiment.getProcessedExpressionDataVectors().size()
                + " processed data vectors" );

        expressionExperiment.getQuantitationTypes().add( preferredMaskedDataQuantitationType );
        expressionExperiment.setNumberOfDataVectors( expressionExperiment.getProcessedExpressionDataVectors().size() );

        this.getSessionFactory().getCurrentSession().update( expressionExperiment );
        assert expressionExperiment.getNumberOfDataVectors() != null;

        this.processedDataVectorCache.clearCache( expressionExperiment.getId() );

        return expressionExperiment;

    }

    @Override
    public ExpressionExperiment createProcessedDataVectors( ExpressionExperiment ee,
            Collection<ProcessedExpressionDataVector> data ) {
        if ( ee == null ) {
            throw new IllegalStateException( "ExpressionExperiment cannot be null" );
        }

        ExpressionExperiment expressionExperiment = ( ExpressionExperiment ) this.getSessionFactory().getCurrentSession()
                .get( ExpressionExperiment.class, ee.getId() );

        assert expressionExperiment != null;

        removeProcessedDataVectors( expressionExperiment );

        Hibernate.initialize( expressionExperiment );
        Hibernate.initialize( expressionExperiment.getQuantitationTypes() );

        expressionExperiment.setProcessedExpressionDataVectors( data );

        QuantitationType qt = data.iterator().next().getQuantitationType();
        // assumes all are same.

        this.getSessionFactory().getCurrentSession().saveOrUpdate( qt );
        expressionExperiment.getQuantitationTypes().add( data.iterator().next().getQuantitationType() );
        expressionExperiment.setNumberOfDataVectors( expressionExperiment.getProcessedExpressionDataVectors().size() );

        this.getSessionFactory().getCurrentSession().update( expressionExperiment );
        assert expressionExperiment.getNumberOfDataVectors() != null;

        this.processedDataVectorCache.clearCache( expressionExperiment.getId() );

        return expressionExperiment;

    }

    @Override
    public Collection<? extends DesignElementDataVector> find( ArrayDesign arrayDesign,
            QuantitationType quantitationType ) {
        final String queryString =
                "select dev from ProcessedExpressionDataVectorImpl dev  inner join fetch dev.bioAssayDimension bd "
                        + " inner join fetch dev.designElement de inner join fetch dev.quantitationType inner join de.arrayDesign ad where ad.id = :adid "
                        + "and dev.quantitationType = :quantitationType ";
        org.hibernate.Query queryObject = super.getSessionFactory().getCurrentSession().createQuery( queryString );
        queryObject.setParameter( "quantitationType", quantitationType );
        queryObject.setParameter( "adid", arrayDesign.getId() );

        //noinspection unchecked
        return queryObject.list();
    }

    @Override
    public Collection<ProcessedExpressionDataVector> find( Collection<QuantitationType> quantitationTypes ) {
        final String queryString = "select dev from ProcessedExpressionDataVectorImpl dev where  "
                + "  dev.quantitationType in ( :quantitationTypes) ";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "quantitationTypes", quantitationTypes ).list();

    }

    @Override
    public Collection<ProcessedExpressionDataVector> find( QuantitationType quantitationType ) {
        final String queryString = "select dev from ProcessedExpressionDataVectorImpl dev  where  "
                + "  dev.quantitationType = :quantitationType ";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "quantitationType", quantitationType ).list();
    }

    @Override
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet expressionExperiment ) {
        return getProcessedDataArrays( expressionExperiment, -1 );
    }

    @Override
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet expressionExperiment,
            Collection<Long> genes ) {
        Collection<BioAssaySet> expressionExperiments = new HashSet<>();
        expressionExperiments.add( expressionExperiment );
        return this.handleGetProcessedExpressionDataArrays( expressionExperiments, genes );
    }

    @Override
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet ee, int limit ) {

        Collection<ProcessedExpressionDataVector> pedvs = this.getProcessedVectors( getExperiment( ee ), limit );

        if ( pedvs.isEmpty() ) {
            log.warn( "No processed vectors for experiment " + ee );
            return new HashSet<>();
        }

        Collection<Long> probes = new ArrayList<>();
        for ( ProcessedExpressionDataVector pedv : pedvs ) {
            probes.add( pedv.getDesignElement().getId() );
        }

        if ( probes.isEmpty() ) {
            return unpack( pedvs ).values();
        }

        Map<Long, Collection<Long>> cs2gene = CommonQueries
                .getCs2GeneMapForProbes( probes, this.getSessionFactory().getCurrentSession() );

        Collection<BioAssayDimension> bioAssayDimensions = this.getBioAssayDimensions( ee );

        if ( bioAssayDimensions.size() == 1 ) {
            return unpack( pedvs, cs2gene ).values();
        }

        /*
         * deal with 'misalignment problem'
         */

        BioAssayDimension longestBad = checkRagged( bioAssayDimensions );

        if ( longestBad != null ) {
            return unpack( pedvs, cs2gene, longestBad );
        }
        return unpack( pedvs, cs2gene ).values();
    }

    @Override
    public Collection<DoubleVectorValueObject> getProcessedDataArrays(
            Collection<? extends BioAssaySet> expressionExperiments, Collection<Long> genes ) {
        return this.handleGetProcessedExpressionDataArrays( expressionExperiments, genes );
    }

    @Override
    public Collection<DoubleVectorValueObject> getProcessedDataArraysByProbe( Collection<? extends BioAssaySet> ees,
            Collection<CompositeSequence> probes ) {

        if ( probes.isEmpty() )
            return new HashSet<>();

        Collection<Long> probeIds = EntityUtils.getIds( probes );

        return getProcessedDataArraysByProbeIds( ees, probeIds );

    }

    @Override
    public Collection<DoubleVectorValueObject> getProcessedDataArraysByProbeIds( BioAssaySet ee,
            Collection<Long> probes ) {
        return this.getProcessedDataArraysByProbeIds( Collections.singleton( ee ), probes );
    }

    @Override
    public Collection<ProcessedExpressionDataVector> getProcessedVectors( ExpressionExperiment ee ) {
        final String queryString = " from ProcessedExpressionDataVectorImpl dedv where dedv.expressionExperiment.id = :ee";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ee", ee.getId() )
                .list();
    }

    /**
     * @param limit if non-null and positive, you will get a random set of vectors for the experiment
     */
    @Override
    public Collection<ProcessedExpressionDataVector> getProcessedVectors( ExpressionExperiment ee, Integer limit ) {

        if ( limit == null || limit < 0 ) {
            return this.getProcessedVectors( ee );
        }

        StopWatch timer = new StopWatch();
        timer.start();
        List<ProcessedExpressionDataVector> result;

        Integer numvecsavailable = ee.getNumberOfDataVectors();
        if ( numvecsavailable == null || numvecsavailable == 0 ) {
            log.info( "Experiment does not have vector count populated." );
            // cannot fix this here, because we're read-only.
        }

        Query q = this.getSessionFactory().getCurrentSession()
                .createQuery( " from ProcessedExpressionDataVectorImpl dedv where dedv.expressionExperiment.id = :ee" );
        q.setParameter( "ee", ee.getId(), LongType.INSTANCE );
        q.setMaxResults( limit );
        if ( numvecsavailable != null && numvecsavailable > limit ) {
            q.setFirstResult( new Random().nextInt( numvecsavailable - limit ) );
        }

        // we should already be read-only, so this is probably pointless.
        q.setReadOnly( true );

        // and so this probably doesn't do anything useful.
        q.setFlushMode( FlushMode.MANUAL );

        //noinspection unchecked
        result = q.list();
        if ( timer.getTime() > 1000 )
            log.info( "Fetch " + limit + " vectors from " + ee.getShortName() + ": " + timer.getTime() + "ms" );

        if ( result.isEmpty() ) {
            log.warn( "Experiment does not have any processed data vectors" );
            return result;
        }

        this.thaw( result ); // needed?
        return result;
    }

    @Override
    public Map<ExpressionExperiment, Map<Gene, Collection<Double>>> getRanks(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes, RankMethod method ) {

        Collection<ArrayDesign> arrayDesigns = CommonQueries
                .getArrayDesignsUsed( EntityUtils.getIds( expressionExperiments ),
                        this.getSessionFactory().getCurrentSession() ).keySet();

        // this could be further improved by getting probes specific to experiments in batches.
        Map<CompositeSequence, Collection<Gene>> cs2gene = CommonQueries
                .getCs2GeneMap( genes, arrayDesigns, this.getSessionFactory().getCurrentSession() );

        if ( cs2gene.isEmpty() ) {
            log.warn( "No composite sequences found for genes" );
            return new HashMap<>();
        }
        Map<ExpressionExperiment, Map<Gene, Collection<Double>>> result = new HashMap<>();

        BatchIterator<CompositeSequence> batchIterator = new BatchIterator<>( cs2gene.keySet(), 500 );

        for ( Collection<CompositeSequence> batch : batchIterator ) {

            final String queryString =
                    "select distinct dedv.expressionExperiment, dedv.designElement, dedv.rankByMean, "
                            + "dedv.rankByMax from ProcessedExpressionDataVectorImpl dedv "
                            + " where dedv.designElement in ( :cs ) and dedv.expressionExperiment in (:ees) ";

            List qr = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                    .setParameter( "cs", batch ).setParameterList( "ees", expressionExperiments ).list();

            for ( Object o : qr ) {
                Object[] oa = ( Object[] ) o;
                ExpressionExperiment e = ( ExpressionExperiment ) oa[0];
                CompositeSequence d = ( CompositeSequence ) oa[1];
                Double rMean = oa[2] == null ? Double.NaN : ( Double ) oa[2];
                Double rMax = oa[3] == null ? Double.NaN : ( Double ) oa[3];

                if ( !result.containsKey( e ) ) {
                    result.put( e, new HashMap<Gene, Collection<Double>>() );
                }

                Map<Gene, Collection<Double>> rmap = result.get( e );

                Collection<Gene> genes4probe = cs2gene.get( d );

                addToGene( method, rmap, rMean, rMax, genes4probe );
            }
        }
        return result;
    }

    @Override
    public Map<Gene, Collection<Double>> getRanks( ExpressionExperiment expressionExperiment, Collection<Gene> genes,
            RankMethod method ) {
        Map<CompositeSequence, Collection<Gene>> cs2gene = CommonQueries
                .getCs2GeneMap( genes, this.getSessionFactory().getCurrentSession() );
        if ( cs2gene.keySet().size() == 0 ) {
            log.warn( "No composite sequences found for genes" );
            return new HashMap<>();
        }

        final String queryString =
                "select distinct dedv.designElement, dedv.rankByMean, dedv.rankByMax from ProcessedExpressionDataVectorImpl dedv "
                        + " where dedv.designElement in ( :cs ) and dedv.expressionExperiment.id = :eeid ";

        List qr = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "cs", cs2gene.keySet() ).setParameter( "eeid", expressionExperiment.getId() ).list();

        Map<Gene, Collection<Double>> result = new HashMap<>();
        for ( Object o : qr ) {
            Object[] oa = ( Object[] ) o;
            CompositeSequence d = ( CompositeSequence ) oa[0];
            Double rMean = oa[1] == null ? Double.NaN : ( Double ) oa[1];
            Double rMax = oa[2] == null ? Double.NaN : ( Double ) oa[2];

            Collection<Gene> genes4probe = cs2gene.get( d );

            addToGene( method, result, rMean, rMax, genes4probe );
        }
        return result;

    }

    @Override
    public Map<CompositeSequence, Double> getRanks( ExpressionExperiment expressionExperiment, RankMethod method ) {
        final String queryString =
                "select dedv.designElement, dedv.rankByMean, dedv.rankByMax from ProcessedExpressionDataVectorImpl dedv "
                        + "where dedv.expressionExperiment.id = :ee";
        List qr = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameter( "ee", expressionExperiment.getId() ).list();
        Map<CompositeSequence, Double> result = new HashMap<>();
        for ( Object o : qr ) {
            Object[] oa = ( Object[] ) o;
            CompositeSequence d = ( CompositeSequence ) oa[0];
            Double rMean = oa[1] == null ? Double.NaN : ( Double ) oa[1];
            Double rMax = oa[2] == null ? Double.NaN : ( Double ) oa[2];
            switch ( method ) {
                case mean:
                    result.put( d, rMean );
                    break;
                case max:
                    result.put( d, rMax );
                    break;
                default:
                    break;
            }
        }
        return result;

    }

    @Override
    public Map<ExpressionExperiment, Map<Gene, Map<CompositeSequence, Double[]>>> getRanksByProbe(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes ) {
        Map<CompositeSequence, Collection<Gene>> cs2gene = CommonQueries
                .getCs2GeneMap( genes, this.getSessionFactory().getCurrentSession() );

        if ( cs2gene.keySet().size() == 0 ) {
            log.warn( "No composite sequences found for genes" );
            return new HashMap<>();
        }

        final String queryString =
                "select distinct dedv.expressionExperiment, dedv.designElement, dedv.rankByMean, dedv.rankByMax "
                        + "from ProcessedExpressionDataVectorImpl dedv " + " inner join dedv.designElement de  "
                        + " where dedv.designElement.id in ( :cs ) and dedv.expressionExperiment.id in (:ees) ";

        List qr = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "cs", EntityUtils.getIds( cs2gene.keySet() ) )
                .setParameterList( "ees", EntityUtils.getIds( expressionExperiments ) ).list();

        Map<ExpressionExperiment, Map<Gene, Map<CompositeSequence, Double[]>>> resultnew = new HashMap<>();
        for ( Object o : qr ) {
            Object[] oa = ( Object[] ) o;
            ExpressionExperiment e = ( ExpressionExperiment ) oa[0];
            CompositeSequence d = ( CompositeSequence ) oa[1];
            Double rMean = ( Double ) oa[2];
            Double rMax = ( Double ) oa[3];

            if ( !resultnew.containsKey( e ) ) {
                resultnew.put( e, new HashMap<Gene, Map<CompositeSequence, Double[]>>() );
            }

            Map<Gene, Map<CompositeSequence, Double[]>> rmapnew = resultnew.get( e );

            Collection<Gene> genes4probe = cs2gene.get( d );

            for ( Gene gene : genes4probe ) {
                if ( !rmapnew.containsKey( gene ) ) {
                    rmapnew.put( gene, new HashMap<CompositeSequence, Double[]>() );
                }

                // return BOTH mean and max

                if ( rMean == null || rMax == null ) {
                    continue;
                }
                Double[] MeanMax = new Double[] { rMean, rMax };
                rmapnew.get( gene ).put( d, MeanMax );
            }
        }
        return resultnew;
    }

    @Override
    public void removeProcessedDataVectors( final ExpressionExperiment expressionExperiment ) {
        assert expressionExperiment != null;

        /*
         * Get quantitation types that will be removed.
         */
        //noinspection unchecked
        List<QuantitationType> qtsToRemove = this.getSessionFactory().getCurrentSession().createQuery(
                "select distinct p.quantitationType from ExpressionExperiment e "
                        + "inner join e.processedExpressionDataVectors p where e.id = :id" )
                .setParameter( "id", expressionExperiment.getId() ).list();

        this.getSessionFactory().getCurrentSession()
                .createQuery( "delete from ProcessedExpressionDataVectorImpl p where p.expressionExperiment = :ee" )
                .setParameter( "ee", expressionExperiment ).executeUpdate();

        if ( !qtsToRemove.isEmpty() ) {
            log.info( "Deleting " + qtsToRemove + " old quantitation types" );
            expressionExperiment.getQuantitationTypes().removeAll( qtsToRemove );
            this.getSessionFactory().getCurrentSession().update( expressionExperiment );
            this.getSessionFactory().getCurrentSession()
                    .createQuery( "delete from QuantitationTypeImpl where id in :ids" )
                    .setParameterList( "ids", qtsToRemove );
        }
    }

    private void addToGene( RankMethod method, Map<Gene, Collection<Double>> result, Double rMean, Double rMax,
            Collection<Gene> genes4probe ) {
        for ( Gene gene : genes4probe ) {
            if ( !result.containsKey( gene ) ) {
                result.put( gene, new ArrayList<Double>() );
            }
            switch ( method ) {
                case mean:
                    result.get( gene ).add( rMean );
                    break;
                case max:
                    result.get( gene ).add( rMax );
                    break;
                default:
                    break;
            }
        }
    }

    private Collection<RawExpressionDataVector> getMissingValueVectors( ExpressionExperiment ee ) {
        final String queryString = "select dedv from RawExpressionDataVectorImpl dedv "
                + "inner join dedv.quantitationType q where q.type = 'PRESENTABSENT'"
                + " and dedv.expressionExperiment  = :ee ";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ee", ee ).list();
    }

    private Collection<RawExpressionDataVector> getPreferredDataVectors( ExpressionExperiment ee ) {
        final String queryString =
                "select dedv from RawExpressionDataVectorImpl dedv inner join dedv.quantitationType q "
                        + " where q.isPreferred = true  and dedv.expressionExperiment.id = :ee";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameter( "ee", ee.getId() )
                .list();
    }

    /**
     * Always provide full vectors, not subsets.
     */
    private void cacheResults( Collection<DoubleVectorValueObject> newResults ) {
        /*
         * Break up by gene and EE to cache collections of vectors for EE-gene combos.
         */
        Map<Long, Map<Long, Collection<DoubleVectorValueObject>>> mapForCache = makeCacheMap( newResults );
        int i = 0;
        for ( Long eeid : mapForCache.keySet() ) {
            for ( Long g : mapForCache.get( eeid ).keySet() ) {
                i++;
                this.processedDataVectorCache.addToCache( eeid, g, mapForCache.get( eeid ).get( g ) );
            }
        }
        // WARNING cache size() can be slow, esp. terracotta.
        log.info( "Cached " + i + ", input " + newResults.size() + "; total cached: "
        /* + this.processedDataVectorCache.size() */ );
    }

    /**
     * We cache vectors at the experiment level. If we need subsets, we have to slice them out.
     *
     * @param bioAssaySets  that we exactly need the data for.
     * @param genes         that might have cached results
     * @param results       from the cache will be put here
     * @param needToSearch  experiments that need to be searched (not fully cached); this will be populated
     * @param genesToSearch that still need to be searched (not in cache)
     */
    private void checkCache( Collection<? extends BioAssaySet> bioAssaySets, Collection<Long> genes,
            Collection<DoubleVectorValueObject> results, Collection<ExpressionExperiment> needToSearch,
            Collection<Long> genesToSearch ) {

        for ( BioAssaySet ee : bioAssaySets ) {

            ExpressionExperiment experiment = null;
            boolean needSubSet = false;
            if ( ee instanceof ExpressionExperiment ) {
                experiment = ( ExpressionExperiment ) ee;
            } else if ( ee instanceof ExpressionExperimentSubSet ) {
                experiment = ( ( ExpressionExperimentSubSet ) ee ).getSourceExperiment();
                needSubSet = true;
            }

            assert experiment != null;

            for ( Long g : genes ) {
                Collection<DoubleVectorValueObject> obs = processedDataVectorCache.get( ee, g );
                if ( obs != null ) {
                    if ( needSubSet ) {
                        obs = sliceSubSet( ( ExpressionExperimentSubSet ) ee, obs );
                    }
                    results.addAll( obs );
                } else {
                    genesToSearch.add( g );
                }
            }
            /*
             * This experiment is not fully cached for the genes in question.
             */
            if ( genesToSearch.size() > 0 ) {
                needToSearch.add( experiment );
            }
        }
    }

    /**
     * See if anything is 'ragged' (fewer bioassays per biomaterial than in some other sample)
     */
    private BioAssayDimension checkRagged( Collection<BioAssayDimension> bioAssayDimensions ) {
        int s = -1;
        int longest = -1;
        BioAssayDimension longestBad = null;
        for ( BioAssayDimension bad : bioAssayDimensions ) {
            Collection<BioAssay> assays = bad.getBioAssays();
            if ( s < 0 ) {
                s = assays.size();
            }

            if ( assays.size() > longest ) {
                longest = assays.size();
                longestBad = bad;
            }
        }
        return longestBad;
    }

    private void doQuantileNormalization( DoubleMatrix<CompositeSequence, Integer> matrix,
            Map<CompositeSequence, DoubleVectorValueObject> vectors ) {

        QuantileNormalizer<CompositeSequence, Integer> normalizer = new QuantileNormalizer<>();

        DoubleMatrix<CompositeSequence, Integer> normalized = normalizer.normalize( matrix );

        for ( int i = 0; i < normalized.rows(); i++ ) {
            double[] row = normalized.getRow( i );
            CompositeSequence cs = normalized.getRowName( i );
            DoubleVectorValueObject vec = vectors.get( cs );
            double[] data = vec.getData();
            System.arraycopy( row, 0, data, 0, row.length );
        }

    }

    private Collection<BioAssayDimension> getBioAssayDimensions( BioAssaySet ee ) {
        if ( ee instanceof ExpressionExperiment ) {
            StopWatch timer = new StopWatch();
            timer.start();
            List r = this.getSessionFactory().getCurrentSession().createQuery(
                    // this does not look efficient.
                    "select distinct bad from ExpressionExperiment e, BioAssayDimensionImpl bad"
                            + " inner join e.bioAssays b inner join bad.bioAssays badba where e = :ee and b in (badba) " )
                    .setParameter( "ee", ee ).list();
            timer.stop();
            if ( timer.getTime() > 100 ) {
                log.info( "Fetch " + r.size() + " bioassayDimensions for experiment id=" + ee.getId() + ": " + timer
                        .getTime() + "ms" );
            }
            //noinspection unchecked
            return ( Collection<BioAssayDimension> ) r;
        }

        // subset.
        return getBioAssayDimensions( getExperiment( ee ) );

    }

    private Map<BioAssaySet, Collection<BioAssayDimension>> getBioAssayDimensions(
            Collection<ExpressionExperiment> ees ) {
        Map<BioAssaySet, Collection<BioAssayDimension>> result = new HashMap<>();

        if ( ees.size() == 1 ) {
            ExpressionExperiment ee = ees.iterator().next();
            result.put( ee, getBioAssayDimensions( ee ) );
            return result;
        }

        StopWatch timer = new StopWatch();
        timer.start();
        List r = this.getSessionFactory().getCurrentSession().createQuery(
                "select distinct e, bad from ExpressionExperiment e, BioAssayDimensionImpl bad"
                        + " inner join e.bioAssays b inner join bad.bioAssays badba where e in (:ees) and b in (badba) " )
                .setParameterList( "ees", ees ).list();

        for ( Object o : r ) {
            Object[] tup = ( Object[] ) o;
            if ( !result.containsKey( tup[0] ) )
                result.put( ( BioAssaySet ) tup[0], new HashSet<BioAssayDimension>() );

            result.get( tup[0] ).add( ( BioAssayDimension ) tup[1] );
        }
        if ( timer.getTime() > 100 ) {
            log.info(
                    "Fetch " + r.size() + " bioassaydimensions for " + ees.size() + " experiment(s): " + timer.getTime()
                            + "ms" );
        }

        return result;

    }

    /**
     * Pre-fetch and construct the BioAssayDimensionValueObjects. Used on the basis that the data probably just have one
     * (or a few) BioAssayDimensionValueObjects needed, not a different one for each vector. See bug 3629 for details.
     */
    private Map<BioAssayDimension, BioAssayDimensionValueObject> getBioAssayDimensionValueObjects(
            Collection<? extends DesignElementDataVector> data ) {
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = new HashMap<>();
        for ( DesignElementDataVector v : data ) {
            BioAssayDimension bioAssayDimension = v.getBioAssayDimension();
            if ( !badVos.containsKey( bioAssayDimension ) ) {
                badVos.put( bioAssayDimension, new BioAssayDimensionValueObject( bioAssayDimension ) );
            }
        }
        return badVos;
    }

    private ExpressionExperiment getExperiment( BioAssaySet bas ) {
        ExpressionExperiment e;
        if ( bas instanceof ExpressionExperiment ) {
            e = ( ExpressionExperiment ) bas;
        } else if ( bas instanceof ExpressionExperimentSubSet ) {
            e = ( ( ExpressionExperimentSubSet ) bas ).getSourceExperiment();
        } else {
            throw new UnsupportedOperationException( "Couldn't handle a " + bas.getClass() );
        }
        assert e != null;
        return e;
    }

    /**
     * Determine the experiments that bioAssaySets refer to.
     *
     * @param bioAssaySets - either ExpressionExperiment or ExpressionExperimentSubSet (which has an associated
     *                     ExpressionExperiment, which is what we're after)
     * @return Note that this collection can be smaller than the input, if two bioAssaySets come from (or are) the same
     * Experiment
     */
    private Collection<ExpressionExperiment> getExperiments( Collection<? extends BioAssaySet> bioAssaySets ) {
        Collection<ExpressionExperiment> result = new HashSet<>();

        for ( BioAssaySet bas : bioAssaySets ) {
            ExpressionExperiment e = getExperiment( bas );

            result.add( e );
        }
        return result;
    }

    private QuantitationType getPreferredMaskedDataQuantitationType( QuantitationType preferredQt ) {
        QuantitationType present = QuantitationType.Factory.newInstance();

        // FIXME this name/description is confusing
        present.setName( preferredQt.getName() + " - Processed data " );
        present.setDescription( "Processed data (Computed by Gemma) for analysis" );

        present.setGeneralType( preferredQt.getGeneralType() );
        present.setIsBackground( false );
        present.setRepresentation( preferredQt.getRepresentation() ); // better be a number!
        present.setScale( preferredQt.getScale() );

        present.setIsPreferred( false ); // I think this is the right thing to do (but doesn't really matter)
        present.setIsMaskedPreferred( true );
        present.setIsBackgroundSubtracted( preferredQt.getIsBackgroundSubtracted() );

        present.setIsBatchCorrected( preferredQt.getIsBatchCorrected() );
        present.setIsRecomputedFromRawData( preferredQt.getIsRecomputedFromRawData() );

        present.setIsNormalized( preferredQt.getIsNormalized() );

        present.setIsRatio( preferredQt.getIsRatio() );
        present.setType( preferredQt.getType() );
        Long id = ( Long ) this.getSessionFactory().getCurrentSession().save( present );
        return ( QuantitationType ) this.getSessionFactory().getCurrentSession().load( QuantitationTypeImpl.class, id );
    }

    private Collection<DoubleVectorValueObject> getProcessedDataArraysByProbeIds( Collection<? extends BioAssaySet> ees,
            Collection<Long> probeIds ) {
        Collection<DoubleVectorValueObject> results = new HashSet<>();

        Map<Long, Collection<Long>> cs2gene = CommonQueries
                .getCs2GeneMapForProbes( probeIds, this.getSessionFactory().getCurrentSession() );

        Map<Long, Collection<Long>> noGeneProbes = new HashMap<>();
        for ( Long pid : probeIds ) {
            if ( !cs2gene.containsKey( pid ) || cs2gene.get( pid ).isEmpty() ) {
                noGeneProbes.put( pid, new HashSet<Long>() );
                cs2gene.remove( pid );
            }
        }

        log.info( cs2gene.size() + " probes associated with a gene; " + noGeneProbes.size() + " not" );

        /*
         * To Check the cache we need the list of genes 1st. Get from CS2Gene list then check the cache.
         */
        Collection<Long> genes = new HashSet<>();
        for ( Long cs : cs2gene.keySet() ) {
            genes.addAll( cs2gene.get( cs ) );
        }

        Collection<ExpressionExperiment> needToSearch = new HashSet<>();
        Collection<Long> genesToSearch = new HashSet<>();
        checkCache( ees, genes, results, needToSearch, genesToSearch );

        if ( !results.isEmpty() )
            log.info( results.size() + " vectors fetched from cache" );

        Map<ProcessedExpressionDataVector, Collection<Long>> rawResults = new HashMap<>();

        /*
         * Small problem: noGeneProbes are never really cached since we use the gene as part of that.
         */
        if ( !noGeneProbes.isEmpty() ) {
            Collection<ExpressionExperiment> eesForNoGeneProbes = new HashSet<>();
            for ( BioAssaySet ee : ees ) {
                if ( ee instanceof ExpressionExperiment ) {
                    eesForNoGeneProbes.add( ( ExpressionExperiment ) ee );
                } else {
                    eesForNoGeneProbes.add( ( ( ExpressionExperimentSubSet ) ee ).getSourceExperiment() );
                }
            }
            needToSearch.addAll( eesForNoGeneProbes );
            rawResults.putAll( getProcessedVectors( EntityUtils.getIds( eesForNoGeneProbes ), noGeneProbes ) );
        }

        if ( !rawResults.isEmpty() )
            log.info( rawResults.size() + " vectors retrieved so far, for noGeneProbes" );

        /*
         * Non-cached items.
         */
        if ( !needToSearch.isEmpty() ) {
            rawResults.putAll( getProcessedVectors( EntityUtils.getIds( needToSearch ), cs2gene ) );
        }

        if ( !rawResults.isEmpty() )
            log.info( rawResults.size() + " vectors retrieved so far, after fetching non-cached." );

        /*
         * Deal with possibility of 'gaps' and unpack the vectors.
         */
        Collection<DoubleVectorValueObject> newResults = new HashSet<>();
        for ( ExpressionExperiment ee : needToSearch ) {

            Collection<BioAssayDimension> bioAssayDimensions = this.getBioAssayDimensions( ee );

            if ( bioAssayDimensions.size() == 1 ) {
                newResults.addAll( unpack( rawResults ) );
            } else {
                /*
                 * See handleGetProcessedExpressionDataArrays(Collection<? extends BioAssaySet>, Collection<Gene>,
                 * boolean) and bug 1704.
                 */
                BioAssayDimension longestBad = checkRagged( bioAssayDimensions );
                assert longestBad != null;
                newResults.addAll( unpack( rawResults, longestBad ) );

            }

            if ( !newResults.isEmpty() ) {
                cacheResults( newResults );

                newResults = sliceSubsets( ees, newResults );

                results.addAll( newResults );
            }
        }

        return results;
    }

    /**
     * @param cs2gene Map of probe to genes.
     * @return map of vectors to genes.
     */
    private Map<ProcessedExpressionDataVector, Collection<Long>> getProcessedVectors( Collection<Long> ees,
            Map<Long, Collection<Long>> cs2gene ) {

        final String queryString;
        if ( ees == null || ees.size() == 0 ) {
            queryString =
                    "select dedv, dedv.designElement.id from ProcessedExpressionDataVectorImpl dedv fetch all properties"
                            + " where dedv.designElement.id in ( :cs ) ";
            return getVectorsForProbesInExperiments( cs2gene, queryString );
        }

        // Do not do in clause for experiments, as it can't use the indices
        queryString =
                "select dedv, dedv.designElement.id from ProcessedExpressionDataVectorImpl dedv fetch all properties"
                        + " where dedv.designElement.id in ( :cs ) and dedv.expressionExperiment.id  = :eeid ";
        Map<ProcessedExpressionDataVector, Collection<Long>> result = new HashMap<>();
        for ( Long ee : ees ) {
            result.putAll( getVectorsForProbesInExperiments( ee, cs2gene, queryString ) );
        }
        return result;

    }

    /**
     * This is an important method for fetching vectors.
     *
     * @return vectors, possibly subsetted.
     */
    private Collection<DoubleVectorValueObject> handleGetProcessedExpressionDataArrays(
            Collection<? extends BioAssaySet> ees, Collection<Long> genes ) {

        // ees must be thawed first as currently implemented (?)

        Collection<DoubleVectorValueObject> results = new HashSet<>();

        /*
         * Check the cache.
         */
        Collection<ExpressionExperiment> needToSearch = new HashSet<>();
        Collection<Long> genesToSearch = new HashSet<>();
        checkCache( ees, genes, results, needToSearch, genesToSearch );
        log.info( "Using " + results.size() + " DoubleVectorValueObject(s) from cache" );

        if ( needToSearch.size() == 0 ) {
            return results;
        }

        /*
         * Get items not in the cache.
         */
        log.info( "Searching for vectors for " + genes.size() + " genes from " + needToSearch.size()
                + " experiments not in cache" );

        Collection<ArrayDesign> arrays = CommonQueries.getArrayDesignsUsed( EntityUtils.getIds( getExperiments( ees ) ),
                this.getSessionFactory().getCurrentSession() ).keySet();
        assert !arrays.isEmpty();
        Map<Long, Collection<Long>> cs2gene = CommonQueries
                .getCs2GeneIdMap( genesToSearch, EntityUtils.getIds( arrays ),
                        this.getSessionFactory().getCurrentSession() );

        if ( cs2gene.size() == 0 ) {
            if ( results.isEmpty() ) {
                log.warn( "No composite sequences found for genes" );
                return new HashSet<>();
            }
            return results;
        }

        /*
         * Fill in the map, because we want to track information on the specificity of the probes used in the data
         * vectors.
         */
        cs2gene = CommonQueries
                .getCs2GeneMapForProbes( cs2gene.keySet(), this.getSessionFactory().getCurrentSession() );

        Map<ProcessedExpressionDataVector, Collection<Long>> processedDataVectors = getProcessedVectors(
                EntityUtils.getIds( needToSearch ), cs2gene );

        Map<BioAssaySet, Collection<BioAssayDimension>> bioAssayDimensions = this.getBioAssayDimensions( needToSearch );

        Collection<DoubleVectorValueObject> newResults = new HashSet<>();

        /*
         * This loop is to ensure that we don't get misaligned vectors for experiments that use more than one array
         * design. See bug 1704. This isn't that common, so we try to break out as soon as possible.
         */
        for ( BioAssaySet bas : needToSearch ) {

            Collection<BioAssayDimension> dims = bioAssayDimensions.get( bas );

            if ( dims == null || dims.isEmpty() ) {
                log.warn( "BioAssayDimensions were null/empty unexpectedly." );
                continue;
            }

            /*
             * Get the vectors for just this experiment. This is made more efficient by removing things from the map
             * each time through.
             */
            Map<ProcessedExpressionDataVector, Collection<Long>> vecsForBas = new HashMap<>();
            if ( needToSearch.size() == 1 ) {
                vecsForBas = processedDataVectors;
            } else {
                // isolate the vectors for the current experiment.
                for ( Iterator<ProcessedExpressionDataVector> it = processedDataVectors.keySet().iterator(); it
                        .hasNext(); ) {
                    ProcessedExpressionDataVector v = it.next();
                    if ( v.getExpressionExperiment().equals( bas ) ) {
                        vecsForBas.put( v, processedDataVectors.get( v ) );
                        it.remove(); // since we're done with it.
                    }
                }
            }

            /*
             * Now see if anything is 'ragged' (fewer bioassays per biomaterial than in some other vector)
             */
            if ( dims.size() == 1 ) {
                newResults.addAll( unpack( vecsForBas ) );
            } else {
                BioAssayDimension longestBad = checkRagged( dims );
                if ( longestBad == null ) {
                    newResults.addAll( unpack( vecsForBas ) );
                } else {
                    newResults.addAll( unpack( vecsForBas, longestBad ) );
                }
            }
        }

        /*
         * Finally....
         */

        if ( !newResults.isEmpty() ) {
            cacheResults( newResults );
            newResults = sliceSubsets( ees, newResults );
            results.addAll( newResults );
        }

        return results;

    }

    /**
     * Figure out if any platform used by the ee is two-channel
     */
    private boolean isTwoChannel( ExpressionExperiment expressionExperiment ) {

        boolean isTwoChannel = false;
        Collection<ArrayDesign> arrayDesignsUsed = CommonQueries
                .getArrayDesignsUsed( expressionExperiment, this.getSessionFactory().getCurrentSession() );
        for ( ArrayDesign ad : arrayDesignsUsed ) {
            TechnologyType technologyType = ad.getTechnologyType();

            if ( technologyType == null ) {
                throw new IllegalStateException(
                        "Array designs must have a technology type assigned before processed vector computation" );
            }

            if ( !technologyType.equals( TechnologyType.ONECOLOR ) && !technologyType.equals( TechnologyType.NONE ) ) {
                isTwoChannel = true;
            }
        }
        return isTwoChannel;
    }

    private Map<Long, Map<Long, Collection<DoubleVectorValueObject>>> makeCacheMap(
            Collection<DoubleVectorValueObject> newResults ) {
        Map<Long, Map<Long, Collection<DoubleVectorValueObject>>> mapForCache = new HashMap<>();
        for ( DoubleVectorValueObject v : newResults ) {
            ExpressionExperimentValueObject e = v.getExpressionExperiment();
            if ( !mapForCache.containsKey( e.getId() ) ) {
                mapForCache.put( e.getId(), new HashMap<Long, Collection<DoubleVectorValueObject>>() );
            }
            Map<Long, Collection<DoubleVectorValueObject>> innerMap = mapForCache.get( e.getId() );
            for ( Long g : v.getGenes() ) {
                if ( !innerMap.containsKey( g ) ) {
                    innerMap.put( g, new HashSet<DoubleVectorValueObject>() );
                }
                innerMap.get( g ).add( v );
            }
        }
        return mapForCache;
    }

    private Map<CompositeSequence, DoubleVectorValueObject> maskAndUnpack(
            Collection<RawExpressionDataVector> preferredData, Collection<RawExpressionDataVector> missingValueData ) {
        Map<CompositeSequence, DoubleVectorValueObject> unpackedData = unpack( preferredData );

        if ( missingValueData.size() == 0 ) {
            log.info( "There is no separate missing data information, simply using the data as is" );
            for ( DoubleVectorValueObject rv : unpackedData.values() ) {
                rv.setMasked( true );
            }
            return unpackedData;
        }

        Collection<BooleanVectorValueObject> unpackedMissingValueData = unpackBooleans( missingValueData );
        Map<CompositeSequenceValueObject, BooleanVectorValueObject> missingValueMap = new HashMap<>();
        for ( BooleanVectorValueObject bv : unpackedMissingValueData ) {
            missingValueMap.put( bv.getDesignElement(), bv );
        }

        boolean warned = false;
        for ( DoubleVectorValueObject rv : unpackedData.values() ) {
            double[] data = rv.getData();
            CompositeSequenceValueObject de = rv.getDesignElement();
            BooleanVectorValueObject mv = missingValueMap.get( de );
            if ( mv == null ) {
                if ( !warned && log.isWarnEnabled() )
                    log.warn( "No mask vector for " + de
                            + ", additional warnings for missing masks for this job will be skipped" );
                // we're missing a mask vector for it for some reason, but still flag it as effectively masked.
                rv.setMasked( true );
                warned = true;
                continue;
            }

            boolean[] mvdata = mv.getData();

            if ( mvdata.length != data.length ) {
                throw new IllegalStateException( "Missing value data didn't match data length" );
            }
            for ( int i = 0; i < data.length; i++ ) {
                if ( !mvdata[i] ) {
                    data[i] = Double.NaN;
                }
            }
            rv.setMasked( true );
        }

        return unpackedData;
    }

    /**
     * Do not call this on ratiometric or count data.
     */
    private void renormalize( Map<CompositeSequence, DoubleVectorValueObject> vectors ) {
        int cols = vectors.values().iterator().next().getBioAssayDimension().getBioAssays().size();
        DoubleMatrix<CompositeSequence, Integer> mat = new DenseDoubleMatrix<>( vectors.size(), cols );
        for ( int i = 0; i < cols; i++ ) {
            mat.setColumnName( i, i );
        }

        int i = 0;
        for ( CompositeSequence c : vectors.keySet() ) {
            DoubleVectorValueObject v = vectors.get( c );
            double[] data = v.getData();
            assert data.length == cols;
            for ( int j = 0; j < cols; j++ ) {
                mat.set( i, j, data[j] );
            }
            mat.setRowName( c, i );
            i++;
        }

        doQuantileNormalization( mat, vectors );

        assert mat.rows() == vectors.size();

    }

    /**
     * Given an ExpressionExperimentSubset and vectors from the source experiment, give vectors that include just the
     * data for the subset.
     */
    private Collection<DoubleVectorValueObject> sliceSubSet( ExpressionExperimentSubSet ee,
            Collection<DoubleVectorValueObject> obs ) {

        Collection<DoubleVectorValueObject> sliced = new HashSet<>();
        if ( obs == null || obs.isEmpty() )
            return sliced;

        this.getSessionFactory().getCurrentSession().buildLockRequest( LockOptions.NONE ).lock( ee );
        Hibernate.initialize( ee.getBioAssays() );
        List<BioAssayValueObject> sliceBioAssays = new ArrayList<>();

        DoubleVectorValueObject exemplar = obs.iterator().next();

        BioAssayDimensionValueObject bad = new BioAssayDimensionValueObject( -1L );
        bad.setName( "Subset of :" + exemplar.getBioAssayDimension().getName() );
        bad.setDescription( "Subset slice" );
        bad.setSourceBioAssayDimension( exemplar.getBioAssayDimension() );
        bad.setIsSubset( true );
        Collection<Long> subsetBioAssayIds = EntityUtils.getIds( ee.getBioAssays() );

        for ( BioAssayValueObject ba : exemplar.getBioAssays() ) {
            if ( !subsetBioAssayIds.contains( ba.getId() ) ) {
                continue;
            }

            sliceBioAssays.add( ba );
        }

        bad.addBioAssays( sliceBioAssays );
        for ( DoubleVectorValueObject vec : obs ) {
            DoubleVectorValueObject s = new DoubleVectorValueObject( ee, vec, bad );
            sliced.add( s );
        }

        return sliced;
    }

    /**
     * @param ees  Experiments and/or subsets required
     * @param vecs vectors to select from and if necessary slice, obviously from the given ees.
     * @return vectors that are for the requested subset. If an ee is not a subset, vectors will be unchanged. Otherwise
     * the data in a vector will be for the subset of samples in the ee subset.
     */
    private Collection<DoubleVectorValueObject> sliceSubsets( Collection<? extends BioAssaySet> ees,
            Collection<DoubleVectorValueObject> vecs ) {
        Collection<DoubleVectorValueObject> results = new HashSet<>();
        if ( vecs == null || vecs.isEmpty() )
            return results;

        for ( BioAssaySet bas : ees ) {
            if ( bas instanceof ExpressionExperimentSubSet ) {

                for ( DoubleVectorValueObject d : vecs ) {
                    if ( d.getExpressionExperiment().getId()
                            .equals( ( ( ExpressionExperimentSubSet ) bas ).getSourceExperiment().getId() ) ) {

                        Collection<DoubleVectorValueObject> ddvos = new HashSet<>();
                        ddvos.add( d );
                        results.addAll( sliceSubSet( ( ExpressionExperimentSubSet ) bas, ddvos ) );// coll

                    }
                }

            } else {
                for ( DoubleVectorValueObject d : vecs ) {
                    if ( d.getExpressionExperiment().getId().equals( bas.getId() ) ) {
                        results.add( d );
                    }
                }
            }

        }

        return results;
    }

    private Map<CompositeSequence, DoubleVectorValueObject> unpack(
            Collection<? extends DesignElementDataVector> data ) {
        Map<CompositeSequence, DoubleVectorValueObject> result = new HashMap<>();
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = getBioAssayDimensionValueObjects( data );
        for ( DesignElementDataVector v : data ) {
            result.put( v.getDesignElement(),
                    new DoubleVectorValueObject( v, badVos.get( v.getBioAssayDimension() ) ) );
        }
        return result;
    }

    private Map<CompositeSequence, DoubleVectorValueObject> unpack( Collection<? extends DesignElementDataVector> data,
            Map<Long, Collection<Long>> cs2GeneMap ) {
        Map<CompositeSequence, DoubleVectorValueObject> result = new HashMap<>();
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = getBioAssayDimensionValueObjects( data );
        for ( DesignElementDataVector v : data ) {
            result.put( v.getDesignElement(),
                    new DoubleVectorValueObject( v, cs2GeneMap.get( v.getDesignElement().getId() ),
                            badVos.get( v.getBioAssayDimension() ) ) );
        }
        return result;
    }

    private Collection<DoubleVectorValueObject> unpack( Collection<? extends DesignElementDataVector> data,
            Map<Long, Collection<Long>> cs2GeneMap, BioAssayDimension longestBad ) {
        Collection<DoubleVectorValueObject> result = new HashSet<>();
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = getBioAssayDimensionValueObjects( data );
        for ( DesignElementDataVector v : data ) {
            result.add( new DoubleVectorValueObject( v, badVos.get( v.getBioAssayDimension() ),
                    cs2GeneMap.get( v.getDesignElement().getId() ), longestBad ) );
        }
        return result;
    }

    private Collection<DoubleVectorValueObject> unpack(
            Map<? extends DesignElementDataVector, Collection<Long>> data ) {
        Collection<DoubleVectorValueObject> result = new HashSet<>();
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = getBioAssayDimensionValueObjects( data.keySet() );

        for ( DesignElementDataVector v : data.keySet() ) {
            result.add( new DoubleVectorValueObject( v, data.get( v ), badVos.get( v.getBioAssayDimension() ) ) );
        }
        return result;
    }

    private Collection<? extends DoubleVectorValueObject> unpack(
            Map<ProcessedExpressionDataVector, Collection<Long>> data, BioAssayDimension longestBad ) {
        Collection<DoubleVectorValueObject> result = new HashSet<>();
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = getBioAssayDimensionValueObjects( data.keySet() );
        for ( DesignElementDataVector v : data.keySet() ) {
            result.add( new DoubleVectorValueObject( v, badVos.get( v.getBioAssayDimension() ), data.get( v ),
                    longestBad ) );
        }
        return result;
    }

    private Collection<BooleanVectorValueObject> unpackBooleans( Collection<? extends DesignElementDataVector> data ) {
        Collection<BooleanVectorValueObject> result = new HashSet<>();

        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = getBioAssayDimensionValueObjects( data );

        for ( DesignElementDataVector v : data ) {
            result.add( new BooleanVectorValueObject( v, badVos.get( v.getBioAssayDimension() ) ) );
        }
        return result;
    }
}
