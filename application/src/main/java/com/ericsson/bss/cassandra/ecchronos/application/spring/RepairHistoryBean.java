package com.ericsson.bss.cassandra.ecchronos.application.spring;

import com.datastax.oss.driver.api.core.CqlSession;
import com.ericsson.bss.cassandra.ecchronos.application.config.Config;
import com.ericsson.bss.cassandra.ecchronos.application.config.repair.GlobalRepairConfig;
import com.ericsson.bss.cassandra.ecchronos.connection.DistributedNativeConnectionProvider;
import com.ericsson.bss.cassandra.ecchronos.core.impl.history.RepairHistoryProviderImpl;
import com.ericsson.bss.cassandra.ecchronos.core.metadata.NodeResolver;
import com.ericsson.bss.cassandra.ecchronos.core.repair.history.RepairHistory;
import com.ericsson.bss.cassandra.ecchronos.core.repair.history.RepairHistoryProvider;
import com.ericsson.bss.cassandra.ecchronos.core.state.ReplicationState;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepairHistoryBean
{
    private final RepairHistory repairHistory;
    private final RepairHistoryProvider repairHistoryProvider;

    public RepairHistoryBean(final Config configuration,
            final DistributedNativeConnectionProvider nativeConnectionProvider,
            final NodeResolver nodeResolver,
            final ReplicationState replicationState)
    {
        CqlSession session = nativeConnectionProvider.getCqlSession();

        GlobalRepairConfig repairConfig = configuration.getRepairConfig();
        repairHistoryProvider = createCassandraHistoryProvider(repairConfig, session, nodeResolver);
        repairHistory = RepairHistory.NO_OP;
    }

    @Bean
    public RepairHistory repairHistory()
    {
        return repairHistory;
    }

    @Bean
    public RepairHistoryProvider repairHistoryProvider()
    {
        return repairHistoryProvider;
    }

    private RepairHistoryProvider createCassandraHistoryProvider(final GlobalRepairConfig repairConfig,
            final CqlSession session,
            final NodeResolver nodeResolver)
    {
        return new RepairHistoryProviderImpl(nodeResolver, session,
                repairConfig.getRepairHistoryLookback().getInterval(TimeUnit.MILLISECONDS));
    }
}

