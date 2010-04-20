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
package ubic.gemma.web.controller.common.auditAndSecurity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetService;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService;
import ubic.gemma.model.common.Describable;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetService;
import ubic.gemma.security.SecurityService;
import ubic.gemma.security.authentication.UserDetailsImpl;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.util.AuthorityConstants;
import ubic.gemma.web.remote.EntityDelegator;

/**
 * Manages data-level security (ie. can make data private).
 * 
 * @author keshav
 * @version $Id$
 */
@Controller
public class SecurityController {

    private static Log log = LogFactory.getLog( SecurityController.class );

    @Autowired
    private ExpressionExperimentService expressionExperimentService = null;

    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;

    @Autowired
    private GeneCoexpressionAnalysisService geneCoexpressionAnalysisService;

    @Autowired
    private GeneSetService geneSetService = null;

    @Autowired
    private SecurityService securityService = null;

    @Autowired
    private UserManager userManager = null;

    /**
     * AJAX
     * 
     * @param userName
     * @param groupName
     * @return
     */
    public boolean addUserToGroup( String userName, String groupName ) {

        if ( userManager.userExists( userName ) ) {
            securityService.addUserToGroup( userName, groupName );
        } else if ( userManager.userWithEmailExists( userName ) ) {
            /*
             * Have to send them an invitation...
             */
            throw new UnsupportedOperationException( "Sorry, you need the username" );
        }

        /*
         * TODO: send the user an email.
         */
        return true;
    }

    /**
     *AJAX
     * 
     * @param groupName
     * @return
     */
    public String createGroup( String groupName ) {

        if ( StringUtils.isBlank( groupName ) || groupName.length() < 3 || !StringUtils.isAlpha( groupName ) ) {
            throw new IllegalArgumentException(
                    "Group name must contain only letters and must be at least 3 letters long." );
        }

        securityService.createGroup( groupName );
        return groupName;
    }

    /**
     * @param groupName
     */
    public void deleteGroup( String groupName ) {

        if ( !this.getGroupsUserCanEdit().contains( groupName ) ) {
            throw new IllegalArgumentException( "You don't have permission to modify that group" );
        }
        /*
         * Additional checks for ability to delete group handled by ss.
         */

        securityService.deleteGroup( groupName );
    }

    /**
     * AJAX
     * 
     * @return List of group names the user can add members to and/or give permissions on objects to.
     */
    public Collection<UserGroupValueObject> getAvailableGroups() {
        Collection<String> editableGroups = getGroupsUserCanEdit();
        Collection<String> groupsUserIsIn = getGroupsForCurrentUser();

        Collection<String> allGroups = new HashSet<String>();
        try {
            // administrator...
            allGroups = userManager.findAllGroups();
        } catch ( AccessDeniedException e ) {
            allGroups = groupsUserIsIn;
        }

        Collection<UserGroupValueObject> result = new HashSet<UserGroupValueObject>();
        for ( String g : allGroups ) {
            UserGroupValueObject gvo = new UserGroupValueObject();
            gvo.setCanEdit( editableGroups.contains( g ) );
            gvo.setMember( groupsUserIsIn.contains( g ) );
            gvo.setGroupName( g );
            result.add( gvo );
        }

        return result;
    }

    /**
     * Return a list of principals that is users
     * 
     * @return SidValueObjects
     */
    public Collection<SidValueObject> getAvailablePrincipalSids() {
        List<SidValueObject> results = new ArrayList<SidValueObject>();
        try {
            for ( Sid s : securityService.getAvailableSids() ) {
                SidValueObject sv = new SidValueObject( s );
                if ( sv.isPrincipal() ) {
                    results.add( sv );
                }
            }
        } catch ( AccessDeniedException e ) {
            results.clear();
        }

        Collections.sort( results );
        return results;
    }

    /**
     * AJAX, but administrator-only!geneCoexpressionAnalysisService
     * 
     * @return
     */
    public Collection<SidValueObject> getAvailableSids() {
        List<SidValueObject> results = new ArrayList<SidValueObject>();
        try {
            for ( Sid s : securityService.getAvailableSids() ) {
                SidValueObject sv = new SidValueObject( s );

                results.add( sv );
            }
        } catch ( AccessDeniedException e ) {
            results.clear();
        }

        Collections.sort( results );

        return results;
    }

