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
package ubic.gemma.analysis.expression.coexpression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import ubic.basecode.dataStructure.CountingMap;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.loader.protein.ProteinLinkOutFormatter;
import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetService;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressedGenesDetails;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionValueObject;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService;
import ubic.gemma.model.association.Gene2GeneProteinAssociation;
import ubic.gemma.model.association.Gene2GeneProteinAssociationService;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpression;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.ontology.providers.GeneOntologyService;
import ubic.gemma.util.AnchorTagUtil;

/**
 * Provides access to Gene2Gene and Probe2Probe links. The use of this service provides 'high-level' access to
 * functionality in the Gene2GeneCoexpressionService and the ProbeLinkCoexpressionAnalyzer.
 * 
 * @author paul
 * @version $Id$
 */
@Service
@Lazy
public class GeneCoexpressionService {

    /*
     * I assume the reason Genes weren't being loaded before is that it was too time consuming, so we'll do this
     * instead...
     */
    private static class SimpleGene extends Gene {
        private static final long serialVersionUID = 1L;

        public SimpleGene( Long id, String name, String officialName, Taxon taxon ) {
            super();
            this.setId( id );
            this.setOfficialSymbol( name );
            this.setOfficialName( officialName );
            this.setTaxon( taxon );
        }
    }

    private static Log log = LogFactory.getLog( GeneCoexpressionService.class.getName() );

    /**
     * How many genes to fill in the "go overlap" info for.
     */
    private static final int NUM_GENES_TO_DETAIL = 25;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    private Gene2GeneCoexpressionService gene2GeneCoexpressionService;

    @Autowired
    private GeneCoexpressionAnalysisService geneCoexpressionAnalysisService;

    @Autowired
    private GeneOntologyService geneOntologyService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private ProbeLinkCoexpressionAnalyzer probeLinkCoexpressionAnalyzer;

    @Autowired
    private Gene2GeneProteinAssociationService gene2GeneProteinAssociationService = null;

    /**
     * Perform a "custom" analysis, using an ad-hoc set of expreriments. Note that if possible, the query will be done
     * using results from an ExpressionExperimentSet.
     * 
     * @param eeIds Expression experiments to consider
     * @param genes Genes to find coexpression for
     * @param stringency Minimum support level
     * @param queryGenesOnly Whether to return only coexpression among the query genes (assuming there are more than
     *        one). Otherwise, coexpression with genes 'external' to the queries will be returned.
     * @param forceProbeLevelSearch If a probe-level search should always be done. This is primarily a testing and
     *        debugging feature. If false, searches will be done using 'canned' results if possible.
     * @return
     */
    public CoexpressionMetaValueObject coexpressionSearch( Collection<Long> eeIds, Collection<Gene> genes,
            int stringency, int maxResults, boolean queryGenesOnly, boolean forceProbeLevelSearch ) {

        if ( eeIds == null ) eeIds = new HashSet<Long>();
        Collection<BioAssaySet> ees = getPossibleExpressionExperiments( genes );

        if ( ees.isEmpty() ) {
            CoexpressionMetaValueObject r = new CoexpressionMetaValueObject();
            r.setErrorState( "Gene(s) are not tested in any experiments" );
            return r;
        }

        if ( !eeIds.isEmpty() ) {
            // remove the expression experiments we're not interested in...
            Collection<BioAssaySet> eesToRemove = new HashSet<BioAssaySet>();
            for ( BioAssaySet ee : ees ) {
                if ( !eeIds.contains( ee.getId() ) ) eesToRemove.add( ee );
            }
            ees.removeAll( eesToRemove );
        }

        /*
         * repopulate eeIds with the actual eeIds we'll be searching through and load ExpressionExperimentValueObjects
         * to get summary information about the datasets...
         */
        eeIds.clear();
        for ( BioAssaySet ee : ees ) {
            eeIds.add( ee.getId() );
        }

        List<ExpressionExperimentValueObject> eevos = getSortedEEvos( eeIds );

        CoexpressionMetaValueObject result = initValueObject( genes, eevos, false );

        if ( eeIds.isEmpty() ) {
            result = new CoexpressionMetaValueObject();
            result.setErrorState( "No experiments selected" );
            return result;
        }

        /*
         * If possible: instead of using the probeLinkCoexpressionAnalyzer, Use a canned analysis with a filter.
         */
        if ( !forceProbeLevelSearch ) {
            Collection<ExpressionExperimentSet> eeSets = expressionExperimentSetService.loadAll();

            for ( ExpressionExperimentSet eeSet : eeSets ) {

                Collection<Long> eeSetIds = new ArrayList<Long>();
                for ( BioAssaySet baSet : eeSet.getExperiments() ) {
                    eeSetIds.add( baSet.getId() );
                }

                if ( eeSetIds.containsAll( eeIds ) ) {
                    log.info( "Using canned analysis to conduct customized analysis" );
                    return getFilteredCannedAnalysisResults( eeSet.getId(), eeIds, genes, stringency, maxResults,
                            queryGenesOnly );
                }
            }
        }

        /*
         * If we get this far, there was no matching analysis so we do it using the probe2probe table. This is
         * relatively slow so should be avoided.
         */

        for ( ExpressionExperimentValueObject eevo : eevos ) {
            // FIXME don't reuse this field.
            eevo.setExternalUri( AnchorTagUtil.getExpressionExperimentUrl( eevo.getId() ) );
        }

        boolean knownGenesOnly = true; // !SecurityService.isUserAdmin();
        result.setKnownGenesOnly( knownGenesOnly );

        Collection<Long> geneIds = new HashSet<Long>( genes.size() );
        for ( Gene gene : genes ) {
            geneIds.add( gene.getId() );
        }

        Map<Gene, CoexpressionCollectionValueObject> allCoexpressions = new HashMap<Gene, CoexpressionCollectionValueObject>();

        if ( genes.size() == 1 ) {
            Gene soleQueryGene = genes.iterator().next();
            allCoexpressions.put( soleQueryGene, probeLinkCoexpressionAnalyzer.linkAnalysis( soleQueryGene, ees,
                    stringency, knownGenesOnly, maxResults ) );
        } else {
            /*
             * Batch mode
             */
            allCoexpressions = probeLinkCoexpressionAnalyzer.linkAnalysis( genes, ees, stringency, knownGenesOnly,
                    queryGenesOnly, maxResults );
        }

        for ( Gene queryGene : allCoexpressions.keySet() ) {

            CoexpressionCollectionValueObject coexpressions = allCoexpressions.get( queryGene );

            result.setErrorState( coexpressions.getErrorState() );
            // fill in the protein interaction details if present
            Map<Long, String> proteinInteractionsForQueryGene = this
                    .getGene2GeneProteinAssociationForQueryGene( queryGene );

            // only fill this in for
            addExtCoexpressionValueObjects( queryGene, eevos, coexpressions.getKnownGeneCoexpression(), stringency,
                    queryGenesOnly, geneIds, result.getKnownGeneResults(), result.getKnownGeneDatasets(),
                    proteinInteractionsForQueryGene );

            // FIXME only do this part if the user is logged in?
            addExtCoexpressionValueObjects( queryGene, eevos, coexpressions.getPredictedGeneCoexpression(), stringency,
                    queryGenesOnly, geneIds, result.getPredictedGeneResults(), result.getPredictedGeneDatasets(),
                    proteinInteractionsForQueryGene );
            addExtCoexpressionValueObjects( queryGene, eevos, coexpressions.getProbeAlignedRegionCoexpression(),
                    stringency, queryGenesOnly, geneIds, result.getProbeAlignedRegionResults(), result
                            .getProbeAlignedRegionDatasets(), proteinInteractionsForQueryGene );

            CoexpressionSummaryValueObject summary = new CoexpressionSummaryValueObject();
            summary.setDatasetsAvailable( eevos.size() );
            summary.setDatasetsTested( coexpressions.getEesQueryTestedIn().size() );
            summary.setLinksFound( coexpressions.getNumKnownGenes() );
            summary.setLinksMetPositiveStringency( coexpressions.getKnownGeneCoexpression()
                    .getPositiveStringencyLinkCount() );
            summary.setLinksMetNegativeStringency( coexpressions.getKnownGeneCoexpression()
                    .getNegativeStringencyLinkCount() );
            result.getSummary().put( queryGene.getOfficialSymbol(), summary );
        }

        return result;
    }

