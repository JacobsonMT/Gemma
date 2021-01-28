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
package ubic.gemma.web.services.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ubic.gemma.core.analysis.service.AnalysisResultSetService;
import ubic.gemma.model.analysis.AnalysisResult;
import ubic.gemma.model.analysis.AnalysisResultSet;
import ubic.gemma.model.analysis.AnalysisResultSetValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.web.services.rest.util.Responder;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.WebService;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.List;

/**
 * Specialized endpoint for {@link ubic.gemma.model.analysis.AnalysisResultSet}
 */
@Controller
@Path("/resultSets")
public class AnalysisResultSetsWebService extends WebService {

    @Autowired
    private AnalysisResultSetService<AnalysisResult, AnalysisResultSet<AnalysisResult>, AnalysisResultSetValueObject<AnalysisResult, AnalysisResultSet<AnalysisResult>>> analysisResultSetService;

    @Autowired
    private BioAssayService bioAssayService;

    /**
     * Retrieve all {@link AnalysisResultSet} matching a set of criteria.
     *
     * @param datasetIds filter result sets that belong to any of the provided dataset identifiers.
     * @param externalIds filter by associated datasets with given external identifiers.
     * @param servlet
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject<List<AnalysisResultSetValueObject>> findAll(
            @QueryParam("datasetIds") Collection<Long> datasetIds,
            @QueryParam("externalIds") Collection<String> externalIds,
            @Context final HttpServletResponse servlet ) {
        Collection<BioAssay> datasets = null;
        if ( datasetIds != null ) {
            datasets = bioAssayService.load( datasetIds );
            for ( BioAssay dataset : datasets ) {
                if ( dataset == null ) {
                    return Responder.code400( "At least one dataset could not be retrieved.", servlet );
                }
            }
        }
        return Responder.code200( analysisResultSetService.findByDatasetInAndExternalIdsLimit( datasets, externalIds, 10 ), servlet );
    }

    /**
     * Retrieve a {@link AnalysisResultSet} given its identifier.
     *
     * @param analysisResultSetId
     * @return
     */
    @GET
    @Path("/{analysisResultSetId}")
    @Produces(MediaType.APPLICATION_JSON)
    public ResponseDataObject<AnalysisResultSetValueObject> findById(
            @PathParam("analysisResultSetId") Long analysisResultSetId,
            @Context final HttpServletResponse servlet ) {
        AnalysisResultSet analysisResultSet = analysisResultSetService.load( analysisResultSetId );
        return Responder.code200( analysisResultSetService.loadValueObject( analysisResultSet ), servlet );
    }
}
