/*
 * The gemma project
 *
 * Copyright (c) 2013 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.analysis.preprocess.batcheffects;

/**
 * provide some basic information about the strength of a batch effect.
 *
 * @author Paul
 */
public class BatchEffectDetails {

    private final boolean hasBatchInformation;
    private final boolean dataWasBatchCorrected;
    private Integer component;
    private double componentVarianceProportion;
    private double pvalue;

    public BatchEffectDetails( boolean hasBatchInformation, boolean dataWasBatchCorrected ) {
        this.hasBatchInformation = hasBatchInformation;
        this.dataWasBatchCorrected = dataWasBatchCorrected;
    }

    public Integer getComponent() {
        return component;
    }

    public void setComponent( Integer component ) {
        this.component = component;
    }

    public double getComponentVarianceProportion() {
        return componentVarianceProportion;
    }

    public void setComponentVarianceProportion( double componentVarianceProportion ) {
        this.componentVarianceProportion = componentVarianceProportion;
    }

    public boolean getDataWasBatchCorrected() {
        return this.dataWasBatchCorrected;
    }

    public double getPvalue() {
        return pvalue;
    }

    public void setPvalue( double pvalue ) {
        this.pvalue = pvalue;
    }

    @Override
    public String toString() {
        return String.format( "BatchEffectDetails [pvalue=%.2g, component=%d, varFraction=%.2f]", pvalue, component,
                componentVarianceProportion );
    }

    public boolean hasNoBatchInfo() {
        return !hasBatchInformation;
    }

}
