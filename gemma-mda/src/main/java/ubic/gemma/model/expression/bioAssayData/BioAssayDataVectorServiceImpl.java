/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package ubic.gemma.model.expression.bioAssayData;

/**
 * @author pavlidis
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorService
 */
public class BioAssayDataVectorServiceImpl extends
        ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorServiceBase {

    /**
     * @see ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorService#saveBioAssayDataVector(ubic.gemma.model.expression.bioAssayData.BioAssayDataVector)
     */
    protected void handleSaveBioAssayDataVector(
            ubic.gemma.model.expression.bioAssayData.BioAssayDataVector bioAssayDataVector )
            throws java.lang.Exception {
        this.getBioAssayDataVectorDao().create( bioAssayDataVector );
    }

    @Override
    protected BioAssayDataVector handleFindOrCreate( BioAssayDataVector bioAssayDataVector ) throws Exception {
        return this.getBioAssayDataVectorDao().findOrCreate( bioAssayDataVector );
    }

    @Override
    protected void handleRemove( BioAssayDataVector bioAssayDataVector ) throws Exception {
        this.getBioAssayDataVectorDao().remove( bioAssayDataVector );
    }

}