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
package ubic.gemma.core.job.executor.common;

import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.tasks.Task;

import java.util.concurrent.Callable;

/**
 * Task Lifecycle Hooks ProgressUpdateAppender -
 *
 * @author anton
 */
public class ExecutingTask<T extends TaskResult> implements Callable<T> {

    private Task<T, ?> task;
    // Does not survive serialization.
    private transient TaskLifecycleHandler statusCallback;
    private transient ProgressUpdateAppender progressAppender;
    private String taskId;
    private TaskCommand taskCommand;
    private Throwable taskExecutionException;

    public ExecutingTask( Task<T, ?> task, TaskCommand taskCommand ) {
        this.task = task;
        this.taskId = taskCommand.getTaskId();
        this.taskCommand = taskCommand;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final T call() throws Exception {
        setup();
        // From here we are running as user who submitted the task.

        statusCallback.onStart();

        T result = null;
        try {
            result = this.task.execute();
        } catch ( Throwable e ) {
            statusCallback.onFailure( e );
            taskExecutionException = e;
        } finally {
            cleanup();
        }
        // SecurityContext is cleared at this point.

        if ( taskExecutionException == null ) {
            statusCallback.onFinish();
            return result;
        }
        result = ( T ) new TaskResult( taskId );
        result.setException( taskExecutionException );
        return result;

    }

    public void setProgressAppender( ProgressUpdateAppender progressAppender ) {
        this.progressAppender = progressAppender;
    }

    public void setStatusCallback( TaskLifecycleHandler statusCallback ) {
        this.statusCallback = statusCallback;
    }

    private void cleanup() {
        SecurityContextHolder.clearContext();

        progressAppender.tearDown();
    }

    private void setup() {
        progressAppender.initialize();

        SecurityContextHolder.setContext( taskCommand.getSecurityContext() ); // TODO: one idea is to have
        // SecurityContextAwareExecutorClass.
    }

    // These hooks are used to update status of the running task.
    public interface TaskLifecycleHandler {
        public void onFailure( Throwable e );

        public void onFinish();

        public void onStart();
    }
}