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
package ubic.gemma.util.progress;

import net.jini.core.lease.Lease;

import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.util.javaspaces.GemmaSpacesProgressEntry;

/**
 * @author keshav
 * @version $Id$
 */
public class GigaSpacesProgressAppender extends ProgressAppender {

    private GemmaSpacesProgressEntry entry = null;

    private GigaSpacesTemplate gigaSpacesTemplate = null;

    public GigaSpacesProgressAppender( GigaSpacesTemplate gigaSpacesTemplate ) {
        this.gigaSpacesTemplate = gigaSpacesTemplate;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.progress.ProgressAppender#append(org.apache.log4j.spi.LoggingEvent)
     */
    @Override
    protected void append( LoggingEvent event ) {

        if ( gigaSpacesTemplate == null )
            throw new RuntimeException( "Cannot log tasks executing on the compute server.  GigaSpacesTemplate "
                    + "has not been added to the application context." );

        if ( event.getLevel().isGreaterOrEqual( Priority.INFO ) && event.getMessage() != null ) {
            // ProgressManager.updateCurrentThreadsProgressJob( event.getMessage().toString() );
            if ( entry == null ) {
                entry = new GemmaSpacesProgressEntry();
                entry.message = "Logging Server Task";
                gigaSpacesTemplate.write( entry, Lease.FOREVER, 5000 );
            } else {
                try {
                    entry = ( GemmaSpacesProgressEntry ) gigaSpacesTemplate.read( entry, 1000 );
                    entry.setMessage( event.getMessage().toString() );
                    gigaSpacesTemplate.update( entry, Lease.FOREVER, 1000 );
                } catch ( Exception e ) {
                    e.printStackTrace();
                }
            }
        }
    }
}
