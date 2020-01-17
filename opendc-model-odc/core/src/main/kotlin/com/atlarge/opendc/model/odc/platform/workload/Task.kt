/*
 * MIT License
 *
 * Copyright (c) 2017 atlarge-research
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

package com.atlarge.opendc.model.odc.platform.workload

import com.atlarge.opendc.model.odc.topology.machine.Machine
import com.atlarge.opendc.simulator.Instant

/**
 * A task that runs as part of a [Job] on a [Machine].
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Task {
    /**
     * The unique identifier of the task.
     */
    val id: Int

    /**
     * The ID of the owner of the task
     */
    val owner_id: Int
   
    /**
     * The priority of the task.
     */
    val priority: Int

    /**
     * The amount of flops for this task.
     */
    val flops: Long

    /**
     * The dependencies of the task.
     */
    val dependencies: Set<Task>

    /**
     * Set of tasks that are dependent on this task.
     */
    val dependents: Set<Task>

    /**
     * The amount of cores required for running the task.
     */
    val cores: Int

    /**
     * The remaining flops for this task.
     */
    val remaining: Long

    /**
     * The input size, i.e. required input data.
     */
    val inputSize: Long

    /**
     * The output size.
     */
    val outputSize: Long

    /**
     * The state of the task.
     */
    val state: TaskState

    /**
     * A flag to indicate whether the task is ready to be started.
     */
    val ready: Boolean
        get() = dependencies.all { it.finished }

    /**
     * A flag to indicate whether the task has finished.
     */
    val finished: Boolean
        get() = state is TaskState.Finished
     
    /**
     * This method is invoked when a task has arrived at a datacenter.
     *
     * @param time The moment in time the task has arrived at the datacenter.
     */
    fun arrive(time: Instant)

    /**
     * Consume the given amount of flops of this task.
     *
     * @param time The current moment in time of the consumption.
     * @param flops The total amount of flops to consume.
     */
    fun consume(time: Instant, flops: Long)
}

