package ubic.gemma.loader.util.fetcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import ubic.basecode.util.NetUtils;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.util.NetDatasourceUtil;

public abstract class FtpFetcher extends AbstractFetcher {

    protected FTPClient ftpClient;

    protected NetDatasourceUtil netDataSourceUtil;

    /**
     * 
     */
    public FtpFetcher() {
        super();
        setNetDataSourceUtil();
    }

    /**
     * @param outputFileName
     * @param seekFile
     * @return
     */
    protected FutureTask<Boolean> defineTask( final String outputFileName, final String seekFile ) {
        FutureTask<Boolean> future = new FutureTask<Boolean>( new Callable<Boolean>() {
            @SuppressWarnings("synthetic-access")
            public Boolean call() throws FileNotFoundException, IOException {
                log.info( "Fetching " + seekFile + " to " + outputFileName );
                boolean status = NetUtils.ftpDownloadFile( ftpClient, seekFile, outputFileName, force );
                ftpClient.disconnect();
                return new Boolean( status );
            }
        } );
        return future;
    }

    public Collection<LocalFile> fetch( String identifier ) {

        File existingFile = null;
        String seekFile = formRemoteFilePath( identifier );
        try {
            File newDir = mkdir( identifier );
            String outputFileName = formLocalFilePath( identifier, newDir );

            existingFile = new File( outputFileName );
            if ( existingFile.canRead() ) {
                log.info( outputFileName + " already exists: checking size match." );
            }

            if ( ftpClient == null || !ftpClient.isConnected() ) {
                ftpClient = this.getNetDataSourceUtil().connect( FTP.BINARY_FILE_TYPE );
            }

            long expectedSize = getExpectedSize( seekFile );

            FutureTask<Boolean> future = this.defineTask( outputFileName, seekFile );
            Collection<LocalFile> result = this.doTask( future, expectedSize, seekFile, outputFileName );
            return result;
        } catch ( UnknownHostException e ) {
            if ( force || !allowUseExisting || existingFile == null ) throw new RuntimeException( e );

            log.warn( "Could not connect to " + this.getNetDataSourceUtil().getHost() + " to check size of " + seekFile
                    + ", using existing file" );
            Collection<LocalFile> fallback = getExistingFile( existingFile, seekFile );
            return fallback;
        } catch ( IOException e ) {
            log.error( e, e );
            throw new RuntimeException( "Couldn't fetch " + seekFile, e );
        }
    }

    /**
     * @param seekFile
     * @return
     * @throws IOException
     * @throws SocketException
     */
    protected long getExpectedSize( final String seekFile ) throws IOException, SocketException {
        long expectedSize = 0;

        try {
            expectedSize = NetUtils.ftpFileSize( ftpClient, seekFile );
        } catch ( FileNotFoundException e ) {
            // when this happens we need to reconnect.
            log.error( e );
            log.warn( "Couldn't get remote file size for " + seekFile );
            InetAddress ad = ftpClient.getRemoteAddress();
            ftpClient.disconnect();
            ftpClient.connect( ad );
        }
        return expectedSize;
    }

    /**
     * @param future
     * @param expectedSize
     * @param outputFileName
     * @param seekFileName
     * @return
     */
    protected Collection<LocalFile> doTask( FutureTask<Boolean> future, long expectedSize, String seekFileName,
            String outputFileName ) {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute( future );
        executor.shutdown();

        try {

            File outputFile = new File( outputFileName );
            waitForDownload( future, expectedSize, outputFile );

            if ( future.get().booleanValue() ) {
                if ( log.isInfoEnabled() ) log.info( "Done: local file is " + outputFile );
                LocalFile file = fetchedFile( seekFileName, outputFile.getAbsolutePath() );
                Collection<LocalFile> result = new HashSet<LocalFile>();
                result.add( file );
                return result;
            }
        } catch ( ExecutionException e ) {
            throw new RuntimeException( "Couldn't fetch " + seekFileName, e );
        } catch ( InterruptedException e ) {
            throw new RuntimeException( "Interrupted: Couldn't fetch " + seekFileName, e );
        }
        throw new RuntimeException( "Couldn't fetch file for " + seekFileName );
    }

    /**
     * @param netDataSourceUtil the netDataSourceUtil to set
     */
    public abstract void setNetDataSourceUtil();

    /**
     * @return the netDataSourceUtil
     */
    public NetDatasourceUtil getNetDataSourceUtil() {
        return this.netDataSourceUtil;
    }
}