    /**
     * This is the entry point for queries starting from a preset ExpressionExperimentSet.
     * 
     * @param eeSetId expressionExperimentSetId
     * @param genes Genes to find coexpression for
     * @param stringency Minimum support level
     * @param queryGenesOnly Whether to return only coexpression among the query genes (assuming there are more than
     *        one). Otherwise, coexpression with genes 'external' to the queries will be returned.
     * @param queryGenesOnly
     * @return
     */
    public CoexpressionMetaValueObject coexpressionSearch( Long eeSetId, Collection<Gene> queryGenes, int stringency,
            int maxResults, boolean queryGenesOnly ) {
        return getFilteredCannedAnalysisResults( eeSetId, null, queryGenes, stringency, maxResults, queryGenesOnly );
    }

    /**
     * Skips some of the postprocessing steps, use in situations where raw speed is more important than details.
     * 
     * @param eeSetId
     * @param queryGenes
     * @param stringency
     * @param maxResults
     * @param queryGenesOnly
     * @param specificProbesOnly if true, limit query to results from specific probes.
     * @return
     */
    public Collection<CoexpressionValueObjectExt> coexpressionSearchQuick( Long eeSetId, Collection<Gene> queryGenes,
            int stringency, int maxResults, boolean queryGenesOnly, boolean specificProbesOnly ) {

        ExpressionExperimentSet baseSet = expressionExperimentSetService.load( eeSetId );

        if ( baseSet == null ) {
            throw new IllegalArgumentException( "No such expressionexperiment set with id=" + eeSetId );
        }

        /*
         * This set of links must be filtered to include those in the data sets being analyzed.
         */
        Map<Gene, Collection<Gene2GeneCoexpression>> gg2gs = getRawCoexpression( queryGenes, stringency, maxResults,
                queryGenesOnly );

        Collection<Long> eeIdsToUse = getIds( baseSet );

        /*
         * We get this prior to filtering so it matches the vectors stored with the analysis.
         */
        Map<Integer, Long> positionToIDMap = GeneLinkCoexpressionAnalyzer.getPositionToIdMap( eeIdsToUse );

        List<CoexpressionValueObjectExt> ecvos = new ArrayList<CoexpressionValueObjectExt>();

        Collection<Gene2GeneCoexpression> seen = new HashSet<Gene2GeneCoexpression>();

        geneService.thawLite( gg2gs.keySet() );

        StopWatch timer = new StopWatch();
        timer.start();
        Collection<Gene> foundGenes = new HashSet<Gene>();
        for ( Gene queryGene : gg2gs.keySet() ) {

            if ( !queryGene.getTaxon().equals( baseSet.getTaxon() ) ) {
                throw new IllegalArgumentException(
                        "Mismatch between taxon for expression experiment set selected and gene queries" );
            }

            Collection<Gene2GeneCoexpression> g2gs = gg2gs.get( queryGene );
            Map<Long, String> proteinInteractionMap = this.getGene2GeneProteinAssociationForQueryGene( queryGene );

            assert g2gs != null;

            for ( Gene2GeneCoexpression g2g : g2gs ) {
                Gene foundGene = g2g.getFirstGene().equals( queryGene ) ? g2g.getSecondGene() : g2g.getFirstGene();
                CoexpressionValueObjectExt cvo = new CoexpressionValueObjectExt();

                // geneService.thawLite( foundGene );
                foundGenes.add( foundGene );

                cvo.setQueryGene( queryGene );
                cvo.setFoundGene( foundGene );

                if ( proteinInteractionMap != null && !( proteinInteractionMap.isEmpty() ) ) {
                    String url = proteinInteractionMap.get( foundGene.getId() );
                    log.debug( "A coexpression link in GEMMA as a interaction in STRING " + url );
                    cvo.setGene2GeneProteinAssociationStringUrl( url );
                }

                /*
                 * necesssary in case any were filtered out (for example, if this is a virtual analysis; or there were
                 * 'troubled' ees. Note that 'supporting' includes 'non-specific' if they were recorded by the analyzer.
                 */
                Collection<Long> supportingDatasets = GeneLinkCoexpressionAnalyzer.getSupportingExperimentIds( g2g,
                        positionToIDMap );

                if ( specificProbesOnly ) {
                    Collection<Long> specificDatasets = GeneLinkCoexpressionAnalyzer.getSpecificExperimentIds( g2g,
                            positionToIDMap );
                    supportingDatasets.retainAll( specificDatasets );
                }

                cvo.setSupportingExperiments( supportingDatasets );
                int numSupportingDatasets = supportingDatasets.size();

                /*
                 * This check is necessary in case any data sets were filtered out. (i.e., we're not interested in the
                 * full set of data sets that were used in the original analysis.
                 */
                if ( numSupportingDatasets < stringency ) {
                    continue;
                }

                if ( g2g.getEffect() < 0 ) {
                    cvo.setPosSupp( 0 );
                    cvo.setNegSupp( numSupportingDatasets );
                } else {
                    cvo.setPosSupp( numSupportingDatasets );
                    cvo.setNegSupp( 0 );
                }

                /*
                 * This check prevents links from being shown twice when we do "among query genes". We don't skip
                 * entirely so we get the counts for the summary table populated correctly.
                 */
                if ( !seen.contains( g2g ) ) {
                    ecvos.add( cvo );
                }

                seen.add( g2g );
            }
        }
        geneService.thawLite( foundGenes );
        if ( timer.getTime() > 1000 ) {
            log.info( "Process " + ecvos.size() + " results in " + timer.getTime() + "ms" );
        }
        return ecvos;

    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setExpressionExperimentSetService( ExpressionExperimentSetService expressionExperimentSetService ) {
        this.expressionExperimentSetService = expressionExperimentSetService;
    }

    public void setGene2GeneCoexpressionService( Gene2GeneCoexpressionService gene2GeneCoexpressionService ) {
        this.gene2GeneCoexpressionService = gene2GeneCoexpressionService;
    }

    public void setGeneCoexpressionAnalysisService( GeneCoexpressionAnalysisService geneCoexpressionAnalysisService ) {
        this.geneCoexpressionAnalysisService = geneCoexpressionAnalysisService;
    }

    public void setGeneOntologyService( GeneOntologyService geneOntologyService ) {
        this.geneOntologyService = geneOntologyService;
    }

    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    public void setProbeLinkCoexpressionAnalyzer( ProbeLinkCoexpressionAnalyzer probeLinkCoexpressionAnalyzer ) {
        this.probeLinkCoexpressionAnalyzer = probeLinkCoexpressionAnalyzer;
    }

    /**
     * @param gene2GeneProteinAssociationService the gene2GeneProteinAssociationService to set
     */
    public void setGene2GeneProteinAssociationService(
            Gene2GeneProteinAssociationService gene2GeneProteinAssociationService ) {
        this.gene2GeneProteinAssociationService = gene2GeneProteinAssociationService;
    }

    /**
     * Convert CoexpressionValueObject into CoexpressionValueObjectExt objects to be passed to the client for display.
     * This is used for probe-level queries.
     * 
     * @param queryGene
     * @param eevos
     * @param coexp
     * @param stringency
     * @param queryGenesOnly
     * @param geneIds
     * @param results object we are adding to
     * @param datasetResults
     * @param proteinInteractionsForQueryGene map keyed on geneid of string url for protein interaction
     */
    private void addExtCoexpressionValueObjects( Gene queryGene, List<ExpressionExperimentValueObject> eevos,
            CoexpressedGenesDetails coexp, int stringency, boolean queryGenesOnly, Collection<Long> geneIds,
            Collection<CoexpressionValueObjectExt> results, Collection<CoexpressionDatasetValueObject> datasetResults,
            Map<Long, String> proteinInteractionsForQueryGene ) {

        for ( CoexpressionValueObject cvo : coexp.getCoexpressionData( stringency ) ) {
            if ( queryGenesOnly && !geneIds.contains( cvo.getGeneId() ) ) continue;

            CoexpressionValueObjectExt ecvo = new CoexpressionValueObjectExt();
            ecvo.setQueryGene( queryGene );
            ecvo.setFoundGene( new SimpleGene( cvo.getGeneId(), cvo.getGeneName(), cvo.getGeneOfficialName(), queryGene
                    .getTaxon() ) );

            ecvo.setPosSupp( cvo.getPositiveLinkSupport() );
            ecvo.setNegSupp( cvo.getNegativeLinkSupport() );
            ecvo.setSupportKey( 10 * Math.max( ecvo.getPosSupp(), ecvo.getNegSupp() ) );

            // if there are some protein protein interactions for this gene see if the given coexpreesed gene is in the
            // map of interaactions
            // and if so get the value for the url.
            if ( proteinInteractionsForQueryGene != null && !( proteinInteractionsForQueryGene.isEmpty() ) ) {
                String url = proteinInteractionsForQueryGene.get( cvo.getGeneId() );
                log.debug( "Coexpression  found for interaction " + url );

                ecvo.setGene2GeneProteinAssociationStringUrl( url );
            }
            /*
             * Fill in the support based on 'non-specific' probes.
             */
            if ( !cvo.getExpressionExperiments().isEmpty() ) {
                ecvo.setNonSpecPosSupp( getNonSpecificLinkCount( cvo.getEEContributing2PositiveLinks(), cvo
                        .getNonspecificEE() ) );
                ecvo.setNonSpecNegSupp( getNonSpecificLinkCount( cvo.getEEContributing2NegativeLinks(), cvo
                        .getNonspecificEE() ) );
            }

            ecvo.setNumTestedIn( cvo.getNumDatasetsTestedIn() );

            ecvo.setGoSim( cvo.getGoOverlap() != null ? cvo.getGoOverlap().size() : 0 );
            ecvo.setMaxGoSim( cvo.getPossibleOverlap() );

            StringBuilder datasetVector = new StringBuilder();
            Collection<Long> supportingEEs = new ArrayList<Long>();

            for ( int i = 0; i < eevos.size(); ++i ) {
                ExpressionExperimentValueObject eevo = eevos.get( i );

                Long eeid = eevo.getId();
                boolean tested = cvo.getDatasetsTestedIn() != null && cvo.getDatasetsTestedIn().contains( eeid );

                assert cvo.getExpressionExperiments().size() <= cvo.getPositiveLinkSupport()
                        + cvo.getNegativeLinkSupport() : "got " + cvo.getExpressionExperiments().size() + " expected "
                        + ( cvo.getPositiveLinkSupport() + cvo.getNegativeLinkSupport() );

                boolean supported = cvo.getExpressionExperiments().contains( eeid );

                boolean specific = !cvo.getNonspecificEE().contains( eeid );

                if ( supported ) {
                    if ( specific ) {
                        datasetVector.append( "3" );
                    } else {
                        datasetVector.append( "2" );
                    }
                    supportingEEs.add( eeid );
                } else if ( tested ) {
                    datasetVector.append( "1" );
                } else {
                    datasetVector.append( "0" );
                }
            }

            ecvo.setDatasetVector( datasetVector.toString() );
            ecvo.setSupportingExperiments( supportingEEs );

            ecvo.setSortKey();
            results.add( ecvo );
        }

        for ( ExpressionExperimentValueObject eevo : eevos ) {
            if ( !coexp.getExpressionExperimentIds().contains( eevo.getId() )
                    || coexp.getLinkCountForEE( eevo.getId() ) == 0 ) continue;
            ExpressionExperimentValueObject coexpEevo = coexp.getExpressionExperiment( eevo.getId() );
            if ( coexpEevo == null ) continue;
            CoexpressionDatasetValueObject ecdvo = new CoexpressionDatasetValueObject();
            ecdvo.setId( eevo.getId() );
            ecdvo.setQueryGene( queryGene.getOfficialSymbol() );
            ecdvo.setCoexpressionLinkCount( coexp.getLinkCountForEE( coexpEevo.getId() ) );
            ecdvo.setRawCoexpressionLinkCount( coexp.getRawLinkCountForEE( coexpEevo.getId() ) );

            // NOTE should be accurate (probe-level query) but we won't show it. See bug 1564
            ecdvo.setProbeSpecificForQueryGene( coexpEevo.getHasProbeSpecificForQueryGene() );
            ecdvo.setArrayDesignCount( eevo.getArrayDesignCount() );
            ecdvo.setBioAssayCount( eevo.getBioAssayCount() );
            datasetResults.add( ecdvo );
        }
    }

    /**
     * This is necessary in case there is more than one gene2gene analysis in the system. The common case is when a new
     * analysis is in progress. Only one analysis should be enableed at any given time.
     * 
     * @param queryGenes
     * @return
     */
    private GeneCoexpressionAnalysis findEnabledCoexpressionAnalysis( Collection<Gene> queryGenes ) {
        GeneCoexpressionAnalysis gA = null;
        Gene g = queryGenes.iterator().next();
        // note: we assume they all come from one taxon.
        Taxon t = g.getTaxon();
        Collection<? extends Analysis> analyses = null;
        // check if the taxon is a species if it is not then it is a parent taxon and need to get child taxa
        // coexpression analyses.
        if ( !t.getIsSpecies() ) {
            analyses = geneCoexpressionAnalysisService.findByParentTaxon( t );
        } else {
            analyses = geneCoexpressionAnalysisService.findByTaxon( t );
        }

        if ( analyses.size() == 0 ) {
            throw new IllegalStateException( "No gene coexpression analysis is available for " + t.getScientificName() );
        } else if ( analyses.size() == 1 ) {
            gA = ( GeneCoexpressionAnalysis ) analyses.iterator().next();
        } else {
            for ( Analysis analysis : analyses ) {
                GeneCoexpressionAnalysis c = ( GeneCoexpressionAnalysis ) analysis;
                if ( c.getEnabled() ) {
                    if ( gA == null ) {
                        gA = c;
                    } else {
                        throw new IllegalStateException(
                                "System should only have a single gene2gene coexpression analysis enabled per taxon, found more than one for "
                                        + t );
                    }
                }
            }
        }
        return gA;
    }

    /**
     * @param eevos
     * @param result
     * @param supportCount
     * @param supportingExperimentIds
     * @param queryGene
     */
    private void generateDatasetSummary( List<ExpressionExperimentValueObject> eevos,
            CoexpressionMetaValueObject result, CountingMap<Long> supportCount,
            Collection<Long> supportingExperimentIds, Gene queryGene ) {
        /*
         * generate dataset summary info for this query gene...
         */
        for ( ExpressionExperimentValueObject eevo : eevos ) {
            if ( !supportingExperimentIds.contains( eevo.getId() ) ) continue;
            CoexpressionDatasetValueObject ecdvo = new CoexpressionDatasetValueObject();
            ecdvo.setId( eevo.getId() );
            ecdvo.setQueryGene( queryGene.getOfficialSymbol() );
            ecdvo.setCoexpressionLinkCount( supportCount.get( eevo.getId() ) );
            ecdvo.setRawCoexpressionLinkCount( null ); // not available
            ecdvo.setProbeSpecificForQueryGene( true ); // we shouldn't display this. See bug 1564.
            ecdvo.setArrayDesignCount( eevo.getArrayDesignCount() );
            ecdvo.setBioAssayCount( eevo.getBioAssayCount() );
            result.getKnownGeneDatasets().add( ecdvo );
        }
    }

    /**
     * @param supporting
     * @param testing
     * @param specific
     * @param allIds
     * @return String representation of binary vector (might as well be a string, as it gets sent to the browser that
     *         way). 0 = not tested; 1 = tested but not supporting; 2 = supporting but not specific; 3 supporting and
     *         specific.
     */
    private String getDatasetVector( Collection<Long> supporting, Collection<Long> testing, Collection<Long> specific,
            List<Long> allIds ) {
        StringBuilder datasetVector = new StringBuilder();
        for ( Long id : allIds ) {
            boolean tested = testing.contains( id );
            boolean supported = supporting.contains( id );
            boolean s = specific.contains( id );

            if ( supported ) {
                if ( s ) {
                    datasetVector.append( "3" );
                } else {
                    datasetVector.append( "2" );
                }
            } else if ( tested ) {
                datasetVector.append( "1" );
            } else {
                datasetVector.append( "0" );
            }
        }
        return datasetVector.toString();
    }

    /**
     * Get coexpression results using a pure gene2gene query (without visiting the probe2probe tables. This is generally
     * faster, probably even if we're only interested in data from a subset of the exeriments.
     * 
     * @param eeSetId the base expression experimnent set to refer to for analysis results.
     * @param eeIds Experiments to limit the results to (can be null)
     * @param queryGenes
     * @param stringency
     * @param maxResults
     * @param queryGenesOnly return links among the query genes only.
     * @return
     */
    private CoexpressionMetaValueObject getFilteredCannedAnalysisResults( Long eeSetId, Collection<Long> eeIds,
            Collection<Gene> queryGenes, int stringency, int maxResults, boolean queryGenesOnly ) {

        ExpressionExperimentSet baseSet = expressionExperimentSetService.load( eeSetId );

        if ( baseSet == null ) {
            throw new IllegalArgumentException( "No such expressionexperiment set with id=" + eeSetId );
        }

        /*
         * This set of links must be filtered to include those in the data sets being analyzed.
         */
        Map<Gene, Collection<Gene2GeneCoexpression>> gg2gs = getRawCoexpression( queryGenes, stringency, maxResults,
                queryGenesOnly );

        Collection<Long> eeIdsFromAnalysis = getIds( baseSet );

        /*
         * We get this prior to filtering so it matches the vectors stored with the analysis.
         */
        Map<Integer, Long> positionToIDMap = GeneLinkCoexpressionAnalyzer.getPositionToIdMap( eeIdsFromAnalysis );

        /*
         * Now we get the data sets we area actually concerned with.
         */
        Collection<Long> eeIdsTouse = null;
        if ( eeIds == null ) {
            eeIdsTouse = eeIdsFromAnalysis;
        } else {
            eeIdsTouse = eeIds;
        }

        List<Long> filteredEeIds = getSortedFilteredIdList( eeIdsTouse );

        // This sort is necessary.(?)
        List<ExpressionExperimentValueObject> eevos = getSortedEEvos( eeIdsTouse );

        CoexpressionMetaValueObject result = initValueObject( queryGenes, eevos, true );

        List<CoexpressionValueObjectExt> ecvos = new ArrayList<CoexpressionValueObjectExt>();

        Collection<Gene2GeneCoexpression> seen = new HashSet<Gene2GeneCoexpression>();

        geneService.thawLite( gg2gs.keySet() );

        // populate the value objects.
        StopWatch timer = new StopWatch();
        for ( Gene queryGene : gg2gs.keySet() ) {
            timer.start();

            if ( !queryGene.getTaxon().equals( baseSet.getTaxon() ) ) {
                throw new IllegalArgumentException(
                        "Mismatch between taxon for expression experiment set selected and gene queries" );
            }

            /*
             * For summary statistics
             */
            CountingMap<Long> supportCount = new CountingMap<Long>();
            Collection<Long> allSupportingDatasets = new HashSet<Long>();
            Collection<Long> allDatasetsWithSpecificProbes = new HashSet<Long>();
            Collection<Long> allTestedDataSets = new HashSet<Long>();

            int linksMetPositiveStringency = 0;
            int linksMetNegativeStringency = 0;

            Collection<Gene2GeneCoexpression> g2gs = gg2gs.get( queryGene );

            assert g2gs != null;

            List<Long> relevantEEIdList = getRelevantEEidsForBitVector( positionToIDMap, g2gs );

            HashMap<Gene, Collection<Gene2GeneCoexpression>> foundGenes = new HashMap<Gene, Collection<Gene2GeneCoexpression>>();

            // for queryGene get the interactions
            Map<Long, String> proteinInteractionMap = this.getGene2GeneProteinAssociationForQueryGene( queryGene );

            for ( Gene2GeneCoexpression g2g : g2gs ) {
                Gene foundGene = g2g.getFirstGene().equals( queryGene ) ? g2g.getSecondGene() : g2g.getFirstGene();

                // FIXME Symptom fix for duplicate found genes
                // Keep track of the found genes that we can correctly identify duplicates.
                // All keep the g2g object for debugging purposes.
                if ( foundGenes.containsKey( foundGene ) ) {
                    foundGenes.get( foundGene ).add( g2g );
                    log.warn( "Duplicate gene found in coexpression results, skipping: " + foundGene
                            + " From analysis: " + g2g.getSourceAnalysis().getId() );
                    continue; // Found a duplicate gene, don't add to results just our debugging list

                }

                foundGenes.put( foundGene, new ArrayList<Gene2GeneCoexpression>() );
                foundGenes.get( foundGene ).add( g2g );

                CoexpressionValueObjectExt cvo = new CoexpressionValueObjectExt();

                geneService.thawLite( foundGene );

                cvo.setQueryGene( queryGene );
                cvo.setFoundGene( foundGene );

                // set the interaction if none null will be put
                if ( proteinInteractionMap != null && !( proteinInteractionMap.isEmpty() ) ) {
                    String url = proteinInteractionMap.get( foundGene.getId() );
                    log.debug( "A coexpression link in Gemma has an interaction in STRING of " + url );
                    cvo.setGene2GeneProteinAssociationStringUrl( url );
                }

                Collection<Long> testingDatasets = GeneLinkCoexpressionAnalyzer.getTestedExperimentIds( g2g,
                        positionToIDMap );
                testingDatasets.retainAll( filteredEeIds );

                /*
                 * necesssary in case any were filtered out (for example, if this is a virtual analysis; or there were
                 * 'troubled' ees. Note that 'supporting' includes 'non-specific' if they were recorded by the analyzer.
                 */
                Collection<Long> supportingDatasets = GeneLinkCoexpressionAnalyzer.getSupportingExperimentIds( g2g,
                        positionToIDMap );

                // necessary in case any were filtered out.
                supportingDatasets.retainAll( filteredEeIds );

                cvo.setSupportingExperiments( supportingDatasets );

                Collection<Long> specificDatasets = GeneLinkCoexpressionAnalyzer.getSpecificExperimentIds( g2g,
                        positionToIDMap );

                /*
                 * Specific probe EEids contains 1 even if the data set wasn't supporting.
                 */
                specificDatasets.retainAll( supportingDatasets );

                int numTestingDatasets = testingDatasets.size();
                int numSupportingDatasets = supportingDatasets.size();

                /*
                 * SANITY CHECKS
                 */
                assert specificDatasets.size() <= numSupportingDatasets;
                assert numTestingDatasets >= numSupportingDatasets;
                assert numTestingDatasets <= eevos.size();

                cvo.setDatasetVector( getDatasetVector( supportingDatasets, testingDatasets, specificDatasets,
                        relevantEEIdList ) );

                /*
                 * This check is necessary in case any data sets were filtered out. (i.e., we're not interested in the
                 * full set of data sets that were used in the original analysis.
                 */
                if ( numSupportingDatasets < stringency ) {
                    continue;
                }

                allTestedDataSets.addAll( testingDatasets );

                int supportFromSpecificProbes = specificDatasets.size();
                if ( g2g.getEffect() < 0 ) {
                    cvo.setPosSupp( 0 );
                    cvo.setNegSupp( numSupportingDatasets );
                    if ( numSupportingDatasets != supportFromSpecificProbes )
                        cvo.setNonSpecNegSupp( numSupportingDatasets - supportFromSpecificProbes );

                    ++linksMetNegativeStringency;
                } else {
                    cvo.setPosSupp( numSupportingDatasets );
                    if ( numSupportingDatasets != supportFromSpecificProbes )
                        cvo.setNonSpecPosSupp( numSupportingDatasets - supportFromSpecificProbes );
                    cvo.setNegSupp( 0 );
                    ++linksMetPositiveStringency;
                }
                cvo.setSupportKey( Math.max( cvo.getPosSupp(), cvo.getNegSupp() ) );
                cvo.setNumTestedIn( numTestingDatasets );

                for ( Long id : supportingDatasets ) {
                    supportCount.increment( id );
                }

                cvo.setSortKey();

                /*
                 * This check prevents links from being shown twice when we do "among query genes". We don't skip
                 * entirely so we get the counts for the summary table populated correctly.
                 */
                if ( !seen.contains( g2g ) ) {
                    ecvos.add( cvo );
                }

                seen.add( g2g );

                allSupportingDatasets.addAll( supportingDatasets );
                allDatasetsWithSpecificProbes.addAll( specificDatasets );

            }

            // FIXME This is only necessary for debugging purposes. Helps us keep track of duplicate genes found above.
            if ( log.isDebugEnabled() ) {
                for ( Gene foundGene : foundGenes.keySet() ) {
                    if ( foundGenes.get( foundGene ).size() > 1 ) {
                        log.debug( "** DUPLICATE: " + foundGene.getOfficialSymbol()
                                + " found multiple times. Gene2Genes objects are: " );
                        for ( Gene2GeneCoexpression g1g : foundGenes.get( foundGene ) ) {
                            log.debug( " ============ Gene2Gene Id: " + g1g.getId() + " 1st gene: "
                                    + g1g.getFirstGene().getOfficialSymbol() + " 2nd gene: "
                                    + g1g.getSecondGene().getOfficialSymbol() + " Source Analysis: "
                                    + g1g.getSourceAnalysis().getId() + " # of dataSets: " + g1g.getNumDataSets() );
                        }
                    }
                }
            }

            CoexpressionSummaryValueObject summary = makeSummary( eevos, allTestedDataSets,
                    allDatasetsWithSpecificProbes, linksMetPositiveStringency, linksMetNegativeStringency );
            result.getSummary().put( queryGene.getOfficialSymbol(), summary );

            generateDatasetSummary( eevos, result, supportCount, allSupportingDatasets, queryGene );

            /*
             * FIXME I'm lazy and rushed, so I'm using an existing field for this info; probably better to add another
             * field to the value object...
             */
            for ( ExpressionExperimentValueObject eevo : eevos ) {
                eevo.setExternalUri( AnchorTagUtil.getExpressionExperimentUrl( eevo.getId() ) );
            }

            Collections.sort( ecvos );
            getGoOverlap( ecvos, queryGene );

            timer.stop();
            if ( timer.getTime() > 1000 ) {
                log.info( "Postprocess " + g2gs.size() + " results for " + queryGene.getOfficialSymbol() + ": "
                        + timer.getTime() + "ms" );
            }
            timer.reset();
        }

        result.getKnownGeneResults().addAll( ecvos );
        return result;
    }

    /**
     * @param ecvos (sorted)
     * @param queryGene
     */
    private void getGoOverlap( List<CoexpressionValueObjectExt> ecvos, Gene queryGene ) {
        if ( !geneOntologyService.isGeneOntologyLoaded() ) {
            return;
        }

        /*
         * get GO overlap info for this query gene...
         */
        StopWatch timer = new StopWatch();
        timer.start();

        int numQueryGeneGoTerms = geneOntologyService.getGOTerms( queryGene ).size();
        Collection<Long> overlapIds = new HashSet<Long>();
        for ( CoexpressionValueObjectExt ecvo : ecvos ) {
            overlapIds.add( ecvo.getFoundGene().getId() );
            if ( overlapIds.size() >= NUM_GENES_TO_DETAIL ) break;
        }
        Map<Long, Collection<OntologyTerm>> goOverlap = geneOntologyService.calculateGoTermOverlap( queryGene,
                overlapIds );
        int i = 0;
        for ( CoexpressionValueObjectExt ecvo : ecvos ) {
            ecvo.setMaxGoSim( numQueryGeneGoTerms );
            Collection<OntologyTerm> overlap = goOverlap.get( ecvo.getFoundGene().getId() );
            ecvo.setGoSim( overlap == null ? null : overlap.size() );
            if ( ++i >= NUM_GENES_TO_DETAIL ) break;
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "GO stats for " + queryGene.getName() + " + " + overlapIds.size() + " coexpressed genes :"
                    + timer.getTime() + "ms" );
        }
    }

