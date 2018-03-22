/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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

package ubic.gemma.model.analysis.expression.diff;

import java.io.Serializable;
import java.util.Collection;

/**
 * @author frances
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
public class GeneDifferentialExpressionMetaAnalysisDetailValueObject implements Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 3868004995989355452L;

    private Integer numGenesAnalyzed;

    private Collection<IncludedResultSetInfoValueObject> includedResultSetsInfo;
    private Collection<GeneDifferentialExpressionMetaAnalysisResultValueObject> results;

    public Collection<IncludedResultSetInfoValueObject> getIncludedResultSetsInfo() {
        return this.includedResultSetsInfo;
    }

    public void setIncludedResultSetsInfo( Collection<IncludedResultSetInfoValueObject> includedResultSetsInfo ) {
        this.includedResultSetsInfo = includedResultSetsInfo;
    }

    public Integer getNumGenesAnalyzed() {
        return this.numGenesAnalyzed;
    }

    public void setNumGenesAnalyzed( Integer numGenesAnalyzed ) {
        this.numGenesAnalyzed = numGenesAnalyzed;
    }

    public Collection<GeneDifferentialExpressionMetaAnalysisResultValueObject> getResults() {
        return this.results;
    }

    public void setResults( Collection<GeneDifferentialExpressionMetaAnalysisResultValueObject> results ) {
        this.results = results;
    }
}
