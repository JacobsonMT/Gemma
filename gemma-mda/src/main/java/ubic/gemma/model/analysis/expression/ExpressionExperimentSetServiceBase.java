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

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.analysis.expression.ExpressionExperimentSetService</code>,
 * provides access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService
 */
public abstract class ExpressionExperimentSetServiceBase implements
        ubic.gemma.model.analysis.expression.ExpressionExperimentSetService {

    @Autowired
    private ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao expressionExperimentSetDao;

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#create(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    public ubic.gemma.model.analysis.expression.ExpressionExperimentSet create(
            final ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet ) {
        try {
            return this.handleCreate( expressionExperimentSet );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.ExpressionExperimentSetServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.ExpressionExperimentSetService.create(ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#delete(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    public void delete( final ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet ) {
        try {
            this.handleDelete( expressionExperimentSet );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.ExpressionExperimentSetServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.ExpressionExperimentSetService.delete(ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#findByName(java.lang.String)
     */
    public java.util.Collection<ExpressionExperimentSet> findByName( final java.lang.String name ) {
        try {
            return this.handleFindByName( name );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.ExpressionExperimentSetServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.ExpressionExperimentSetService.findByName(java.lang.String name)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#getAnalyses(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    public java.util.Collection<ExpressionAnalysis> getAnalyses(
            final ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet ) {
        try {
            return this.handleGetAnalyses( expressionExperimentSet );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.ExpressionExperimentSetServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.ExpressionExperimentSetService.getAnalyses(ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#load(java.lang.Long)
     */
    public ubic.gemma.model.analysis.expression.ExpressionExperimentSet load( final java.lang.Long id ) {
        try {
            return this.handleLoad( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.ExpressionExperimentSetServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.ExpressionExperimentSetService.load(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#loadAll()
     */
    public java.util.Collection<ExpressionExperimentSet> loadAll() {
        try {
            return this.handleLoadAll();
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.ExpressionExperimentSetServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.ExpressionExperimentSetService.loadAll()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#loadUserSets(ubic.gemma.model.common.auditAndSecurity.User)
     */
    public java.util.Collection<ExpressionExperimentSet> loadUserSets(
            final ubic.gemma.model.common.auditAndSecurity.User user ) {
        try {
            return this.handleLoadUserSets( user );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.ExpressionExperimentSetServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.ExpressionExperimentSetService.loadUserSets(ubic.gemma.model.common.auditAndSecurity.User user)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>expressionExperimentSet</code>'s DAO.
     */

    public void setExpressionExperimentSetDao(
            ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao expressionExperimentSetDao ) {
        this.expressionExperimentSetDao = expressionExperimentSetDao;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetService#update(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)
     */
    public void update( final ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet ) {
        try {
            this.handleUpdate( expressionExperimentSet );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.ExpressionExperimentSetServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.ExpressionExperimentSetService.update(ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>expressionExperimentSet</code>'s DAO.
     */
    protected ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao getExpressionExperimentSetDao() {
        return this.expressionExperimentSetDao;
    }

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)}
     */
    protected abstract ubic.gemma.model.analysis.expression.ExpressionExperimentSet handleCreate(
            ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #delete(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)}
     */
    protected abstract void handleDelete(
            ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByName(java.lang.String)}
     */
    protected abstract java.util.Collection<ExpressionExperimentSet> handleFindByName( java.lang.String name )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getAnalyses(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)}
     */
    protected abstract java.util.Collection<ExpressionAnalysis> handleGetAnalyses(
            ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.analysis.expression.ExpressionExperimentSet handleLoad( java.lang.Long id )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadAll()}
     */
    protected abstract java.util.Collection<ExpressionExperimentSet> handleLoadAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadUserSets(ubic.gemma.model.common.auditAndSecurity.User)}
     */
    protected abstract java.util.Collection<ExpressionExperimentSet> handleLoadUserSets(
            ubic.gemma.model.common.auditAndSecurity.User user ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.analysis.expression.ExpressionExperimentSet)}
     */
    protected abstract void handleUpdate(
            ubic.gemma.model.analysis.expression.ExpressionExperimentSet expressionExperimentSet )
            throws java.lang.Exception;

}