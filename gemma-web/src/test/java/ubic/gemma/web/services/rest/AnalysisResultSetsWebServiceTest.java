package ubic.gemma.web.services.rest;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.CollectionUtils;
import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.analysis.AnalysisResultSetValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.util.BaseSpringWebTest;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class AnalysisResultSetsWebServiceTest extends BaseSpringWebTest {

    @Autowired
    private AnalysisResultSetsWebService service;

    @Test
    public void testFindAllWhenNoDatasetsAreProvidedThenReturnLatestAnalysisResults() {
        HttpServletResponse response = new MockHttpServletResponse();
        service.findAll( null,
                null,
                response );
        assertEquals( response.getStatus(), 200 );
    }

    @Test
    public void testFindAllWithDatasetIdsThenReturnLatestAnalysisResults() {
        HttpServletResponse response = new MockHttpServletResponse();
        ExpressionExperiment ee = getTestPersistentExpressionExperiment();
        Collection<Analysis> analyses = addTestAnalyses( ee );
        assertFalse( ee.getBioAssays().isEmpty() );
        Collection<Long> datasetIds = ee.getBioAssays().stream().map( BioAssay::getId ).collect( Collectors.toList() );
        ResponseDataObject<List<AnalysisResultSetValueObject>> result = service.findAll(
                datasetIds,
                null,
                response );
        assertFalse( result.getData().isEmpty() );
        assertEquals( response.getStatus(), 200 );
    }

    @Test
    public void testFindAllWithExternalIdsThenReturnLatestAnalysisResults() {
        HttpServletResponse response = new MockHttpServletResponse();
        service.findAll( null,
                Arrays.asList( "GEO123123", "GEO1213121" ),
                response );
        assertEquals( response.getStatus(), 200 );
    }

    @Test
    public void testFindByIdThenReturn200Success() {
        ExpressionExperiment ee = getTestPersistentBasicExpressionExperiment();
        Collection<Analysis> analyses = addTestAnalyses( ee );
        Analysis firstAnalysis = analyses.stream().findFirst().get();
        HttpServletResponse response = new MockHttpServletResponse();
        service.findById( firstAnalysis.getId(), response );
        assertEquals( response.getStatus(), 200 );
    }

    @Test
    public void testFindByIdWhenResultSetDoesNotExistsThenReturn404NotFoundError() {
        Long id = 1L;
        HttpServletResponse response = new MockHttpServletResponse();
        WebApplicationException e = assertThrows( WebApplicationException.class, () -> {
            service.findById( id, response );
        } );
        assertEquals( e.getResponse().getStatus(), 404 );
    }
}