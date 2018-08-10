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

package com.atlarge.opendc.model.odc.integration.jpa.schema

import java.time.LocalDateTime
import javax.persistence.Entity

/**
 * A [Simulation] has several [Path]s, which define the topology of the datacenter at different times. A [Simulation]
 * also has several [Experiment]s, which can be run on a combination of [Path]s, [Scheduler]s and [Trace]s.
 * [Simulation]s also serve as the scope to which different [User]s can be authorized.
 *
 *
 * @property id The unique identifier of this simulation.
 * @property name the name of the simulation.
 * @property createdAt The date at which the simulation was created.
 * @property lastEditedAt The date at which the simulation was lasted edited.
 */
@Entity
data class Simulation(val id: Int?, val name: String, val createdAt: LocalDateTime, val lastEditedAt: LocalDateTime)
