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
 * Spring Service base class for <code>ubic.gemma.model.common.auditAndSecurity.UserService</code>, provides access to
 * all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.common.auditAndSecurity.UserService
 */
public abstract class UserServiceBase implements ubic.gemma.model.common.auditAndSecurity.UserService {

    private ubic.gemma.model.common.auditAndSecurity.UserDao userDao;

    private ubic.gemma.model.common.auditAndSecurity.UserRoleDao userRoleDao;

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#addRole(ubic.gemma.model.common.auditAndSecurity.User,
     *      java.lang.String)
     */
    public void addRole( final ubic.gemma.model.common.auditAndSecurity.User user, final java.lang.String roleName ) {
        try {
            this.handleAddRole( user, roleName );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.UserServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.UserService.addRole(ubic.gemma.model.common.auditAndSecurity.User user, java.lang.String roleName)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#create(ubic.gemma.model.common.auditAndSecurity.User)
     */
    public ubic.gemma.model.common.auditAndSecurity.User create(
            final ubic.gemma.model.common.auditAndSecurity.User user )
            throws ubic.gemma.model.common.auditAndSecurity.UserExistsException {
        try {
            return this.handleCreate( user );
        } catch ( ubic.gemma.model.common.auditAndSecurity.UserExistsException ex ) {
            throw ex;
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.UserServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.UserService.create(ubic.gemma.model.common.auditAndSecurity.User user)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#delete(java.lang.String)
     */
    public void delete( final java.lang.String userName ) {
        try {
            this.handleDelete( userName );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.UserServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.UserService.delete(java.lang.String userName)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#findByEmail(java.lang.String)
     */
    public ubic.gemma.model.common.auditAndSecurity.User findByEmail( final java.lang.String email ) {
        try {
            return this.handleFindByEmail( email );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.UserServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.UserService.findByEmail(java.lang.String email)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#findByUserName(java.lang.String)
     */
    public ubic.gemma.model.common.auditAndSecurity.User findByUserName( final java.lang.String userName ) {
        try {
            return this.handleFindByUserName( userName );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.UserServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.UserService.findByUserName(java.lang.String userName)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#load(java.lang.Long)
     */
    public ubic.gemma.model.common.auditAndSecurity.User load( final java.lang.Long id ) {
        try {
            return this.handleLoad( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.UserServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.UserService.load(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#loadAll()
     */
    public java.util.Collection<User> loadAll() {
        try {
            return this.handleLoadAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.UserServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.UserService.loadAll()' --> " + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#loadAllRoles()
     */
    public java.util.Collection<UserRole> loadAllRoles() {
        try {
            return this.handleLoadAllRoles();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.UserServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.UserService.loadAllRoles()' --> " + th,
                    th );
        }
    }

    /**
     * Sets the reference to <code>user</code>'s DAO.
     */
    public void setUserDao( ubic.gemma.model.common.auditAndSecurity.UserDao userDao ) {
        this.userDao = userDao;
    }

    /**
     * Sets the reference to <code>userRole</code>'s DAO.
     */
    public void setUserRoleDao( ubic.gemma.model.common.auditAndSecurity.UserRoleDao userRoleDao ) {
        this.userRoleDao = userRoleDao;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#update(ubic.gemma.model.common.auditAndSecurity.User)
     */
    public void update( final ubic.gemma.model.common.auditAndSecurity.User user ) {
        try {
            this.handleUpdate( user );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.auditAndSecurity.UserServiceException(
                    "Error performing 'ubic.gemma.model.common.auditAndSecurity.UserService.update(ubic.gemma.model.common.auditAndSecurity.User user)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the message having the given <code>key</code> using the given <code>arguments</code> for the given
     * <code>locale</code>.
     * 
     * @param key the key of the message in the messages.properties message bundle.
     * @param arguments any arguments to substitute when resolving the message.
     * @param locale the locale of the messages to retrieve.
     */
    protected String getMessage( final java.lang.String key, final java.lang.Object[] arguments,
            final java.util.Locale locale ) {
        return this.getMessages().getMessage( key, arguments, locale );
    }

    /**
     * Gets the message having the given <code>key</code> in the underlying message bundle.
     * 
     * @param key the key of the message in the messages.properties message bundle.
     */
    protected String getMessage( final String key ) {
        return this.getMessages().getMessage( key, null, null );
    }

    /**
     * Gets the message having the given <code>key</code> and <code>arguments</code> in the underlying message bundle.
     * 
     * @param key the key of the message in the messages.properties message bundle.
     * @param arguments any arguments to substitute when resolving the message.
     */
    protected String getMessage( final String key, final Object[] arguments ) {
        return this.getMessages().getMessage( key, arguments, null );
    }

    /**
     * Gets the message source available to this service.
     */
    protected org.springframework.context.MessageSource getMessages() {
        return ( org.springframework.context.MessageSource ) ubic.gemma.spring.BeanLocator.instance().getBean(
                "messageSource" );
    }

    /**
     * Gets the current <code>principal</code> if one has been set, otherwise returns <code>null</code>.
     * 
     * @return the current principal
     */
    protected java.security.Principal getPrincipal() {
        return ubic.gemma.spring.PrincipalStore.get();
    }

    /**
     * Gets the reference to <code>user</code>'s DAO.
     */
    protected ubic.gemma.model.common.auditAndSecurity.UserDao getUserDao() {
        return this.userDao;
    }

    /**
     * Gets the reference to <code>userRole</code>'s DAO.
     */
    protected ubic.gemma.model.common.auditAndSecurity.UserRoleDao getUserRoleDao() {
        return this.userRoleDao;
    }

    /**
     * Performs the core logic for {@link #addRole(ubic.gemma.model.common.auditAndSecurity.User, java.lang.String)}
     */
    protected abstract void handleAddRole( ubic.gemma.model.common.auditAndSecurity.User user, java.lang.String roleName )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.common.auditAndSecurity.User)}
     */
    protected abstract ubic.gemma.model.common.auditAndSecurity.User handleCreate(
            ubic.gemma.model.common.auditAndSecurity.User user ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #delete(java.lang.String)}
     */
    protected abstract void handleDelete( java.lang.String userName ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByEmail(java.lang.String)}
     */
    protected abstract ubic.gemma.model.common.auditAndSecurity.User handleFindByEmail( java.lang.String email )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByUserName(java.lang.String)}
     */
    protected abstract ubic.gemma.model.common.auditAndSecurity.User handleFindByUserName( java.lang.String userName )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.common.auditAndSecurity.User handleLoad( java.lang.Long id )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection<User> handleLoadAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadAllRoles()}
     */
    protected abstract java.util.Collection<UserRole> handleLoadAllRoles() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.common.auditAndSecurity.User)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.common.auditAndSecurity.User user )
            throws java.lang.Exception;

}