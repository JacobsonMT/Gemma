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

package ubic.gemma.util;

import ubic.gemma.model.genome.Taxon;

/**
 * A utility class for taxon.
 * <hr>
 * <p>
 * Copyright (c) 2006 UBC Pavlab
 * 
 * @author klc
 * @version $Id$
 */
public class TaxonUtility {

    /**
     * @param Taxon
     * @return boolean
     */
    public static boolean isHuman( Taxon tax ) {
        if ( tax.getNcbiId() == 9606 ) return true;
        if ( tax.getScientificName().equalsIgnoreCase( "homo sapiens" ) ) return true;
        if ( tax.getCommonName().equalsIgnoreCase( "human" ) ) return true;

        return false;
    }

    /**
     * @param Taxon
     * @return boolean
     */
    public static boolean isMouse( Taxon tax ) {

        if ( tax.getNcbiId() == 10090 ) return true;
        if ( tax.getScientificName().equalsIgnoreCase( "mus musculus" ) ) return true;
        if ( tax.getCommonName().equalsIgnoreCase( "mouse" ) ) return true;

        return false;
    }

    /**
     * @param Taxon
     * @return boolean
     */
    public static boolean isRat( Taxon tax ) {

        if ( tax.getNcbiId() == 10116 ) return true;
        if ( tax.getScientificName().equalsIgnoreCase( "Rattus norvegicus" ) ) return true;
        if ( tax.getCommonName().equalsIgnoreCase( "rat" ) ) return true;

        return false;

    }

}
