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
package ubic.gemma.persistence.service.common.quantitationtype;

import java.util.List;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.quantitationtype.QuantitationType;

/**
 * @author kelsey
 * @version $Id$
 */
public interface QuantitationTypeService {

    /**
     * 
     */
    @Secured({ "GROUP_USER" })
    public QuantitationType create( QuantitationType quantitationType );

    /**
     * 
     */
    public QuantitationType find( QuantitationType quantitationType );

    /**
     * 
     */
    @Secured({ "GROUP_USER" })
    public QuantitationType findOrCreate( QuantitationType quantitationType );

    /**
     * 
     */
    public QuantitationType load( java.lang.Long id );

    /**
     * 
     */
    public java.util.Collection<QuantitationType> loadAll();

    /**
     * 
     */
    @Secured({ "GROUP_USER" })
    public void remove( QuantitationType quantitationType );

    /**
     * 
     */
    @Secured({ "GROUP_USER" })
    public void update( QuantitationType quantitationType );

    @Secured({ "GROUP_USER" })
    public List<QuantitationType> loadByDescription( String description );

}
