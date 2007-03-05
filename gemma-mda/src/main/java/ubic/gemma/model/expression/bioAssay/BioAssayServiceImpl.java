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
package ubic.gemma.model.expression.bioAssay;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.biomaterial.BioMaterial;

/**
 * @author pavlidis
 * @author keshav
 * @author joseph
 * @version $Id$
 * @see ubic.gemma.model.expression.bioAssay.BioAssayService
 */
public class BioAssayServiceImpl extends ubic.gemma.model.expression.bioAssay.BioAssayServiceBase {
    private static Log log = LogFactory.getLog( BioAssayServiceImpl.class.getName() );

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#saveBioAssay(edu.columbia.gemma.expression.bioAssay.BioAssay)
     */
    protected void handleSaveBioAssay( ubic.gemma.model.expression.bioAssay.BioAssay bioAssay )
            throws java.lang.Exception {
        this.getBioAssayDao().create( bioAssay );
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        return this.getBioAssayDao().countAll();
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#getAllBioAssays()
     */
    @Override
    protected java.util.Collection handleGetAllBioAssays() throws java.lang.Exception {
        return this.getBioAssayDao().loadAll();
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#findOrCreate(edu.columbia.gemma.expression.bioAssay.BioAssay)
     */
    @Override
    protected BioAssay handleFindOrCreate( BioAssay bioAssay ) throws Exception {
        return this.getBioAssayDao().findOrCreate( bioAssay );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#remove(edu.columbia.gemma.expression.bioAssay.BioAssay)
     */
    @Override
    protected void handleRemove( BioAssay bioAssay ) throws Exception {
        this.getBioAssayDao().remove( bioAssay );

    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#findById(Long)
     */
    @Override
    protected BioAssay handleFindById( Long id ) throws Exception {
        return this.getBioAssayDao().findById( id );
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#loadAll()
     */
    @Override
    protected Collection handleLoadAll() throws Exception {
        return this.getBioAssayDao().loadAll();
    }

    /**
     * @see ubic.gemma.model.expression.bioAssay.BioAssayService#update(BioAssay)
     */
    @Override
    protected void handleUpdate( BioAssay bioAssay ) throws Exception {
        this.getBioAssayDao().update( bioAssay );
    }

    @Override
    protected Collection handleFindBioAssayDimensions( BioAssay bioAssay ) throws Exception {
        if ( bioAssay.getId() == null ) throw new IllegalArgumentException( "BioAssay must be persistent" );
        return this.getBioAssayDao().findBioAssayDimensions( bioAssay );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssay.BioAssayServiceBase#handleAssociateBioMaterial(ubic.gemma.model.expression.bioAssay.BioAssay,
     *      ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    protected void handleAddBioMaterialAssociation( BioAssay bioAssay, BioMaterial bioMaterial ) throws Exception {
        // add bioMaterial to bioAssay
        Collection<BioMaterial> currentBioMaterials = bioAssay.getSamplesUsed();
        currentBioMaterials.add( bioMaterial );
        bioAssay.setSamplesUsed( currentBioMaterials );

        // add bioAssay to bioMaterial
        Collection<BioAssay> currentBioAssays = bioMaterial.getBioAssaysUsedIn();
        currentBioAssays.add( bioAssay );
        bioMaterial.setBioAssaysUsedIn( currentBioAssays );

        // update bioMaterial name - remove text after pipes
        // this should not be necessary going forward

        // build regular expression - match only text before the first pipe
        Pattern pattern = Pattern.compile( "^(.*?)|" );
        String bmName = bioMaterial.getName();
        Matcher matcher = pattern.matcher( bmName );
        if ( matcher.find() ) {
            String shortName = matcher.group();
            bioMaterial.setName( shortName );
        }

        // TODO make this transactional
        // update bioAssay
        this.update( bioAssay );
        // update bioMaterial
        this.getBioMaterialService().update( bioMaterial );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.bioAssay.BioAssayServiceBase#handleRemoveBioMaterial(ubic.gemma.model.expression.bioAssay.BioAssay,
     *      ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    @Override
    protected void handleRemoveBioMaterialAssociation( BioAssay bioAssay, BioMaterial bioMaterial ) throws Exception {
        // remove bioMaterial from bioAssay
        Collection<BioMaterial> currentBioMaterials = bioAssay.getSamplesUsed();
        currentBioMaterials.remove( bioMaterial );
        bioAssay.setSamplesUsed( currentBioMaterials );

        // remove bioAssay from bioMaterial
        Collection<BioAssay> currentBioAssays = bioMaterial.getBioAssaysUsedIn();
        currentBioAssays.remove( bioAssay );
        bioMaterial.setBioAssaysUsedIn( currentBioAssays );

        // TODO make this transactional

        // update bioAssay
        this.update( bioAssay );

        // check to see if the bioMaterial is now orphaned.
        // if it is, delete it
        // if not, update it
        if ( currentBioAssays.size() == 0 ) {
            this.getBioMaterialService().remove( bioMaterial );
        } else {
            // update bioMaterial
            this.getBioMaterialService().update( bioMaterial );
        }

    }

}