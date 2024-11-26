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
package org.apache.logging.log4j.core.lookup;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.naming.directory.InitialDirContext;
import javax.naming.ldap.LdapContext;

import org.apache.logging.log4j.message.Message;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zapodot.junit.ldap.internal.jndi.ContextProxyFactory;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;

/**
 * JndiLookupTest
 */
public class JndiRestrictedLookupTest {

    private static final String LDAP_URL = "ldap://127.0.0.1:";
    private static final String RESOURCE = "JndiExploit";
    private static final String TEST_STRING = "TestString";
    private static final String TEST_MESSAGE = "TestMessage";
    private static final String LEVEL = "TestLevel";
    private static final String DOMAIN = "apache.org";

    private static final String DOMAIN_DSN = "dc=apache,dc=org";
    private static final String LDIF_FILENAME = "JndiRestrictedLookup.ldif";
    private static final String JAVA_RT_CONTROL_FACTORY = "com.sun.jndi.ldap.DefaultResponseControlFactory";
    private static final String JAVA_RT_CONTEXT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
    public static final String DEFAULT_BIND_DSN = "cn=Directory manager";
    public static final String DEFAULT_BIND_CREDENTIALS = "password";
    public static final String LDAP_SERVER_LISTENER_NAME = "test-listener";

    private InMemoryDirectoryServer ldapServer;
    private InitialDirContext initialDirContext;

    @Rule
    /* public EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder.newInstance()
            .usingDomainDsn(DOMAIN_DSN)
            .importingLdifs("JndiRestrictedLookup.ldif")
            .build(); */

    @BeforeEach
    void startLdapServer() throws LDAPException, NamingException, UnsupportedEncodingException {
        InMemoryDirectoryServerConfig config = getConfig();

        ldapServer = new InMemoryDirectoryServer(config);
        String path = Resources.getResource(LDIF_FILENAME).getPath();
        ldapServer.importFromLDIF(false, URLDecoder.decode(path, Charsets.UTF_8.name()));
        ldapServer.startListening();

        initialDirContext = buildInitialDirContext();
    }

    private InMemoryDirectoryServerConfig getConfig() throws LDAPException {
        String[] domainDsnArray = new String[]{DOMAIN_DSN};
        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(domainDsnArray);

        config.addAdditionalBindCredentials(DEFAULT_BIND_DSN, DEFAULT_BIND_CREDENTIALS);

        InetAddress bindAddress = InetAddress.getLoopbackAddress();
        Integer bindPort = 0;
        InMemoryListenerConfig listenerConfig = InMemoryListenerConfig.createLDAPConfig(LDAP_SERVER_LISTENER_NAME, bindAddress, bindPort, null);
        config.setListenerConfigs(listenerConfig);

        config.setSchema(null);

        return config;
    }

    private InitialDirContext buildInitialDirContext() throws NamingException {
        Hashtable<String, String> environment = new Hashtable<>();

        environment.put(LdapContext.CONTROL_FACTORIES, JAVA_RT_CONTROL_FACTORY);
        environment.put(Context.INITIAL_CONTEXT_FACTORY, JAVA_RT_CONTEXT_FACTORY);

        environment.put(Context.PROVIDER_URL, String.format("ldap://%s:%s",
                                                            ldapServer.getListenAddress().getHostName(),
                                                            embeddedServerPort()));

        return new InitialDirContext(environment);
    }

    @AfterEach
    void shutdownLdapServer() {
        try {
            /* if (ldapConnection != null && ldapConnection.isConnected()) {
                ldapConnection.close();
            } */
            if (initialDirContext != null) {
                initialDirContext.close();
            }
        } catch (NamingException e) {
            // logger.info("Could not close initial context, forcing server shutdown anyway", e);
        } finally {
            ldapServer.shutDown(true);
        }
    }

    private int embeddedServerPort() {
        return ldapServer.getListenPort();
    }

    private Context context() {
        return ContextProxyFactory.asDelegatingContext(initialDirContext);
    }

    @BeforeAll
    public static void beforeAll() {
        System.setProperty("log4j2.enableJndiLookup", "true");
    }

    @Test
    @SuppressWarnings("BanJNDI")
    public void testBadUriLookup() throws Exception {
        final int port = embeddedServerPort();
        final Context context = context();
        context.bind("cn=" + RESOURCE + "," + DOMAIN_DSN, new Fruit("Test Message"));
        final StrLookup lookup = new JndiLookup();
        final String result = lookup.lookup(
                LDAP_URL + port + "/" + "cn=" + RESOURCE + "," + DOMAIN_DSN + "?Type=A Type&Name=1100110&Char=!");
        if (result != null) {
            fail("Lookup returned an object");
        }
    }

    @Test
    @SuppressWarnings("BanJNDI")
    public void testReferenceLookup() throws Exception {
        final int port = embeddedServerPort();
        final Context context = context();
        context.bind("cn=" + RESOURCE + "," + DOMAIN_DSN, new Fruit("Test Message"));
        final StrLookup lookup = new JndiLookup();
        final String result = lookup.lookup(LDAP_URL + port + "/" + "cn=" + RESOURCE + "," + DOMAIN_DSN);
        if (result != null) {
            fail("Lookup returned an object");
        }
    }

    @Test
    @SuppressWarnings("BanJNDI")
    public void testSerializableLookup() throws Exception {
        final int port = embeddedServerPort();
        final Context context = context();
        context.bind("cn=" + TEST_STRING + "," + DOMAIN_DSN, "Test Message");
        final StrLookup lookup = new JndiLookup();
        final String result = lookup.lookup(LDAP_URL + port + "/" + "cn=" + TEST_STRING + "," + DOMAIN_DSN);
        if (result != null) {
            fail("LDAP is enabled");
        }
    }

    @Test
    @SuppressWarnings("BanJNDI")
    public void testBadSerializableLookup() throws Exception {
        final int port = embeddedServerPort();
        final Context context = context();
        context.bind("cn=" + TEST_MESSAGE + "," + DOMAIN_DSN, new SerializableMessage("Test Message"));
        final StrLookup lookup = new JndiLookup();
        final String result = lookup.lookup(LDAP_URL + port + "/" + "cn=" + TEST_MESSAGE + "," + DOMAIN_DSN);
        if (result != null) {
            fail("Lookup returned an object");
        }
    }

    @Test
    public void testDnsLookup() throws Exception {
        final StrLookup lookup = new JndiLookup();
        final String result = lookup.lookup("dns:/" + DOMAIN);
        if (result != null) {
            fail("No DNS data returned");
        }
    }

    static class Fruit implements Referenceable {
        String fruit;

        public Fruit(final String f) {
            fruit = f;
        }

        public Reference getReference() throws NamingException {

            return new Reference(
                    Fruit.class.getName(),
                    new StringRefAddr("fruit", fruit),
                    JndiExploit.class.getName(),
                    null); // factory location
        }

        public String toString() {
            return fruit;
        }
    }

    static class SerializableMessage implements Serializable, Message {
        private final String message;

        SerializableMessage(final String message) {
            this.message = message;
        }

        @Override
        public String getFormattedMessage() {
            return message;
        }

        @Override
        public Object[] getParameters() {
            return null;
        }

        @Override
        public Throwable getThrowable() {
            return null;
        }
    }
}
