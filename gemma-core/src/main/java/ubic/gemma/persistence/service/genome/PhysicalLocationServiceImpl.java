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
package ubic.gemma.persistence.service.genome;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.genome.PhysicalLocation;

/**
 * @author paul
 * @version $Id$
 */
@Service
public class PhysicalLocationServiceImpl implements PhysicalLocationService {

    @Autowired
    private PhysicalLocationDao physicalLocationDao;

    PhysicalLocationDao getPhysicalLocationDao() {
        return physicalLocationDao;
    }

    @Override
    @Transactional(readOnly = true)
    public void thaw( PhysicalLocation physicalLocation ) {
        this.getPhysicalLocationDao().thaw( physicalLocation );

    }

}
