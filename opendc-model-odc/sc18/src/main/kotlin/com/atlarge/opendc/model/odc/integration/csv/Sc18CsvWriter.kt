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

package com.atlarge.opendc.model.odc.integration.csv

import com.atlarge.opendc.model.odc.integration.jpa.schema.JobMetrics
import com.atlarge.opendc.model.odc.integration.jpa.schema.StageMeasurement
import com.atlarge.opendc.model.odc.integration.jpa.schema.TaskMetrics
import com.opencsv.CSVWriter
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.cancelAndJoin
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.runBlocking
import java.io.Closeable
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.coroutines.experimental.CoroutineContext

/**
 * A class that allows users to export results of experiments to CSV files.
 *
 * @param basePath The path to the directory where to write the files.
 * @param coroutineContext The coroutine context in which we run the writer threads.
 * @param bufferSize The size of the buffer for the writer threads.
 */
class Sc18CsvWriter(basePath: Path, coroutineContext: CoroutineContext, bufferSize: Int = Channel.UNLIMITED) : Closeable {
    /**
     * This [Job] is the parent job of all writer jobs.
     */
    private val job = Job()

    /**
     * The [SendChannel] that consumes the [StageMeasurement]s.
     */
    val stageMeasurements: SendChannel<StageMeasurement> = actor(coroutineContext, capacity = bufferSize, parent = job) {
        val writer = CSVWriter(basePath.resolve(Paths.get("stage_measurements.csv")).toFile().bufferedWriter())

        // Write header
        writer.writeNext(arrayOf(
            null,
            "experiment",
            "trace",
            "scheduler",
            "stage",
            "tick",
            "cpu",
            "wall",
            "size",
            "iterations"
        ), false)

        var counter: Long = 0

        for (element in channel) {
            writer.writeNext(arrayOf(
                (counter++).toString(),
                element.experiment.id.toString(),
                element.experiment.trace.id.toString(),
                element.experiment.scheduler.name,
                element.stage.toString(),
                element.time.toString(),
                element.cpu.toString(),
                element.wall.toString(),
                element.size.toString(),
                element.iterations.toString()
            ), false)
        }

        writer.flush()
        writer.close()
    }

    /**
     * The [SendChannel] that consumes the [TaskMetrics] instances.
     */
    val taskMetrics: SendChannel<TaskMetrics> = actor(coroutineContext, capacity = bufferSize, parent = job) {
        val writer = CSVWriter(basePath.resolve(Paths.get("task_metrics.csv")).toFile().bufferedWriter())

        // Write header
        writer.writeNext(arrayOf(
            null,
            "experiment",
            "scheduler",
            "waiting",
            "execution",
            "turnaround",
            "job_id",
            "task_id"
        ), false)

        var counter: Long = 0

        for (element in channel) {
            writer.writeNext(arrayOf(
                (counter++).toString(),
                element.experiment.id.toString(),
                element.experiment.scheduler.name,
                element.waiting.toString(),
                element.execution.toString(),
                element.turnaround.toString(),
                element.job.id.toString(),
                element.task.id.toString()
            ), false)
        }

        writer.flush()
        writer.close()
    }

    /**
     * The [SendChannel] that consumes the [JobMetrics] instances.
     */
    val jobMetrics: SendChannel<JobMetrics> = actor(coroutineContext, capacity = bufferSize, parent = job) {
        val writer = CSVWriter(basePath.resolve(Paths.get("job_metrics.csv")).toFile().bufferedWriter())

        // Write header
        writer.writeNext(arrayOf(
            null,
            "experiment",
            "scheduler",
            "job_id",
            "critical_path",
            "critical_path_length",
            "waiting_time",
            "makespan",
            "nsl"
        ), false)

        var counter: Long = 0

        for (element in channel) {
            writer.writeNext(arrayOf(
                (counter++).toString(),
                element.experiment.id.toString(),
                element.experiment.scheduler.name,
                element.job.id.toString(),
                element.criticalPath.toString(),
                element.criticalPathLength.toString(),
                element.waiting.toString(),
                element.makespan.toString(),
                element.nsl.toString()
            ), false)
        }

        writer.flush()
        writer.close()
    }

    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * `try`-with-resources statement.
     *
     * @throws Exception if this resource cannot be closed
     */
    override fun close() = runBlocking {
        stageMeasurements.close()
        taskMetrics.close()
        jobMetrics.close()
        job.cancelAndJoin()
    }
}
