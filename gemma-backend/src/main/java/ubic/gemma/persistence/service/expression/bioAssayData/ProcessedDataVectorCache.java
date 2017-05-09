/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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
package ubic.gemma.persistence.service.expression.bioAssayData;

import java.util.Collection;

import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.experiment.BioAssaySet;

/**
 * Cache of data vectors
 * 
 * @author Paul
 * @version $Id$
 */
public interface ProcessedDataVectorCache {

    /**
     * @param eeid
     * @param g
     * @param collection
     */
    public abstract void addToCache( Long eeid, Long g, Collection<DoubleVectorValueObject> collection );

    public abstract void clearCache();

    /**
     * Remove cached items for experiment with given id.
     * 
     * @param eeid
     */
    public abstract void clearCache( Long eeid );

    /**
     * @param ee
     * @param g
     * @return
     */
    public abstract Collection<DoubleVectorValueObject> get( BioAssaySet ee, Long g );

    /**
     * @return number of elements currently in the cache. Warning: expensive operation, and only an approximate count.
     */
    public abstract int size();

}