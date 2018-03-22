/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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

package ubic.gemma.model.expression.biomaterial;

import ubic.gemma.model.common.AbstractAuditable;

/**
 * @author Paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public abstract class Treatment extends AbstractAuditable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 7148213633537076048L;
    private Integer orderApplied = 1;
    private ubic.gemma.model.common.measurement.Measurement actionMeasurement;
    private ubic.gemma.model.common.description.Characteristic action;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public Treatment() {
    }

    public ubic.gemma.model.common.description.Characteristic getAction() {
        return this.action;
    }

    public void setAction( ubic.gemma.model.common.description.Characteristic action ) {
        this.action = action;
    }

    public ubic.gemma.model.common.measurement.Measurement getActionMeasurement() {
        return this.actionMeasurement;
    }

    public void setActionMeasurement( ubic.gemma.model.common.measurement.Measurement actionMeasurement ) {
        this.actionMeasurement = actionMeasurement;
    }

    /**
     * @return The order in which this treatment was applied to the biomaterial, relative to the other treatments.
     */
    public Integer getOrderApplied() {
        return this.orderApplied;
    }

    public void setOrderApplied( Integer orderApplied ) {
        this.orderApplied = orderApplied;
    }

    public static final class Factory {

        public static ubic.gemma.model.expression.biomaterial.Treatment newInstance() {
            return new ubic.gemma.model.expression.biomaterial.TreatmentImpl();
        }

    }

}