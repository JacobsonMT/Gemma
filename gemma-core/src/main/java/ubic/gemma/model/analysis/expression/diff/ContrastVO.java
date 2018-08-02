/*
 * The Gemma project
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
package ubic.gemma.model.analysis.expression.diff;

/**
 * Helper object, not for general use.
 *
 * @author Paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class ContrastVO {

    private final Long factorValueId;

    private final Long id;

    private Double logFoldChange;

    private Double pvalue;

    private Long secondFactorValueId;

    public ContrastVO( Long id, Long factorValueId, Double logFoldchange, Double pvalue, Long secondFactorValueId ) {
        super();
        this.id = id;
        this.factorValueId = factorValueId; // can be null if it's a continuous factor
        this.logFoldChange = logFoldchange;
        this.secondFactorValueId = secondFactorValueId;

        this.pvalue = pvalue;
    }

    public Long getFactorValueId() {
        return factorValueId;
    }

    public Long getId() {
        return id;
    }

    public Double getLogFoldChange() {
        return logFoldChange;
    }

    public Double getPvalue() {
        return pvalue;
    }

    public Long getSecondFactorValueId() {
        return secondFactorValueId;
    }

}