    /**
     * @param expressionExperimentSet
     */
    private List<Long> getIds( ExpressionExperimentSet expressionExperimentSet ) {
        List<Long> ids = new ArrayList<Long>( expressionExperimentSet.getExperiments().size() );
        for ( BioAssaySet dataset : expressionExperimentSet.getExperiments() ) {
            ids.add( dataset.getId() );
        }
        return ids;
    }

    /**
     * @param contributingEEs
     * @param nonSpecificEEs
     * @return
     */
    private int getNonSpecificLinkCount( Collection<Long> contributingEEs, Collection<Long> nonSpecificEEs ) {
        int n = 0;
        for ( Long id : contributingEEs ) {
            if ( nonSpecificEEs.contains( id ) ) ++n;
        }
        return n;
    }

    /**
     * Ignore experiments that don't represent the genes we're querying for.
     * 
     * @param genes
     * @return
     */
    private Collection<BioAssaySet> getPossibleExpressionExperiments( Collection<Gene> genes ) {
        Collection<BioAssaySet> result = new HashSet<BioAssaySet>();
        if ( genes.isEmpty() ) {
            return result;
        }

        for ( Gene g : genes ) {
            result.addAll( expressionExperimentService.findByGene( g ) );
        }
        if ( result.size() == 0 ) {
            log.warn( "No datasets for gene. If this is unexpected, check that the GENE2CS table is up to date." );
        }
        return result;
    }

