package com.atlarge.opendc.model.odc.platform

import com.atlarge.opendc.model.odc.JpaBootstrap
import com.atlarge.opendc.model.odc.integration.csv.Sc18CsvWriter
import com.atlarge.opendc.model.odc.integration.jpa.schema.ExperimentState
import com.atlarge.opendc.model.odc.integration.jpa.schema.Job
import com.atlarge.opendc.model.odc.integration.jpa.schema.JobMetrics
import com.atlarge.opendc.model.odc.integration.jpa.schema.TaskMetrics
import com.atlarge.opendc.model.odc.integration.jpa.transaction
import com.atlarge.opendc.model.odc.platform.scheduler.stages.StageMeasurement
import com.atlarge.opendc.model.odc.platform.workload.Task
import com.atlarge.opendc.model.odc.platform.workload.TaskState
import com.atlarge.opendc.model.odc.platform.workload.toposort
import com.atlarge.opendc.simulator.Duration
import com.atlarge.opendc.simulator.Instant
import com.atlarge.opendc.simulator.kernel.Kernel
import com.atlarge.opendc.simulator.platform.Experiment
import com.atlarge.opendc.simulator.util.instrument
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.asReceiveChannel
import kotlinx.coroutines.experimental.channels.filter
import kotlinx.coroutines.experimental.channels.map
import kotlinx.coroutines.experimental.channels.toChannel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import java.io.Closeable
import javax.persistence.EntityManager
import kotlin.coroutines.experimental.coroutineContext
import kotlin.math.max
import kotlin.system.measureTimeMillis
import com.atlarge.opendc.model.odc.integration.jpa.schema.Experiment as InternalExperiment
import com.atlarge.opendc.model.odc.integration.jpa.schema.Job as InternalJob
import com.atlarge.opendc.model.odc.integration.jpa.schema.StageMeasurement as InternalStageMeasurement
import com.atlarge.opendc.model.odc.integration.jpa.schema.Task as InternalTask
import com.atlarge.opendc.model.odc.integration.jpa.schema.TaskState as InternalTaskState
import com.atlarge.opendc.model.odc.integration.jpa.schema.Trace as InternalTrace

/**
 * This class runs the experiments as part of the paper A Reference Architecture for Datacenter Scheduling.
 *
 * @property manager The [EntityManager] to manage the experiment in the database.
 * @property writer The writer to export the results to files.
 * @property experiment The [InternalExperiment] in the database to run.
 * @property warmUp A flag to indicate this is a warm-up experiment.
 */
