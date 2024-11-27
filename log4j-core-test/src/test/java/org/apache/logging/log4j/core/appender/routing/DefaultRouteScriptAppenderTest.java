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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.apache.logging.log4j.core.util.Constants;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 *
 */
@RunWith(Parameterized.class)
public class DefaultRouteScriptAppenderTest {

    @Parameterized.Parameters(name = "{0} {1}")
    public static Object[][] getParameters() {
        // @formatter:off
        return new Object[][] {
            {"log4j-routing-default-route-script-groovy.xml", false},
            {"log4j-routing-default-route-script-javascript.xml", false},
            {"log4j-routing-script-staticvars-javascript.xml", true},
            {"log4j-routing-script-staticvars-groovy.xml", true},
        };
        // @formatter:on
    }

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(Constants.SCRIPT_LANGUAGES, "Groovy, Javascript");
    }

    @Rule
    public final LoggerContextRule loggerContextRule;

    private final boolean expectBindingEntries;

    public DefaultRouteScriptAppenderTest(final String configLocation, final boolean expectBindingEntries) {
        this.loggerContextRule = new LoggerContextRule(configLocation);
        this.expectBindingEntries = expectBindingEntries;
    }

    private void checkStaticVars() {
        final RoutingAppender routingAppender = getRoutingAppender();
        final ConcurrentMap<Object, Object> map = routingAppender.getScriptStaticVariables();
        if (expectBindingEntries) {
            assertEquals(map.get("TestKey"), "TestValue2");
            assertEquals(map.get("MarkerName"), "HEXDUMP");
        }
    }

    private ListAppender getListAppender() {
        final String key = "Service2";
        final RoutingAppender routingAppender = getRoutingAppender();
        assertTrue(routingAppender.isStarted());
        final Map<String, AppenderControl> appenders = routingAppender.getAppenders();
        final AppenderControl appenderControl = appenders.get(key);
        assertNotNull(appenderControl, "No appender control generated for '" + key + "'; appenders = " + appenders);
        final ListAppender listAppender = (ListAppender) appenderControl.getAppender();
        return listAppender;
    }

    private RoutingAppender getRoutingAppender() {
        return loggerContextRule.getRequiredAppender("Routing", RoutingAppender.class);
    }

    private void logAndCheck() {
        final Marker marker = MarkerManager.getMarker("HEXDUMP");
        final Logger logger = loggerContextRule.getLogger(DefaultRouteScriptAppenderTest.class);
        logger.error("Hello");
        final ListAppender listAppender = getListAppender();
        assertEquals(1, listAppender.getEvents().size(), "Incorrect number of events");
        logger.error("World");
        assertEquals(2, listAppender.getEvents().size(), "Incorrect number of events");
        logger.error(marker, "DEADBEEF");
        assertEquals(3, listAppender.getEvents().size(), "Incorrect number of events");
    }

    @Test(expected = AssertionError.class)
    public void testAppenderAbsence() {
        loggerContextRule.getListAppender("List1");
    }

    @Test
    public void testListAppenderPresence() {
        // No appender until an event is routed, even thought we initialized the default route on startup.
        assertNull(getRoutingAppender().getAppenders().get("Service2"), "No appender control generated");
    }

    @Test
    public void testNoPurgePolicy() {
        // No PurgePolicy in this test
        assertNull(getRoutingAppender().getPurgePolicy(), "Unexpected PurgePolicy");
    }

    @Test
    public void testNoRewritePolicy() {
        // No RewritePolicy in this test
        assertNull(getRoutingAppender().getRewritePolicy(), "Unexpected RewritePolicy");
    }

    @Test
    public void testRoutingAppenderDefaultRouteKey() {
        final RoutingAppender routingAppender = getRoutingAppender();
        assertNotNull(routingAppender.getDefaultRouteScript());
        assertNotNull(routingAppender.getDefaultRoute());
        assertEquals(routingAppender.getDefaultRoute().getKey(), "Service2");
    }

    @Test
    public void testRoutingAppenderPresence() {
        getRoutingAppender();
    }

    @Test
    public void testRoutingPresence1() {
        logAndCheck();
        checkStaticVars();
    }

    @Test
    public void testRoutingPresence2() {
        logAndCheck();
        checkStaticVars();
    }
}
