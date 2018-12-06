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

import com.atlarge.opendc.model.odc.OdcModel
import com.atlarge.opendc.model.odc.platform.scheduler.StageScheduler
import com.atlarge.opendc.model.odc.platform.workload.Task
import com.atlarge.opendc.model.odc.topology.machine.Cpu
import com.atlarge.opendc.model.odc.topology.machine.Machine
import com.atlarge.opendc.model.topology.Topology
import com.atlarge.opendc.model.topology.destinations
import com.atlarge.opendc.simulator.context
import java.util.Random

/**
 * This interface represents the **T2** stage of the Reference Architecture for Schedulers and provides the scheduler
 * with a sorted list of tasks to schedule.
 */
interface TaskSortingPolicy {
    /**
     * Sort the given list of tasks on a given criterion.
     *
     * @param tasks The list of tasks that should be sorted.
     * @return The sorted list of tasks.
     */
    suspend fun sort(tasks: List<Task>): List<Task>
}

/**
 * The [FifoSortingPolicy] sorts tasks based on the order of arrival in the queue.
 */
class FifoSortingPolicy: TaskSortingPolicy {
    override suspend fun sort(tasks: List<Task>): List<Task> = tasks
}

/**
 * The [SrtfSortingPolicy] sorts tasks based on the remaining duration (in runtime) of the task.
 */
class SrtfSortingPolicy : TaskSortingPolicy {
    override suspend fun sort(tasks: List<Task>): List<Task> = tasks.sortedBy { it.remaining }
}

/**
 * The [RandomSortingPolicy] sorts tasks randomly.
 *
 * @property random The [Random] instance to use when sorting the list of tasks.
 */
class RandomSortingPolicy(private val random: Random = Random()) : TaskSortingPolicy {
    override suspend fun sort(tasks: List<Task>): List<Task> = tasks.shuffled(random)
}

/**
 * Heterogeneous Earliest Finish Time (HEFT) scheduling.
 *
 * https://en.wikipedia.org/wiki/Heterogeneous_Earliest_Finish_Time
 */
class HeftSortingPolicy : TaskSortingPolicy {
    override suspend fun sort(tasks: List<Task>): List<Task> =
        context<StageScheduler.State, OdcModel>().run {
            model.run {
                val machines = state.machines;
                fun average_computation_cost(task: Task): Double {
                    return machines.sumByDouble { machine ->
                        val cpus = machine.outgoingEdges.destinations<Cpu>("cpu")
                        val cores = cpus.map { it.cores }.sum()
                        val speed = cpus.fold(0) { acc, cpu -> acc + cpu.clockRate * cpu.cores } / cores
                        (task.remaining / speed).toDouble()
                    } / machines.size
                }
                fun average_communication_cost(dependency: Task): Double {
                    // Here we assume that all the output of the dependency
                    // (parent) task is needed as input for the task.
                    return machines.sumByDouble { machine ->
                        val ethernet_speeds = machine.outgoingEdges.destinations<Double>("ethernet_speed")
                        val ethernet_speed = ethernet_speeds.sum()
                        (dependency.output_size / ethernet_speed).toDouble()
                    } / machines.size
                }
                // Upward rank of a `task`, as defined in the HEFT policy.
                fun upward_rank(task: Task): Double {
                    val avg_comp_cost = average_computation_cost(task)
                    val highest_dependent_cost = (task.dependents.map { dependent_task ->
                        average_communication_cost(dependent_task) + upward_rank(dependent_task)
                    }.max() ?: 0.0)
                    return avg_comp_cost + highest_dependent_cost
                }

                tasks.sortedByDescending { task -> upward_rank(task) }
            }
        }
}
