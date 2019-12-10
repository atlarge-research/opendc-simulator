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
import java.util.NavigableMap
import java.util.Random
import java.util.TreeMap
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
                    return machine.ethernetSpeed.toDouble() / task.inputSize
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
 * Round robin (RR) scheduling.
 *
 * https://en.wikipedia.org/wiki/Round-robin_scheduling
 */
class RrMachineSelectionPolicy(private var current: Int = 0) : MachineSelectionPolicy {
    override suspend fun select(machines: List<Machine>, task: Task): Machine? =
        context<StageScheduler.State, OdcModel>().run {
            model.run {
                if (machines.isEmpty()) {
                    return null
                }
                val ids: NavigableMap<Int, Machine> = TreeMap(machines.associateBy { it.id })
                current = ids.higherKey(current) ?: ids.firstKey()
                return ids[current]
            }
        }
}

/**
 * Lottery Scheduling
 *
 * https://en.wikipedia.org/wiki/Lottery_scheduling
 */
class LotteryMachineSelectionPolicy(private var defaultTickets: Int = 100, 
                                    private var distributionMap: Map<Machine, Int> = mutableMapOf(), 
                                    private val random: Random = Random()) : MachineSelectionPolicy {
    /**
     * A map where the key number maps to the machine
     * that owns the range of tickets that
     * start at that number and end at the next
     * index
     */
    val ticketMap: MutableMap<Int, Machine> = mutableMapOf()
    val knownMachines: MutableSet<Machine> = mutableSetOf()
    var totalTickets = 0

    fun pushMachine(machine: Machine, tickets: Int = defaultTickets) {
        ticketMap.set(totalTickets, machine)
        totalTickets += tickets
        knownMachines.add(machine)
    }

    init {
        // Fill the map based on the already known values
        for ((machine, tickets) in distributionMap) {
            pushMachine(machine, tickets)
        }
    }

    fun ensureMachineChances(machines: List<Machine>) {
        for (machine in machines) {
            if (!knownMachines.contains(machine)) {
                // Not known yet, add it to this list
                // and assign it a ticket range
                pushMachine(machine)
            }
        }
    }

    fun findWinner(number: Int, startRange: Int = 0, endRange: Int = ticketMap.size): Machine? {
        // Get half of the passed range
        val halfRange: Int = startRange + ((endRange - startRange) / 2)
        
        // Get the start value there
        val startValue = ticketMap.keys.elementAt(halfRange)
        if (startValue > number) {
            // Search to the left of this
            return findWinner(number, startRange, halfRange)
        } else if (startValue == number || halfRange == ticketMap.size - 1 || number < ticketMap.keys.elementAt(halfRange + 1)) {
            // We found it
            return ticketMap.get(startValue)
        } else {
            // Search to the right of this
            return findWinner(number, halfRange, endRange)
        }   
    }

    override suspend fun select(machines: List<Machine>, task: Task): Machine? =
        context<StageScheduler.State, OdcModel>().run {
            model.run {
                if (machines.isEmpty()) {
                    return null
                }

                // Make sure all machines are actually in the ticket map
                ensureMachineChances(machines)

                var winner: Machine?;
                do {
                    // Draw a ticket
                    val ticket: Int = random.nextInt(totalTickets)

                    // Find the winner
                    winner = findWinner(ticket)

                    // Check if the winner is present
                } while (winner == null || !(winner in machines))

                return winner
            }
        }
}
