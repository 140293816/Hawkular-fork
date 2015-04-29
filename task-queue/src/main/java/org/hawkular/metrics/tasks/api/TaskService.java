/*
 * Copyright 2014-2015 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.metrics.tasks.api;

import com.google.common.util.concurrent.ListenableFuture;
import org.joda.time.DateTime;

/**
 * @author jsanda
 */
public interface TaskService {
    /**
     * Starts the scheduler. Task execution can be scheduled every second or every minute. Task execution for a
     * particular time slice will run at the end of said time slice or later but never sooner.
     *
     * TODO log warning if scheduling falls behind
     */
    void start();

    void shutdown();

    ListenableFuture<Task> scheduleTask(DateTime time, Task task);
}