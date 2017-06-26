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
package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.Auditable;

import java.util.Collection;
import java.util.Date;

/**
 * Spring Service base class for <code>AuditEventService</code>, provides access to all services and entities referenced
 * by this service.
 *
 * @see AuditEventService
 */
public abstract class AuditEventServiceBase implements AuditEventService {

    final AuditEventDao auditEventDao;

    public AuditEventServiceBase( AuditEventDao auditEventDao ) {
        this.auditEventDao = auditEventDao;
    }

    /**
     * @see AuditEventService#getNewSinceDate(Date)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Auditable> getNewSinceDate( final Date date ) {
        return this.handleGetNewSinceDate( date );
    }

    /**
     * @see AuditEventService#getUpdatedSinceDate(Date)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Auditable> getUpdatedSinceDate( final Date date ) {
        return this.handleGetUpdatedSinceDate( date );
    }

    /**
     * Performs the core logic for {@link #getNewSinceDate(Date)}
     */
    protected abstract Collection<Auditable> handleGetNewSinceDate( Date date );

    /**
     * Performs the core logic for {@link #getUpdatedSinceDate(Date)}
     */
    protected abstract Collection<Auditable> handleGetUpdatedSinceDate( Date date );

}