    /**
     * @param groupName
     * @return
     */
    public Collection<UserValueObject> getGroupMembers( String groupName ) {
        Collection<UserValueObject> result = new HashSet<UserValueObject>();

        // happens if user is not in any displayed groups.
        if ( StringUtils.isBlank( groupName ) ) {
            return result;
        }
        List<String> usersInGroup = userManager.findUsersInGroup( groupName );

        for ( String userName : usersInGroup ) {
            UserDetails details = userManager.loadUserByUsername( userName );

            UserValueObject uvo = new UserValueObject();

            uvo.setUserName( details.getUsername() );

            if ( details instanceof UserDetailsImpl ) {
                uvo.setEmail( ( ( UserDetailsImpl ) details ).getEmail() );
            }

            uvo.setCurrentGroup( groupName );
            uvo.setInGroup( true );

            uvo.setAllowModification( true );

            /*
             * You can't remove yourself from a group, or remove users from the USER group.
             */
            if ( userName.equals( userManager.getCurrentUsername() )
                    || groupName.equals( AuthorityConstants.USER_GROUP_NAME ) ) {
                uvo.setAllowModification( false );
            }

            result.add( uvo );
        }

        return result;
    }

    /**
     * AJAX
     * 
     * @param ed
     * @return
     */
    public SecurityInfoValueObject getSecurityInfo( EntityDelegator ed ) {

        Securable s = getSecurable( ed );

        SecurityInfoValueObject result = securable2VO( s );

        return result;
    }

    /**
     * AJAX
     * 
     * @param currentGroup A specific group that we're focusing on. Can be null. Used to populate client-side checkboxes
     *        to show permissions.
     * @param privateOnly Only show data that are private (non-publicly readable); otherwise show all the data for the
     *        user. This option is probably of most use to administrators.
     * @return
     */
    public Collection<SecurityInfoValueObject> getUsersData( String currentGroup, boolean privateOnly ) {
        Collection<Securable> secs = new HashSet<Securable>();

        // Add experiments.
        secs.addAll( getUsersExperiments( privateOnly ) );

        // Add Analyses
        secs.addAll( getUsersAnalyses( privateOnly ) );

        Collection<SecurityInfoValueObject> result = securables2VOs( secs, currentGroup );

        result.addAll( securables2VOs( getUsersGeneGroups( privateOnly ), currentGroup ) );

        result.addAll( securables2VOs( getUsersExperimentSets( privateOnly ), currentGroup ) );

        /*
         * add other types of securables here.
         */

        return result;
    }

    /**
     * @param privateOnly
     * @return
     */
    public Collection<Securable> getUsersGeneGroups( boolean privateOnly ) {
        Collection<Securable> secs = new HashSet<Securable>();

        Collection<GeneSet> geneSets = geneSetService.loadMyGeneSets();
        if ( privateOnly ) {
            try {
                secs.addAll( securityService.choosePrivate( geneSets ) );
            } catch ( AccessDeniedException e ) {
                // okay, they just aren't allowed to see those.
            }
        } else {
            secs.addAll( geneSets );
        }

        return secs;
    }

    /**
     * AJAX
     * 
     * @param ed
     * @param groupName
     * @return
     */
    public boolean makeGroupReadable( EntityDelegator ed, String groupName ) {
        Securable s = getSecurable( ed );
        securityService.makeReadableByGroup( s, groupName );
        return true;
    }

    /**
     * AJAX
     * 
     * @param ed
     * @param groupName
     * @return
     */
    public boolean makeGroupWriteable( EntityDelegator ed, String groupName ) {
        Securable s = getSecurable( ed );
        securityService.makeWriteableByGroup( s, groupName );
        return true;
    }

    /**
     * AJAX
     * 
     * @param ed
     * @return
     */
    public boolean makePrivate( EntityDelegator ed ) {
        Securable s = getSecurable( ed );
        securityService.makePrivate( s );
        return true;
    }

    /**
     * AJAX
     * 
     * @param ed
     * @return
     */
    public boolean makePublic( EntityDelegator ed ) {
        Securable s = getSecurable( ed );
        securityService.makePublic( s );
        return true;
    }

