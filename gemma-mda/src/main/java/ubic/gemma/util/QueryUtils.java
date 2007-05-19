/*
 * The Gemma project
 * 
 * Copyright (c) 2007 Columbia University
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
package ubic.gemma.util;

import java.util.Collection;

import org.hibernate.Session;
import org.springframework.orm.hibernate3.SessionFactoryUtils;

/**
 * Convenience methods for doing queries
 * 
 * @author Paul
 * @version $Id$
 */
public class QueryUtils {

    /**
     * @param queryString with no parameters
     * @return a single object
     * @throws InvalidDataAccessResourceUsageException if more than one object is returned
     * @throws DataAccessException on other errors
     */
    public static Object query( Session session, final String queryString ) {
        try {
            org.hibernate.Query queryObject = session.createQuery( queryString );
            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance was found when executing query --> '" + queryString + "'" );
                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }

            return result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw SessionFactoryUtils.convertHibernateAccessException( ex );
        }
    }

    /**
     * @param id
     * @param queryString with parameter "id"
     * @return a single Object, even if the query actually returns more.
     */
    public static Object queryById( Session session, Long id, final String queryString ) {
        try {
            org.hibernate.Query queryObject = session.createQuery( queryString );
            queryObject.setParameter( "id", id );
            queryObject.setMaxResults( 1 );
            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                result = results.iterator().next();
            }

            return result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw SessionFactoryUtils.convertHibernateAccessException( ex );
        }
    }

    /**
     * @param id with parameter "id"
     * @param queryString
     * @return
     */
    public static Collection queryByIdReturnCollection( Session session, Long id, final String queryString ) {
        try {
            org.hibernate.Query queryObject = session.createQuery( queryString );
            queryObject.setParameter( "id", id );
            return queryObject.list();
        } catch ( org.hibernate.HibernateException ex ) {
            throw SessionFactoryUtils.convertHibernateAccessException( ex );
        }
    }
    
    /**
     * @param id with parameter "id"
     * @param queryString
     * @return
     */
    public static Collection queryByIdReturnCollection( Session session, Long id, final String queryString, int limit ) {
        try {
            org.hibernate.Query queryObject = session.createQuery( queryString );
            queryObject.setParameter( "id", id );
            queryObject.setMaxResults( limit );
            return queryObject.list();
        } catch ( org.hibernate.HibernateException ex ) {
            throw SessionFactoryUtils.convertHibernateAccessException( ex );
        }
    }

    /**
     * @param ids
     * @param queryString with parameter "ids"
     * @return a single Object
     */
    public static Object queryByIds( Session session, Collection<Long> ids, final String queryString ) {
        try {
            org.hibernate.Query queryObject = session.createQuery( queryString );
            queryObject.setParameterList( "ids", ids );
            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance was found when executing query --> '" + queryString + "'" );
                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }

            return result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw SessionFactoryUtils.convertHibernateAccessException( ex );
        }
    }

    /**
     * Run a native SQL query with no parameters.
     * 
     * @param queryString
     * @return Collection of records (Object[])
     */
    @SuppressWarnings("unchecked")
    public static Collection<Object[]> nativeQuery( Session session, final String queryString ) {
        try {
            org.hibernate.Query queryObject = session.createSQLQuery( queryString );
            return queryObject.list();
        } catch ( org.hibernate.HibernateException ex ) {
            throw SessionFactoryUtils.convertHibernateAccessException( ex );
        }

    }

    /**
     * Run a native SQL query with a single 'id' parameter
     * 
     * @param id
     * @param queryString
     * @return Collection of records (Object[])
     */
    @SuppressWarnings("unchecked")
    public static Collection<Object[]> nativeQueryById( Session session, Long id, final String queryString ) {
        try {
            org.hibernate.Query queryObject = session.createSQLQuery( queryString );
            queryObject.setLong( "id", id );
            return queryObject.list();
        } catch ( org.hibernate.HibernateException ex ) {
            throw SessionFactoryUtils.convertHibernateAccessException( ex );
        }
    }

}
