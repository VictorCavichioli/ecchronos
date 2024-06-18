package com.ericsson.bss.cassandra.ecchronos.core.sync;

import com.datastax.oss.driver.api.core.ConsistencyLevel;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.ericsson.bss.cassandra.ecchronos.connection.StatementDecorator;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.datastax.oss.driver.api.core.metadata.Node;
import com.google.common.base.Preconditions;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.bindMarker;
import java.time.Instant;
import java.util.UUID;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.temporal.ChronoUnit;

// CREATE TABLE ecchronos.nodes_sync
// (
//     ecchronos_id TEXT,
//     datacenter_name TEXT,
//     node_id UUID,
//     node_endpoint TEXT,
//     node_status TEXT,
//     last_connection TIMESTAMP,
//     next_connection TIMESTAMP,
//     PRIMARY KEY
//     (
//         ecchronos_id,
//         datacenter_name,
//         node_endpoint,
//         node_status
//     )
// ) WITH CLUSTERING ORDER BY(
//     datacenter_name DESC,
//     node_endpoint DESC,
//     node_status DESC
// );

public final class EccNodesSync
{
    private static final Integer DEFAULT_CONNECTION_DELAY_IN_MI = 30;
    private static final String DEFAULT_TABLE_NAME = "nodes_sync";
    private static final String COLUMN_ECCHRONOS_ID = "ecchronos_id";
    private static final String COLUMN_DC_NAME = "datacenter_name";
    private static final String COLUMN_NODE_ID = "node_id";
    private static final String COLUMN_NODE_ENDPOINT = "node_endpoint";
    private static final String COLUMN_NODE_STATUS = "node_status";
    private static final String COLUMN_LAST_CONNECTION = "last_connection";
    private static final String COLUMN_NEXT_CONNECTION = "next_connection";

    private final CqlSession session;

    private final PreparedStatement myCompeteStatement;

    private final Builder myNodesSyncBuilder;

    public EccNodesSync(final Builder builder)
    {
        myNodesSyncBuilder = builder;
        session = Preconditions.checkNotNull(builder.mySession,
                "Session cannot be null");

        myCompeteStatement = session.prepare(competeStatement());
    }

    public ResultSet acquireNode(
        final String endpoint,
        final Node node) throws UnknownHostException
    {
        try
        {
            String ecchronosID = "ecchronos-" + InetAddress.getLocalHost().getHostName();

            ResultSet insertStatement = insertNodeInfo(
                ecchronosID,
                node.getDatacenter().toString(),
                endpoint,
                node.getState().toString(),
                Instant.now(),
                Instant.now().plus(DEFAULT_CONNECTION_DELAY_IN_MI, ChronoUnit.MINUTES),
                node.getHostId()
                );
            return insertStatement;
        }
        catch (UnknownHostException e)
        {
            throw new UnknownHostException(e.getMessage());
        }
    }

    public ResultSet insertNodeInfo(
        final String ecchronosID,
        final String datacenterName,
        final String nodeEndpoint,
        final String nodeStatus,
        final Instant lastConnection,
        final Instant nextConnection,
        final UUID nodeID
    )
    {
        BoundStatement insertNodeSyncInfo = myCompeteStatement
            .bind(
                ecchronosID,
                datacenterName,
                nodeEndpoint,
                nodeStatus,
                lastConnection,
                nextConnection,
                nodeID
            );
        return execute(insertNodeSyncInfo);
    }

    public ResultSet execute(final BoundStatement statement)
    {
        Statement executeStatement = statement;

        // if (dataCenter != null)
        // {
        // TO DO
        //     executeStatement = statement;
        // }

        return session.execute(myNodesSyncBuilder.myStatementDecorator.apply(executeStatement));
    }

    private SimpleStatement competeStatement()
    {
        SimpleStatement competeStatement = QueryBuilder
                .insertInto(myNodesSyncBuilder.myKeyspaceName, myNodesSyncBuilder.myTableName)
                .value(COLUMN_ECCHRONOS_ID, bindMarker())
                .value(COLUMN_DC_NAME, bindMarker())
                .value(COLUMN_NODE_ENDPOINT, bindMarker())
                .value(COLUMN_NODE_STATUS, bindMarker())
                .value(COLUMN_LAST_CONNECTION, bindMarker())
                .value(COLUMN_NEXT_CONNECTION, bindMarker())
                .value(COLUMN_NODE_ID, bindMarker())
                .build()
                .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
        return competeStatement;
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private CqlSession mySession;
        private String myKeyspaceName = "ecchronos";
        private String myTableName = DEFAULT_TABLE_NAME;
        private StatementDecorator myStatementDecorator;

        /**
         * Build ECC sync with session.
         *
         * @param session Session.q
         * @return Builder
         */
        public Builder withSession(final CqlSession session)
        {
            mySession = session;
            return this;
        }

        /**
         * Build ECC sync with keyspace.
         *
         * @param keyspaceName Keyspace.
         * @return Builder
         */
        public Builder withKeyspace(final String keyspaceName)
        {
            myKeyspaceName = keyspaceName;
            return this;
        }

        /**
         * Build ECC sync with table.
         *
         * @param tableName table.
         * @return Builder
         */
        public Builder withTable(final String tableName)
        {
            myTableName = tableName;
            return this;
        }

        /**
         * Build ECC sync with table.
         *
         * @param tableName table.
         * @return Builder
         */
        public Builder withStatementDecorator(final StatementDecorator statementDecorator)
        {
            myStatementDecorator = statementDecorator;
            return this;
        }

        /**
         * Build ECC sync.
         *
         * @return Builder
         */
        public EccNodesSync build()
        {
            return new EccNodesSync(this);
        }
    }
}
