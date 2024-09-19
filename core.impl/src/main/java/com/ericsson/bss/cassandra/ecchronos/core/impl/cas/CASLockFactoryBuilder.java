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
package com.ericsson.bss.cassandra.ecchronos.core.impl.cas;

import com.ericsson.bss.cassandra.ecchronos.connection.DistributedNativeConnectionProvider;
import com.ericsson.bss.cassandra.ecchronos.core.state.HostStates;

public class CASLockFactoryBuilder
{
    private static final String DEFAULT_KEYSPACE_NAME = "ecchronos";
    private static final long DEFAULT_EXPIRY_TIME_IN_SECONDS = 30L;

    private DistributedNativeConnectionProvider myNativeConnectionProvider;
    private HostStates myHostStates;
    private String myKeyspaceName = DEFAULT_KEYSPACE_NAME;
    private long myCacheExpiryTimeInSeconds = DEFAULT_EXPIRY_TIME_IN_SECONDS;

    public final CASLockFactoryBuilder withNativeConnectionProvider(
            final DistributedNativeConnectionProvider nativeConnectionProvider)
    {
        myNativeConnectionProvider = nativeConnectionProvider;
        return this;
    }

    public final CASLockFactoryBuilder withHostStates(final HostStates hostStates)
    {
        myHostStates = hostStates;
        return this;
    }

    public final CASLockFactoryBuilder withKeyspaceName(final String keyspaceName)
    {
        myKeyspaceName = keyspaceName;
        return this;
    }

    public final CASLockFactoryBuilder withCacheExpiryInSeconds(final long cacheExpiryInSeconds)
    {
        myCacheExpiryTimeInSeconds = cacheExpiryInSeconds;
        return this;
    }

    public final CASLockFactory build()
    {
        if (myNativeConnectionProvider == null)
        {
            throw new IllegalArgumentException("Native connection provider cannot be null");
        }

        if (myHostStates == null)
        {
            throw new IllegalArgumentException("Host states cannot be null");
        }

        return new CASLockFactory(this);
    }

    public final DistributedNativeConnectionProvider getNativeConnectionProvider()
    {
        return myNativeConnectionProvider;
    }

    public final HostStates getHostStates()
    {
        return myHostStates;
    }


    public final String getKeyspaceName()
    {
        return myKeyspaceName;
    }

    public final long getCacheExpiryTimeInSecond()
    {
        return myCacheExpiryTimeInSeconds;
    }

}
