/*
 * The Gemma project.
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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.genome.gene;

import java.util.Collection;

/**
 * @see ubic.gemma.model.genome.gene.GeneProductService
 */
public class GeneProductServiceImpl extends ubic.gemma.model.genome.gene.GeneProductServiceBase {

    /* (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneProductServiceBase#handleGetGenesByName(java.lang.String)
     */
    @Override
    protected Collection handleGetGenesByName( String search ) throws Exception {
        return this.getGeneProductDao().getGenesByName( search );
    }

    /* (non-Javadoc)
     * @see ubic.gemma.model.genome.gene.GeneProductServiceBase#handleGetGenesByNcbiId(java.lang.String)
     */
    @Override
    protected Collection handleGetGenesByNcbiId( String search ) throws Exception {
        return this.getGeneProductDao().getGenesByNcbiId( search );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#create(ubic.gemma.model.genome.gene.GeneProduct)
     */
    protected ubic.gemma.model.genome.gene.GeneProduct handleCreate(
            ubic.gemma.model.genome.gene.GeneProduct geneProduct ) throws java.lang.Exception {
        return ( GeneProduct ) this.getGeneProductDao().create( geneProduct );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#findOrCreate(ubic.gemma.model.genome.gene.GeneProduct)
     */
    protected ubic.gemma.model.genome.gene.GeneProduct handleFindOrCreate(
            ubic.gemma.model.genome.gene.GeneProduct geneProduct ) throws java.lang.Exception {
        return this.getGeneProductDao().findOrCreate( geneProduct );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#delete(ubic.gemma.model.genome.gene.GeneProduct)
     */
    protected void handleDelete( ubic.gemma.model.genome.gene.GeneProduct geneProduct ) throws java.lang.Exception {
        this.getGeneProductDao().remove( geneProduct );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#update(ubic.gemma.model.genome.gene.GeneProduct)
     */
    protected void handleUpdate( ubic.gemma.model.genome.gene.GeneProduct geneProduct ) throws java.lang.Exception {
        this.getGeneProductDao().update( geneProduct );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductService#load(java.lang.Long)
     */
    protected ubic.gemma.model.genome.gene.GeneProduct handleLoad( java.lang.Long id ) throws java.lang.Exception {
        return ( GeneProduct ) this.getGeneProductDao().load( id );
    }

}