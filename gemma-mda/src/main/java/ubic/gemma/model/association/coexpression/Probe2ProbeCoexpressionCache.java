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
package ubic.gemma.model.association.coexpression;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import ubic.gemma.model.genome.CoexpressionCacheValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.ConfigUtils;

/**
 * Configures the cache for data vectors.
 * <p>
 * Implementation note: This uses ehCache. I have decided to make one cache per expression experiment. The reason for
 * this is that having complex keys for cached Elements based on expression experiment AND gene makes it difficult to
 * invalidate the cache when an expression experiment's data changes. The drawback is that there are potentially
 * hundreds of caches; I don't know if there are any performance considerations there.
 * 
 * @author paul
 * @version $Id$
 */
public class Probe2ProbeCoexpressionCache {

    private static final String PROCESSED_DATA_VECTOR_CACHE_NAME_BASE = "Probe2ProbeCache";
    private static final int PROCESSED_DATA_VECTOR_CACHE_DEFAULT_MAX_ELEMENTS = 100000;
    private static final int PROCESSED_DATA_VECTOR_CACHE_DEFAULT_TIME_TO_LIVE = 10000;
    private static final int PROCESSED_DATA_VECTOR_CACHE_DEFAULT_TIME_TO_IDLE = 10000;
    private static final boolean PROCESSED_DATA_VECTOR_CACHE_DEFAULT_ETERNAL = true;
    private static final boolean PROCESSED_DATA_VECTOR_CACHE_DEFAULT_OVERFLOW_TO_DISK = true;

    private CacheManager cacheManager;

    private Boolean enabled = true;

    /**
     * @return the enabled
     */
    public Boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled( Boolean enabled ) {
        this.enabled = enabled;
    }

    /**
     * We retain references to the caches separately from the CacheManager. This _could_ create leaks of caches if the
     * cache manager needs to recreate a cache for some reason. Something to keep in mind.
     */
    private final Map<Long, Cache> caches = new HashMap<Long, Cache>();

    /**
     * @param eeID
     * @param coExVOForCache
     */
    @SuppressWarnings("unchecked")
    public void addToCache( Long eeID, CoexpressionCacheValueObject coExVOForCache ) {

        Cache c = getCache( eeID );

        Gene queryGene = coExVOForCache.getQueryGene();

        Element element = c.get( queryGene );
        if ( element != null ) {
            ( ( Collection<CoexpressionCacheValueObject> ) element.getObjectValue() ).add( coExVOForCache );
        } else {
            Collection<CoexpressionCacheValueObject> cachedValues = new HashSet<CoexpressionCacheValueObject>();
            cachedValues.add( coExVOForCache );
            c.put( new Element( queryGene, cachedValues ) );
        }
    }

    /**
     * 
     */
    public void clearAllCaches() {
        for ( Long e : caches.keySet() ) {
            clearCache( e );
        }
    }

    /**
     * Remove all elements from the cache for the given expression experiment, if the cache exists.
     * 
     * @param e the expression experiment - specific cache to be cleared.
     */
    public void clearCache( Long e ) {
        CacheManager manager = CacheManager.getInstance();
        Cache cache = manager.getCache( getCacheName( e ) );
        if ( cache != null ) cache.removeAll();
    }

    /**
     * @return
     */
    public Collection<Cache> getAllCaches() {
        return caches.values();
    }

    /**
     * Get the vector cache for a particular experiment
     * 
     * @param e
     * @return
     */
    public Cache getCache( Long e ) {
        if ( !caches.containsKey( e ) ) {
            initializeCache( e );
        }
        return caches.get( e );
    }

    /**
     * @param eeID
     * @param queryGene
     * @return null if there are no cached results.
     */
    @SuppressWarnings("unchecked")
    public Collection<CoexpressionCacheValueObject> retrieve( Long eeID, Gene queryGene ) {
        Cache c = getCache( eeID );
        Element element = c.get( queryGene );
        if ( element != null ) {
            return ( Collection<CoexpressionCacheValueObject> ) element.getValue();
        }
        return null;

    }

    /**
     * @param cacheManager the cacheManager to set
     */
    public void setCacheManager( CacheManager cacheManager ) {
        this.cacheManager = cacheManager;
    }

    private String getCacheName( Long id ) {
        return PROCESSED_DATA_VECTOR_CACHE_NAME_BASE + "_" + id;
    }

    /**
     * Initialize the vector cache; if it already exists it will not be recreated.
     * 
     * @return
     */
    private void initializeCache( Long e ) {

        if ( caches.containsKey( e ) ) {
            return;
        }

        /*
         * TODO: allow easy disabling of cache.
         */

        int maxElements = ConfigUtils.getInt( "gemma.cache.probe2probe.maxelements",
                PROCESSED_DATA_VECTOR_CACHE_DEFAULT_MAX_ELEMENTS );
        int timeToLive = ConfigUtils.getInt( "gemma.cache.probe2probe.timetolive",
                PROCESSED_DATA_VECTOR_CACHE_DEFAULT_TIME_TO_LIVE );
        int timeToIdle = ConfigUtils.getInt( "gemma.cache.probe2probe.timetoidle",
                PROCESSED_DATA_VECTOR_CACHE_DEFAULT_TIME_TO_IDLE );

        boolean overFlowToDisk = ConfigUtils.getBoolean( "gemma.cache.probe2probe.usedisk",
                PROCESSED_DATA_VECTOR_CACHE_DEFAULT_OVERFLOW_TO_DISK );

        boolean eternal = ConfigUtils.getBoolean( "gemma.cache.probe2probe.eternal",
                PROCESSED_DATA_VECTOR_CACHE_DEFAULT_ETERNAL );

        boolean diskPersistent = ConfigUtils.getBoolean( "gemma.cache.diskpersistent", false );

        String cacheName = getCacheName( e );

        if ( !cacheManager.cacheExists( cacheName ) ) {

            cacheManager.addCache( new Cache( cacheName, maxElements, MemoryStoreEvictionPolicy.LRU, overFlowToDisk,
                    null, eternal, timeToLive, timeToIdle, diskPersistent, 600 /* diskExpiryThreadInterval */, null ) );
        }
        caches.put( e, cacheManager.getCache( cacheName ) );

    }

}
