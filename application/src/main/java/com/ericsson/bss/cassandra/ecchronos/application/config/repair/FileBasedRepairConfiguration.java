package com.ericsson.bss.cassandra.ecchronos.application.config.repair;

import com.ericsson.bss.cassandra.ecchronos.application.exceptions.ConfigurationException;
import com.ericsson.bss.cassandra.ecchronos.application.spring.AbstractRepairConfigurationProvider;
import com.ericsson.bss.cassandra.ecchronos.core.repair.config.RepairConfiguration;
import com.ericsson.bss.cassandra.ecchronos.core.table.TableReference;
import java.util.Set;

import org.springframework.context.ApplicationContext;

import com.ericsson.bss.cassandra.ecchronos.application.config.ConfigurationHelper;

import com.google.common.annotations.VisibleForTesting;

public class FileBasedRepairConfiguration extends AbstractRepairConfigurationProvider
{
    private static final String CONFIGURATION_FILE = "schedule.yml";

    private final RepairSchedule repairSchedule;

    public FileBasedRepairConfiguration(final ApplicationContext applicationContext) throws ConfigurationException
    {
        this(applicationContext, ConfigurationHelper.DEFAULT_INSTANCE, CONFIGURATION_FILE);
    }

    @VisibleForTesting
    FileBasedRepairConfiguration(final ApplicationContext applicationContext,
            final ConfigurationHelper configurationHelper,
            final String configurationFile) throws ConfigurationException
    {
        super(applicationContext);

        repairSchedule = configurationHelper.getConfiguration(configurationFile, RepairSchedule.class);
    }

    @Override
    public final Set<RepairConfiguration> forTable(final TableReference tableReference)
    {
        return repairSchedule.getRepairConfigurations(tableReference.getKeyspace(), tableReference.getTable());
    }
}