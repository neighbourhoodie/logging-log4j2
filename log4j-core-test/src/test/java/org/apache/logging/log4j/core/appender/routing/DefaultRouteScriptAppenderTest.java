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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AsyncAppenderTest;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.util.Constants;
import org.junit.Assert;
// import org.junit.BeforeClass;
// import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
// import org.junit.jupiter.params.ParameterizedTest;
// import org.junit.runner.RunWith;
// import org.junit.runners.Parameterized;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
// public abstract class AbstractDefaultRouteScriptAppenderTest {
//     private LoggerContext loggerContext = null;
//     private boolean expectBindingEntries = false;

//     AbstractDefaultRouteScriptAppenderTest(LoggerContext ctx, boolean expectBindingEntries) {
//         this.loggerContext = ctx;
//         this.expectBindingEntries = expectBindingEntries;
//     }

//     @BeforeClass
//     public static void beforeClass() {
//         System.setProperty(Constants.SCRIPT_LANGUAGES, "Groovy, Javascript");
//     }

//     // TODO put all the tests here
// }


// @LoggerContextSource("log4j-routing-default-route-script-groovy.xml")
// public class RouteScriptGroovy extends AbstractDefaultRouteScriptAppenderTest {
//     RouteScriptGroovy(LoggerContext ctx) {
//         super(ctx, false);
//     }
// }

/**
 *
 */
// @RunWith(Parameterized.class)
public class DefaultRouteScriptAppenderTest {

    // @Parameterized.Parameters(name = "{0} {1}")
    
    public static Stream<Arguments> getParameters() {
        // @formatter:off
        return Stream.of(
            // Arguments.of("/log4j-routing-default-route-script-groovy.xml", false),
            Arguments.of("/log4j-routing-default-route-script-groovy.xml", false),
            Arguments.of("/log4j-routing-default-route-script-javascript.xml", false),
            Arguments.of("/log4j-routing-script-staticvars-javascript.xml", true),
            Arguments.of("/log4j-routing-script-staticvars-groovy.xml", true)
        );
        // @formatter:on
    }

    @BeforeAll
    public static void beforeClass() {
        System.setProperty(Constants.SCRIPT_LANGUAGES, "Groovy, Javascript");
    }

    // @Rule
    public final LoggerContextRule loggerContextRule = null;

    private LoggerContext context = null;
    private boolean expectBindingEntries;

    // public DefaultRouteScriptAppenderTest(final String configLocation, final boolean expectBindingEntries) {
    //     this.loggerContextRule = new LoggerContextRule(configLocation);
    //     this.expectBindingEntries = expectBindingEntries;
    // }

    private void checkStaticVars() {
        final RoutingAppender routingAppender = getRoutingAppender();
        final ConcurrentMap<Object, Object> map = routingAppender.getScriptStaticVariables();
        if (expectBindingEntries) {
            assertEquals("TestValue2", map.get("TestKey"));
            assertEquals("HEXDUMP", map.get("MarkerName"));
        }
    }

    private ListAppender getListAppender() {
        final String key = "Service2";
        final RoutingAppender routingAppender = getRoutingAppender();
        Assert.assertTrue(routingAppender.isStarted());
        final Map<String, AppenderControl> appenders = routingAppender.getAppenders();
        final AppenderControl appenderControl = appenders.get(key);
        assertNotNull("No appender control generated for '" + key + "'; appenders = " + appenders, appenderControl);
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
        assertEquals("Incorrect number of events", 1, listAppender.getEvents().size());
        logger.error("World");
        assertEquals("Incorrect number of events", 2, listAppender.getEvents().size());
        logger.error(marker, "DEADBEEF");
        assertEquals("Incorrect number of events", 3, listAppender.getEvents().size());
    }

    private void setupContext(final String configLocation, final boolean expectBindingEntries) throws IOException, URISyntaxException {
        this.expectBindingEntries = expectBindingEntries;

        final URI uri = LoggerContextRule.class.getResource(configLocation).toURI();
        final Path path = Paths.get(uri);

        System.err.println("ALBA path");
        System.err.println(path);

        final InputStream inputStream = Files.newInputStream(path);
        final ConfigurationSource source = new ConfigurationSource(inputStream, path);

        System.err.println("ALBA source");
        System.err.println(source);

        context = (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
        final Configuration configuration = new XmlConfigurationFactory().getConfiguration(context, source);
        assertNotNull(configuration, "No configuration created");
        Configurator.reconfigure(configuration);
    }

    @ParameterizedTest()
    @MethodSource("getParameters")
    public void testAppenderAbsence(final String configLocation, final boolean expectBindingEntries) throws IOException, URISyntaxException {
        setupContext(configLocation, expectBindingEntries);

        assertThrows(UnsupportedOperationException.class, () -> context.getConfiguration().getAppender("List1"));
    }

    public void testListAppenderPresence() {
        // No appender until an event is routed, even thought we initialized the default route on startup.
        Assert.assertNull(
                "No appender control generated",
                getRoutingAppender().getAppenders().get("Service2"));
    }

    @Test
    @Disabled
    public void testNoPurgePolicy() {
        // No PurgePolicy in this test
        Assert.assertNull("Unexpected PurgePolicy", getRoutingAppender().getPurgePolicy());
    }

    @Test
    @Disabled
    public void testNoRewritePolicy() {
        // No RewritePolicy in this test
        Assert.assertNull("Unexpected RewritePolicy", getRoutingAppender().getRewritePolicy());
    }

    @Test
    @Disabled
    public void testRoutingAppenderDefaultRouteKey() {
        final RoutingAppender routingAppender = getRoutingAppender();
        Assert.assertNotNull(routingAppender.getDefaultRouteScript());
        Assert.assertNotNull(routingAppender.getDefaultRoute());
        assertEquals("Service2", routingAppender.getDefaultRoute().getKey());
    }

    @Test
    @Disabled
    public void testRoutingAppenderPresence() {
        getRoutingAppender();
    }

    @Test
    @Disabled
    public void testRoutingPresence1() {
        logAndCheck();
        checkStaticVars();
    }

    @Test
    @Disabled
    public void testRoutingPresence2() {
        logAndCheck();
        checkStaticVars();
    }
}
