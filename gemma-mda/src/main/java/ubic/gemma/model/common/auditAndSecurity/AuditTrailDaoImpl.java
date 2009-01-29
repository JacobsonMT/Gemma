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

import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.userdetails.UserDetails;

import ubic.gemma.model.common.Auditable;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao
 * @author pavlidis
 * @version $Id$
 */
public class AuditTrailDaoImpl extends ubic.gemma.model.common.auditAndSecurity.AuditTrailDaoBase {

    private static Log log = LogFactory.getLog( AuditTrailDaoImpl.class.getName() );

    /**
     * 
     */
    @Override
    protected AuditEvent handleAddEvent( final Auditable auditable, final AuditEvent auditEvent ) throws Exception {

        if ( auditEvent.getAction() == null ) {
            throw new IllegalArgumentException( "auditEvent was missing a required field" );
        }

        if ( auditEvent.getDate() == null ) {
            auditEvent.setDate( Calendar.getInstance().getTime() );
        }

        if ( auditEvent.getPerformer() == null ) {
            User user = getUser(); // could be null, if anonymous.
            auditEvent.setPerformer( user );
        }

        HibernateTemplate templ = this.getHibernateTemplate();
        templ.executeWithNativeSession( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                // session.lock( auditable, LockMode.NONE );
                session.update( auditable );
                if ( !Hibernate.isInitialized( auditable ) ) Hibernate.initialize( auditable );
                session.persist( auditEvent );
                auditable.getAuditTrail().addEvent( auditEvent );
                session.flush();
                session.evict( auditable );
                return null;
            }
        } );

        assert auditEvent.getId() != null;
        assert auditable.getAuditTrail().getEvents().size() > 0;
        return auditEvent;
    }

    /**
     * 
     */
    @Override
    protected void handleThaw( final Auditable auditable ) {
        if ( auditable == null ) return;
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.executeWithNativeSession( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.update( auditable );
                if ( auditable.getAuditTrail() == null ) return null;
                if ( auditable.getAuditTrail().getEvents() == null ) return null;
                thaw( auditable.getAuditTrail() );
                session.evict( auditable );
                return null;
            }
        } );

    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditTrailDao#thaw(ubic.gemma.model.common.auditAndSecurity.AuditTrail)
     */
    @Override
    protected void handleThaw( final ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail ) {
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.executeWithNativeSession( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.lock( auditTrail, LockMode.NONE );
                Hibernate.initialize( auditTrail );
                if ( auditTrail.getEvents() == null ) return null;
                for ( AuditEvent ae : auditTrail.getEvents() ) {
                    Hibernate.initialize( ae );
                    if ( ae.getPerformer() != null ) {
                        User performer = ( User ) session.get( UserImpl.class, ae.getPerformer().getId() );
                        Hibernate.initialize( performer );
                        session.evict( performer );
                    } else {
                        log.warn( "No performer for audit event: id=" + ae.getId() );
                    }
                }
                session.evict( auditTrail );
                return null;
            }
        } );

    }

    /**
     * @return
     */
    private String getPrincipalName() {
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
     * @return
     */
    @SuppressWarnings("unchecked")
    private User getUser() {
        String name = getPrincipalName();
        assert name != null; // might be anonymous

        /*
         * Note: this name is defined in the applicationContext-security.xml file. Normally audit events would not be
         * added by 'anonymous' using the methods in this class, but this allows the possibility.
         */
        if ( name.equals( "anonymous" ) ) {
            return null;
        }

        String queryString = "from ContactImpl where userName=:userName";
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString, "userName", name );

        assert results.size() == 1;
        Object result = results.iterator().next();
        return ( User ) result;
    }

}