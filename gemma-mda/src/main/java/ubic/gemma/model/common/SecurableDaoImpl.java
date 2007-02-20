/*
 * The Gemma project.
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
package ubic.gemma.model.common;

import org.hibernate.proxy.HibernateProxy;

/**
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.common.Securable
 */
public class SecurableDaoImpl extends ubic.gemma.model.common.SecurableDaoBase {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.SecurableDaoBase#getRecipient(java.lang.Long)
     */
    public String getRecipient( Long id ) {

        String queryString = "SELECT recipient FROM acl_permission WHERE id= " + id;

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createSQLQuery( queryString );
            return ( String ) queryObject.uniqueResult();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.SecurableDaoBase#getAclObjectIdentityId(java.lang.Object, java.lang.Long)
     */
    public Long getAclObjectIdentityId( Object target, Long id ) {

        String object = target.getClass().getName() + ":" + id;

        if ( target instanceof HibernateProxy ) {

            HibernateProxy proxy = ( HibernateProxy ) target;
            Object implementation = proxy.getHibernateLazyInitializer().getImplementation();
            object = implementation.getClass().getName() + ":" + id;
        }

        String queryString = "SELECT id FROM acl_object_identity WHERE object_identity=\'" + object + "\'";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createSQLQuery( queryString );
            Integer result = ( Integer ) queryObject.uniqueResult();
            Long longId = new Long( result );

            return longId;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }
}