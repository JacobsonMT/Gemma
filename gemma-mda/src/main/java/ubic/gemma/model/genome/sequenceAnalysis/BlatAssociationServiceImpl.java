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
package ubic.gemma.model.genome.sequenceAnalysis;

import java.util.Collection;

import ubic.gemma.model.genome.Gene;

/**
 * @version $Id$
 * @see ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService
 */
public class BlatAssociationServiceImpl extends ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationServiceBase {

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService#create(ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation)
     */
    @Override
    protected ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation handleCreate(
            ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation blatAssociation ) throws java.lang.Exception {
        return ( BlatAssociation ) this.getBlatAssociationDao().create( blatAssociation );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService#find(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    protected java.util.Collection handleFind( ubic.gemma.model.genome.biosequence.BioSequence bioSequence )
            throws java.lang.Exception {
        return this.getBlatAssociationDao().find( bioSequence );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationServiceBase#handleFind(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection handleFind( Gene gene ) throws Exception {
        return this.getBlatAssociationDao().find( gene );
    }

    @Override
    protected void handleThaw( BlatAssociation blatAssociation ) throws Exception {
        this.getBlatAssociationDao().thaw( blatAssociation );
    }

    @Override
    protected void handleThaw( Collection blatAssociations ) throws Exception {
        this.getBlatAssociationDao().thaw( blatAssociations );
    }

    @Override
    protected void handleUpdate( BlatAssociation blatAssociation ) throws Exception {
        this.getBlatAssociationDao().update( blatAssociation );
    }

}