class Sc18Experiment(private val manager: EntityManager,
                     private val writer: Sc18CsvWriter,
                     private val experiment: InternalExperiment,
                     private val warmUp: Boolean) : Experiment<Unit>, Closeable {
    /**
     * The logging instance.
     */
    private val logger = KotlinLogging.logger {}


    /**
     * Run the experiment using the specified simulation kernel implementation.
     *
     * @param factory The simulation kernel implementation to use.
     * @param timeout The maximum duration of the experiment before returning to the caller.
     * @return The result of the experiment or `null`.
     */
    override fun run(factory: Kernel, timeout: Duration): Unit? {
        if (experiment.state != ExperimentState.CLAIMED) {
            throw IllegalStateException("The experiment is in illegal state ${experiment.state}")
        }

        logger.info { "Initialising experiment ${experiment.id}" }

        // Set the simulation state
        manager.transaction {
            experiment.state = ExperimentState.SIMULATING
        }

        val bootstrap = JpaBootstrap(experiment)
        val simulation = factory.create(bootstrap)

        val trace = experiment.trace
        val tasks = trace.jobs.flatMap { it.tasks }

        // The port we use to install the instruments
        val port = simulation.openPort()

        // Capture the stage measurements of the scheduler
        val stages = if (!warmUp) {
            port.install(Channel.UNLIMITED, experiment.scheduler.bus.instrument())
                .filter { it is StageMeasurement }
                .map {
                    it as StageMeasurement
                    InternalStageMeasurement(null, experiment, it.stage, it.time, it.cpu, it.wall, it.size, it.iterations)
                }
        } else {
            emptyList<InternalStageMeasurement>().asReceiveChannel()
        }
        val stageThread = launch { stages.toChannel(writer.stageMeasurements) }

        // A method to flush the remaining measurements to the database
        fun finalize() = runBlocking {
            logger.info { "Flushing remaining measurements to database" }

            // Stop gathering new measurements
            port.close()

            // Wait for the stages to be written
            stageThread.join()
        }

        logger.info { "Starting simulation" }
        logger.info { "Scheduling total of ${trace.jobs.size} jobs and ${tasks.size} tasks" }

        val measurement = measureTimeMillis {
            while (true) {
                // Have all jobs finished yet
                if (trace.jobs.all { it.finished })
                    break

                // If we have reached a timeout, return
                if (simulation.time >= timeout) {
                    // Flush remaining data
                    finalize()

                    // Mark the experiment as aborted
                    manager.transaction {
                        experiment.last = simulation.time
                        experiment.state = ExperimentState.ABORTED
                    }

                    logger.warn { "Experiment aborted due to timeout" }
                    return null
                }

                try {
                    // Run next simulation cycle
                    simulation.step()
                } catch (e: Throwable) {
                    logger.error(e) { "An error occurred during execution of the experiment" }
                }
            }
        }

        logger.info { "Simulation done in $measurement milliseconds" }

        // Flush remaining data to database
        finalize()

        // Collect metrics of tasks like start time, execution time and finish time
        runBlocking {
            if (warmUp) {
                return@runBlocking
            }

            launch(Unconfined, parent = coroutineContext[kotlinx.coroutines.experimental.Job]) {
                trace.jobs
                    .flatMap { job ->
                        job.tasks.map { task ->
                            val finished = task.state as com.atlarge.opendc.model.odc.platform.workload.TaskState.Finished
                            TaskMetrics(
                                null,
                                experiment,
                                task as com.atlarge.opendc.model.odc.integration.jpa.schema.Task,
                                job as Job,
                                finished.waitingTime,
                                finished.executionTime,
                                finished.finishTime - finished.submitTime
                            )
                        }
                    }
                    .asReceiveChannel()
                    .toChannel(writer.taskMetrics)
            }


            trace.jobs
                .map { job ->
                    val criticalPath = job.toposort()
                    val finishes = mutableMapOf<Task, Instant>()
                    val length = mutableMapOf<Task, Int>()

                    for (task in criticalPath) {
                        val state = task.state as TaskState.Finished
                        val parent = task.dependencies.maxBy {
                            val finished = task.state as TaskState.Finished
                            finished.finishTime
                        }
                        val parentState = parent?.state as? TaskState.Finished

                        finishes[task] = max(parentState?.finishTime ?: 0, state.startTime) + state.executionTime
                        length[task] = (parent?.let { length[it] } ?: 0) + 1
                    }

                    val (cpl, count) = let { _ ->
                        val max = finishes.maxBy { it.value }
                        val count = max?.let { length[it.key] } ?: 0
                        val min = job.tasks.map { (it.state as TaskState.Finished).startTime }.min()
                        Pair(kotlin.math.max(1, (max?.value ?: 0) - (min ?: 0)), count)
                    }

                    val (makespan, waiting) = let { _ ->
                        val submit = job.tasks.map { (it.state as TaskState.Finished).submitTime }.min() ?: 0
                        val start = job.tasks.map { (it.state as TaskState.Finished).startTime }.min() ?: 0
                        val finish = job.tasks.map { (it.state as TaskState.Finished).finishTime }.max() ?: 0
                        Pair(finish - submit, start - submit)
                    }

                    JobMetrics(null, experiment, job as Job, cpl, count, waiting, makespan, makespan / cpl)
                }
                .asReceiveChannel()
                .toChannel(writer.jobMetrics)
        }

        // Mark experiment as finished
        manager.transaction {
            experiment.last = simulation.time
            experiment.state = ExperimentState.FINISHED
        }

        return Unit
    }

    /**
     * Run the experiment on the specified simulation kernel implementation.
     *
     * @param factory The factory to create the simulation kernel with.
     * @throws IllegalStateException if the simulation is already running or finished.
     */
    override fun run(factory: Kernel) = run(factory, -1)!!

    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * `try`-with-resources statement.
     *
     * @throws Exception if this resource cannot be closed
     */
    override fun close() = manager.close()
}
