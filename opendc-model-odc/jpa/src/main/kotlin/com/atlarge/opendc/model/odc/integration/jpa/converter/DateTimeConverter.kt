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

package com.atlarge.opendc.model.odc.integration.jpa.converter

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.persistence.AttributeConverter

/**
 * The datetime_created and datetime_last_edited columns are in a subset of ISO-8601 (second fractions are ommitted):
 *   YYYY-MM-DDTHH:MM:SS, where...
 *       -   YYYY is the four-digit year,
 *       -   MM is the two-digit month (1-12)
 *       -   DD is the two-digit day of the month (1-31)
 *       -   HH is the two-digit hours part (0-23)
 *       -   MM is the two-digit minutes part (0-59)
 *       -   SS is the two-digit seconds part (0-59)
 *
 */
class DateTimeConverter : AttributeConverter<LocalDateTime, String> {
    /**
     * The [DateTimeFormatter] to parse th [LocalDateTime] instance.
     */
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    /**
     * Converts the data stored in the database column into the
     * value to be stored in the entity attribute.
     * Note that it is the responsibility of the converter writer to
     * specify the correct dbData type for the corresponding column
     * for use by the JDBC driver: i.e., persistence providers are
     * not expected to do such type conversion.
     *
     * @param dbData the data from the database column to be converted
     * @return the converted value to be stored in the entity attribute
     */
    override fun convertToEntityAttribute(dbData: String): LocalDateTime = LocalDateTime.parse(dbData, formatter)

    /**
     * Converts the value stored in the entity attribute into the
     * data representation to be stored in the database.
     *
     * @param attribute the entity attribute value to be converted
     * @return the converted data to be stored in the database column
     */
    override fun convertToDatabaseColumn(attribute: LocalDateTime): String = attribute.format(formatter)
}
