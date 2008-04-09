/*
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

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;
import org.compass.gps.spi.CompassGpsInterfaceDevice;

import ubic.gemma.util.AbstractSpringAwareCLI;
import ubic.gemma.util.CompassUtils;

/**
 * Simple command line to index the gemma db. Can index gene's, Expression experiments or array Designs
 * 
 * @author klc
 * @version $Id$
 */
public class IndexGemmaCLI extends AbstractSpringAwareCLI {

    private boolean indexEE = false;
    private boolean indexAD = false;
    private boolean indexG = false;
    private boolean indexB = false;
    private boolean indexP = false;
    private boolean indexQ = false;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option geneOption = OptionBuilder.withDescription( "Use this option for indexing Genes" ).withLongOpt( "genes" )
                .create( 'g' );
        addOption( geneOption );

        Option eeOption = OptionBuilder.withDescription( "Use this option for indexing Expression Experiments" )
                .withLongOpt( "ExpressionExperiments" ).create( 'e' );
        addOption( eeOption );

        Option adOption = OptionBuilder.withDescription( "Use this option for indexing Array Designs" ).withLongOpt(
                "ArrayDesigns" ).create( 'a' );
        addOption( adOption );


        Option bibliographicOption = OptionBuilder.withDescription(
                "Use this option for indexing Bibliographic References" ).withLongOpt( "Bibliographic" ).create( 'b' );
        addOption( bibliographicOption );

        Option probeOption = OptionBuilder.withDescription( "Use this option for indexing probes" ).withLongOpt(
                "probes" ).create( 's' );
        addOption( probeOption );

        Option sequenceOption = OptionBuilder.withDescription( "Use this option for indexing sequences" ).withLongOpt(
                "sequences" ).create( 'q' );
        addOption( sequenceOption );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractSpringAwareCLI#processOptions()
     */
    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'e' ) ) indexEE = true;

        if ( hasOption( 'a' ) ) indexAD = true;

        if ( hasOption( 'g' ) ) indexG = true;

        if ( hasOption( 'b' ) ) indexB = true;

        if ( hasOption( 's' ) ) indexP = true;

        if ( hasOption( 'q' ) ) indexQ = true;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        IndexGemmaCLI p = new IndexGemmaCLI();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( "Total indexing time: " + watch.getTime() );
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
        Exception err = processCommandLine( "Index Gemma", args );
        if ( err != null ) {
            return err;
        }
        try {
            if ( this.indexG ) {
                rebuildIndex( ( CompassGpsInterfaceDevice ) this.getBean( "geneGps" ), "Gene index" );
            }
            if ( this.indexEE ) {
                rebuildIndex( ( CompassGpsInterfaceDevice ) this.getBean( "expressionGps" ),
                        "Expression Experiment index" );
            }
            if ( this.indexAD ) {
                rebuildIndex( ( CompassGpsInterfaceDevice ) this.getBean( "arrayGps" ), "Array Design index" );
            }
            if ( this.indexB ) {
                rebuildIndex( ( CompassGpsInterfaceDevice ) this.getBean( "bibliographicGps" ),
                        "Bibliographic Reference Index" );
            }
            if ( this.indexP ) {
                rebuildIndex( ( CompassGpsInterfaceDevice ) this.getBean( "probeGps" ), "Probe Reference Index" );
            }
            if ( this.indexQ ) {
                rebuildIndex( ( CompassGpsInterfaceDevice ) this.getBean( "biosequenceGps" ), "BioSequence Index" );
            }
        } catch ( Exception e ) {
            log.error( e );
            return e;
        }
        return null;
    }

    /**
     * @param device
     * @param whatIndexingMsg
     * @throws Exception
     */
    protected void rebuildIndex( CompassGpsInterfaceDevice device, String whatIndexingMsg ) throws Exception {

        long time = System.currentTimeMillis();

        log.info( "Rebuilding " + whatIndexingMsg );
        // /device.index();
        CompassUtils.rebuildCompassIndex( device );

        time = System.currentTimeMillis() - time;

        log.info( "Finished rebuilding " + whatIndexingMsg + ".  Took (ms): " + time );
        log.info( " \n " );

    }

    @Override
    public String getShortDesc() {
        return "Create or update the searchable indexes for a Gemma production system";
    }
}
