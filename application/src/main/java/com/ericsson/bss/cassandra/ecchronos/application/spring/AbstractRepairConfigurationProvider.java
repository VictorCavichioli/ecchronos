package com.ericsson.bss.cassandra.ecchronos.application.spring;

import com.ericsson.bss.cassandra.ecchronos.application.config.Config;
import com.ericsson.bss.cassandra.ecchronos.core.repair.config.RepairConfiguration;
import com.ericsson.bss.cassandra.ecchronos.core.table.TableReference;
import java.util.HashSet;
import java.util.Set;

import org.springframework.context.ApplicationContext;

public abstract class AbstractRepairConfigurationProvider
{
    private final ApplicationContext applicationContext;

    public final ApplicationContext getApplicationContext()
    {
        return applicationContext;
    }

    private final RepairConfiguration defaultRepairConfiguration;

    protected AbstractRepairConfigurationProvider(final ApplicationContext anApplicationContext)
    {
        this.applicationContext = anApplicationContext;

        Config config = applicationContext.getBean(Config.class);
        this.defaultRepairConfiguration = config.getRepairConfig().asRepairConfiguration();
    }

    public final Set<RepairConfiguration> get(final TableReference tableReference)
    {
        Set<RepairConfiguration> repairConfigurations = new HashSet<>();
        repairConfigurations.addAll(forTable(tableReference));
        if (repairConfigurations.isEmpty())
        {
            repairConfigurations.add(defaultRepairConfiguration);
        }
        return repairConfigurations;
    }

    public abstract Set<RepairConfiguration> forTable(TableReference tableReference);
}

