/*
 * The Gemma_sec1 project
 * 
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.core.analysis.report;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * Methods for reading and creating reports on ExpressinExperiments. Reports are typically updated either on demand or
 * after an analysis; and retrieval is usually from the web interface.
 *
 * @author paul
 */
public interface ExpressionExperimentReportService {

    /**
     * Invalidate the cached 'report' for the experiment with the given id. If it is not cached nothing happens.
     */
    void evictFromCache( Long id );

    /**
     * Generate a value object that contain summary information about links, biomaterials, and datavectors
     */
    ExpressionExperimentValueObject generateSummary( Long id );

    /**
     * Generates reports on ALL experiments, including 'private' ones. This should only be run by administrators as it
     * takes a while to run.
     */
    @Secured({ "GROUP_AGENT" })
    void generateSummaryObjects();

    /**
     * generates a collection of value objects that contain summary information about links, biomaterials, and
     * dataVectors
     */
    Collection<ExpressionExperimentValueObject> generateSummaryObjects( Collection<Long> ids );

    void getAnnotationInformation( Collection<? extends ExpressionExperimentValueObject> vos );

    Map<Long, Date> getEventInformation( Collection<? extends ExpressionExperimentValueObject> vos );

    /**
     * Fills in link analysis and differential expression analysis summaries, and other info from the report.
     *
     * @return map of when the objects were most recently updated (or created)
     */
    Map<Long, Date> getReportInformation( Collection<? extends ExpressionExperimentValueObject> vos );

    /**
     * retrieves a collection of cached value objects containing summary information
     *
     * @return a collection of cached value objects
     */
    Collection<ExpressionExperimentValueObject> retrieveSummaryObjects( Collection<Long> ids );

    /**
     * Recalculates the batch effect and batch confound information for datasets that have been updated
     * in the last 24 hours.
     */
    @SuppressWarnings("unused") // Used by scheduler
    @Secured({ "GROUP_AGENT" })
    void recalculateBatchInfo();
}