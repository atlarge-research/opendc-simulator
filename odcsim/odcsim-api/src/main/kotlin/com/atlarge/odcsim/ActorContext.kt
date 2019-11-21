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

package com.atlarge.odcsim

/**
 * Represents the context in which the execution of an actor's behavior takes place.
 */
interface ActorContext {
    /**
     * The identity of the actor, bound to the lifecycle of this actor instance.
     */
    val self: ActorRef

    /**
     * A view of the children of this actor.
     */
    val children: List<ActorRef>

    /**
     * The point of time within the simulation.
     */
    val time: Instant

    /**
     * The [ActorSystem] the actor is part of.
     */
    val system: ActorSystem

    /**
     * Obtain the child of this actor with the specified name.
     *
     * @param name The name of the child actor to obtain.
     * @return The reference to the child actor or `null` if it does not exist.
     */
    fun getChild(name: String): ActorRef?

    /**
     * Send the specified message to the actor referenced by this [ActorRef].
     *
     * @param ref The actor to send the message to.
     * @param msg The message to send to the referenced actor.
     * @param after The delay after which the message should be received by the actor.
     */
    fun send(ref: ActorRef, msg: Any, after: Duration = 0.1)

    /**
     * Spawn a child actor from the given [Behavior] and with the specified name.
     *
     * The name may not be empty or start with "$". Moreover, the name of an actor must be unique and this method
     * will throw an [IllegalArgumentException] in case a child actor of the given name already exists.
     *
     * @param behavior The behavior of the child actor to spawn.
     * @param name The name of the child actor to spawn.
     * @return A reference to the child that has/will be spawned.
     */
    fun spawn(behavior: Behavior, name: String): ActorRef

    /**
     * Force the specified current or child actor to terminate after it finishes processing its current message.
     * Nothing will happen if the actor is already stopped.
     *
     * Only itself and direct children of the actor may be stopped through the actor context. Trying to stop other
     * actors via this method will result in an [IllegalArgumentException]. Instead, stopping other actors has to be
     * expressed as an explicit stop message that the actor accept.
     *
     * @param actor The reference to the actor to stop.
     */
    fun stop(actor: ActorRef)
}
