/*
 * The Gemma project
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

package ubic.gemma.search;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocCollector;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.compass.core.Compass;
import org.compass.core.CompassCallback;
import org.compass.core.CompassException;
import org.compass.core.CompassHighlightedText;
import org.compass.core.CompassHighlighter;
import org.compass.core.CompassHit;
import org.compass.core.CompassHits;
import org.compass.core.CompassQuery;
import org.compass.core.CompassSession;
import org.compass.core.CompassTemplate;
import org.compass.core.engine.SearchEngineException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicService;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.Treatment;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PredictedGene;
import ubic.gemma.model.genome.ProbeAlignedRegion;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.gene.GeneProductService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.util.ReflectionUtil;

/**
 * This service is used for performing searches using free text or exact matches to items in the database. <h2>
 * Implementation notes</h2>
 * <p>
 * Internally, there are generally two kinds of searches performed, percise database searches looking for exact matches
 * in the database and compass/lucene searches which look for matches in the stored index.
 * <p>
 * To add more dependencies to this Service edit the applicationContext-search.xml
 * 
 * @author klc
 * @author paul
 * @author keshav
 * @version $Id$
 */
@Service
public class SearchService implements InitializingBean {

    /**
     * Key for internal in-memory on-the-fly indexes
     */
    private static final String INDEX_KEY = "content";

    /**
     * Defines the properties we look at for 'highlighting'.
     */
    static final String[] propertiesToSearch = new String[] { "all", "name", "description",
            "expressionExperiment.description", "expressionExperiment.name", "shortName", "abstract",
            "expressionExperiment.bioAssays.name", "expressionExperiment.bioAssays.description", "title",
            "expressionExperiment.experimentalDesign.experimentalFactors.name",
            "expressionExperiment.experimentalDesign.experimentalFactors.description",
            "expressionExperiment.primaryPublication.title", "expressionExperiment.primaryPublication.abstractText",
            "expressionExperiment.primaryPublication.authorList",
            "expressionExperiment.otherRelevantPublications.abstractText",
            "expressionExperiment.otherRelevantPublications.title" };

    /**
     * Penalty applied to scores on hits for entities that derive from an association. For example, if a hit to an EE
     * came from text associated with one of its biomaterials, the score is penalized by this amount.
     */
    private static final double INDIRECT_DB_HIT_PENALTY = 0.8;

    /**
     * Penalty applied to all 'index' hits
     */
    private static final double COMPASS_HIT_SCORE_PENALTY_FACTOR = 0.9;

    /**
     * How long an item in the cache lasts when it is not accessed.
     */
    private static final int ONTOLOGY_CACHE_TIME_TO_IDLE = 600;

    /**
     * How long after creation before an object is evicted.
     */
    private static final int ONTOLOGY_CACHE_TIME_TO_DIE = 2000;

    /**
     * How many term children can stay in memory
     */
    private static final int ONTOLOGY_INFO_CACHE_SIZE = 500;

    private static final int MINIMUM_EE_QUERY_LENGTH = 3;

    private static final int MINIMUM_STRING_LENGTH_FOR_FREE_TEXT_SEARCH = 3;

    private static final String NCBI_GENE = "ncbi_gene";

    private static Log log = LogFactory.getLog( SearchService.class.getName() );

    Analyzer analyzer = new StandardAnalyzer();

    private Cache childTermCache;

    @Autowired
    private Gene2GOAssociationService gene2GOAssociationService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private GeneProductService geneProductService;

    @Autowired
    private CompositeSequenceService compositeSequenceService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private BibliographicReferenceService bibliographicReferenceService;

    @Autowired
    private BioSequenceService bioSequenceService;

    @Autowired
    private CharacteristicService characteristicService;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private Compass compassGene;

    @Autowired
    private Compass compassExpression;

    @Autowired
    private Compass compassArray;

    @Autowired
    private Compass compassBibliographic;

    @Autowired
    private Compass compassBiosequence;

    @Autowired
    private Compass compassProbe;

