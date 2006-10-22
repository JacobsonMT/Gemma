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
package ubic.gemma.model.expression.bioAssayData;

import java.util.Collection;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService
 */
public class BioAssayDimensionServiceImpl extends ubic.gemma.model.expression.bioAssayData.BioAssayDimensionServiceBase {

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService#findOrCreate(ubic.gemma.model.expression.bioAssayData.BioAssayDimension)
     */
    @Override
    protected ubic.gemma.model.expression.bioAssayData.BioAssayDimension handleFindOrCreate(
            ubic.gemma.model.expression.bioAssayData.BioAssayDimension bioAssayDimension ) throws java.lang.Exception {
        return this.getBioAssayDimensionDao().findOrCreate( bioAssayDimension );
    }

    @Override
    protected Collection handleGetDesignElementDataVectors( BioAssayDimension bioAssayDimension ) throws Exception {
        if ( bioAssayDimension.getId() == null ) {
            throw new IllegalArgumentException( "BioAssayDimension must be persistent" );
        }
        return this.getBioAssayDimensionDao().findDesignElementDataVectors( bioAssayDimension.getId() );
    }

}