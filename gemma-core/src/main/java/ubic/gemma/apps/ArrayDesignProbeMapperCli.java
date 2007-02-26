package ubic.gemma.apps;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeMapperService;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignGeneMappingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;

/**
 * Process the blat results for an array design to map them onto genes.
 * <p>
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
 * TODO : allow tuning of paramters from the command line.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignProbeMapperCli extends ArrayDesignSequenceManipulatingCli {
    ArrayDesignProbeMapperService arrayDesignProbeMapperService;
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

        Option taxonOption = OptionBuilder
                .hasArg()
                .withArgName( "taxon" )
                .withDescription(
                        "Taxon common name (e.g., human); analysis will be run for all ArrayDesigns from that taxon (overrides -a)" )
                .create( 't' );

        addOption( taxonOption );

    }

    public static void main( String[] args ) {
        ArrayDesignProbeMapperCli p = new ArrayDesignProbeMapperCli();
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
        Exception err = processCommandLine( "Array design mapping of probes to genes", args );
        if ( err != null ) return err;

        if ( this.taxon != null ) {
            log.warn( "*** Running mapping for all " + taxon.getCommonName() + " Array designs *** " );
            Collection<String> errorObjects = new HashSet<String>();
            Collection<String> persistedObjects = new HashSet<String>();

            Collection<ArrayDesign> allArrayDesigns = arrayDesignService.loadAll();
            for ( ArrayDesign design : allArrayDesigns ) {
                if ( taxon.equals( arrayDesignService.getTaxon( design.getId() ) ) ) {
                    log.info( "============== Start processing: " + design + " ==================" );
                    try {
                        arrayDesignProbeMapperService.processArrayDesign( design );
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
            ArrayDesign arrayDesign = locateArrayDesign( arrayDesignName );
            unlazifyArrayDesign( arrayDesign );
            arrayDesignProbeMapperService.processArrayDesign( arrayDesign );
            audit( arrayDesign, "Run with default parameters" );
        }

        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        arrayDesignProbeMapperService = ( ArrayDesignProbeMapperService ) this
                .getBean( "arrayDesignProbeMapperService" );

        this.taxonService = ( TaxonService ) this.getBean( "taxonService" );

        if ( this.hasOption( 't' ) ) {
            this.taxonName = this.getOptionValue( 't' );
            this.taxon = taxonService.findByCommonName( this.taxonName );
            if ( taxon == null ) {
                throw new IllegalArgumentException( "No taxon named " + taxonName );
            }
        }
    }

    /**
     * @param arrayDesign
     */
    private void audit( ArrayDesign arrayDesign, String note ) {
        AuditEventType eventType = ArrayDesignGeneMappingEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( arrayDesign, eventType, note );
    }

}
