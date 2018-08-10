package com.atlarge.opendc.simulator.util

import com.atlarge.opendc.omega.OmegaKernel
import com.atlarge.opendc.simulator.Bootstrap
import com.atlarge.opendc.simulator.Context
import com.atlarge.opendc.simulator.Process
import com.atlarge.opendc.simulator.kernel.Simulation
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.channels.first
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.coroutines.experimental.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Test cases for the [EventBus] component.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
internal class EventBusTest {
    @Test
    fun `subscribe-publish`() {
        val bus = EventBus()
        val test = object : Process<Unit, Unit> {
            override val initialState = Unit

            override suspend fun Context<Unit, Unit>.run() {
                // Subscribe to event bus
                bus.subscribe()

                // Get first event
                assertEquals(receive(5)!!, 1)
            }
        }

        val simulation: Simulation<Unit> = OmegaKernel.create(Bootstrap.create {
            it.start(bus)
            it.start(test)
            it.schedule(EventBus.Publish(1), bus, delay = 2)
        })

        simulation.run(10)
    }

    @Test
    fun `subscribe-unsubscribe-publish`() {
        val bus = EventBus()
        val test = object : Process<Unit, Unit> {
            override val initialState = Unit

            override suspend fun Context<Unit, Unit>.run() {
                // Subscribe to event bus
                bus.subscribe()

                // Unsubscribe from the bus
                bus.unsubscribe()

                // Get first event
                assertNull(receive(5))
            }
        }

        val simulation: Simulation<Unit> = OmegaKernel.create(Bootstrap.create {
            it.start(bus)
            it.start(test)
            it.schedule(EventBus.Publish(1), bus, delay = 2)
        })

        simulation.run(10)
    }

    @Test
    fun subscribe_another_process() {
        val bus = EventBus()
        val testA = object : Process<Unit, Unit> {
            override val initialState = Unit

            override suspend fun Context<Unit, Unit>.run() {
                assertEquals(receive(5)!!, 2)
            }
        }

        val testB = object : Process<Unit, Unit> {
            override val initialState = Unit

            override suspend fun Context<Unit, Unit>.run() {
                bus.send(EventBus.Subscribe, sender = testA)
            }
        }

        val simulation: Simulation<Unit> = OmegaKernel.create(Bootstrap.create {
            it.start(bus)
            it.start(testA)
            it.start(testB)
            it.schedule(EventBus.Publish(2), bus, delay = 2)
        })

        simulation.run(10)
    }

    @Test
    fun `event-bus-instrument`() {
        val bus = EventBus()

        val simulation: Simulation<Unit> = OmegaKernel.create(Bootstrap.create {
            it.start(bus)
            it.schedule(EventBus.Publish(1), bus, delay = 2)
        })

        val events = simulation.openPort().install(bus.instrument())

        val job = launch(Unconfined) {
            assertEquals(1, events.first())
        }

        simulation.run(10)

        runBlocking {
            withTimeout(100) {
                job.join()
            }
        }
    }

}
