/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.exametrika.common.utils.Fields;
import com.exametrika.common.utils.Fields.IField;


/**
 * The {@link FieldsTests} are tests for {@link Fields}.
 * 
 * @see Fields
 * @author Medvedev-A
 */
public class FieldsTests
{
    private Object field1 = new Object();
    private boolean field2 = true;
    private byte field3 = 123;
    private char field4 = 'a';
    private short field5 = 124;
    private int field6 = 125;
    private long field7 = 126;
    private float field8 = 127;
    private double field9 = 128;
    private static Object field10 = new Object();
    private static boolean field11 = true;
    private static byte field12 = 123;
    private static char field13 = 'a';
    private static short field14 = 124;
    private static int field15 = 125;
    private static long field16 = 126;
    private static float field17 = 127;
    private static double field18 = 128;
    
    @Test
    public void testFields()
    {
        FieldsTests instance = new FieldsTests();
        
        IField f1 = Fields.get(FieldsTests.class, "field1");
        assertThat(f1.getObject(instance) == instance.field1, is(true));
        f1.setObject(instance, f1);
        assertThat(instance.field1 == f1, is(true));
        
        IField f2 = Fields.get(FieldsTests.class, "field2");
        assertThat(f2.getBoolean(instance) == instance.field2, is(true));
        f2.setBoolean(instance, false);
        assertThat(instance.field2, is(false));
        
        IField f3 = Fields.get(FieldsTests.class, "field3");
        assertThat(f3.getByte(instance) == instance.field3, is(true));
        f3.setByte(instance, (byte)321);
        assertThat(instance.field3, is((byte)321));
        
        IField f4 = Fields.get(FieldsTests.class, "field4");
        assertThat(f4.getChar(instance) == instance.field4, is(true));
        f4.setChar(instance, (char)322);
        assertThat(instance.field4, is((char)322));
        
        IField f5 = Fields.get(FieldsTests.class, "field5");
        assertThat(f5.getShort(instance) == instance.field5, is(true));
        f5.setShort(instance, (short)323);
        assertThat(instance.field5, is((short)323));
        
        IField f6 = Fields.get(FieldsTests.class, "field6");
        assertThat(f6.getInt(instance) == instance.field6, is(true));
        f6.setInt(instance, 324);
        assertThat(instance.field6, is(324));
        
        IField f7 = Fields.get(FieldsTests.class, "field7");
        assertThat(f7.getLong(instance) == instance.field7, is(true));
        f7.setLong(instance, 325l);
        assertThat(instance.field7, is(325l));
        
        IField f8 = Fields.get(FieldsTests.class, "field8");
        assertThat(f8.getFloat(instance) == instance.field8, is(true));
        f8.setFloat(instance, 326f);
        assertThat(instance.field8, is(326f));
        
        IField f9 = Fields.get(FieldsTests.class, "field9");
        assertThat(f9.getDouble(instance) == instance.field9, is(true));
        f9.setDouble(instance, 327d);
        assertThat(instance.field9, is(327d));

        IField f10 = Fields.get(FieldsTests.class, "field10");
        assertThat(f10.getObject(null) == field10, is(true));
        f10.setObject(null, f10);
        assertThat(field10 == f10, is(true));
        
        IField f11 = Fields.get(FieldsTests.class, "field11");
        assertThat(f11.getBoolean(null) == field11, is(true));
        f11.setBoolean(null, false);
        assertThat(field11, is(false));
        
        IField f12 = Fields.get(FieldsTests.class, "field12");
        assertThat(f12.getByte(null) == field12, is(true));
        f12.setByte(null, (byte)432);
        assertThat(field12, is((byte)432));
        
        IField f13 = Fields.get(FieldsTests.class, "field13");
        assertThat(f13.getChar(null) == field13, is(true));
        f13.setChar(null, (char)433);
        assertThat(field13, is((char)433));
        
        IField f14 = Fields.get(FieldsTests.class, "field14");
        assertThat(f14.getShort(null) == field14, is(true));
        f14.setShort(null, (short)434);
        assertThat(field14, is((short)434));
        
        IField f15 = Fields.get(FieldsTests.class, "field15");
        assertThat(f15.getInt(null) == field15, is(true));
        f15.setInt(null, 435);
        assertThat(field15, is(435));
        
        IField f16 = Fields.get(FieldsTests.class, "field16");
        assertThat(f16.getLong(null) == field16, is(true));
        f16.setLong(null, 436l);
        assertThat(field16, is(436l));
        
        IField f17 = Fields.get(FieldsTests.class, "field17");
        assertThat(f17.getFloat(null) == field17, is(true));
        f17.setFloat(null, 437f);
        assertThat(field17, is(437f));
        
        IField f18 = Fields.get(FieldsTests.class, "field18");
        assertThat(f18.getDouble(null) == field18, is(true));
        f18.setDouble(null, 438d);
        assertThat(field18, is(438d));
    }
}
