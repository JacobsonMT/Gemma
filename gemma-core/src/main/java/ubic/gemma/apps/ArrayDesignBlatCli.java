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
package ubic.gemma.apps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;

import ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentService;
import ubic.gemma.loader.genome.BlatResultParser;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignSequenceAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

/**
 * Command line interface to run blat on the sequences for a microarray; the results are persisted in the DB. You must
 * start the BLAT server first before using this.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignBlatCli extends ArrayDesignSequenceManipulatingCli {
    ArrayDesignSequenceAlignmentService arrayDesignSequenceAlignmentService;

    String blatResultFile = null;

    Double blatScoreThreshold = Blat.DEFAULT_BLAT_SCORE_THRESHOLD;

    private TaxonService taxonService;

    private String taxonName;

    private Taxon taxon;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();

        Option blatResultOption = OptionBuilder.hasArg().withArgName( "PSL file" ).withDescription(
                "Blat result file in PSL format (if supplied, BLAT will not be run); -t option overrides" )
                .withLongOpt( "blatfile" ).create( 'b' );

        Option blatScoreThreshold = OptionBuilder.hasArg().withArgName( "Blat score threshold" ).withDescription(
                "Threshold (0-1.0) for acceptance of BLAT alignments [Default = " + this.blatScoreThreshold + "]" )
                .withLongOpt( "scoreThresh" ).create( 's' );

        Option taxonOption = OptionBuilder
                .hasArg()
                .withArgName( "taxon" )
                .withDescription(
                        "Taxon common name (e.g., human); blat will be run for all ArrayDesigns from that taxon (overrides -a and -b)" )
                .create( 't' );

        addOption( taxonOption );

        addOption( blatScoreThreshold );
        addOption( blatResultOption );
    }

    public static void main( String[] args ) {
        ArrayDesignBlatCli p = new ArrayDesignBlatCli();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine(
                "Array design sequence BLAT - only works if server is already started or if a PSL file is provided!",
                args );
        if ( err != null ) return err;

        if ( StringUtils.isNotBlank( this.arrayDesignName ) ) {

            ArrayDesign arrayDesign = locateArrayDesign( arrayDesignName );

            unlazifyArrayDesign( arrayDesign );
            Collection<BlatResult> persistedResults;
            try {
                if ( this.blatResultFile != null ) {
                    Collection<BlatResult> blatResults = getBlatResultsFromFile( arrayDesign );

                    if ( blatResults == null || blatResults.size() == 0 ) {
                        throw new IllegalStateException( "No blat results in file!" );
                    }

                    log.info( "Got " + blatResults.size() + " blat records" );
                    persistedResults = arrayDesignSequenceAlignmentService
                            .processArrayDesign( arrayDesign, blatResults );
                    audit( arrayDesign, "BLAT results read from file: " + blatResultFile );
                } else {
                    // Run blat from scratch.
                    persistedResults = arrayDesignSequenceAlignmentService.processArrayDesign( arrayDesign );
                    audit( arrayDesign, "Based on a fresh alignment analysis" );
                }
                log.info( "Persisted " + persistedResults.size() + " results" );
            } catch ( FileNotFoundException e ) {
                return e;
            } catch ( IOException e ) {
                return e;
            }

        } else if ( taxon != null ) {
            log.warn( "*** Running BLAT for all " + taxon.getCommonName() + " Array designs *** " );
            Collection<String> errorObjects = new HashSet<String>();
            Collection<String> persistedObjects = new HashSet<String>();

            Collection<ArrayDesign> allArrayDesigns = arrayDesignService.loadAll();
            for ( ArrayDesign design : allArrayDesigns ) {
                if ( taxon.equals( arrayDesignService.getTaxon( design.getId() ) ) ) {
                    log.info( "============== Start processing: " + design + " ==================" );
                    try {
                        arrayDesignService.thaw( design );
                        arrayDesignSequenceAlignmentService.processArrayDesign( design );
                        persistedObjects.add( design.getName() );
                        audit( design, "Part of a batch job" );
                    } catch ( Exception e ) {
                        errorObjects.add( design + ": " + e.getMessage() );
                        log.error( "**** Exception while processing " + design + ": " + e.getMessage() + " ****" );
                        log.error( e, e );
                    }
                }
            }
            summarizeProcessing( errorObjects, persistedObjects );
        } else {
            bail( ErrorCode.MISSING_ARGUMENT );
        }

        return null;
    }

    /**
     * @param arrayDesign
     * @return
     * @throws IOException
     */
    private Collection<BlatResult> getBlatResultsFromFile( ArrayDesign arrayDesign ) throws IOException {
        File f = new File( blatResultFile );
        if ( !f.canRead() ) {
            log.error( "Cannot read from " + blatResultFile );
            bail( ErrorCode.INVALID_OPTION );
        }
        Taxon taxon = arrayDesignService.getTaxon( arrayDesign.getId() );
        log.info( "Reading blat results in from " + f.getAbsolutePath() );
        BlatResultParser parser = new BlatResultParser();
        parser.setScoreThreshold( this.blatScoreThreshold );
        parser.setTaxon( taxon );
        parser.parse( f );
        Collection<BlatResult> blatResults = parser.getResults();
        return blatResults;
    }

    /**
     * @param arrayDesign
     */
    private void audit( ArrayDesign arrayDesign, String note ) {
        AuditEventType eventType = ArrayDesignSequenceAnalysisEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( arrayDesign, eventType, note );
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        if ( hasOption( 'b' ) ) {
            this.blatResultFile = this.getOptionValue( 'b' );
        }

        if ( hasOption( 's' ) ) {
            this.blatScoreThreshold = this.getDoubleOptionValue( 's' );
        }
        this.taxonService = ( TaxonService ) this.getBean( "taxonService" );

        if ( this.hasOption( 't' ) ) {
            this.taxonName = this.getOptionValue( 't' );
            this.taxon = taxonService.findByCommonName( this.taxonName );
            if ( taxon == null ) {
                throw new IllegalArgumentException( "No taxon named " + taxonName );
            }
        }

        arrayDesignSequenceAlignmentService = ( ArrayDesignSequenceAlignmentService ) this
                .getBean( "arrayDesignSequenceAlignmentService" );

    }

}