    /**
     * Retrieve all gene2gene coexpression information for the genes at the specified stringency, using methods that
     * don't filter by experiment.
     * 
     * @param queryGenes
     * @param stringency
     * @param maxResults
     * @param queryGenesOnly
     * @return
     */
    private Map<Gene, Collection<Gene2GeneCoexpression>> getRawCoexpression( Collection<Gene> queryGenes,
            int stringency, int maxResults, boolean queryGenesOnly ) {
        Map<Gene, Collection<Gene2GeneCoexpression>> gg2gs = new HashMap<Gene, Collection<Gene2GeneCoexpression>>();

        if ( queryGenes.size() == 0 ) {
            return gg2gs;
        }

        StopWatch timer = new StopWatch();
        timer.start();
        GeneCoexpressionAnalysis gA = findEnabledCoexpressionAnalysis( queryGenes );
        timer.stop();
        if ( timer.getTime() > 100 ) {
            log.info( "Get analysis: " + timer.getTime() + "ms" );
        }
        timer.reset();
        timer.start();

        if ( queryGenesOnly ) {
            if ( queryGenes.size() < 2 ) {
                throw new IllegalArgumentException( "Must have at least two genes to do 'my genes only'" );
            }
            gg2gs = gene2GeneCoexpressionService.findInterCoexpressionRelationship( queryGenes, stringency, gA );
        } else {
            gg2gs = gene2GeneCoexpressionService.findCoexpressionRelationships( queryGenes, stringency, maxResults, gA );
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Get raw coexpression: " + timer.getTime() + "ms" );
        }

        return gg2gs;
    }

