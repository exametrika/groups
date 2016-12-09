/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import sun.misc.Unsafe;



/**
 * The {@link Fields} contains different utility methods for accessing object fields.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Fields
{
    private static final Unsafe unsafe = Classes.getUnsafe();
    
    public interface IField
    {
        <T> T getObject(Object instance);
        
        void setObject(Object instance, Object value);
        
        boolean getBoolean(Object instance);
        
        void setBoolean(Object instance, boolean value);
        
        byte getByte(Object instance);
        
        void setByte(Object instance, byte value);
        
        char getChar(Object instance);
        
        void setChar(Object instance, char value);
        
        short getShort(Object instance);
        
        void setShort(Object instance, short value);
        
        int getInt(Object instance);
        
        void setInt(Object instance, int value);
        
        long getLong(Object instance);
        
        void setLong(Object instance, long value);
        
        float getFloat(Object instance);
        
        void setFloat(Object instance, float value);

        double getDouble(Object instance);
        
        void setDouble(Object instance, double value);
    }
    
    public static IField get(String className, String fieldName)
    {
        try
        {
            Class clazz = Class.forName(className);
            return get(clazz, fieldName);
        }
        catch (Throwable e)
        {
            return null;
        }
    }
    
    public static IField get(Class<?> clazz, String fieldName)
    {
        Field field = getField(clazz, fieldName);
        if (field != null)
            return new UnsafeField(field);
        else
            return null;
    }
    
    public static Field getField(String className, String fieldName)
    {
        try
        {
            Class clazz = Class.forName(className);
            return getField(clazz, fieldName);
        }
        catch (Throwable e)
        {
            return null;
        }
    }
    
    public static Field getField(Class<?> clazz, String fieldName)
    {
        for (Field field : clazz.getDeclaredFields())
        {
            if (field.getName().equals(fieldName))
                return field;
        }
        
        Field field = null;
        Class superClass = clazz.getSuperclass();
        if (superClass != null)
            field = getField(superClass, fieldName);
        
        if (field != null)
            return field;
        
        for (Class superInterface : clazz.getInterfaces())
        {
            field = getField(superInterface, fieldName);
            if (field != null)
                return field;
        }
        
        return null;
    }

    public static Object getBase(Field field)
    {
        if (field != null)
        {
            if (Modifier.isStatic(field.getModifiers()))
                return unsafe.staticFieldBase(field);
            else
                return null;
        }
        else
            return null;
    }
    
    public static long getOffset(Field field)
    {
        if (field != null)
        {
            if (Modifier.isStatic(field.getModifiers()))
                return unsafe.staticFieldOffset(field);
            else
                return unsafe.objectFieldOffset(field);
        }
        else
            return 0;
    }
    
    private static class UnsafeField implements IField
    {
        private final Object base;
        private final long offset;
        private final boolean isStatic;

        public UnsafeField(Field field)
        {
            Assert.notNull(field);
            
            if (Modifier.isStatic(field.getModifiers()))
            {
                base = unsafe.staticFieldBase(field);
                offset = unsafe.staticFieldOffset(field);
                isStatic = true;
            }
            else
            {
                this.base = null;
                this.offset = unsafe.objectFieldOffset(field);
                this.isStatic = false;
            }
        }
        
        @Override
        public <T> T getObject(Object instance)
        {
            if (isStatic)
                return (T)unsafe.getObject(base, offset);
            else
                return (T)unsafe.getObject(instance, offset);
        }

        @Override
        public void setObject(Object instance, Object value)
        {
            if (isStatic)
                unsafe.putObject(base, offset, value);
            else
                unsafe.putObject(instance, offset, value);
        }

        @Override
        public boolean getBoolean(Object instance)
        {
            if (isStatic)
                return unsafe.getBoolean(base, offset);
            else
                return unsafe.getBoolean(instance, offset);
        }

        @Override
        public void setBoolean(Object instance, boolean value)
        {
            if (isStatic)
                unsafe.putBoolean(base, offset, value);
            else
                unsafe.putBoolean(instance, offset, value);
        }

        @Override
        public byte getByte(Object instance)
        {
            if (isStatic)
                return unsafe.getByte(base, offset);
            else
                return unsafe.getByte(instance, offset);
        }

        @Override
        public void setByte(Object instance, byte value)
        {
            if (isStatic)
                unsafe.putByte(base, offset, value);
            else
                unsafe.putByte(instance, offset, value);
        }

        @Override
        public char getChar(Object instance)
        {
            if (isStatic)
                return unsafe.getChar(base, offset);
            else
                return unsafe.getChar(instance, offset);
        }

        @Override
        public void setChar(Object instance, char value)
        {
            if (isStatic)
                unsafe.putChar(base, offset, value);
            else
                unsafe.putChar(instance, offset, value);
        }

        @Override
        public short getShort(Object instance)
        {
            if (isStatic)
                return unsafe.getShort(base, offset);
            else
                return unsafe.getShort(instance, offset);
        }

        @Override
        public void setShort(Object instance, short value)
        {
            if (isStatic)
                unsafe.putShort(base, offset, value);
            else
                unsafe.putShort(instance, offset, value);
        }

        @Override
        public int getInt(Object instance)
        {
            if (isStatic)
                return unsafe.getInt(base, offset);
            else
                return unsafe.getInt(instance, offset);
        }

        @Override
        public void setInt(Object instance, int value)
        {
            if (isStatic)
                unsafe.putInt(base, offset, value);
            else
                unsafe.putInt(instance, offset, value);
        }

        @Override
        public long getLong(Object instance)
        {
            if (isStatic)
                return unsafe.getLong(base, offset);
            else
                return unsafe.getLong(instance, offset);
        }

        @Override
        public void setLong(Object instance, long value)
        {
            if (isStatic)
                unsafe.putLong(base, offset, value);
            else
                unsafe.putLong(instance, offset, value);
        }

        @Override
        public float getFloat(Object instance)
        {
            if (isStatic)
                return unsafe.getFloat(base, offset);
            else
                return unsafe.getFloat(instance, offset);
        }

        @Override
        public void setFloat(Object instance, float value)
        {
            if (isStatic)
                unsafe.putFloat(base, offset, value);
            else
                unsafe.putFloat(instance, offset, value);
        }

        @Override
        public double getDouble(Object instance)
        {
            if (isStatic)
                return unsafe.getDouble(base, offset);
            else
                return unsafe.getDouble(instance, offset);
        }

        @Override
        public void setDouble(Object instance, double value)
        {
            if (isStatic)
                unsafe.putDouble(base, offset, value);
            else
                unsafe.putDouble(instance, offset, value);
        }
    }
    
    private Fields()
    {
    }
}
