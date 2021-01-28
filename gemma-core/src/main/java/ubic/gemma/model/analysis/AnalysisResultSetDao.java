package ubic.gemma.model.analysis;

import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.persistence.service.BaseVoEnabledDao;

import java.util.Collection;

/**
 * Generic DAO for manipulating {@link AnalysisResultSet}.
 *
 * @param <O>
 * @param <VO>
 */
public interface AnalysisResultSetDao<K extends AnalysisResult, O extends AnalysisResultSet<K>, VO extends AnalysisResultSetValueObject<K, O>> extends BaseVoEnabledDao<O, VO> {

    /**
     * Retrieve result sets associated to a set of {@link BioAssay} and external identifiers.
     *
     * @param bioAssayIds related {@link BioAssay}, or any if null
     * @param externalIds related external identifier associated to the {@link BioAssay}, or any if null
     * @param limit maximum number of results to return
     * @return
     */
    Collection<ExpressionAnalysisResultSet> findByDatasetInAndExternalIdsLimit( Collection<BioAssay> bioAssayIds, Collection<String> externalIds, int limit );
}
