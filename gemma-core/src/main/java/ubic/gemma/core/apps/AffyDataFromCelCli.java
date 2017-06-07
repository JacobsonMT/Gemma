/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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

package ubic.gemma.core.apps;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.loader.expression.geo.DataUpdater;
import ubic.gemma.core.loader.expression.geo.model.GeoPlatform;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataReplacedEventImpl;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;

/**
 * Add (or possibly replace) the data associated with an affymetrix data set, going back to the CEL files. Can handle
 * exon or 3' arrays.
 *
 * @author paul
 * @version $Id$
 */
public class AffyDataFromCelCli extends ExpressionExperimentManipulatingCLI {

    private static final String APT_FILE_OPT = "aptFile";
    private static final String CDF_FILE_OPT = "cdfFile";
    private String aptFile = null;
    // /space/grp/databases/arrays/cdfs...
    private String cdfFile = null;

    /**
     * @param args
     */
    public static void main( String[] args ) {
        AffyDataFromCelCli c = new AffyDataFromCelCli();
        c.doWork( args );
    }

    /**
     * @param ee
     */
    public boolean checkForAlreadyDone( BioAssaySet ee ) {
        for ( QuantitationType qt : eeService.getQuantitationTypes( ( ExpressionExperiment ) ee ) ) {
            if ( qt.getIsMaskedPreferred() && qt.getIsRecomputedFromRawData() ) {
                return true;
            }
        }

        if ( super.auditEventService.hasEvent( ee, DataReplacedEventImpl.class ) ) {
            return true;
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.AbstractCLI#getCommandName()
     */
    @Override
    public String getCommandName() {
        return "affyFromCel";
    }

    @Override
    public String getShortDesc() {
        return "Reanalyze Affymetrix data from CEL files, if available; affy-power-tools must be configured.";
    }

    @Override
    protected void buildOptions() {
        super.buildOptions();
        super.addOption( APT_FILE_OPT, true,
                "File output from apt-probeset-summarize; use if you want to override usual GEO download behaviour; don't use with "
                        + CDF_FILE_OPT );
        super.addOption( CDF_FILE_OPT, true,
                "CDF file for Affy 3' arrays; otherwise will try to find automatically using the value of affy.power.tools.cdf.path; don't use with "
                        + APT_FILE_OPT );
        super.addForceOption();

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        super.processCommandLine( args );

        DataUpdater serv = getBean( DataUpdater.class );

        if ( StringUtils.isNotBlank( aptFile ) ) {
            if ( this.expressionExperiments.size() > 1 ) {
                throw new IllegalArgumentException( "Can't use -aptfile unless you are doing just one experiment" );
            }
            BioAssaySet ee = this.expressionExperiments.iterator().next();
            ExpressionExperiment thawedEe = ( ExpressionExperiment ) ee;
            this.eeService.thawLite( thawedEe );

            Collection<ArrayDesign> arrayDesignsUsed = this.eeService.getArrayDesignsUsed( ee );

            if ( arrayDesignsUsed.size() > 1 ) {
                throw new IllegalArgumentException( "Cannot update data for experiment that uses multiple platforms" );
            }

            ArrayDesign ad = arrayDesignsUsed.iterator().next();
            try {
                log.info( "Loading data from " + aptFile );
                if ( ad.getTechnologyType().equals( TechnologyType.ONECOLOR ) && (
                        GeoPlatform.isAffymetrixExonArray( ad.getShortName() ) || ad.getName().toLowerCase()
                                .contains( "exon" ) ) ) {
                    serv.addAffyExonArrayData( thawedEe, aptFile );
                } else if ( ad.getTechnologyType().equals( TechnologyType.ONECOLOR ) && ad.getName().toLowerCase()
                        .contains( "affy" ) ) {
                    serv.addAffyData( thawedEe, aptFile );
                } else {
                    throw new IllegalArgumentException( "Option -aptfile only valid if you are using an exon array." );
                }
            } catch ( Exception e ) {
                log.error( e, e );
                return e;
            }
            return null;
        }

        for ( BioAssaySet ee : this.expressionExperiments ) {
            try {
                /*
                 * if the audit trail already has a DataReplacedEvent, skip it, unless --force.
                 */
                if ( !force && checkForAlreadyDone( ee ) ) {
                    log.warn( ee + ": Already has been recomputed from raw data, skipping (use 'force' to override')" );
                    this.errorObjects.add( ee + ": Already has been computed from raw data" );
                    continue;
                }

                ExpressionExperiment thawedEe = ( ExpressionExperiment ) ee;
                this.eeService.thawLite( thawedEe );

                Collection<ArrayDesign> arrayDesignsUsed = this.eeService.getArrayDesignsUsed( ee );

                if ( arrayDesignsUsed.size() > 1 ) {
                    log.warn( ee + ": Cannot update data for experiment that uses multiple platforms" );
                    this.errorObjects.add( ee + ": Cannot update data for experiment that uses multiple platforms" );
                    continue;
                }

                ArrayDesign ad = arrayDesignsUsed.iterator().next();

                if ( ad.getName().toLowerCase().contains( "exon" ) && ad.getTechnologyType()
                        .equals( TechnologyType.ONECOLOR ) ) {
                    log.info( thawedEe + " looks like affy exon array" );
                    serv.addAffyExonArrayData( thawedEe );
                    this.successObjects.add( thawedEe.toString() );
                    log.info( "Successfully processed: " + thawedEe );
                } else if ( ad.getTechnologyType().equals( TechnologyType.ONECOLOR ) && ad.getName().toLowerCase()
                        .contains( "affy" ) ) {
                    log.info( thawedEe + " looks like a affy 3-prime array" );
                    serv.reprocessAffyThreePrimeArrayData( thawedEe, cdfFile );
                    this.successObjects.add( thawedEe.toString() );
                    log.info( "Successfully processed: " + thawedEe );
                } else {
                    log.warn( ee + ": This CLI can only deal with Affymetrix platforms (exon or 3' probe designs)" );
                    this.errorObjects.add( ee
                            + ": This CLI can only deal with Affymetrix platforms (exon or 3' probe designs)" );
                }
            } catch ( Exception e ) {
                log.error( e, e );
                this.errorObjects.add( ee + " " + e.getLocalizedMessage() );
            }

        }

        super.summarizeProcessing();

        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( APT_FILE_OPT ) ) {
            this.aptFile = getOptionValue( APT_FILE_OPT );
        }
        if ( hasOption( CDF_FILE_OPT ) ) {
            this.cdfFile = getOptionValue( CDF_FILE_OPT );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.AbstractCLIContextCLI#getCommandGroup()
     */
    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.EXPERIMENT;
    }

}
