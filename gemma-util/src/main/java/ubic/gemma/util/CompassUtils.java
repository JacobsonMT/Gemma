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
package ubic.gemma.util;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.store.FSDirectory;
import org.compass.gps.spi.CompassGpsInterfaceDevice;

/**
 * Utility methods to manipulate compass (and lucene).
 * 
 * @author keshav
 * @version $Id$
 */
public class CompassUtils {

    private static Log log = LogFactory.getLog( CompassUtils.class );

    /**
     * Deletes compass lock file(s).
     * 
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public static void deleteCompassLocks() {
        log.debug( "compass lock dir: " + FSDirectory.LOCK_DIR );

        Collection<File> lockFiles = FileUtils.listFiles( new File( FSDirectory.LOCK_DIR ), FileFilterUtils
                .suffixFileFilter( "lock" ), null );

        if ( lockFiles.size() == 0 ) {
            log.info( "Compass lock files do not exist." );
            return;
        }

        for ( File file : lockFiles ) {
            log.warn( "removing file " + file );
            // FileUtils.forceDeleteOnExit( file ); //delete on jvm term.
            file.delete(); // delete right away, not on jvm termination (not forcing).
        }
    }

    /**
     * Deletes and re-creates the index.
     * 
     * @param gps
     * @throws IOException
     */
    public static synchronized void rebuildCompassIndex( CompassGpsInterfaceDevice gps ) {
        boolean wasRunningBefore = gps.isRunning();

        log.info( "CompassGps was running? " + wasRunningBefore );

        /* Check state of device. If not running and you try to index, you will get a device exception. */
        if ( !wasRunningBefore ) {
            enableIndexMirroring( gps );
        }

        /* We don't need to check if index already exists. If it doesn't, it won't be deleted. */
        gps.getIndexCompass().getSearchEngineIndexManager().deleteIndex();
        log.info( "Deleting old index" );
        gps.getIndexCompass().getSearchEngineIndexManager().createIndex();
        log.info( "indexing now ... " );
        gps.index();

        /* Return state of device */
        if ( !wasRunningBefore ) {
            disableIndexMirroring( gps );
        }

    }

    /**
     * Add the compass contexts to the other spring contexts
     * 
     * @param paths
     */
    private static void addCompassContext( List<String> paths ) {
        paths.add( "classpath*:ubic/gemma/compass.xml" );
        paths.add( "classpath*:ubic/gemma/applicationContext-compass.xml" );
    }

    /**
     * Add the compass test contexts to the other spring contexts.
     * 
     * @param paths
     */
    private static void addCompassTestContext( List<String> paths ) {
        paths.add( "classpath*:ubic/gemma/compasstest.xml" );
        paths.add( "classpath*:ubic/gemma/applicationContext-compass.xml" );
    }

    /**
     * disables the index mirroring operation.
     * 
     * @param device
     */
    public static void disableIndexMirroring( CompassGpsInterfaceDevice device ) {
        device.stop();
    }

    /**
     * enables the index mirroring operation.
     * 
     * @param device
     */
    public static void enableIndexMirroring( CompassGpsInterfaceDevice device ) {
        device.start();
    }

    /**
     * "Turning on" means adding the compass context to our spring context, as well as creating the compass index
     * directory. This does not turn on index mirroring to automatically update the index while persisting data (to a
     * database). To do this, call enableIndexMirroring after running this.
     * 
     * @param testEnv
     * @param paths
     */
    public static void turnOnCompass( boolean testEnv, List<String> paths ) {
        deleteCompassLocks();
        if ( testEnv ) {
            addCompassTestContext( paths );
        } else {
            addCompassContext( paths );
        }

    }
}
