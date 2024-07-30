/*
 * Copyright 2023 Telefonaktiebolaget LM Ericsson
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
package com.ericsson.bss.cassandra.ecchronos.application.config.connection;

import com.ericsson.bss.cassandra.ecchronos.application.AgentJmxConnectionProvider;
import com.ericsson.bss.cassandra.ecchronos.application.AgentNativeConnectionProvider;
import com.ericsson.bss.cassandra.ecchronos.application.DefaultJmxConnectionProvider;
import com.ericsson.bss.cassandra.ecchronos.application.config.Config;
import com.ericsson.bss.cassandra.ecchronos.connection.JmxConnectionProvider;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.function.Supplier;

public class JmxConnection extends Connection<JmxConnectionProvider>
{
    private static final int DEFAULT_PORT = 7199;
    private JMXAgentConfig myJmxAgentConfig = new JMXAgentConfig();

    public JmxConnection()
    {
        try
        {
            if (myJmxAgentConfig.isAgentEnabled())
            {
                setProvider(AgentJmxConnectionProvider.class);
            }
            else
            {
                setProvider(DefaultJmxConnectionProvider.class);
            }
            setPort(DEFAULT_PORT);
        }
        catch (NoSuchMethodException ignored)
        {
            // Do something useful ...
        }
    }

    @JsonProperty("agent")
    public final JMXAgentConfig getJMXAgentConfig()
    {
        return myJmxAgentConfig;
    }

    @JsonProperty("agent")
    public final void setJMXAgentConfig(final JMXAgentConfig jmxAgentConfig)
    {
        myJmxAgentConfig = jmxAgentConfig;
    }

    @Override
    protected final Class<?>[] expectedConstructor()
    {
        if (myJmxAgentConfig.isAgentEnabled())
        {
            return new Class<?>[]
                {
                        Config.class,
                        Supplier.class,
                        AgentNativeConnectionProvider.class
                };
        }
        return new Class<?>[]
                {
                        Config.class, Supplier.class
                };
    }
}
