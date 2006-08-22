/* Copyright (c) 2006 University of British Columbia
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

package ubic.gemma.web.util.progress;

import java.util.Collection;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Singleton for creating observers for monitoring long-running processes.
 * <p>
 * To use, call ProgressManager.createProgressJob(). The returned value can be used by a client object to indicate
 * progress by calling updateProgress on the job that is returned. Observers can register themselves for receiving
 * progress updates by passing themselves into the addToNotification function
 * 
 * @author kelsey
 * @version $Id$
 */
public class ProgressManager {

    protected static final Log logger = LogFactory.getLog( ProgressManager.class );

    /*
     * Must use the getter methods to use these static hashmaps so that i can gaurantee syncronization amongst different
     * threads using the maps.
     */
    private static Map<ProgressJob, Collection<ProgressObserver>> notificationListByJob = new ConcurrentHashMap<ProgressJob, Collection<ProgressObserver>>();
    private static Map<String, Collection<ProgressObserver>> notificationListByUser = new ConcurrentHashMap<String, Collection<ProgressObserver>>();
    private static Map<String, Collection<ProgressJob>> progressJobs = new ConcurrentHashMap<String, Collection<ProgressJob>>();

//    protected static synchronized  Map<ProgressJob, Collection<ProgressObserver>> getNotificationListByJob() {return notificationListByJob};
//    protected static synchronized  Map<String, Collection<ProgressObserver>> getnotificationListByUser() {return notificationListByUser};
//    protected static synchronized  Map<String, Collection<ProgressJob>> getprogressJobs() {return progressJobs};
    /**
     * @param pj
     * @param po
     * @return FIXME: Don't Use. Adding notification by ProgressJob doesn't make sense as it genearlly turns out to be
     *         the situation that the observer doesn't have an instance of the progressJob that they would like to
     *         observe. But there seams to be no other way to deal with an owner having multiple jobs running at the
     *         same time and being able to distinguish which job would like to be observed. this needs to be flushed out
     *         and this functionality needs to be added correctly.
     */
    public static synchronized boolean addToNotification( ProgressJob pj, ProgressObserver po ) {

        if ( !progressJobs.containsKey( pj.getUser() ) ) return false; // No such job exists

        if ( !notificationListByJob.containsKey( pj ) ) {
            Collection<ProgressObserver> newList = new Vector<ProgressObserver>();
            newList.add( po );
            notificationListByJob.put( pj, newList );

        } else {
            Collection<ProgressObserver> poList = notificationListByJob.get( pj );
            if ( !poList.contains( po ) ) poList.add( po );
        }
        return true;

    }

    /**
     * @param username
     * @param po
     * @return currently the best way for observers to add themselves to watching a given job. There are issues with an
     *         owner having multiple jobs.
     */
    public static synchronized boolean addToNotification( String username, ProgressObserver po ) {
        logger.error( "Attempting to add an observer to notification list:  " + username + "  " + po );

        if ( !progressJobs.containsKey( username ) ) return false; // No such user exists with any jobs

        if ( !notificationListByUser.containsKey( username ) ) {
            Collection<ProgressObserver> newList = new Vector<ProgressObserver>();
            newList.add( po );
            notificationListByUser.put( username, newList );

        } else {
            Collection<ProgressObserver> poList = notificationListByUser.get( username );
            if ( !poList.contains( po ) ) {
                poList.add( po );
                logger.error( "Added observer to notification list:  " + username + "  " + po );
            }
        }

        return true;
    }

    /**
     * @param userName (owner of the job)
     * @param description (description of the job)
     * @return Use this static method for creating ProgressJobs.
     */
    public static synchronized ProgressJob createProgressJob( String userName, String description ) {

        Collection<ProgressJob> usersJobs;
        ProgressJob newJob;

        if ( !progressJobs.containsKey( userName ) ) {
            progressJobs.put( userName, new Vector<ProgressJob>() );
        }

        usersJobs = progressJobs.get( userName );
        newJob = new ProgressJobImpl( userName, description );
        usersJobs.add( newJob );

        logger.error( "Created new ProgressJob: " + newJob );

        return newJob;
    }

    /**
     * @param ajob Removes ProgressJob form lists and provides clean up. This is a package level service used for
     *        maintaing progress jobs.
     *        TODO removal of a job shouldn't remove the user as a user may have more than 1 job.
     */
    static synchronized boolean  destroyProgressJob( ProgressJob ajob ) {

        if ( notificationListByJob.containsKey( ajob ) ) notificationListByJob.remove( ajob );

        if ( notificationListByUser.containsKey( ajob.getUser() ) ) notificationListByUser.remove( ajob.getUser() );

        if ( progressJobs.containsKey( ajob.getUser() ) ) {
            Collection jobs = progressJobs.get( ajob.getUser() );
            jobs.remove( ajob );
        }

        return true;
    }

    /**
     * @param pj
     * @return Another packgage level serverice for notifying the observers that there has been changes in the
     *         ProgressJob that they have registered to watch.
     */
    static synchronized boolean  notify( ProgressJob pj ) {

        if ( notificationListByJob.containsKey( pj ) ) {

            for ( ProgressObserver observer : notificationListByJob.get( pj ) ) {
                observer.progressUpdate( pj.getProgressData() );
            }

            // return true;
        }

        if ( notificationListByUser.containsKey( pj.getUser() ) ) {

            for ( ProgressObserver observer : notificationListByUser.get( pj.getUser() ) ) {
                observer.progressUpdate( pj.getProgressData() );
                logger.error( "Notifying observer:  " + pj.getUser() + "  " + pj.getProgressData().getPercent()
                        + "%  :" + pj.getProgressData().getDescription() );
            }

            return true;
        }

        return false;

    }

}
