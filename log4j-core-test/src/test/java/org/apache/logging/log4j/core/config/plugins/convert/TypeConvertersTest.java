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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Provider;
import java.security.Security;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.rolling.action.Duration;
import org.apache.logging.log4j.core.layout.GelfLayout;
import org.apache.logging.log4j.core.net.Facility;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests {@link TypeConverters}.
 */
public class TypeConvertersTest {

    @SuppressWarnings("boxing")
    public static Stream<Arguments> data() throws Exception {
        final byte[] byteArray = {
            (byte) 0xc7, (byte) 0x73, (byte) 0x21, (byte) 0x8c, (byte) 0x7e, (byte) 0xc8, (byte) 0xee, (byte) 0x99
        };
        return Stream.of(
                // Format: value, expected, default, type
                // boolean
                Arguments.of("true", true, null, Boolean.class),
                Arguments.of("false", false, null, Boolean.class),
                Arguments.of("True", true, null, Boolean.class),
                Arguments.of("TRUE", true, null, Boolean.class),
                Arguments.of("blah", false, null, Boolean.class),
                // TODO: is this acceptable? it's how Boolean.parseBoolean works
                Arguments.of(null, null, null, Boolean.class),
                Arguments.of(null, true, "true", Boolean.class),
                Arguments.of("no", false, null, Boolean.class), // TODO: see above
                Arguments.of("true", true, "false", boolean.class),
                Arguments.of("FALSE", false, "true", boolean.class),
                Arguments.of(null, false, "false", boolean.class),
                Arguments.of("invalid", false, "false", boolean.class),
                // byte
                Arguments.of("42", (byte) 42, null, Byte.class),
                Arguments.of("53", (byte) 53, null, Byte.class),
                // char
                Arguments.of("A", 'A', null, char.class),
                Arguments.of("b", 'b', null, char.class),
                Arguments.of("b0", null, null, char.class),
                // integer
                Arguments.of("42", 42, null, Integer.class),
                Arguments.of("53", 53, null, Integer.class),
                Arguments.of("-16", -16, null, Integer.class),
                Arguments.of("0", 0, null, Integer.class),
                Arguments.of("n", null, null, Integer.class),
                Arguments.of("n", 5, "5", Integer.class),
                Arguments.of("4.2", null, null, Integer.class),
                Arguments.of("4.2", 0, "0", Integer.class),
                Arguments.of(null, null, null, Integer.class),
                Arguments.of("75", 75, "0", int.class),
                Arguments.of("-30", -30, "0", int.class),
                Arguments.of("0", 0, "10", int.class),
                Arguments.of(null, 10, "10", int.class),
                // longs
                Arguments.of("55", 55L, null, Long.class),
                Arguments.of("1234567890123456789", 1234567890123456789L, null, Long.class),
                Arguments.of("123123123L", null, null, Long.class),
                Arguments.of("123123123123", 123123123123L, null, Long.class),
                Arguments.of("-987654321", -987654321L, null, Long.class),
                Arguments.of("-45l", null, null, Long.class),
                Arguments.of("0", 0L, null, Long.class),
                Arguments.of("asdf", null, null, Long.class),
                Arguments.of("3.14", null, null, Long.class),
                Arguments.of("3.14", 0L, "0", Long.class),
                Arguments.of("*3", 1000L, "1000", Long.class),
                Arguments.of(null, null, null, Long.class),
                Arguments.of("3000", 3000L, "0", long.class),
                Arguments.of("-543210", -543210L, "0", long.class),
                Arguments.of("22.7", -53L, "-53", long.class),
                // short
                Arguments.of("42", (short) 42, null, short.class),
                Arguments.of("53", (short) 53, null, short.class),
                Arguments.of("-16", (short) -16, null, Short.class),
                // Log4j
                // levels
                Arguments.of("ERROR", Level.ERROR, null, Level.class),
                Arguments.of("WARN", Level.WARN, null, Level.class),
                Arguments.of("FOO", null, null, Level.class),
                Arguments.of("FOO", Level.DEBUG, "DEBUG", Level.class),
                Arguments.of("OFF", Level.OFF, null, Level.class),
                Arguments.of(null, null, null, Level.class),
                Arguments.of(null, Level.INFO, "INFO", Level.class),
                // results
                Arguments.of("ACCEPT", Filter.Result.ACCEPT, null, Filter.Result.class),
                Arguments.of("NEUTRAL", Filter.Result.NEUTRAL, null, Filter.Result.class),
                Arguments.of("DENY", Filter.Result.DENY, null, Filter.Result.class),
                Arguments.of("NONE", null, null, Filter.Result.class),
                Arguments.of("NONE", Filter.Result.NEUTRAL, "NEUTRAL", Filter.Result.class),
                Arguments.of(null, null, null, Filter.Result.class),
                Arguments.of(null, Filter.Result.ACCEPT, "ACCEPT", Filter.Result.class),
                // syslog facilities
                Arguments.of("KERN", Facility.KERN, "USER", Facility.class),
                Arguments.of("mail", Facility.MAIL, "KERN", Facility.class),
                Arguments.of("Cron", Facility.CRON, null, Facility.class),
                Arguments.of("not a real facility", Facility.AUTH, "auth", Facility.class),
                Arguments.of(null, null, null, Facility.class),
                // GELF compression types
                Arguments.of("GZIP", GelfLayout.CompressionType.GZIP, "GZIP", GelfLayout.CompressionType.class),
                Arguments.of("ZLIB", GelfLayout.CompressionType.ZLIB, "GZIP", GelfLayout.CompressionType.class),
                Arguments.of("OFF", GelfLayout.CompressionType.OFF, "GZIP", GelfLayout.CompressionType.class),
                // arrays
                Arguments.of("123", "123".toCharArray(), null, char[].class),
                Arguments.of("123", "123".getBytes(Charset.defaultCharset()), null, byte[].class),
                Arguments.of("0xC773218C7EC8EE99", byteArray, null, byte[].class),
                Arguments.of("0xc773218c7ec8ee99", byteArray, null, byte[].class),
                Arguments.of("Base64:cGxlYXN1cmUu", "pleasure.".getBytes("US-ASCII"), null, byte[].class),
                // JRE
                // JRE Charset
                Arguments.of("UTF-8", StandardCharsets.UTF_8, null, Charset.class),
                Arguments.of("ASCII", Charset.forName("ASCII"), "UTF-8", Charset.class),
                Arguments.of("Not a real charset", StandardCharsets.UTF_8, "UTF-8", Charset.class),
                Arguments.of(null, StandardCharsets.UTF_8, "UTF-8", Charset.class),
                Arguments.of(null, null, null, Charset.class),
                // JRE File
                Arguments.of("c:/temp", new File("c:/temp"), null, File.class),
                // JRE Class
                Arguments.of(TypeConvertersTest.class.getName(), TypeConvertersTest.class, null, Class.class),
                Arguments.of("boolean", boolean.class, null, Class.class),
                Arguments.of("byte", byte.class, null, Class.class),
                Arguments.of("char", char.class, null, Class.class),
                Arguments.of("double", double.class, null, Class.class),
                Arguments.of("float", float.class, null, Class.class),
                Arguments.of("int", int.class, null, Class.class),
                Arguments.of("long", long.class, null, Class.class),
                Arguments.of("short", short.class, null, Class.class),
                Arguments.of("void", void.class, null, Class.class),
                Arguments.of("\t", Object.class, Object.class.getName(), Class.class),
                Arguments.of("\n", null, null, Class.class),
                // JRE URL
                Arguments.of("http://locahost", new URL("http://locahost"), null, URL.class),
                Arguments.of("\n", null, null, URL.class),
                // JRE URI
                Arguments.of("http://locahost", new URI("http://locahost"), null, URI.class),
                Arguments.of("\n", null, null, URI.class),
                // JRE BigInteger
                Arguments.of(
                        "9223372036854775817000", new BigInteger("9223372036854775817000"), null, BigInteger.class),
                Arguments.of("\n", null, null, BigInteger.class),
                // JRE BigInteger
                Arguments.of(
                        "9223372036854775817000.99999",
                        new BigDecimal("9223372036854775817000.99999"),
                        null,
                        BigDecimal.class),
                Arguments.of("\n", null, null, BigDecimal.class),
                // JRE Security Provider
                Arguments.of(Security.getProviders()[0].getName(), Security.getProviders()[0], null, Provider.class),
                Arguments.of("\n", null, null, Provider.class),
                // Duration
                Arguments.of("P7DT10H", Duration.parse("P7DT10H"), null, Duration.class),
                // JRE InetAddress
                Arguments.of("127.0.0.1", InetAddress.getByName("127.0.0.1"), null, InetAddress.class),
                // JRE Path
                Arguments.of("/path/to/file", Paths.get("/path", "to", "file"), null, Path.class),
                // JRE UUID
                Arguments.of(
                        "8fd389fb-9154-4096-b52e-435bde4a1835",
                        UUID.fromString("8fd389fb-9154-4096-b52e-435bde4a1835"),
                        null,
                        UUID.class));
    }

    @ParameterizedTest
    @MethodSource("data")
    void testConvert(final String value, final Object expected, final String defaultValue, final Class<?> clazz) {
        final Object actual = TypeConverters.convert(value, clazz, defaultValue);
        final String assertionMessage = "\nGiven: " + value + "\nDefault: " + defaultValue;
        assertThat(actual).as(assertionMessage).isEqualTo(expected);
    }
}
