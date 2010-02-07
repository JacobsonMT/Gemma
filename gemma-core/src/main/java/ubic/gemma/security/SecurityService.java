/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.util.AuthorityConstants;

/**
 * Methods for changing security on objects, creating and modifying groups, checking security on objects.
 * 
 * @author keshav
 * @author paul
 * @version $Id$
 */
@Service
public class SecurityService {

    /**
     * This is defined in spring-security AuthenticationConfigBuilder, and can be set in the <security:anonymous />
     * configuration of the <security:http/> namespace config
     */
    public static final String ANONYMOUS = "anonymousUser";

    private static AuthenticationTrustResolver authenticationTrustResolver = new AuthenticationTrustResolverImpl();

    /**
     * Returns true if the current user has admin authority.
     * 
     * @return true if the current user has admin authority
     */
    public static boolean isUserAdmin() {

        if ( !isUserLoggedIn() ) {
            return false;
        }

        Collection<GrantedAuthority> authorities = getAuthentication().getAuthorities();
        assert authorities != null;
        for ( GrantedAuthority authority : authorities ) {
            if ( authority.getAuthority().equals( AuthorityConstants.ADMIN_GROUP_AUTHORITY ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return
     */
    public static boolean isUserAnonymous() {
        return authenticationTrustResolver.isAnonymous( getAuthentication() )
                || getAuthentication().getPrincipal().equals( "anonymousUser" );
    }

    /**
     * Returns true if the user is non-anonymous.
     * 
     * @return
     */
    public static boolean isUserLoggedIn() {
        return !isUserAnonymous();
    }

    /**
     * Returns the Authentication object from the SecurityContextHolder.
     * 
     * @return Authentication
     */
    private static Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if ( authentication == null ) throw new RuntimeException( "Null authentication object" );

        return authentication;
    }

    @Autowired
    private MutableAclService aclService;

    private Log log = LogFactory.getLog( SecurityService.class );

    private ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy = new ObjectIdentityRetrievalStrategyImpl();

    @Autowired
    private SidRetrievalStrategy sidRetrievalStrategy;

    @Autowired
    private UserManager userManager;

    /**
     * @param userName
     * @param groupName
     */
    public void addUserToGroup( String userName, String groupName ) {
        this.userManager.addUserToGroup( userName, groupName );
    }

    /**
     * A securable is considered "owned" if 1) the user is the actual owner assigned in the ACL or 2) the user is an
     * administrator. In other words, for an administrator, the value will always be true.
     * 
     * @param securables
     * @return
     */
    public Map<Securable, Boolean> areOwnedByCurrentUser( Collection<? extends Securable> securables ) {

        Map<Securable, Boolean> result = new HashMap<Securable, Boolean>();

        Map<ObjectIdentity, Securable> objectIdentities = getObjectIdentities( securables );

        if ( objectIdentities.isEmpty() ) return result;

        /*
         * Take advantage of fast bulk loading of ACLs. Other methods sohuld adopt this if they turn out to be heavily
         * used/slow.
         */
        Map<ObjectIdentity, Acl> acls = aclService
                .readAclsById( new Vector<ObjectIdentity>( objectIdentities.keySet() ) );

        String currentUsername = userManager.getCurrentUsername();

        boolean isAdmin = isUserAdmin();

        for ( ObjectIdentity oi : acls.keySet() ) {
            Acl a = acls.get( oi );
            Sid owner = a.getOwner();

            result.put( objectIdentities.get( oi ), false );
            if ( isAdmin
                    || ( owner != null && owner instanceof PrincipalSid && ( ( PrincipalSid ) owner ).getPrincipal()
                            .equals( currentUsername ) ) ) {
                result.put( objectIdentities.get( oi ), true );
            }
        }
        return result;

    }

    /**
     * @param securables
     * @return
     */
    @Secured( { "ACL_SECURABLE_COLLECTION_READ" })
    public java.util.Map<Securable, Boolean> arePrivate( Collection<? extends Securable> securables ) {
        Map<Securable, Boolean> result = new HashMap<Securable, Boolean>();
        Map<ObjectIdentity, Securable> objectIdentities = getObjectIdentities( securables );

        if ( objectIdentities.isEmpty() ) return result;

        /*
         * Take advantage of fast bulk loading of ACLs. Other methods sohuld adopt this if they turn out to be heavily
         * used/slow.
         */
        Map<ObjectIdentity, Acl> acls = aclService
                .readAclsById( new Vector<ObjectIdentity>( objectIdentities.keySet() ) );

        for ( ObjectIdentity oi : acls.keySet() ) {
            Acl a = acls.get( oi );
            boolean p = isPrivate( a );
            result.put( objectIdentities.get( oi ), p );
        }
        return result;
    }

    @Secured( { "ACL_SECURABLE_COLLECTION_READ" })
    public Map<Securable, Boolean> areShared( Collection<? extends Securable> securables ) {
        Map<Securable, Boolean> result = new HashMap<Securable, Boolean>();
        Map<ObjectIdentity, Securable> objectIdentities = getObjectIdentities( securables );

        if ( objectIdentities.isEmpty() ) return result;

        Map<ObjectIdentity, Acl> acls = aclService
                .readAclsById( new Vector<ObjectIdentity>( objectIdentities.keySet() ) );

        for ( ObjectIdentity oi : acls.keySet() ) {
            Acl a = acls.get( oi );
            boolean p = isShared( a );
            result.put( objectIdentities.get( oi ), p );
        }
        return result;
    }

    /**
     * @param securables
     * @return the subset which are private, if any
     */
    public Collection<Securable> choosePrivate( Collection<? extends Securable> securables ) {
        Collection<Securable> result = new HashSet<Securable>();
        Map<Securable, Boolean> arePrivate = arePrivate( securables );

        for ( Securable s : securables ) {
            if ( arePrivate.get( s ) ) result.add( s );
        }
        return result;
    }

    /**
     * @param securables
     * @return the subset that are public, if any
     */
    @Secured( { "ACL_SECURABLE_COLLECTION_READ" })
    public Collection<Securable> choosePublic( Collection<? extends Securable> securables ) {
        Collection<Securable> result = new HashSet<Securable>();

        Map<Securable, Boolean> arePrivate = arePrivate( securables );

        for ( Securable s : securables ) {
            if ( !arePrivate.get( s ) ) result.add( s );
        }
        return result;
    }

    /**
     * If the group already exists, an exception will be thrown.
     * 
     * @param groupName
     */
    @Transactional
    public void createGroup( String groupName ) {

        /*
         * Nice if we can get around this uniqueness constraint...but I guess it's not easy.
         */
        if ( userManager.groupExists( groupName ) ) {
            throw new IllegalArgumentException( "A group already exists with that name" );
        }

        /*
         * We do make the groupAuthority unique.
         */
        String groupAuthority = groupName.toUpperCase() + "_"
                + RandomStringUtils.randomAlphanumeric( 32 ).toUpperCase();

        List<GrantedAuthority> auths = new ArrayList<GrantedAuthority>();
        auths.add( new GrantedAuthorityImpl( groupAuthority ) );

        this.userManager.createGroup( groupName, auths );
        addUserToGroup( userManager.getCurrentUsername(), groupName );

    }

    /**
     * @param groupName
     */
    public void deleteGroup( String groupName ) {
        /*
         * FIXME this doesn't clean up everything.
         */

        userManager.deleteGroup( groupName );
    }

    /**
     * @param s
     * @return list of userNames who can edit the given securable.
     */
    @Secured( { "ACL_SECURABLE_READ" })
    public Collection<String> editableBy( Securable s ) {

        Collection<String> allUsers = userManager.findAllUsers();

        Collection<String> result = new HashSet<String>();

        for ( String u : allUsers ) {
            if ( isEditableByUser( s, u ) ) {
                result.add( u );
            }
        }

        return result;

    }

    /**
     * This methods is only available to administrators.
     * 
     * @return collection of all available security ids (basically, user names and group authorities.
     */
    @Secured("GROUP_ADMIN")
    public Collection<Sid> getAvailableSids() {

        Collection<Sid> results = new HashSet<Sid>();

        Collection<String> users = userManager.findAllUsers();

        for ( String u : users ) {
            results.add( new PrincipalSid( u ) );
        }

        Collection<String> groups = userManager.findAllGroups();

        for ( String g : groups ) {
            List<GrantedAuthority> ga = userManager.findGroupAuthorities( g );
            for ( GrantedAuthority grantedAuthority : ga ) {
                results.add( new GrantedAuthoritySid( grantedAuthority.getAuthority() ) );
            }
        }

        return results;
    }

    /**
     * @param s
     * @return
     */
    @Secured( { "ACL_SECURABLE_COLLECTION_READ" })
    public Map<Securable, Collection<String>> getGroupsEditableBy( Collection<? extends Securable> securables ) {
        Collection<String> groupNames = getGroupsUserCanView();
        Map<Securable, Collection<String>> result = new HashMap<Securable, Collection<String>>();

        List<Permission> write = new ArrayList<Permission>();
        write.add( BasePermission.WRITE );

        List<Permission> admin = new ArrayList<Permission>();
        admin.add( BasePermission.ADMINISTRATION );

        for ( String groupName : groupNames ) {
            Map<Securable, Boolean> groupHasPermission = this.groupHasPermission( securables, write, groupName );

            populateGroupsEditableBy( result, groupName, groupHasPermission );

            groupHasPermission = this.groupHasPermission( securables, admin, groupName );

            populateGroupsEditableBy( result, groupName, groupHasPermission );

        }

        return result;
    }

    /**
     * @param s
     * @return
     */
    @Secured( { "ACL_SECURABLE_READ" })
    public Collection<String> getGroupsEditableBy( Securable s ) {
        Collection<String> groupNames = getGroupsUserCanView();

        Collection<String> result = new HashSet<String>();

        for ( String string : groupNames ) {
            if ( this.isEditableByGroup( s, string ) ) {
                result.add( string );
            }
        }

        return result;
    }

    /**
     * @param s
     * @return
     */
    @Secured( { "ACL_SECURABLE_COLLECTION_READ" })
    public Map<Securable, Collection<String>> getGroupsReadableBy( Collection<? extends Securable> securables ) {

        Map<Securable, Collection<String>> result = new HashMap<Securable, Collection<String>>();

        if ( securables.isEmpty() ) return result;

        Collection<String> groupNames = getGroupsUserCanView();

        List<Permission> read = new ArrayList<Permission>();
        read.add( BasePermission.READ );

        List<Permission> admin = new ArrayList<Permission>();
        admin.add( BasePermission.ADMINISTRATION );

        for ( String groupName : groupNames ) {
            Map<Securable, Boolean> groupHasPermission = this.groupHasPermission( securables, read, groupName );

            populateGroupsEditableBy( result, groupName, groupHasPermission );

            groupHasPermission = this.groupHasPermission( securables, admin, groupName );

            populateGroupsEditableBy( result, groupName, groupHasPermission );
        }

        return result;
    }

    /**
     * @param s
     * @return
     */
    @Secured( { "ACL_SECURABLE_READ" })
    public Collection<String> getGroupsReadableBy( Securable s ) {
        Collection<String> groupNames = getGroupsUserCanView();

        Collection<String> result = new HashSet<String>();

        for ( String string : groupNames ) {
            if ( this.isReadableByGroup( s, string ) ) {
                result.add( string );
            }
        }

        return result;
    }

    /**
     * @param userName
     * @return
     */
    public Collection<String> getGroupsUserCanEdit( String userName ) {
        Collection<String> groupNames = getGroupsUserCanView();

        Collection<String> result = new HashSet<String>();
        for ( String gname : groupNames ) {
            UserGroup g = userManager.findGroupByName( gname );
            if ( this.isEditableByUser( g, userName ) ) {
                result.add( gname );
            }
        }

        return result;

    }

    /**
     * @param s
     * @return
     */
    @Secured("ACL_SECURABLE_READ")
    public Sid getOwner( Securable s ) {
        ObjectIdentity oi = this.objectIdentityRetrievalStrategy.getObjectIdentity( s );
        Acl a = this.aclService.readAclById( oi );
        return a.getOwner();
    }

    /**
     * Pretty much have to be either the owner of the securables or administrator to call this.
     * 
     * @param securables
     * @return
     * @throws AccessDeniedException if the current user is not allowed to access the information.
     */
    @Secured("ACL_SECURABLE_COLLECTION_READ")
    public Map<Securable, Sid> getOwners( Collection<? extends Securable> securables ) {
        Map<Securable, Sid> result = new HashMap<Securable, Sid>();
        Map<ObjectIdentity, Securable> objectIdentities = getObjectIdentities( securables );

        if ( securables.isEmpty() ) return result;

        /*
         * Take advantage of fast bulk loading of ACLs. Other methods sohuld adopt this if they turn out to be heavily
         * used/slow.
         */
        Map<ObjectIdentity, Acl> acls = aclService
                .readAclsById( new Vector<ObjectIdentity>( objectIdentities.keySet() ) );

        for ( ObjectIdentity oi : acls.keySet() ) {
            Acl a = acls.get( oi );
            Sid owner = a.getOwner();
            if ( owner == null )
                result.put( objectIdentities.get( oi ), null );
            else
                result.put( objectIdentities.get( oi ), owner );
        }
        return result;
    }

    /**
     * @param s
     * @param groupName
     * @return
     */
    @Secured("ACL_SECURABLE_READ")
    public boolean isEditableByGroup( Securable s, String groupName ) {
        List<Permission> requiredPermissions = new ArrayList<Permission>();
        requiredPermissions.add( BasePermission.WRITE );

        if ( groupHasPermission( s, requiredPermissions, groupName ) ) {
            return true;
        }

        requiredPermissions.clear();
        requiredPermissions.add( BasePermission.ADMINISTRATION );
        return groupHasPermission( s, requiredPermissions, groupName );
    }

    /**
     * @param s
     * @param userName
     * @return true if the user has WRITE permissions or ADMIN
     */
    @Secured("ACL_SECURABLE_READ")
    public boolean isEditableByUser( Securable s, String userName ) {
        List<Permission> requiredPermissions = new ArrayList<Permission>();
        requiredPermissions.add( BasePermission.WRITE );
        if ( hasPermission( s, requiredPermissions, userName ) ) {
            return true;
        }

        requiredPermissions.clear();
        requiredPermissions.add( BasePermission.ADMINISTRATION );
        return hasPermission( s, requiredPermissions, userName );
    }

    /**
     * @param s
     * @return
     */
    public boolean isOwnedByCurrentUser( Securable s ) {
        ObjectIdentity oi = objectIdentityRetrievalStrategy.getObjectIdentity( s );

        try {
            Acl acl = this.aclService.readAclById( oi );

            Sid owner = acl.getOwner();
            if ( owner == null ) return false;

            if ( owner instanceof PrincipalSid ) {
                return ( ( PrincipalSid ) owner ).getPrincipal().equals( userManager.getCurrentUsername() );
            }

        } catch ( NotFoundException nfe ) {
            return false;
        }

        return false;
    }

    /**
     * Convenience method to determine the visibility of an object.
     * 
     * @param s
     * @return true if anonymous users can view (READ) the object, false otherwise. If the object doesn't have an ACL,
     *         return true (be safe!)
     * @see org.springframework.security.acls.jdbc.BasicLookupStrategy
     */
    public boolean isPrivate( Securable s ) {

        if ( s == null ) {
            return false;
        }

        /*
         * Implementation note: this code mimics AclEntryVoter.vote, but in adminsitrative mode so no auditing etc
         * happens.
         */

        List<Permission> perms = new Vector<Permission>();
        perms.add( BasePermission.READ );

        Sid anonSid = new GrantedAuthoritySid( new GrantedAuthorityImpl(
                AuthenticatedVoter.IS_AUTHENTICATED_ANONYMOUSLY ) );

        List<Sid> sids = new Vector<Sid>();
        sids.add( anonSid );

        ObjectIdentity oi = new ObjectIdentityImpl( s.getClass(), s.getId() );

        /*
         * Note: in theory, it should pay attention to the sid we ask for and return nothing if there is no acl.
         * However, the implementation actually ignores the sid argument. See BasicLookupStrategy
         */
        try {
            Acl acl = this.aclService.readAclById( oi, sids );

            return isPrivate( acl );
        } catch ( NotFoundException nfe ) {
            return true;
        }
    }

    /**
     * Convenience method to determine the visibility of an object.
     * 
     * @param s
     * @return the negation of isPrivate().
     */
    public boolean isPublic( Securable s ) {
        return !isPrivate( s );
    }

    @Secured("ACL_SECURABLE_READ")
    public boolean isReadableByGroup( Securable s, String groupName ) {
        List<Permission> requiredPermissions = new ArrayList<Permission>();
        requiredPermissions.add( BasePermission.READ );

        if ( groupHasPermission( s, requiredPermissions, groupName ) ) {
            return true;
        }

        requiredPermissions.clear();
        requiredPermissions.add( BasePermission.ADMINISTRATION );
        return groupHasPermission( s, requiredPermissions, groupName );
    }

    public boolean isShared( Securable s ) {
        if ( s == null ) {
            return false;
        }

        /*
         * Implementation note: this code mimics AclEntryVoter.vote, but in adminsitrative mode so no auditing etc
         * happens.
         */

        List<Permission> perms = new Vector<Permission>();
        perms.add( BasePermission.READ );

        ObjectIdentity oi = objectIdentityRetrievalStrategy.getObjectIdentity( s );

        /*
         * Note: in theory, it should pay attention to the sid we ask for and return nothing if there is no acl.
         * However, the implementation actually ignores the sid argument. See BasicLookupStrategy
         */
        try {
            Acl acl = this.aclService.readAclById( oi );

            return isShared( acl );
        } catch ( NotFoundException nfe ) {
            return true;
        }
    }

    /**
     * @param s
     * @param userName
     * @return true if the given user can read the securable, false otherwise. (READ or ADMINISTRATION required)
     */
    @Secured( { "ACL_SECURABLE_READ" })
    public boolean isViewableByUser( Securable s, String userName ) {
        List<Permission> requiredPermissions = new ArrayList<Permission>();
        requiredPermissions.add( BasePermission.READ );
        if ( hasPermission( s, requiredPermissions, userName ) ) {
            return true;
        }

        requiredPermissions.clear();
        requiredPermissions.add( BasePermission.ADMINISTRATION );
        return hasPermission( s, requiredPermissions, userName );
    }

    /**
     * Administrative method to allow a user to get access to an object. This is useful for cases where a data set is
     * loaded by admin but we need to hand it off to a user.
     * 
     * @param s
     * @param userName
     */
    @Secured("GROUP_ADMIN")
    @Transactional
    public void makeOwnedByUser( Securable s, String userName ) {
        MutableAcl acl = getAcl( s );

        if ( acl.getOwner().equals( userName ) ) {
            return;
        }

        acl.setOwner( new PrincipalSid( userName ) );
        aclService.updateAcl( acl );

        /*
         * FIXME: I don't know if these are necessary if you are the owner.
         */
        addPrincipalAuthority( s, BasePermission.WRITE, userName );
        addPrincipalAuthority( s, BasePermission.READ, userName );
    }

    /**
     * @param objs
     */
    public void makePrivate( Collection<? extends Securable> objs ) {
        for ( Securable s : objs ) {
            makePrivate( s );
        }
    }

    /**
     * Makes the object private.
     * 
     * @param object
     */
    @Secured("ACL_SECURABLE_EDIT")
    @Transactional
    public void makePrivate( Securable object ) {
        if ( object == null ) {
            return;
        }

        if ( isPrivate( object ) ) {
            log.warn( "Object is already private" );
            return;
        }

        /*
         * Remove ACE for IS_AUTHENTICATED_ANOYMOUSLY, if it's there.
         */
        String authorityToRemove = AuthorityConstants.IS_AUTHENTICATED_ANONYMOUSLY;

        removeGrantedAuthority( object, BasePermission.READ, authorityToRemove );

        if ( isPublic( object ) ) {
            throw new IllegalStateException( "Failed to make object private: " + object );
        }

    }

    /**
     * @param objs
     */
    @Transactional
    public void makePublic( Collection<? extends Securable> objs ) {
        for ( Securable s : objs ) {
            makePublic( s );
        }
    }

    /**
     * Makes the object public
     * 
     * @param object
     */
    @Secured("ACL_SECURABLE_EDIT")
    @Transactional
    public void makePublic( Securable object ) {

        if ( object == null ) {
            return;
        }

        if ( isPublic( object ) ) {
            log.warn( "Object is already public" );
            return;
        }

        /*
         * Add an ACE for IS_AUTHENTICATED_ANOYMOUSLY.
         */

        MutableAcl acl = getAcl( object );

        if ( acl == null ) {
            throw new IllegalArgumentException( "makePrivate is only valid for objects that have an ACL" );
        }

        acl.insertAce( acl.getEntries().size(), BasePermission.READ, new GrantedAuthoritySid( new GrantedAuthorityImpl(
                AuthorityConstants.IS_AUTHENTICATED_ANONYMOUSLY ) ), true );

        aclService.updateAcl( acl );

        if ( isPrivate( object ) ) {
            throw new IllegalStateException( "Failed to make object public: " + object );
        }

    }

    /**
     * Adds read permission.
     * 
     * @param s
     * @param groupName
     * @throws AccessDeniedException
     */
    @Secured("ACL_SECURABLE_EDIT")
    @Transactional
    public void makeReadableByGroup( Securable s, String groupName ) throws AccessDeniedException {
        Collection<String> groups = checkForGroupAccessByCurrentuser( groupName );

        if ( !groups.contains( groupName ) ) {
            throw new AccessDeniedException( "User doesn't have access to that group" );
        }

        if ( isReadableByGroup( s, groupName ) ) {
            return;
        }

        addGroupAuthority( s, BasePermission.READ, groupName );

    }

    /**
     * Remove read permissions; also removes write permissions.
     * 
     * @param s
     * @param groupName, with or without GROUP_
     * @throws AccessDeniedException
     */
    @Secured("ACL_SECURABLE_EDIT")
    @Transactional
    public void makeUnreadableByGroup( Securable s, String groupName ) throws AccessDeniedException {

        Collection<String> groups = checkForGroupAccessByCurrentuser( groupName );

        if ( !groups.contains( groupName ) && !isUserAdmin() ) {
            throw new AccessDeniedException( "User doesn't have access to that group: " + groupName );
        }

        List<GrantedAuthority> groupAuthorities = userManager.findGroupAuthorities( groupName );

        if ( groupAuthorities == null || groupAuthorities.isEmpty() ) {
            throw new IllegalStateException( "Group has no authorities" );
        }

        if ( groupAuthorities.size() > 1 ) {
            throw new UnsupportedOperationException( "Sorry, groups can only have a single authority" );
        }

        GrantedAuthority ga = groupAuthorities.get( 0 );

        String authority = ga.getAuthority();

        removeGrantedAuthority( s, BasePermission.READ, userManager.getRolePrefix() + authority );
        removeGrantedAuthority( s, BasePermission.WRITE, userManager.getRolePrefix() + authority );
    }

    /**
     * Remove write permissions. Leaves read permissions, if present.
     * 
     * @param s
     * @param groupName
     * @throws AccessDeniedException
     */
    @Secured("ACL_SECURABLE_EDIT")
    @Transactional
    public void makeUnwriteableByGroup( Securable s, String groupName ) throws AccessDeniedException {

        Collection<String> groups = checkForGroupAccessByCurrentuser( groupName );

        if ( !groups.contains( groupName ) && !isUserAdmin() ) {
            throw new AccessDeniedException( "User doesn't have access to that group" );
        }

        removeGrantedAuthority( s, BasePermission.WRITE, groupName );
    }

    /**
     * Adds write (and read) permissions.
     * 
     * @param s
     * @param groupName
     * @throws AccessDeniedException
     */
    @PreAuthorize("hasPermission(#s, write)")
    @Transactional
    public void makeWriteableByGroup( Securable s, String groupName ) throws AccessDeniedException {
        Collection<String> groups = checkForGroupAccessByCurrentuser( groupName );

        if ( !groups.contains( groupName ) ) {
            throw new AccessDeniedException( "User doesn't have access to that group" );
        }

        if ( isEditableByGroup( s, groupName ) ) {
            return;
        }

        addGroupAuthority( s, BasePermission.WRITE, groupName );
        addGroupAuthority( s, BasePermission.READ, groupName );
    }

    /**
     * @param s
     * @return list of userNames of users who can read the given securable.
     */
    @Secured("ACL_SECURABLE_EDIT")
    public Collection<String> readableBy( Securable s ) {
        Collection<String> allUsers = userManager.findAllUsers();

        Collection<String> result = new HashSet<String>();

        for ( String u : allUsers ) {
            if ( isViewableByUser( s, u ) ) {
                result.add( u );
            }
        }

        return result;
    }

    /**
     * @param userName
     * @param groupName
     */
    public void removeUserFromGroup( String userName, String groupName ) {
        this.userManager.removeUserFromGroup( userName, groupName );
    }

    /**
     * Change the 'owner' of an object to a specific user. Note that this doesn't support making the owner a
     * grantedAuthority.
     * 
     * @param s
     * @param userName
     */
    @Secured("GROUP_ADMIN")
    public void setOwner( Securable s, String userName ) {

        // make sure user exists and is enabled.
        UserDetails user = this.userManager.loadUserByUsername( userName );
        if ( !user.isEnabled() || !user.isAccountNonExpired() || !user.isAccountNonLocked() ) {
            throw new IllegalArgumentException( "User  " + userName + " has a disabled account" );
        }

        ObjectIdentity oi = this.objectIdentityRetrievalStrategy.getObjectIdentity( s );
        MutableAcl a = ( MutableAcl ) this.aclService.readAclById( oi );

        a.setOwner( new PrincipalSid( userName ) );

        this.aclService.updateAcl( a );

    }

    /**
     * Provide permission to the given group on the given securable.
     * 
     * @param s
     * @param permission
     * @param groupName e.g. "GROUP_JOESLAB"
     */
    private void addGroupAuthority( Securable s, Permission permission, String groupName ) {
        MutableAcl acl = getAcl( s );

        List<GrantedAuthority> groupAuthorities = userManager.findGroupAuthorities( groupName );

        if ( groupAuthorities == null || groupAuthorities.isEmpty() ) {
            throw new IllegalStateException( "Group has no authorities" );
        }

        if ( groupAuthorities.size() > 1 ) {
            throw new UnsupportedOperationException( "Sorry, groups can only have a single authority" );
        }

        GrantedAuthority ga = groupAuthorities.get( 0 );

        acl.insertAce( acl.getEntries().size(), permission,
                new GrantedAuthoritySid( userManager.getRolePrefix() + ga ), true );
        aclService.updateAcl( acl );
    }

    /**
     * @param s
     * @param permission
     * @param principal i.e. username
     */
    private void addPrincipalAuthority( Securable s, Permission permission, String principal ) {
        MutableAcl acl = getAcl( s );
        acl.insertAce( acl.getEntries().size(), permission, new PrincipalSid( principal ), true );
        aclService.updateAcl( acl );
    }

    /**
     * Check if the current user can access the given group.
     * 
     * @param groupName
     * @return
     */
    private Collection<String> checkForGroupAccessByCurrentuser( String groupName ) {
        if ( groupName.equals( AuthorityConstants.ADMIN_GROUP_NAME ) ) {
            throw new AccessDeniedException( "Attempt to mess with ADMIN privileges denied" );
        }
        Collection<String> groups = userManager.findGroupsForUser( userManager.getCurrentUsername() );
        return groups;
    }

    /**
     * @param s
     * @return
     */
    private MutableAcl getAcl( Securable s ) {
        ObjectIdentity oi = objectIdentityRetrievalStrategy.getObjectIdentity( s );

        try {
            return ( MutableAcl ) aclService.readAclById( oi );
        } catch ( NotFoundException e ) {
            return null;
        }
    }

    private Collection<String> getGroupsUserCanView() {
        Collection<String> groupNames;
        try {
            // administrator...
            groupNames = userManager.findAllGroups();
        } catch ( AccessDeniedException e ) {
            groupNames = userManager.findGroupsForUser( userManager.getCurrentUsername() );
        }
        return groupNames;
    }

    /**
     * @param securables
     * @return
     */
    private Map<ObjectIdentity, Securable> getObjectIdentities( Collection<? extends Securable> securables ) {
        Map<ObjectIdentity, Securable> result = new HashMap<ObjectIdentity, Securable>();
        for ( Securable s : securables ) {
            result.put( objectIdentityRetrievalStrategy.getObjectIdentity( s ), s );
        }
        return result;
    }

    private Map<Securable, Boolean> groupHasPermission( Collection<? extends Securable> securables,
            List<Permission> requiredPermissions, String groupName ) {
        Map<Securable, Boolean> result = new HashMap<Securable, Boolean>();
        Map<ObjectIdentity, Securable> objectIdentities = getObjectIdentities( securables );

        List<GrantedAuthority> auths = userManager.findGroupAuthorities( groupName );

        List<Sid> sids = new ArrayList<Sid>();
        for ( GrantedAuthority a : auths ) {
            GrantedAuthoritySid sid = new GrantedAuthoritySid( new GrantedAuthorityImpl( userManager.getRolePrefix()
                    + a.getAuthority() ) );
            sids.add( sid );
        }

        Map<ObjectIdentity, Acl> acls = aclService
                .readAclsById( new Vector<ObjectIdentity>( objectIdentities.keySet() ) );

        for ( ObjectIdentity oi : acls.keySet() ) {
            Acl a = acls.get( oi );
            try {
                result.put( objectIdentities.get( oi ), a.isGranted( requiredPermissions, sids, true ) );
            } catch ( NotFoundException ignore ) {
            }
        }
        return result;
    }

    /**
     * @param domainObject
     * @param requiredPermissions
     * @param groupName
     * @return
     */
    private boolean groupHasPermission( Securable domainObject, List<Permission> requiredPermissions, String groupName ) {
        ObjectIdentity objectIdentity = objectIdentityRetrievalStrategy.getObjectIdentity( domainObject );

        List<GrantedAuthority> auths = userManager.findGroupAuthorities( groupName );

        List<Sid> sids = new ArrayList<Sid>();
        for ( GrantedAuthority a : auths ) {
            GrantedAuthoritySid sid = new GrantedAuthoritySid( new GrantedAuthorityImpl( userManager.getRolePrefix()
                    + a.getAuthority() ) );
            sids.add( sid );
        }

        try {
            // Lookup only ACLs for SIDs we're interested in (this actually get them all)
            Acl acl = aclService.readAclById( objectIdentity, sids );
            // administrative mode = true
            return acl.isGranted( requiredPermissions, sids, true );
        } catch ( NotFoundException ignore ) {
            return false;
        }

    }

    /*
     * Private method that really doesn't work unless you are admin
     */
    private boolean hasPermission( Securable domainObject, List<Permission> requiredPermissions, String userName ) {

        // Obtain the OID applicable to the domain object
        ObjectIdentity objectIdentity = objectIdentityRetrievalStrategy.getObjectIdentity( domainObject );

        // Obtain the SIDs applicable to the principal
        UserDetails user = userManager.loadUserByUsername( userName );
        Authentication authentication = new UsernamePasswordAuthenticationToken( userName, user.getPassword(), user
                .getAuthorities() );
        List<Sid> sids = sidRetrievalStrategy.getSids( authentication );

        Acl acl = null;

        try {
            // Lookup only ACLs for SIDs we're interested in (this actually get them all)
            acl = aclService.readAclById( objectIdentity, sids );
            // administrative mode = true
            return acl.isGranted( requiredPermissions, sids, true );
        } catch ( NotFoundException ignore ) {
            return false;
        }
    }

    /**
     * @param acl
     * @return
     */
    private boolean isPrivate( Acl acl ) {

        /*
         * If the given Acl has anonymous permissions on it, then we can't be private.
         */
        for ( AccessControlEntry ace : acl.getEntries() ) {

            if ( !ace.getPermission().equals( BasePermission.READ ) ) continue;

            Sid sid = ace.getSid();
            if ( sid instanceof GrantedAuthoritySid ) {
                String grantedAuthority = ( ( GrantedAuthoritySid ) sid ).getGrantedAuthority();
                if ( grantedAuthority.equals( AuthorityConstants.IS_AUTHENTICATED_ANONYMOUSLY ) && ace.isGranting() ) {
                    return false;
                }
            }
        }

        /*
         * Even if the object is not private, it's parent might be and we might inherit that. Recursion happens here.
         */
        Acl parentAcl = acl.getParentAcl();
        if ( parentAcl != null && acl.isEntriesInheriting() ) {
            return isPrivate( parentAcl );
        }

        /*
         * We didn't find a granted authority on IS_AUTHENTICATED_ANONYMOUSLY
         */
        return true;

    }

    /**
     * @param acl
     * @return true if the ACL grants READ authority to at least one group that is not admin or agent.
     */
    private boolean isShared( Acl acl ) {
        for ( AccessControlEntry ace : acl.getEntries() ) {

            if ( !ace.getPermission().equals( BasePermission.READ ) ) continue;

            Sid sid = ace.getSid();
            if ( sid instanceof GrantedAuthoritySid ) {
                String grantedAuthority = ( ( GrantedAuthoritySid ) sid ).getGrantedAuthority();
                if ( grantedAuthority.startsWith( "GROUP_" ) && ace.isGranting() ) {

                    if ( grantedAuthority.equals( AuthorityConstants.AGENT_GROUP_AUTHORITY )
                            || grantedAuthority.equals( AuthorityConstants.ADMIN_GROUP_AUTHORITY ) ) {
                        continue;
                    }
                    return true;

                }
            }
        }

        /*
         * Even if the object is not private, it's parent might be and we might inherit that. Recursion happens here.
         */
        Acl parentAcl = acl.getParentAcl();
        if ( parentAcl != null && acl.isEntriesInheriting() ) {
            return isShared( parentAcl );
        }

        /*
         * We didn't find a granted authority for any group.
         */
        return false;
    }

    private void populateGroupsEditableBy( Map<Securable, Collection<String>> result, String groupName,
            Map<Securable, Boolean> groupHasPermission ) {
        for ( Securable s : groupHasPermission.keySet() ) {
            if ( groupHasPermission.get( s ) ) {
                if ( !result.containsKey( s ) ) {
                    result.put( s, new HashSet<String>() );
                }
                result.get( s ).add( groupName );
            }

        }
    }

    /**
     * @param s
     * @param permission
     * @param authority e.g. "GROUP_JOESLAB"
     */
    private void removeGrantedAuthority( Securable object, Permission permission, String authority ) {
        MutableAcl acl = getAcl( object );

        if ( acl == null ) {
            throw new IllegalArgumentException( "makePrivate is only valid for objects that have an ACL" );
        }

        List<Integer> toremove = new Vector<Integer>();
        for ( int i = 0; i < acl.getEntries().size(); i++ ) {
            AccessControlEntry entry = acl.getEntries().get( i );

            if ( !entry.getPermission().equals( permission ) ) {
                continue;
            }

            Sid sid = entry.getSid();
            if ( sid instanceof GrantedAuthoritySid ) {

                if ( ( ( GrantedAuthoritySid ) sid ).getGrantedAuthority().equals( authority ) ) {
                    toremove.add( i );
                }
            }
        }

        if ( toremove.size() > 1 ) {
            // problem is that as you delete them, the list changes size... so the indexes don't match...have to update
            // first.
            throw new UnsupportedOperationException( "Can't deal with case of more than one ACE to remove" );
        }

        if ( toremove.isEmpty() ) {
            log.warn( "No changes, didn't remove: " + authority );
        } else {

            for ( Integer j : toremove ) {
                acl.deleteAce( j );
            }

            aclService.updateAcl( acl );
        }
    }

}