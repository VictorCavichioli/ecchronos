/*
 * Copyright 2018 Telefonaktiebolaget LM Ericsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ericsson.bss.cassandra.ecchronos.connection.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.rmi.ssl.SslRMIClientSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.ericsson.bss.cassandra.ecchronos.connection.JmxConnectionProvider;

public class DistributedJmxConnectionProvider implements JmxConnectionProvider
{
    private static final Logger LOG = LoggerFactory.getLogger(DistributedJmxConnectionProvider.class);

    private static final String JMX_FORMAT_URL = "service:jmx:rmi:///jndi/rmi://%s:%d/jmxrmi";

    public static final int DEFAULT_PORT = 7199;
    public static final String DEFAULT_HOST = "localhost";

    private final AtomicReference<JMXConnector> myJmxConnection = new AtomicReference<>();

    private final CqlSession mySession;
    private final List<Node> myNodesList;
    private static Map<UUID, JMXConnector> myJMXConnections = new ConcurrentHashMap<>();
    private final Supplier<String[]> myCredentialsSupplier;
    private final Supplier<Map<String, String>> myTLSSupplier;

    public DistributedJmxConnectionProvider(
        final CqlSession cqlSession,
        final List<Node> nodesList,
        final Supplier<String[]> credentialsSupplier,
        final Supplier<Map<String, String>> tlsSupplier)
            throws IOException
    {
        mySession = cqlSession;
        myNodesList = nodesList;
        myCredentialsSupplier = credentialsSupplier;
        myTLSSupplier = tlsSupplier;
        reconnectAll();
    }

    @Override
    public final JMXConnector getJmxConnector() throws IOException
    {
        JMXConnector jmxConnector = myJmxConnection.get();

        if (jmxConnector == null || !isConnected(jmxConnector))
        {
            reconnectAll();
            return getJmxConnector();
        }

        return jmxConnector;
    }

    @Override
    public final void close() throws IOException
    {
    }

    private void reconnectAll() throws IOException
    {
        for (int i = 0; i <= myNodesList.size(); i++)
        {
            Node node = myNodesList.get(i);
            LOG.info("Creating connection with node {}", node.getHostId());
            try
            {
                reconnect(node);
                LOG.info("Connection created with success");
            }
            catch (Exception e)
            {
                LOG.info("Unable to connect with node {} connection refused", node.getHostId());
            }
        }
    }

    private void reconnect(final Node node) throws IOException
    {
        String host = node.getBroadcastRpcAddress().get().getHostString();
        Integer port = getJMXPort(node);
        if (host.contains(":"))
        {
            // Use square brackets to surround IPv6 addresses
            host = "[" + host + "]";
        }

        LOG.info("Starting to instantiate JMXService with host: {} and port: {}", host, port);
        JMXServiceURL jmxUrl = new JMXServiceURL(String.format(JMX_FORMAT_URL, host, port));
        LOG.debug("Connecting JMX through {}, credentials: {}, tls: {}", jmxUrl, isAuthEnabled(), isTLSEnabled());
        JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxUrl, createJMXEnv());
        if (isConnected(jmxConnector))
        {
            LOG.info("Connected JMX for {}", jmxUrl);
            myJMXConnections.put(node.getHostId(), jmxConnector);
        }
    }

    private Map<String, Object> createJMXEnv()
    {
        Map<String, Object> env = new HashMap<>();
        String[] credentials = getCredentialsConfig();
        Map<String, String> tls = getTLSConfig();
        if (credentials != null)
        {
            env.put(JMXConnector.CREDENTIALS, credentials);
        }

        if (!tls.isEmpty())
        {
            for (Map.Entry<String, String> configEntry : tls.entrySet())
            {
                String key = configEntry.getKey();
                String value = configEntry.getValue();

                if (!value.isEmpty())
                {
                    System.setProperty(key, value);
                }
                else
                {
                    System.clearProperty(key);
                }
            }
            env.put("com.sun.jndi.rmi.factory.socket", new SslRMIClientSocketFactory());
        }
        return env;
    }

    private String[] getCredentialsConfig()
    {
        return myCredentialsSupplier.get();
    }

    private Map<String, String> getTLSConfig()
    {
        return myTLSSupplier.get();
    }

    private boolean isAuthEnabled()
    {
        return getCredentialsConfig() != null;
    }

    private boolean isTLSEnabled()
    {
        return !getTLSConfig().isEmpty();
    }

    private Integer getJMXPort(final Node node)
    {
        SimpleStatement simpleStatement = SimpleStatement
            .builder("SELECT value FROM system_views.system_properties WHERE name = 'cassandra.jmx.remote.port';")
            .setNode(node)
            .build();
        Row row = mySession.execute(simpleStatement).one();
        if (row != null)
        {
            Integer port =Integer.parseInt(row.getString("value"));
            return port;
        }
        else
        {
            return DEFAULT_PORT;
        }
    }

    private void switchJmxConnection(final JMXConnector newJmxConnector) throws IOException
    {
        JMXConnector oldJmxConnector = myJmxConnection.getAndSet(newJmxConnector);

        if (oldJmxConnector != null)
        {
            oldJmxConnector.close();
        }
    }

    public void switchJmxConnection(final UUID nodeID, final JMXConnector newJmxConnector) throws IOException
    {
        JMXConnector oldJmxConnector = myJMXConnections.get(nodeID);
        myJMXConnections.put(nodeID, newJmxConnector);

        if (oldJmxConnector != null)
        {
            oldJmxConnector.close();
        }
    }

    private static boolean isConnected(final JMXConnector jmxConnector)
    {
        try
        {
            jmxConnector.getConnectionId();
        }
        catch (IOException e)
        {
            return false;
        }

        return true;
    }

    public static boolean isConnected(final UUID nodeID)
    {
        return isConnected(myJMXConnections.get(nodeID));
    }
}
