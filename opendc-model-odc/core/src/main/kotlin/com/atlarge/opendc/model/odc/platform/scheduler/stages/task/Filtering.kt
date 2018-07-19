/*
 * MIT License
 *
 * Copyright (c) 2018 atlarge-research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.atlarge.opendc.model.odc.platform.scheduler.stages.task

import com.atlarge.opendc.model.odc.platform.workload.Task

/**
 * This interface represents the **T1** stage of the Reference Architecture for Schedulers and provides the scheduler
 * with a list of eligible tasks to be scheduled.
 */
interface TaskEligibilityFilteringPolicy {
    /**
     * Filter the list of tasks provided as input, based on a filter-policy, e.g. a policy that allows
     * tasks to pass through only if their dependencies have already finished.
     *
     * @param queue The list of tasks that are ready to be scheduled.
     * @return The tasks that are allowed to be scheduled.
     */
    suspend fun filter(queue: Set<Task>): List<Task>
}

/**
 * The [FunctionalTaskEligibilityFilteringPolicy] filters tasks based on whether their dependencies have finished running.
 */
class FunctionalTaskEligibilityFilteringPolicy : TaskEligibilityFilteringPolicy {
    override suspend fun filter(queue: Set<Task>): List<Task> = queue.filter { it.ready }
}
