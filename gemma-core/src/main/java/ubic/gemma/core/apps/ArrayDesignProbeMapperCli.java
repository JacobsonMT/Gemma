package ubic.gemma.core.apps;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.core.analysis.sequence.ProbeMapperConfig;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignProbeMapperService;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.persistence.util.Settings;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Process the blat results for an array design to map them onto genes.
 * Typical workflow would be to run:
 * <ol>
 * <li>Create the array design, perhaps by loading a GPL or via a GSE.
 * <li>ArrayDesignSequenceAssociationCli - attach sequences to array design, fetching from BLAST database if necessary.
 * For affymetrix designs, get the probe sequences and pass them to the command line (also a web interface)
 * <li>ArrayDesignBlatCli - runs blat. You must start the appropriate server using the configuration in the properties
 * files.
 * <li>ArrayDesignProbeMapperCli (this class); you must have the correct GoldenPath database installed and available as
 * configured in your properties files.
 * </ol>
 * This can also allow directly associating probes with genes (via products) based on an input file, without any
 * sequence analysis.
 *
 * @author pavlidis
 */
public class ArrayDesignProbeMapperCli extends ArrayDesignSequenceManipulatingCli {
    private static final String CONFIG_OPTION = "config";
    private static final String MIRNA_ONLY_MODE_OPTION = "mirna";
    private final static String OPTION_ASEMBLY = "a";
    private final static String OPTION_ENSEMBL = "n";
    private final static String OPTION_EST = "e"; // usually off
    private final static String OPTION_KNOWNGENE = "k";
    private final static String OPTION_MICRORNA = "i";
    private final static String OPTION_MRNA = "m";
    private final static String OPTION_NSCAN = "s";
    private final static String OPTION_REFSEQ = "r";

    private String[] probeNames = null;
    private ProbeMapperConfig config;
    private ArrayDesignProbeMapperService arrayDesignProbeMapperService;
    private String directAnnotationInputFileName = null;
    private boolean ncbiIds = false;
    private ExternalDatabase sourceDatabase = null;
    private Taxon taxon = null;
    private TaxonService taxonService;
    private boolean useDB = true;

