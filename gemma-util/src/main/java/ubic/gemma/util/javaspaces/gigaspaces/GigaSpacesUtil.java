/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.util.javaspaces.gigaspaces;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.context.support.GenericWebApplicationContext;

import ubic.gemma.util.javaspaces.GemmaSpacesGenericEntry;

import com.j_spaces.core.IJSpace;
import com.j_spaces.core.admin.IJSpaceContainerAdmin;
import com.j_spaces.core.admin.StatisticsAdmin;
import com.j_spaces.core.client.FinderException;
import com.j_spaces.core.client.SpaceFinder;
import com.j_spaces.core.exception.StatisticsNotAvailable;

/**
 * A utility class to test gigaspaces features such as if the space is running, whether to add the gigaspaces beans to
 * the spring context, whether workers are available, etc. This class is {@link ApplicationContextAware} and therefore
 * knows about the context that creates it.
 * 
 * @author keshav
 * @version $Id$
 */
public class GigaSpacesUtil implements ApplicationContextAware {

    private static Log log = LogFactory.getLog( GigaSpacesUtil.class );

    private static final String GIGASPACES_SPRING_CONTEXT = "ubic/gemma/gigaspaces.xml";

    private ApplicationContext applicationContext = null;

    /**
     * Determines if the {@link ApplicationContext} contains gigaspaces beans.
     */
    private boolean contextContainsGigaspaces() {

        return applicationContext.containsBean( "gigaspacesTemplate" );

    }

    /**
     * Checks if space is running at specified url.
     * 
     * @param ctx
     * @return boolean
     */
    public static boolean isSpaceRunning( String url ) {

        boolean running = true;
        try {
            SpaceFinder.find( url );
        } catch ( FinderException e ) {
            running = false;
            log.error( "Error finding space at: " + url + "." );
            // e.printStackTrace();
        } finally {
            return running;
        }
    }

    /**
     * Add the gigaspaces contexts to the other spring contexts.
     * 
     * @param paths
     */
    public static void addGigaspacesContextToPaths( List<String> paths ) {
        paths.add( "classpath*:" + GIGASPACES_SPRING_CONTEXT );
    }

    /**
     * First checks if the space is running at url. If space is running, adds the gigaspaces beans to the context if
     * they do not exist. If the space is not running, returns the original context.
     * 
     * @param url
     * @return ApplicatonContext
     */
    public ApplicationContext addGigaspacesToApplicationContext( String url ) {

        if ( !isSpaceRunning( url ) ) {
            log.error( "Cannot add Gigaspaces to application context. Space not started at " + url
                    + ". Returning context without gigaspaces beans." );

            return applicationContext;

        }

        if ( !contextContainsGigaspaces() ) {

            GenericWebApplicationContext genericCtx = new GenericWebApplicationContext();
            XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader( genericCtx );
            xmlReader.loadBeanDefinitions( new ClassPathResource( GIGASPACES_SPRING_CONTEXT ) );
            // PropertiesBeanDefinitionReader propReader = new PropertiesBeanDefinitionReader( genericCtx );
            // propReader.loadBeanDefinitions(new ClassPathResource("otherBeans.properties"));

            genericCtx.setParent( applicationContext );

            genericCtx.refresh();

            return genericCtx;

        }

        else {
            log.info( "Application context unchanged. Gigaspaces beans already exist." );
        }

        return applicationContext;

    }