    @Autowired
    private CacheManager cacheManager;

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        try {

            if ( cacheManager.cacheExists( "OntologyChildrenCache" ) ) {
                return;
            }
            childTermCache = new Cache( "OntologyChildrenCache", ONTOLOGY_INFO_CACHE_SIZE,
                    MemoryStoreEvictionPolicy.LFU, false, null, true, ONTOLOGY_CACHE_TIME_TO_DIE,
                    ONTOLOGY_CACHE_TIME_TO_IDLE, false, 500, null );

            cacheManager.addCache( childTermCache );
            childTermCache = cacheManager.getCache( "OntologyChildrenCache" );

        } catch ( CacheException e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * The results are sorted in order of decreasing score, organized by class. The following objects can be searched
     * for, depending on the configuration of the input object.
     * <ul>
     * <li>Genes
     * <li>ExpressionExperiments
     * <li>CompositeSequences (probes)
     * <li>ArrayDesigns (platforms)
     * <li>Characteristics (e.g., Ontology annotations)
     * <li>BioSequences
     * <li>BibliographicReferences (articles)
     * </ul>
     * 
     * @param settings
     * @return Map of Class to SearchResults. The results are already filtered for security considerations.
     */
    public Map<Class<?>, List<SearchResult>> search( SearchSettings settings ) {
        Map<Class<?>, List<SearchResult>> searchResults = new HashMap<Class<?>, List<SearchResult>>();
        try {
            searchResults = this.search( settings, true );

        } catch ( org.compass.core.engine.SearchEngineQueryParseException qpe ) {
            log.error( "Query parse Error: " + settings + "; message=" + qpe.getMessage(), qpe );

        } catch ( Exception e ) {
            log.error( "Search error on settings: " + settings + "; message=" + e.getMessage(), e );
        }
        return searchResults;
    }

    /**
     * Makes an attempt at determining of the query term is a valid URI from an Ontology in Gemma or a Gene URI (a GENE
     * URI is in the form: http://purl.org/commons/record/ncbi_gene/20655 (but is not a valid ontology loaded in gemma)
     * ). If so then searches for objects that have been tagged with that term or any of that terms children. If not a
     * URI then proceeds with the generalSearch.
     * 
     * @param settings
     * @param fillObjects If false, the entities will not be filled in inside the searchsettings; instead, they will be
     *        nulled (for security purposes). You can then use the id and Class stored in the SearchSettings to load the
     *        entities at your leisure. If true, the entities are loaded in the usual secure fashion. Setting this to
     *        false can be an optimization if all you need is the id.
     * @return
     * @see SearchService.search(SearchSettings settings)
     */
    public Map<Class<?>, List<SearchResult>> search( SearchSettings settings, boolean fillObjects ) {

        // 1st check to see if the query is a URI (from an ontology).
        // Do this by seeing if we can find it in the loaded ontologies.
        // Escape with general utilities because might not be doing a lucene backed search. (just a hibernate one).
        String uriString = StringEscapeUtils.escapeJava( StringUtils.strip( settings.getQuery() ) );
        OntologyTerm matchingTerm = this.ontologyService.getTerm( uriString );

        if ( matchingTerm != null && matchingTerm.getUri() != null ) {
            // Was a URI from a loaded ontology soo get the children.
            Collection<OntologyTerm> terms2Search4 = matchingTerm.getChildren( true );
            terms2Search4.add( matchingTerm );

            Collection<Class<?>> classesToSearch = new HashSet<Class<?>>();
            classesToSearch.add( ExpressionExperiment.class );
            classesToSearch.add( BioMaterial.class );

            Collection<SearchResult> matchingResults = this.databaseCharacteristicExactUriSearchForOwners(
                    classesToSearch, terms2Search4 );
            Map<Class<?>, List<SearchResult>> sortedFilteredResults = new HashMap<Class<?>, List<SearchResult>>();

            for ( SearchResult searchR : matchingResults ) {
                if ( sortedFilteredResults.containsKey( searchR.getResultClass() ) ) {
                    sortedFilteredResults.get( searchR.getResultClass() ).add( searchR );
                } else {
                    List<SearchResult> rs = new ArrayList<SearchResult>();
                    rs.add( searchR );
                    sortedFilteredResults.put( searchR.getResultClass(), rs );
                }
            }

            return sortedFilteredResults;

        }// Perhaps is a valid gene URL. Want to search for the gene in gemma.
        else if ( StringUtils.containsIgnoreCase( uriString, NCBI_GENE ) ) {
            // 1st get objects tagged with the given gene identifier
            Collection<Class<?>> classesToFilterOn = new HashSet<Class<?>>();
            classesToFilterOn.add( ExpressionExperiment.class );

            Collection<Characteristic> foundCharacteristics = characteristicService.findByUri( uriString );
            Map<Characteristic, Object> parentMap = characteristicService.getParents( foundCharacteristics );

            Collection<SearchResult> characteriticOwnerResults = filterCharacteristicOwnersByClass( classesToFilterOn,
                    parentMap );

            // Get results from general search using the found gene's gene symbol
            String ncbiAccessionFromUri = StringUtils.substringAfterLast( uriString, "/" );
            Gene g = geneService.findByNCBIId( ncbiAccessionFromUri );
            if ( g == null ) {
                Map<Class<?>, List<SearchResult>> justCharacteristicResults = new HashMap<Class<?>, List<SearchResult>>();
                List<SearchResult> sortedCharacteristicResults = new ArrayList<SearchResult>();
                sortedCharacteristicResults.addAll( characteriticOwnerResults );
                justCharacteristicResults.put( ExpressionExperiment.class, sortedCharacteristicResults );
                return justCharacteristicResults;
            }

            settings.setQuery( g.getOfficialSymbol() );
            Map<Class<?>, List<SearchResult>> genResults = this.search( settings );
            genResults.get( ExpressionExperiment.class ).addAll( characteriticOwnerResults );
            return genResults;

        }

        // Not an valid ontology term or a gene.
        // Proceed with general query parsing and searching.
        return generalSearch( settings, fillObjects );
    }

    /**
     * @param query if empty, all experiments for the taxon are returned; otherwise, we use the search facility.
     * @param taxonId required.
     * @return Collection of ids.
     */
    public Collection<Long> searchExpressionExperiments( String query, Long taxonId ) {
        Taxon taxon = taxonService.load( taxonId );
        Collection<Long> eeIds = new HashSet<Long>();
        if ( StringUtils.isNotBlank( query ) ) {

            if ( query.length() < MINIMUM_EE_QUERY_LENGTH ) return eeIds;

            // Initial list
            List<SearchResult> results = this.search( SearchSettings.expressionExperimentSearch( query ), false ).get(
                    ExpressionExperiment.class );
            for ( SearchResult result : results ) {
                eeIds.add( result.getId() );
            }

            // Filter by taxon
            if ( taxon != null ) {
                Collection<Long> eeIdsToKeep = new HashSet<Long>();
                Collection<ExpressionExperiment> ees = expressionExperimentService.findByTaxon( taxon );
                for ( ExpressionExperiment ee : ees ) {
                    if ( eeIds.contains( ee.getId() ) ) eeIdsToKeep.add( ee.getId() );
                }
                eeIds.retainAll( eeIdsToKeep );
            }
        } else {
            Collection<ExpressionExperiment> ees = ( taxon != null ) ? expressionExperimentService.findByTaxon( taxon )
                    : expressionExperimentService.loadAll();
            for ( ExpressionExperiment ee : ees ) {
                eeIds.add( ee.getId() );
            }
        }
        return eeIds;
    }

    /**
     * Makes no attempt at resovling the search query as a URI. Will tokenize the search query if there are control
     * characters in the String. URI's will get parsed into multiple query terms and lead to bad results.
     * 
     * @param settings Will try to resolve general terms like brain --> to appropriate OntologyTerms and search for
     *        objects tagged with those terms (if isUseCharacte = true)
     * @param fillObjects If false, the entities will not be filled in inside the searchsettings; instead, they will be
     *        nulled (for security purposes). You can then use the id and Class stored in the SearchSettings to load the
     *        entities at your leisure. If true, the entities are loaded in the usual secure fashion. Setting this to
     *        false can be an optimization if all you need is the id. * @return
     */
    protected Map<Class<?>, List<SearchResult>> generalSearch( SearchSettings settings, boolean fillObjects ) {
        String searchString = QueryParser.escape( StringUtils.strip( settings.getQuery() ) );
        settings.setQuery( searchString );

        // If nothing to search return nothing.
        if ( StringUtils.isBlank( searchString ) ) {
            return new HashMap<Class<?>, List<SearchResult>>();
        }

        List<SearchResult> rawResults = new ArrayList<SearchResult>();

        if ( settings.isSearchExperiments() ) {
            Collection<SearchResult> foundEEs = expressionExperimentSearch( settings );
            rawResults.addAll( foundEEs );
        }

        Collection<SearchResult> genes = null;
        if ( settings.isSearchGenes() ) {
            genes = geneSearch( settings );
            accreteResults( rawResults, genes );
        }

        Collection<SearchResult> compositeSequences = null;
        if ( settings.isSearchProbes() ) {
            compositeSequences = compositeSequenceSearch( settings );
            accreteResults( rawResults, compositeSequences );
        }

        if ( settings.isSearchArrays() ) {
            Collection<SearchResult> foundADs = arrayDesignSearch( settings, compositeSequences );
            accreteResults( rawResults, foundADs );
        }

        if ( settings.isSearchBioSequences() ) {
            Collection<SearchResult> bioSequences = bioSequenceSearch( settings, genes );
            accreteResults( rawResults, bioSequences );
        }

        if ( settings.isSearchGenesByGO() ) {
            Collection<SearchResult> ontologyGenes = dbHitsToSearchResult( gene2GOAssociationService.findByGOTerm(
                    searchString, settings.getTaxon() ) );
            accreteResults( rawResults, ontologyGenes );
        }

        if ( settings.isSearchBibrefs() ) {
            Collection<SearchResult> bibliographicReferences = compassBibliographicReferenceSearch( settings );
            accreteResults( rawResults, bibliographicReferences );
        }

        Map<Class<?>, List<SearchResult>> sortedLimitedResults = getSortedLimitedResults( settings, rawResults,
                fillObjects );

        log.info( "search for: " + settings.getQuery() + " " + rawResults.size()
                + " raw results (final tally may be filtered)" );

        return sortedLimitedResults;
    }

    /**
     * Runs inside Compass transaction
     * 
     * @param query
     * @param session
     * @return
     */
    Collection<SearchResult> performSearch( SearchSettings settings, CompassSession session ) {
        StopWatch watch = startTiming();

        String query = settings.getQuery().trim();
        if ( StringUtils.isBlank( query ) || query.length() < MINIMUM_STRING_LENGTH_FOR_FREE_TEXT_SEARCH
                || query.equals( "*" ) ) return new ArrayList<SearchResult>();

        CompassQuery compassQuery = session.queryBuilder().queryString( query.trim() ).toQuery();
        CompassHits hits = compassQuery.hits();

        watch.stop();
        if ( watch.getTime() > 1000 ) {
            log.info( "Getting " + hits.getLength() + " hits for " + query + " took " + watch.getTime() + " ms" );
        }

        cacheHighlightedText( hits );

        Collection<SearchResult> searchResults = getSearchResults( hits );

        return searchResults;
    }

    /**
     * Add results.
     * 
     * @param rawResults To add to
     * @param newResults To be added
     */
    private void accreteResults( List<SearchResult> rawResults, Collection<SearchResult> newResults ) {
        for ( SearchResult sr : newResults ) {
            if ( !rawResults.contains( sr ) ) {
                /*
                 * We do this because we don't want to clobber results, when the same object comes up more than once in
                 * different searches. FIXME - perhaps check if the score of the existing one is lower?
                 */
                rawResults.add( sr );
            }
        }
    }

    /**
     * @param characteristicUris
     * @param term
     */
    @SuppressWarnings("unchecked")
    private void addChildTerms( Collection<String> characteristicUris, OntologyTerm term ) {
        String uri = term.getUri();
        /*
         * getChildren can be very slow for 'high-level' classes like "neoplasm", so we use a cache.
         */
        Collection<OntologyTerm> children = null;
        if ( StringUtils.isBlank( uri ) ) {
            // shouldn't happen, but just in case
            if ( log.isDebugEnabled() ) log.debug( "Blank uri for " + term );
        }

        Element cachedChildren = this.childTermCache.get( uri );
        // log.debug("Getting children of " + term);
        if ( cachedChildren == null ) {
            children = term.getChildren( false );
            childTermCache.put( new Element( uri, children ) );
        } else {
            children = ( Collection<OntologyTerm> ) cachedChildren.getValue();
        }

        for ( OntologyTerm child : children ) {
            characteristicUris.add( child.getUri() );
        }
    }

    /**
     * A general search for array designs.
     * <p>
     * This search does both an database search and a compass search. This is also contains an underlying
     * {@link CompositeSequence} search, returning the {@link ArrayDesign} collection for the given composite sequence
     * search string (the returned collection of array designs does not contain duplicates).
     * 
     * @param searchString
     * @param probeResults Collection of results from a previous CompositeSequence search. Can be null; otherwise used
     *        to avoid a second search for probes. The array designs for the probes are added to the final results.
     * @return
     */
    private Collection<SearchResult> arrayDesignSearch( SearchSettings settings, Collection<SearchResult> probeResults ) {

        StopWatch watch = startTiming();
        String searchString = settings.getQuery();
        Collection<SearchResult> results = new HashSet<SearchResult>();

        ArrayDesign shortNameResult = arrayDesignService.findByShortName( searchString );
        if ( shortNameResult != null ) {
            results.add( new SearchResult( shortNameResult, 1.0 ) );
        } else {
            ArrayDesign nameResult = arrayDesignService.findByName( searchString );
            if ( nameResult != null ) results.add( new SearchResult( nameResult, 1.0 ) );
        }

        Collection<ArrayDesign> altNameResults = arrayDesignService.findByAlternateName( searchString );
        for ( ArrayDesign arrayDesign : altNameResults ) {
            results.add( new SearchResult( arrayDesign, 0.9 ) );
        }

        Collection<ArrayDesign> manufacturerResults = arrayDesignService.findByManufacturer( searchString );
        for ( ArrayDesign arrayDesign : manufacturerResults ) {
            results.add( new SearchResult( arrayDesign, 0.9 ) );
        }

        results.addAll( compassArrayDesignSearch( settings ) );
        results.addAll( databaseArrayDesignSearch( settings ) );

        Collection<SearchResult> probes = null;
        if ( probeResults == null ) {
            probes = compassCompositeSequenceSearch( settings );
        } else {
            probes = probeResults;
        }

        for ( SearchResult r : probes ) {
            CompositeSequence cs = ( CompositeSequence ) r.getResultObject();
            if ( cs.getArrayDesign() == null ) // This might happen as compass
                // might not have indexed the AD
                // for the CS
                continue;
            results.add( r );
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            log.info( "Array Design search for '" + settings + "' took " + watch.getTime() + " ms" );

        return results;
    }

    /**
     * *
     * 
     * @param searchString
     * @param previousGeneSearchResults Can be null, otherwise used to avoid a second search for genes. The biosequences
     *        for the genes are added to the final results.
     * @return
     */
    private Collection<SearchResult> bioSequenceSearch( SearchSettings settings,
            Collection<SearchResult> previousGeneSearchResults ) {
        StopWatch watch = startTiming();

        Collection<SearchResult> allResults = new HashSet<SearchResult>();
        allResults.addAll( compassBioSequenceSearch( settings, previousGeneSearchResults ) );
        allResults.addAll( databaseBioSequenceSearch( settings ) );

        watch.stop();
        if ( watch.getTime() > 1000 )
            log.info( "Biosequence search for '" + settings + "' took " + watch.getTime() + " ms " + allResults.size()
                    + " results." );
        return allResults;
    }

    /**
     * This is needed to unpack the highlighted text from the index. Or something like that. If we don't do this the
     * highlighted text is always empty (last time I checked...)
     * 
     * @param hits
     */
    private Map<CompassHit, String> cacheHighlightedText( CompassHits hits ) {
        Map<CompassHit, String> textMap = new HashMap<CompassHit, String>();
        for ( int i = 0; i < hits.getLength(); i++ ) {
            CompassHit hit = hits.hit( i );

            // if you skip this, we get nothing for highlighted text later.
            String text = getHighlightedText( hits, i );
            if ( text != null ) {
                textMap.put( hit, text );
            }
        }
        return textMap;
    }

    /**
     * @param settings
     */
    @SuppressWarnings("unchecked")
    private Collection<SearchResult> characteristicExpressionExperimentSearch( final SearchSettings settings ) {
        Collection<SearchResult> results = new HashSet<SearchResult>();

        Collection<Class<?>> classesToSearch = new HashSet<Class<?>>();
        classesToSearch.add( ExpressionExperiment.class );
        classesToSearch.add( BioMaterial.class );
        classesToSearch.add( FactorValue.class );
        classesToSearch.add( Treatment.class );

        Collection<SearchResult> characterSearchResults = ontologySearchAnnotatedObject( classesToSearch, settings );

        StopWatch watch = new StopWatch();
        watch.start();

        // filter and get parents...
        int numEEs = 0;
        Collection<BioMaterial> biomaterials = new HashSet<BioMaterial>();
        Collection<FactorValue> factorValues = new HashSet<FactorValue>();
        Collection<Treatment> treatments = new HashSet<Treatment>();

        for ( SearchResult sr : characterSearchResults ) {
            Class resultClass = sr.getResultClass();
            if ( ExpressionExperiment.class.isAssignableFrom( resultClass ) ) {
                sr.setHighlightedText( sr.getHighlightedText() + " (characteristic)" );
                results.add( sr );
                numEEs++;
            } else if ( BioMaterial.class.isAssignableFrom( resultClass ) ) {
                biomaterials.add( ( BioMaterial ) sr.getResultObject() );
            } else if ( FactorValue.class.isAssignableFrom( resultClass ) ) {
                factorValues.add( ( FactorValue ) sr.getResultObject() );
            } else if ( Treatment.class.isAssignableFrom( resultClass ) ) {
                treatments.add( ( Treatment ) sr.getResultObject() );
            }
        }

        /*
         * Much faster to batch it...
         */
        if ( biomaterials.size() > 0 ) {
            Collection<ExpressionExperiment> ees = expressionExperimentService.findByBioMaterials( biomaterials );
            for ( ExpressionExperiment ee : ees ) {
                results.add( new SearchResult( ee, INDIRECT_DB_HIT_PENALTY, "BioMaterial characteristic" ) );
            }
        }

        if ( factorValues.size() > 0 ) {
            Collection<ExpressionExperiment> ees = expressionExperimentService.findByFactorValues( factorValues );
            for ( ExpressionExperiment ee : ees ) {
                if ( log.isDebugEnabled() ) log.debug( ee );
                results.add( new SearchResult( ee, INDIRECT_DB_HIT_PENALTY, "Factor characteristic" ) );
            }
        }

        if ( treatments.size() > 0 ) {
            log.info( "Not processing treatments, but hits were found" );
            // Collection<ExpressionExperiment> ees = expressionExperimentService.findByTreatments( treatments );
            // for ( ExpressionExperiment ee : ees ) {
            // if ( !results.contains( ee ) ) {
            // results.add( new SearchResult( ee, INDIRECT_DB_HIT_PENALTY, "Treatment" ) );
            // }
            // }
        }

        if ( log.isDebugEnabled() ) {
            log.debug( "ExpressionExperiment search: " + settings + " -> " + results.size() + " characteristic hits" );
        }

        if ( watch.getTime() > 1000 ) {
            log.info( "Retrieving " + results.size() + " experiments from " + characterSearchResults.size()
                    + " retrieved characteristics took " + watch.getTime() + " ms" );
            log.info( "Breakdown: " + numEEs + " via direct association with EE; " + biomaterials.size()
                    + " via association with Biomaterial; " + factorValues.size() + " via experimental design" );
        }

        return results;
    }

    /**
     * Search for the query in ontologies, including items that are associated with children of matching query terms.
     * That is, 'brain' should return entities tagged as 'hippocampus'.
     * 
     * @param classes Classes of characteristic-bound entities. For example, to get matching characteristics of
     *        ExpressionExperiments, pass ExpressionExperiments.class in this collection parameter.
     * @param settings
     * @return SearchResults of CharcteristicObjects. Typically to be useful one needs to retrieve the 'parents'
     *         (entities which have been 'tagged' with the term) of those Characteristics
     */
    private Collection<SearchResult> characteristicSearchWithChildren( Collection<Class<?>> classes,
            SearchSettings settings ) {

        String query = settings.getQuery();

        Set<String> rawTerms = extractTerms( query );

        Collection<SearchResult> allResults = new HashSet<SearchResult>();
        Map<SearchResult, String> matchMap = new HashMap<SearchResult, String>();

        for ( String rawTerm : rawTerms ) {
            if ( StringUtils.isBlank( rawTerm ) ) {
                continue;
            }
            log.info( "Ontology search term:" + rawTerm );
            allResults.addAll( characteristicSearchWord( classes, matchMap, rawTerm ) );
        }

        return postProcessCharacteristicResults( query, allResults, matchMap );

    }

    /**
     * @param classes
     * @param matches
     * @param query
     * @return
     */
    private Collection<SearchResult> characteristicSearchWord( Collection<Class<?>> classes,
            Map<SearchResult, String> matches, String query ) {

        StopWatch watch = startTiming();
        Collection<String> characteristicUris = new HashSet<String>();

        Collection<OntologyIndividual> individuals = ontologyService.findIndividuals( query );
        if ( individuals.size() > 0 && watch.getTime() > 1000 ) {
            log.info( "Found " + individuals.size() + " individuals matching '" + query + "' in " + watch.getTime()
                    + "ms" );
        }
        watch.reset();
        watch.start();

        for ( OntologyIndividual term : individuals ) {
            if ( ( term != null ) && ( term.getUri() != null ) ) characteristicUris.add( term.getUri() );
        }

        Collection<OntologyTerm> matchingTerms = ontologyService.findTerms( query );

        if ( watch.getTime() > 1000 ) {
            log.info( "Found " + matchingTerms.size() + " ontology classes matching '" + query + "' in "
                    + watch.getTime() + "ms" );
        }

        watch.reset();
        watch.start();

        for ( OntologyTerm term : matchingTerms ) {
            String uri = term.getUri();
            if ( uri == null || uri.isEmpty() ) continue;
            characteristicUris.add( uri );
            addChildTerms( characteristicUris, term );
        }

        // int cacheHits = childTermCache.getStatistics().getCacheHits();
        // if ( log.isDebugEnabled() ) log.debug( cacheHits + " cache hits for ontology children" );

        if ( watch.getTime() > 1000 ) {
            log.info( "Found " + characteristicUris.size() + " possible matches + child terms in " + watch.getTime()
                    + "ms" );
        }
        watch.reset();
        watch.start();

        /*
         * Find occurrences of these terms in our system. This is fast, so long as there aren't too many.
         */
        Collection<SearchResult> matchingCharacteristics = dbHitsToSearchResult( characteristicService
                .findByUri( characteristicUris ) );

        Collection<Characteristic> cs = new HashSet<Characteristic>();
        for ( SearchResult crs : matchingCharacteristics ) {
            cs.add( ( Characteristic ) crs.getResultObject() );
        }

        /*
         * Add characteristics that have values matching the query; this pulls in items not associated with ontology
         * terms (free text). We do this here so we can apply the query logic to the matches.
         */
        String dbQueryString = query.replaceAll( "\\*", "" );
        Collection<Characteristic> valueMatches = characteristicService.findByValue( dbQueryString + "%" );

        if ( valueMatches != null && !valueMatches.isEmpty() ) cs.addAll( valueMatches );

        /*
         * Retrieve the owner objects
         */
        Collection<SearchResult> matchingEntities = getAnnotatedEntities( classes, cs );

        if ( watch.getTime() > 1000 ) {
            log.info( "Slow search: found " + matchingEntities.size() + " matches to characteristics for '" + query
                    + "' from " + characteristicUris.size() + " URIS in " + watch.getTime() + "ms" );
        }

        watch.stop();

        for ( SearchResult searchR : matchingEntities ) {
            if ( !matches.containsKey( searchR ) ) {
                matches.put( searchR, query );
            } else {
                matches.put( searchR, matches.get( searchR ) + " " + query );
            }
        }
        return matchingCharacteristics;
    }

    /**
     * A Compass search on array designs.
     * 
     * @param query
     * @return {@link Collection}
     */
    private Collection<SearchResult> compassArrayDesignSearch( SearchSettings settings ) {
        return compassSearch( compassArray, settings );
    }

    /**
     * @param query
     * @return
     */
    private Collection<SearchResult> compassBibliographicReferenceSearch( SearchSettings settings ) {
        return compassSearch( compassBibliographic, settings );
    }

    /**
     * A compass backed search that finds biosequences that match the search string. Searches the gene and probe indexes
     * for matches then converts those results to biosequences
     * 
     * @param searchString
     * @param previousGeneSearchResults Can be null, otherwise used to avoid a second search for genes. The biosequences
     *        for the genes are added to the final results.
     * @return
     * @throws Exception
     */
    private Collection<SearchResult> compassBioSequenceSearch( SearchSettings settings,
            Collection<SearchResult> previousGeneSearchResults ) {

        Collection<SearchResult> results = compassSearch( compassBiosequence, settings );

        Collection<SearchResult> geneResults = null;
        if ( previousGeneSearchResults == null ) {
            log.info( "Biosequence Search:  running gene search with " + settings.getQuery() );
            geneResults = compassGeneSearch( settings );
        } else {
            log.info( "Biosequence Search:  using previous results" );
            geneResults = previousGeneSearchResults;
        }

        Map<Gene, SearchResult> genes = new HashMap<Gene, SearchResult>();
        for ( SearchResult sr : geneResults ) {
            Object resultObject = sr.getResultObject();
            if ( Gene.class.isAssignableFrom( resultObject.getClass() ) ) {
                genes.put( ( Gene ) resultObject, sr );
            } else {
                log.warn( "Expected a Gene, got a " + resultObject.getClass() + "" );
            }
        }

        Map<Gene, Collection<BioSequence>> seqsFromDb = bioSequenceService.findByGenes( genes.keySet() );
        for ( Gene gene : seqsFromDb.keySet() ) {
            List<BioSequence> bs = new ArrayList<BioSequence>( seqsFromDb.get( gene ) );
            bioSequenceService.thaw( bs );
            results.addAll( dbHitsToSearchResult( bs, genes.get( gene ) ) );
        }

        return results;
    }

    /**
     * @param settings
     * @return
     */
    private Collection<SearchResult> compassCompositeSequenceSearch( final SearchSettings settings ) {
        return compassSearch( compassProbe, settings );
    }

    /**
     * A compass search on expressionExperiments.
     * 
     * @param query
     * @return {@link Collection}
     */
    private Collection<SearchResult> compassExpressionSearch( SearchSettings settings ) {
        return compassSearch( compassExpression, settings );
    }

    /**
     * @param query
     * @return
     */
    private Collection<SearchResult> compassGeneSearch( final SearchSettings settings ) {
        return compassSearch( compassGene, settings );
    }

    /**
     * @param bean
     * @param settings
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<SearchResult> compassSearch( Compass bean, final SearchSettings settings ) {

        if ( !settings.isUseIndices() ) return new HashSet<SearchResult>();

        CompassTemplate template = new CompassTemplate( bean );
        Collection<SearchResult> searchResults = ( Collection<SearchResult> ) template.execute( new CompassCallback() {
            public Object doInCompass( CompassSession session ) throws CompassException {
                return performSearch( settings, session );
            }
        } );
        if ( log.isDebugEnabled() ) {
            log.debug( "Compass search via " + bean.getSettings().getSetting( "compass.name" ) + " : " + settings
                    + " -> " + searchResults.size() + " hits" );
        }
        return searchResults;
    }

    /**
     * Search by name of the composite sequence as well as gene.
     * 
     * @return
     * @throws Exception
     */
    private Collection<SearchResult> compositeSequenceSearch( SearchSettings settings ) {

        StopWatch watch = startTiming();

        /*
         * FIXME: this at least partly ignores any array design that was set as a restriction, especially in a gene
         * search.
         */

        Collection<SearchResult> allResults = new HashSet<SearchResult>();
        // Temporaily removing compass searching of composite sequences because it only bloats the results.
        // allResults.addAll( compassCompositeSequenceSearch( settings ) );
        allResults.addAll( databaseCompositeSequenceSearch( settings ) );
        // allResults.addAll( compositeSequenceByGeneSearch( settings, geneSearchResults ) );

        /*
         * This last step is needed because the compassSearch for compositeSequences returns bioSequences too.
         */
        Collection<SearchResult> finalResults = new HashSet<SearchResult>();
        for ( SearchResult sr : allResults ) {
            if ( CompositeSequence.class.isAssignableFrom( sr.getResultClass() ) ) {
                finalResults.add( sr );
            }
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            log.info( "Composite sequence search for '" + settings + "' took " + watch.getTime() + " ms, "
                    + finalResults.size() + " results." );
        return finalResults;
    }

    /**
     * Turn a string into a Lucene-indexable document.
     * 
     * @param content
     * @return
     */
    private Document createDocument( String content ) {
        Document doc = new Document();
        Field f = new Field( INDEX_KEY, content, Field.Store.YES, Field.Index.ANALYZED );
        doc.add( f );
        return doc;
    }

    /**
     * Searches the DB for array designs which have composite sequences whose names match the given search string.
     * Because of the underlying database search, this is acl aware. That is, returned array designs are filtered based
     * on access control list (ACL) permissions.
     * 
     * @param searchString
     * @return
     * @throws Exception
     */
    private Collection<SearchResult> databaseArrayDesignSearch( SearchSettings settings ) {

        if ( !settings.isUseDatabase() ) return new HashSet<SearchResult>();

        StopWatch watch = startTiming();

        Collection<ArrayDesign> adSet = new HashSet<ArrayDesign>();

        // search by exact composite sequence name
        Collection<CompositeSequence> matchedCs = compositeSequenceService.findByName( settings.getQuery() );
        for ( CompositeSequence sequence : matchedCs ) {
            adSet.add( sequence.getArrayDesign() );
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            log.info( "Array Design Compositesequence DB search for " + settings + " took " + watch.getTime() + " ms"
                    + " found " + adSet.size() + " Ads" );

        return dbHitsToSearchResult( adSet );

    }

    /**
     * A database serach for biosequences. Biosequence names are already indexed by compass...
     * 
     * @param searchString
     * @return
     */
    private Collection<SearchResult> databaseBioSequenceSearch( SearchSettings settings ) {

        if ( !settings.isUseDatabase() ) return new HashSet<SearchResult>();

        StopWatch watch = startTiming();

        String searchString = settings.getQuery();

        // replace * with % for inexact symbol search
        String inexactString = searchString;
        Pattern pattern = Pattern.compile( "\\*" );
        Matcher match = pattern.matcher( inexactString );
        inexactString = match.replaceAll( "%" );

        Collection<BioSequence> bs = bioSequenceService.findByName( inexactString );
        bioSequenceService.thaw( bs );
        Collection<SearchResult> bioSequenceList = new HashSet<SearchResult>( dbHitsToSearchResult( bs ) );

        watch.stop();
        if ( watch.getTime() > 1000 )
            log.info( "BioSequence DB search for " + searchString + " took " + watch.getTime() + " ms and found"
                    + bioSequenceList.size() + " BioSequences" );

        return bioSequenceList;
    }

    /**
     * Takes a list of ontology terms, and classes of objects of interest to be returned. Looks through the
     * characteristic table for an exact match with the given ontology terms. Only tries to match the uri's.
     * 
     * @param clazz Class of objects to restrict the search to (typically ExpressionExperiment.class, for example).
     * @param terms A list of ontololgy terms to search for
     * @return Collection of search results for the objects owning the found characteristics, where the owner is of
     *         class clazz
     */
    @SuppressWarnings("unchecked")
    private Collection<SearchResult> databaseCharacteristicExactUriSearchForOwners( Collection<Class<?>> classes,
            Collection<OntologyTerm> terms ) {

        // Collection<Characteristic> characteristicValueMatches = new ArrayList<Characteristic>();
        Collection<Characteristic> characteristicURIMatches = new ArrayList<Characteristic>();

        for ( OntologyTerm term : terms ) {
            // characteristicValueMatches.addAll( characteristicService.findByValue( term.getUri() ));
            characteristicURIMatches.addAll( characteristicService.findByUri( term.getUri() ) );
        }

        Map parentMap = characteristicService.getParents( characteristicURIMatches );
        // parentMap.putAll( characteristicService.getParents(characteristicValueMatches ) );

        return filterCharacteristicOwnersByClass( classes, parentMap );
    }

    /**
     * Search the DB for composite sequences and the genes that are matched to them.
     * 
     * @param searchString
     * @return
     * @throws Exception
     */
    private Collection<SearchResult> databaseCompositeSequenceSearch( final SearchSettings settings ) {

        if ( !settings.isUseDatabase() ) return new HashSet<SearchResult>();

        StopWatch watch = startTiming();

        Set<Gene> geneSet = new HashSet<Gene>();

        String searchString = settings.getQuery();
        ArrayDesign ad = settings.getArrayDesign();

        // search by exact composite sequence name
        Collection<CompositeSequence> matchedCs = new HashSet<CompositeSequence>();
        if ( ad != null ) {
            CompositeSequence cs = compositeSequenceService.findByName( ad, searchString );
            matchedCs.add( cs );
        } else {
            matchedCs = compositeSequenceService.findByName( searchString );
        }

        /*
         * In case the query _is_ a gene
         */
        Collection<SearchResult> rawGeneResults = this.databaseGeneSearch( settings );
        for ( SearchResult searchResult : rawGeneResults ) {
            Object j = searchResult.getResultObject();
            if ( Gene.class.isAssignableFrom( j.getClass() ) ) {
                geneSet.add( ( Gene ) j );
            }
        }

        for ( Gene g : geneSet ) {
            if ( settings.getArrayDesign() != null ) {
                matchedCs.addAll( compositeSequenceService.findByGene( g, settings.getArrayDesign() ) );
            } else {
                matchedCs.addAll( compositeSequenceService.findByGene( g ) );
            }
        }

        // search by associated genes.
        for ( CompositeSequence sequence : matchedCs ) {
            geneSet.addAll( compositeSequenceService.getGenes( sequence ) );
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            log.info( "Gene composite sequence DB search " + searchString + " took " + watch.getTime() + " ms, "
                    + geneSet.size() + " items." );

        Collection<SearchResult> results = dbHitsToSearchResult( geneSet );

        results.addAll( dbHitsToSearchResult( matchedCs ) );

        return results;
    }

    /**
     * Does search on exact string by: id, name and short name. This only returns results if these fields match exactly,
     * but it's fast.
     * 
     * @param query
     * @return {@link Collection}
     */
    private Collection<SearchResult> databaseExpressionExperimentSearch( final SearchSettings settings ) {

        if ( !settings.isUseDatabase() ) return new HashSet<SearchResult>();

        StopWatch watch = startTiming();

        Map<ExpressionExperiment, String> results = new HashMap<ExpressionExperiment, String>();
        ExpressionExperiment ee = expressionExperimentService.findByName( settings.getQuery() );
        if ( ee != null ) {
            results.put( ee, ee.getName() );
        } else {
            ee = expressionExperimentService.findByShortName( settings.getQuery() );
            if ( ee != null ) {
                results.put( ee, ee.getShortName() );
            } else {
                try {
                    // maybe user put in a primary key value.
                    ee = expressionExperimentService.load( new Long( settings.getQuery() ) );
                    if ( ee != null ) results.put( ee, ee.getId().toString() );
                } catch ( NumberFormatException e ) {
                    // no-op - it's not an ID.
                }
            }
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            log.info( "DB Expression Experiment search for " + settings + " took " + watch.getTime() + " ms and found "
                    + results.size() + " EEs" );

        Collection<SearchResult> r = dbHitsToSearchResult( results );
        return r;
    }

    /**
     * Search the DB for genes that exactly match the given search string searches geneProducts, gene and bioSequence
     * tables
     * 
     * @param searchString
     * @return
     * @throws Exception
     */
    private Collection<SearchResult> databaseGeneSearch( SearchSettings settings ) {

        if ( !settings.isUseDatabase() ) return new HashSet<SearchResult>();

        StopWatch watch = startTiming();
        String searchString = settings.getQuery();
        if ( StringUtils.isBlank( searchString ) ) return new HashSet<SearchResult>();

        Collection<SearchResult> results = new HashSet<SearchResult>();

        /*
         * First search by accession. If we find it, stop.
         */
        Gene result = geneService.findByNCBIId( searchString );
        if ( result != null ) {
            results.add( this.dbHitToSearchResult( null, result ) );
        } else {
            result = geneService.findByAccession( searchString, null );
            if ( result != null ) {
                results.add( this.dbHitToSearchResult( null, result ) );
            }
        }
        if ( results.size() > 0 ) {
            filterByTaxon( settings, results );
            watch.stop();
            if ( watch.getTime() > 1000 )
                log.info( "Gene DB search for " + searchString + " took " + watch.getTime() + " ms and found "
                        + results.size() + " genes" );
            return results;
        }

        // replace * at end with % for inexact symbol search
        String inexactString = searchString;
        Pattern pattern = Pattern.compile( "\\*$" );
        Matcher match = pattern.matcher( inexactString );
        inexactString = match.replaceAll( "%" );
        // note that at this point, the inexactString might not have a wildcard - only if the user asked for it.

        String exactString = inexactString.replaceAll( "%", "" );

        // if the query is shortish, always do a wild card search. This gives better behavior in 'live
        // search' situations. If we do wildcards on very short queries we get too many results.
        Collection<Gene> geneSet = new HashSet<Gene>();
        if ( searchString.length() <= 2 ) {
            // case 0: user entered a very short string. We search only for exact matches.
            geneSet.addAll( geneService.findByOfficialSymbolInexact( exactString ) );
        } else if ( searchString.length() > 2 && inexactString.endsWith( "%" ) ) {
            // case 1: user asked for wildcard. We allow this on strings of length 3 or more.
            geneSet.addAll( geneService.findByOfficialSymbolInexact( inexactString ) );
        } else if ( searchString.length() > 3 && searchString.length() < 6 ) {
            // case 2: user did not ask for a wildcard, but we add it anyway, if the string is 4 or 5 characters.
            if ( !inexactString.endsWith( "%" ) ) {
                inexactString = inexactString + "%";
            }
            geneSet.addAll( geneService.findByOfficialSymbolInexact( inexactString ) );
        } else {
            // case 3: string is long enough, and user did not ask for wildcard.
            geneSet.addAll( geneService.findByOfficialSymbol( exactString ) );
        }

        /*
         * TODO The rest we are doing by exact matches only. For aliases it would probably be good to do it like
         * official symbols. For the other searches I think exact matches are the only thing that makes sense.
         */

        /*
         * If we found a match using official symbol, don't bother with this
         */
        if ( geneSet.isEmpty() ) {
            geneSet.addAll( geneService.findByAlias( exactString ) );
            geneSet.addAll( geneProductService.getGenesByName( exactString ) );
            geneSet.addAll( geneProductService.getGenesByNcbiId( exactString ) );
            geneSet.addAll( bioSequenceService.getGenesByAccession( exactString ) );
            geneSet.addAll( bioSequenceService.getGenesByName( exactString ) );
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            log.info( "Gene DB search for " + searchString + " took " + watch.getTime() + " ms and found "
                    + geneSet.size() + " genes" );

        results = dbHitsToSearchResult( geneSet );
        filterByTaxon( settings, results );
        return results;
    }

    /**
     * Convert hits from database searches into SearchResults.
     * 
     * @param entities
     * @return
     */
    private Collection<SearchResult> dbHitsToSearchResult( Collection<? extends Object> entities ) {
        return this.dbHitsToSearchResult( entities, null );
    }

    /**
     * Convert hits from database searches into SearchResults.
     * 
     * @param entities
     * @param compassHitDerivedFrom SearchResult that these entities were derived from. For example, if you
     *        compass-searched for genes, and then used the genes to get sequences from the database, the gene is
     *        compassHitsDerivedFrom. If null, we treat this as a direct hit.
     * @return
     */
    private List<SearchResult> dbHitsToSearchResult( Collection<? extends Object> entities,
            SearchResult compassHitDerivedFrom ) {
        List<SearchResult> results = new ArrayList<SearchResult>();
        for ( Object e : entities ) {
            if ( e == null ) {
                log.warn( "Null search result object" );
                continue;
            }
            SearchResult esr = dbHitToSearchResult( compassHitDerivedFrom, e );
            results.add( esr );
        }
        return results;
    }

    /**
     * Convert hits from database searches into SearchResults.
     * 
     * @param entities
     * @return
     */
    private Collection<SearchResult> dbHitsToSearchResult( Map<? extends Object, String> entities ) {
        return this.dbHitsToSearchResult( entities, null );
    }

    /**
     * Convert hits from database searches into SearchResults.
     * 
     * @param entities
     * @param compassHitDerivedFrom SearchResult that these entities were derived from. For example, if you
     *        compass-searched for genes, and then used the genes to get sequences from the database, the gene is
     *        compassHitsDerivedFrom. If null, we treat this as a direct hit.
     * @return
     */
    private List<SearchResult> dbHitsToSearchResult( Map<? extends Object, String> entities,
            SearchResult compassHitDerivedFrom ) {
        List<SearchResult> results = new ArrayList<SearchResult>();
        for ( Object e : entities.keySet() ) {
            SearchResult esr = dbHitToSearchResult( compassHitDerivedFrom, e, entities.get( e ) );
            results.add( esr );
        }
        return results;
    }

    /**
     * @param compassHitDerivedFrom
     * @param e
     * @return
     */
    private SearchResult dbHitToSearchResult( SearchResult compassHitDerivedFrom, Object e ) {
        return this.dbHitToSearchResult( compassHitDerivedFrom, e, null );
    }

    /**
     * @param compassHitDerivedFrom
     * @param e
     * @return
     */
    private SearchResult dbHitToSearchResult( SearchResult compassHitDerivedFrom, Object e, String text ) {
        SearchResult esr = null;
        if ( compassHitDerivedFrom != null && text == null ) {
            esr = new SearchResult( e, compassHitDerivedFrom.getScore() * INDIRECT_DB_HIT_PENALTY );
            esr.setHighlightedText( compassHitDerivedFrom.getHighlightedText() );
        } else {
            // log.info( e + " " + text );
            esr = new SearchResult( e, 1.0, text );
        }
        return esr;
    }

    /**
     * @param parentMap
     */
    private void debugParentFetch( Map<Characteristic, Object> parentMap ) {
        /*
         * This is purely debugging.
         */
        if ( parentMap.size() > 0 ) {
            // if ( log.isDebugEnabled() ) log.debug( "Found " + parentMap.size() + " owners for
            // characteristics:"
            // );
            for ( Object obj : parentMap.values() ) {
                if ( obj instanceof Auditable ) {
                    if ( log.isDebugEnabled() ) {
                        // log.debug( " Owner Id: " + ( ( Auditable ) obj ).getId() + " Owner Class: " +
                        // obj.getClass()
                        // );
                    }
                } else {
                    if ( log.isDebugEnabled() ) {
                        // log.debug( " Owner : " + obj.toString() + " Owner Class: " + obj.getClass() );
                    }
                }
            }
        }
    }

    /**
     * Deals with the case where the user queried something like "hypothalamus AND sex" (without the quotes).
     * 
     * @param matches
     * @param parsedQuery
     * @return
     */
    private Collection<SearchResult> doCharacteristicSearchWithLogic( Map<SearchResult, String> matches,
            Query parsedQuery ) {
        Collection<SearchResult> results = new HashSet<SearchResult>();
        try {

            Map<String, Collection<SearchResult>> invertedMatches = new HashMap<String, Collection<SearchResult>>();
            Directory idx = indexCharacteristicHits( matches, invertedMatches );
            IndexSearcher searcher = new IndexSearcher( idx );
            TopDocCollector hc = new TopDocCollector( 1000 );
            searcher.search( parsedQuery, hc );

            TopDocs topDocs = hc.topDocs();

            int hitcount = topDocs.totalHits;
            log.info( "Hits: " + hitcount );

            /*
             * If we got hits, it means that some of our results match... so we have to retrive the objects.
             */

            for ( int i = 0; i < hitcount; i++ ) {

                ScoreDoc scoreDoc = topDocs.scoreDocs[i];

                Document doc = searcher.doc( scoreDoc.doc );

                String match = doc.getField( INDEX_KEY ).stringValue();
                Collection<SearchResult> resultsMatching = invertedMatches.get( match );
                if ( resultsMatching != null ) {
                    log.debug( "All matches to '" + match + "': " + resultsMatching.size() );
                    for ( SearchResult searchResult : resultsMatching ) {
                        results.add( searchResult );
                    }
                }
            }

        } catch ( CorruptIndexException e ) {
            throw new RuntimeException( e );
        } catch ( LockObtainFailedException e ) {
            throw new RuntimeException( e );
        } catch ( IOException e ) {
            throw new RuntimeException( e );

        }
        return results;
    }

    /**
     * A general search for expression experiments. This search does both an database search and a compass search.
     * 
     * @param settings
     * @return {@link Collection}
     */
    private Collection<SearchResult> expressionExperimentSearch( final SearchSettings settings ) {
        StopWatch watch = startTiming();

        Collection<SearchResult> results = new HashSet<SearchResult>();

        if ( settings.isUseDatabase() ) {
            results.addAll( databaseExpressionExperimentSearch( settings ) );
        }

        if ( results.size() == 0 ) {
            /*
             * User didn't put in an exact id, so they get a slower more thorough search.
             */

            if ( settings.isUseIndices() ) {
                results.addAll( compassExpressionSearch( settings ) );
            }

            if ( settings.isUseCharacteristics() ) {
                results.addAll( characteristicExpressionExperimentSearch( settings ) );
            }
        }

        /*
         * Find data sets that match the platform -- TODO make this do something intelligent with GPL570 + brain.
         */
        Collection<SearchResult> matchingPlatforms = arrayDesignSearch( settings, null );
        for ( SearchResult adRes : matchingPlatforms ) {
            if ( adRes.getResultObject() instanceof ArrayDesign ) {
                ArrayDesign ad = ( ArrayDesign ) adRes.getResultObject();
                Collection<ExpressionExperiment> expressionExperiments = this.arrayDesignService
                        .getExpressionExperiments( ad );
                if ( expressionExperiments.size() > 0 )
                    results.addAll( dbHitsToSearchResult( expressionExperiments ) );
            }
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            log.info( "Expression Experiment search for '" + settings + "' took " + watch.getTime() + " ms, "
                    + results.size() + " hits." );

        return results;
    }

    /**
     * @param query
     * @return
     */
    private Set<String> extractTerms( String query ) {
        Query lquer = this.makeLuceneQuery( query );

        Set<String> rawTerms = new HashSet<String>();
        if ( lquer instanceof BooleanQuery ) {
            BooleanClause[] clauses = ( ( BooleanQuery ) lquer ).getClauses();
            for ( BooleanClause booleanClause : clauses ) {
                rawTerms.add( booleanClause.toString().replaceAll( "^[\\+-]", "" ) );
            }
        } else if ( lquer instanceof PhraseQuery ) {
            rawTerms.add( ( ( PhraseQuery ) lquer ).toString().replaceAll( "\"", "" ) );
        } else if ( lquer instanceof PrefixQuery ) {
            rawTerms.add( ( ( PrefixQuery ) lquer ).getPrefix().field() );
        } else {
            rawTerms.add( query );
        }
        return rawTerms;
    }

    /**
     * This can only be used on SearchResults where the result object has a "getTaxon" method.
     * 
     * @param settings
     * @param geneSet
     */
    private void filterByTaxon( SearchSettings settings, Collection<SearchResult> results ) {
        if ( settings.getTaxon() == null ) {
            return;
        }
        Collection<SearchResult> toRemove = new HashSet<SearchResult>();
        Taxon t = settings.getTaxon();
        for ( SearchResult sr : results ) {

            Object o = sr.getResultObject();
            try {
                Method m = o.getClass().getMethod( "getTaxon", new Class[] {} );
                Taxon currentTaxon = ( Taxon ) m.invoke( o, new Object[] {} );
                if ( !currentTaxon.equals( t ) ) {
                    toRemove.add( sr );
                }
            } catch ( SecurityException e ) {
                throw new RuntimeException( e );
            } catch ( NoSuchMethodException e ) {
                /*
                 * In case of a programming error where the results don't have a taxon at all, we assume we should
                 * filter them out but issue a warning.
                 */
                toRemove.add( sr );
                log.warn( "No getTaxon method for: " + o.getClass() + ".  Filtering from results. Error was: " + e );
            } catch ( IllegalArgumentException e ) {
                throw new RuntimeException( e );
            } catch ( IllegalAccessException e ) {
                throw new RuntimeException( e );
            } catch ( InvocationTargetException e ) {
                throw new RuntimeException( e );
            }
        }
        results.removeAll( toRemove );
    }

    /**
     * @param clazz
     * @param characteristic2entity
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<SearchResult> filterCharacteristicOwnersByClass( Collection<Class<?>> classes,
            Map<Characteristic, Object> characteristic2entity ) {
        Collection<SearchResult> results = new HashSet<SearchResult>();
        for ( Characteristic c : characteristic2entity.keySet() ) {
            Object o = characteristic2entity.get( c );
            for ( Class clazz : classes ) {
                if ( clazz.isAssignableFrom( o.getClass() ) ) {
                    String matchedText = c.getValue();
                    if ( c instanceof VocabCharacteristic && ( ( VocabCharacteristic ) c ).getValueUri() != null ) {
                        matchedText = "Ontology term: " + matchedText;
                    }
                    results.add( new SearchResult( o, 1.0, matchedText ) );
                }
            }
        }
        return results;
    }

    /**
     * Combines compass style search, the db style search, and the compositeSequence search and returns 1 combined list
     * with no duplicates.
     * 
     * @param searchString
     * @return
     * @throws Exception
     */
    private Collection<SearchResult> geneSearch( final SearchSettings settings ) {

        StopWatch watch = startTiming();

        String searchString = settings.getQuery();

        Collection<SearchResult> geneDbList = databaseGeneSearch( settings );
        Set<SearchResult> combinedGeneList = new HashSet<SearchResult>();
        combinedGeneList.addAll( geneDbList );

        Collection<SearchResult> geneCompassList = compassGeneSearch( settings );
        combinedGeneList.addAll( geneCompassList );

        if ( combinedGeneList.size() == 0 ) {
            Collection<SearchResult> geneCsList = databaseCompositeSequenceSearch( settings );
            for ( SearchResult res : geneCsList ) {
                if ( res.getResultClass().isAssignableFrom( Gene.class ) ) combinedGeneList.add( res );
            }
        }

        filterByTaxon( settings, combinedGeneList );

        if ( watch.getTime() > 1000 )
            log.info( "Gene search for " + searchString + " took " + watch.getTime() + " ms; "
                    + combinedGeneList.size() + " results." );
        return combinedGeneList;
    }

    /**
     * Given classes to search and characteristics,
     * 
     * @param classes Which classes of entities to look for
     * @param cs
     * @return
     */
    private Collection<SearchResult> getAnnotatedEntities( Collection<Class<?>> classes, Collection<Characteristic> cs ) {

        Map<Characteristic, Object> characterstic2entity = characteristicService.getParents( cs );
        Collection<SearchResult> matchedEntities = filterCharacteristicOwnersByClass( classes, characterstic2entity );

        if ( log.isDebugEnabled() ) {
            debugParentFetch( characterstic2entity );
        }
        return matchedEntities;
    }

    /**
     * @param hits
     * @param i
     */
    private String getHighlightedText( CompassHits hits, int i ) {

        CompassHighlighter highlighter = hits.highlighter( i );

        for ( String p : propertiesToSearch ) {
            try {
                String text = highlighter.fragmentsWithSeparator( p );
                if ( text != null && StringUtils.isNotBlank( text ) ) {
                    return text + " (" + p + ")"; // note we don't actually use this.
                }
            } catch ( SearchEngineException e ) {
                // no big deal - we asked for a property it doesn't have. Must be a
                // better way...
                // log.debug( e );
            } catch ( IllegalArgumentException e ) {
                // log.debug( e ); // can be useful for debugging properties searched.
                // again, the property isn't in the compass bean, ignore.
            }
        }

        // Explanation exp = LuceneHelper.getLuceneSearchEngineHits( hits ).explain( i );
        // log.info( hits.data( i ) + " " + exp.toString() );
        return null;
    }

    /**
     * @param searchResults
     * @return List of ids for the entities held by the search results.
     */
    private List<Long> getIds( List<SearchResult> searchResults ) {
        List<Long> list = new ArrayList<Long>();
        for ( SearchResult ee : searchResults ) {
            list.add( ee.getId() );
        }
        return list;
    }

    /**
     * @param hits
     * @return
     */
    private Collection<SearchResult> getSearchResults( CompassHits hits ) {
        Collection<SearchResult> results = new HashSet<SearchResult>();
        for ( int i = 0, len = hits.getLength(); i < len; i++ ) {

            SearchResult r = new SearchResult( hits.data( i ) );
            /*
             * Always give compass hits a lower score so they can be differentiated from exact database hits.
             */
            r.setScore( new Double( hits.score( i ) * COMPASS_HIT_SCORE_PENALTY_FACTOR ) );

            CompassHighlightedText highlightedText = hits.highlightedText( i );
            if ( highlightedText != null && highlightedText.getHighlightedText() != null ) {
                r.setHighlightedText( "... " + highlightedText.getHighlightedText() + " ..." );
            } else {
                log.debug( "No highlighted text for " + r );
            }
            results.add( r );
            i++;
        }
        return results;
    }

    /**
     * @param settings
     * @param results
     * @param rawResults
     * @param fillObjects
     */
    private Map<Class<?>, List<SearchResult>> getSortedLimitedResults( SearchSettings settings,
            List<SearchResult> rawResults, boolean fillObjects ) {

        Map<Class<?>, List<SearchResult>> results = new HashMap<Class<?>, List<SearchResult>>();
        Collections.sort( rawResults );

        results.put( ArrayDesign.class, new ArrayList<SearchResult>() );
        results.put( BioSequence.class, new ArrayList<SearchResult>() );
        results.put( BibliographicReference.class, new ArrayList<SearchResult>() );
        results.put( CompositeSequence.class, new ArrayList<SearchResult>() );
        results.put( ExpressionExperiment.class, new ArrayList<SearchResult>() );
        results.put( Gene.class, new ArrayList<SearchResult>() );
        results.put( PredictedGene.class, new ArrayList<SearchResult>() );
        results.put( ProbeAlignedRegion.class, new ArrayList<SearchResult>() );

        /*
         * Get the top N results, overall (NOT within each class - experimental.)
         */
        for ( int i = 0, limit = Math.min( rawResults.size(), settings.getMaxResults() ); i < limit; i++ ) {
            SearchResult sr = rawResults.get( i );

            /*
             * FIXME This is unpleasant and should be removed when BioSequences are correctly detached.
             */
            Class<? extends Object> resultClass = EntityUtils.getImplementationForProxy( sr.getResultObject() )
                    .getClass();

            resultClass = ReflectionUtil.getBaseForImpl( resultClass );

            assert results.containsKey( resultClass ) : "Unknown class " + resultClass;
            results.get( resultClass ).add( sr );
        }

        if ( fillObjects ) {
            /**
             * Now retrieve the entities and put them in the SearchResult. Entities that are filtered out by the
             * SecurityInterceptor will be removed at this stage.
             */
            for ( Class<? extends Object> clazz : results.keySet() ) {
                List<SearchResult> r = results.get( clazz );
                if ( r.size() == 0 ) continue;
                Map<Long, SearchResult> rMap = new HashMap<Long, SearchResult>();
                for ( SearchResult searchResult : r ) {
                    if ( !rMap.containsKey( searchResult.getId() )
                            || ( rMap.get( searchResult.getId() ).getScore() < searchResult.getScore() ) ) {
                        rMap.put( searchResult.getId(), searchResult );
                    }
                }

                Collection<? extends Object> entities = retrieveResultEntities( clazz, r );
                List<SearchResult> filteredResults = new ArrayList<SearchResult>();
                for ( Object entity : entities ) {
                    Long id = EntityUtils.getId( entity );
                    SearchResult keeper = rMap.get( id );
                    keeper.setResultObject( entity );
                    filteredResults.add( keeper );
                }
                results.put( clazz, filteredResults );

            }
        } else {
            for ( SearchResult sr : rawResults ) {
                sr.setResultObject( null );
            }
        }

        return results;
    }

    /**
     * @param matches
     * @param invertedMatches
     * @return
     * @throws CorruptIndexException
     * @throws LockObtainFailedException
     * @throws IOException
     */
    private RAMDirectory indexCharacteristicHits( Map<SearchResult, String> matches,
            Map<String, Collection<SearchResult>> invertedMatches ) throws CorruptIndexException,
            LockObtainFailedException, IOException {
        /*
         * make in in-memory index. See http://javatechniques.com/blog/lucene-in-memory-text-search-engine (somewhat out
         * of date); maybe there is an easier way
         */
        RAMDirectory idx = new RAMDirectory();
        IndexWriter writer = new IndexWriter( idx, this.analyzer, true, MaxFieldLength.LIMITED );

        for ( SearchResult o : matches.keySet() ) {
            String text = matches.get( o );
            if ( !invertedMatches.containsKey( text ) ) {
                invertedMatches.put( text, new HashSet<SearchResult>() );
                writer.addDocument( createDocument( text ) );
            }
            invertedMatches.get( text ).add( o );
        }

        writer.close();

        return idx;
    }

    /**
     * Turn query into a Lucene query.
     * 
     * @param query
     * @return
     */
    private Query makeLuceneQuery( String query ) {
        QueryParser parser = new QueryParser( INDEX_KEY, this.analyzer );
        QueryParser.Operator defaultOperator = null;
        String sDefaultOperator = ConfigUtils.getDefaultSearchOperator();
        if ( sDefaultOperator.equalsIgnoreCase( ( "or" ) ) ) {
            defaultOperator = QueryParser.OR_OPERATOR;
        } else if ( sDefaultOperator.equalsIgnoreCase( ( "and" ) ) ) {
            defaultOperator = QueryParser.AND_OPERATOR;
        } else {
            throw new IllegalArgumentException( "Unknown defaultOperator: " + sDefaultOperator
                    + ", only OR and AND are supported" );
        }
        parser.setDefaultOperator( defaultOperator );
        Query parsedQuery;
        try {
            parsedQuery = parser.parse( query );
        } catch ( ParseException e ) {
            throw new RuntimeException( "Cannot parse query: " + e.getMessage() );
        }
        return parsedQuery;
    }

    /**
     * Attempts to find an exact match for the search term in the characteristic table (by value and value URI). If the
     * search term is found then uses that URI to find the parents and returns them as SearchResults.
     * 
     * @param classes
     * @param searchString
     * @return
     */
    private Collection<SearchResult> ontologySearchAnnotatedObject( Collection<Class<?>> classes,
            SearchSettings settings ) {

        /*
         * Direct search.
         */
        Collection<SearchResult> results = new HashSet<SearchResult>();

        /*
         * Include children in ontologies, if any. This can be slow if there are a lot of children.
         */
        Collection<SearchResult> childResults = characteristicSearchWithChildren( classes, settings );

        results.addAll( childResults );

        return results;

    }

    /**
     * If necessary, screen the results for the logic requested by the user. Thus, "sex AND hypothalamus" will return
     * only results that have both terms associated with them.
     * 
     * @param query
     * @param unprocessedResults
     * @param matches Map of SearchResult to the matching String (the characteristic value, basically)
     * @return
     */
    private Collection<SearchResult> postProcessCharacteristicResults( String query,
            Collection<SearchResult> unprocessedResults, Map<SearchResult, String> matches ) {
        Query parsedQuery = makeLuceneQuery( query );
        return doCharacteristicSearchWithLogic( matches, parsedQuery );
    }

    /**
     * Retrieve entities from the persistent store.
     * 
     * @param entityClass
     * @param results
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<? extends Object> retrieveResultEntities( Class entityClass, List<SearchResult> results ) {
        List<Long> ids = getIds( results );
        if ( ExpressionExperiment.class.isAssignableFrom( entityClass ) ) {
            return expressionExperimentService.loadMultiple( ids );
        } else if ( ArrayDesign.class.isAssignableFrom( entityClass ) ) {
            return arrayDesignService.loadMultiple( ids );
        } else if ( CompositeSequence.class.isAssignableFrom( entityClass ) ) {
            return compositeSequenceService.loadMultiple( ids );
        } else if ( BibliographicReference.class.isAssignableFrom( entityClass ) ) {
            return bibliographicReferenceService.loadMultiple( ids );
        } else if ( Gene.class.isAssignableFrom( entityClass ) ) {
            return geneService.loadMultiple( ids );
        } else if ( BioSequence.class.isAssignableFrom( entityClass ) ) {
            return bioSequenceService.loadMultiple( ids );
        } else {
            throw new UnsupportedOperationException( "Don't know how to retrieve objects for class=" + entityClass );
        }
    }

    private StopWatch startTiming() {
        StopWatch watch = new StopWatch();
        watch.start();
        return watch;
    }

}