    /**
     * @param positionToIDMap
     * @param g2gs
     * @return
     */
    private List<Long> getRelevantEEidsForBitVector( Map<Integer, Long> positionToIDMap,
            Collection<Gene2GeneCoexpression> g2gs ) {
        Collection<Long> relevantEEIds = new HashSet<Long>();
        List<Long> relevantEEIdList = new ArrayList<Long>();
        for ( Gene2GeneCoexpression g2g : g2gs ) {
            relevantEEIds.addAll( GeneLinkCoexpressionAnalyzer.getTestedExperimentIds( g2g, positionToIDMap ) );
        }
        relevantEEIdList.addAll( relevantEEIds );
        Collections.sort( relevantEEIdList );
        return relevantEEIdList;
    }

    /**
     * @param eeIds
     * @return
     */
    private List<ExpressionExperimentValueObject> getSortedEEvos( Collection<Long> eeIds ) {

        /* security will filter experiments */
        Collection<ExpressionExperiment> filteredExperiments = expressionExperimentService.loadMultiple( eeIds );

        Collection<Long> filteredIds = new HashSet<Long>();
        for ( ExpressionExperiment ee : filteredExperiments ) {
            filteredIds.add( ee.getId() );
        }

        List<ExpressionExperimentValueObject> eevos = new ArrayList<ExpressionExperimentValueObject>(
                expressionExperimentService.loadValueObjects( filteredIds ) );

        Collections.sort( eevos, new Comparator<ExpressionExperimentValueObject>() {
            public int compare( ExpressionExperimentValueObject eevo1, ExpressionExperimentValueObject eevo2 ) {
                return eevo1.getId().compareTo( eevo2.getId() );
            }
        } );
        return eevos;
    }

