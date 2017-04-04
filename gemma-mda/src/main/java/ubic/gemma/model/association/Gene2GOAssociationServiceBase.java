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
package ubic.gemma.model.association;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.description.VocabCharacteristicDao;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.CacheUtils;
import ubic.gemma.util.Settings;

/**
 * <p>
 * Spring Service base class for <code>Gene2GOAssociationService</code>, provides access to all services and entities
 * referenced by this service.
 * </p>
 *
 * @see Gene2GOAssociationService
 */
public abstract class Gene2GOAssociationServiceBase implements Gene2GOAssociationService, InitializingBean {
    private static final String G2G_CACHE_NAME = "Gene2GoServiceCache";
    protected Cache gene2goCache;

    @Autowired
    private Gene2GOAssociationDao gene2GOAssociationDao;

    @Autowired
    private ubic.gemma.model.common.description.VocabCharacteristicDao vocabCharacteristicDao;

    @Autowired
    private CacheManager cacheManager;

    @Override
    public void afterPropertiesSet() throws Exception {

        boolean terracottaEnabled = Settings.getBoolean( "gemma.cache.clustered", false );

        cacheManager.addCache( gene2goCache );
        this.gene2goCache = CacheUtils
                .createOrLoadCache( cacheManager, G2G_CACHE_NAME, terracottaEnabled, 5000, false, false, 1000, 500,
                        false );

    }

    /**
     * @see Gene2GOAssociationService#create(Gene2GOAssociation)
     */
    @Override
    @Transactional
    public Gene2GOAssociation create( final Gene2GOAssociation gene2GOAssociation ) {
        try {
            return this.handleCreate( gene2GOAssociation );
        } catch ( Throwable th ) {
            throw new Gene2GOAssociationServiceException(
                    "Error performing 'Gene2GOAssociationService.create(Gene2GOAssociation gene2GOAssociation)' --> "
                            + th, th );
        }
    }

    /**
     * @see Gene2GOAssociationService#find(Gene2GOAssociation)
     */
    @Override
    @Transactional(readOnly = true)
    public Gene2GOAssociation find( final Gene2GOAssociation gene2GOAssociation ) {
        try {
            return this.handleFind( gene2GOAssociation );
        } catch ( Throwable th ) {
            throw new Gene2GOAssociationServiceException(
                    "Error performing 'Gene2GOAssociationService.find(Gene2GOAssociation gene2GOAssociation)' --> "
                            + th, th );
        }
    }

    /**
     * @see Gene2GOAssociationService#findAssociationByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<Gene2GOAssociation> findAssociationByGene( final ubic.gemma.model.genome.Gene gene ) {
        try {
            return this.handleFindAssociationByGene( gene );
        } catch ( Throwable th ) {
            throw new Gene2GOAssociationServiceException(
                    "Error performing 'Gene2GOAssociationService.findAssociationByGene(ubic.gemma.model.genome.Gene gene)' --> "
                            + th, th );
        }
    }

    /**
     * @see Gene2GOAssociationService#findByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<VocabCharacteristic> findByGene( final ubic.gemma.model.genome.Gene gene ) {
        try {
            return this.handleFindByGene( gene );
        } catch ( Throwable th ) {
            throw new Gene2GOAssociationServiceException(
                    "Error performing 'Gene2GOAssociationService.findByGene(ubic.gemma.model.genome.Gene gene)' --> "
                            + th, th );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<Gene> findByGOTerm( final java.lang.String goID ) {
        try {
            return this.getGene2GOAssociationDao().findByGoTerm( goID );
        } catch ( Throwable th ) {
            throw new Gene2GOAssociationServiceException(
                    "Error performing 'Gene2GOAssociationService.findByGOTerm(java.lang.String goID, ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * @see Gene2GOAssociationService#findByGOTerm(java.lang.String, ubic.gemma.model.genome.Taxon)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<Gene> findByGOTerm( final java.lang.String goID,
            final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            return this.handleFindByGOTerm( goID, taxon );
        } catch ( Throwable th ) {
            throw new Gene2GOAssociationServiceException(
                    "Error performing 'Gene2GOAssociationService.findByGOTerm(java.lang.String goID, ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * @see Gene2GOAssociationService#findOrCreate(Gene2GOAssociation)
     */
    @Override
    @Transactional(readOnly = true)
    public Gene2GOAssociation findOrCreate( final Gene2GOAssociation gene2GOAssociation ) {
        try {
            return this.handleFindOrCreate( gene2GOAssociation );
        } catch ( Throwable th ) {
            throw new Gene2GOAssociationServiceException(
                    "Error performing 'Gene2GOAssociationService.findOrCreate(Gene2GOAssociation gene2GOAssociation)' --> "
                            + th, th );
        }
    }

    /**
     * @see Gene2GOAssociationService#removeAll()
     */
    @Override
    @Transactional
    public void removeAll() {
        try {
            this.handleRemoveAll();
        } catch ( Throwable th ) {
            throw new Gene2GOAssociationServiceException(
                    "Error performing 'Gene2GOAssociationService.removeAll()' --> " + th, th );
        }
    }

    /**
     * Gets the reference to <code>gene2GOAssociation</code>'s DAO.
     */

    protected Gene2GOAssociationDao getGene2GOAssociationDao() {
        return this.gene2GOAssociationDao;
    }

    /**
     * Sets the reference to <code>gene2GOAssociation</code>'s DAO.
     */
    public void setGene2GOAssociationDao( Gene2GOAssociationDao gene2GOAssociationDao ) {
        this.gene2GOAssociationDao = gene2GOAssociationDao;
    }

    /**
     * Gets the reference to <code>vocabCharacteristic</code>'s DAO.
     */
    protected VocabCharacteristicDao getVocabCharacteristicDao() {
        return this.vocabCharacteristicDao;
    }

    /**
     * Sets the reference to <code>vocabCharacteristic</code>'s DAO.
     */
    public void setVocabCharacteristicDao(
            ubic.gemma.model.common.description.VocabCharacteristicDao vocabCharacteristicDao ) {
        this.vocabCharacteristicDao = vocabCharacteristicDao;
    }

    /**
     * Performs the core logic for {@link #create(Gene2GOAssociation)}
     */
    protected abstract Gene2GOAssociation handleCreate( Gene2GOAssociation gene2GOAssociation )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #find(Gene2GOAssociation)}
     */
    protected abstract Gene2GOAssociation handleFind( Gene2GOAssociation gene2GOAssociation )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findAssociationByGene(ubic.gemma.model.genome.Gene)}
     */
    protected abstract java.util.Collection<Gene2GOAssociation> handleFindAssociationByGene(
            ubic.gemma.model.genome.Gene gene ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByGene(ubic.gemma.model.genome.Gene)}
     */
    protected abstract java.util.Collection<VocabCharacteristic> handleFindByGene( ubic.gemma.model.genome.Gene gene )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByGOTerm(java.lang.String, ubic.gemma.model.genome.Taxon)}
     */
    protected abstract java.util.Collection<Gene> handleFindByGOTerm( java.lang.String goID,
            ubic.gemma.model.genome.Taxon taxon ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(Gene2GOAssociation)}
     */
    protected abstract Gene2GOAssociation handleFindOrCreate( Gene2GOAssociation gene2GOAssociation )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #removeAll()}
     */
    protected abstract void handleRemoveAll() throws java.lang.Exception;

}