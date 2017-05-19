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
package ubic.gemma.core.tasks.analysis.diffex;

import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalysisConfig;
import ubic.gemma.core.analysis.expression.diff.DifferentialExpressionAnalyzerServiceImpl.AnalysisType;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.core.tasks.Task;
import ubic.gemma.persistence.util.Settings;

import java.util.Collection;

/**
 * A command object to be used by spaces.
 * 
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisTaskCommand extends TaskCommand {

    private static final long serialVersionUID = 1L;

    /**
     * Proposed analysis type. If null the system tries to figure it out.
     */
    private AnalysisType analysisType;

    private ExpressionExperiment expressionExperiment = null;

    /**
     * The factors to actually use in the analysis. If null the system tries to figure it out.
     */
    private Collection<ExperimentalFactor> factors;

    private boolean forceAnalysis = false;

    /**
     * Whether interactions among the factors should be included. The implementation may limit this to two-way
     * interactions for only up to two factors, so this may not have the effect desired.
     */
    private boolean includeInteractions = false;

    /**
     * Whether to moderate test statistics via empirical Bayes
     */
    private boolean moderateStatistics = false;

    private Double qvalueThreshold = DifferentialExpressionAnalysisConfig.DEFAULT_QVALUE_THRESHOLD;

    private ExperimentalFactor subsetFactor;

    private DifferentialExpressionAnalysis toRedo;

    @Deprecated
    private boolean updateStatsOnly = true;

    public DifferentialExpressionAnalysisTaskCommand( ExpressionExperiment ee ) {
        super();
        this.expressionExperiment = ee;
    }

    /**
     * @param ee
     * @param toRedo
     * @param updateAnalysis if true, the analysis is updated. If false, only the summary statistics are updated (e.g.,
     *        the pvalue distribution ).
     */
    public DifferentialExpressionAnalysisTaskCommand( ExpressionExperiment ee, DifferentialExpressionAnalysis toRedo,
            boolean updateAnalysis ) {
        super();
        this.expressionExperiment = ee;
        this.toRedo = toRedo;
        this.updateStatsOnly = !updateAnalysis;
        this.remoteOnly = Settings.getBoolean( "gemma.grid.gridonly.diff" );
    }

    /**
     * @param taskId
     * @param forceAnalysis
     * @param expressionExperiment
     */
    public DifferentialExpressionAnalysisTaskCommand( String taskId, boolean forceAnalysis,
            ExpressionExperiment expressionExperiment ) {
        super();
        this.setTaskId( taskId );
        this.forceAnalysis = forceAnalysis;
        this.expressionExperiment = expressionExperiment;
        this.remoteOnly = Settings.getBoolean( "gemma.grid.gridonly.diff" );
    }

    public AnalysisType getAnalysisType() {
        return analysisType;
    }

    public ExpressionExperiment getExpressionExperiment() {
        return expressionExperiment;
    }

    public Collection<ExperimentalFactor> getFactors() {
        return factors;
    }

    public Double getQvalueThreshold() {
        return qvalueThreshold;
    }

    /**
     * @return the subsetFactor
     */
    public ExperimentalFactor getSubsetFactor() {
        return subsetFactor;
    }

    @Override
    public Class<? extends Task<TaskResult, ? extends TaskCommand>> getTaskClass() {
        return DifferentialExpressionAnalysisTask.class;
    }

    public DifferentialExpressionAnalysis getToRedo() {
        return toRedo;
    }

    public boolean isForceAnalysis() {
        return forceAnalysis;
    }

    public boolean isIncludeInteractions() {
        return includeInteractions;
    }

    public boolean isModerateStatistics() {
        return moderateStatistics;
    }

    public boolean isUpdateStatsOnly() {
        return updateStatsOnly;
    }

    public void setAnalysisType( AnalysisType analysisType ) {
        this.analysisType = analysisType;
    }

    public void setExpressionExperiment( ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    public void setFactors( Collection<ExperimentalFactor> factors ) {
        this.factors = factors;
    }

    public void setForceAnalysis( boolean forceAnalysis ) {
        this.forceAnalysis = forceAnalysis;
    }

    /**
     * Sets preference for interactions to be included.
     * 
     * @param includeInteractions
     */
    public void setIncludeInteractions( boolean includeInteractions ) {
        this.includeInteractions = includeInteractions;
    }

    public void setModerateStatistics( boolean moderateStatistics ) {
        this.moderateStatistics = moderateStatistics;
    }

    public void setQvalueThreshold( Double qvalueThreshold ) {
        this.qvalueThreshold = qvalueThreshold;
    }

    /**
     * @param subsetFactor the subsetFactor to set
     */
    public void setSubsetFactor( ExperimentalFactor subsetFactor ) {
        this.subsetFactor = subsetFactor;
    }

    public void setToRedo( DifferentialExpressionAnalysis toRedo ) {
        this.toRedo = toRedo;
    }

    public void setUpdateStatsOnly( boolean updateStatsOnly ) {
        this.updateStatsOnly = updateStatsOnly;
    }
}
