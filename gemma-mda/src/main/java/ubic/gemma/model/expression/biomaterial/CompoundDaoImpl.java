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
package ubic.gemma.model.expression.biomaterial;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import ubic.gemma.util.BeanPropertyCompleter;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.biomaterial.Compound
 */
public class CompoundDaoImpl extends ubic.gemma.model.expression.biomaterial.CompoundDaoBase {

    private static Log log = LogFactory.getLog( CompoundDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.biomaterial.CompoundDaoBase#find(ubic.gemma.model.expression.biomaterial.Compound)
     */
    @Override
    public Compound find( Compound compound ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( Compound.class );
            queryObject.add( Restrictions.eq( "name", compound.getName() ) );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + Compound.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = ( Compound ) results.iterator().next();
                }
            }
            return ( Compound ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.biomaterial.CompoundDaoBase#findOrCreate(ubic.gemma.model.expression.biomaterial.Compound)
     */
    @Override
    public Compound findOrCreate( Compound compound ) {
        if ( compound.getName() == null ) {
            log.debug( "Compound must have a name to use as comparison key" );
            return null;
        }
        Compound newCompound = this.find( compound );
        if ( newCompound != null ) {
            BeanPropertyCompleter.complete( newCompound, compound );
            return newCompound;
        }
        log.debug( "Creating new compound: " + compound.getName() );
        return ( Compound ) create( compound );
    }
}