    /**
     * AJAX
     * 
     * @param ed
     * @param groupName
     * @return
     */
    public boolean removeGroupReadable( EntityDelegator ed, String groupName ) {
        Securable s = getSecurable( ed );
        securityService.makeUnreadableByGroup( s, groupName );
        return true;
    }

    /**
     * AJAX
     * 
     * @param ed
     * @param groupName
     * @return
     */
    public boolean removeGroupWriteable( EntityDelegator ed, String groupName ) {
        Securable s = getSecurable( ed );
        securityService.makeUnwriteableByGroup( s, groupName );
        return true;
    }

    /**
     * AJAX
     * 
     * @param userNames
     * @param groupName
     * @return
     */
    public boolean removeUsersFromGroup( Collection<String> userNames, String groupName ) {
        for ( String userName : userNames ) {

            if ( userName.equals( "administrator" ) && groupName.equals( AuthorityConstants.ADMIN_GROUP_NAME ) ) {
                throw new IllegalArgumentException( "You cannot remove the administrator from the ADMIN group!" );
            }

            if ( groupName.equals( AuthorityConstants.USER_GROUP_NAME ) ) {
                throw new IllegalArgumentException( "You cannot remove users from the USER group!" );
            }

            securityService.removeUserFromGroup( userName, groupName );
        }
        return true;
    }

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param geneSetService
     */
    public void setGeneSetService( GeneSetService geneSetService ) {
        this.geneSetService = geneSetService;
    }

    /**
     * @param securityService
     */
    public void setSecurityService( SecurityService securityService ) {
        this.securityService = securityService;
    }

    /**
     * @param userManager the userManager to set
     */
    public void setUserManager( UserManager userManager ) {
        this.userManager = userManager;
    }

    /**
     * AJAX
     * 
     * @param settings
     */
    public void updatePermission( SecurityInfoValueObject settings ) {
        EntityDelegator sd = new EntityDelegator();
        sd.setId( settings.getEntityId() );
        sd.setClassDelegatingFor( settings.getEntityClazz() );
        Securable s = getSecurable( sd );

        if ( settings.isPubliclyReadable() ) {
            securityService.makePublic( s );
        } else {
            securityService.makePrivate( s );
        }

        try {
            if ( settings.getOwner().isPrincipal() ) {
                securityService.makeOwnedByUser( s, settings.getOwner().getAuthority() );
            } else {
                // throw new UnsupportedOperationException( "Sorry, not supported - user is " + settings.getOwner() );
                log.warn( "Can't make user " + settings.getOwner() + " owner, not implemented" );
                // securityService.makeOwnedByGroup( s, settings.getOwner().getAuthority() );
            }
        } catch ( AccessDeniedException e ) {
            // okay, only works if you are administrator.
        }

        /*
         * This works in one of two ways. If settings.currentGroup is non-null, we just update the permissions for that
         * group - this may leave them unchanged. Otherwise, we update them all based on
         * groupsThatCanRead/groupsThatCanWrite
         */
        String currentGroupName = settings.getCurrentGroup();
        if ( StringUtils.isNotBlank( currentGroupName )
                && !( currentGroupName.equals( AuthorityConstants.ADMIN_GROUP_NAME ) || currentGroupName
                        .equals( AuthorityConstants.AGENT_GROUP_NAME ) ) ) {

            if ( !getGroupsUserCanEdit().contains( currentGroupName ) ) {
                throw new AccessDeniedException( "Access denied to permissions for group=" + currentGroupName );

            }

            Boolean readable = settings.isCurrentGroupCanRead();
            Boolean writeable = settings.isCurrentGroupCanWrite();
            if ( readable == null || writeable == null ) {
                throw new IllegalArgumentException( "Must provide settings for 'currentGroup'" );
            }

            if ( readable ) {
                securityService.makeReadableByGroup( s, currentGroupName );
            } else {

                securityService.makeUnreadableByGroup( s, currentGroupName );
            }

            if ( writeable ) {
                // if writable should be readable
                securityService.makeReadableByGroup( s, currentGroupName );
                securityService.makeWriteableByGroup( s, currentGroupName );
            } else {
                securityService.makeUnwriteableByGroup( s, currentGroupName );
            }

        } else {
            /*
             * Remove all group permissions - we'll set them back to what was requested. Exception: we don't allow
             * changes to admin or agent permissions by this route.
             */
            for ( String groupName : getGroupsUserCanEdit() ) {

                if ( groupName.equals( AuthorityConstants.ADMIN_GROUP_NAME )
                        || groupName.equals( AuthorityConstants.AGENT_GROUP_NAME ) ) {
                    // never changes this.
                    continue;
                }

                securityService.makeUnreadableByGroup( s, groupName );
                securityService.makeUnwriteableByGroup( s, groupName );
            }

            /*
             * Add selected ones back
             */
            for ( String reader : settings.getGroupsThatCanRead() ) {
                if ( reader.equals( AuthorityConstants.ADMIN_GROUP_NAME )
                        || reader.equals( AuthorityConstants.AGENT_GROUP_NAME ) ) {
                    // never changes this.
                    continue;
                }

                securityService.makeReadableByGroup( s, reader );
            }
            for ( String writer : settings.getGroupsThatCanWrite() ) {
                if ( writer.equals( AuthorityConstants.ADMIN_GROUP_NAME )
                        || writer.equals( AuthorityConstants.AGENT_GROUP_NAME ) ) {
                    // never changes this.
                    continue;
                }
                // when it is writable it should be readable
                securityService.makeReadableByGroup( s, currentGroupName );
                securityService.makeWriteableByGroup( s, writer );
            }
        }

        log.info( "Updated permissions on " + s );
    }

