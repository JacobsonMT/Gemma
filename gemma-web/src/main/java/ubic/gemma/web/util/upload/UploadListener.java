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
 * Credit:
 *   If you're nice, you'll leave this bit:
 *
 *   Class by Pierre-Alexandre Losson -- http://www.telio.be/blog
 *   email : plosson@users.sourceforge.net
 */
package ubic.gemma.web.util.upload;

import javax.servlet.http.HttpServletRequest;

import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.web.util.progress.ProgressData;
import ubic.gemma.web.util.progress.ProgressJob;
import ubic.gemma.web.util.progress.ProgressManager;

/**
 * @author Original : plosson on 05-janv.-2006 10:46:33 - Last modified by Author: plosson $ on $Date: 2006/01/05
 *         10:09:38
 * @author pavlidis*
 * @version $Id$
 */
public class UploadListener implements OutputStreamListener {

    private static Log log = LogFactory.getLog( UploadListener.class.getName() );

    private long delay = 0;

    private double totalToRead = 0;
    private double totalBytesRead = 0;
    private int totalFiles = -1;
    private ProgressJob pJob;

    /**
     * @param request
     * @param debugDelay
     */
    public UploadListener( HttpServletRequest request, long debugDelay ) {

        this.delay = debugDelay;
        totalToRead = request.getContentLength();

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.upload.OutputStreamListener#start()
     */
    public void start() {
        totalFiles++;
        pJob = ProgressManager.createProgressJob( SecurityContextHolder.getContext().getAuthentication().getName(),
                "File Upload" );
        pJob.updateProgress( new ProgressData( 0, "Uploading File..." ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.upload.OutputStreamListener#bytesRead(int)
     */
    public void bytesRead( int bytesRead ) {
        int oldPercent = pJob.getProgressData().getPercent();

        totalBytesRead = totalBytesRead + bytesRead;
        Double newPercent = ( totalBytesRead / totalToRead ) * 100 ;

        if ( newPercent.intValue() > oldPercent ) {
            pJob.updateProgress();
            oldPercent =  newPercent.intValue();
        }

        try {
            Thread.sleep( delay );
        } catch ( InterruptedException e ) {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.upload.OutputStreamListener#error(java.lang.String)
     */
    @SuppressWarnings("unused")
    public void error( String message ) {
        //pJob.updateProgress( new ProgressData( ( totalBytesRead / totalToRead ) * 100, "Failed to upload", true ) );
        log.error( "There was an error in uploading a file in " + this );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.upload.OutputStreamListener#done()
     */
    
    public void done() {
        pJob.updateProgress( new ProgressData( 100, "Finished Uploading. Processing File...", true ) );
        ProgressManager.destroyProgressJob( pJob );
        
    }

}
