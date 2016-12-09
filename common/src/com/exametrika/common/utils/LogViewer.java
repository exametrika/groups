/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The {@link LogViewer} is used to view log files.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public class LogViewer
{
    public static void main(String[] args)
    {
        if (args.length < 1)
        {
            System.out.println("Usage:");
            System.out.println("    java LogViwer log_file_name [filter_reg_exp] [start_with_reg_exp] [log_entry_terminator]");
            System.out.println("        log_file_name        - file name of log");
            System.out.println("        filter_reg_exp       - regular expression of filter applied to log entries");
            System.out.println("        start_with_reg_exp   - regular expression of condition used to start log entries with");
            System.out.println("        log_entry_terminator - string that marks end of log entry (by default is ' ')");
            return;
        }

        System.out.println("\u001bc");

        Pattern filterPattern = null;
        if (args.length > 1)
            filterPattern = Pattern.compile(args[1], Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

        Pattern startPattern = null;
        if (args.length > 2)
            startPattern = Pattern.compile(args[2], Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

        String entryTerminator;
        if (args.length > 3)
            entryTerminator = args[3];
        else
            entryTerminator = " ";

        int count = 0;
        BufferedReader reader = null;

        try
        {
            reader = new BufferedReader(new FileReader(args[0]));

            String[] entry = new String[2];
            boolean start = startPattern == null;
            while (true)
            {
                if (!readEntry(reader, entryTerminator, entry))
                    break;

                if (!start)
                {
                    Matcher matcher = startPattern.matcher(entry[0]);
                    if (!matcher.matches())
                        continue;

                    start = true;
                }

                if (filterPattern != null)
                {
                    Matcher matcher = filterPattern.matcher(entry[0]);
                    if (!matcher.matches())
                        continue;
                }

                System.out.print(entry[1]);
                System.out.print("\n\n");
                count++;
            }
            
            System.out.println("\u001b[36;1mTotal entries:" + count + "\u001b[0m");
        }
        catch (IOException e)
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (IOException e2)
                {
                    e2.printStackTrace();
                }
            }
            e.printStackTrace();
            return;
        }
    }

    private static boolean readEntry(BufferedReader reader, String entryTerminator, String[] entry) throws IOException
    {
        StringBuilder printBuilder = null;
        StringBuilder searchBuilder = null;

        while (true)
        {
            String line = reader.readLine();
            if (line == null || line.equals(entryTerminator))
                break;

            if (printBuilder == null)
            {
                printBuilder = new StringBuilder();
                printBuilder.append("\u001b[36;1m");
                printBuilder.append(line);
                printBuilder.append("\u001b[0m");

                searchBuilder = new StringBuilder();
                searchBuilder.append(line);
            }
            else
            {
                printBuilder.append('\n');
                printBuilder.append(line);

                searchBuilder.append('\n');
                searchBuilder.append(line);
            }
        }

        if (printBuilder == null)
            return false;

        entry[0] = searchBuilder.toString();
        entry[1] = printBuilder.toString();
        return true;
    }
}