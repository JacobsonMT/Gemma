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
package ubic.gemma.model.analysis.expression;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @version $Id$
 * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService
 */
@Service
public class ExpressionExperimentSetServiceImpl extends
        ubic.gemma.model.analysis.expression.ExpressionExperimentSetServiceBase {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#load(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<ExpressionExperimentSet> load( Collection<Long> ids ) {
        return ( Collection<ExpressionExperimentSet> ) this.getExpressionExperimentSetDao().load( ids );
    }

    public Collection<ExpressionExperimentSet> loadAllMultiExperimentSets() {
        return this.getExpressionExperimentSetDao().loadAllMultiExperimentSets();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#loadMySets()
     */
    @Override
    public Collection<ExpressionExperimentSet> loadMySets() {
        return this.getExpressionExperimentSetDao().loadAllMultiExperimentSets();
    }

    @Override
    public Collection<ExpressionExperimentSet> loadMySharedSets() {
        return this.getExpressionExperimentSetDao().loadAllMultiExperimentSets();
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#create(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    @Override
    protected ubic.gemma.model.analysis.expression.ExpressionExperimentSet handleCreate(
            ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet )
            throws java.lang.Exception {
        return this.getExpressionExperimentSetDao().create( expressionExperimentSet );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#delete(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    @Override
    protected void handleDelete( ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet )
            throws java.lang.Exception {
        this.getExpressionExperimentSetDao().remove( expressionExperimentSet );
    }

    @Override
    protected Collection<ExpressionExperimentSet> handleFindByName( String name ) throws Exception {
        return this.getExpressionExperimentSetDao().findByName( name );
    }

    @Override
    protected Collection<ExpressionAnalysis> handleGetAnalyses( ExpressionExperimentSet expressionExperimentSet )
            throws Exception {
        return this.getExpressionExperimentSetDao().getAnalyses( expressionExperimentSet );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#load(java.lang.Long)
     */
    @Override
    protected ubic.gemma.model.analysis.expression.ExpressionExperimentSet handleLoad( java.lang.Long id )
            throws java.lang.Exception {
        return this.getExpressionExperimentSetDao().load( id );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#loadAll()
     */
    @SuppressWarnings("unchecked")
    @Override
    protected java.util.Collection<ExpressionExperimentSet> handleLoadAll() throws java.lang.Exception {
        return ( Collection<ExpressionExperimentSet> ) this.getExpressionExperimentSetDao().loadAll();

    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#loadUserSets(ubic.gemma.model.common.auditAndSecurity.User)
     */
    @Override
    protected java.util.Collection<ExpressionExperimentSet> handleLoadUserSets(
            ubic.gemma.model.common.auditAndSecurity.User user ) throws java.lang.Exception {
        // @todo implement protected java.util.Collection
        // handleLoadUserSets(ubic.gemma.model.common.auditAndSecurity.User user)
        throw new java.lang.UnsupportedOperationException(
                "ubic.gemma.model.analysis.expression.ExpressionExperimentSetService.handleLoadUserSets(ubic.gemma.model.common.auditAndSecurity.User user) Not implemented!" );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#update(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    @Override
    protected void handleUpdate( ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet )
            throws java.lang.Exception {
        if ( expressionExperimentSet.getExperiments().size() == 0 ) {
            throw new IllegalArgumentException( "Attempt to update ExpressionExperimentSet so it has no members" );
        }

        if ( StringUtils.isBlank( expressionExperimentSet.getName() ) ) {
            throw new IllegalArgumentException( "Attempt to update an ExpressionExperimentSet so it has no name" );
        }

        this.getExpressionExperimentSetDao().update( expressionExperimentSet );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#thaw(ubic.gemma.model.analysis.expression
     * .ExpressionExperimentSet)
     */
    @Override
    public void thaw( ExpressionExperimentSet expressionExperimentSet ) {
        this.getExpressionExperimentSetDao().thaw( expressionExperimentSet );
    }

}