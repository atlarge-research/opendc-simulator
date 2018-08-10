package com.atlarge.opendc.simulator

/**
 * A bootstrapping interface for a conceptual model that is a logical representation of some system of entities,
 * relationships and processes, as a basis for simulations.
 *
 * @param M The shape of the model that is bootstrapped.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Bootstrap<M> {
    /**
     * Apply the apply procedure for model `M` for a simulation in the given context.
     *
     * @param context The context to apply to model in.
     * @return The initialised, resulting model for the simulation.
     */
    fun apply(context: Context<M>): M

    /**
     * A context for the apply of some model type `M` that allows the model to register the entities of the model to
     * the simulation kernel.
     *
     * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
     */
    interface Context<out M> {
        /**
         * Asynchronously start the given [Process] in simulation. The process will be scheduled by the kernel as soon
         * as possible. If the process is already running, the method returns `true`.
         *
         * @param process The process to start.
         * @return `true` if the entity had not yet been registered, `false` otherwise.
         */
        fun start(process: Process<*, M>): Boolean

        /**
         * Stop the [Process] associated with the given [Entity] from further participating in the simulation.
         *
         * @param entity The entity to stop the process of.
         * @return `true` if the process of the entity was still running, `false` otherwise (there is no process
         * associated or it has already terminated).
         */
        fun stop(entity: Entity<*>): Boolean

        /**
         * Schedule a message to be received by the given [Entity].
         *
         * @param message The message to schedule.
         * @param destination The destination of the message.
         * @param sender The sender of the message.
         * @param delay The amount of time to wait before processing the message.
         */
        fun schedule(message: Any, destination: Entity<*>, sender: Entity<*>? = null, delay: Duration = 0)
    }

    companion object {
        /**
         * Create a [Bootstrap] procedure using the given block to produce a apply for a model of type `M`.
         *
         * @param block The block to produce the apply.
         * @return The apply procedure that has been built.
         */
        fun <M> create(block: (Context<M>) -> M): Bootstrap<M> = object : Bootstrap<M> {
            override fun apply(context: Context<M>) = block(context)
        }
    }
}
