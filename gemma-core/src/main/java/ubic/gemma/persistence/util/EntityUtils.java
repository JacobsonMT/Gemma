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
package ubic.gemma.persistence.util;

import gemma.gsec.model.Securable;
import gemma.gsec.util.SecurityUtil;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.LockOptions;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.proxy.HibernateProxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.*;

/**
 * @author paul
 */
public class EntityUtils {

    /**
     * Expert only. Put the given entity into the Session, with LockMode.NONE
     * Based on idea from https://forum.hibernate.org/viewtopic.php?p=2284826#p2284826
     *
     * @param session Hibernate Session (use factory.getCurrentSession())
     * @param obj     the entity
     * @param clazz   the class type of the persisted entity. Don't use obj.getClass() as this might return a proxy type.
     * @param id      identifier of the obj
     */
    public static void attach( Session session, Object obj, Class<?> clazz, Long id ) {
        if ( obj == null || id == null )
            return;
        if ( !session.isOpen() )
            throw new IllegalArgumentException( "Illegal attempt to use a closed session" );
        if ( !session.contains( obj ) ) {

            Object oldObj = session.get( clazz, id );

            if ( oldObj != null ) {
                session.evict( oldObj );
            }
        }
        session.buildLockRequest( LockOptions.NONE ).lock( obj );
    }

    public static Long getId( Object entity ) {
        return getId( entity, "getId" );
    }

