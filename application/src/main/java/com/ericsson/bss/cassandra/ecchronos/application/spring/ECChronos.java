package com.ericsson.bss.cassandra.ecchronos.application.spring;

import com.ericsson.bss.cassandra.ecchronos.application.config.Config;
import com.ericsson.bss.cassandra.ecchronos.application.config.repair.FileBasedRepairConfiguration;
import com.ericsson.bss.cassandra.ecchronos.application.config.repair.GlobalRepairConfig;
import com.ericsson.bss.cassandra.ecchronos.application.exceptions.ConfigurationException;
import com.ericsson.bss.cassandra.ecchronos.connection.DistributedJmxConnectionProvider;
import com.ericsson.bss.cassandra.ecchronos.connection.DistributedNativeConnectionProvider;
import com.ericsson.bss.cassandra.ecchronos.core.impl.repair.DefaultRepairConfigurationProvider;
import com.ericsson.bss.cassandra.ecchronos.core.impl.repair.RepairSchedulerImpl;
import com.ericsson.bss.cassandra.ecchronos.core.impl.repair.state.RepairStateFactoryImpl;
import com.ericsson.bss.cassandra.ecchronos.core.impl.table.TimeBasedRunPolicy;
import com.ericsson.bss.cassandra.ecchronos.core.repair.scheduler.RepairScheduler;
import com.ericsson.bss.cassandra.ecchronos.core.state.ReplicationState;
import com.ericsson.bss.cassandra.ecchronos.core.table.ReplicatedTableProvider;
import com.ericsson.bss.cassandra.ecchronos.core.table.TableReferenceFactory;
import java.io.Closeable;
import java.util.Collections;

import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ECChronos implements Closeable
{
    private final ECChronosInternals myECChronosInternals;
    private final RepairSchedulerImpl myRepairSchedulerImpl;
    private final TimeBasedRunPolicy myTimeBasedRunPolicy;

    public ECChronos(
            final Config configuration,
            final ApplicationContext applicationContext,
            final DistributedNativeConnectionProvider nativeConnectionProvider,
            final DistributedJmxConnectionProvider jmxConnectionProvider,
            final ReplicationState replicationState,
            final DefaultRepairConfigurationProvider defaultRepairConfigurationProvider)
            throws ConfigurationException
    {
        myECChronosInternals = new ECChronosInternals(configuration, nativeConnectionProvider, jmxConnectionProvider);

        CqlSession session = nativeConnectionProvider.getCqlSession();

        myTimeBasedRunPolicy = TimeBasedRunPolicy.builder()
                .withSession(session)
                .withKeyspaceName("ecchronos")
                .build();

        myRepairSchedulerImpl = RepairSchedulerImpl.builder()
                .withJmxProxyFactory(myECChronosInternals.getJmxProxyFactory())
                .withScheduleManager(myECChronosInternals.getScheduleManager())
                .withTableRepairMetrics(myECChronosInternals.getTableRepairMetrics())
                .withCassandraMetrics(myECChronosInternals.getCassandraMetrics())
                .withReplicationState(replicationState)
                .withRepairPolicies(Collections.singletonList(myTimeBasedRunPolicy))
                .withCassandraMetrics(myECChronosInternals.getCassandraMetrics())
                .build();

        AbstractRepairConfigurationProvider repairConfigurationProvider = new FileBasedRepairConfiguration(applicationContext);

        defaultRepairConfigurationProvider.fromBuilder(DefaultRepairConfigurationProvider.newBuilder()
                .withRepairScheduler(myRepairSchedulerImpl)
                .withSession(session)
                .withNodesList(nativeConnectionProvider.getNodes())
                .withReplicatedTableProvider(myECChronosInternals.getReplicatedTableProvider())
                .withRepairConfiguration(repairConfigurationProvider::get)
                .withTableReferenceFactory(myECChronosInternals.getTableReferenceFactory()));

    }

    @Bean
    public TableReferenceFactory tableReferenceFactory()
    {
        return myECChronosInternals.getTableReferenceFactory();
    }

    @Bean(destroyMethod = "")
    public RepairScheduler repairScheduler()
    {
        return myRepairSchedulerImpl;
    }

    @Bean
    public ReplicatedTableProvider replicatedTableProvider()
    {
        return myECChronosInternals.getReplicatedTableProvider();
    }

    @Override
    public final void close()
    {

        myRepairSchedulerImpl.close();
        myECChronosInternals.close();
    }
}

