/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.config.plugins.convert;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 *
 */
class DateTypeConverterTest {

    static Stream<Arguments> data() {
        final long millis = System.currentTimeMillis();
        return Stream.of(
                Arguments.of(Date.class, millis, new Date(millis)),
                Arguments.of(java.sql.Date.class, millis, new java.sql.Date(millis)),
                Arguments.of(Time.class, millis, new Time(millis)),
                Arguments.of(Timestamp.class, millis, new Timestamp(millis)));
    }

    @ParameterizedTest
    @MethodSource("data")
    void testFromMillis(final Class<? extends Date> dateClass, final long timestamp, final Object expected)
            throws Exception {
        assertEquals(expected, DateTypeConverter.fromMillis(timestamp, dateClass));
    }
}
