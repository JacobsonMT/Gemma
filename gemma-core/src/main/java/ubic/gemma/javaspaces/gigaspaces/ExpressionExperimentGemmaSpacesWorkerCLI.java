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
package ubic.gemma.javaspaces.gigaspaces;

import net.jini.core.lease.Lease;
import net.jini.space.JavaSpace;

import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springmodules.javaspaces.DelegatingWorker;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.util.AbstractSpringAwareCLI;
import ubic.gemma.util.SecurityUtil;
import ubic.gemma.util.javaspaces.GemmaSpacesGenericEntry;
import ubic.gemma.util.javaspaces.gigaspaces.GemmaSpacesEnum;
import ubic.gemma.util.javaspaces.gigaspaces.GigaSpacesUtil;

import com.j_spaces.core.IJSpace;

/**
 * This command line interface is used to take {@link ExpressionExperimentTask} tasks from the {@link JavaSpace} and
 * returns the results.
 * 
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentGemmaSpacesWorkerCLI extends AbstractSpringAwareCLI {

    private static Log log = LogFactory.getLog( ExpressionExperimentGemmaSpacesWorkerCLI.class );

    private GigaSpacesTemplate template;

    private DelegatingWorker worker;

    private Thread itbThread;

    private IJSpace space = null;

    private GemmaSpacesGenericEntry genericEntry = null;

    private Long workerRegistrationId = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {
        // TODO Auto-generated method stub

    }

    /**
     * Initializes the spring beans.
     * 
     * @throws Exception
     */
    protected void init() throws Exception {

        /* register the shutdown hook so cleanup occurs even if VM is incorrectly terminated */
        ShutdownHook shutdownHook = new ShutdownHook();
        Runtime.getRuntime().addShutdownHook( shutdownHook );

        GigaSpacesUtil gigaspacesUtil = ( GigaSpacesUtil ) this.getBean( "gigaSpacesUtil" );
        ApplicationContext updatedContext = gigaspacesUtil
                .addGigaspacesToApplicationContext( GemmaSpacesEnum.DEFAULT_SPACE.getSpaceUrl() );

        if ( !updatedContext.containsBean( "gigaspacesTemplate" ) )
            throw new RuntimeException( "Gigaspaces beans could not be loaded. Cannot start worker." );

        template = ( GigaSpacesTemplate ) updatedContext.getBean( "gigaspacesTemplate" );
        worker = ( DelegatingWorker ) updatedContext.getBean( "worker" );
        space = ( IJSpace ) template.getSpace();

        workerRegistrationId = RandomUtils.nextLong();
        genericEntry = new GemmaSpacesGenericEntry();
        genericEntry.message = ExpressionExperimentTask.class.getName();
        genericEntry.registrationId = workerRegistrationId;
        Lease lease = space.write( genericEntry, null, 60000000 );
        if ( lease == null ) log.error( "Null Lease returned" );
        // TODO set lease time to large number so it never expires (or only when the
        // worker shuts down. That is, do some "worker cleanup" when it shuts down.
        // This includes removing entries it has written to the space, etc.)?
    }

    /**
     * Starts the thread for this worker.
     */
    protected void start() {
        log.debug( "Current Thread: " + Thread.currentThread().getName() + " Authentication: "
                + SecurityContextHolder.getContext().getAuthentication() );

        itbThread = new Thread( worker );
        itbThread.start();

    }

    /**
     * Starts the command line interface.
     * 
     * @param args
     */
    public static void main( String[] args ) {
        log.info( "Running GigaSpaces Worker To Handle Expression Experiments ... \n" );

        SecurityUtil.passAuthenticationToChildThreads();

        ExpressionExperimentGemmaSpacesWorkerCLI p = new ExpressionExperimentGemmaSpacesWorkerCLI();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( this.getClass().getName(), args );
        try {
            init();
            start();
        } catch ( Exception e ) {
            log.error( "transError problem..." + e.getMessage() );
            e.printStackTrace();
        }
        return err;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractSpringAwareCLI#processOptions()
     */
    @Override
    protected void processOptions() {
        super.processOptions();
    }

    /**
     * A worker shutdown hook.
     * 
     * @author keshav
     */
    public class ShutdownHook extends Thread {
        // TODO move me to a base task.
        public void run() {
            log.info( "Worker shut down.  Running shutdown hook ... cleaning up registered entries for this worker." );
            if ( space != null ) {
                try {
                    space.clear( genericEntry, null );
                } catch ( Exception e ) {

                    log.error( "Error clearing the generic entry " + genericEntry + "for task " + genericEntry.message
                            + "from space." );
                    e.printStackTrace();
                }
            }
        }
    }

}
