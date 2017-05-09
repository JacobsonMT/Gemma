/*
 * The Gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import gemma.gsec.authentication.UserDetailsImpl;
import gemma.gsec.authentication.UserManager;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import ubic.gemma.core.job.executor.common.ExecutingTask;
import ubic.gemma.core.job.executor.common.ProgressUpdateAppender;
import ubic.gemma.core.tasks.AbstractTask;
import ubic.gemma.core.tasks.Task;
import ubic.gemma.core.testing.BaseSpringContextTest;

/**
 * @author anton
 * @version $Id$
 */
public class ExecutingTaskTest extends BaseSpringContextTest {

    private static class FailureTestTask extends AbstractTask<TaskResult, TaskCommand> {

        @Override
        public TaskResult execute() {
            throw new AccessDeniedException( "Test!" );
        }
    }

    private static class SuccessTestTask extends AbstractTask<TaskResult, TaskCommand> {

        @Override
        public TaskResult execute() {
            return new TaskResult( getTaskCommand(), "SUCCESS" );
        }
    }

    @Autowired
    UserManager userManager;
    private AtomicBoolean onStartRan = new AtomicBoolean( false );

    private AtomicBoolean onFailureRan = new AtomicBoolean( false );
    private AtomicBoolean onFinishRan = new AtomicBoolean( false );

    private AtomicBoolean appenderInitialized = new AtomicBoolean( false );
    private AtomicBoolean appenderTakenDown = new AtomicBoolean( false );
    private SecurityContext securityContextAfterInitialize;

    private SecurityContext securityContextAfterFinish;

    private SecurityContext securityContextAfterFail;

    private ProgressUpdateAppender progressAppender = new ProgressUpdateAppender() {
        @Override
        public void initialize() {
            appenderInitialized.set( true );
        }

        @Override
        public void tearDown() {
            appenderTakenDown.set( true );
        }
    };

    private ExecutingTask.TaskLifecycleHandler statusUpdateHandler = new ExecutingTask.TaskLifecycleHandler() {
        @Override
        public void onFailure( Throwable e ) {
            onFailureRan.set( true );
        }

        @Override
        public void onFinish() {
            onFinishRan.set( true );
        }

        @Override
        public void onStart() {
            onStartRan.set( true );
        }
    };

    private ExecutingTask.TaskLifecycleHandler statusSecurityContextCheckerHandler = new ExecutingTask.TaskLifecycleHandler() {
        @Override
        public void onFailure( Throwable e ) {
            securityContextAfterFinish = SecurityContextHolder.getContext();
        }

        @Override
        public void onFinish() {
            securityContextAfterFinish = SecurityContextHolder.getContext();
        }

        @Override
        public void onStart() {
            securityContextAfterInitialize = SecurityContextHolder.getContext();
        }
    };

    @Before
    public void setUp() throws Exception {
        onStartRan.set( false );
        onFailureRan.set( false );
        onFinishRan.set( false );

        appenderInitialized.set( false );
        appenderTakenDown.set( false );

        securityContextAfterInitialize = null;
        securityContextAfterFinish = null;
        securityContextAfterFail = null;
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testOrderOfExecutionFailure() {
        TaskCommand taskCommand = new TaskCommand();
        Task<TaskResult, TaskCommand> task = new FailureTestTask();
        task.setTaskCommand( taskCommand );

        ExecutingTask<TaskResult> executingTask = new ExecutingTask<>( task, taskCommand );
        executingTask.setProgressAppender( progressAppender );
        executingTask.setStatusCallback( statusUpdateHandler );

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<TaskResult> future = executorService.submit( executingTask );
        try {
            TaskResult taskResult = future.get();
            Throwable exception = taskResult.getException();
            assertEquals( AccessDeniedException.class, exception.getClass() );
            assertEquals( "Test!", exception.getMessage() );
        } catch ( InterruptedException e ) {
            fail();
        } catch ( ExecutionException e ) {
            fail();
        }

        assertTrue( appenderInitialized.get() );
        assertTrue( appenderTakenDown.get() );

        assertTrue( onStartRan.get() );
        assertFalse( onFinishRan.get() );
        assertTrue( onFailureRan.get() );
    }

    // TODO: Test security context under different failure scenarios
    // - exception in the task
    // - exception in the lifecycle hook
    // - exception in progress appender setup/teardown hook
    @Test
    public void testOrderOfExecutionSuccess() {
        TaskCommand taskCommand = new TaskCommand();
        Task<TaskResult, TaskCommand> task = new SuccessTestTask();
        task.setTaskCommand( taskCommand );

        ExecutingTask<TaskResult> executingTask = new ExecutingTask<>( task, taskCommand );
        executingTask.setProgressAppender( progressAppender );
        executingTask.setStatusCallback( statusUpdateHandler );

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<TaskResult> future = executorService.submit( executingTask );
        try {
            TaskResult taskResult = future.get();
            String answer = ( String ) taskResult.getAnswer();
            assertEquals( "SUCCESS", answer );
        } catch ( InterruptedException e ) {
            fail();
        } catch ( ExecutionException e ) {
            fail();
        }

        assertTrue( appenderInitialized.get() );
        assertTrue( appenderTakenDown.get() );

        assertTrue( onStartRan.get() );
        assertTrue( onFinishRan.get() );
        assertFalse( onFailureRan.get() );
    }

    @Test
    public void testSecurityContextManagement() {
        try {
            this.userManager.loadUserByUsername( "ExecutingTaskTestUser" );
        } catch ( UsernameNotFoundException e ) {
            this.userManager.createUser( new UserDetailsImpl( "foo", "ExecutingTaskTestUser", true, null,
                    "fooUser@chibi.ubc.ca", "key", new Date() ) );
        }

        runAsUser( "ExecutingTaskTestUser" );
        TaskCommand taskCommand = new TaskCommand();
        runAsAdmin();

        Task<TaskResult, TaskCommand> task = new SuccessTestTask();
        task.setTaskCommand( taskCommand );

        ExecutingTask<TaskResult> executingTask = new ExecutingTask<>( task, taskCommand );
        executingTask.setProgressAppender( progressAppender );
        executingTask.setStatusCallback( statusSecurityContextCheckerHandler );

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<TaskResult> future = executorService.submit( executingTask );
        try {
            TaskResult taskResult = future.get();
            String answer = ( String ) taskResult.getAnswer();
            assertEquals( "SUCCESS", answer );
        } catch ( InterruptedException e ) {
            fail();
        } catch ( ExecutionException e ) {
            fail();
        }

        assertEquals( taskCommand.getSecurityContext(), securityContextAfterInitialize );
        assertNotSame( taskCommand.getSecurityContext(), securityContextAfterFinish );
        assertEquals( null, securityContextAfterFail );
    }
}
