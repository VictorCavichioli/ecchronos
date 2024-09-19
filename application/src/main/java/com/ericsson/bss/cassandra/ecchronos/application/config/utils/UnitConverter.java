package com.ericsson.bss.cassandra.ecchronos.application.config.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UnitConverter
{
    private static final Pattern BYTE_PATTERN = Pattern.compile("^([0-9]+)([kKmMgG]?)$");

    private static final long ONE_KiB = 1024L;
    private static final long ONE_MiB = 1024L * ONE_KiB;
    private static final long ONE_GiB = 1024L * ONE_MiB;

    private UnitConverter()
    {
    }

    public static long toBytes(final String value)
    {
        Matcher matcher = BYTE_PATTERN.matcher(value);
        if (!matcher.matches())
        {
            throw new IllegalArgumentException("Unknown value " + value);
        }
        long baseValue = Long.parseLong(matcher.group(1));

        switch (matcher.group(2))
        {
        case "g":
        case "G":
            return baseValue * ONE_GiB;
        case "m":
        case "M":
            return baseValue * ONE_MiB;
        case "k":
        case "K":
            return baseValue * ONE_KiB;
        default: // Bytes
            return baseValue;
        }
    }
}

