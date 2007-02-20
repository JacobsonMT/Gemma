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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.acegisecurity.Authentication;
import org.acegisecurity.acl.basic.BasicAclExtendedDao;
import org.acegisecurity.acl.basic.NamedEntityObjectIdentity;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.userdetails.UserDetails;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.springframework.util.StringUtils;

import ubic.gemma.model.association.RelationshipImpl;
import ubic.gemma.model.common.Securable;
import ubic.gemma.model.common.SecurableDao;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailImpl;
import ubic.gemma.model.common.description.DatabaseEntryImpl;
import ubic.gemma.model.common.description.LocalFileImpl;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeImpl;
import ubic.gemma.model.expression.bioAssay.BioAssayImpl;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorImpl;
import ubic.gemma.model.expression.designElement.CompositeSequenceImpl;
import ubic.gemma.model.expression.experiment.ExperimentalFactorImpl;
import ubic.gemma.model.genome.GeneImpl;
import ubic.gemma.model.genome.TaxonImpl;
import ubic.gemma.model.genome.biosequence.BioSequenceImpl;
import ubic.gemma.model.genome.gene.GeneAliasImpl;
import ubic.gemma.model.genome.gene.GeneProductImpl;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean name="securityService"
 * @spring.property name="basicAclExtendedDao" ref="basicAclExtendedDao"
 * @spring.property name="securableDao" ref="securableDao"
 */
public class SecurityService {

    private static final String NET_SF = "net.sf";

    private Log log = LogFactory.getLog( SecurityService.class );

    private BasicAclExtendedDao basicAclExtendedDao = null;
    private SecurableDao securableDao = null;

    private final int PUBLIC_MASK = 6;
    private final int PRIVATE_MASK = 0;
    private static final String ADMINISTRATOR = "administrator";
    private static final String ACCESSOR_PREFIX = "get";

    /**
     * For some types of objects, we don't put permissions on them directly, but on the containing object. Example:
     * reporter - we secure the arrayDesign, but not the reporter.
     */
    private static final Collection<Class> unsecuredClasses = new HashSet<Class>();

    /*
     * Classes to skip because they aren't secured. Either these are always "public" objects, or they are secured by
     * composition. In principle this shouldn't needed in most cases because the methods for the corresponding services
     * are not interccepted anyway.
     */
    static {// TODO use parent classes and interfaces (like DesignElement.class)
        // unsecuredClasses.add( DataVectorImpl.class );
        unsecuredClasses.add( DesignElementDataVectorImpl.class );
        unsecuredClasses.add( DatabaseEntryImpl.class );
        unsecuredClasses.add( BioSequenceImpl.class );
        unsecuredClasses.add( RelationshipImpl.class );
        // unsecuredClasses.add( DesignElementImpl.class );
        unsecuredClasses.add( CompositeSequenceImpl.class );
        unsecuredClasses.add( TaxonImpl.class );
        unsecuredClasses.add( GeneImpl.class );
        unsecuredClasses.add( GeneProductImpl.class );
        unsecuredClasses.add( GeneAliasImpl.class );
        unsecuredClasses.add( QuantitationTypeImpl.class );
        // these are not of type Securable
        unsecuredClasses.add( AuditTrailImpl.class );
        unsecuredClasses.add( DatabaseEntryImpl.class );
        unsecuredClasses.add( LocalFileImpl.class );
        // TODO remove these
        unsecuredClasses.add( BioAssayImpl.class );
        unsecuredClasses.add( ExperimentalFactorImpl.class );
    }

    /**
     * Changes the acl_permission of the object to either administrator/PRIVATE (mask=0), or read-write/PUBLIC (mask=6).
     * 
     * @param object
     * @param mask
     */
    public void changePermission( Object object, int mask ) {

        log.debug( "Changing acl of object " + object + "." );

        if ( mask != PUBLIC_MASK && mask != PRIVATE_MASK ) {
            throw new RuntimeException( "Supported masks are " + PRIVATE_MASK + " (PRIVATE) and " + PUBLIC_MASK
                    + "(PUBLIC)." );
        }

        SecurityContext securityCtx = SecurityContextHolder.getContext();
        Authentication authentication = securityCtx.getAuthentication();
        Object principal = authentication.getPrincipal();

        if ( object instanceof Securable ) {

            processAssociations( object, mask, authentication, principal );
        } else {
            throw new RuntimeException( "Object not Securable.  Cannot change permissions for object of type " + object
                    + "." );
        }

    }

