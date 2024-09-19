package com.ericsson.bss.cassandra.ecchronos.application.config.repair;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Interval
{
    private long myTime;
    private TimeUnit myUnit;

    public Interval()
    {
        // Default constructor for jackson
    }

    public Interval(final long time, final TimeUnit timeUnit)
    {
        myTime = time;
        myUnit = timeUnit;
    }

    public final long getInterval(final TimeUnit timeUnit)
    {
        return timeUnit.convert(myTime, myUnit);
    }

    @JsonProperty("time")
    public final long getTime()
    {
        return myTime;
    }

    @JsonProperty("time")
    public final void setTime(final long time)
    {
        myTime = time;
    }

    @JsonProperty("unit")
    public final TimeUnit getUnit()
    {
        return myUnit;
    }

    @JsonProperty("unit")
    public final void setUnit(final String unit)
    {
        myUnit = TimeUnit.valueOf(unit.toUpperCase(Locale.US));
    }
}

