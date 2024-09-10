/*
 * Copyright 2024 Telefonaktiebolaget LM Ericsson
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

import com.ericsson.bss.cassandra.ecchronos.application.providers.AgentJmxConnectionProvider;
import com.ericsson.bss.cassandra.ecchronos.connection.DistributedJmxConnectionProvider;

import com.ericsson.bss.cassandra.ecchronos.connection.DistributedNativeConnectionProvider;
import com.ericsson.bss.cassandra.ecchronos.data.sync.EccNodesSync;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.function.Supplier;

public class DistributedJmxConnection extends Connection<DistributedJmxConnectionProvider>
{
    private RetryPolicyConfig myRetryPolicyConfig = new RetryPolicyConfig();

    public DistributedJmxConnection()
    {
        try
        {
            setProvider(AgentJmxConnectionProvider.class);
        }
        catch (NoSuchMethodException ignored)
        {
            // Do something useful ...
        }
    }

    @JsonProperty("retryPolicy")
    public final RetryPolicyConfig getRetryPolicyConfig()
    {
        return myRetryPolicyConfig;
    }

    @JsonProperty("retryPolicy")
    public final void setRetryPolicyConfig(final RetryPolicyConfig retryPolicyConfig)
    {
        myRetryPolicyConfig = retryPolicyConfig;
    }

    /**
     * @return
     */
    @Override
    protected Class<?>[] expectedConstructor()
    {
        return new Class<?>[] {
                                Supplier.class,
                                DistributedNativeConnectionProvider.class,
                                EccNodesSync.class
        };
    }
}