    /**
     * First checks if the space is running at the given url. If it is running, returns the {@link StatisticsAdmin},
     * which is useful for administration statistics. If the space is not running, returns null.
     * 
     * @param url
     */
    public static StatisticsAdmin getStatisticsAdmin( String url ) {
        if ( !isSpaceRunning( url ) ) {
            log.error( "Space not started at " + url + ". Cannot get statistics admin." );
            return null;
        }
        try {
            IJSpace space = ( IJSpace ) SpaceFinder.find( url );
            StatisticsAdmin admin = ( StatisticsAdmin ) space.getAdmin();
            return admin;
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * Logs the space statistics from the {@link StatisticsAdmin}.
     * 
     * @param url
     */
    public static void logSpaceStatistics( String url ) {
        StatisticsAdmin admin = getStatisticsAdmin( url );

        if ( admin != null ) {
            try {
                Map statsMap = admin.getStatistics();
                Collection keys = statsMap.keySet();
                Iterator iter = keys.iterator();
                while ( iter.hasNext() ) {
                    log.debug( statsMap.get( iter.next() ) );
                }
            } catch ( StatisticsNotAvailable e ) {
                throw new RuntimeException( e );
            } catch ( RemoteException e ) {
                throw new RuntimeException( e );
            }
        }
        log.error( "Statistics unavailable." );
    }

    /**
     * First checks to see if the space is running at the given url. If the space is running, returns the
     * {@link IJSpaceContainerAdmin}, which is useful to obtain space information such as the runtime configuration
     * report. If the space is not running, returns null.
     * 
     * @param url
     * @return {@link IJSpaceContainerAdmin}
     */
    public static IJSpaceContainerAdmin getContainerSpaceAdmin( String url ) {
        if ( !isSpaceRunning( url ) ) {
            log.error( "Space not started at " + url + ". Cannot get container admin." );
            return null;
        }
        try {
            IJSpace space = ( IJSpace ) SpaceFinder.find( url );
            IJSpaceContainerAdmin admin = ( IJSpaceContainerAdmin ) space.getContainer();
            return admin;
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Logs the runtime configuration report. This report contains information about the space, including the system
     * environment configuration.
     */
    public static void logRuntimeConfigurationReport() {
        IJSpaceContainerAdmin admin = ( IJSpaceContainerAdmin ) GigaSpacesUtil
                .getContainerSpaceAdmin( GemmaSpacesEnum.DEFAULT_SPACE.getSpaceUrl() );

        if ( admin != null ) {
            try {
                log.info( "Runtime configuration report: " + admin.getRuntimeConfigReport() );
            } catch ( RemoteException e ) {
                e.printStackTrace();
            }
        }

        log.error( "Runtime configuration report unavailable." );
    }

    /**
     * @param url
     * @return int
     */
    public int numWorkersRegistered( String url ) {
        int count = 0;

        if ( !isSpaceRunning( url ) ) {
            log.error( "Space not started at " + url + ". Returning a count of 0 (workers registered)." );
            return count;
        }

        try {
            IJSpace space = ( IJSpace ) SpaceFinder.find( url );

            count = space.count( new GemmaSpacesGenericEntry(), null );
            log.info( "count: " + count );
        } catch ( Exception e ) {
            log.error( "Could not check for workers registered.  Assuming 0 workers are registered." );
            e.printStackTrace();
            return 0;
        }

        return count;
    }

    /**
     * Returns a list of all the registered workers.
     * 
     * @param url
     * @return List<GemmaSpacesGenericEntry>
     */
    public List<GemmaSpacesGenericEntry> getRegisteredWorkers( String url ) {

        GemmaSpacesGenericEntry[] workerEntries = null;
        if ( !isSpaceRunning( url ) ) {
            log.error( "Space not started at " + url + ". Returning a count of 0 (workers registered)." );
            return null;
        }

        try {
            IJSpace space = ( IJSpace ) SpaceFinder.find( url );

            Object[] commandObjects = space.readMultiple( new GemmaSpacesGenericEntry(), null, 120000 );
            workerEntries = new GemmaSpacesGenericEntry[commandObjects.length];

            for ( int i = 0; i < commandObjects.length; i++ ) {
                GemmaSpacesGenericEntry entry = ( GemmaSpacesGenericEntry ) commandObjects[i];
                workerEntries[i] = entry;
                log.debug( "entry: " + entry );
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            return null;
        }
        return Arrays.asList( workerEntries );
    }

    /**
     * Returns true if workers are registered with the space at the given url.
     * 
     * @param url
     * @return boolean
     */
    public boolean areWorkersRegistered( String url ) {
        boolean registered = false;

        if ( numWorkersRegistered( url ) > 0 ) registered = true;

        return registered;

    }

    /**
     * Returns the list of tasks that can currently be serviced at the space url based on the currently registered
     * workers.
     * 
     * @param url
     * @return List <String>
     */
    public List<String> tasksThatCanBeServiced( String url ) {

        List<String> taskNames = new ArrayList<String>();

        if ( !this.areWorkersRegistered( url ) ) {
            log.error( "No workers are registered with space at " + url + ".  Currently no tasks can be serviced." );
        }

        List<GemmaSpacesGenericEntry> workerEntries = this.getRegisteredWorkers( url );
        for ( GemmaSpacesGenericEntry entry : workerEntries ) {
            String taskName = entry.getMessage();
            log.debug( taskName );
            taskNames.add( taskName );
        }

        return taskNames;
    }

    /**
     * Returns true if the task can be serviced by the space at the given url.
     * 
     * @param taskName
     * @param url The url of the space.
     * @return boolean
     */
    public boolean canServiceTask( String taskName, String url ) {
        boolean serviceable = false;

        List<String> serviceableTasks = this.tasksThatCanBeServiced( url );

        if ( serviceableTasks.contains( taskName ) ) {
            serviceable = true;
            log.info( "Can service task with name " + taskName );
        }

        else {
            log.error( "Cannot service task with name " + taskName );
        }

        return serviceable;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    public void setApplicationContext( ApplicationContext applicationContext ) throws BeansException {
        this.applicationContext = applicationContext;

    }

}
