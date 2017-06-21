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
package ubic.gemma.core.apps;

import java.io.BufferedWriter;
import java.io.File;
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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.core.analysis.sequence.SequenceManipulation;
import ubic.gemma.core.analysis.sequence.SequenceWriter;
import ubic.gemma.core.loader.genome.BlatResultParser;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.util.Settings;
import ubic.gemma.core.util.TimeUtil;
import ubic.gemma.core.util.concurrent.GenericStreamConsumer;

/**
 * Class to manage the gfServer and run BLAT searches. Delegates to the command-line shell to run blat.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ShellDelegatingBlat implements Blat {

    public static enum BlattableGenome {
        HUMAN, MOUSE, RAT
    }

    private static final int BLAT_UPDATE_INTERVAL_MS = 1000 * 30;

    private static final Log log = LogFactory.getLog( ShellDelegatingBlat.class );

    /**
     * Minimum alignment length for retention.
     */
    private static final int MIN_SCORE = 16;

    private static String os = System.getProperty( "os.name" ).toLowerCase();

    /**
     * Strings of As or Ts at the start or end of a sequence longer than this will be stripped off prior to analysis.
     */
    private static final int POLY_AT_THRESHOLD = 5;

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
        assert taxon != null;

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

    private double blatScoreThreshold = DEFAULT_BLAT_SCORE_THRESHOLD;
    private boolean doShutdown = true;

    // typical values.
    private String gfClientExe = "/cygdrive/c/cygwin/usr/local/bin/gfClient.exe";
    private String gfServerExe = "/cygdrive/c/cygwin/usr/local/bin/gfServer.exe";
    private String host = "localhost";
    private int humanSensitiveServerPort;

    private String humanSeqFiles;
    private int humanServerPort;
    private int mouseSensitiveServerPort;

    private String mouseSeqFiles;

    private int mouseServerPort;

    private int ratSensitiveServerPort;

    private String ratSeqFiles;

    private int ratServerPort;

    private String seqDir = "/";

    // private String humanServerHost;
    //
    // private String mouseServerHost;
    //
    // private String ratServerHost;

    private Process serverProcess;

    /**
     * Create a blat object with settings read from the config file.
     */
    public ShellDelegatingBlat() {
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
    public ShellDelegatingBlat( String host, int humanServerPort, String seqDir ) {

        if ( host == null || humanServerPort <= 0 || seqDir == null )
            throw new IllegalArgumentException( "All values must be non-null" );
        this.host = host;
        this.humanServerPort = humanServerPort;
        this.seqDir = seqDir;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Blat#blatQuery(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    public Collection<BlatResult> blatQuery( BioSequence b ) throws IOException {
        Taxon t = b.getTaxon();
        if ( t == null ) {
            throw new IllegalArgumentException( "Cannot blat sequence unless taxon is given or inferrable" );
        }

        return blatQuery( b, t, false );
    }

    /*
     * (non-Javadoc)
     * 
     * @see Blat#blatQuery(ubic.gemma.model.genome.biosequence.BioSequence,
     * ubic.gemma.model.genome.Taxon, boolean)
     */
    @Override
    public Collection<BlatResult> blatQuery( BioSequence b, Taxon taxon, boolean sensitive ) throws IOException {
        assert seqDir != null;
        // write the sequence to a temporary file.
        String seqName = b.getName().replaceAll( " ", "_" );
        File querySequenceFile = File.createTempFile( seqName, ".fa" );

        try (BufferedWriter out = new BufferedWriter( new FileWriter( querySequenceFile ) );) {
            String trimmed = SequenceManipulation.stripPolyAorT( b.getSequence(), POLY_AT_THRESHOLD );
            out.write( ">" + seqName + "\n" + trimmed );
            log.info( "Wrote sequence to " + querySequenceFile.getPath() );
        }
        String outputPath = getTmpPslFilePath( seqName );

        Collection<BlatResult> results = gfClient( querySequenceFile, outputPath,
                choosePortForQuery( taxon, sensitive ) );

        ExternalDatabase searchedDatabase = getSearchedGenome( taxon );
        for ( BlatResult result : results ) {
            result.setSearchedDatabase( searchedDatabase );
        }

        cleanUpTmpFiles( querySequenceFile, outputPath );
        return results;

    }

    /*
     * (non-Javadoc)
     * 
     * @see Blat#blatQuery(java.util.Collection, boolean, ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Map<BioSequence, Collection<BlatResult>> blatQuery( Collection<BioSequence> sequences, boolean sensitive,
            Taxon taxon ) throws IOException {
        Map<BioSequence, Collection<BlatResult>> results = new HashMap<>();

        File querySequenceFile = File.createTempFile( "sequences-for-blat", ".fa" );
        int count = SequenceWriter.writeSequencesToFile( sequences, querySequenceFile );
        if ( count == 0 ) {
            querySequenceFile.delete();
            throw new IllegalArgumentException( "No sequences!" );
        }

        String outputPath = getTmpPslFilePath( "blat-output" );

        Integer port = choosePortForQuery( taxon, sensitive );

        if ( port == null ) {
            throw new IllegalStateException( "Could not locate port for BLAT with settings taxon=" + taxon
                    + ", sensitive=" + sensitive + ", check your configuration." );
        }

        Collection<BlatResult> rawresults = gfClient( querySequenceFile, outputPath, port );

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

    /*
     * (non-Javadoc)
     * 
     * @see Blat#blatQuery(java.util.Collection, ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Map<BioSequence, Collection<BlatResult>> blatQuery( Collection<BioSequence> sequences, Taxon taxon )
            throws IOException {
        return blatQuery( sequences, false, taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see Blat#getBlatScoreThreshold()
     */
    @Override
    public double getBlatScoreThreshold() {
        return this.blatScoreThreshold;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Blat#getGfClientExe()
     */
    @Override
    public String getGfClientExe() {
        return this.gfClientExe;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Blat#getGfServerExe()
     */
    @Override
    public String getGfServerExe() {
        return this.gfServerExe;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Blat#getHost()
     */
    @Override
    public String getHost() {
        return this.host;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Blat#getHumanServerPort()
     */
    @Override
    public int getHumanServerPort() {
        return this.humanServerPort;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Blat#getMouseServerPort()
     */
    @Override
    public int getMouseServerPort() {
        return this.mouseServerPort;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Blat#getRatServerPort()
     */
    @Override
    public int getRatServerPort() {
        return this.ratServerPort;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Blat#getSeqDir()
     */
    @Override
    public String getSeqDir() {
        return this.seqDir;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Blat#getSeqFiles(ShellDelegatingBlat.BlattableGenome)
     */
    @Override
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

    /*
     * (non-Javadoc)
     * 
     * @see Blat#processPsl(java.io.InputStream, ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Collection<BlatResult> processPsl( InputStream inputStream, Taxon taxon ) throws IOException {

        if ( inputStream.available() == 0 ) {
            throw new IOException( "No data from the blat output file. Make sure the gfServer is running" );
        }

        log.debug( "Processing " + inputStream );
        BlatResultParser brp = new BlatResultParser();
        brp.setTaxon( taxon );
        brp.setScoreThreshold( this.blatScoreThreshold );
        brp.parse( inputStream );
        log.info( brp.getNumSkipped() + " results were skipped as being below score= " + this.blatScoreThreshold + "; "
                + brp.getResults().size() + " results retained" );
        return brp.getResults();
    }

    /*
     * (non-Javadoc)
     * 
     * @see Blat#setBlatScoreThreshold(double)
     */
    @Override
    public void setBlatScoreThreshold( double blatScoreThreshold ) {
        this.blatScoreThreshold = blatScoreThreshold;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Blat#startServer(ShellDelegatingBlat.BlattableGenome, int)
     */
    @Override
    public void startServer( BlattableGenome genome, int port ) throws IOException {
        Socket socket = null;
        try {
            socket = new Socket( host, port );
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

            }

        } finally {
            if ( socket != null ) {
                socket.close();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see Blat#stopServer(int)
     */
    @Override
    public void stopServer( int port ) {
        if ( !doShutdown ) {
            return;
        }
        log.info( "Shutting down gfServer" );

        if ( serverProcess == null ) return;
        // serverProcess.destroy();
        try {
            // this doesn't work unless the server was invoked with the option "-canStop"
            Process server = Runtime.getRuntime()
                    .exec( this.getGfServerExe() + " stop " + this.getHost() + " " + port );
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
     * @param genome
     * @return
     */
    private Integer choosePortForQuery( Taxon taxon, boolean sensitive ) {
        BlattableGenome genome = inferBlatDatabase( taxon );
        if ( sensitive ) {
            switch ( genome ) {
                case HUMAN:
                    return humanSensitiveServerPort;
                case MOUSE:
                    return mouseSensitiveServerPort;
                case RAT:
                    return ratSensitiveServerPort;
                default:
                    return humanSensitiveServerPort;

            }
        }
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
     * Run a gfClient query, using a call to exec().
     * 
     * @param querySequenceFile
     * @param outputPath
     * @return
     */
    private Collection<BlatResult> execGfClient( File querySequenceFile, String outputPath, int portToUse )
            throws IOException {
        final String cmd = gfClientExe + " -nohead -minScore=" + MIN_SCORE + " " + host + " " + portToUse + " " + seqDir
                + " " + querySequenceFile.getAbsolutePath() + " " + outputPath;
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
                    // okay, still
                    // waiting.
                }
                Thread.sleep( BLAT_UPDATE_INTERVAL_MS );
                // I hope this is okay...
                synchronized ( outputPath ) {
                    File outputFile = new File( outputPath );
                    Long size = outputFile.length();
                    NumberFormat nf = new DecimalFormat();
                    nf.setMaximumFractionDigits( 2 );
                    String minutes = TimeUtil.getMinutesElapsed( overallWatch );
                    log.info( "BLAT output so far: " + nf.format( size / 1024.0 ) + " kb (" + minutes
                            + " minutes elapsed)" );
                }
            }

            overallWatch.stop();
            String minutes = TimeUtil.getMinutesElapsed( overallWatch );
            log.info( "Blat took a total of " + minutes + " minutes" );

            // int exitVal = run.waitFor();

            log.debug( "blat exit value=" + exitVal );
        } catch ( InterruptedException e ) {
            throw new RuntimeException( e );
        }
        log.debug( "GfClient Success" );

        return processPsl( outputPath, null );
    }

    /**
     * Get a temporary file name.
     * 
     * @throws IOException
     */
    private String getTmpPslFilePath( String base ) throws IOException {
        File tmpdir = new File( Settings.getDownloadPath() );
        if ( StringUtils.isBlank( base ) ) {
            return File.createTempFile( "blat-output", ".psl", tmpdir ).getPath();
        }
        return File.createTempFile( base, ".psl", tmpdir ).getPath();
    }

    /**
     * @param querySequenceFile
     * @param outputPath
     * @return processed results.
     * @throws IOException
     */
    private Collection<BlatResult> gfClient( File querySequenceFile, String outputPath, int portToUse )
            throws IOException {
        // if ( hasNativeLibrary ) return jniGfClientCall( querySequenceFile, outputPath, portToUse );

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
        this.humanServerPort = Settings.getInt( "gfClient.humanServerPort" );
        this.mouseServerPort = Settings.getInt( "gfClient.mouseServerPort" );
        this.ratServerPort = Settings.getInt( "gfClient.ratServerPort" );

        this.humanSensitiveServerPort = Settings.getInt( "gfClient.sensitive.humanServerPort" );
        this.mouseSensitiveServerPort = Settings.getInt( "gfClient.sensitive.mouseServerPort" );
        this.ratSensitiveServerPort = Settings.getInt( "gfClient.sensitive.ratServerPort" );
        // this.humanServerHost = ConfigUtils.getString( "gfClient.humanServerHost" );
        // this.mouseServerHost = ConfigUtils.getString( "gfClient.mouseServerHost" );
        // this.ratServerHost = ConfigUtils.getString( "gfClient.ratServerHost" );
        this.host = Settings.getString( "gfClient.host" );
        this.seqDir = Settings.getString( "gfClient.seqDir" );
        this.mouseSeqFiles = Settings.getString( "gfClient.mouse.seqFiles" );
        this.ratSeqFiles = Settings.getString( "gfClient.rat.seqFiles" );
        this.humanSeqFiles = Settings.getString( "gfClient.human.seqFiles" );
        this.gfClientExe = Settings.getString( "gfClient.exe" );
        this.gfServerExe = Settings.getString( "gfServer.exe" );

        if ( gfServerExe == null ) {
            /*
             * This won't ever really work -- it's left over from earlier iterations.
             */
            log.warn( "You will not be able to start the server: gfServer.exe is not set in config" );
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
                @Override
                public Boolean call() {
                    GfClientCall( host, Integer.toString( portToUse ), seqDir, querySequenceFile.getPath(),
                            outputPath );
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
                    String minutes = TimeUtil.getMinutesElapsed( overallWatch );
                    log.info( "BLAT output so far: " + nf.format( size / 1024.0 ) + " kb (" + minutes
                            + " minutes elapsed)" );
                }

            }

            overallWatch.stop();
            String minutes = TimeUtil.getMinutesElapsed( overallWatch );
            log.info( "Blat took a total of " + minutes + " minutes" );

        } catch ( UnsatisfiedLinkError e ) {
            log.error( e, e );
            log.info( "Falling back on exec()" );
            this.execGfClient( querySequenceFile, outputPath, portToUse );
        }
        return this.processPsl( outputPath, null );
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

}
