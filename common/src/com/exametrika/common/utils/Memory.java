/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import sun.misc.Unsafe;

/**
 * The {@link Memory} contains different utility methods for work with object and array memory layout.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class Memory
{
    public static class FieldLayout
    {
        public final Field field;
        public final int offset;
        public final int size;
        public final int padding;
        
        public FieldLayout(Field field, int offset, int size, int padding)
        {
            Assert.notNull(field);
            
            this.field = field;
            this.offset = offset;
            this.size = size;
            this.padding = padding;
        }
        
        @Override
        public String toString()
        {
            return String.format("    %04d: %s (%d)\n", offset, field.getName(), size) + 
                (padding > 0 ? "          <padding> (" + padding + ")\n" : "");
        }
    }
    
    public static class Layout
    {
        public final Class clazz;
        public final int headerSize;
        public final int size;
        
        public Layout(Class clazz, int headerSize, int size)
        {
            Assert.notNull(clazz);
            
            this.clazz = clazz;
            this.headerSize = headerSize;
            this.size = size;
        }
    }
    
    public static class ClassLayout extends Layout
    {
        public final List<FieldLayout> fields;
        
        public ClassLayout(Class clazz, int headerSize, List<FieldLayout> fields, int size)
        {
            super(clazz, headerSize, size);

            Assert.notNull(fields);
            
            this.fields = Immutables.wrap(fields);
        }
        
        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append(clazz.getName() + " (size - " + size + "):\n");
            builder.append(String.format("    %04d: <header> (%s)\n", 0, headerSize));
            Class clazz = null;
            for (FieldLayout field : fields)
            {
                if (field.field.getDeclaringClass() != clazz)
                {
                    builder.append("  class " + field.field.getDeclaringClass().getSimpleName() + "\n");
                    clazz = field.field.getDeclaringClass();
                }
                builder.append(field);
            }
            
            return builder.toString();
        }
    }
    
    public static class ArrayLayout extends Layout
    {
        public final Class elementType;
        public final int length;
        public final int elementSize;
        public final int padding;
        
        public ArrayLayout(Class clazz, int headerSize, Class elementType, int length, int elementSize, int padding, int size)
        {
            super(clazz, headerSize, size);
            
            Assert.notNull(elementType);
            
            this.elementType = elementType;
            this.length = length;
            this.elementSize = elementSize;
            this.padding = padding;
        }
        
        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("%s[%d] (size - %s):\n", elementType.getName(), length, size));
            builder.append(String.format("  %04d: <header> (%s)\n", 0, headerSize));
            if (length > 0)
            {
                builder.append(String.format("  %04d: <element> (%s)\n", headerSize, elementSize));
                if (length > 1)
                {
                    if (length > 2)
                        builder.append("        ...\n");
                    builder.append(String.format("  %04d: <element> (%s)\n", headerSize + (length - 1) * elementSize, elementSize));
                }
            }
            
            if (padding > 0)
                builder.append(String.format("        <padding> (%s)\n", padding));
            
            return builder.toString();
        }
    }
    
    /**
     * Returns shallow size of specified object.
     *
     * @param value value
     * @return shallow size of specified object
     */
    public int getShallowSize(Object value)
    {
        Assert.notNull(value);
        
        if (value.getClass().isArray())
            return getShallowSize(value.getClass(), Array.getLength(value));
        else
            return getShallowSize(value.getClass());
    }
    
    /**
     * Returns shallow size for array of specified class.
     *
     * @param arrayClazz array class
     * @param length array length
     * @return estimated shallow memory size occuped by array
     */
    public static int getShallowSize(Class arrayClazz, int length)
    {
        Assert.notNull(arrayClazz);
        Assert.isTrue(arrayClazz.isArray());
        int elementSize = getFieldSize(arrayClazz.getComponentType());
        int size = Classes.getUnsafe().arrayBaseOffset(arrayClazz) + elementSize * length;
        
        if (size % 8 != 0)
            size = (size / 8 + 1) * 8;
        
        return size;
    }
    
    /**
     * Returns shallow size for non-array object or primitive value of specified class.
     *
     * @param clazz class non-array object or primitive value class
     * @return estimated shallow memory size occuped by value of specified class
     */
    public static int getShallowSize(Class clazz)
    {
        Assert.notNull(clazz);
        Assert.isTrue(!clazz.isAnnotation() && !clazz.isInterface() && !clazz.isArray());
        
        if (clazz.isPrimitive())
            return getPrimitiveSize(clazz);
        
        int[] res = new int[2];
        getMaxOffset(clazz, res);
        
        int size = res[0] + res[1];
        if (size == 0)
        {
            getMaxOffset(Empty.class, res);
            size = res[0];
        }
        
        if (size % 8 != 0)
            size = (size / 8 + 1) * 8;
        
        return size;
    }
    
    public Layout getMemoryLayout(Object value)
    {
        Assert.notNull(value);
        
        if (value.getClass().isArray())
            return getMemoryLayout(value.getClass(), Array.getLength(value));
        else
            return getMemoryLayout(value.getClass());
    }
    
    public static ArrayLayout getMemoryLayout(Class arrayClazz, int length)
    {
        Assert.notNull(arrayClazz);
        Assert.isTrue(arrayClazz.isArray());
        int elementSize = getFieldSize(arrayClazz.getComponentType());
        int headerSize = Classes.getUnsafe().arrayBaseOffset(arrayClazz);
        int size =  headerSize + elementSize * length;
        int padding = 0;
        if (size % 8 != 0)
        {
            int newSize = (size / 8 + 1) * 8;
            padding = newSize - size;
            size = newSize;
        }
        
        return new ArrayLayout(arrayClazz, headerSize, arrayClazz.getComponentType(), length, elementSize, padding, size);
    }
    
    public static ClassLayout getMemoryLayout(Class clazz)
    {
        Assert.notNull(clazz);
        Assert.isTrue(!clazz.isAnnotation() && !clazz.isInterface() && !clazz.isArray());
        
        if (clazz.isPrimitive())
            return new ClassLayout(clazz, 0, Collections.<FieldLayout>emptyList(), getPrimitiveSize(clazz));
        
        ClassLayoutInfo classLayout = new ClassLayoutInfo();
        
        getClassLayout(clazz, classLayout);
        if (classLayout.fields.isEmpty())
        {
            int size = getShallowSize(Object.class);
            return new ClassLayout(clazz, size, Collections.<FieldLayout>emptyList(), size);
        }
        
        Collections.sort(classLayout.fields, new Comparator<FieldLayoutInfo>()
        {
            @Override
            public int compare(FieldLayoutInfo o1, FieldLayoutInfo o2)
            {
                if (o1.offset > o2.offset)
                    return 1;
                else if (o1.offset < o2.offset)
                    return -1;
                else
                    return 0;
            }
        });
        
        int size = classLayout.maxOffset + classLayout.lastFieldSize;
        
        if (size % 8 != 0)
            size = (size / 8 + 1) * 8;
        
        List<FieldLayout> fields = new ArrayList<FieldLayout>();
        for (int i = 0; i < classLayout.fields.size(); i++)
        {
            FieldLayoutInfo info = classLayout.fields.get(i);
            int nextOffset;
            if (i < classLayout.fields.size() - 1)
                nextOffset = classLayout.fields.get(i + 1).offset;
            else
                nextOffset = size;
            
            fields.add(new FieldLayout(info.field, info.offset, info.size, nextOffset - info.offset - info.size));
        }
        
        return new ClassLayout(clazz, classLayout.fields.get(0).offset, fields, size);
    }

    private static void getMaxOffset(Class clazz, int[] res)
    {
        Field[] fields = clazz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++)
        {
            Field field = fields[i];
            if (Modifier.isStatic(field.getModifiers()))
                continue;
            
            int offset = (int)Classes.getUnsafe().objectFieldOffset(field);
            if (res[0] < offset)
            {
                res[0] = offset;
                res[1] = getFieldSize(field.getType());
            }
        }
        
        if (clazz.getSuperclass() != null)
            getMaxOffset(clazz.getSuperclass(), res);
    }

    private static int getFieldSize(Class clazz)
    {
        if (clazz.isPrimitive())
            return getPrimitiveSize(clazz);
        else
            return Unsafe.ADDRESS_SIZE;
    }
    
    private static int getPrimitiveSize(Class clazz)
    {
        if (clazz == byte.class || clazz == boolean.class)
            return 1;
        else if (clazz == char.class || clazz == short.class)
            return 2;
        else if (clazz == int.class || clazz == float.class)
            return 4;
        else if (clazz == long.class || clazz == double.class)
            return 8;
        else
        {
            Assert.isTrue(false);
            return 0;
        }
    }
    
    private static void getClassLayout(Class clazz, ClassLayoutInfo classLayout)
    {
        Field[] fields = clazz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++)
        {
            Field field = fields[i];
            if (Modifier.isStatic(field.getModifiers()))
                continue;
            
            int offset = (int)Classes.getUnsafe().objectFieldOffset(field);
            int size = getFieldSize(field.getType());
            
            if (classLayout.maxOffset < offset)
            {
                classLayout.maxOffset = offset;
                classLayout.lastFieldSize = size;
            }
            
            FieldLayoutInfo fieldLayout = new FieldLayoutInfo();
            fieldLayout.field = field;
            fieldLayout.offset = offset;
            fieldLayout.size = size;
            classLayout.fields.add(fieldLayout);
        }
        
        if (clazz.getSuperclass() != null)
            getClassLayout(clazz.getSuperclass(), classLayout);
    }
    
    private Memory()
    {
    }
    
    private static class ClassLayoutInfo
    {
        List<FieldLayoutInfo> fields = new ArrayList<FieldLayoutInfo>();
        int maxOffset;
        int lastFieldSize;
    }
    
    private static class FieldLayoutInfo
    {
        Field field;
        int offset;
        int size;
    }
    
    private static class Empty
    {
        @SuppressWarnings("unused")
        private boolean b;
    }
}
