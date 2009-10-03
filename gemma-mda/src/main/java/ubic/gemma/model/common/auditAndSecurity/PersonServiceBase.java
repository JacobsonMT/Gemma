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
package ubic.gemma.model.common.auditAndSecurity;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.common.auditAndSecurity.PersonService</code>, provides access to
 * all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.common.auditAndSecurity.PersonService
 */
public abstract class PersonServiceBase implements ubic.gemma.model.common.auditAndSecurity.PersonService {

    private ubic.gemma.model.common.auditAndSecurity.PersonDao personDao;

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonService#create(ubic.gemma.model.common.auditAndSecurity.Person)
     */
    public ubic.gemma.model.common.auditAndSecurity.Person create(
            final ubic.gemma.model.common.auditAndSecurity.Person person ) {
        try {
            return this.handleCreate( person );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.PersonServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.PersonService.create(ubic.gemma.model.common.auditAndSecurity.Person person)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonService#findByFullName(java.lang.String, java.lang.String)
     */
    public java.util.Collection findByFullName( final java.lang.String name, final java.lang.String lastName ) {
        try {
            return this.handleFindByFullName( name, lastName );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.PersonServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.PersonService.findByFullName(java.lang.String name, java.lang.String lastName)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonService#findOrCreate(ubic.gemma.model.common.auditAndSecurity.Person)
     */
    public ubic.gemma.model.common.auditAndSecurity.Person findOrCreate(
            final ubic.gemma.model.common.auditAndSecurity.Person person ) {
        try {
            return this.handleFindOrCreate( person );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.PersonServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.PersonService.findOrCreate(ubic.gemma.model.common.auditAndSecurity.Person person)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonService#loadAll()
     */
    public java.util.Collection loadAll() {
        try {
            return this.handleLoadAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.PersonServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.PersonService.loadAll()' --> " + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.PersonService#remove(ubic.gemma.model.common.auditAndSecurity.Person)
     */
    public void remove( final ubic.gemma.model.common.auditAndSecurity.Person person ) {
        try {
            this.handleRemove( person );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.PersonServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.PersonService.remove(ubic.gemma.model.common.auditAndSecurity.Person person)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>person</code>'s DAO.
     */
    public void setPersonDao( ubic.gemma.model.common.auditAndSecurity.PersonDao personDao ) {
        this.personDao = personDao;
    }

    /**
     * Gets the reference to <code>person</code>'s DAO.
     */
    protected ubic.gemma.model.common.auditAndSecurity.PersonDao getPersonDao() {
        return this.personDao;
    }

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.common.auditAndSecurity.Person)}
     */
    protected abstract ubic.gemma.model.common.auditAndSecurity.Person handleCreate(
            ubic.gemma.model.common.auditAndSecurity.Person person ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByFullName(java.lang.String, java.lang.String)}
     */
    protected abstract java.util.Collection handleFindByFullName( java.lang.String name, java.lang.String lastName )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.common.auditAndSecurity.Person)}
     */
    protected abstract ubic.gemma.model.common.auditAndSecurity.Person handleFindOrCreate(
            ubic.gemma.model.common.auditAndSecurity.Person person ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection handleLoadAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.common.auditAndSecurity.Person)}
     */
    protected abstract void handleRemove( ubic.gemma.model.common.auditAndSecurity.Person person )
            throws java.lang.Exception;

}