package com.ericsson.bss.cassandra.ecchronos.application.spring;

import com.datastax.oss.driver.api.core.CqlSession;

import com.ericsson.bss.cassandra.ecchronos.application.config.Config;
import com.ericsson.bss.cassandra.ecchronos.connection.DistributedJmxConnectionProvider;
import com.ericsson.bss.cassandra.ecchronos.connection.DistributedNativeConnectionProvider;
import com.ericsson.bss.cassandra.ecchronos.core.impl.cas.CASLockFactory;
import com.ericsson.bss.cassandra.ecchronos.core.impl.jmx.DistributedJmxProxyFactoryImpl;
import com.ericsson.bss.cassandra.ecchronos.core.impl.metrics.CassandraMetrics;
import com.ericsson.bss.cassandra.ecchronos.core.impl.repair.scheduling.ScheduleManagerImpl;
import com.ericsson.bss.cassandra.ecchronos.core.impl.state.HostStatesImpl;
import com.ericsson.bss.cassandra.ecchronos.core.impl.table.ReplicatedTableProviderImpl;
import com.ericsson.bss.cassandra.ecchronos.core.impl.table.TableReferenceFactoryImpl;
import com.ericsson.bss.cassandra.ecchronos.core.jmx.DistributedJmxProxyFactory;
import com.ericsson.bss.cassandra.ecchronos.core.repair.scheduler.ScheduleManager;
import com.ericsson.bss.cassandra.ecchronos.core.state.HostStates;
import com.ericsson.bss.cassandra.ecchronos.core.table.*;
import java.io.Closeable;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ECChronosInternals implements Closeable
{
    private static final Logger LOG = LoggerFactory.getLogger(ECChronosInternals.class);
    private static final NoOpRepairMetrics NO_OP_REPAIR_METRICS = new NoOpRepairMetrics();

    private final ScheduleManagerImpl myScheduleManagerImpl;
    private final HostStatesImpl myHostStatesImpl;
    private final ReplicatedTableProviderImpl myReplicatedTableProvider;
    private final TableReferenceFactory myTableReferenceFactory;
    private final DistributedJmxProxyFactory myJmxProxyFactory;
    private final CASLockFactory myLockFactory;
    private final CassandraMetrics myCassandraMetrics;

    public ECChronosInternals(
            final Config configuration,
            final DistributedNativeConnectionProvider nativeConnectionProvider,
            final DistributedJmxConnectionProvider jmxConnectionProvider)
    {
        myJmxProxyFactory = DistributedJmxProxyFactoryImpl.builder()
                .withJmxConnectionProvider(jmxConnectionProvider)
                .build();

        myHostStatesImpl = HostStatesImpl.builder()
                .withJmxProxyFactory(myJmxProxyFactory)
                .build();

        CqlSession session = nativeConnectionProvider.getCqlSession();

        myTableReferenceFactory = new TableReferenceFactoryImpl(session);

        myReplicatedTableProvider = new ReplicatedTableProviderImpl(session, myTableReferenceFactory);

        myLockFactory = CASLockFactory.builder()
                .withNativeConnectionProvider(nativeConnectionProvider)
                .withHostStates(myHostStatesImpl)
                .withKeyspaceName("ecchronos")
                .withCacheExpiryInSeconds(10)
                .build();

        myCassandraMetrics = new CassandraMetrics(myJmxProxyFactory);
        myScheduleManagerImpl = ScheduleManagerImpl.builder()
                .withLockFactory(myLockFactory)
                .withRunInterval(configuration.getSchedulerConfig().getFrequency().getInterval(TimeUnit.MILLISECONDS),
                        TimeUnit.MILLISECONDS)
                .withNodeIDList(jmxConnectionProvider.getJmxConnections().keySet())
                .build();
    }

    public final TableReferenceFactory getTableReferenceFactory()
    {
        return myTableReferenceFactory;
    }

    public final HostStates getHostStates()
    {
        return myHostStatesImpl;
    }

    public final ReplicatedTableProvider getReplicatedTableProvider()
    {
        return myReplicatedTableProvider;
    }

    public final ScheduleManager getScheduleManager()
    {
        return myScheduleManagerImpl;
    }

    public final DistributedJmxProxyFactory getJmxProxyFactory()
    {
        return myJmxProxyFactory;
    }

    public final CassandraMetrics getCassandraMetrics()
    {
        return myCassandraMetrics;
    }

    public final TableRepairMetrics getTableRepairMetrics()
    {
        return NO_OP_REPAIR_METRICS;
    }

    @Override
    public final void close()
    {
        myScheduleManagerImpl.close();

        myLockFactory.close();

        myHostStatesImpl.close();

        myCassandraMetrics.close();
    }

    private static class NoOpRepairMetrics implements TableRepairMetrics
    {

        @Override
        public void repairState(final TableReference tableReference,
                final int repairedRanges,
                final int notRepairedRanges)
        {
            LOG.trace("Updated repair state of {}, {}/{} repaired ranges", tableReference, repairedRanges,
                    notRepairedRanges);
        }

        @Override
        public void lastRepairedAt(final TableReference tableReference, final long lastRepairedAt)
        {
            LOG.debug("Table {} last repaired at {}", tableReference, lastRepairedAt);
        }

        @Override
        public void remainingRepairTime(final TableReference tableReference, final long remainingRepairTime)
        {
            LOG.debug("Table {} remaining repair time {}", tableReference, remainingRepairTime);
        }

        @Override
        public void repairSession(final TableReference tableReference,
                final long timeTaken,
                final TimeUnit timeUnit,
                final boolean successful)
        {
            if (LOG.isTraceEnabled())
            {
                LOG.trace("Repair timing for table {} {}ms, it was {}", tableReference,
                        timeUnit.toMillis(timeTaken), successful ? "successful" : "not successful");
            }
        }
    }
}
