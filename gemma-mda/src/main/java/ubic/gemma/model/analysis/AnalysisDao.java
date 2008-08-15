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
//
// Attention: Generated code! Do not modify by hand!
// Generated by: SpringDao.vsl in andromda-spring-cartridge.
//
package ubic.gemma.model.analysis;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;

/**
 * @see ubic.gemma.model.analysis.Analysis
 */
public interface AnalysisDao extends ubic.gemma.model.common.AuditableDao {
    /**
     * Loads an instance of ubic.gemma.model.analysis.Analysis from the persistent store.
     */
    public ubic.gemma.model.common.Securable load( java.lang.Long id );

    /**
     * <p>
     * Does the same thing as {@link #load(java.lang.Long)} with an additional flag called <code>transform</code>. If
     * this flag is set to <code>TRANSFORM_NONE</code> then the returned entity will <strong>NOT</strong> be
     * transformed. If this flag is any of the other constants defined in this class then the result <strong>WILL BE</strong>
     * passed through an operation which can optionally transform the entity (into a value object for example). By
     * default, transformation does not occur.
     * </p>
     * 
     * @param id the identifier of the entity to load.
     * @return either the entity or the object transformed from the entity.
     */
    public Object load( int transform, java.lang.Long id );

    /**
     * Loads all entities of type {@link ubic.gemma.model.analysis.Analysis}.
     * 
     * @return the loaded entities.
     */
    public java.util.Collection loadAll();

    /**
     * <p>
     * Does the same thing as {@link #loadAll()} with an additional flag called <code>transform</code>. If this flag
     * is set to <code>TRANSFORM_NONE</code> then the returned entity will <strong>NOT</strong> be transformed. If
     * this flag is any of the other constants defined here then the result <strong>WILL BE</strong> passed through an
     * operation which can optionally transform the entity (into a value object for example). By default, transformation
     * does not occur.
     * </p>
     * 
     * @param transform the flag indicating what transformation to use.
     * @return the loaded entities.
     */
    public java.util.Collection loadAll( final int transform );

    /**
     * Updates the <code>analysis</code> instance in the persistent store.
     */
    public void update( ubic.gemma.model.analysis.Analysis analysis );

    /**
     * Updates all instances in the <code>entities</code> collection in the persistent store.
     */
    public void update( java.util.Collection entities );

    /**
     * Removes the instance of ubic.gemma.model.analysis.Analysis from the persistent store.
     */
    public void remove( ubic.gemma.model.analysis.Analysis analysis );

    /**
     * Removes the instance of ubic.gemma.model.analysis.Analysis having the given <code>identifier</code> from the
     * persistent store.
     */
    public void remove( java.lang.Long id );

    /**
     * Removes all entities in the given <code>entities<code> collection.
     */
    public void remove( java.util.Collection entities );

    /**
     * <p>
     * Returns a collection of anaylsis that have a name that starts with the given name
     * </p>
     */
    public java.util.Collection findByName( java.lang.String name );

    /**
     * <p>
     * Does the same thing as {@link #findByName(java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string
     * defined in {@link #findByName(java.lang.String)}.
     * </p>
     */
    public java.util.Collection findByName( String queryString, java.lang.String name );

    /**
     * <p>
     * Does the same thing as {@link #findByName(java.lang.String)} with an additional flag called
     * <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public java.util.Collection findByName( int transform, java.lang.String name );

    /**
     * <p>
     * Does the same thing as {@link #findByName(boolean, java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string
     * defined in {@link #findByName(int, java.lang.String name)}.
     * </p>
     */
    public java.util.Collection findByName( int transform, String queryString, java.lang.String name );

    /**
     * 
     */
    public java.util.Collection<DifferentialExpressionAnalysis> findByInvestigation(
            ubic.gemma.model.analysis.Investigation investigation );

    /**
     * <p>
     * Given a collection of investigations returns a Map of Analysis --> collection of Investigations
     * </p>
     * <p>
     * The collection of investigations returned by the map will include all the investigations for the analysis key iff
     * one of the investigations for that analysis was in the given collection started with
     * </p>
     */
    public java.util.Map findByInvestigations( java.util.Collection investigators );

    /**
     * 
     */
    public java.util.Collection findByTaxon( ubic.gemma.model.genome.Taxon taxon );

}