    /**
     * @param targetObject
     * @param mask
     * @param authentication
     * @param principal
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws SecurityException
     * @throws IllegalArgumentException
     */
    private void processAssociations( Object targetObject, int mask, Authentication authentication, Object principal ) {

        Class clazz = targetObject.getClass();
        Method[] methods = clazz.getMethods();

        for ( Method method : methods ) {
            String name = method.getName();
            if ( StringUtils.startsWithIgnoreCase( name, ACCESSOR_PREFIX ) ) {
                Class returnType = method.getReturnType();

                if ( returnType.getName().equalsIgnoreCase( LazyInitializer.class.getName() ) ) {
                    continue;
                }
                if ( returnType.getName().contains( NET_SF ) ) {
                    continue;
                }
                if ( returnType.getName().equalsIgnoreCase( String.class.getName() ) ) {
                    continue;
                }
                if ( returnType.getName().equalsIgnoreCase( Integer.class.getName() ) ) {
                    continue;
                }
                if ( returnType.getName().equalsIgnoreCase( Long.class.getName() ) ) {
                    continue;
                }
                if ( returnType.getName().equalsIgnoreCase( Class.class.getName() ) ) {
                    continue;
                }

                try {
                    if ( returnType == java.util.Collection.class ) {

                        Collection returnedCollection = ( Collection ) clazz.getMethod( name, null ).invoke(
                                targetObject, null );
                        if ( returnedCollection.isEmpty() ) continue;

                        /* check if an object in collection is in unsecuredCol */
                        Object objInCol = returnedCollection.iterator().next();
                        if ( unsecuredClasses.contains( objInCol.getClass() ) ) {
                            continue;
                        } else {
                            /* if object in collectin is not in unsecuredCol, process */
                            Iterator iter = returnedCollection.iterator();
                            while ( iter.hasNext() ) {
                                Object ob = iter.next();
                                log.debug( "process " + ob );
                                changePermission( ob, mask );// recursive
                            }
                        }
                    } else {
                        Object ob = clazz.getMethod( name, null ).invoke( targetObject, null );

                        ob = getImplementationFromProxy( ob );

                        if ( ob == null || unsecuredClasses.contains( ob.getClass() )
                                || ( ( Securable ) ob ).getId() == null ) continue;
                        changePermission( ob, mask );// recursive
                    }
                } catch ( Exception e ) {
                    throw new RuntimeException( "Error is: " + e );
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

        Securable securedObject = ( Securable ) object;
        /* id of target object */
        Long id = securedObject.getId();

        /* id of acl_object_identity */
        Long objectIdentityId = securableDao.getAclObjectIdentityId( object, id );

        String recipient = null;
        if ( objectIdentityId == null ) return recipient;

        recipient = securableDao.getRecipient( objectIdentityId );

        if ( principal.toString().equals( ADMINISTRATOR ) ) {
            if ( !recipient.equals( principal.toString() ) ) {
                RunAsManager runAsManager = new RunAsManager();
                runAsManager.buildRunAs( object, authentication, recipient );

            } else {
                recipient = principal.toString();
            }
        } else {
            throw new RuntimeException( "User " + principal + " not authorized to execute this method." );
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
        Object obj = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

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
        return SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /**
     * Returns the Authentication object from the SecurityContextHolder.
     * 
     * @return Authentication
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Returns the Implementation object from the HibernateProxy. If target is not an instanceof HibernateProxy, target
     * is returned.
     * 
     * @param target
     * @return Object
     */
    public static Object getImplementationFromProxy( Object target ) {
        // TODO move method in a utility as it is accesseded by daos (SeurableDaoImpl)
        if ( target instanceof HibernateProxy ) {
            HibernateProxy proxy = ( HibernateProxy ) target;
            return proxy.getHibernateLazyInitializer().getImplementation();
        }

        return target;
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
