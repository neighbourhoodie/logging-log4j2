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
package org.apache.logging.log4j.core.appender.rolling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.test.junit.CleanFolders;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test LOG4J2-2485.
 */
public class RollingAppenderDirectWriteStartupSizeTest {

    private static final String CONFIG = "log4j-rolling-direct-startup-size.xml";

    private static final String DIR = "target/rolling-direct-startup-size";

    private static final String FILE = "size-test.log";

    private static final String MESSAGE = "test message";

    @BeforeAll
    public static void beforeAll() throws Exception {
        final Path log = Paths.get(DIR, FILE);
        if (Files.exists(log)) {
            Files.delete(log);
        }

        Files.createDirectories(log.getParent());
        Files.createFile(log);
        Files.write(log, MESSAGE.getBytes());
    }

    @BeforeEach
    @LoggerContextSource(value = CONFIG, timeout = 10)
    public void setUp() throws Exception {
        new CleanFolders(false, true, 10, DIR);
    }

    @AfterEach
    @LoggerContextSource(value = CONFIG, timeout = 10)
    public void teardown() throws Exception {
        new CleanFolders(false, true, 10, DIR);
    }

    @Test
    @LoggerContextSource(value = CONFIG, timeout = 10)
    public void testRollingFileAppenderWithReconfigure(@Named("RollingFile") RollingFileAppender rfAppender)
            throws Exception {
        final RollingFileManager manager = rfAppender.getManager();

        assertNotNull(manager);
        assertEquals(MESSAGE.getBytes().length, manager.size, "Existing file size not preserved on startup");
    }
}
