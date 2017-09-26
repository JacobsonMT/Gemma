/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisDao;

import java.util.Collection;

/**
 * @author pavlidis
 * @see ExpressionExperimentSubSetService
 */
@Service
public class ExpressionExperimentSubSetServiceImpl extends ExpressionExperimentSubSetServiceBase {

    @Autowired
    private DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao;

    @Autowired
    private ExpressionExperimentSubSetDao expressionExperimentSubSetDao;

    @Override
    @Transactional
    public void delete( ExpressionExperimentSubSet entity ) {
        this.handleDelete( entity );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperimentSubSet find( ExpressionExperimentSubSet entity ) {
        return this.getExpressionExperimentSubSetDao().find( entity );
    }

    @Override
    public ExpressionExperimentSubSet findOrCreate( ExpressionExperimentSubSet entity ) {
        return this.getExpressionExperimentSubSetDao().findOrCreate( entity );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<FactorValue> getFactorValuesUsed( ExpressionExperimentSubSet entity, ExperimentalFactor factor ) {
        return this.getExpressionExperimentSubSetDao().getFactorValuesUsed( entity, factor );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<FactorValueValueObject> getFactorValuesUsed( Long subSetId, Long experimentalFactor ) {
        return this.getExpressionExperimentSubSetDao().getFactorValuesUsed( subSetId, experimentalFactor );

    }

    @Override
    protected ExpressionExperimentSubSet handleCreate(
            ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet expressionExperimentSubSet ) {
        return this.getExpressionExperimentSubSetDao().create( expressionExperimentSubSet );
    }

    /**
     * doesn't include removal of sample coexpression matrices, PCA, probe2probe coexpression links, or adjusting
     * experiment set members
     *
     * @param subset subset
     */
    protected void handleDelete( ExpressionExperimentSubSet subset ) {

        if ( subset == null ) {
            throw new IllegalArgumentException( "ExperimentSubSet cannot be null" );
        }

        // Remove differential expression analyses
        Collection<DifferentialExpressionAnalysis> diffAnalyses = this.differentialExpressionAnalysisDao
                .findByInvestigation( subset );
        for ( DifferentialExpressionAnalysis de : diffAnalyses ) {
            Long toDelete = de.getId();
            this.differentialExpressionAnalysisDao.remove( toDelete );
        }

        this.expressionExperimentSubSetDao.remove( subset );

        /*
         * FIXME Coexpression involving this data set will linger on ...
         */
    }

    /**
     * Loads one subset, given an id
     *
     * @param id id
     * @return ExpressionExperimentSubSet
     */
    @Override
    protected ExpressionExperimentSubSet handleLoad( Long id ) {
        return this.getExpressionExperimentSubSetDao().load( id );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Collection<ExpressionExperimentSubSet> handleLoadAll() {
        return ( Collection<ExpressionExperimentSubSet> ) this.getExpressionExperimentSubSetDao().loadAll();
    }

}