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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;

import org.acegisecurity.Authentication;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.acl.basic.BasicAclExtendedDao;
import org.acegisecurity.acl.basic.NamedEntityObjectIdentity;
import org.acegisecurity.acl.basic.SimpleAclEntry;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.userdetails.UserDetails;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import ubic.gemma.model.common.Securable;
import ubic.gemma.model.common.SecurableDao;
import ubic.gemma.util.SecurityUtil;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean name="securityService"
 * @spring.property name="basicAclExtendedDao" ref="basicAclExtendedDao"
 * @spring.property name="securableDao" ref="securableDao"
 */
public class SecurityService {

    private Log log = LogFactory.getLog( SecurityService.class );

    private static final String ACCESSOR_PREFIX = "get";

    private BasicAclExtendedDao basicAclExtendedDao = null;
    private SecurableDao securableDao = null;
    private UnsecuredSet unsecuredClasses = new UnsecuredSet( null );

    public static final String ADMIN_AUTHORITY = "admin";
    public static final int PUBLIC_MASK = SimpleAclEntry.READ_WRITE;
    public static final int PRIVATE_MASK = SimpleAclEntry.NOTHING;

    /**
     * Changes the acl_permission of the object to either administrator/PRIVATE (mask=0), or read-write/PUBLIC (mask=6).
     * 
     * @param object
     * @param mask
     * @param visited A Collection of objects already visited. This is need so objects in a bi-directional relationship
     *        are not processed twice.
     */
    public void setPermissions( Object object, int mask, Collection<Object> visited ) {

        log.debug( "Changing acl of object " + object + "." );

        if ( mask != PUBLIC_MASK && mask != PRIVATE_MASK ) {
            throw new RuntimeException( "Supported masks are " + PRIVATE_MASK + " (PRIVATE) and " + PUBLIC_MASK
                    + " (PUBLIC)." );
        }

        SecurityContext securityCtx = SecurityContextHolder.getContext();
        Authentication authentication = securityCtx.getAuthentication();
        Object principal = authentication.getPrincipal();

        if ( object instanceof Securable ) {
            if ( !visited.contains( object ) ) {
                visited.add( object );
                processAssociations( object, mask, authentication, principal, visited );
            } else {
                log.debug( "Object " + object.getClass() + " already visited." );
            }
        } else {
            throw new RuntimeException( "Object not Securable.  Cannot change permissions for object of type "
                    + object.getClass().getName() + "." );
        }

    }

    /**
     * @param targetObject
     * @param mask
     * @param authentication
     * @param principal
     * @param visited A Collection of objects already visited. This is need so objects in a bi-directional relationship
     *        are not processed twice.
     */
    private void processAssociations( Object targetObject, int mask, Authentication authentication, Object principal,
            Collection<Object> visited ) {

        Class clazz = targetObject.getClass();
        Method[] methods = clazz.getMethods();

        for ( Method method : methods ) {
            String name = method.getName();
            if ( StringUtils.startsWithIgnoreCase( name, ACCESSOR_PREFIX ) ) {
                Class returnType = method.getReturnType();
                if ( unsecuredClasses.contains( returnType )
                        || ( returnType != java.util.Collection.class && !Securable.class.isAssignableFrom( returnType
                                .getClass() ) ) ) {
                    continue;
                }

                try {
                    if ( returnType == java.util.Collection.class ) {

                        Collection returnedCollection = ( Collection ) clazz.getMethod( name, null ).invoke(
                                targetObject, null );
                        if ( returnedCollection.isEmpty() ) continue;

                        /* check if an object in collection is in unsecuredCol */
                        Object objInCol = returnedCollection.iterator().next();
                        if ( unsecuredClasses.contains( objInCol.getClass() )
                                || !Securable.class.isAssignableFrom( objInCol.getClass() ) ) {
                            continue;
                        } else {
                            /* if object in collection is not in unsecuredCol and is a Securable, process */
                            Iterator iter = returnedCollection.iterator();
                            while ( iter.hasNext() ) {
                                Object ob = iter.next();
                                log.debug( "process " + ob );
                                setPermissions( ob, mask, visited );// recursive
                            }
                        }
                    } else {
                        Object ob = clazz.getMethod( name, null ).invoke( targetObject, null );

                        ob = SecurityUtil.getImplementationFromProxy( ob );

                        if ( ob == null || unsecuredClasses.contains( ob.getClass() )
                                || !Securable.class.isAssignableFrom( ob.getClass() )
                                || ( ( Securable ) ob ).getId() == null ) {
                            continue;
                        }
                        setPermissions( ob, mask, visited );// recursive
                    }
                } catch ( Exception e ) {
                    throw new RuntimeException( e );
                }
            }

        }
        String recipient = configureWhoToRunAs( targetObject, mask, authentication, principal );
        if ( recipient != null ) changeMask( targetObject, mask, recipient );
    }

