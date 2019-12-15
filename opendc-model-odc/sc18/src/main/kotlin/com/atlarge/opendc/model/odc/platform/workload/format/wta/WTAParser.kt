/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

package com.atlarge.opendc.model.odc.platform.workload.format.wta

import mu.KotlinLogging
import org.apache.avro.generic.GenericRecord
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.AvroParquetReader
import com.atlarge.opendc.model.odc.integration.jpa.schema.Job as InternalJob
import com.atlarge.opendc.model.odc.integration.jpa.schema.Task as InternalTask
import com.atlarge.opendc.model.odc.integration.jpa.schema.Trace as InternalTrace


class WTAParser {
    private val logger = KotlinLogging.logger {}

    /**
     * Parse the given [String] as path to a parquet directory or file.
     *
     * @param input The [String] to a directory or file.
     * @return The [Trace] that has been parsed.
     */
    fun parse(input: String): InternalTrace {
        val jobs = mutableMapOf<Int, InternalJob>()
        val tasks = mutableMapOf<Int, InternalTask>()
        val taskDependencies = mutableMapOf<InternalTask, List<Int>>()

        val reader = AvroParquetReader.builder<GenericRecord>(Path(input)).build()


        while (true) {
            val nextRecord = reader.read() ?: break

            val jobId = (nextRecord.get("workflow_id") as Long).toInt()
            val taskId = (nextRecord.get("id") as Long).toInt()
            val priority: Int = 0
            val submitTime = (nextRecord.get("ts_submit") as Long) / 1000
            val runtime = (nextRecord.get("runtime") as Long) / 1000
            val cores = (nextRecord.get("resource_amount_requested") as Double).toInt()
            val dependencies = (nextRecord.get("parents") as ArrayList<GenericRecord>).map {
                (it.get("item") as Long).toInt()
            }
            val inputSize: Long = 0
            val outputSize: Long = 0

            val flops: Long = 4100 * runtime * cores

            val task = InternalTask(taskId, jobId, priority, flops, cores, mutableSetOf(), mutableSetOf(),
                inputSize, outputSize, submitTime)
            val job = jobs.getOrPut(jobId) { InternalJob(jobId, mutableSetOf()) }

            job.tasks.add(task)
            tasks[taskId] = task
            taskDependencies[task] = dependencies
        }

        taskDependencies.forEach { task, dependencies ->
            task.dependencies.addAll(dependencies.map { taskId ->
                tasks[taskId] ?: throw IllegalArgumentException("Dependency task with id $taskId not found")
            })
            dependencies.forEach {
                tasks[it]!!.dependents.add(task)
            }
        }

        return InternalTrace(0, "WTA Trace", jobs.values.toList())
    }
}
