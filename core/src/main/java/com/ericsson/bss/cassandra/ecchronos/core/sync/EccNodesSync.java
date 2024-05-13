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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.bindMarker;
import java.time.Instant;
import java.util.UUID;
import java.net.InetAddress;
import java.net.UnknownHostException;

// CREATE TABLE ecchronos.nodes_sync
// (
//     ecchronos_id TEXT,
//     datacenter_name TEXT,
//     node_id UUID,
//     node_ip TEXT,
//     cql_port int,
//     jmx_port int,
//     last_connection TIMESTAMP,
//     next_connection TIMESTAMP,
//     PRIMARY KEY
//     (
//         ecchronos_id,
//         datacenter_name,
//         node_ip
//     )
// ) WITH CLUSTERING ORDER BY(
//     datacenter_name DESC,
//     node_ip DESC
// );

public final class EccNodesSync
{
    private static final Logger LOG = LoggerFactory.getLogger(EccNodesSync.class);
    private static final int DEFAULT_JMX_PORT = 7100;
    private static final String DEFAULT_TABLE_NAME = "nodes_sync";
    private static final String COLUMN_ECCHRONOS_ID = "ecchronos_id";
    private static final String COLUMN_DC_NAME = "datacenter_name";
    private static final String COLUMN_NODE_ID = "node_id";
    private static final String COLUMN_NODE_IP = "node_ip";
    private static final String COLUMN_NODE_CQL_PORT = "cql_port";
    private static final String COLUMN_NODE_JMX_PORT = "jmx_port";
    private static final String COLUMN_LAST_CONNECTION = "last_connection";
    private static final String COLUMN_NEXT_CONNECTION = "next_connection";

    private final CqlSession session;

    private final PreparedStatement myCompeteStatement;
    private final PreparedStatement myUpdateStatement;

    private final Builder myNodesSyncBuilder;

    public EccNodesSync(final Builder builder)
    {
        myNodesSyncBuilder = builder;
        session = Preconditions.checkNotNull(builder.mySession,
                "Session cannot be null");

        myCompeteStatement = session.prepare(competeStatement());
        myUpdateStatement = session.prepare(updateStatement());
    }

    public ResultSet acquireNode(final String host, final int port, final Node node) throws UnknownHostException
    {
        try
        {
            String ecchronosID = "ecchronos-" + InetAddress.getLocalHost().getHostName();

            ResultSet insertStatement = insertNodeInfo(
                ecchronosID,
                node.getDatacenter().toString(),
                node.getHostId(),
                host,
                port,
                DEFAULT_JMX_PORT,
                Instant.now(),
                Instant.now()
                );

            return insertStatement;
        }
        catch (UnknownHostException e)
        {
            throw new UnknownHostException(e.getMessage());
        }
    }


    public ResultSet updateNode(
        final String datacenter,
        final String nodeIP,
        final int port) throws UnknownHostException
    {
        try
        {
            String ecchronosID = "ecchronos-" + InetAddress.getLocalHost().getHostName();

            ResultSet updateStatement = updateJmxPort(
                ecchronosID,
                datacenter,
                nodeIP,
                port
                );

            return updateStatement;
        }
        catch (UnknownHostException e)
        {
            throw new UnknownHostException(e.getMessage());
        }
    }

    public ResultSet insertNodeInfo(
        final String ecchronosID,
        final String datacenter,
        final UUID nodeID,
        final String nodeIP,
        final int cqlPort,
        final int jmxPort,
        final Instant lastConnection,
        final Instant nextConnection
    )
    {
        BoundStatement insertNodeSyncInfo = myCompeteStatement
            .bind(
                ecchronosID,
                datacenter,
                nodeID,
                nodeIP,
                cqlPort,
                jmxPort,
                lastConnection,
                nextConnection
            );
        return execute(insertNodeSyncInfo);
    }

    public ResultSet updateJmxPort(
        final String ecchronosID,
        final String datacenter,
        final String nodeIP,
        final int jmxPort
    )
    {
        BoundStatement updateJmxPort = myUpdateStatement
            .bind(
                jmxPort,
                ecchronosID,
                datacenter,
                nodeIP
            );
        return execute(updateJmxPort);
    }

    public ResultSet execute(final BoundStatement statement)
    {
        Statement executeStatement = statement;

        // if (dataCenter != null)
        // {
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
                .value(COLUMN_NODE_ID, bindMarker())
                .value(COLUMN_NODE_IP, bindMarker())
                .value(COLUMN_NODE_CQL_PORT, bindMarker())
                .value(COLUMN_NODE_JMX_PORT, bindMarker())
                .value(COLUMN_LAST_CONNECTION, bindMarker())
                .value(COLUMN_NEXT_CONNECTION, bindMarker())
                .build()
                .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
        return competeStatement;
    }

    private SimpleStatement updateStatement()
    {
        SimpleStatement updateStatement = QueryBuilder
            .update(myNodesSyncBuilder.myKeyspaceName, myNodesSyncBuilder.myTableName)
            .setColumn(COLUMN_NODE_JMX_PORT, bindMarker())
            .whereColumn(COLUMN_ECCHRONOS_ID)
            .isEqualTo(bindMarker())
            .whereColumn(COLUMN_DC_NAME)
            .isEqualTo(bindMarker())
            .whereColumn(COLUMN_NODE_IP)
            .isEqualTo(bindMarker())
            .build()
            .setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM);
        return updateStatement;
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
