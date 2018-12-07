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

package com.atlarge.opendc.model.odc.platform.workload.format.sgwf

import java.io.InputStream

import com.atlarge.opendc.model.odc.integration.jpa.schema.Job as InternalJob
import com.atlarge.opendc.model.odc.integration.jpa.schema.Task as InternalTask
import com.atlarge.opendc.model.odc.integration.jpa.schema.Trace as InternalTrace

/**
 * A parser for a simplified version of the Grid Workload Format. See the
 * Grid Workloads Archive (http://gwa.ewi.tudelft.nl/) for more information about the format.
 *
 * TODO: Describe simplified format
 */
class SgwfParser {
    /**
     * Parse the given [InputStream] as Simplified Grid Workload Format file. After the method returns, the input stream
     * is closed.
     *
     * @param input The [InputStream] to parse as Simplified Grid Workload Format file.
     * @return The [Trace] that has been parsed.
     */
    fun parse(input: InputStream): InternalTrace {
        val jobs = mutableMapOf<Int, InternalJob>()
        val tasks = mutableMapOf<Int, InternalTask>()
        val taskDependencies = mutableMapOf<InternalTask, List<Int>>()

        input.bufferedReader().use { reader ->
            reader
                .lineSequence()
                .drop(1) // drop the header
                .filter { line ->
                    !line.startsWith("#") && line.isNotBlank()
                }
                .forEach { line ->
                    val values = line.split(",")

                    if (values.size < 7) {
                        throw IllegalArgumentException("The line is malformed: $line")
                    }

                    val jobId = values[0].trim().toInt()
                    val taskId = values[1].trim().toInt()
                    val priority: Int = 0
                    val submitTime = values[2].trim().toLong()
                    val runtime = values[3].trim().toLong()
                    val cores = values[4].trim().toInt()
                    val dependencies = values[6].split(" ")
                        .filter { it.isNotEmpty() }
                        .map { it.trim().toInt() }
                    val inputSize: Long = 0
                    val outputSize: Long = 0

                    val flops: Long = 4000 * runtime * cores

                    val task = InternalTask(taskId, priority, flops, cores, mutableSetOf(), mutableSetOf(),
                        inputSize, outputSize, submitTime)
                    val job = jobs.getOrPut(jobId) { InternalJob(jobId, mutableSetOf()) }

                    job.tasks.add(task)
                    tasks[taskId] = task
                    taskDependencies[task] = dependencies
                }
        }

        taskDependencies.forEach { task, dependencies ->
            task.dependencies.addAll(dependencies.map { taskId ->
                tasks[taskId] ?: throw IllegalArgumentException("Dependency task with id $taskId not found")
            })
            dependencies.forEach {
                tasks[it]!!.dependents.add(task)
            }
        }

        return InternalTrace(0, "SGWF Trace", jobs.values.toList())
    }
}
