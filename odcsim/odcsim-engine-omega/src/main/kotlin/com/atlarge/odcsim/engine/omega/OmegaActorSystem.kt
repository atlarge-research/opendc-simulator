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

package com.atlarge.odcsim.engine.omega

import com.atlarge.odcsim.ActorContext
import com.atlarge.odcsim.ActorPath
import com.atlarge.odcsim.ActorRef
import com.atlarge.odcsim.ActorSystem
import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.Duration
import com.atlarge.odcsim.Instant
import com.atlarge.odcsim.PreStart
import com.atlarge.odcsim.Signal
import java.util.PriorityQueue
import java.util.Queue
import kotlin.math.max
import org.jetbrains.annotations.Async

/**
 * The reference implementation of the [ActorSystem] instance for the OpenDC simulation core.
 *
 * This engine implementation is a single-threaded implementation, running actors synchronously and
 * provides a single priority queue for all events (messages, ticks, etc) that occur.
 *
 * @param name The name of the engine instance.
 */
class OmegaActorSystem(override val name: String) : ActorSystem {
    /**
     * The state of the actor system.
     */
    private var state: ActorSystemState = ActorSystemState.CREATED

    /**
     * The event queue to process
     */
    private val messageQueue: Queue<EnvelopeImpl> = PriorityQueue()

    /**
     * The registry of actors in the system.
     */
    private val registry: MutableMap<ActorPath, Actor> = HashMap()

    /**
     * The root actor path of the system.
     */
    private val root: ActorPath = ActorPath.Root()

    /**
     * The current point in simulation time.
     */
    private var time: Instant = .0

    init {
        val rootActor = Actor(ActorRefImpl(this, root), null, object : Behavior {})
        registry[root] = rootActor
        rootActor.start()
    }

    override fun run(until: Duration) {
        require(until >= .0) { "The given instant must be a non-negative number" }

        // Start the system/guardian actor on initial run
        if (state == ActorSystemState.TERMINATED) {
            throw IllegalStateException("The ActorSystem has been terminated.")
        }

        while (time < until) {
            // Check whether the system was interrupted
            if (Thread.interrupted()) {
                throw InterruptedException()
            }

            val envelope = messageQueue.peek() ?: break
            val delivery = envelope.time.takeUnless { it > until } ?: break

            // A message should never be delivered out of order in this single-threaded implementation. Assert for
            // sanity
            assert(delivery >= time) { "Message delivered out of order [expected=$delivery, actual=$time]" }

            time = delivery
            messageQueue.poll()

            processEnvelope(envelope)
        }

        // Jump forward in time as the caller expects the system to have run until the specified instant
        // Taking the maximum value prevents the caller to jump backwards in time
        time = max(time, until)
    }

    override fun spawn(behavior: Behavior, name: String): ActorRef {
        return registry[root]!!.spawn(behavior, name)
    }

    override fun terminate() {
        registry[root]!!.stop(null)
        state = ActorSystemState.TERMINATED
    }

    /**
     * Schedule a message to be processed by the engine.
     *
     * @param path The path to the destination of the message.
     * @param message The message to schedule.
     * @param delay The time to wait before processing the message.
     */
    private fun schedule(@Async.Schedule path: ActorPath, message: Any, delay: Duration) {
        require(delay >= .0) { "The given delay must be a non-negative number" }
        scheduleEnvelope(EnvelopeImpl(path, time + delay, message))
    }

    /**
     * Schedule the specified envelope to be processed by the engine.
     */
    private fun scheduleEnvelope(@Async.Schedule envelope: EnvelopeImpl) {
        messageQueue.add(envelope)
    }

    /**
     * Process the delivery of a message.
     */
    private fun processEnvelope(@Async.Execute envelope: EnvelopeImpl) {
        val actor = registry[envelope.destination] ?: return

        // Notice that messages for unknown/terminated actors are ignored for now
        actor.isolate { it.interpretMessage(envelope.message) }
    }

