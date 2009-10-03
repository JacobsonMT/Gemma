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

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.common.measurement.MeasurementService</code>, provides access to
 * all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.common.measurement.MeasurementService
 */
public abstract class MeasurementServiceBase implements ubic.gemma.model.common.measurement.MeasurementService {

    private ubic.gemma.model.common.measurement.MeasurementDao measurementDao;

    /**
     * @see ubic.gemma.model.common.measurement.MeasurementService#create(ubic.gemma.model.common.measurement.Measurement)
     */
    public ubic.gemma.model.common.measurement.Measurement create(
            final ubic.gemma.model.common.measurement.Measurement measurement ) {
        try {
            return this.handleCreate( measurement );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.measurement.MeasurementServiceException(
                    "Error performing 'ubic.gemma.model.common.measurement.MeasurementService.create(ubic.gemma.model.common.measurement.Measurement measurement)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>measurement</code>'s DAO.
     */
    public void setMeasurementDao( ubic.gemma.model.common.measurement.MeasurementDao measurementDao ) {
        this.measurementDao = measurementDao;
    }

    /**
     * Gets the reference to <code>measurement</code>'s DAO.
     */
    protected ubic.gemma.model.common.measurement.MeasurementDao getMeasurementDao() {
        return this.measurementDao;
    }

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.common.measurement.Measurement)}
     */
    protected abstract ubic.gemma.model.common.measurement.Measurement handleCreate(
            ubic.gemma.model.common.measurement.Measurement measurement ) throws java.lang.Exception;

}