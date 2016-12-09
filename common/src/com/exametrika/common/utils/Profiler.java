/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.math.RoundingMode;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Locale;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.tasks.ThreadInterruptedException;



/**
 * The {@link Profiler} is a simple profiler.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public final class Profiler
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final double frequency;
    private static final long overhead;
    public static Profiler p = new Profiler();
    private final static NumberFormat format;
    private final String name;
    private final long outliersThreshold;
    private final int binWidthPower;
    private final int minBound;
    private long[] histogram;
    private long begin;
    private long sum;
    private long sumSquares;
    private long count;
    private long outliersCount;
    private volatile int barrier;
    
    static
    {
        String libName = "exaj";
        if (OSes.IS_64_BIT)
            libName += "-x64";
        
        System.loadLibrary(libName);
        
        long start = System.nanoTime();
        long s = rdtsc();
        
        try
        {
            Thread.sleep(500);
        }
        catch (InterruptedException e)
        {
            throw new ThreadInterruptedException(e);
        }
        
        long e = rdtsc();
        long end = System.nanoTime();

        frequency = (double)(e - s) / (end - start);
        
        s = rdtsc();
        int calibrateCount = 1000000;
        for (int k = 0; k < calibrateCount; k++)
            rdtsc();
        e = rdtsc();
        
        overhead = (e - s) / calibrateCount;
        
        format = createDefaultNumberFormat();
    }

    public Profiler()
    {
        this(null, 100000, 11, 12, 1000);
    }
    
    public Profiler(String name, long outliersThreshold, int binCount, int binWidthPower, int minBound)
    {
        this.name = name;
        
        this.outliersThreshold = (long)(outliersThreshold * frequency); 
        histogram = new long[binCount];
        this.binWidthPower = binWidthPower;
        this.minBound = minBound;
    }
    
    public static double getFrequency()
    {
        return frequency;
    }
    
    public static double getOverhead()
    {
        return overhead / frequency;
    }
    
    public long getCount()
    {
        return count + outliersCount;
    }
    
    public double getSum()
    {
        return sum / frequency + getAverage() * outliersCount;
    }
    
    public double getAverage()
    {
        if (count == 0)
            return 0;
        
        return (double)sum / count / frequency;
    }
    
    public double getDeviation()
    {
        if (count == 0)
            return 0;
        
        double averageSquare = (double)sumSquares / count / (frequency * frequency);
        double average = getAverage();
        
        return Math.sqrt(averageSquare - average * average);
    }
    
    public void begin()
    {
        barrier = 0;
        begin = rdtsc();
    }
    
    public void end()
    {
        long delta = rdtsc() - begin - overhead;
        barrier = 0;
        if (delta < 0)
            delta = 0;
        if (delta < outliersThreshold)
        {
            sum += delta;
            sumSquares += delta * delta;
            count++;
            
            int binIndex;
            if (delta < minBound)
                binIndex = 0;
            else
                binIndex = (int)((delta - minBound) >>> binWidthPower);
            if (binIndex >= histogram.length)
                binIndex = histogram.length - 1;
            histogram[binIndex]++;
        }
        else
            outliersCount++;
    }
    
    public void clear()
    {
        begin = 0;
        sum = 0;
        sumSquares = 0;
        count = 0;
        outliersCount = 0;
        histogram = new long[histogram.length];
    }
    
    public void print()
    {
        System.out.println(this);
    }
    
    @Override
    public String toString()
    {
        return messages.toString(name, getCount(), format.format(getAverage()), format.format(getDeviation()), 
            getSum(), outliersCount, percentilesString(true), percentilesString(false), histogramString()).toString();
    }

    public static native long rdtsc();
    
    private String histogramString()
    {
        StringBuilder builder = new StringBuilder();
        
        boolean first = true;
        for (int i = 0; i < histogram.length; i++)
        {
            if (first)
                first = false;
            else
                builder.append(", ");
            
            double percents = (double)histogram[i] * 100 / count;
            if (percents >= 5)
                builder.append(((i << binWidthPower) + minBound) + "(" + format.format(percents) + "%)");
            else
                builder.append('-');
        }
        
        return Strings.wrap(builder.toString(), 4, 120, ",", false);
    }

    private String percentilesString(boolean less)
    {
        StringBuilder builder = new StringBuilder();
        
        Object[] values = new Object[2];
        percentile(less, 30, values);
        builder.append(MessageFormat.format("30th({0}%) - {1}", format.format(((Double)values[0]).doubleValue()), values[1]));
        
        values = new Object[2];
        percentile(less, 50, values);
        builder.append(MessageFormat.format(", 50th({0}%) - {1}", format.format(((Double)values[0]).doubleValue()), values[1]));
        
        values = new Object[2];
        percentile(less, 70, values);
        builder.append(MessageFormat.format(", 70th({0}%) - {1}", format.format(((Double)values[0]).doubleValue()), values[1]));
        
        values = new Object[2];
        percentile(less, 90, values);
        builder.append(MessageFormat.format(", 90th({0}%) - {1}", format.format(((Double)values[0]).doubleValue()), values[1]));
        
        return builder.toString();
    }
    
    private void percentile(boolean less, double bound, Object[] values)
    {
        double percentsSum = 0;
        
        if (less)
        {
            for (int i = 0; i < histogram.length; i++)
            {
                percentsSum += (double)histogram[i] * 100 / count;
                if (percentsSum >= bound)
                {
                    values[0] = percentsSum;
                    values[1] = (i << binWidthPower) + minBound;
                    return;
                }
            }
        }
        else
        {
            for (int i = histogram.length - 1; i >= 0; i--)
            {
                percentsSum += (double)histogram[i] * 100 / count;
                if (percentsSum >= bound)
                {
                    values[0] = percentsSum;
                    values[1] = (i << binWidthPower) + minBound;
                    return;
                }
            }   
        }
        values[0] = 0d;
        values[1] = 0;
    }
    
    private static NumberFormat createDefaultNumberFormat()
    {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setGroupingUsed(true);
        format.setMaximumIntegerDigits(30);
        format.setMaximumFractionDigits(3);
        format.setMinimumFractionDigits(0);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format;
    }

    private interface IMessages
    {
        @DefaultMessage("Profiling {0}: count - {1}, average - {2}ns, stddev: {3}, sum - {4}, outliers: {5}\n    percentiles <=: {6}\n    percentiles >=: {7}\n    histogram: {8}.")
        ILocalizedMessage toString(String name, long count, String average, String deviation, double sum, long outliers, 
            String percentilesLess, String percentilesGreater, String histogram);
    }
}
