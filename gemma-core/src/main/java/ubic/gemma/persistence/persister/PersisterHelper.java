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
package ubic.gemma.persistence.persister;

import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.persistence.service.common.auditAndSecurity.CurationDetailsDao;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;

/**
 * A service that knows how to persist Gemma-domain objects. Associations are checked and persisted in turn if needed.
 * Where appropriate, objects are only created anew if they don't already exist in the database, according to rules
 * documented elsewhere.
 *
 * @author pavlidis
 * @author keshav
 */
@Service
public class PersisterHelper extends RelationshipPersister {

    @Autowired
    private CurationDetailsDao curationDetailsDao;

    @Autowired
    public PersisterHelper( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    @Override
    @Transactional
    public Object persist( Object entity ) {
        try {

            this.getSession().setFlushMode( FlushMode.COMMIT );

            if ( entity instanceof Auditable ) {
                Auditable auditable = ( Auditable ) entity;

                if ( auditable.getAuditTrail() == null ) {
                    auditable.setAuditTrail( AuditTrail.Factory.newInstance() );
                }

                auditable.setAuditTrail( persistAuditTrail( auditable.getAuditTrail() ) );

                if ( entity instanceof Curatable ) {
                    this.initializeCurationDetails( ( Curatable ) entity );
                }
            }

            return super.persist( entity );
        } finally {
            this.getSession().setFlushMode( FlushMode.AUTO );
        }
    }

    private void initializeCurationDetails( Curatable curatable ) {
        if ( curatable.getCurationDetails() == null ) {
            CurationDetails cd = this.curationDetailsDao.create();
            curatable.setCurationDetails( cd );
        }
    }

}
