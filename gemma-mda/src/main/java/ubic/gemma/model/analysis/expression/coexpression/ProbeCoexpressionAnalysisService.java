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
package ubic.gemma.model.analysis.expression.coexpression;

import java.util.Collection;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author kelsey
 * @version $Id$
 */
public interface ProbeCoexpressionAnalysisService extends
        ubic.gemma.model.analysis.AnalysisService<ProbeCoexpressionAnalysis> {

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public ProbeCoexpressionAnalysis create( ProbeCoexpressionAnalysis probeCoexpressionAnalysis );

    /**
     * Retrieve the list of probes used in the probe-level coexpression analysis for the given experiment. This assumes
     * there is just one analysis available for the experiment - if there are multiple, the union of all probes used
     * will be returned (This is probably okay because 1) usually there is just one analysis and 2) even if there were
     * more than one, this method presupposes that the question is at the level of the experiment).
     * 
     * @param experiment
     * @return
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public Collection<CompositeSequence> getAssayedProbes( ExpressionExperiment experiment );

    /**
     * @param o
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( ProbeCoexpressionAnalysis o );
}
