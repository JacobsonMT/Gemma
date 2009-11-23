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
package ubic.gemma.model.genome;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.genome.ChromosomeService</code>, provides access to all services
 * and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.genome.ChromosomeService
 */
public abstract class ChromosomeServiceBase implements ubic.gemma.model.genome.ChromosomeService {

    @Autowired
    private ubic.gemma.model.genome.ChromosomeDao chromosomeDao;

    /**
     * @see ubic.gemma.model.genome.ChromosomeService#find(ubic.gemma.model.genome.Chromosome)
     */
    public ubic.gemma.model.genome.Chromosome find( final ubic.gemma.model.genome.Chromosome chromosome ) {
        try {
            return this.handleFind( chromosome );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.ChromosomeServiceException(
                    "Error performing 'ubic.gemma.model.genome.ChromosomeService.find(ubic.gemma.model.genome.Chromosome chromosome)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeService#findOrCreate(ubic.gemma.model.genome.Chromosome)
     */
    public ubic.gemma.model.genome.Chromosome findOrCreate( final ubic.gemma.model.genome.Chromosome chromosome ) {
        try {
            return this.handleFindOrCreate( chromosome );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.ChromosomeServiceException(
                    "Error performing 'ubic.gemma.model.genome.ChromosomeService.findOrCreate(ubic.gemma.model.genome.Chromosome chromosome)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>chromosome</code>'s DAO.
     */
    public void setChromosomeDao( ubic.gemma.model.genome.ChromosomeDao chromosomeDao ) {
        this.chromosomeDao = chromosomeDao;
    }

    /**
     * Gets the reference to <code>chromosome</code>'s DAO.
     */
    protected ubic.gemma.model.genome.ChromosomeDao getChromosomeDao() {
        return this.chromosomeDao;
    }

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.genome.Chromosome)}
     */
    protected abstract ubic.gemma.model.genome.Chromosome handleFind( ubic.gemma.model.genome.Chromosome chromosome )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.genome.Chromosome)}
     */
    protected abstract ubic.gemma.model.genome.Chromosome handleFindOrCreate(
            ubic.gemma.model.genome.Chromosome chromosome ) throws java.lang.Exception;

}