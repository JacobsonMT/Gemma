/*
 * The Gemma project
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
package ubic.gemma.analysis.preprocess;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

/**
 * Computes a missing value matrix for ratiometric data sets.
 * <p>
 * Supported formats and special cases:
 * <ul>
 * <li>Genepix: CH1B_MEDIAN etc; (various versions)</li>
 * <li>Incyte GEMTools: RAW_DATA etc (no background values)</li>
 * <li>Quantarray: CH1_BKD etc</li>
 * <li>F635.Median / F532.Median (genepix as rendered in some data sets)</li>
 * <li>CH1_SMTM (found in GPL230)</li>
 * <li>Caltech (GPL260)</li>
 * <li>Agilent (Ch2BkgMedian etc or CH2_SIG_MEAN etc)</li>
 * <li>GSE3251 (ch1.Background etc)
 * <li>GPL560 (*_CY3 vs *CY5)
 * <li>GSE1501 (NormCH2)
 * </ul>
 * <p>
 * The missing values are computed with the following considerations with respect to available data
 * </p>
 * <ol>
 * <li>This only works if there are signal values for both channels
 * <li>If there are background values, they are used to compute signal-to-noise ratios</li>
 * <li>If the signal values already contain missing data, these are still considered missing.</li>
 * <li>If there are no background values, all values will be considered 'present' unless the signal values are both
 * zero or missing.
 * <li>If the preferred quantitation type data is a missing value, then the data are considered missing (for
 * consistency).
 * </ol>
 * 
 * @spring.bean id="twoChannelMissingValues"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="designElementDataVectorService" ref="designElementDataVectorService"
 * @author pavlidis
 * @version $Id$
 */
public class TwoChannelMissingValues {

    private static Log log = LogFactory.getLog( TwoChannelMissingValues.class.getName() );

    private ExpressionExperimentService expressionExperimentService;

    private DesignElementDataVectorService designElementDataVectorService;

    /**
     * @param expExp The expression experiment to analyze. The quantitation types to use are selected automatically. If
     *        you want more control use other computeMissingValues methods.
     * @param ad The array design to consider; this can be null, but should be included if the expression experiment
     *        uses more than one array design.
     * @param signalToNoiseThreshold A value such as 1.5 or 2.0; only spots for which at least ONE of the channel signal
     *        is more than signalToNoiseThreshold*background (and the preferred data are not missing) will be considered
     *        present.
     * @return DesignElementDataVectors corresponding to a new PRESENTCALL quantitation type for the experiment.
     */
    @SuppressWarnings("unchecked")
    public Collection<DesignElementDataVector> computeMissingValues( ExpressionExperiment expExp, ArrayDesign ad,
            double signalToNoiseThreshold ) {

        expressionExperimentService.thawLite( expExp );
        Collection<DesignElementDataVector> vectors = expressionExperimentService.getDesignElementDataVectors( expExp,
                ExpressionDataMatrixBuilder.getUsefulQuantitationTypes( expExp ) );

        designElementDataVectorService.thaw( vectors );

        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( vectors );
        Collection<BioAssayDimension> dims = builder.getBioAssayDimensions( ad );
        Collection<DesignElementDataVector> finalResults = new HashSet<DesignElementDataVector>();

        /*
         * Note we have to do this one array design at a time, because we are producing DesignElementDataVectors which
         * must be associated with the correct BioAssayDimension.
         */
        for ( BioAssayDimension bioAssayDimension : dims ) {
            Collection<BioAssay> bioAssays = bioAssayDimension.getBioAssays();
            Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
            for ( BioAssay ba : bioAssays ) {
                ads.add( ba.getArrayDesignUsed() );
            }
            if ( ads.size() > 1 ) {
                throw new IllegalArgumentException( "Can't handle vectors with multiple array design represented" );
            }
            ArrayDesign ades = bioAssayDimension.getBioAssays().iterator().next().getArrayDesignUsed();
            ExpressionDataDoubleMatrix preferredData = builder.getPreferredData( ades );
            ExpressionDataDoubleMatrix bkgDataA = builder.getBackgroundChannelA( ades );
            ExpressionDataDoubleMatrix bkgDataB = builder.getBackgroundChannelB( ades );
            ExpressionDataDoubleMatrix signalDataA = builder.getSignalChannelA( ades );
            ExpressionDataDoubleMatrix signalDataB = builder.getSignalChannelB( ades );
            Collection<DesignElementDataVector> dimRes = computeMissingValues( expExp, bioAssayDimension,
                    preferredData, signalDataA, signalDataB, bkgDataA, bkgDataB, signalToNoiseThreshold );

            finalResults.addAll( dimRes );
        }
        return finalResults;
    }