    public static void main( String[] args ) {
        ArrayDesignProbeMapperCli p = new ArrayDesignProbeMapperCli();
        tryDoWorkNoExit( p, args );
    }

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.PLATFORM;
    }

    @Override
    public String getCommandName() {
        return "mapPlatformToGenes";
    }

    @Override
    public String getShortDesc() {
        return "Process the BLAT results for an array design to map them onto genes";
    }

    @SuppressWarnings("AccessStaticViaInstance")
    @Override
    protected void buildOptions() {
        super.buildOptions();

        requireLogin(); // actually only needed if using the db to save results (usual case)

        addOption( OptionBuilder.hasArg().withArgName( "value" ).withDescription(
                "Sequence identity threshold, default = " + ProbeMapperConfig.DEFAULT_IDENTITY_THRESHOLD )
                .withLongOpt( "identityThreshold" ).create( 'i' ) );

        addOption( OptionBuilder.hasArg().withArgName( "value" )
                .withDescription( "Blat score threshold, default = " + ProbeMapperConfig.DEFAULT_SCORE_THRESHOLD )
                .withLongOpt( "scoreThreshold" ).create( 's' ) );

        addOption( OptionBuilder.hasArg().withArgName( "value" ).withDescription(
                "Minimum fraction of probe overlap with exons, default = "
                        + ProbeMapperConfig.DEFAULT_MINIMUM_EXON_OVERLAP_FRACTION ).withLongOpt( "overlapThreshold" )
                .create( 'o' ) );

        addOption( OptionBuilder.withDescription(
                "Assign non-gene mappings to a ProbeAlignedRegion including creation of new ones (default="
                        + ProbeMapperConfig.DEFAULT_ALLOW_PARS + ")" ).create( "usePars" ) );

        addOption( OptionBuilder.withDescription(
                "Allow mapping to predicted genes (overrides Acembly, Ensembl and Nscan; default="
                        + ProbeMapperConfig.DEFAULT_ALLOW_PREDICTED + ")" ).create( "usePred" ) );

        addOption( OptionBuilder.hasArg().withArgName( "configstring" ).withDescription(
                "String describing which tracks to search, for example 'rkenmias' for all, 'rm' to limit search to Refseq with mRNA evidence. If this option is not set,"
                        + " all will be used except as listed below and in combination with the 'usePred' option:\n "

                        + OPTION_REFSEQ + " - search refseq track for genes (best to leave on)\n"

                        + OPTION_KNOWNGENE + " - search refseq track for genes (best to leave on)\n"

                        + OPTION_MICRORNA + " - search miRNA track for genes (doesn't hurt)\n"

                        + OPTION_EST + " - search EST track for transcripts (Default=false)\n"

                        + OPTION_MRNA + " - search mRNA track for transcripts (Default=false)\n"

                        + OPTION_ASEMBLY + " - search Acembly track for predicted genes\n"

                        + OPTION_ENSEMBL + " - search Ensembl track for predicted genes \n"

                        + OPTION_NSCAN + " - search NScan track for predicted genes\n" ).create( CONFIG_OPTION ) );

        addOption( OptionBuilder.withDescription(
                "Only seek miRNAs; this is the same as '-config " + OPTION_MICRORNA + "; overrides -config." )
                .create( MIRNA_ONLY_MODE_OPTION ) );

        Option taxonOption = OptionBuilder.hasArg().withArgName( "taxon" ).withDescription(
                "Taxon common name (e.g., human); if using '-import', this taxon will be assumed; otherwise analysis will be run for all"
                        + " ArrayDesigns from that taxon (overrides -a)" ).create( 't' );

        addOption( taxonOption );

        Option force = OptionBuilder.withDescription( "Run no matter what" ).create( "force" );

        addOption( force );

        Option directAnnotation = OptionBuilder.withDescription(
                "Import annotations from a file rather than our own analysis. You must provide the taxon option. "
                        + "File format: 2 columns with column 1= probe name in Gemma, "
                        + "column 2=sequence name (not required, and not used for direct gene-based annotation)"
                        + " column 3 = gene symbol (will be matched to that in Gemma)" ).hasArg().withArgName( "file" )
                .create( "import" );

        addOption( directAnnotation );

        addOption( "ncbi", false,
                "If set, it is assumed the direct annotation file is NCBI gene ids, not gene symbols (only valid with -import)" );

        Option databaseOption = OptionBuilder
                .withDescription( "Source database name (GEO etc); required if using -import" ).hasArg()
                .withArgName( "dbname" ).create( "source" );
        addOption( databaseOption );

        Option noDatabaseOption = OptionBuilder
                .withDescription( "Don't save the results to the database, print to stdout instead (not with -import)" )
                .create( "nodb" );

        addOption( noDatabaseOption );

        Option probesToDoOption = OptionBuilder
                .withDescription( "Comma-delimited list of probe names to process (for testing); implies -nodb" )
                .hasArg().withArgName( "probes" ).create( "probes" );

        addOption( probesToDoOption );

        //  using more than one thread won't work
        //   super.addThreadsOption();
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( args );

        if ( err != null )
            return err;

        if ( directAnnotationInputFileName == null )
            configure();

        final Date skipIfLastRunLaterThan = getLimitingDate();

        allowSubsumedOrMerged = true;

        if ( this.taxon != null && this.directAnnotationInputFileName == null && this.arrayDesignsToProcess
                .isEmpty() ) {
            log.warn( "*** Running mapping for all " + taxon.getCommonName()
                    + " Array designs, troubled platforms may be skipped *** " );
        }

        if ( !this.arrayDesignsToProcess.isEmpty() ) {
            for ( ArrayDesign arrayDesign : this.arrayDesignsToProcess ) {

                if ( this.probeNames != null ) {
                    processProbes( arrayDesign );
                    break;
                }

                if ( !needToRun( skipIfLastRunLaterThan, arrayDesign, ArrayDesignGeneMappingEvent.class ) ) {
                    log.warn( arrayDesign + " not ready to run" );
                    return null;
                }

                arrayDesign = unlazifyArrayDesign( arrayDesign );
                if ( directAnnotationInputFileName != null ) {
                    try {
                        File f = new File( this.directAnnotationInputFileName );
                        if ( !f.canRead() ) {
                            throw new IOException( "Cannot read from " + this.directAnnotationInputFileName );
                        }
                        arrayDesignProbeMapperService
                                .processArrayDesign( arrayDesign, taxon, f, this.sourceDatabase, this.ncbiIds );
                        audit( arrayDesign, "Imported from " + f, new AnnotationBasedGeneMappingEventImpl() );
                    } catch ( IOException e ) {
                        return e;
                    }
                } else {

                    if ( !this.useDB ) {
                        log.info( "**** Writing to STDOUT instead of the database ***" );
                        System.out.print( config );
                    }

                    arrayDesignProbeMapperService.processArrayDesign( arrayDesign, config, this.useDB );
                    if ( useDB ) {

                        if ( this.hasOption( MIRNA_ONLY_MODE_OPTION ) ) {
                            audit( arrayDesign, "Run in miRNA-only mode.", new AlignmentBasedGeneMappingEventImpl() );
                        } else if ( this.hasOption( CONFIG_OPTION ) ) {
                            audit( arrayDesign, "Run with configuration=" + this.getOptionValue( CONFIG_OPTION ),
                                    new AlignmentBasedGeneMappingEventImpl() );
                        } else {
                            audit( arrayDesign, "Run with default parameters",
                                    new AlignmentBasedGeneMappingEventImpl() );
                        }
                    }
                }
            }
        } else if ( taxon != null || skipIfLastRunLaterThan != null || autoSeek ) {

            if ( directAnnotationInputFileName != null ) {
                throw new IllegalStateException(
                        "Sorry, you can't provide an input mapping file when doing multiple arrays at once" );
            }

            batchRun( skipIfLastRunLaterThan );

        } else {
            return new IllegalArgumentException( "Seems you did not set options to get anything to happen." );
        }

        return null;
    }

    /**
     * Override to do additional checks to make sure the array design is in a state of readiness for probe mapping.
     */
    @Override
    protected boolean needToRun( Date skipIfLastRunLaterThan, ArrayDesign arrayDesign,
            Class<? extends ArrayDesignAnalysisEvent> eventClass ) {

        if ( this.hasOption( "force" ) ) {
            return true;
        }

        if ( this.directAnnotationInputFileName != null ) {
            return true;
        }

        arrayDesign = arrayDesignService.thawLite( arrayDesign );

        /*
         * Do not run this on "Generic" platforms or those which are loaded using a direct annotation input file!
         * (FIXME: this is a duplicate check)
         */
        if ( arrayDesign.getTechnologyType().equals( TechnologyType.NONE ) || !(
                arrayDesign.getTechnologyType().equals( TechnologyType.DUALMODE ) || arrayDesign.getTechnologyType()
                        .equals( TechnologyType.ONECOLOR ) || arrayDesign.getTechnologyType()
                        .equals( TechnologyType.TWOCOLOR ) ) ) {
            log.info( "Skipping because it is not a microarray platform" );
            return false;
        }

        if ( !super.needToRun( skipIfLastRunLaterThan, arrayDesign, eventClass ) ) {
            return false;
        }

        log.debug( "Re-Checking status of " + arrayDesign );

        List<AuditEvent> allEvents = this.auditTrailService.getEvents( arrayDesign );
        AuditEvent lastSequenceAnalysis = null;
        AuditEvent lastRepeatMask = null;
        AuditEvent lastSequenceUpdate = null;
        AuditEvent lastProbeMapping = null;

        log.debug( allEvents.size() + " to inspect" );
        for ( int j = allEvents.size() - 1; j >= 0; j-- ) {
            AuditEvent currentEvent = allEvents.get( j );
            if ( currentEvent == null )
                continue; // legacy of ordered-list which could end up with gaps; should not be
            // needed any more
            if ( currentEvent.getEventType() == null )
                continue;

            // we only care about ArrayDesignAnalysisEvent events.
            Class<? extends AuditEventType> currentEventClass = currentEvent.getEventType().getClass();

            log.debug( "Inspecting: " + currentEventClass );

            // Get the most recent event of each type.
            if ( lastRepeatMask == null && ArrayDesignRepeatAnalysisEvent.class
                    .isAssignableFrom( currentEventClass ) ) {
                lastRepeatMask = currentEvent;
                log.debug( "Last repeat mask: " + lastRepeatMask.getDate() );
            } else if ( lastSequenceUpdate == null && ArrayDesignSequenceUpdateEvent.class
                    .isAssignableFrom( currentEventClass ) ) {
                lastSequenceUpdate = currentEvent;
                log.debug( "Last sequence update: " + lastSequenceUpdate.getDate() );
            } else if ( lastSequenceAnalysis == null && ArrayDesignSequenceAnalysisEvent.class
                    .isAssignableFrom( currentEventClass ) ) {
                lastSequenceAnalysis = currentEvent;
                log.debug( "Last sequence analysis: " + lastSequenceAnalysis.getDate() );
            } else if ( lastProbeMapping == null && ArrayDesignGeneMappingEvent.class
                    .isAssignableFrom( currentEventClass ) ) {
                lastProbeMapping = currentEvent;
                log.info( "Last probe mapping analysis: " + lastProbeMapping.getDate() );
            }

        }

        /*
         * FIXME merged arraydesigns don't really need to get done, but it is confusing when looking at reports so we do
         * allow it. Fix would be to report when the mergees were run.
         * 
         * The issue addressed here is that we don't normally run sequence analysis etc. on merged platforms either, so
         * it looks like they are not ready for probemapping.
         */
        boolean isNotMerged = arrayDesign.getMergees().isEmpty();

        /*
         * make sure the last repeat mask and sequence analysis were done after the last sequence update. This is a
         * check that is not done by the default 'needToRun' implementation. Note that Repeatmasking can be done in any
         * order w.r.t. the sequence analysis, so we don't need to check that order.
         */
        if ( isNotMerged && lastSequenceUpdate != null ) {
            if ( lastRepeatMask != null && lastSequenceUpdate.getDate().after( lastRepeatMask.getDate() ) ) {
                log.warn( arrayDesign + ": Sequences were updated more recently than the last repeat masking" );
                return false;

            }

            if ( lastSequenceAnalysis != null && lastSequenceUpdate.getDate()
                    .after( lastSequenceAnalysis.getDate() ) ) {
                log.warn( arrayDesign + ": Sequences were updated more recently than the last sequence analysis" );
                return false;
            }
        }

        /*
         * This checks to make sure all the prerequsite steps have actually done. NOTE we don't check the sequence
         * update because this wasn't always filled in. Really we should.
         */

        if ( isNotMerged && lastSequenceAnalysis == null ) {
            log.warn( arrayDesign + ": Must do sequence analysis before probe mapping" );
            // We return false because we're not in a state to run it.
            return false;
        }

        if ( isNotMerged && lastRepeatMask == null ) {
            log.warn( arrayDesign + ": Must do repeat mask analysis before probe mapping" );
            return false;
        }

        if ( skipIfLastRunLaterThan != null && lastProbeMapping != null && lastProbeMapping.getDate()
                .after( skipIfLastRunLaterThan ) ) {
            log.info( arrayDesign + " was probemapped since " + skipIfLastRunLaterThan + ", skipping." );
            return false;
        }

        // we've validated the super.needToRun result, so we pass it on.
        return true;
    }

    /**
     * See 'configure' for how the other options are handled. (non-Javadoc)
     *
     * @see ArrayDesignSequenceManipulatingCli#processOptions()
     */
    @Override
    protected void processOptions() {
        super.processOptions();
        arrayDesignProbeMapperService = this.getBean( ArrayDesignProbeMapperService.class );
        this.taxonService = this.getBean( TaxonService.class );

        if ( hasOption( THREADS_OPTION ) ) {
            this.numThreads = this.getIntegerOptionValue( "threads" );
        }

        if ( this.hasOption( "import" ) ) {
            if ( !this.hasOption( 't' ) ) {
                throw new IllegalArgumentException( "You must provide the taxon when using the import option" );
            }
            if ( !this.hasOption( "source" ) ) {
                throw new IllegalArgumentException(
                        "You must provide source database name when using the import option" );
            }
            String sourceDBName = this.getOptionValue( "source" );

            ExternalDatabaseService eds = this.getBean( ExternalDatabaseService.class );

            this.sourceDatabase = eds.find( sourceDBName );

            this.directAnnotationInputFileName = this.getOptionValue( "import" );

            if ( this.hasOption( "ncbi" ) ) {
                this.ncbiIds = true;
            }
        }
        if ( this.hasOption( 't' ) ) {
            this.taxon = this.setTaxonByName( taxonService );
        }

        if ( this.hasOption( "nodb" ) ) {
            this.useDB = false;
        }

        if ( this.hasOption( "probes" ) ) {

            if ( this.arrayDesignsToProcess == null || this.arrayDesignsToProcess.size() > 1 ) {
                throw new IllegalArgumentException( "With '-probes' you must provide exactly one platform" );
            }

            this.useDB = false;
            probeNames = this.getOptionValue( "probes" ).split( "," );

            if ( probeNames.length == 0 ) {
                throw new IllegalArgumentException( "You must provide at least one probe name when using '-probes'" );
            }
        }

    }

    private void audit( ArrayDesign arrayDesign, String note, ArrayDesignGeneMappingEvent eventType ) {
        arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );
        auditTrailService.addUpdateEvent( arrayDesign, eventType, note );
    }

    private void batchRun( final Date skipIfLastRunLaterThan ) {
        Collection<ArrayDesign> allArrayDesigns;

        if ( this.taxon != null ) {
            allArrayDesigns = arrayDesignService.findByTaxon( this.taxon );
        } else {
            allArrayDesigns = arrayDesignService.loadAll();
        }

        final SecurityContext context = SecurityContextHolder.getContext();

        class ADProbeMapperCliConsumer extends Consumer {

            private ADProbeMapperCliConsumer( BlockingQueue<ArrayDesign> q ) {
                super( q, context );
            }

            void consume( ArrayDesign x ) {

                if ( x.getCurationDetails().getTroubled() ) {
                    log.warn( "Skipping troubled platform: " + x );
                    errorObjects.add( x + ": " + "Skipped because it is troubled; run in non-batch-mode" );
                    return;
                }

                /*
                 * Note that if the array design has multiple taxa, analysis will be run on all of the sequences, not
                 * just the ones from the taxon specified.
                 */
                processArrayDesign( skipIfLastRunLaterThan, x );

            }
        }

        BlockingQueue<ArrayDesign> arrayDesigns = new ArrayBlockingQueue<>( allArrayDesigns.size() );
        arrayDesigns.addAll( allArrayDesigns );

        Collection<Thread> threads = new ArrayList<>();
        for ( int i = 0; i < this.numThreads; i++ ) {
            ADProbeMapperCliConsumer c1 = new ADProbeMapperCliConsumer( arrayDesigns );
            Thread k = new Thread( c1 );
            threads.add( k );
            k.start();
        }

        waitForThreadPoolCompletion( threads );

        /*
         * All done
         */
        summarizeProcessing();
    }

    private void configure() {
        this.config = new ProbeMapperConfig();

        /*
         * Hackery to work around hg19 problems.; no longer an issue for miRNA
         */
        boolean isMissingTracks = taxon != null && taxon.getCommonName().equals( "human" ) && Settings
                .getString( "gemma.goldenpath.db.human" ).equals( "hg19" );

        if ( this.hasOption( MIRNA_ONLY_MODE_OPTION ) ) {
            // if ( isMissingTracks ) {
            // throw new IllegalArgumentException( "There is no miRNA track for this taxon." );
            // }
            log.info( "Micro RNA only mode" );
            config.setAllTracksOff();
            config.setUseMiRNA( true );
        } else if ( this.hasOption( CONFIG_OPTION ) ) {

            String configString = this.getOptionValue( CONFIG_OPTION );

            if ( !configString.matches(
                    "[" + OPTION_REFSEQ + OPTION_KNOWNGENE + OPTION_MICRORNA + OPTION_EST + OPTION_MRNA + OPTION_ASEMBLY
                            + OPTION_ENSEMBL + OPTION_NSCAN + "]+" ) ) {
                throw new IllegalArgumentException(
                        "Configuration string must only contain values [" + OPTION_REFSEQ + OPTION_KNOWNGENE
                                + OPTION_MICRORNA + OPTION_EST + OPTION_MRNA + OPTION_ASEMBLY + OPTION_ENSEMBL
                                + OPTION_NSCAN + "]" );
            }

            config.setAllTracksOff();

            config.setUseEsts( configString.contains( OPTION_EST ) );
            config.setUseMrnas( configString.contains( OPTION_MRNA ) );
            config.setUseMiRNA( configString.contains( OPTION_MICRORNA ) );
            config.setUseEnsembl( configString.contains( OPTION_ENSEMBL ) );
            config.setUseNscan( configString.contains( OPTION_NSCAN ) );
            config.setUseRefGene( configString.contains( OPTION_REFSEQ ) );
            config.setUseKnownGene( configString.contains( OPTION_KNOWNGENE ) );
            config.setUseAcembly( configString.contains( OPTION_ASEMBLY ) );
        }

        if ( hasOption( 's' ) ) {
            double blatscorethresh = getDoubleOptionValue( 's' );
            if ( blatscorethresh < 0 || blatscorethresh > 1 ) {
                throw new IllegalArgumentException( "BLAT score threshold must be between 0 and 1" );
            }
            config.setBlatScoreThreshold( blatscorethresh );
        }

        if ( hasOption( "usePars" ) ) {
            config.setAllowMakeProbeAlignedRegion( true );
            config.setAllowProbeAlignedRegions( true );
        }

        if ( hasOption( "usePred" ) ) {
            config.setAllowPredictedGenes( true );
        }

        if ( hasOption( 'i' ) ) {
            double option = getDoubleOptionValue( 'i' );
            if ( option < 0 || option > 1 ) {
                throw new IllegalArgumentException( "Identity threshold must be between 0 and 1" );
            }
            config.setIdentityThreshold( option );
        }

        if ( hasOption( 'o' ) ) {
            double option = getDoubleOptionValue( 'o' );
            if ( option < 0 || option > 1 ) {
                throw new IllegalArgumentException( "Overlap threshold must be between 0 and 1" );
            }
            config.setMinimumExonOverlapFraction( option );
        }

        // if ( isMissingTracks && config.isUseMiRNA() ) {
        // log.warn( "At last check hg19 did not have miRNA tracks, turning option off" );
        // config.setUseMiRNA( false );
        // }
        if ( isMissingTracks && config.isUseAcembly() ) {
            log.warn( "At last check hg19 did not have acembly tracks, turning option off" );
            config.setUseAcembly( false );
        }

        log.info( config );

    }

    private void processArrayDesign( Date skipIfLastRunLaterThan, ArrayDesign design ) {
        if ( taxon != null && !arrayDesignService.getTaxa( design.getId() ).contains( taxon ) ) {
            return;
        }

        if ( !allowSubsumedOrMerged && isSubsumedOrMerged( design ) ) {
            log.warn( design + " is subsumed or merged into another design, it will not be run." );
            // not really an error, but nice to get notification.
            errorObjects.add( design + ": " + "Skipped because it is subsumed by or merged into another design." );
            return;
        }

        if ( design.getTechnologyType().equals( TechnologyType.NONE ) ) {
            log.warn( design + " is not a microarray platform, it will not be run" );
            // not really an error, but nice to get notification.
            errorObjects.add( design + ": " + "Skipped because it is not a microarray platform." );
            return;
        }

        if ( !needToRun( skipIfLastRunLaterThan, design, ArrayDesignGeneMappingEvent.class ) ) {
            if ( skipIfLastRunLaterThan != null ) {
                log.warn( design + " was last run more recently than " + skipIfLastRunLaterThan );
                errorObjects.add( design + ": " + "Skipped because it was last run after " + skipIfLastRunLaterThan );
            } else {
                log.warn( design + " seems to be up to date or is not ready to run" );
                errorObjects.add( design + " seems to be up to date or is not ready to run" );
            }
            return;
        }

        log.info( "============== Start processing: " + design + " ==================" );
        try {
            design = arrayDesignService.thaw( design );

            arrayDesignProbeMapperService.processArrayDesign( design, this.config, this.useDB );
            successObjects.add( design.getName() );
            ArrayDesignGeneMappingEvent eventType = new AlignmentBasedGeneMappingEventImpl();
            audit( design, "Part of a batch job", eventType );

        } catch ( Exception e ) {
            errorObjects.add( design + ": " + e.getMessage() );
            log.error( "**** Exception while processing " + design + ": " + e.getMessage() + " ****" );
            log.error( e, e );
        }
    }

    private void processProbes( ArrayDesign arrayDesign ) {
        assert this.probeNames != null && this.probeNames.length > 0;
        arrayDesign = arrayDesignService.thawLite( arrayDesign );
        CompositeSequenceService compositeSequenceService = this.getBean( CompositeSequenceService.class );

        for ( String probeName : this.probeNames ) {
            CompositeSequence probe = compositeSequenceService.findByName( arrayDesign, probeName );

            if ( probe == null ) {
                log.warn( "No such probe: " + probeName + " on " + arrayDesign.getShortName() );
                continue;
            }

            probe = compositeSequenceService.thaw( probe );

            Map<String, Collection<BlatAssociation>> results = this.arrayDesignProbeMapperService
                    .processCompositeSequence( this.config, taxon, null, probe );

            for ( Collection<BlatAssociation> col : results.values() ) {
                for ( BlatAssociation association : col ) {
                    if ( log.isDebugEnabled() )
                        log.debug( association );
                }

                arrayDesignProbeMapperService.printResult( probe, col );

            }
        }
    }

}