    /**
     * For a given query gene retrieve it's protein protein interactions. Iterating through those interactions create a
     * map keyed on the gene association that was retreived for that given gene. E.g. query gene 'AB' has interactions
     * with 'BB' and 'CC' then create a map using the ids as keys from BB and CC. and the value using the String url for
     * that interaction
     * 
     * @param gene The gene to find associations for
     * @return Map of gene ids and their string urls.
     */
    protected Map<Long, String> getGene2GeneProteinAssociationForQueryGene( Gene gene ) {
        Map<Long, String> stringUrlsMappedByGeneID = new HashMap<Long, String>();
        Collection<Gene2GeneProteinAssociation> proteinInteractions = this.gene2GeneProteinAssociationService
                .findProteinInteractionsForGene( gene );
        // check if found any interactions
        if ( proteinInteractions != null && !proteinInteractions.isEmpty() ) {

            for ( Gene2GeneProteinAssociation proteinInteraction : proteinInteractions ) {
                log.debug( "found interaction for gene " + proteinInteraction.getFirstGene() + " and "
                        + proteinInteraction.getSecondGene() );
                if ( proteinInteraction.getDatabaseEntry() != null
                        && proteinInteraction.getSecondGene().getId() != null
                        && proteinInteraction.getFirstGene().getId() != null ) {
                    // can append extra details to link if required this formating code should be somewhere else?
                    ProteinLinkOutFormatter proteinFormatter = new ProteinLinkOutFormatter();
                    String proteinProteinIdUrl = proteinFormatter.getStringProteinProteinInteractionLinkGemmaDefault( 
                            proteinInteraction.getDatabaseEntry() );
                    if ( proteinInteraction.getFirstGene().getId().equals( gene.getId() ) ) {
                        stringUrlsMappedByGeneID.put( proteinInteraction.getSecondGene().getId(), proteinProteinIdUrl );
                    } else {
                        stringUrlsMappedByGeneID.put( proteinInteraction.getFirstGene().getId(), proteinProteinIdUrl );
                    }
                }
            }
        }
        return stringUrlsMappedByGeneID;

    }