    /**
     * @param object
     * @param mask
     * @param recipient
     */
    private void changeMask( Object object, int mask, String recipient ) {
        try {
            basicAclExtendedDao.changeMask( new NamedEntityObjectIdentity( object ), recipient, mask );
        } catch ( Exception e ) {
            throw new RuntimeException( "Problems changing mask of " + object, e );
        }
    }

    /**
     * Runs as the recipient (in acl_permission) if the principal does not match the recipient. Returns null if
     * principal is not an administrator.
     * 
     * @param object
     * @param mask
     * @param authentication
     * @param principal
     */
    private String configureWhoToRunAs( Object object, int mask, Authentication authentication, Object principal ) {
        assert principal != null;
        Securable securedObject = ( Securable ) object;
        /* id of target object */
        Long id = securedObject.getId();

        /* id of acl_object_identity */
        Long objectIdentityId = securableDao.getAclObjectIdentityId( object, id );

        String recipient = null;
        if ( objectIdentityId == null ) return recipient;

        recipient = securableDao.getRecipient( objectIdentityId );

        if ( recipient == null ) {
            throw new IllegalStateException( "No recipient for object " + objectIdentityId );
        }

        if ( isUserAdmin() ) {
            if ( !recipient.equals( principal.toString() ) ) {
                RunAsManager runAsManager = new RunAsManager();
                runAsManager.buildRunAs( object, authentication, recipient );

            } else {
                recipient = principal.toString();
            }
        } else {
            throw new RuntimeException( "User '" + principal
                    + "' is not authorized to execute this method, you must be an 'administrator'." );
        }
        return recipient;
    }

    /**
     * Returns the username of the current principal (user). This can be invoked from anywhere (ie. in a controller,
     * service, dao), and does not rely on any external security features. This is useful for determining who is the
     * current user.
     * 
     * @return String
     */
    public static String getPrincipalName() {

        Object obj = getPrincipal();

        String username = null;
        if ( obj instanceof UserDetails ) {
            username = ( ( UserDetails ) obj ).getUsername();
        } else {
            username = obj.toString();
        }

        return username;
    }

    /**
     * Returns the username of the current principal (user). This can be invoked from anywhere (ie. in a controller,
     * service, dao), and does not rely on any external security features. The return type should checked if it is an
     * instance of UserDetails and typecast to access information about the current user (ie. GrantedAuthority).
     * 
     * @return Object
     */
    public static Object getPrincipal() {
        return getAuthentication().getPrincipal();
    }

    /**
     * Returns the Authentication object from the SecurityContextHolder.
     * 
     * @return Authentication
     */
    public static Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if ( authentication == null ) throw new RuntimeException( "Null authentication object" );

        return authentication;
    }

    /**
     * Returns true if the current user has admin authority.
     * 
     * @return true if the current user has admin authority
     */
    public static boolean isUserAdmin() {
        GrantedAuthority[] authorities = getAuthentication().getAuthorities();
        assert authorities != null;
        for ( GrantedAuthority authority : authorities ) {
            if ( authority.getAuthority().equals( ADMIN_AUTHORITY ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param aclDao the aclDao to set
     */
    public void setBasicAclExtendedDao( BasicAclExtendedDao basicAclExtendedDao ) {
        this.basicAclExtendedDao = basicAclExtendedDao;
    }

    /**
     * @param securableDao the securableDao to set
     */
    public void setSecurableDao( SecurableDao securableDao ) {
        this.securableDao = securableDao;
    }

}
