/*
 * MIT License
 *
 * Copyright (c) 2017 atlarge-research
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

package com.atlarge.opendc.model.odc.integration.jpa

import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.consume
import kotlinx.coroutines.experimental.channels.produce
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.RollbackException
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.coroutineContext

/**
 * Run the given block in a transaction, committing on return of the block.
 *
 * @param block The block to execute in the transaction.
 */
inline fun EntityManager.transaction(block: () -> Unit) {
    transaction.begin()
    block()
    transaction.commit()
}

/**
 * Write the given channel in batch to the database.
 *
 * @param factory The [EntityManagerFactory] to use to create an [EntityManager] which can persist the entities.
 * @param batchSize The size of each batch.
 */
suspend fun <E> ReceiveChannel<E>.persist(factory: EntityManagerFactory, batchSize: Int = 1000) {
    val writer = factory.createEntityManager()

    this
        .buffer(coroutineContext, batchSize)
        .consume {
        val transaction = writer.transaction
        try {

            for (buffer in this) {
                transaction.begin()
                buffer.forEach { writer.persist(it) }

                writer.flush()
                writer.clear()

                transaction.commit()
            }
        } catch(e: RollbackException) {
            // Rollback transaction if still active
            if (transaction.isActive) {
                transaction.rollback()
            }

            throw e
        } finally {
            writer.close()
        }
    }
}

/**
 * Buffer a given amount of elements before emitting them as a list.
 *
 * @param size The size of the buffer.
 * @return A [ReceiveChannel] that emits lists of type [E] that have been buffered.
 */
private fun <E> ReceiveChannel<E>.buffer(context: CoroutineContext = Unconfined, size: Int = 1000): ReceiveChannel<List<E>> = produce(context) {
    consume {
        var buffer: MutableList<E> = ArrayList(size)

        for (element in this) {
            if (buffer.size == size) {
                send(buffer)
                buffer = ArrayList(size)
            }

            buffer.add(element)
        }

        send(buffer)
    }
}
