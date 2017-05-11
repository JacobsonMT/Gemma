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
package ubic.gemma.persistence.service.common.auditAndSecurity;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.Person;

/**
 * <p>
 * Spring Service base class for <code>PersonService</code>, provides access to
 * all services and entities referenced by this service.
 * </p>
 * 
 * @see PersonService
 */
public abstract class PersonServiceBase implements PersonService {

    @Autowired
    private PersonDao personDao;

    /**
     * @see PersonService#create(ubic.gemma.model.common.auditAndSecurity.Person)
     */
    @Override
    @Transactional
    public Person create( final Person person ) {
        return this.handleCreate( person );

    }

    /**
     * @see PersonService#findByFullName(java.lang.String, java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Person> findByFullName( final java.lang.String name, final java.lang.String lastName ) {
        return this.handleFindByFullName( name, lastName );

    }

    /**
     * @see PersonService#findOrCreate(ubic.gemma.model.common.auditAndSecurity.Person)
     */
    @Override
    @Transactional
    public ubic.gemma.model.common.auditAndSecurity.Person findOrCreate(
            final ubic.gemma.model.common.auditAndSecurity.Person person ) {
        return this.handleFindOrCreate( person );

    }

    /**
     * @see PersonService#loadAll()
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<Person> loadAll() {
        return this.handleLoadAll();

    }

    /**
     * @see PersonService#remove(ubic.gemma.model.common.auditAndSecurity.Person)
     */
    @Override
    @Transactional
    public void remove( final Person person ) {
        this.handleRemove( person );

    }

    /**
     * Sets the reference to <code>person</code>'s DAO.
     */
    public void setPersonDao( PersonDao personDao ) {
        this.personDao = personDao;
    }

    /**
     * Gets the reference to <code>person</code>'s DAO.
     */
    protected PersonDao getPersonDao() {
        return this.personDao;
    }

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.common.auditAndSecurity.Person)}
     */
    protected abstract ubic.gemma.model.common.auditAndSecurity.Person handleCreate(
            ubic.gemma.model.common.auditAndSecurity.Person person );

    /**
     * Performs the core logic for {@link #findByFullName(java.lang.String, java.lang.String)}
     */
    protected abstract java.util.Collection<Person> handleFindByFullName( java.lang.String name,
            java.lang.String lastName );

    /**
     * Performs the core logic for {@link #findOrCreate(ubic.gemma.model.common.auditAndSecurity.Person)}
     */
    protected abstract Person handleFindOrCreate( Person person );

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection<Person> handleLoadAll();

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.common.auditAndSecurity.Person)}
     */
    protected abstract void handleRemove( ubic.gemma.model.common.auditAndSecurity.Person person );

}