    /**
     * An actor as represented in the Omega engine.
     *
     * @property self The [ActorRef] to this actor.
     * @property parent The parent actor.
     * @param behavior The behavior of this actor.
     */
    private inner class Actor(override val self: ActorRef, private val parent: Actor?, private var behavior: Behavior) : ActorContext {
        val childActors: MutableMap<String, Actor> = mutableMapOf()

        override val time: Instant
            get() = this@OmegaActorSystem.time

        override val children: List<ActorRef>
            get() = childActors.values.map { it.self }

        override val system: ActorSystem
            get() = this@OmegaActorSystem

        override fun getChild(name: String): ActorRef? = childActors[name]?.self

        override fun send(ref: ActorRef, msg: Any, after: Duration) = schedule(ref.path, msg, after)

        override fun spawn(behavior: Behavior, name: String): ActorRef {
            require(name.isNotEmpty()) { "Actor name may not be empty" }
            require(!name.startsWith("$")) { "Actor name may not start with $-sign" }
            return internalSpawn(behavior, name)
        }

        private fun internalSpawn(behavior: Behavior, name: String): ActorRef {
            require(name !in childActors) { "Actor name $name not unique" }
            val ref = ActorRefImpl(this@OmegaActorSystem, self.path.child(name))
            val actor = Actor(ref, this, behavior)
            registry[ref.path] = actor
            childActors[name] = actor
            schedule(ref.path, PreStart, .0)
            actor.start()
            return ref
        }

        override fun stop(actor: ActorRef) {
            when {
                // Must be a direct child of this actor
                actor.path.parent == self.path -> {
                    val ref = childActors[actor.path.name] ?: return
                    ref.stop(null)
                }
                self == actor -> stop(null)
                else -> throw IllegalArgumentException(
                    "Only direct children of an actor may be stopped through the actor context, " +
                        "but [$actor] is not a child of [$self]. Stopping other actors has to be expressed as " +
                        "an explicit stop message that the actor accepts."
                )
            }
        }

        /**
         * Start this actor.
         */
        fun start() {
            behavior.start(this)
        }

        /**
         * Stop this actor.
         */
        fun stop(failure: Throwable?, propagated: Boolean = false) {
            val it = childActors.values.iterator()
            while (it.hasNext()) {
                val child = it.next()
                child.stop(failure, true)
                it.remove()
            }
            registry.remove(self.path)
            if (!propagated) {
                parent?.childActors?.remove(self.path.name)
            }
        }

        /**
         * Interpret the given message send to an actor.
         */
        fun interpretMessage(msg: Any) {
            if (msg is Signal) {
                behavior.receiveSignal(this, msg)
            } else {
                behavior.receive(this, msg)
            }
        }

        override fun equals(other: Any?): Boolean =
            other is Actor && self.path == other.self.path

        override fun hashCode(): Int = self.path.hashCode()
    }

    /**
     * Isolate uncaught exceptions originating from actor interpreter invocations.
     */
    private inline fun <U> Actor.isolate(block: (Actor) -> U): U? {
        return try {
            block(this)
        } catch (e: InterruptedException) {
            // Pass on thread interrupts
            throw e
        } catch (t: Throwable) {
            // Forcefully stop the actor if it crashed
            stop(t)
            t.printStackTrace()
            null
        }
    }

    /**
     * Enumeration to track the state of the actor system.
     */
    private enum class ActorSystemState {
        CREATED, TERMINATED
    }

    /**
     * Internal [ActorRef] implementation for this actor system.
     */
    private data class ActorRefImpl(
        private val owner: OmegaActorSystem,
        override val path: ActorPath
    ) : ActorRef {
        override fun toString(): String = "Actor[$path]"

        override fun compareTo(other: ActorRef): Int = path.compareTo(other.path)
    }

    /**
     * A wrapper around a message that has been scheduled for processing.
     *
     * @property destination The destination of the message.
     * @property time The point in time to deliver the message.
     * @property message The message to wrap.
     */
    private class EnvelopeImpl(
        val destination: ActorPath,
        val time: Instant,
        val message: Any
    ) : Comparable<EnvelopeImpl> {
        override fun compareTo(other: EnvelopeImpl): Int {
            val cmp = time.compareTo(other.time)
            return if (cmp == 0) {
                destination.compareTo(other.destination)
            } else {
                cmp
            }
        }
    }
}
