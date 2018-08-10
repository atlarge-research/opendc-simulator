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

package com.atlarge.opendc.model.odc.topology.format.sc18

import com.atlarge.opendc.model.odc.integration.jpa.schema.Cpu
import com.atlarge.opendc.model.odc.integration.jpa.schema.Datacenter
import com.atlarge.opendc.model.odc.integration.jpa.schema.Path
import com.atlarge.opendc.model.odc.integration.jpa.schema.Rack
import com.atlarge.opendc.model.odc.integration.jpa.schema.RoomType
import com.atlarge.opendc.model.odc.integration.jpa.schema.Section
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.InputStream

import com.atlarge.opendc.model.odc.integration.jpa.schema.Machine as InternalMachine
import com.atlarge.opendc.model.odc.integration.jpa.schema.Room as InternalRoom

/**
 * A parser for the JSON experiment setup files used for the SC18 paper.
 *
 * @property objectMapper The Jackson [ObjectMapper] to map the setup file.
 */
class Sc18SetupParser(private val objectMapper: ObjectMapper = jacksonObjectMapper()) {
    /**
     * Parse the given [InputStream] as setup file and convert it into a [Path].
     * After the method returns, the input stream is closed.
     *
     * @param input The [InputStream] to parse as setup file.
     * @return The [Path] that has been parsed.
     */
    fun parseSetup(input: InputStream): Setup = objectMapper.readValue(input)

    /**
     * Parse the given [InputStream] as setup file. After the method returns, the input stream
     * is closed.
     *
     * @param input The [InputStream] to parse as setup file.
     * @return The [Path] that has been parsed.
     */
    fun parse(input: InputStream): Path {
        val setup: Setup = parseSetup(input)
        val rooms = setup.rooms.map { room ->
            val objects = room.objects.map { roomObject ->
                when (roomObject) {
                    is RoomObject.Rack -> {
                        val machines = roomObject.machines.mapIndexed { position, machine ->
                            val cpus = machine.cpus.map { id ->
                                when (id) {
                                    1 -> Cpu(null, "intel", "i7", "v6", "6700k", 4100, 4, 70.0)
                                    2 -> Cpu(null, "intel", "i5", "v6", "6700k", 3500, 2, 50.0)
                                    else -> throw IllegalArgumentException("The cpu id $id is not recognized")
                                }
                            }
                            InternalMachine(null, position, cpus.toSet(), emptySet())
                        }
                        Rack(null, "", 0, 42, machines)
                    }
                }
            }
            InternalRoom(null, room.type, RoomType.valueOf(room.type), objects)
        }
        val datacenter = Datacenter(null, rooms)
        val sections = listOf(Section(null, datacenter, 0))
        return Path(null, sections)
    }
}


