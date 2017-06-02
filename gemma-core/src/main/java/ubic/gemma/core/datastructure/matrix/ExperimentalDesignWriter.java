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
package ubic.gemma.core.datastructure.matrix;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.StringUtil;
import ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporterImpl;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * Output compatible with {@link ExperimentalDesignImporterImpl}.
 * 
 * @author keshav
 * @version $Id$
 * @see ExperimentalDesignImporterImpl
 */
public class ExperimentalDesignWriter {

    private Log log = LogFactory.getLog( this.getClass() );

    /**
     * @param writer
     * @param ExpressionExperiment ee
     * @param writeHeader
     * @param sortByDesign whether the design should be arranged in the order defined by
     *        ExpressionDataMatrixColumnSort.orderByExperimentalDesign
     * @throws IOException
     */
    public void write( Writer writer, ExpressionExperiment ee, boolean writeHeader, boolean sortByDesign )
            throws IOException {

        Collection<BioAssay> bioAssays = ee.getBioAssays();
        boolean writeBaseHeader = writeHeader;
        write( writer, ee, bioAssays, writeBaseHeader, writeHeader, sortByDesign );
    }

    /**
     * @param writer
     * @param ExpressionExperiment ee
     * @param writeBaseHeader comments
     * @param writeHeader column names
     * @param bioAssays
     * @param sortByDesign whether the design should be arranged in the order defined by
     *        ExpressionDataMatrixColumnSort.orderByExperimentalDesign
     * @throws IOException
     */
    public void write( Writer writer, ExpressionExperiment ee, Collection<BioAssay> bioAssays, boolean writeBaseHeader, boolean writeHeader,
            boolean sortByDesign )
            throws IOException {

        ExperimentalDesign ed = ee.getExperimentalDesign();

        /*
         * See BaseExpressionDataMatrix.setUpColumnElements() for how this is constructed for the DataMatrix, and for
         * some notes about complications.
         */
        Map<BioMaterial, Collection<BioAssay>> bioMaterials = new HashMap<>();
        for ( BioAssay bioAssay : bioAssays ) {
            BioMaterial bm = bioAssay.getSampleUsed();
            if ( !bioMaterials.containsKey( bm ) ) {
                bioMaterials.put( bm, new HashSet<BioAssay>() );
            }
            bioMaterials.get( bm ).add( bioAssay );
        }

        Collection<ExperimentalFactor> efs = ed.getExperimentalFactors();

        List<ExperimentalFactor> orderedFactors = new ArrayList<>();
        orderedFactors.addAll( efs );

        StringBuffer buf = new StringBuffer();

        if ( writeHeader ) {
            writeHeader( writer, ee, orderedFactors, writeBaseHeader, buf );
        }

        for ( BioMaterial bioMaterial : bioMaterials.keySet() ) {

            /* column 0 of the design matrix */
            String rowName = ExpressionDataWriterUtils.constructBioAssayName( bioMaterial,
                    bioMaterials.get( bioMaterial ) );
            buf.append( rowName );

            buf.append( "\t" );

            /* column 1 */
            String externalId = ExpressionDataWriterUtils.getExternalId( bioMaterial, bioMaterials.get( bioMaterial ) );

            buf.append( externalId );

            /* columns 2 ... n where n+1 is the number of factors */
            Collection<FactorValue> candidateFactorValues = bioMaterial.getFactorValues();
            for ( ExperimentalFactor ef : orderedFactors ) {
                buf.append( "\t" );
                for ( FactorValue candidateFactorValue : candidateFactorValues ) {
                    if ( candidateFactorValue.getExperimentalFactor().equals( ef ) ) {
                        log.debug( candidateFactorValue.getExperimentalFactor() + " matched." );
                        String matchedFactorValue = ExpressionDataWriterUtils
                                .constructFactorValueName( candidateFactorValue );
                        buf.append( matchedFactorValue );
                        break;
                    }
                    log.debug( candidateFactorValue.getExperimentalFactor()
                            + " didn't match ... trying the next factor." );
                }
            }
            buf.append( "\n" );
        }

        if ( log.isDebugEnabled() ) log.debug( buf.toString() );

        writer.write( buf.toString() );
        writer.flush();

    }

    /**
     * Write an (R-friendly) header
     * 
     * @param writer
     * @param expressionExperiment
     * @param factors
     * @param writeBaseHeader
     * @param buf
     */
    private void writeHeader( Writer writer, ExpressionExperiment expressionExperiment,
            Collection<ExperimentalFactor> factors, boolean writeBaseHeader, StringBuffer buf ) {

        if ( writeBaseHeader ) {
            ExpressionDataWriterUtils.appendBaseHeader( expressionExperiment, true, buf );
        }

        for ( ExperimentalFactor ef : factors ) {
            buf.append( ExperimentalDesignImporterImpl.EXPERIMENTAL_FACTOR_DESCRIPTION_LINE_INDICATOR );
            buf.append( ef.getName() + " :" );
            if ( ef.getCategory() != null ) {
                buf.append( " Category=" + ef.getCategory().getValue().replaceAll( "\\s", "_" ) );
            }
            buf.append( " Type=" );

            if ( ef.getType().equals( FactorType.CATEGORICAL ) ) {
                buf.append( "Categorical" );
            } else {
                buf.append( "Continuous" );
            }

            buf.append( "\n" );
        }

        buf.append( "Bioassay\tExternalID" );

        for ( ExperimentalFactor ef : factors ) {
            String efName = StringUtil.makeValidForR( ef.getName() );
            buf.append( "\t" + efName );
        }

        buf.append( "\n" );
    }

}