    /**
     * Remove data sets that are 'troubled' and sort the list.
     * 
     * @param datasets
     * @return
     */
    private List<Long> getSortedFilteredIdList( Collection<Long> datasets ) {

        removeTroubledEes( datasets );

        List<Long> ids = new ArrayList<Long>( datasets );
        Collections.sort( ids );
        return ids;
    }

    /**
     * @param genes
     * @param eevos
     * @param isCanned
     * @return
     */
    private CoexpressionMetaValueObject initValueObject( Collection<Gene> genes,
            List<ExpressionExperimentValueObject> eevos, boolean isCanned ) {
        CoexpressionMetaValueObject result = new CoexpressionMetaValueObject();
        result.setQueryGenes( new ArrayList<Gene>( genes ) );
        result.setDatasets( eevos );
        result.setIsCannedAnalysis( isCanned );
        result.setKnownGeneDatasets( new ArrayList<CoexpressionDatasetValueObject>() );
        result.setKnownGeneResults( new ArrayList<CoexpressionValueObjectExt>() );
        result.setPredictedGeneDatasets( new ArrayList<CoexpressionDatasetValueObject>() );
        result.setPredictedGeneResults( new ArrayList<CoexpressionValueObjectExt>() );
        result.setProbeAlignedRegionDatasets( new ArrayList<CoexpressionDatasetValueObject>() );
        result.setProbeAlignedRegionResults( new ArrayList<CoexpressionValueObjectExt>() );
        result.setSummary( new HashMap<String, CoexpressionSummaryValueObject>() );
        return result;
    }

