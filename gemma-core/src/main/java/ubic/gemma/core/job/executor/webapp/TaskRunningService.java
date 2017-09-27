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
package ubic.gemma.core.job.executor.webapp;

import ubic.gemma.core.job.ConflictingTaskException;
import ubic.gemma.core.job.SubmittedTask;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.tasks.Task;

import java.util.Collection;

/**
 * @author paul, anton
 */
public interface TaskRunningService {

    SubmittedTask getSubmittedTask( String taskId );

    // TODO: Make this user specific. Probably in a controller with a session scoped collection.
    // TODO: at that level (WebAwareTaskRunningService) have a rate limiter for task submission by the same user
    // TODO: this is to detect duplicate task submission (e.g. within a few seconds)

    /**
     * @return the submittedTasks
     */
    Collection<SubmittedTask<? extends TaskResult>> getSubmittedTasks();

    /**
     * Submit a task and track its progress. When it is finished, the results can be retrieved with checkResult(). Tasks
     * can be cancelled with cancelTask().
     *
     * @param taskCommand The command to run. The submissionTime of the task is set after this call. This does not mean
     *                    that the job has started - it might be queued.
     * @throws ubic.gemma.core.job.ConflictingTaskException if the task is disallowed due to another conflicting task (e.g.,
     *                                                      two tasks of the same type by the same user).
     */
    <C extends TaskCommand> String submitLocalTask( C taskCommand ) throws ConflictingTaskException;

    <T extends Task> String submitLocalTask( T task ) throws ConflictingTaskException;

    /**
     * Run task remotely if possible, otherwise run locally.
     *
     * @param taskCommand task command
     * @return string
     * @throws ConflictingTaskException conflicting task problem
     */
    <C extends TaskCommand> String submitRemoteTask( C taskCommand ) throws ConflictingTaskException;
}