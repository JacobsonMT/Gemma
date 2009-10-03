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

import org.apache.commons.lang.StringUtils;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.AuditEvent
 */
public class AuditEventImpl extends ubic.gemma.model.common.auditAndSecurity.AuditEvent {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 6713721089643871509L;

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.AuditEvent#toString()
     */
    @Override
    public java.lang.String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append( this.getDate() );
        buf.append( " by " );
        buf.append( this.getPerformer().getUserName() );
        if ( !StringUtils.isEmpty( this.getNote() ) ) {
            buf.append( "\n" );
            buf.append( this.getNote() );
        }
        if ( !StringUtils.isEmpty( this.getDetail() ) ) {
            buf.append( "\n" );
            buf.append( this.getDetail() );
        }
        return buf.toString();
    }

}