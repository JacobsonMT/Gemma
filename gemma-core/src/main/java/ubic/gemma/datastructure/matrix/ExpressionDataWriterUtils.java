/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.datastructure.matrix;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.util.DateUtil;

/**
 * @author keshav
 * @version $Id$
 */
public class ExpressionDataWriterUtils {

    private static final String WEBSITE = "http://bioinformatics.ubc.ca/Gemma";

    /**
     * Appends base header information (about the experiment) to a file.
     * 
     * @param experiment
     * @param design
     * @param buf
     */
    public static void appendBaseHeader( ExpressionExperiment experiment, boolean design, StringBuffer buf ) {
        String fileType = "data";

        if ( design ) fileType = "design";

        buf.append( "# Expression " + fileType + " file generated by Gemma on "
                + DateUtil.convertDateToString( new Date() ) + "\n" );
        if ( experiment != null ) {
            buf.append( "# shortName=" + experiment.getShortName() + "\n" );
            buf.append( "# name=" + experiment.getName() + "\n" );
            buf.append( "# Experiment details: " + WEBSITE + "/expressionExperiment/showExpressionExperiment.html?id="
                    + experiment.getId() + "\n" );
        }

        buf.append( "# If you use this file for your research, please cite the Gemma web site: "
                + ExpressionDataWriterUtils.WEBSITE + "\n" );

    }

    /**
     * Contstructs a bioassay name. This is useful when writing out data to a file.
     * 
     * @param matrix
     * @param assayColumnIndex The column index in the matrix.
     * @return
     */
    @SuppressWarnings("unchecked")
    public static String constructBioAssayName( ExpressionDataMatrix matrix, int assayColumnIndex ) {

        BioMaterial bioMaterialForColumn = matrix.getBioMaterialForColumn( assayColumnIndex );
        Collection<BioAssay> bioAssaysForColumn = matrix.getBioAssaysForColumn( assayColumnIndex );

        return constructBioAssayName( bioMaterialForColumn, bioAssaysForColumn );

    }

    /**
     * @param bioMaterial
     * @param bioAssays
     * @return
     */
    public static String constructBioAssayName( BioMaterial bioMaterial, Collection<BioAssay> bioAssays ) {
        StringBuffer colBuf = new StringBuffer();
        colBuf.append( bioMaterial.getName() + ":" );

        for ( Iterator<BioAssay> it = bioAssays.iterator(); it.hasNext(); ) {
            BioAssay ba = it.next();
            colBuf.append( ba.getName() );
            if ( it.hasNext() ) {
                colBuf.append( "," );
            }
        }
        String colName = StringUtils.deleteWhitespace( colBuf.toString() );

        String rCompatibleColName = constructRCompatibleBioAssayName( colName );

        return rCompatibleColName;
    }

    /**
     * @param colName
     * @return
     */
    private static String constructRCompatibleBioAssayName( String colName ) {

        colName = StringUtils.replaceChars( colName, ':', '.' );
        colName = StringUtils.replaceChars( colName, '|', '.' );
        colName = StringUtils.replaceChars( colName, '-', '.' );

        return colName;
    }

    /**
     * Replaces spaces and hyphens with underscores and removes the factor preamble ("factor name:").
     * 
     * @param factor
     * @param factorValue
     * @return
     */
    public static String constructFactorValueName( ExperimentalFactor factor, FactorValue factorValue ) {
        String matchedFactorValue = factorValue.toString();
        matchedFactorValue = StringUtils.removeStart( matchedFactorValue, factor.getName() + ":" );
        matchedFactorValue = matchedFactorValue.trim();
        matchedFactorValue = matchedFactorValue.replaceAll( "-", "_" );
        matchedFactorValue = matchedFactorValue.replaceAll( "\\s", "_" );
        return matchedFactorValue;
    }

}
