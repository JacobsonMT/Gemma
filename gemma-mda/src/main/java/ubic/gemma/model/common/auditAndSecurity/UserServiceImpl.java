/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package ubic.gemma.model.common.auditAndSecurity;

import java.util.Collection;
import java.util.HashSet;
import org.springframework.dao.DataIntegrityViolationException;

import ubic.gemma.Constants;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.UserService
 * @author pavlidis
 * @version $Id$
 */
public class UserServiceImpl extends ubic.gemma.model.common.auditAndSecurity.UserServiceBase {

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#getUser(java.lang.String)
     */
    @Override
    protected ubic.gemma.model.common.auditAndSecurity.User handleGetUser( java.lang.String userName )
            throws java.lang.Exception {
        return this.getUserDao().findByUserName( userName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#getUsers(ubic.gemma.model.common.auditAndSecurity.User)
     */
    @Override
    protected java.util.Collection handleGetUsers() throws java.lang.Exception {
        return this.getUserDao().loadAll();
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#FindById(long)
     */
    @Override
    protected User handleFindById( Long id ) throws java.lang.Exception {
        return ( User ) this.getUserDao().load( id );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#saveUser(ubic.gemma.model.common.auditAndSecurity.User)
     */
    @Override
    protected User handleSaveUser( ubic.gemma.model.common.auditAndSecurity.User user ) throws UserExistsException {

        if ( user.getUserName() == null ) {
            throw new IllegalArgumentException( "UserName cannot be null" );
        }

        // defensive programming...
        for ( UserRole role : user.getRoles() ) {
            role.setUserName( user.getUserName() );
        }

        if ( this.getUserDao().findByUserName( user.getUserName() ) != null ) {
            throw new UserExistsException( "User '" + user.getUserName() + "' already exists!" );
        }

        if ( this.findByEmail( user.getEmail() ) != null ) {
            throw new UserExistsException( "A user with email address " + user.getEmail() + " already exists." );
        }

        try {
            user.setConfirmPassword( user.getPassword() );
            return ( User ) this.getUserDao().create( user );
        } catch ( DataIntegrityViolationException e ) {
            throw new UserExistsException( "User '" + user.getUserName() + "' already exists!" );
        } catch ( org.springframework.dao.InvalidDataAccessResourceUsageException e ) {
            // shouldn't happen if we don't have duplicates in the first place...but just in case.
            throw new UserExistsException( "User '" + user.getUserName() + "' already exists!" );
        }

    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#removeUser(java.lang.String)
     */
    @Override
    protected void handleRemoveUser( java.lang.String userName ) throws java.lang.Exception {

        this.getUserDao().remove( this.getUserDao().findByUserName( userName ) );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handleAddRole( User user, String role ) throws Exception {
        if ( role == null ) throw new IllegalArgumentException( "Got passed null role!" );
        if ( user == null ) throw new IllegalArgumentException( "Got passed null user" );

        if ( !role.equals( Constants.ADMIN_ROLE ) && !role.equals( Constants.USER_ROLE ) ) {
            throw new IllegalArgumentException( role + " is not a recognized role" );
        }

        UserRole newRole = UserRole.Factory.newInstance();
        newRole.setName( role );
        newRole.setUserName( user.getUserName() );
        newRole = this.getUserRoleService().saveRole( newRole );
        if ( user.getRoles() == null ) user.setRoles( new HashSet() );
        Collection<UserRole> roles = user.getRoles();
        roles.add( newRole );
        user.setRoles( roles );
        this.getUserDao().update( user );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.UserServiceBase#handleFindByUserName(java.lang.String)
     */
    @Override
    protected User handleFindByUserName( String userName ) throws Exception {
        return this.getUserDao().findByUserName( userName );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.UserServiceBase#handleFindByEmail(java.lang.String)
     */
    @Override
    protected User handleFindByEmail( String email ) throws Exception {
        Contact c = this.getUserDao().findByEmail( email );
        if ( c instanceof User ) return ( User ) c;
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.UserServiceBase#handleUpdate(ubic.gemma.model.common.auditAndSecurity.User)
     */
    @Override
    protected void handleUpdate( User user ) throws Exception {
        this.getUserDao().update( user );
    }
}
