package com.ericsson.bss.cassandra.ecchronos.application.config.connection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JMXAgentConfig
{
    private static boolean agentEnabled;

    public JMXAgentConfig()
    {

    }

    @JsonCreator
    public JMXAgentConfig(
        @JsonProperty("enabled") final boolean enabled)
    {
        agentEnabled = enabled;
    }

    @JsonProperty("enabled")
    public final void setAgentEnabled(final boolean enabled)
    {
        agentEnabled = enabled;
    }

    @JsonProperty("enabled")
    public final boolean isAgentEnabled()
    {
        return agentEnabled;
    }
}
