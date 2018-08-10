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

package com.atlarge.opendc.model.odc.platform.scheduler.stages

import com.atlarge.opendc.simulator.Instant
import java.lang.management.ManagementFactory

/**
 * The measurements related to a single stage in the scheduler.
 *
 * @property stage The identifier of the stage.
 * @property time The point in time at which the measurement occurred.
 * @property cpu The duration in cpu time (ns) of the stage.
 * @property wall The duration in wall time (ns) of the stage.
 * @property size The total size of the input of the stage.
 * @property iterations The amount of iterations in the stage.
 */
data class StageMeasurement(val stage: Int,
                            val time: Instant,
                            val cpu: Long,
                            val wall: Long,
                            val size: Int,
                            val iterations: Int)

/**
 * A class that accumulates and manages the measurements of the stages.
 *
 * @property time The point in simulation time at which the measurements occur.
 * @property size The input size of the scheduler.
 */
class StageMeasurementAccumulator(val time: Instant, val size: Int) {
    /**
     * A collection of measurements that have been collected during the runtime of the continuation.
     */
    val measurements: MutableList<StageMeasurement> = mutableListOf()

    /**
     * The MXBean to measure cpu time.
     */
    val bean = ManagementFactory.getThreadMXBean()

    /**
     * Measure the initial cpu time
     */
    private var cpuStart = -1L

    /**
     * Measure the initial wall time.
     */
    private var wallStart = -1L

    /**
     * Start the accumulation of measurements.
     */
    fun start() {
        measurements.clear()
        cpuStart = bean.currentThreadUserTime
        wallStart = System.nanoTime()
    }

    /**
     * End the accumulation of measurements.
     */
    fun end() {
        val cpu = bean.currentThreadUserTime - cpuStart - measurements.map { it.cpu }.sum()
        val wall = System.nanoTime() - wallStart  - measurements.map { it.wall }.sum()
        val measurement = StageMeasurement(measurements.size + 1, time, cpu, wall, size, 1)
        measurements.add(measurement)
    }

    /**
     * Measure the duration of a stage.
     *
     * @param stage The identifier of the stage.
     * @param input The size of the input.
     * @param block The block to measure.
     */
    inline fun <R> runStage(stage: Int, input: Int, block: () -> R): R {
        val cpuStart = bean.currentThreadUserTime
        val wallStart = System.nanoTime()

        val res = block()

        val cpu = bean.currentThreadUserTime - cpuStart
        val wall = System.nanoTime() - wallStart

        val previous = if (stage - 1 < measurements.size) measurements[stage - 1] else null
        if (previous != null) {
            measurements[stage - 1] = StageMeasurement(stage, time, cpu + previous.cpu, wall + previous.wall, input + previous.size, previous.iterations + 1)
        } else {
            measurements.add(StageMeasurement(stage, time, cpu, wall, input, 1))
        }

        return res
    }
}
