/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.model.genome.gene;

import java.util.Collection;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.BaseDao;

/**
 * The interface for managing groupings of genes.
 * 
 * @author kelsey
 * @version $Id$
 */
public interface GeneSetDao extends BaseDao<GeneSet> {

    /**
     * @param gene
     * @return
     */
    Collection<GeneSet> findByGene( Gene gene );
    
    /**
     * @param name  uses the given name to do a name* search in the db
     * @return a collection of geneSets that match the given search term. 
     */
    Collection<GeneSet> findByName( String name);

    /**
     * @param name
     * @param taxon
     * @return
     */
    Collection<GeneSet> findByName( String name, Taxon taxon );

    /**
     * @param tax
     * @return
     */
    Collection<GeneSet> loadAll( Taxon tax );

}
