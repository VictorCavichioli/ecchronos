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
package com.ericsson.bss.cassandra.ecchronos.connection.impl.common;

import java.util.UUID;

public class InitialContact
{
    private final String dataCenter;
    private final UUID hostId;

    public InitialContact(final String dcName, final UUID nodeID)
    {
        this.dataCenter = dcName;
        this.hostId = nodeID;
    }

    public final String getDataCenter()
    {
        return dataCenter;
    }

    public final UUID getHostId()
    {
        return hostId;
    }
}
