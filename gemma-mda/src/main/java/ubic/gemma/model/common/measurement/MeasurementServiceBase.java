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
package ubic.gemma.model.common.measurement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Service base class for <code>MeasurementService</code>, provides access to all services and entities
 * referenced by this service.
 * 
 * @see MeasurementService
 */
public abstract class MeasurementServiceBase implements MeasurementService {

    @Autowired
    private MeasurementDao measurementDao;

    /**
     * @see MeasurementService#create(Measurement)
     */
    @Override
    @Transactional
    public Measurement create( final Measurement measurement ) {
        return this.handleCreate( measurement );

    }

    /**
     * Gets the reference to <code>measurement</code>'s DAO.
     */
    MeasurementDao getMeasurementDao() {
        return this.measurementDao;
    }

    /**
     * Performs the core logic for {@link #create(Measurement)}
     */
    protected abstract Measurement handleCreate( Measurement measurement );

}