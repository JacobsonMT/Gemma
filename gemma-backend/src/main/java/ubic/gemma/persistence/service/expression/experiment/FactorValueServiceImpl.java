/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence.service.expression.experiment;

import java.util.Collection;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see FactorValueService
 */
@Service
public class FactorValueServiceImpl extends FactorValueServiceBase {

    @Override
    @Transactional(readOnly = true)
    public Collection<FactorValue> findByValue( String valuePrefix ) {
        return this.getFactorValueDao().findByValue( valuePrefix );
    }

    @Override
    protected FactorValue handleCreate( FactorValue factorValue ) {
        return this.getFactorValueDao().create( factorValue );
    }

    @Override
    protected void handleDelete( FactorValue factorValue ) {
        this.getFactorValueDao().remove( factorValue );
    }

    @Override
    protected FactorValue handleFindOrCreate( FactorValue factorValue ) {
        return this.getFactorValueDao().findOrCreate( factorValue );
    }

    /**
     * @see FactorValueService#getAllFactorValues()
     */
    @SuppressWarnings("unchecked")
    protected java.util.Collection<FactorValue> handleGetAllFactorValues() {
        return ( Collection<FactorValue> ) this.getFactorValueDao().loadAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see FactorValueServiceBase#handleLoad(java.lang.Long)
     */
    @Override
    protected FactorValue handleLoad( Long id ) {
        return this.getFactorValueDao().load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see FactorValueServiceBase#handleLoadAll()
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<FactorValue> handleLoadAll() {
        return ( Collection<FactorValue> ) this.getFactorValueDao().loadAll();
    }

    /**
     * @see FactorValueService#saveFactorValue(ubic.gemma.model.expression.experiment.FactorValue)
     */
    protected void handleSaveFactorValue( ubic.gemma.model.expression.experiment.FactorValue factorValue ) {
        this.getFactorValueDao().create( factorValue );
    }

    /*
     * (non-Javadoc)
     * 
     * @see FactorValueServiceBase#handleUpdate(java.util.Collection)
     */
    @Override
    protected void handleUpdate( Collection<FactorValue> factorValues ) {
        this.getFactorValueDao().update( factorValues );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * FactorValueServiceBase#handleUpdate(ubic.gemma.model.expression.experiment
     * .FactorValue)
     */
    @Override
    protected void handleUpdate( FactorValue factorValue ) {
        this.getFactorValueDao().update( factorValue );
    }

}