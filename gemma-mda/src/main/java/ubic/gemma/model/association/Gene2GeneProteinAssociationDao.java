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
package ubic.gemma.model.association;

import java.util.Collection;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.BaseDao;

/**
 * 
 * Dao for Gene2GeneProteinAssociation
 * 
 * @author ldonnison
 * @version $Id$
 */
public interface Gene2GeneProteinAssociationDao extends BaseDao<Gene2GeneProteinAssociation> {

    
    /**
     * Given a gene2GeneProteinAssociation find its entry. It presumes that gene one and gene two are in the same order
     * as stored in the db.
     * @param gene2GeneProteinAssociation
     * @return gene2GeneProteinAssociation matching record
     */
    public Gene2GeneProteinAssociation find( Gene2GeneProteinAssociation gene2GeneProteinAssociation );

    
    /**
     * Thaw the gene2GeneProteinAssociation genes are not automatically loaded, so this
     * method thaws the genes.
     * @param gene2GeneProteinAssociation
     * @throws Exception
     */
    public void thaw (Gene2GeneProteinAssociation gene2GeneProteinAssociation) throws Exception;
    
    /**
     * Finder method that given a gene finds its interactions. The query checks if the gene matches 
     * either gene one or gene two.
     * @param gene The gene to find interactions for
     * @return gene2GeneProteinAssociation Collection of associations assocaited with this gene
     */
    public Collection<Gene2GeneProteinAssociation> findProteinInteractionsForGene( Gene gene );
    
    
    
    /**
     * Given a Gene2GeneProteinAssociation check if it already exists by checking if for those two genes in an interaction exists in db. 
     * If the interaction exists then issue an update.
     * 
     * @param gene2GeneProteinAssociation Gene2GeneProteinAssociation to be created or updated
     * @return gene2GeneProteinAssociation Created or updated Gene2GeneProteinAssociation
     */
    public Gene2GeneProteinAssociation createOrUpdate( Gene2GeneProteinAssociation gene2GeneProteinAssociation );
    

}