    /**
     * @param source
     * @param bioAssayDimension
     * @param preferred
     * @param signalChannelA
     * @param signalChannelB
     * @param bkgChannelA
     * @param bkgChannelB
     * @param signalToNoiseThreshold
     * @return DesignElementDataVectors corresponding to a new PRESENTCALL quantitation type for the design elements and
     *         biomaterial dimension represented in the inputs.
     * @see computeMissingValues( ExpressionExperiment expExp, double signalToNoiseThreshold )
     */
    public Collection<DesignElementDataVector> computeMissingValues( ExpressionExperiment source,
            BioAssayDimension bioAssayDimension, ExpressionDataDoubleMatrix preferred,
            ExpressionDataDoubleMatrix signalChannelA, ExpressionDataDoubleMatrix signalChannelB,
            ExpressionDataDoubleMatrix bkgChannelA, ExpressionDataDoubleMatrix bkgChannelB,
            double signalToNoiseThreshold ) {

        validate( preferred, signalChannelA, signalChannelB, bkgChannelA, bkgChannelB, signalToNoiseThreshold );

        ByteArrayConverter converter = new ByteArrayConverter();
        Collection<DesignElementDataVector> results = new HashSet<DesignElementDataVector>();
        QuantitationType present = getQuantitationType( signalToNoiseThreshold );
        source.getQuantitationTypes().add(present);

        int count = 0;
        for ( ExpressionDataMatrixRowElement element : signalChannelA.getRowElements() ) {

            DesignElement designElement = element.getDesignElement();

            DesignElementDataVector vect = DesignElementDataVector.Factory.newInstance();
            vect.setQuantitationType( present );
            vect.setExpressionExperiment( source );
            vect.setDesignElement( designElement );
            vect.setBioAssayDimension( bioAssayDimension );

            // FIXME preferred.columns is slow.
            int numCols = preferred.columns( designElement );

            boolean[] detectionCalls = new boolean[numCols];
            Double[] prefRow = preferred.getRow( designElement );

            Double[] signalA = signalChannelA != null ? signalChannelA.getRow( designElement ) : null;
            Double[] signalB = signalChannelB != null ? signalChannelB.getRow( designElement ) : null;
            Double[] bkgA = null;
            Double[] bkgB = null;

            if ( bkgChannelA != null ) bkgA = bkgChannelA.getRow( designElement );

            if ( bkgChannelB != null ) bkgB = bkgChannelB.getRow( designElement );

            // columsn only for this designelement!

            for ( int col = 0; col < numCols; col++ ) {

                // If the "preferred" value is already missing, we retain that.
                Double pref = prefRow == null ? Double.NaN : prefRow[col];
                if ( pref == null || pref.isNaN() ) {
                    detectionCalls[col] = false;
                    continue;
                }

                Double bkgAV = 0.0;
                Double bkgBV = 0.0;

                if ( bkgA != null ) bkgAV = bkgA[col];

                if ( bkgB != null ) bkgBV = bkgB[col];

                Double sigAV = signalA[col] == null ? 0.0 : signalA[col];
                Double sigBV = signalB[col] == null ? 0.0 : signalB[col];

                boolean call = computeCall( signalToNoiseThreshold, sigAV, sigBV, bkgAV, bkgBV );
                detectionCalls[col] = call;
            }

            vect.setData( converter.booleanArrayToBytes( detectionCalls ) );
            results.add( vect );

            if ( ++count % 4000 == 0 ) {
                log.info( count + " vectors examined for missing values, " + results.size()
                        + " vectors generated so far." );
            }

        }
        log.info( "Finished: " + count + " vectors examined for missing values" );
        return results;
    }

