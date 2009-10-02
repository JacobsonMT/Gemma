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
package ubic.gemma.loader.util.fetcher;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.FutureTask;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceProcessingService;
import ubic.gemma.model.common.description.LocalFile;

/**
 * @author pavlidis
 * @version $Id$
 */
public abstract class AbstractFetcher implements Fetcher {

    protected static final int INFO_UPDATE_INTERVAL = 5000;
    protected static Log log = LogFactory.getLog( ArrayDesignSequenceProcessingService.class.getName() );
    /**
     * Whether we are allowed to use an existing file rather than downloading again, in the case where we can't connect
     * to the remote host to check the size of the file. Setting force=true overrides this. Default is FALSE.
     */
    protected boolean allowUseExisting = false;

    /**
     * Whether download is required even if the sizes match.
     */
    protected boolean force = false;

    protected String localBasePath = null;

    protected String remoteBaseDir = null;

    /**
     * 
     */
    public AbstractFetcher() {
        super();
        initConfig();
    }

    /**
     * @return Returns the localBasePath.
     */
    public String getLocalBasePath() {
        return this.localBasePath;
    }

    /**
     * @return the force
     */
    public boolean isForce() {
        return this.force;
    }

    /**
     * @param allowUseExisting the allowUseExisting to set
     */
    public void setAllowUseExisting( boolean allowUseExisting ) {
        this.allowUseExisting = allowUseExisting;
    }

    /**
     * Set to true if downloads should proceed even if the file already exists.
     * 
     * @param force
     */
    public void setForce( boolean force ) {
        this.force = force;
    }

    /**
     * @param seekFile
     * @return
     */
    protected LocalFile fetchedFile( String seekFile ) {
        return this.fetchedFile( seekFile, seekFile );
    }

    /**
     * @param seekFilePath Absolute path to the file for download
     * @param outputFilePath Absolute path to the download location.
     * @return
     */
    protected LocalFile fetchedFile( String seekFilePath, String outputFilePath ) {
        LocalFile file = LocalFile.Factory.newInstance();
        file.setVersion( new SimpleDateFormat().format( new Date() ) );
        try {
            file.setRemoteURL( ( new File( seekFilePath ) ).toURI().toURL() );
            file.setLocalURL( ( new File( outputFilePath ).toURI().toURL() ) );
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( e );
        }
        return file;
    }

    protected abstract String formLocalFilePath( String identifier, File newDir );

    protected abstract String formRemoteFilePath( String identifier );

    /**
     * Wrap the existing file in the required Collection&lt;LocalFile&gt;
     * 
     * @param existingFile
     * @param seekFile
     * @return
     */
    protected Collection<LocalFile> getExistingFile( File existingFile, String seekFile ) {
        Collection<LocalFile> fallback = new HashSet<LocalFile>();
        LocalFile lf = LocalFile.Factory.newInstance();
        try {
            lf.setLocalURL( existingFile.toURI().toURL() );
            lf.setRemoteURL( ( new File( seekFile ) ).toURI().toURL() );
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( e );
        }
        lf.setSize( existingFile.length() );
        fallback.add( lf );
        return fallback;
    }

    protected abstract void initConfig();

    /**
     * Like mkdir(accession) but for cases where there is no accession.
     * 
     * @return
     */
    protected File mkdir() throws IOException {
        return this.mkdir( null );
    }

    /**
     * Create a directory according to the current accession number and set path information, including any nonexisting
     * parent directories. If the path cannot be used, we use a temporary directory.
     * 
     * @param accession
     * @return new directory
     * @throws IOException
     */
    protected File mkdir( String accession ) throws IOException {
        File newDir = null;
        File targetPath = null;

        if ( localBasePath != null ) {

            targetPath = new File( localBasePath );

            if ( !( targetPath.exists() && targetPath.canRead() ) ) {
                log.warn( "Attempting to create directory '" + localBasePath + "'" );
                targetPath.mkdirs();
            }

            if ( accession == null ) {
                newDir = targetPath;
            } else {
                newDir = new File( targetPath + File.separator + accession );
            }

        }

        if ( localBasePath == null || !targetPath.canRead() ) {
            log.warn( "Could not create output directory " + newDir );

            File tmpDir;
            String systemTempDir = System.getProperty( "java.io.tmpdir" );
            if ( accession == null ) {
                tmpDir = new File( systemTempDir );
            } else {
                tmpDir = new File( systemTempDir + File.separator + accession );
            }
            log.warn( "Will use local temporary directory: " + tmpDir.getAbsolutePath() );
            newDir = tmpDir;
        }

        if ( newDir == null ) {
            throw new IOException( "Could not create target directory, was null" );
        }
        if ( !newDir.exists() && !newDir.mkdirs() ) {
            throw new IOException( "Could not create target directory " + newDir.getAbsolutePath() );
        }
        if ( !newDir.canWrite() ) {
            throw new IOException( "Cannot write to target directory " + newDir.getAbsolutePath() );
        }

        log.info( "New dir is " + newDir );

        return newDir;
    }

    /**
     * @param future
     * @return true if it finished normally, false if it was cancelled.
     */
    protected boolean waitForDownload( FutureTask<Boolean> future ) {
        StopWatch timer = new StopWatch();
        timer.start();
        long lastTime = timer.getTime();
        while ( !future.isDone() && !future.isCancelled() ) {
            try {
                Thread.sleep( INFO_UPDATE_INTERVAL );
            } catch ( InterruptedException ie ) {
                log.info( "Cancelling download" );
                boolean cancelled = future.cancel( true );
                if ( cancelled ) {
                    log.info( "Download stopped successfully." );
                    return false;
                }
                throw new RuntimeException( "Cancellation failed." );

            }

            if ( log.isInfoEnabled() && timer.getTime() > ( lastTime + 2000L ) ) {
                log.info( "Waiting ... " + timer.getTime() + "ms elapsed...." );
            }
        }
        return true;
    }

    /**
     * @param future
     * @param expectedSize
     * @param outputFileName
     * @return true if it finished normally, false if it was cancelled.
     */
    protected boolean waitForDownload( FutureTask<Boolean> future, long expectedSize, File outputFile ) {
        while ( !future.isDone() && !future.isCancelled() ) {
            try {
                Thread.sleep( INFO_UPDATE_INTERVAL );
            } catch ( InterruptedException ie ) {
                log.info( "Cancelling download" );
                boolean cancelled = future.cancel( true );
                if ( cancelled ) {
                    log.info( "Download stopped successfully." );
                    return false;
                }

                // double check...
                if ( future.isCancelled() ) {
                    return false;
                }

                log.error( "Cancellation failed..." );

                throw new RuntimeException( "Cancellation failed." );

            }

            if ( log.isInfoEnabled() ) {
                log.info( ( outputFile.length() + ( expectedSize > 0 ? "/" + expectedSize : "" ) + " bytes read" ) );
            }
        }
        log.info( "Done with download, " + outputFile.length() + " bytes read" );
        return true;
    }

}
