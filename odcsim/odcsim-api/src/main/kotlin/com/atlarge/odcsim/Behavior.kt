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
 * The representation of the behavior of an actor.
 *
 * Users are advised not to close over [ActorContext] within [Behavior], as it will causes it to become immobile,
 * meaning it cannot be moved to another context and executed there, and therefore it cannot be replicated or forked
 * either.
 */
interface Behavior {
    /**
     * Start the actor running this [Behavior].
     *
     * @param ctx The [ActorContext] in which the actor is currently running.
     */
    fun start(ctx: ActorContext) {}

    /**
     * Process an incoming message.
     *
     * @param ctx The [ActorContext] in which the actor is currently running.
     * @param msg The message that was received.
     */
    fun receive(ctx: ActorContext, msg: Any) {}

    /**
     * Process an incoming signal.
     *
     * @param ctx The [ActorContext] in which the actor is currently running.
     * @param signal The signal that was received.
     */
    fun receiveSignal(ctx: ActorContext, signal: Signal) {}
}