    /**
     * AJAX
     * 
     * @param settings
     */
    public void updatePermissions( Collection<SecurityInfoValueObject> settings ) {
        for ( SecurityInfoValueObject so : settings ) {
            this.updatePermission( so );
        }
    }

    /**
     * @return groups the user can edit (not just the ones they are in!)
     */
    private Collection<String> getGroupsForCurrentUser() {
        return userManager.findGroupsForUser( userManager.getCurrentUsername() );
    }

    /**
     * @param userName
     * @return
     */
    private Collection<String> getGroupsUserCanEdit() {
        return securityService.getGroupsUserCanEdit( userManager.getCurrentUsername() );
    }

    /**
     * @param ed
     * @return
     * @throws IllegalArgumentException if the Securable cannot be loaded
     */
    private Securable getSecurable( EntityDelegator ed ) {
        String classDelegatingFor = ed.getClassDelegatingFor();

        Class<?> clazz;
        Securable s = null;
        try {
            clazz = Class.forName( classDelegatingFor );
        } catch ( ClassNotFoundException e1 ) {
            throw new RuntimeException( e1 );
        }
        if ( ExpressionExperiment.class.isAssignableFrom( clazz ) ) {
            s = expressionExperimentService.load( ed.getId() );
        } else if ( GeneCoexpressionAnalysis.class.isAssignableFrom( clazz ) ) {
            s = geneCoexpressionAnalysisService.load( ed.getId() );
        } else if ( GeneSet.class.isAssignableFrom( clazz ) ) {
            s = geneSetService.load( ed.getId() );
        } else {
            throw new UnsupportedOperationException( clazz + " not supported by security controller yet" );
        }

        if ( s == null ) {
            throw new IllegalArgumentException( "Entity does not exist or user does not have access." );
        }
        return s;
    }

    /**
     * @param privateOnly
     * @return
     */
    private Collection<Securable> getUsersAnalyses( boolean privateOnly ) {
        Collection<Securable> secs = new HashSet<Securable>();

        Collection<GeneCoexpressionAnalysis> analyses = geneCoexpressionAnalysisService.loadMyAnalyses();
        if ( privateOnly ) {
            try {
                secs.addAll( securityService.choosePrivate( analyses ) );
            } catch ( AccessDeniedException e ) {
                // okay, they just aren't allowed to see those.
            }
        } else {
            secs.addAll( analyses );
        }

        return secs;
    }

    /**
     * @param privateOnly
     * @return
     */
    private Collection<Securable> getUsersExperiments( boolean privateOnly ) {
        Collection<ExpressionExperiment> ees = expressionExperimentService.loadMyExpressionExperiments();

        Collection<Securable> secs = new HashSet<Securable>();

        if ( privateOnly ) {
            try {
                secs.addAll( securityService.choosePrivate( ees ) );
            } catch ( AccessDeniedException e ) {
                // okay, they just aren't allowed to see those.
            }
        } else {
            secs.addAll( ees );
        }
        return secs;
    }

