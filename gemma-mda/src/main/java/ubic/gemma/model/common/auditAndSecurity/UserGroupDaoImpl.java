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
package ubic.gemma.model.common.auditAndSecurity;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.UserGroup
 */
@Repository
public class UserGroupDaoImpl extends ubic.gemma.model.common.auditAndSecurity.UserGroupDaoBase {

    protected final Log log = LogFactory.getLog( getClass() );

    /**
     * @param sessionFactory
     */
    @Autowired
    public UserGroupDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.common.auditAndSecurity.UserGroupDao#addAuthority(ubic.gemma.model.common.auditAndSecurity.UserGroup
     * , java.lang.String)
     */
    public void addAuthority( UserGroup group, String authority ) {

        for ( GroupAuthority ga : group.getAuthorities() ) {
            if ( ga.getAuthority().equals( authority ) ) {
                log.warn( "Group already has authority " + authority );
                return;
            }
        }

        GroupAuthority ga = GroupAuthority.Factory.newInstance();
        ga.setAuthority( authority );

        group.getAuthorities().add( ga );

        this.getHibernateTemplate().update( group );

    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.common.auditAndSecurity.UserGroupDao#addToGroup(ubic.gemma.model.common.auditAndSecurity.UserGroup
     * , ubic.gemma.model.common.auditAndSecurity.User)
     */
    public void addToGroup( UserGroup group, User user ) {
        group.getGroupMembers().add( user );
        this.getHibernateTemplate().update( group );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.common.auditAndSecurity.UserGroupDao#findGroupsForUser(ubic.gemma.model.common.auditAndSecurity
     * .User)
     */
    @SuppressWarnings("unchecked")
    public Collection<UserGroup> findGroupsForUser( User user ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select ug from UserGroupImpl ug inner join ug.groupMembers memb where memb = :user", "user", user );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.common.auditAndSecurity.UserGroupDao#removeAuthority(ubic.gemma.model.common.auditAndSecurity
     * .UserGroup, java.lang.String)
     */
    public void removeAuthority( UserGroup group, String authority ) {

        for ( Iterator<GroupAuthority> iterator = group.getAuthorities().iterator(); iterator.hasNext(); ) {
            GroupAuthority ga = iterator.next();
            if ( ga.getAuthority().equals( authority ) ) {
                iterator.remove();
            }
        }

        this.getHibernateTemplate().update( group );
    }
}