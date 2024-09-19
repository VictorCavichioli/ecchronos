package com.ericsson.bss.cassandra.ecchronos.application.config.scheduler;

import com.ericsson.bss.cassandra.ecchronos.application.config.repair.Interval;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.concurrent.TimeUnit;

public class SchedulerConfig
{
    private static final int THIRTY_SECONDS = 30;

    private Interval myFrequency = new Interval(THIRTY_SECONDS, TimeUnit.SECONDS);

    @JsonProperty("frequency")
    public final Interval getFrequency()
    {
        return myFrequency;
    }

    @JsonProperty("frequency")
    public final void setFrequency(final Interval frequency)
    {
        myFrequency = frequency;
    }
}
