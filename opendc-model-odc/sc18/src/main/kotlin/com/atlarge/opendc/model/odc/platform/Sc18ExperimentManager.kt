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

package com.atlarge.opendc.model.odc.platform

import com.atlarge.opendc.model.odc.integration.csv.Sc18CsvWriter
import com.atlarge.opendc.model.odc.integration.jpa.converter.SchedulerConverter
import com.atlarge.opendc.model.odc.integration.jpa.schema.Experiment
import com.atlarge.opendc.model.odc.integration.jpa.schema.ExperimentState
import com.atlarge.opendc.model.odc.integration.jpa.schema.Path
import com.atlarge.opendc.model.odc.integration.jpa.schema.Simulation
import com.atlarge.opendc.model.odc.integration.jpa.schema.Trace
import java.io.Closeable
import java.time.LocalDateTime
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import kotlin.coroutines.experimental.buildSequence

/**
 * This class manages the experiments that are run for the SC18 paper: A Reference Architecture for Datacenter
 * Scheduling.
 *
 * The amount experiments generated is based on the schedulers to test, the amount of repeats per scheduler and the
 * amount of warm-up passes.
 *
 * @property factory The [EntityManagerFactory] in which the data of the experiments is stored.
 * @property schedulers The schedulers to simulate.
 * @property writer The csv writer to use.
 * @property trace The trace to use represented by its identifier.
 * @property path The path to use represented by its identifier.
 * @property repeat The amount of times to repeat a simulation for a scheduler.
 * @property warmUp The amount of warm up passes.
 */
class Sc18ExperimentManager(
    private val factory: EntityManagerFactory,
    private val writer: Sc18CsvWriter,
    private val schedulers: List<String>,
    private val trace: Int,
    private val path: Int,
    private val repeat: Int,
    private val warmUp: Int
) : Closeable {
    /**
     * The internal entity manager for persisting the experiments.
     */
    private val manager: EntityManager = factory.createEntityManager()

    /**
     * The [SchedulerConverter] to use.
     */
    private val schedulerConverter = SchedulerConverter()

    /**
     * A [Sequence] consisting of the generated experiments.
     */
    val experiments: Sequence<Sc18Experiment> = buildSequence {
        for (scheduler in schedulers) {
            val range = 0 until repeat + warmUp
            yieldAll(range.map { buildExperiment(scheduler, it) })
        }
    }

    /**
     * Build an [Sc18Experiment] for the given scheduler identified by string.
     *
     * @param schedulerName The scheduler to create an experiment for.
     * @param i The index of the experiment.
     */
    private fun buildExperiment(schedulerName: String, i: Int): Sc18Experiment {
        val isWarmUp = i < warmUp
        val name =  if (isWarmUp) "WARM-UP" else "EXPERIMENT"
        val scheduler = schedulerConverter.convertToEntityAttribute(schedulerName)

        val internalManager = factory.createEntityManager()
        val simulation = Simulation(null, "Simulation", LocalDateTime.now(), LocalDateTime.now())
        val experiment = Experiment(null, name, scheduler, simulation, loadTrace(internalManager), loadPath(internalManager)).apply {
            state = ExperimentState.CLAIMED
        }

        manager.transaction.begin()
        manager.persist(simulation)
        manager.persist(experiment)
        manager.transaction.commit()

        return Sc18Experiment(internalManager, writer, experiment, isWarmUp)
    }

    /**
     * Load the [Trace] from the database.
     */
    private fun loadTrace(entityManager: EntityManager): Trace = entityManager
        .createQuery("SELECT t FROM com.atlarge.opendc.model.odc.integration.jpa.schema.Trace t WHERE t.id = :id", Trace::class.java)
        .setParameter("id", trace)
        .singleResult

    /**
     * Load the [Path] from the database.
     */
    private fun loadPath(entityManager: EntityManager): Path = entityManager
        .createQuery("SELECT p FROM com.atlarge.opendc.model.odc.integration.jpa.schema.Path p WHERE p.id = :id", Path::class.java)
        .setParameter("id", path)
        .singleResult

    /**
     * Close this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * `try`-with-resources statement.*
     *
     * @throws Exception if this resource cannot be closed
     */
    override fun close() = manager.close()
}
