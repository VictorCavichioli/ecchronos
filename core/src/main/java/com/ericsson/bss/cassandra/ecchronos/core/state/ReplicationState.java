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
package com.ericsson.bss.cassandra.ecchronos.core.state;

import com.datastax.oss.driver.api.core.metadata.Node;
import com.ericsson.bss.cassandra.ecchronos.core.metadata.DriverNode;
import com.ericsson.bss.cassandra.ecchronos.core.table.TableReference;
import java.util.Map;

import com.ericsson.bss.cassandra.ecchronos.core.utils.LongTokenRange;
import com.google.common.collect.ImmutableSet;

/**
 * Replication state interface used to retrieve mappings between token range to responsible nodes.
 *
 * Within a keyspace the methods are expected to return the exact same object instance for a set of nodes.
 */
public interface ReplicationState
{
    /**
     * Get the nodes that are responsible for the provided token range.
     * The provided token range can be a sub range of an existing one.
     *
     * @param tableReference The table used to calculate the proper replication.
     * @param tokenRange The token range to get nodes for.
     * @return The responsible nodes or null if either the token range does not exist or is intersecting two ranges.
     */
    ImmutableSet<DriverNode> getNodes(TableReference tableReference, LongTokenRange tokenRange, Node currentNode);

    /**
     * Get the nodes that are a replica for the provided table that have ranges in common with the local node.
     *
     * @param tableReference The table to fetch replicas for.
     * @return The replicas for the table
     */
    ImmutableSet<DriverNode> getReplicas(TableReference tableReference, Node currentNode);

    /**
     * Get the nodes that are responsible for the provided token range, check clusterwide.
     * The provided token range can be a sub range of an existing one.
     *
     * @param tableReference The table used to calculate the proper replication.
     * @param tokenRange The token range to get nodes for.
     * @return The responsible nodes or null if either the token range does not exist or is intersecting two ranges.
     */
    ImmutableSet<DriverNode> getNodesClusterWide(TableReference tableReference, LongTokenRange tokenRange, Node currentNode);

    /**
     * Get a map of the current replication state for the provided table.
     *
     * @param tableReference
     *            The table used to calculate the proper replication.
     * @return The map consisting of token -&gt; responsible nodes.
     */
    Map<LongTokenRange, ImmutableSet<DriverNode>> getTokenRangeToReplicas(TableReference tableReference, Node currentNode);

    Map<LongTokenRange, ImmutableSet<DriverNode>> getTokenRanges(TableReference tableReference, Node currentNode);
}
