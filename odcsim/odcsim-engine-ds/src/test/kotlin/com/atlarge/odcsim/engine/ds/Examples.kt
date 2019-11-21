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

package com.atlarge.odcsim.engine.ds

import com.atlarge.odcsim.ActorContext
import com.atlarge.odcsim.ActorRef
import org.junit.jupiter.api.Test
import com.atlarge.odcsim.ActorSystemFactory
import com.atlarge.odcsim.Behavior
import kotlin.random.Random

/**
 * A collection of examples showing the functionality of the odcsim framework.
 */
class Examples {
    /**
     * The [ActorSystemFactory] we use to construct the actor system.
     */
    private val factory: ActorSystemFactory = DistributedActorSystemFactory()

    /**
     * This example shows how we construct two actors that exchange ping-pong messages with each other.
     */
    @Test
    fun pingPong() {
        val system = factory(name = "ping-pong")

        data class Ping(val replyTo: ActorRef)
        data class Pong(val replyTo: ActorRef)

        val pong = system.spawn(object : Behavior {
            override fun receive(ctx: ActorContext, msg: Any) {
                if (msg is Ping) {
                    println("[${ctx.time}] PING")
                    ctx.send(msg.replyTo, Pong(ctx.self))
                }
            }
        }, name = "pong")

        val ping = system.spawn(object : Behavior {
            override fun start(ctx: ActorContext) {
                ctx.send(pong, Ping(ctx.self))
            }


            override fun receive(ctx: ActorContext, msg: Any) {
                if (msg is Pong) {
                    println("[${ctx.time}] PONG")
                    ctx.send(msg.replyTo, Ping(ctx.self))
                }
            }
        }, name = "ping")


        system.run(until = 10.0)
    }

    /**
     * This example shows how to implement the classic HOLD benchmark using our framework.
     */
    @Test
    fun hold() {
        val system = factory(name = "hold")

        data class Start(val processors: List<ActorRef>)

        class HoldActor : Behavior {
            private val random = Random
            lateinit var processors: List<ActorRef>

            override fun receive(ctx: ActorContext, msg: Any) {
                if (msg is Start) {
                    processors = msg.processors
                }

                ctx.send(processors.random(random), "hold", after = random.nextDouble())
            }
        }

        system.spawn(object : Behavior {
            override fun start(ctx: ActorContext) {
                val actors = List(20) { i ->
                    system.spawn(HoldActor(), name = "hold-$i")
                }

                for (actor in actors) {
                    ctx.send(actor, Start(actors), after = 0.0)
                }
            }
        }, name = "manager")


        system.run(until = 10.0)
    }
}
