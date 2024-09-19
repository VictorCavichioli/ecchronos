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

import java.util.concurrent.ScheduledExecutorService;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;

/**
 * Represents a container for builder configurations and state for the CASLockStatement.
 * This class is used to decouple builder fields from CASLock to avoid excessive field count.
 */
public class CASLockProperties
{
    private final String myKeyspaceName;
    private final ScheduledExecutorService myExecutor;
    private final ConsistencyLevel mySerialConsistencyLevel;
    private final CqlSession mySession;

    CASLockProperties(
            final String keyspaceName,
            final ScheduledExecutorService executor,
            final CqlSession session)
    {
        myKeyspaceName = keyspaceName;
        myExecutor = executor;
        mySerialConsistencyLevel = ConsistencyLevel.SERIAL;
        mySession = session;
    }



    public final String getKeyspaceName()
    {
        return myKeyspaceName;
    }

    public final ScheduledExecutorService getExecutor()
    {
        return myExecutor;
    }

    public final ConsistencyLevel getSerialConsistencyLevel()
    {
        return mySerialConsistencyLevel;
    }

    public final CqlSession getSession()
    {
        return mySession;
    }

}

