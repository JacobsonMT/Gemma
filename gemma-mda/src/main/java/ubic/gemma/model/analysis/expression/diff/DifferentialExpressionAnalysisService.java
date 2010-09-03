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
package ubic.gemma.model.analysis.expression.diff;

import java.util.Collection;
import java.util.Map;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author kelsey
 * @version $Id$
 */
public interface DifferentialExpressionAnalysisService extends
        ubic.gemma.model.analysis.AnalysisService<DifferentialExpressionAnalysis> {

    /**
     * @param par
     * @param threshold for corrected pvalue. Results may not be accurate for 'unreasonable' thresholds.
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Integer countDownregulated( ExpressionAnalysisResultSet par, double threshold );

    /**
     * @param ExpressionAnalysisResultSet
     * @param threshold (double) for corrected pvalue. Results may not be accurate for 'unreasonable' thresholds.
     * @return an integer count of all the probes that met the given threshold in the given expressionAnalysisResultSet
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Integer countProbesMeetingThreshold( ExpressionAnalysisResultSet ears, double threshold );

    /**
     * @param par
     * @param threshold for corrected pvalue. Results may not be accurate for 'unreasonable' thresholds.
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Integer countUpregulated( ExpressionAnalysisResultSet par, double threshold );

    /**
     * 
     */

    @Secured( { "GROUP_USER" })
    public DifferentialExpressionAnalysis create( DifferentialExpressionAnalysis analysis );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<DifferentialExpressionAnalysis> find( ubic.gemma.model.genome.Gene gene,
            ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet resultSet, double threshold );

    public Collection<DifferentialExpressionAnalysis> findByFactor( ExperimentalFactor ef );

    /**
     * <p>
     * Given a collection of ids, return a map of id -> differential expression analysis (one per id).
     * </p>
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY" })
    public Map<Long, DifferentialExpressionAnalysis> findByInvestigationIds( Collection<Long> investigationIds );

    /**
     * <p>
     * Return a collection of experiments in which the given gene was analyzed.
     * </p>
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<BioAssaySet> findExperimentsWithAnalyses( ubic.gemma.model.genome.Gene gene );

    /**
     * @param expressionExperiment
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<DifferentialExpressionAnalysis> getAnalyses( ExpressionExperiment expressionExperiment );

    /**
     * @param resultSetIds
     * @return
     */
    public Collection<ExpressionAnalysisResultSet> getResultSets( Collection<Long> resultSetIds );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<ExpressionAnalysisResultSet> getResultSets( ExpressionExperiment expressionExperiment );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ" })
    public void thaw( Collection<DifferentialExpressionAnalysis> expressionAnalyses );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public void thaw( DifferentialExpressionAnalysis differentialExpressionAnalysis );

    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( DifferentialExpressionAnalysis o );

    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( ExpressionAnalysisResultSet a );

}