    public static Long getId( Object entity, String methodName ) {
        try {
            Method m = entity.getClass().getMethod( methodName );
            return ( Long ) m.invoke( entity );
        } catch ( SecurityException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | IllegalArgumentException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Given a set of entities, create a map of their ids to the entities.
     *
     * @param entities where id is called "id"
     */
    public static <T> Map<Long, T> getIdMap( Collection<? extends T> entities ) {
        Map<Long, T> result = new HashMap<>();

        for ( T object : entities ) {
            result.put( getId( object, "getId" ), object );
        }

        return result;

    }

    /**
     * @param methodName accessor e.g. "getId"
     */
    public static <T> Map<Long, T> getNestedIdMap( Collection<? extends T> entities, String nestedProperty,
            String methodName ) {
        Map<Long, T> result = new HashMap<>();

        for ( T object : entities ) {
            try {
                result.put( getId( FieldUtils.readField(object, nestedProperty, true), methodName ), object );
            } catch ( IllegalAccessException e ) {
                throw new RuntimeException( e );
            }
        }

        return result;
    }

    /**
     * @param methodName accessor e.g. "getId"
     */
    public static <T> Map<Long, T> getIdMap( Collection<? extends T> entities, String methodName ) {
        Map<Long, T> result = new HashMap<>();

        for ( T object : entities ) {
            result.put( getId( object, methodName ), object );
        }

        return result;
    }

    /**
     * @return either a list (if entities was a list) or collection of ids.
     */
    public static Collection<Long> getIds( Collection<?> entities ) {

        Collection<Long> r;

        if ( List.class.isAssignableFrom( entities.getClass() ) ) {
            r = new ArrayList<>();
        } else {
            r = new HashSet<>();
        }

        for ( Object object : entities ) {
            r.add( getId( object ) );
        }
        return r;
    }

    /**
     * Convenience method for pushing an ID into a collection (encapsulates a common idiom)
     *
     * @return a collection with one item in it.
     */
    public static Collection<Long> getIds( Object entity ) {
        Collection<Long> r;
        r = new HashSet<>();
        r.add( getId( entity ) );
        return r;
    }

    public static Class<?> getImplClass( Class<?> type ) {
        String canonicalName = type.getName();
        try {
            return Class.forName( canonicalName.endsWith( "Impl" ) ? type.getName() : type.getName() + "Impl" );
        } catch ( ClassNotFoundException e ) {
            // OLDCODE throw new RuntimeException( "No 'impl' class for " + type.getCanonicalName() + " found" );
            // not all our mapped classes end with Impl any more.
            return type;
        }
    }

    /**
     * Expert only. Must be called within a session? Not sure why this is necessary. Obtain the implementation for a
     * proxy. If target is not an instanceof HibernateProxy, target is returned.
     *
     * @param target The object to be unproxied.
     * @return the underlying implementation.
     */
    public static Object getImplementationForProxy( Object target ) {
        if ( isProxy( target ) ) {
            HibernateProxy proxy = ( HibernateProxy ) target;
            return proxy.getHibernateLazyInitializer().getImplementation();
        }
        return target;
    }

    /**
     * @return true if the target is a hibernate proxy.
     */
    public static boolean isProxy( Object target ) {
        return target instanceof HibernateProxy;
    }

    /**
     * Expert use only. Used to expose some ACL information to the DAO layer (normally this happens in an interceptor).
     *
     * @param securedClass Securable type
     * @param ids          to be filtered
     * @param showPublic   also show public items (won't work if showOnlyEditable is true)
     * @return filtered IDs, at the very least limited to those that are readable by the current user
     */
    public static Collection<Long> securityFilterIds( Class<? extends Securable> securedClass, Collection<Long> ids,
            boolean showOnlyEditable, boolean showPublic, Session sess ) {

        if ( ids.isEmpty() )
            return ids;
        if ( SecurityUtil.isUserAdmin() ) {
            return ids;
        }

        /*
         * Find groups user is a member of
         */

        String userName = SecurityUtil.getCurrentUsername();

        boolean isAnonymous = SecurityUtil.isUserAnonymous();

        if ( isAnonymous && ( showOnlyEditable || !showPublic ) ) {
            return new HashSet<>();
        }

        String queryString = "select aoi.OBJECT_ID";
        queryString += " from ACLOBJECTIDENTITY aoi";
        queryString += " join ACLENTRY ace on ace.OBJECTIDENTITY_FK = aoi.ID ";
        queryString += " join ACLSID sid on sid.ID = aoi.OWNER_SID_FK ";
        queryString += " where aoi.OBJECT_ID in (:ids)";
        queryString += " and aoi.OBJECT_CLASS = :clazz and ";
        queryString += addGroupAndUserNameRestriction( showOnlyEditable, showPublic );

        // will be empty if anonymous
        //noinspection unchecked
        Collection<String> groups = sess.createQuery(
                "select ug.name from UserGroup ug inner join ug.groupMembers memb where memb.userName = :user" )
                .setParameter( "user", userName ).list();

        Query query = sess.createSQLQuery( queryString ).setParameter( "clazz", securedClass.getName() )
                .setParameterList( "ids", ids );

        if ( queryString.contains( ":groups" ) ) {
            query.setParameterList( "groups", groups );
        }

        if ( queryString.contains( ":userName" ) ) {
            query.setParameter( "userName", userName );
        }

        //noinspection unchecked
        List<BigInteger> r = query.list();
        Set<Long> rl = new HashSet<>();
        for ( BigInteger bi : r ) {
            rl.add( bi.longValue() );
        }

        if ( !ids.containsAll( rl ) ) {
            // really an assertion, but being extra-careful
            throw new SecurityException( "Security filter failure" );
        }

        return rl;
    }

    /**
     * Have to add 'and' to start of this if it's a later clause
     * Author: nicolas with fixes to generalize by paul, same code appears in the PhenotypeAssociationDaoImpl
     *
     * @param showOnlyEditable only show those the user has access to edit
     * @param showPublic       also show public items (wont work if showOnlyEditable is true)
     */
    public static String addGroupAndUserNameRestriction( boolean showOnlyEditable, boolean showPublic ) {

        String sqlQuery = "";

        if ( !SecurityUtil.isUserAnonymous() ) {

            if ( showPublic && !showOnlyEditable ) {
                sqlQuery += "  ((sid.PRINCIPAL = :userName";
            } else {
                sqlQuery += "  (sid.PRINCIPAL = :userName";
            }

            // if ( !groups.isEmpty() ) { // is it possible to be empty? If they are non-anonymous it will not be, there
            // // will at least be GROUP_USER? Or that doesn't count?
            sqlQuery += " or (ace.SID_FK in (";
            // SUBSELECT
            sqlQuery += " select sid.ID from USER_GROUP ug ";
            sqlQuery += " join GROUP_AUTHORITY ga on ug.ID = ga.GROUP_FK ";
            sqlQuery += " join ACLSID sid on sid.GRANTED_AUTHORITY=CONCAT('GROUP_', ga.AUTHORITY) ";
            sqlQuery += " where ug.name in (:groups) ";
            if ( showOnlyEditable ) {
                sqlQuery += ") and ace.MASK = 2) "; // 2 = read-writable
            } else {
                sqlQuery += ") and (ace.MASK = 1 or ace.MASK = 2)) "; // 1 = read only
            }
            // }
            sqlQuery += ") ";

            if ( showPublic && !showOnlyEditable ) {
                // publicly readable data.
                sqlQuery += "or (ace.SID_FK = 4 and ace.MASK = 1)) "; // 4 =IS_AUTHENTICATED_ANONYMOUSLY
            }

        } else if ( showPublic && !showOnlyEditable ) {
            // publicly readable data
            sqlQuery += " (ace.SID_FK = 4 and ace.MASK = 1) "; // 4 = IS_AUTHENTICATED_ANONYMOUSLY
        }

        return sqlQuery;
    }

}