    /**
     * @param privateOnly
     * @return
     */
    private Collection<Securable> getUsersExperimentSets( boolean privateOnly ) {
        Collection<Securable> secs = new HashSet<Securable>();

        Collection<ExpressionExperimentSet> analyses = expressionExperimentSetService.loadMySets();
        if ( privateOnly ) {
            try {
                secs.addAll( securityService.choosePrivate( analyses ) );
            } catch ( AccessDeniedException e ) {
                // okay, they just aren't allowed to see those.
            }
        } else {
            secs.addAll( analyses );
        }
        return secs;
    }

    /**
     * Create a fully-populated value object for the given securable.
     * 
     * @param s
     * @return
     */
    private SecurityInfoValueObject securable2VO( Securable s ) {
        /*
         * Problem: this is quite slow.
         */
        boolean isPublic = securityService.isPublic( s );
        boolean isShared = securityService.isShared( s );

        SecurityInfoValueObject result = new SecurityInfoValueObject( s );

        result.setAvailableGroups( getGroupsForCurrentUser() );
        result.setPubliclyReadable( isPublic );
        result.setGroupsThatCanRead( securityService.getGroupsReadableBy( s ) );
        result.setGroupsThatCanWrite( securityService.getGroupsEditableBy( s ) );
        result.setShared( isShared );
        result.setOwner( new SidValueObject( securityService.getOwner( s ) ) );

        if ( Describable.class.isAssignableFrom( s.getClass() ) ) {
            result.setEntityDescription( ( ( Describable ) s ).getDescription() );
            result.setEntityName( ( ( Describable ) s ).getName() );
        }
        return result;
    }

    /**
     * @param securables
     * @param currentGroup A specific group that we're focusing on. Can be null
     * @return
     */
    private Collection<SecurityInfoValueObject> securables2VOs( Collection<? extends Securable> securables,
            String currentGroup ) {

        Collection<SecurityInfoValueObject> result = new HashSet<SecurityInfoValueObject>();

        if ( securables.isEmpty() ) {
            return result;
        }

        /*
         * Fast computations out-of-loop
         */
        Collection<String> groupsForCurrentUser = getGroupsForCurrentUser();
        Map<Securable, Boolean> privacy = securityService.arePrivate( securables );
        Map<Securable, Boolean> sharedness = securityService.areShared( securables );
        Map<Securable, Sid> owners = securityService.getOwners( securables );
        Map<Securable, Collection<String>> groupsReadableBy = securityService.getGroupsReadableBy( securables );
        Map<Securable, Collection<String>> groupsEditableBy = securityService.getGroupsEditableBy( securables );

        // int i = 0; // TESTING
        for ( Securable s : securables ) {
            SecurityInfoValueObject vo = new SecurityInfoValueObject( s );
            vo.setCurrentGroup( currentGroup );
            vo.setAvailableGroups( groupsForCurrentUser );
            vo.setPubliclyReadable( !privacy.get( s ) );
            vo.setShared( sharedness.get( s ) );
            vo.setOwner( new SidValueObject( owners.get( s ) ) );
            vo.setGroupsThatCanRead( groupsReadableBy.get( s ) );
            vo.setGroupsThatCanWrite( groupsEditableBy.get( s ) );

            vo.setEntityClazz( s.getClass().getName() );

            if ( currentGroup != null ) {
                vo.setCurrentGroupCanRead( groupsReadableBy.get( s ).contains( currentGroup ) );
                vo.setCurrentGroupCanWrite( groupsEditableBy.get( s ).contains( currentGroup ) );
            }

            if ( Describable.class.isAssignableFrom( s.getClass() ) ) {
                // vo.setEntityDescription( ( ( Describable ) s ).getDescription() );
                vo.setEntityName( ( ( Describable ) s ).getName() );
            }

            if ( ExpressionExperiment.class.isAssignableFrom( s.getClass() ) ) {
                vo.setEntityShortName( ( ( ExpressionExperiment ) s ).getShortName() );
            }

            result.add( vo );
            // if ( ++i > 10 ) break; // TESTING
        }
        return result;
    }

}
