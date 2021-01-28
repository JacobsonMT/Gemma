package ubic.gemma.model.analysis.expression.diff;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.core.analysis.service.AnalysisResultSetService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.persistence.service.AbstractVoEnabledService;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetDao;

import java.util.Collection;

@Service
public class ExpressionAnalysisResultSetService extends AbstractVoEnabledService<ExpressionAnalysisResultSet, ExpressionAnalysisResultSetValueObject> implements AnalysisResultSetService<DifferentialExpressionAnalysisResult, ExpressionAnalysisResultSet, ExpressionAnalysisResultSetValueObject> {

    private final ExpressionAnalysisResultSetDao voDao;

    @Autowired
    public ExpressionAnalysisResultSetService( ExpressionAnalysisResultSetDao voDao ) {
        super( voDao );
        this.voDao = voDao;
    }

    @Override
    public Collection<ExpressionAnalysisResultSet> findByDatasetInAndExternalIdsLimit( Collection<BioAssay> datasets, Collection<String> externalIds, int limit ) {
        return voDao.findByDatasetInAndExternalIdsLimit( datasets, externalIds, limit );
    }
}
