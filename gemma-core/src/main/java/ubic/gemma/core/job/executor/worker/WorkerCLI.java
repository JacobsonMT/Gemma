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
package ubic.gemma.core.job.executor.worker;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.AbstractSpringAwareCLI;
import ubic.gemma.persistence.util.SpringContextUtil;

/**
 * Generic tool for starting a remote worker.
 *
 * @author keshav
 */
public class WorkerCLI extends AbstractSpringAwareCLI {

    private RemoteTaskRunningService taskRunningService;

    @Override
    public String getShortDesc() {
        return "Start worker application for processing tasks sent by Gemma webapp.";
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {

    }

    @Override
    protected String[] getAdditionalSpringConfigLocations() {
        return new String[] { "classpath*:ubic/gemma/workerContext-component-scan.xml",
                "classpath*:ubic/gemma/workerContext-jms.xml" };
    }

    @Override
    protected void createSpringContext( CommandLine commandLine ) {
        ctx = SpringContextUtil.getApplicationContext( commandLine.hasOption( "testing" ), false /* webapp */,
                this.getAdditionalSpringConfigLocations() );

        /*
         * Important to ensure that threads get permissions from their context - not global!
         */
        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_INHERITABLETHREADLOCAL );
    }

    @Override
    public String getCommandName() {
        return "startWorker";
    }

    @Override
    protected void buildOptions( Options options ) {
    }

    @Override
    protected void doWork() throws Exception {
        this.init();
    }

    private void init() {
        ShutdownHook shutdownHook = new ShutdownHook();
        Runtime.getRuntime().addShutdownHook( shutdownHook );

        taskRunningService = ctx.getBean( RemoteTaskRunningService.class );
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public class ShutdownHook extends Thread {
        @Override
        public void run() {
            AbstractCLI.log.info( "Remote task executor is shutting down..." );
            AbstractCLI.log.info( "Attempting to cancel all running tasks..." );
            taskRunningService.shutdown();
            AbstractCLI.log.info( "Shutdown sequence completed." );
        }
    }
}
