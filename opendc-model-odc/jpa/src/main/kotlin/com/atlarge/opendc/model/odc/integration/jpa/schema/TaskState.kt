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

package com.atlarge.opendc.model.odc.integration.jpa.schema

import com.atlarge.opendc.simulator.Instant
import com.atlarge.opendc.simulator.instrumentation.lerp
import javax.persistence.Entity

/**
 * The state of a [Task].
 *
 * @property id The unique identifier of the state.
 * @property task The task this is the state of.
 * @property experiment The experiment the machine is running in.
 * @property time The current moment in time.
 * @property remaining The remaining flops for the task.
 * @property cores The cores used for the task.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
@Entity
data class TaskState(
    val id: Int?,
    val task: Task,
    val experiment: Experiment,
    val time: Instant,
    val remaining: Int,
    val cores: Int
) {
    companion object {
        /**
         * A linear interpolator for [TaskState] instances.
         */
        val Interpolator: (Double, TaskState, TaskState) -> TaskState = { f, a, b ->
            a.copy(
                id = null,
                time = lerp(a.time, b.time, f),
                remaining = lerp(a.remaining, b.remaining, f)
            )
        }
    }
}


