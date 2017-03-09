/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.persistence.BaseDao;

import java.util.Collection;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.AuditTrail
 */
public interface AuditTrailDao extends BaseDao<AuditTrail> {

    /**
     * Add the given event to the audit trail of the given AbstractAuditable entity. For efficiency, it is best to set the audit
     * event performer before passing in.
     */
    AuditEvent addEvent( Auditable auditable, AuditEvent auditEvent );

    /**
     * get all entities of the class specified that have the event type specified in their audit trails
     */
    Collection<Auditable> getEntitiesWithEvent( Class<Auditable> entityClass,
            Class<? extends AuditEventType> auditEventClass );
}
