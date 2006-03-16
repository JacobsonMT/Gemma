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
package ubic.gemma.loader.util.persister;

import ubic.gemma.model.common.Auditable;

/**
 * A service that knows how to persist Gemma-domain objects. Associations are checked and persisted in turn if needed.
 * Where appropriate, objects are only created anew if they don't already exist in the database, according to rules
 * documented elsewhere.
 * 
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @spring.bean id="persisterHelper"
 */
public class PersisterHelper extends ExpressionPersister {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.loader.loaderutils.Loader#create(ubic.gemma.model.genome.Gene)
     */
    @SuppressWarnings("unchecked")
    public Object persist( Object entity ) {

        if ( entity instanceof Auditable ) {
            Auditable a = ( Auditable ) entity;
            a.setAuditTrail( persistAuditTrail( a.getAuditTrail() ) );
            return super.persist( a );
        }
        return super.persist( entity );
    }

}
