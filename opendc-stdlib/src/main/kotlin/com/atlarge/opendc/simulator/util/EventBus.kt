package com.atlarge.opendc.simulator.util

import com.atlarge.opendc.simulator.Context
import com.atlarge.opendc.simulator.Entity
import com.atlarge.opendc.simulator.Process
import com.atlarge.opendc.simulator.instrumentation.Instrument
import com.atlarge.opendc.simulator.untypedContext

/**
 * A mechanism for publish-subscribe-style communication between entities without requiring the entities to
 * explicitly register with one another (and thus be aware of each other).
 *
 * Please not that the event bus does not preserve the sender of the published events. If you need a reference to the
 * original sender you have to provide it inside the message.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
class EventBus : Process<EventBus.State, Any> {
    /**
     * The state of the event bus.
     */
    data class State(val subscribers: Set<Entity<*, *>> = emptySet())

    /**
     * The initial state of the event bus.
     */
    override val initialState = EventBus.State()

    /**
     * Run the simulation process for this entity.
     */
    override suspend fun Context<State, Any>.run() {
        while (true) {
            val msg = receive()
            when (msg) {
                Subscribe ->
                    if (sender != null) {
                        state = state.copy(subscribers = state.subscribers.plus(sender!!))
                    }
                Unsubscribe ->
                    if (sender != null) {
                        state = state.copy(subscribers = state.subscribers.minus(sender!!))
                    }
                is Publish ->
                    state.subscribers.forEach {
                        it.send(msg.event)
                    }
            }
        }
    }

    /**
     * Subscribe to this [EventBus] with the calling process.
     *
     * @throws IllegalStateException if the context cannot be found.
     */
    suspend fun subscribe() = untypedContext().run { this@EventBus.send(Subscribe) }

    /**
     * Unsubscribe to from [EventBus] with the calling process.
     *
     * @throws IllegalStateException if the context cannot be found.
     */
    suspend fun unsubscribe() = untypedContext().run { this@EventBus.send(Unsubscribe) }

    /**
     * Publish the given event to the [EventBus] with the calling process.
     *
     * @param event The event to publish.
     * @throws IllegalStateException if the context cannot be found.
     */
    suspend fun publish(event: Any) = untypedContext().run { this@EventBus.send(Publish(event)) }

    /**
     * This message is sent to the [EventBus] in order to subscribe to the bus
     * as sender of the message.
     */
    object Subscribe

    /**
     * This message is sent to the [EventBus] in order to unsubscribe from the
     * bus.
     */
    object Unsubscribe

    /**
     * This message is used to publish an event in the bus.
     *
     * @property event The event to publish.
     */
    data class Publish(val event: Any)
}

/**
 * Create an [Instrument] which listens to the event bus and publishes all events into a channel which can be processed
 * by the user.
 */
fun <M> EventBus.instrument(): Instrument<Any, M> = {
    subscribe()
    while (!isClosedForSend) {
        val event = receive()

        if (sender == this@instrument) {
            send(event)
        }
    }
    unsubscribe()
}
