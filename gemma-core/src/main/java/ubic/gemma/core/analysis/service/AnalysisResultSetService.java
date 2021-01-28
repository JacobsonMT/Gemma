package ubic.gemma.core.analysis.service;

import ubic.gemma.model.analysis.AnalysisResult;
import ubic.gemma.model.analysis.AnalysisResultSet;
import ubic.gemma.model.analysis.AnalysisResultSetValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.persistence.service.BaseVoEnabledService;

import java.util.Collection;

/**
 * Interface for services providing {@link AnalysisResultSet}.
 *
 * @param <O> the type of result set
 * @param <V> a value object type to expose result sets
 */
public interface AnalysisResultSetService<K extends AnalysisResult, O extends AnalysisResultSet<K>, V extends AnalysisResultSetValueObject<K, O>> extends BaseVoEnabledService<O, V> {

    /**
     *
     *
     * @param datasets
     * @param externalIds
     * @param i
     * @return
     */
    Collection<ExpressionAnalysisResultSet> findByDatasetInAndExternalIdsLimit( Collection<BioAssay> datasets, Collection<String> externalIds, int i );
}
