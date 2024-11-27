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
package org.apache.logging.log4j.core.appender.routing;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.List;
import org.apache.logging.log4j.EventLogger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 *
 */
public class RoutingDefaultAppenderTest {
    private static final String LOG_FILE = "target/routing1/routingtest.log";

    private final LoggerContextRule loggerContextRule = new LoggerContextRule("log4j-routing3.xml");

    @Rule
    public RuleChain rules = loggerContextRule.withCleanFilesRule(LOG_FILE);

    @Test
    public void routingTest() {
        StructuredDataMessage msg = new StructuredDataMessage("Test", "This is a test", "Service");
        EventLogger.logEvent(msg);
        final List<LogEvent> list = loggerContextRule.getListAppender("List").getEvents();
        assertNotNull(list, "No events generated");
        assertTrue(list.size() == 1, "Incorrect number of events. Expected 1, got " + list.size());
        msg = new StructuredDataMessage("Test", "This is a test", "Alert");
        EventLogger.logEvent(msg);
        final File file = new File(LOG_FILE);
        assertTrue(file.exists(), "Alert file was not created");
    }
}
