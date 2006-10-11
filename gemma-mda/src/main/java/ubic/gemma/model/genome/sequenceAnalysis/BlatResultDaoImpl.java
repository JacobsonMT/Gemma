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
import java.util.List;

import org.hibernate.Criteria;

import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.util.BusinessKey;

/**
 * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResult
 */
public class BlatResultDaoImpl extends ubic.gemma.model.genome.sequenceAnalysis.BlatResultDaoBase {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultDaoBase#find(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<BlatResult> findByBioSequence( BioSequence bioSequence ) {
        BusinessKey.checkValidKey( bioSequence );

        Criteria queryObject = super.getSession( false ).createCriteria( BlatResult.class );

        BusinessKey.attachCriteria( queryObject, bioSequence, "querySequence" );

        List results = queryObject.list();

        if ( results != null ) {
            for ( Object object : results ) {
                BlatResult br = ( BlatResult ) object;
                if ( br.getTargetChromosome() != null ) {
                    br.getTargetChromosome().getName(); // to initialize the proxies.
                }
                br.getQuerySequence();
            }
        }

        return results;
    }
}