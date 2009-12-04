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

package ubic.gemma.model.association;

import java.util.Collection;

import org.springframework.stereotype.Service;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * @see ubic.gemma.model.association.Gene2GOAssociationService
 * @author klc
 * @version $Id$
 */
@Service
public class Gene2GOAssociationServiceImpl extends ubic.gemma.model.association.Gene2GOAssociationServiceBase {

    /**
     * @see ubic.gemma.model.association.Gene2GOAssociationService#create(ubic.gemma.model.association.Gene2GOAssociation)
     */
    @Override
    protected Gene2GOAssociation handleCreate( Gene2GOAssociation gene2GOAssociation ) throws java.lang.Exception {
        return this.getGene2GOAssociationDao().create( gene2GOAssociation );
    }

    /**
     * @see Gene2GOAssociationService#find(Gene2GOAssociation)
     */
    @Override
    protected Gene2GOAssociation handleFind( Gene2GOAssociation gene2GOAssociation ) throws java.lang.Exception {
        return this.getGene2GOAssociationDao().find( gene2GOAssociation );
    }

    /*
     * (non-Javadoc)
     * @see Gene2GOAssociationServiceBase#handleFindAssociationByGene(ubic.gemma.model.genome .Gene)
     */
    @Override
    protected Collection handleFindAssociationByGene( Gene gene ) throws java.lang.Exception {
        return this.getGene2GOAssociationDao().findAssociationByGene( gene );
    }

    /*
     * (non-Javadoc)
     * @see Gene2GOAssociationServiceBase#handleFindByGene(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection handleFindByGene( Gene gene ) throws Exception {
        return this.getGene2GOAssociationDao().findByGene( gene );

    }

    /*
     * (non-Javadoc)
     * @see Gene2GOAssociationServiceBase#handleFindByGOTerm(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleFindByGOTerm( String goID, Taxon taxon ) throws Exception {
        return this.getGene2GOAssociationDao().findByGoTerm( goID, taxon );
    }

    /**
     * @see Gene2GOAssociationService#findOrCreate(Gene2GOAssociation)
     */
    @Override
    protected Gene2GOAssociation handleFindOrCreate( Gene2GOAssociation gene2GOAssociation ) throws java.lang.Exception {
        return this.getGene2GOAssociationDao().findOrCreate( gene2GOAssociation );
    }

    /*
     * (non-Javadoc)
     * @see Gene2GOAssociationServiceBase#handleRemoveAll()
     */
    @Override
    protected void handleRemoveAll() throws Exception {
        this.getGene2GOAssociationDao().removeAll();
    }

}