    /**
     * @param eevos
     * @param datasetsTested
     * @param datasetsWithSpecificProbes
     * @param linksMetPositiveStringency
     * @param linksMetNegativeStringency
     * @return
     */
    private CoexpressionSummaryValueObject makeSummary( List<ExpressionExperimentValueObject> eevos,
            Collection<Long> datasetsTested, Collection<Long> datasetsWithSpecificProbes,
            int linksMetPositiveStringency, int linksMetNegativeStringency ) {
        CoexpressionSummaryValueObject summary = new CoexpressionSummaryValueObject();
        summary.setDatasetsAvailable( eevos.size() );
        summary.setDatasetsTested( datasetsTested.size() );
        summary.setDatasetsWithSpecificProbes( datasetsWithSpecificProbes.size() );
        summary.setLinksFound( linksMetPositiveStringency + linksMetNegativeStringency );
        summary.setLinksMetPositiveStringency( linksMetPositiveStringency );
        summary.setLinksMetNegativeStringency( linksMetNegativeStringency );
        return summary;
    }

    /**
     * FIXME partly duplicates code from ExpressionExperimentManipulatingCLI / AuditableUtil.
     * 
     * @param ees
     */
    private void removeTroubledEes( Collection<Long> ees ) {

        if ( ees == null || ees.size() == 0 ) {
            log.warn( "No experiments to remove troubled from" );
            return;
        }

        int size = ees.size();
        final Map<Long, AuditEvent> trouble = expressionExperimentService.getLastTroubleEvent( ees );
        CollectionUtils.filter( ees, new Predicate() {
            public boolean evaluate( Object id ) {
                boolean hasTrouble = trouble.containsKey( id );
                return !hasTrouble;
            }
        } );
        int newSize = ees.size();
        if ( newSize != size ) {
            assert newSize < size;
            log.info( "Removed " + ( size - newSize ) + " experiments with 'trouble' flags, leaving " + newSize );
        }
    }

}
