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
import com.atlarge.opendc.model.odc.topology.machine.Machine
import com.atlarge.opendc.simulator.context

/**
 * This interface represents the **R4** stage of the Reference Architecture for Schedulers and acts as a filter yielding
 * a list of resources with sufficient resource-capacities, based on fixed or dynamic requirements, and on predicted or
 * monitored information about processing unit availability, memory occupancy, etc.
 */
interface MachineDynamicFilteringPolicy {
    /**
     * Filter the list of machines based on dynamic information.
     *
     * @param machines The list of machines in the system.
     * @param task The task that is to be scheduled.
     * @return The machines on which the task can be scheduled.
     */
    suspend fun filter(machines: Set<Machine>, task: Task): List<Machine>
}

/**
 * A [MachineDynamicFilteringPolicy] based on the amount of cores available on the machine and the cores required for
 * the task.
 */
class FunctionalMachineDynamicFilteringPolicy : MachineDynamicFilteringPolicy {
    override suspend fun filter(machines: Set<Machine>, task: Task): List<Machine> =
        context<StageScheduler.State, OdcModel>().run {
            machines
                .filter { state.machineCores[it] ?: 0 >= task.cores }
        }
}
