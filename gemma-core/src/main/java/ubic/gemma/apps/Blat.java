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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.sequence.SequenceManipulation;
import ubic.gemma.loader.genome.BlatResultParser;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.concurrent.GenericStreamConsumer;

/**
 * Class to manage the gfServer and run BLAT searches.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class Blat {

    private static final int BLAT_UPDATE_INTERVAL_MS = 1000 * 30;

    /**
     * Spaces in the sequence name will cause problems when converting back from the PSL format, so they are replaced.
     */
    public static final String SPACE_REPLACEMENT = "_____";

    private static final Log log = LogFactory.getLog( Blat.class );

    /**
     * This value is basically a threshold fraction of aligned bases in the query
     * 
     * @see BlatResult implementation for how the score is computed.
     */
    public static final double DEFAULT_BLAT_SCORE_THRESHOLD = 0.7;

    public static final double STEPSIZE = 7;

    private double blatScoreThreshold = DEFAULT_BLAT_SCORE_THRESHOLD;

    private static String os = System.getProperty( "os.name" ).toLowerCase();

    public static enum BlattableGenome {
        HUMAN, MOUSE, RAT
    };

    static {
        if ( !os.toLowerCase().startsWith( "windows" ) ) {
            try {
                log.debug( "Loading gfClient library, looking in " + System.getProperty( "java.library.path" ) );
                System.loadLibrary( "Blat" );
                log.info( "Loaded Blat native library successfully" );
            } catch ( UnsatisfiedLinkError e ) {
                log.error( e, e );
                throw new ExceptionInInitializerError( "Unable to locate or load the Blat native library: "
                        + e.getMessage() );
            }
        }
    }
    private boolean doShutdown = true;

    // typical values.
    private String gfClientExe = "/cygdrive/c/cygwin/usr/local/bin/gfClient.exe";
    private String gfServerExe = "/cygdrive/c/cygwin/usr/local/bin/gfServer.exe";
    private String host = "localhost";
    private String seqDir = "/";

    private String humanSeqFiles;
    private String ratSeqFiles;
    private String mouseSeqFiles;

    private Process serverProcess;
    private int humanServerPort;
    private int mouseServerPort;
    private int ratServerPort;

    private String humanServerHost;
    private String mouseServerHost;
    private String ratServerHost;

    /**
     * Create a blat object with settings read from the config file.
     */
    public Blat() {
        try {
            init();
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( "Could not load configuration", e );
        }
    }

    /**
     * @param host
     * @param port
     * @param seqDir
     */
    public Blat( String host, int humanServerPort, String seqDir ) {

        if ( host == null || humanServerPort <= 0 || seqDir == null )
            throw new IllegalArgumentException( "All values must be non-null" );
        this.host = host;
        this.humanServerPort = humanServerPort;
        this.seqDir = seqDir;
    }

    /**
     * @return Returns the gfClientExe.
     */
    public String getGfClientExe() {
        return this.gfClientExe;
    }

    /**
     * @return Returns the gfServerExe.
     */
    public String getGfServerExe() {
        return this.gfServerExe;
    }

    /**
     * @return Returns the host.
     */
    public String getHost() {
        return this.host;
    }

    /**
     * @return Returns the seqDir.
     */
    public String getSeqDir() {
        return this.seqDir;
    }

    /**
     * @return Returns the seqFiles.
     */
    public String getSeqFiles( BlattableGenome genome ) {
        switch ( genome ) {
            case HUMAN:
                return this.humanSeqFiles;
            case MOUSE:
                return this.mouseSeqFiles;
            case RAT:
                return this.ratSeqFiles;
            default:
                return this.humanSeqFiles;

        }
    }

    /**
     * Strings of As or Ts at the start or end of a sequence longer than this will be stripped off prior to analysis.
     */
    private static final int POLY_AT_THRESHOLD = 5;

    /**
     * Run a BLAT search using the gfClient.
     * 
     * @param b
     * @param genome
     * @return Collection of BlatResult objects.
     * @throws IOException
     */
    public Collection<BlatResult> blatQuery( BioSequence b, Taxon taxon ) throws IOException {
        assert seqDir != null;
        // write the sequence to a temporary file.
        File querySequenceFile = File.createTempFile( b.getName(), ".fa" );

        BufferedWriter out = new BufferedWriter( new FileWriter( querySequenceFile ) );
        String trimmed = SequenceManipulation.stripPolyAorT( b.getSequence(), POLY_AT_THRESHOLD );
        out.write( ">" + b.getName() + "\n" + trimmed );
        out.close();
        log.info( "Wrote sequence to " + querySequenceFile.getPath() );

        String outputPath = getTmpPslFilePath( b.getName() );

        Collection<BlatResult> results = gfClient( querySequenceFile, outputPath, choosePortForQuery( taxon ) );

        ExternalDatabase searchedDatabase = getSearchedGenome( taxon );
        for ( BlatResult result : results ) {
            result.setSearchedDatabase( searchedDatabase );
        }

        cleanUpTmpFiles( querySequenceFile, outputPath );
        return results;

    }

    /**
     * Run a BLAT search using the gfClient.
     * 
     * @param b. The genome is inferred from the Taxon held by the sequence.
     * @return Collection of BlatResult objects.
     * @throws IOException
     */
    public Collection<BlatResult> blatQuery( BioSequence b ) throws IOException {
        Taxon t = b.getTaxon();
        if ( t == null ) {
            throw new IllegalArgumentException( "Cannot blat sequence unless taxon is given or inferrable" );
        }

        return blatQuery( b, t );
    }

    /**
     * @param genome
     * @return
     */
    private int choosePortForQuery( Taxon taxon ) {
        BlattableGenome genome = inferBlatDatabase( taxon );
        switch ( genome ) {
            case HUMAN:
                return humanServerPort;
            case MOUSE:
                return mouseServerPort;
            case RAT:
                return ratServerPort;
            default:
                return humanServerPort;

        }
    }

    /**
     * @param querySequenceFile
     * @param outputPath
     */
    private void cleanUpTmpFiles( File querySequenceFile, String outputPath ) {
        if ( !querySequenceFile.delete() || !( new File( outputPath ) ).delete() ) {
            log.warn( "Could not clean up temporary files." );
        }
    }

    /**
     * @param sequences
     * @param taxon The taxon whose database will be searched.
     * @return map of the input sequences to a corresponding collection of blat result(s)
     * @throws IOException
     */
    public Map<BioSequence, Collection<BlatResult>> blatQuery( Collection<BioSequence> sequences, Taxon taxon )
            throws IOException {
        Map<BioSequence, Collection<BlatResult>> results = new HashMap<BioSequence, Collection<BlatResult>>();

        File querySequenceFile = File.createTempFile( "pattern", ".fa" );
        BufferedWriter out = new BufferedWriter( new FileWriter( querySequenceFile ) );

        /*
         * Note this silliness. Often the sequences have been read in from a file in the first place. The problem is
         * there are no easy hooks to gfClient that don't use a file. This could be changed at a later time. It would
         * require customizing Kent's code (even more than we do).
         */

        log.debug( "Processing " + sequences.size() + " sequences for blat analysis" );
        int count = 0;
        Collection<Object> identifiers = new HashSet<Object>();
        int repeats = 0;
        for ( BioSequence b : sequences ) {
            if ( StringUtils.isNotBlank( b.getSequence() ) ) {
                String identifier = b.getName();
                identifier = identifier.replaceAll( " ", SPACE_REPLACEMENT );
                if ( identifiers.contains( identifier ) ) {
                    repeats++;
                    continue; // don't repeat sequences.
                }
                out.write( ">" + identifier + "\n" + b.getSequence() + "\n" );
                identifiers.add( identifier );
            } else {
                log.warn( "Blank sequence for " + b );
            }
            if ( ++count % 5000 == 0 ) {
                log.debug( "Wrote " + count + " sequences" );
            }
        }
        out.close();

        if ( count == 0 ) {
            querySequenceFile.delete();
            throw new IllegalArgumentException( "No sequences!" );
        }

        log.info( "Wrote " + count + " sequences( " + repeats + " repeated items were skipped)." );

        String outputPath = getTmpPslFilePath( "process" );

        Collection<BlatResult> rawresults = gfClient( querySequenceFile, outputPath, choosePortForQuery( taxon ) );

        log.info( "Got " + rawresults.size() + " raw blat results" );

        ExternalDatabase searchedDatabase = getSearchedGenome( taxon );

        for ( BlatResult blatResult : rawresults ) {
            blatResult.setSearchedDatabase( searchedDatabase );

            BioSequence query = blatResult.getQuerySequence();

            if ( !results.containsKey( query ) ) {
                results.put( query, new HashSet<BlatResult>() );
            }

            results.get( query ).add( blatResult );
        }
        querySequenceFile.delete();
        return results;
    }

    /**
     * @param taxon
     * @return
     */
    public static ExternalDatabase getSearchedGenome( Taxon taxon ) {
        BlattableGenome genome = inferBlatDatabase( taxon );
        ExternalDatabase searchedDatabase = ExternalDatabase.Factory.newInstance();
        searchedDatabase.setType( DatabaseType.SEQUENCE );
        searchedDatabase.setName( genome.toString().toLowerCase() );
        return searchedDatabase;
    }

    /**
     * @param taxon
     * @return
     */
    private static BlattableGenome inferBlatDatabase( Taxon taxon ) {
        BlattableGenome bg = BlattableGenome.MOUSE;

        if ( taxon.getNcbiId() == 10090 || taxon.getCommonName().equals( "mouse" ) ) {
            bg = BlattableGenome.MOUSE;
        } else if ( taxon.getNcbiId() == 10116 || taxon.getCommonName().equals( "rat" ) ) {
            bg = BlattableGenome.RAT;
        } else if ( taxon.getNcbiId() == 9606 || taxon.getCommonName().equals( "human" ) ) {
            bg = BlattableGenome.HUMAN;
        } else {
            throw new UnsupportedOperationException( "Cannot determine which database to search for " + taxon );
        }
        return bg;
    }

    /**
     * Start the server, if the port isn't already being used. If the port is in use, we assume it is a gfServer.
     */
    public void startServer( BlattableGenome genome, int port ) throws IOException {
        try {
            new Socket( host, port );
            log.info( "There is already a server on port " + port );
            this.doShutdown = false;
        } catch ( UnknownHostException e ) {
            throw new RuntimeException( "Unknown host " + host, e );
        } catch ( IOException e ) {
            String cmd = this.getGfServerExe() + " -canStop -stepSize=" + STEPSIZE + " start " + this.getHost() + " "
                    + port + " " + this.getSeqFiles( genome );
            log.info( "Starting gfServer with command " + cmd );
            this.serverProcess = Runtime.getRuntime().exec( cmd, null, new File( this.getSeqDir() ) );

            try {
                Thread.sleep( 100 );
                int exit = serverProcess.exitValue();
                if ( exit != 0 ) {
                    throw new IOException( "Could not start server" );
                }
            } catch ( IllegalThreadStateException e1 ) {
                log.info( "Server seems to have started" );
            } catch ( InterruptedException e1 ) {
                ;
            }

        }
    }

    /**
     * Stop the gfServer, if it was started by this.
     */
    public void stopServer( int port ) {
        if ( false && !doShutdown ) {
            return;
        }
        log.info( "Shutting down gfServer" );

        if ( serverProcess == null ) return;
        // serverProcess.destroy();
        try {
            // this doesn't work unless the server was invoked with the option "-canStop"
            Process server = Runtime.getRuntime().exec( this.getGfServerExe() + " stop " + this.getHost() + " " + port );
            server.waitFor();
            int exit = server.exitValue();
            log.info( "Server on port " + port + " shut down with exit value " + exit );
        } catch ( InterruptedException e ) {
            log.error( e, e );
        } catch ( IOException e ) {
            log.error( e, e );
        }

    }

    /**
     * Run a gfClient query, using a call to exec().
     * 
     * @param querySequenceFile
     * @param outputPath
     * @return
     */
    private Collection<BlatResult> execGfClient( File querySequenceFile, String outputPath, int portToUse )
            throws IOException {
        final String cmd = gfClientExe + " -nohead -minScore=16 " + host + " " + portToUse + " " + seqDir + " "
                + querySequenceFile.getAbsolutePath() + " " + outputPath;
        log.info( cmd );

        final Process run = Runtime.getRuntime().exec( cmd );

        // to ensure that we aren't left waiting for these streams
        GenericStreamConsumer gscErr = new GenericStreamConsumer( run.getErrorStream() );
        GenericStreamConsumer gscIn = new GenericStreamConsumer( run.getInputStream() );
        gscErr.start();
        gscIn.start();

        try {

            int exitVal = Integer.MIN_VALUE;

            // wait...
            StopWatch overallWatch = new StopWatch();
            overallWatch.start();

            while ( exitVal == Integer.MIN_VALUE ) {
                try {
                    exitVal = run.exitValue();
                } catch ( IllegalThreadStateException e ) {
                    // okay, still waiting.
                }
                Thread.sleep( BLAT_UPDATE_INTERVAL_MS );
                // I hope this is okay...
                synchronized ( outputPath ) {
                    File outputFile = new File( outputPath );
                    Long size = outputFile.length();
                    NumberFormat nf = new DecimalFormat();
                    nf.setMaximumFractionDigits( 2 );
                    String minutes = getMinutesElapsed( overallWatch );
                    log.info( "BLAT output so far: " + nf.format( size / 1024.0 ) + " kb (" + minutes
                            + " minutes elapsed)" );
                }
            }

            overallWatch.stop();
            String minutes = getMinutesElapsed( overallWatch );
            log.info( "Blat took a total of " + minutes + " minutes" );

            // int exitVal = run.waitFor();

            log.debug( "blat exit value=" + exitVal );
        } catch ( InterruptedException e ) {
            throw new RuntimeException( e );
        }
        log.debug( "GfClient Success" );

        return processPsl( outputPath, null );
    }

    // /**
    // * @param future
    // * @return
    // * @throws InterruptedException
    // * @throws ExecutionException
    // * @throws IOException
    // */
    // private StringBuilder getErrOutput( FutureTask<Process> future ) throws InterruptedException, ExecutionException,
    // IOException {
    // InputStream result = future.get().getErrorStream();
    // BufferedReader br = new BufferedReader( new InputStreamReader( result ) );
    // String l = null;
    // StringBuilder buf = new StringBuilder();
    // while ( ( l = br.readLine() ) != null ) {
    // buf.append( l + "\n" );
    // }
    // br.close();
    // return buf;
    // }

    /**
     * Get a temporary file name.
     * 
     * @throws IOException
     */
    private String getTmpPslFilePath( String base ) throws IOException {
        File tmpdir = new File( ConfigUtils.getDownloadPath() );
        if ( StringUtils.isBlank( base ) ) {
            return File.createTempFile( "pattern", ".psl", tmpdir ).getPath();
        } else {
            return File.createTempFile( base, ".psl", tmpdir ).getPath();
        }
    }

    /**
     * @param querySequenceFile
     * @param outputPath
     * @return processed results.
     * @throws IOException
     */
    private Collection<BlatResult> gfClient( File querySequenceFile, String outputPath, int portToUse )
            throws IOException {
        if ( !os.startsWith( "windows" ) ) return jniGfClientCall( querySequenceFile, outputPath, portToUse );

        return execGfClient( querySequenceFile, outputPath, portToUse );
    }

    /**
     * @param host
     * @param port
     * @param seqDir
     * @param inputFile
     * @param outputFile
     */
    private native void GfClientCall( String h, String p, String dir, String input, String output );

    /**
     * @throws ConfigurationException
     */
    private void init() throws ConfigurationException {

        log.debug( "Reading global config" );
        this.humanServerPort = ConfigUtils.getInt( "gfClient.humanServerPort" );
        this.mouseServerPort = ConfigUtils.getInt( "gfClient.mouseServerPort" );
        this.ratServerPort = ConfigUtils.getInt( "gfClient.ratServerPort" );
        this.humanServerHost = ConfigUtils.getString( "gfClient.humanServerHost" );
        this.mouseServerHost = ConfigUtils.getString( "gfClient.mouseServerHost" );
        this.ratServerHost = ConfigUtils.getString( "gfClient.ratServerHost" );
        this.host = ConfigUtils.getString( "gfClient.host" );
        this.seqDir = ConfigUtils.getString( "gfClient.seqDir" );
        this.mouseSeqFiles = ConfigUtils.getString( "gfClient.mouse.seqFiles" );
        this.ratSeqFiles = ConfigUtils.getString( "gfClient.rat.seqFiles" );
        this.humanSeqFiles = ConfigUtils.getString( "gfClient.human.seqFiles" );
        this.gfClientExe = ConfigUtils.getString( "gfClient.exe" );
        this.gfServerExe = ConfigUtils.getString( "gfServer.exe" );

        if ( gfServerExe == null ) {
            log.warn( "You will not be able to start the server due to a configuration error." );
        }

        if ( gfClientExe == null && os.startsWith( "windows" ) ) {
            throw new ConfigurationException( "BLAT client calls will not work under windows." );
        }

    }

    /**
     * @param querySequenceFile
     * @param outputPath
     * @return processed results.
     */
    private Collection<BlatResult> jniGfClientCall( final File querySequenceFile, final String outputPath,
            final int portToUse ) throws IOException {
        try {
            log.debug( "Starting blat run" );

            FutureTask<Boolean> blatThread = new FutureTask<Boolean>( new Callable<Boolean>() {
                public Boolean call() throws FileNotFoundException, IOException {
                    GfClientCall( host, Integer.toString( portToUse ), seqDir, querySequenceFile.getPath(), outputPath );
                    return true;
                }
            } );

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute( blatThread );
            executor.shutdown();

            // wait...
            StopWatch overallWatch = new StopWatch();
            overallWatch.start();

            while ( !blatThread.isDone() ) {
                try {
                    Thread.sleep( BLAT_UPDATE_INTERVAL_MS );
                } catch ( InterruptedException ie ) {
                    throw new RuntimeException( ie );
                }

                synchronized ( outputPath ) {
                    File outputFile = new File( outputPath );
                    Long size = outputFile.length();
                    NumberFormat nf = new DecimalFormat();
                    nf.setMaximumFractionDigits( 2 );
                    String minutes = getMinutesElapsed( overallWatch );
                    log.info( "BLAT output so far: " + nf.format( size / 1024.0 ) + " kb (" + minutes
                            + " minutes elapsed)" );
                }

            }

            overallWatch.stop();
            String minutes = getMinutesElapsed( overallWatch );
            log.info( "Blat took a total of " + minutes + " minutes" );

        } catch ( UnsatisfiedLinkError e ) {
            log.error( e, e );
            log.info( "Falling back on exec()" );
            this.execGfClient( querySequenceFile, outputPath, portToUse );
        }
        return this.processPsl( outputPath, null );
    }

    private String getMinutesElapsed( StopWatch overallWatch ) {
        Long overallElapsed = overallWatch.getTime();
        NumberFormat nf = new DecimalFormat();
        nf.setMaximumFractionDigits( 2 );
        String minutes = nf.format( overallElapsed / ( 60.0 * 1000.0 ) );
        return minutes;
    }

    /**
     * @param filePath to the Blat output file in psl format
     * @return processed results.
     */
    private Collection<BlatResult> processPsl( String filePath, Taxon taxon ) throws IOException {
        log.debug( "Processing " + filePath );
        BlatResultParser brp = new BlatResultParser();
        brp.setTaxon( taxon );
        brp.setScoreThreshold( this.blatScoreThreshold );
        brp.parse( filePath );
        return brp.getResults();
    }

    /**
     * @param inputStream to the Blat output file in psl format
     * @return processed results.
     */
    public Collection<BlatResult> processPsl( InputStream inputStream, Taxon taxon ) throws IOException {
        log.debug( "Processing " + inputStream );
        BlatResultParser brp = new BlatResultParser();
        brp.setTaxon( taxon );
        brp.setScoreThreshold( this.blatScoreThreshold );
        brp.parse( inputStream );
        return brp.getResults();
    }

    /**
     * @return Returns the humanServerPort.
     */
    public int getHumanServerPort() {
        return this.humanServerPort;
    }

    /**
     * @return Returns the mouseServerPort.
     */
    public int getMouseServerPort() {
        return this.mouseServerPort;
    }

    /**
     * @return Returns the ratServerPort.
     */
    public int getRatServerPort() {
        return this.ratServerPort;
    }

    /**
     * @return the blatScoreThreshold
     */
    public double getBlatScoreThreshold() {
        return this.blatScoreThreshold;
    }

    /**
     * @param blatScoreThreshold the blatScoreThreshold to set
     */
    public void setBlatScoreThreshold( double blatScoreThreshold ) {
        this.blatScoreThreshold = blatScoreThreshold;
    }

}
