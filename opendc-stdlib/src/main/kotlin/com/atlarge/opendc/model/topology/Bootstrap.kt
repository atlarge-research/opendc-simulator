package com.atlarge.opendc.model.topology

import com.atlarge.opendc.simulator.Bootstrap
import com.atlarge.opendc.simulator.Entity
import com.atlarge.opendc.simulator.Process

/**
 * Create a [Bootstrap] procedure for the given [Topology].
 *
 * @return A apply procedure for the topology.
 */
fun <T : Topology> T.bootstrap(): Bootstrap<T> = Bootstrap.create { ctx ->
    forEach {
        if (it is Process<*, *>) {
            @Suppress("UNCHECKED_CAST")
            ctx.start(it as Process<*, T>)
        }
    }
    listeners += object : TopologyListener {
        override fun Topology.onNodeAdded(node: Entity<*>) {
            if (node is Process<*, *>) {
                @Suppress("UNCHECKED_CAST")
                ctx.start(node as Process<*, T>)
            }
        }

        override fun Topology.onNodeRemoved(node: Entity<*>) {
            ctx.stop(node)
        }
    }
    this
}
