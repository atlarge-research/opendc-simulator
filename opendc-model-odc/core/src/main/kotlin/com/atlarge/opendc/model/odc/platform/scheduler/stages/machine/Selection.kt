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

package com.atlarge.opendc.model.odc.platform.scheduler.stages.machine

import com.atlarge.opendc.model.odc.OdcModel
import com.atlarge.opendc.model.odc.platform.scheduler.StageScheduler
import com.atlarge.opendc.model.odc.platform.workload.Task
import com.atlarge.opendc.model.odc.topology.machine.Cpu
import com.atlarge.opendc.model.odc.topology.machine.Machine
import com.atlarge.opendc.model.topology.destinations
import com.atlarge.opendc.simulator.context
import java.util.Random
import kotlin.math.abs

/**
 * This interface represents the **R5** stage of the Reference Architecture for Schedulers and matches the the selected
 * task with a (set of) resource(s), using policies such as First-Fit, Worst-Fit, and Best-Fit.
 */
interface MachineSelectionPolicy {
    /**
     * Select a machine on which the task should be scheduled.
     *
     * @param machines The list of machines in the system.
     * @param task The task that is to be scheduled.
     * @return The selected machine or `null` if no machine could be found.
     */
    suspend fun select(machines: List<Machine>, task: Task): Machine?
}

/**
 * A [MachineSelectionPolicy] that selects the first machine that is available.
 */
class FirstFitMachineSelectionPolicy : MachineSelectionPolicy {
    override suspend fun select(machines: List<Machine>, task: Task): Machine? = machines.firstOrNull()
}

/**
 * A [MachineSelectionPolicy] that selects the machine using a Best-Fit allocation algorithm: select the machine with
 * the smallest amount of available cores such that the given task can be scheduled on it.
 */
class BestFitMachineSelectionPolicy : MachineSelectionPolicy {
    override suspend fun select(machines: List<Machine>, task: Task): Machine? =
        context<StageScheduler.State, OdcModel>().run {
            machines
                .sortedBy { abs(task.cores - (state.machineCores[it] ?: 0)) }
                .firstOrNull()
        }
}

/**
 * A [MachineSelectionPolicy] that selects the machine using a Worst-Fit allocation algorithm: select the machine with
 * the largest amount of available cores such that the given task can be scheduled on it.
 */
class WorstFitMachineSelectionPolicy : MachineSelectionPolicy {
    override suspend fun select(machines: List<Machine>, task: Task): Machine? =
        context<StageScheduler.State, OdcModel>().run {
            machines
                .sortedByDescending { abs(task.cores - (state.machineCores[it] ?: 0)) }
                .firstOrNull()
        }
}

/**
 * A [MachineSelectionPolicy] that selects the machine randomly.
 *
 * @property random The [Random] instance used to pick the machine.
 */
class RandomMachineSelectionPolicy(private val random: Random = Random()) : MachineSelectionPolicy {
    override suspend fun select(machines: List<Machine>, task: Task): Machine? =
        if (machines.isNotEmpty())
            machines[random.nextInt(machines.size)]
        else
            null
}

/**
 * Heterogeneous Earliest Finish Time (HEFT) scheduling.
 *
 * https://en.wikipedia.org/wiki/Heterogeneous_Earliest_Finish_Time
 */
class HeftMachineSelectionPolicy : MachineSelectionPolicy {
    override suspend fun select(machines: List<Machine>, task: Task): Machine? =
        context<StageScheduler.State, OdcModel>().run {
            model.run {
                // NOTE: higher is better.
                fun communication(task: Task, machine: Machine): Double {
                    return machine.ethernetSpeed / task.inputSize
                }
                fun availableCompute(machine: Machine): Double {
                    val cpus = machine.outgoingEdges.destinations<Cpu>("cpu")
                    val cores = cpus.map { it.cores }.sum()
                    val speed = cpus.fold(0) { acc, cpu -> acc + cpu.clockRate * cpu.cores } / cores
                    return (1.0 - machine.state.load) * speed
                }

                machines.maxBy { machine -> communication(task, machine) + availableCompute(machine) }
            }
        }
}

/**
 * Critical-Path-on-a-Processor (CPOP) scheduling as described by H. Topcuoglu et al. in
 * "Task Scheduling Algorithms for Heterogeneous Processors".
 */
class CpopMachineSelectionPolicy : MachineSelectionPolicy {
    override suspend fun select(machines: List<Machine>, task: Task): Machine? =
        context<StageScheduler.State, OdcModel>().run {
            model.run {
                // NOTE: higher is better.
                fun communication(task: Task, machine: Machine): Double {
                    val ethernet_speeds = machine.outgoingEdges.destinations<Double>("ethernet_speed")
                    val ethernet_speed = ethernet_speeds.sum()
                    return ethernet_speed.toDouble() / task.inputSize
                }
                fun available_compute(machine: Machine): Double {
                    val cpus = machine.outgoingEdges.destinations<Cpu>("cpu")
                    val cores = cpus.map { it.cores }.sum()
                    val speed = cpus.fold(0) { acc, cpu -> acc + cpu.clockRate * cpu.cores } / cores
                    return (1.0 - machine.state.load) * speed
                }

                machines.maxBy { machine -> communication(task, machine) + available_compute(machine) }
            }
        }
}

/**
 * Round robin (RR) scheduling.
 *
 * https://en.wikipedia.org/wiki/Round-robin_scheduling
 */
class RrMachineSelectionPolicy(private var next: Int = 0) : MachineSelectionPolicy {
    override suspend fun select(machines: List<Machine>, task: Task): Machine? =
        context<StageScheduler.State, OdcModel>().run {
            model.run {
                // Try to find the machine with id `next`, if that machine can't
                // be found try `next + 1`, etc.
                val machines = machines.sortedBy { machine -> machine.id }
                val max_id: Int = machines.maxBy{ machine -> machine.id }?.id ?: 0
                while (machines.size > 0) {
                    val index = machines.binarySearchBy(next) { machine -> machine.id }
                    next += 1;
                    if (index != -1) {
                        return machines[index]
                    } else if (next > max_id) {
                        next = 0;
                    }
                }
                return null
            }
        }
}
