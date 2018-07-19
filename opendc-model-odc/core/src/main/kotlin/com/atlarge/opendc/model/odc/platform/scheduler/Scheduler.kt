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

package com.atlarge.opendc.model.odc.platform.scheduler

import com.atlarge.opendc.model.odc.OdcModel
import com.atlarge.opendc.model.odc.platform.workload.Task
import com.atlarge.opendc.model.odc.topology.machine.Machine
import com.atlarge.opendc.simulator.Process

/**
 * A cloud scheduler interface that schedules tasks across machines.
 *
 * @param S The shape of the state of the scheduler.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Scheduler<S> : Process<S, OdcModel> {
    /**
     * The name of this scheduler.
     */
    val name: String

    /**
     * This message is sent to a scheduler to indicate a scheduling cycle.
     *
     * @property tasks The new tasks that should be added to the queue.
     */
    data class Schedule(val tasks: Set<Task>)

    /**
     * This message is sent to a scheduler to introduce new resources and release old resources.
     *
     * @property registered The new machines that have been registered to the datacenter.
     * @property unregistered The machines that have been unregistered.
     */
    data class Resources(val registered: Set<Machine>, val unregistered: Set<Machine>)
}