    private boolean computeCall( double signalToNoiseThreshold, Double sigAV, Double sigBV, Double bkgAV, Double bkgBV ) {
        if ( ( sigAV == null && sigBV == null ) || ( sigAV.isNaN() && sigBV.isNaN() ) ) return false;
        return sigAV > bkgAV * signalToNoiseThreshold || sigBV > bkgBV * signalToNoiseThreshold;
    }

    /**
     * Construct the quantitation type that will be used for the generated DesignElementDataVEctors.
     * 
     * @param signalToNoiseThreshold
     * @return
     */
    private QuantitationType getQuantitationType( double signalToNoiseThreshold ) {
        QuantitationType present = QuantitationType.Factory.newInstance();
        present.setName( "Detection call" );
        present.setDescription( "Detection call based on signal to noise threshold of " + signalToNoiseThreshold
                + " (Computed by Gemma)" );
        present.setGeneralType( GeneralType.CATEGORICAL );
        present.setIsBackground( false );
        present.setRepresentation( PrimitiveType.BOOLEAN );
        present.setScale( ScaleType.OTHER );
        present.setIsPreferred( false );
        present.setIsBackgroundSubtracted( false );
        present.setIsNormalized( false );
        present.setIsRatio( false );
        present.setType( StandardQuantitationType.PRESENTABSENT );
        return present;
    }

    /**
     * Check to make sure all the pieces are correctly in place to do the computation.
     * 
     * @param preferred
     * @param signalChannelA
     * @param signalChannelB
     * @param bkgChannelA
     * @param bkgChannelB
     * @param signalToNoiseThreshold
     */
    private void validate( ExpressionDataDoubleMatrix preferred, ExpressionDataDoubleMatrix signalChannelA,
            ExpressionDataDoubleMatrix signalChannelB, ExpressionDataDoubleMatrix bkgChannelA,
            ExpressionDataDoubleMatrix bkgChannelB, double signalToNoiseThreshold ) {
        // not exhaustive...
        if ( preferred == null || signalChannelA == null || signalChannelB == null ) {
            throw new IllegalArgumentException( "Must have data matrices" );
        }

        if ( ( bkgChannelA != null && bkgChannelA.rows() == 0 ) || ( bkgChannelB != null && bkgChannelB.rows() == 0 ) ) {
            throw new IllegalArgumentException( "Background values must not be empty when non-null" );
        }

        if ( !( signalChannelA.rows() == signalChannelB.rows() ) ) {
            log.warn( "Collection sizes probably should match in channel A and B " + signalChannelA.rows() + " != "
                    + signalChannelB.rows() );
        }

        if ( !( signalChannelA.rows() == preferred.rows() ) ) { // vectors with all-missing data are already removed
            log.warn( "Collection sizes probably should match in channel A and preferred type " + signalChannelA.rows()
                    + " != " + preferred.rows() );
        }

        if ( ( bkgChannelA != null && bkgChannelB != null ) && bkgChannelA.rows() != bkgChannelB.rows() )
            log.warn( "Collection sizes probably should match for background  " + bkgChannelA.rows() + " != "
                    + bkgChannelB.rows() );

        if ( signalToNoiseThreshold <= 0.0 ) {
            throw new IllegalArgumentException( "Signal-to-noise threshold must be greater than zero" );
        }

        int numSamplesA = signalChannelA.columns();
        int numSamplesB = signalChannelB.columns();

        if ( numSamplesA != numSamplesB || numSamplesB != preferred.columns() ) {
            throw new IllegalArgumentException( "Number of samples doesn't match!" );
        }

    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
        this.designElementDataVectorService = designElementDataVectorService;
    }
}
