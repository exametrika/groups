/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.expression;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.expression.impl.StandardConversionProvider;
import com.exametrika.common.expression.impl.nodes.ArrayExpressionNode;
import com.exametrika.common.expression.impl.nodes.BinaryExpressionNode;
import com.exametrika.common.expression.impl.nodes.BreakExpressionNode;
import com.exametrika.common.expression.impl.nodes.CastExpressionNode;
import com.exametrika.common.expression.impl.nodes.ConstantExpressionNode;
import com.exametrika.common.expression.impl.nodes.ConstructorExpressionNode;
import com.exametrika.common.expression.impl.nodes.ContinueExpressionNode;
import com.exametrika.common.expression.impl.nodes.ElvisExpressionNode;
import com.exametrika.common.expression.impl.nodes.ForExpressionNode;
import com.exametrika.common.expression.impl.nodes.IfExpressionNode;
import com.exametrika.common.expression.impl.nodes.IsExpressionNode;
import com.exametrika.common.expression.impl.nodes.LikeExpressionNode;
import com.exametrika.common.expression.impl.nodes.MapExpressionNode;
import com.exametrika.common.expression.impl.nodes.MethodExpressionNode;
import com.exametrika.common.expression.impl.nodes.NullSafeExpressionNode;
import com.exametrika.common.expression.impl.nodes.ProjectionExpressionNode;
import com.exametrika.common.expression.impl.nodes.PropertyAssignmentExpressionNode;
import com.exametrika.common.expression.impl.nodes.PropertyExpressionNode;
import com.exametrika.common.expression.impl.nodes.ReturnExpressionNode;
import com.exametrika.common.expression.impl.nodes.SelectionExpressionNode;
import com.exametrika.common.expression.impl.nodes.SelectionExpressionNode.Operation;
import com.exametrika.common.expression.impl.nodes.SelfExpressionNode;
import com.exametrika.common.expression.impl.nodes.StatementExpressionNode;
import com.exametrika.common.expression.impl.nodes.TernaryExpressionNode;
import com.exametrika.common.expression.impl.nodes.UnaryExpressionNode;
import com.exametrika.common.expression.impl.nodes.VariableAssignmentExpressionNode;
import com.exametrika.common.expression.impl.nodes.VariableExpressionNode;
import com.exametrika.common.expression.impl.nodes.WhileExpressionNode;
import com.exametrika.common.utils.Pair;


/**
 * The {@link ExpressionNodeTests} are tests for {@link IExpressionNode} implementations.
 * 
 * @see Expressions
 * @author Medvedev-A
 */
public class ExpressionNodeTests
{
    @Test
    public void testArray() throws Throwable
    {
        ArrayExpressionNode node = new ArrayExpressionNode(Arrays.<IExpressionNode>asList(new ConstantExpressionNode(123),
            new ConstantExpressionNode(null)));
        ExpressionContext context = new ExpressionContext();
        assertThat((List)node.evaluate(context, null), is(Arrays.asList(123, null)));
    }
    
    @Test
    public void testVariableAssignment() throws Throwable
    {
        VariableExpressionNode var = new VariableExpressionNode("test", 0);
        VariableAssignmentExpressionNode node = new VariableAssignmentExpressionNode(var, new ConstantExpressionNode(123));
        ExpressionContext context = new ExpressionContext();
        assertThat((Integer)node.evaluate(context, null), is(123));
        
        assertThat((Integer)var.evaluate(context, null), is(123));
    }
    
    @Test
    public void testBinary() throws Throwable
    {
        BinaryExpressionNode node = new BinaryExpressionNode(new ConstantExpressionNode(true), new ConstantExpressionNode(false), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.OR);
        ExpressionContext context = new ExpressionContext();
        assertThat((Boolean)node.evaluate(context, null), is(true));
        node = new BinaryExpressionNode(new ConstantExpressionNode(null), new ConstantExpressionNode(true), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.OR);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        node = new BinaryExpressionNode(new ConstantExpressionNode(0), new ConstantExpressionNode(null), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.OR);
        assertThat((Boolean)node.evaluate(context, null), is(false));
        
        node = new BinaryExpressionNode(new ConstantExpressionNode(null), new ConstantExpressionNode(true), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.AND);
        assertThat((Boolean)node.evaluate(context, null), is(false));
        node = new BinaryExpressionNode(new ConstantExpressionNode(true), new ConstantExpressionNode(1), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.AND);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        node = new BinaryExpressionNode(new ConstantExpressionNode(true), new ConstantExpressionNode(null), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.AND);
        assertThat((Boolean)node.evaluate(context, null), is(false));
        
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(345), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.BOR);
        assertThat((Long)node.evaluate(context, null), is(123l | 345l));
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(345), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.BAND);
        assertThat((Long)node.evaluate(context, null), is(123l & 345l));
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(345), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.XOR);
        assertThat((Long)node.evaluate(context, null), is(123l ^ 345l));
        
        node = new BinaryExpressionNode(new ConstantExpressionNode(null), new ConstantExpressionNode(null), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.EQ);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        node = new BinaryExpressionNode(new ConstantExpressionNode(null), new ConstantExpressionNode(123), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.EQ);
        assertThat((Boolean)node.evaluate(context, null), is(false));
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(123), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.EQ);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        node = new BinaryExpressionNode(new ConstantExpressionNode(123.1), new ConstantExpressionNode(123.1), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.EQ);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        node = new BinaryExpressionNode(new ConstantExpressionNode("test"), new ConstantExpressionNode("test"), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.EQ);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        
        node = new BinaryExpressionNode(new ConstantExpressionNode(null), new ConstantExpressionNode(null), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.NEQ);
        assertThat((Boolean)node.evaluate(context, null), is(false));
        node = new BinaryExpressionNode(new ConstantExpressionNode(null), new ConstantExpressionNode(123), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.NEQ);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(123), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.NEQ);
        assertThat((Boolean)node.evaluate(context, null), is(false));
        node = new BinaryExpressionNode(new ConstantExpressionNode(123.1), new ConstantExpressionNode(123.1), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.NEQ);
        assertThat((Boolean)node.evaluate(context, null), is(false));
        node = new BinaryExpressionNode(new ConstantExpressionNode("test"), new ConstantExpressionNode("test"), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.NEQ);
        assertThat((Boolean)node.evaluate(context, null), is(false));
        
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(345), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.LT);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(123.3), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.LT);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        node = new BinaryExpressionNode(new ConstantExpressionNode("abc"), new ConstantExpressionNode("dce"), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.LT);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(345), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.LTE);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(123.3), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.LTE);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        node = new BinaryExpressionNode(new ConstantExpressionNode("abc"), new ConstantExpressionNode("dce"), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.LTE);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(123), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.LTE);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(123.0), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.LTE);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        node = new BinaryExpressionNode(new ConstantExpressionNode("abc"), new ConstantExpressionNode("abc"), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.LTE);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        
        node = new BinaryExpressionNode(new ConstantExpressionNode(345), new ConstantExpressionNode(123), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.GT);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        node = new BinaryExpressionNode(new ConstantExpressionNode(123.3), new ConstantExpressionNode(123), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.GT);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        node = new BinaryExpressionNode(new ConstantExpressionNode("dce"), new ConstantExpressionNode("abc"), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.GT);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        
        node = new BinaryExpressionNode(new ConstantExpressionNode(345), new ConstantExpressionNode(123), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.GTE);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        node = new BinaryExpressionNode(new ConstantExpressionNode(123.3), new ConstantExpressionNode(123), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.GTE);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        node = new BinaryExpressionNode(new ConstantExpressionNode("dce"), new ConstantExpressionNode("abc"), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.GTE);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(123), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.GTE);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(123.0), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.GTE);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        node = new BinaryExpressionNode(new ConstantExpressionNode("abc"), new ConstantExpressionNode("abc"), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.GTE);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(10), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.SHL);
        assertThat((Long)node.evaluate(context, null), is(123l << 10));
        
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(10), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.SHR);
        assertThat((Long)node.evaluate(context, null), is(123l >> 10));
        
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(10), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.USHR);
        assertThat((Long)node.evaluate(context, null), is(123l >>> 10));
        
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(345), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.ADDITION);
        assertThat((Long)node.evaluate(context, null), is(123l + 345));
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(345.0), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.ADDITION);
        assertThat((Double)node.evaluate(context, null), is(123l + 345.0d));
        node = new BinaryExpressionNode(new ConstantExpressionNode("123"), new ConstantExpressionNode(345), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.ADDITION);
        assertThat((String)node.evaluate(context, null), is("123" + 345));
        
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(345), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.SUBTRACTION);
        assertThat((Long)node.evaluate(context, null), is(123l - 345));
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(345), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.MULTIPLICATION);
        assertThat((Long)node.evaluate(context, null), is(123l * 345));
        node = new BinaryExpressionNode(new ConstantExpressionNode(123.0), new ConstantExpressionNode(345), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.DIVISION);
        assertThat((Double)node.evaluate(context, null), is(123.0d / 345));
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(345), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.REMAINDER);
        assertThat((Long)node.evaluate(context, null), is(123l % 345));
        
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(Collections.singletonMap(123, 345)), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.IN);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(Arrays.asList(123, 345)), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.IN);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(new Object[]{123, 345}), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.IN);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(new int[]{123, 345}), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.IN);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        
        node = new BinaryExpressionNode(new ConstantExpressionNode("world"), new ConstantExpressionNode("Hello world!"), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.IN);
        assertThat((Boolean)node.evaluate(context, null), is(true));
        
        node = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(Collections.singletonMap(123, 345)), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.NOT_IN);
        assertThat((Boolean)node.evaluate(context, null), is(false));
    }
    
    @Test
    public void testCast() throws Throwable
    {
        CastExpressionNode node = new CastExpressionNode(new ConstantExpressionNode("test"), new ConstantExpressionNode("java.lang.String"));
        ExpressionContext context = new ExpressionContext();
        assertThat((String)node.evaluate(context, null), is("test"));
        
        node = new CastExpressionNode(new ConstantExpressionNode(null), new ConstantExpressionNode("java.lang.String"));
        assertThat(node.evaluate(context, null), nullValue());
        
        node = new CastExpressionNode(new ConstantExpressionNode("test2"), new ConstantExpressionNode("java.lang.String"));
        assertThat((String)node.evaluate(context, null), is("test2"));
    }
    
    @Test
    public void testConstant() throws Throwable
    {
        ConstantExpressionNode node = new ConstantExpressionNode("test");
        ExpressionContext context = new ExpressionContext();
        assertThat((String)node.evaluate(context, null), is("test"));
    }
    
    @Test
    public void testElvis() throws Throwable
    {
        ElvisExpressionNode node = new ElvisExpressionNode(new ConstantExpressionNode("test"), new ConstantExpressionNode("default"));
        ExpressionContext context = new ExpressionContext();
        assertThat((String)node.evaluate(context, null), is("test"));
        
        node = new ElvisExpressionNode(new ConstantExpressionNode(null), new ConstantExpressionNode("default"));
        assertThat((String)node.evaluate(context, null), is("default"));
    }
    
    @Test
    public void testIndex() throws Throwable
    {
        PropertyExpressionNode node = new PropertyExpressionNode(true, new ConstantExpressionNode(0), null, false);
        ExpressionContext context = new ExpressionContext();
        assertThat((Integer)node.evaluate(context, Arrays.asList(123)), is(123));
        
        node = new PropertyExpressionNode(true, new ConstantExpressionNode("0"), null, false);
        assertThat((Integer)node.evaluate(context, Arrays.asList(123)), is(123));
        
        node = new PropertyExpressionNode(true, new ConstantExpressionNode(0), null, false);
        assertThat((Integer)node.evaluate(context, new Object[]{123}), is(123));
        
        node = new PropertyExpressionNode(true, new ConstantExpressionNode(0), null, false);
        assertThat((Integer)node.evaluate(context, new int[]{123}), is(123));
        
        node = new PropertyExpressionNode(true, new ConstantExpressionNode(0), null, false);
        assertThat((Character)node.evaluate(context, "test"), is('t'));
        
        node = new PropertyExpressionNode(true, new ConstantExpressionNode("test"), null, false);
        assertThat((Integer)node.evaluate(context, Collections.singletonMap("test", 123)), is(123));
        
        node = new PropertyExpressionNode(true, new ConstantExpressionNode("test1"), null, false);
        assertThat(node.evaluate(context, Collections.singletonMap("test", 123)), nullValue());
    }
    
    @Test
    public void testIndexAssignment() throws Throwable
    {
        PropertyAssignmentExpressionNode node = new PropertyAssignmentExpressionNode(true, new ConstantExpressionNode(0), null, false,
            new ConstantExpressionNode(456));
        ExpressionContext context = new ExpressionContext();
        List<Integer> list = new ArrayList<Integer>();
        assertThat((Integer)node.evaluate(context, list), is(456));
        assertThat(list.size(), is(1));
        assertThat(list.get(0), is(456));

        Object[] array = new Object[1];
        assertThat((Integer)node.evaluate(context, array), is(456));
        assertThat(array.length, is(1));
        assertThat((Integer)array[0], is(456));
        
        int[] array2 = new int[1];
        assertThat((Integer)node.evaluate(context, array2), is(456));
        assertThat(array2.length, is(1));
        assertThat(array2[0], is(456));

        Map<String, Integer> map = new HashMap<String, Integer>();
        node = new PropertyAssignmentExpressionNode(true, new ConstantExpressionNode("test"), null, false,
            new ConstantExpressionNode(456));
        assertThat((Integer)node.evaluate(context, map), is(456));
        assertThat(map.size(), is(1));
        assertThat(map.get("test"), is(456));
    }
    
    @Test
    public void testIs() throws Throwable
    {
        IsExpressionNode node = new IsExpressionNode(new ConstantExpressionNode("test"), new ConstantExpressionNode("java.lang.String"));
        ExpressionContext context = new ExpressionContext();
        assertThat((Boolean)node.evaluate(context, null), is(true));
        
        node = new IsExpressionNode(new ConstantExpressionNode(null), new ConstantExpressionNode("string"));
        assertThat((Boolean)node.evaluate(context, null), is(false));
        
        node = new IsExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode("string"));
        assertThat((Boolean)node.evaluate(context, null), is(false));
    }
    
    @Test
    public void testMap() throws Throwable
    {
        MapExpressionNode node = new MapExpressionNode(Arrays.<Pair<IExpressionNode, IExpressionNode>>asList(
            new Pair(new ConstantExpressionNode(123),new ConstantExpressionNode(123)),
            new Pair(new ConstantExpressionNode(null),new ConstantExpressionNode(null))));
        ExpressionContext context = new ExpressionContext();
        Map<Object, Object> map = new HashMap<Object, Object>();
        map.put(123, 123);
        map.put(null, null);
        assertThat((Map)node.evaluate(context, null), is(map));
    }
    
    @Test
    public void testLike() throws Throwable
    {
        LikeExpressionNode node = new LikeExpressionNode(new ConstantExpressionNode("Hello world!"),new ConstantExpressionNode("He*ll? w*d?"));
        ExpressionContext context = new ExpressionContext();
        assertThat((Boolean)node.evaluate(context, null), is(true));
        
        node = new LikeExpressionNode(new ConstantExpressionNode("Hello world"),new ConstantExpressionNode("He*ll? w*d?"));
        assertThat((Boolean)node.evaluate(context, null), is(false));
        
        node = new LikeExpressionNode(new ConstantExpressionNode(null),new ConstantExpressionNode("He*ll? w*d?"));
        assertThat((Boolean)node.evaluate(context, null), is(false));
    }
    
    @Test
    public void testNullSafe() throws Throwable
    {
        NullSafeExpressionNode node = new NullSafeExpressionNode(new ConstantExpressionNode("test"));
        ExpressionContext context = new ExpressionContext();
        assertThat((String)node.evaluate(context, "abc"), is("test"));
        assertThat(node.evaluate(context, null), nullValue());
    }
    
    @Test
    public void testProjection() throws Throwable
    {
        ProjectionExpressionNode node = new ProjectionExpressionNode(new SelfExpressionNode());
        ExpressionContext context = new ExpressionContext();
        assertThat((List)node.evaluate(context, null), is(Collections.emptyList()));
        assertThat((List)node.evaluate(context, Arrays.asList(123, 345)), is(Arrays.asList(123, 345)));
        assertThat((List)node.evaluate(context, new Object[]{123, 345}), is(Arrays.asList(123, 345)));
        assertThat((List)node.evaluate(context, new int[]{123, 345}), is(Arrays.asList(123, 345)));
        assertThat((List)node.evaluate(context, "abc"), is(Arrays.asList('a', 'b', 'c')));
        assertThat((List)node.evaluate(context, Collections.singletonMap(123, 345)), is(
            com.exametrika.common.utils.Collections.toList(Collections.singletonMap(123, 345).entrySet().iterator())));
    }
    
    @Test
    public void testProperty() throws Throwable
    {
        PropertyExpressionNode node = new PropertyExpressionNode(true, new ConstantExpressionNode("fieldA"), null, false);
        ExpressionContext context = new ExpressionContext();
        assertThat((String)node.evaluate(context, Collections.singletonMap("fieldA", "map")), is("map"));
        assertThat((Integer)node.evaluate(context, new TestA()), is(321));
        assertThat((Integer)node.evaluate(context, new TestA()), is(321));
        assertThat((Integer)node.evaluate(context, new TestB()), is(432));
        assertThat((String)node.evaluate(context, new TestC()), is("fieldA"));
        
        node = new PropertyExpressionNode(true, new ConstantExpressionNode("fieldB"), null, false);
        assertThat((Integer)node.evaluate(context, new TestA()), is(123));
        assertThat((Integer)node.evaluate(context, new TestB()), is(345));
        assertThat((String)node.evaluate(context, new TestC()), is("fieldB"));
        
        node = new PropertyExpressionNode(true, new ConstantExpressionNode("fieldC"), new ConstantExpressionNode(TestA.class.getName()), true);
        assertThat((Integer)node.evaluate(context, null), is(345));
        assertThat((Integer)node.evaluate(context, null), is(345));
        node = new PropertyExpressionNode(true, new ConstantExpressionNode("fieldC"), new ConstantExpressionNode(TestB.class.getName()), true);
        assertThat((Integer)node.evaluate(context, null), is(789));
        node = new PropertyExpressionNode(true, new ConstantExpressionNode("fieldC"), new ConstantExpressionNode(TestC.class.getName()), true);
        assertThat((String)node.evaluate(context, null), is("fieldC"));
        
        node = new PropertyExpressionNode(true, new ConstantExpressionNode("fieldD"), new ConstantExpressionNode(TestA.class.getName()), true);
        assertThat((Integer)node.evaluate(context, null), is(567));
        node = new PropertyExpressionNode(true, new ConstantExpressionNode("fieldD"), new ConstantExpressionNode(TestB.class.getName()), true);
        assertThat((Integer)node.evaluate(context, null), is(765));
        node = new PropertyExpressionNode(true, new ConstantExpressionNode("fieldD"), new ConstantExpressionNode(TestC.class.getName()), true);
        assertThat((String)node.evaluate(context, null), is("fieldD"));
    }
    
    @Test
    public void testPropertyAssignment() throws Throwable
    {
        PropertyAssignmentExpressionNode node = new PropertyAssignmentExpressionNode(true, new ConstantExpressionNode("fieldA"), null, false,
            new ConstantExpressionNode(9876));
        ExpressionContext context = new ExpressionContext();
        TestA a = new TestA();
        TestB b = new TestB();
        TestC c = new TestC();
        assertThat((Integer)node.evaluate(context, a), is(9876));
        assertThat(a.fieldA, is(9876));
        assertThat((Integer)node.evaluate(context, b), is(9876));
        assertThat((Integer)b.fieldA, is(9876));
        assertThat((String)node.evaluate(context, c), is("9876"));
        assertThat(c.fieldA, is("9876"));
        
        node = new PropertyAssignmentExpressionNode(true, new ConstantExpressionNode("fieldB"), null, false,
            new ConstantExpressionNode(8765));
        assertThat((Integer)node.evaluate(context, a), is(8765));
        assertThat(a.getFieldB(), is(8765));
        assertThat((Integer)node.evaluate(context, b), is(8765));
        assertThat(b.getFieldB(), is(8765));
        assertThat((String)node.evaluate(context, c), is("8765"));
        assertThat(c.getFieldB(), is("8765"));
        
        node = new PropertyAssignmentExpressionNode(true, new ConstantExpressionNode("fieldC"), new ConstantExpressionNode(TestA.class.getName()), true,
            new ConstantExpressionNode(7654));
        assertThat((Integer)node.evaluate(context, null), is(7654));
        assertThat(TestA.fieldC, is(7654));
        
        node = new PropertyAssignmentExpressionNode(true, new ConstantExpressionNode("fieldC"), new ConstantExpressionNode(TestB.class.getName()), true,
            new ConstantExpressionNode(6543));
        assertThat((Integer)node.evaluate(context, null), is(6543));
        assertThat(TestB.fieldC, is(6543));
        
        node = new PropertyAssignmentExpressionNode(true, new ConstantExpressionNode("fieldC"), new ConstantExpressionNode(TestC.class.getName()), true,
            new ConstantExpressionNode(7654));
        assertThat((String)node.evaluate(context, null), is("7654"));
        assertThat(TestC.fieldC, is("7654"));
        
        node = new PropertyAssignmentExpressionNode(true, new ConstantExpressionNode("fieldD"), new ConstantExpressionNode(TestA.class.getName()), true,
            new ConstantExpressionNode(7654));
        assertThat((Integer)node.evaluate(context, null), is(7654));
        assertThat(TestA.getFieldD(), is(7654));
        
        node = new PropertyAssignmentExpressionNode(true, new ConstantExpressionNode("fieldD"), new ConstantExpressionNode(TestB.class.getName()), true,
            new ConstantExpressionNode(6543));
        assertThat((Integer)node.evaluate(context, null), is(6543));
        assertThat(TestB.getFieldD(), is(6543));
        
        node = new PropertyAssignmentExpressionNode(true, new ConstantExpressionNode("fieldD"), new ConstantExpressionNode(TestC.class.getName()), true,
            new ConstantExpressionNode(7654));
        assertThat((String)node.evaluate(context, null), is("7654"));
        assertThat(TestC.getFieldD(), is("7654"));
    }
    
    @Test
    public void testMethod() throws Throwable
    {
        MethodExpressionNode node = new MethodExpressionNode(true, new ConstantExpressionNode("methodA"), Arrays.<IExpressionNode>asList(), null, false);
        ExpressionContext context = new ExpressionContext();
        assertThat((String)node.evaluate(context, new TestA()), is("A.methodA"));
        assertThat((String)node.evaluate(context, new TestA()), is("A.methodA"));
        assertThat((String)node.evaluate(context, new TestB()), is("B.methodA"));
        assertThat((String)node.evaluate(context, new TestC()), is("C.methodA"));
        
        node = new MethodExpressionNode(true, new ConstantExpressionNode("methodA"), Arrays.<IExpressionNode>asList(
            new ConstantExpressionNode(123), new ConstantExpressionNode(345)), null, false);
        
        assertThat((String)node.evaluate(context, new TestA()), is("A.methodA(a,b)"));
        assertThat((String)node.evaluate(context, new TestA()), is("A.methodA(a,b)"));
        assertThat((String)node.evaluate(context, new TestB()), is("B.methodA(a,b)"));
        assertThat((String)node.evaluate(context, new TestC()), is("C.methodA(a,b)"));
        
        node = new MethodExpressionNode(true, new ConstantExpressionNode("methodB"), Arrays.<IExpressionNode>asList(),
            new ConstantExpressionNode(TestA.class.getName()), true);
        assertThat((String)node.evaluate(context, null), is("A.methodB"));
        assertThat((String)node.evaluate(context, null), is("A.methodB"));
        
        node = new MethodExpressionNode(true, new ConstantExpressionNode("methodB"), Arrays.<IExpressionNode>asList(
            new ConstantExpressionNode(123), new ConstantExpressionNode(345)), new ConstantExpressionNode(TestA.class.getName()), true);
        assertThat((String)node.evaluate(context, null), is("A.methodB(a,b)"));
        
        node = new MethodExpressionNode(true, new ConstantExpressionNode("methodB"), Arrays.<IExpressionNode>asList(),
            new ConstantExpressionNode(TestB.class.getName()), true);
        assertThat((String)node.evaluate(context, null), is("B.methodB"));
        
        node = new MethodExpressionNode(true, new ConstantExpressionNode("methodB"), Arrays.<IExpressionNode>asList(
            new ConstantExpressionNode(123), new ConstantExpressionNode(345)), new ConstantExpressionNode(TestB.class.getName()), true);
        assertThat((String)node.evaluate(context, null), is("B.methodB(a,b)"));
        
        node = new MethodExpressionNode(true, new ConstantExpressionNode("methodB"), Arrays.<IExpressionNode>asList(),
            new ConstantExpressionNode(TestC.class.getName()), true);
        assertThat((String)node.evaluate(context, null), is("C.methodB"));
        
        node = new MethodExpressionNode(true, new ConstantExpressionNode("methodB"), Arrays.<IExpressionNode>asList(
            new ConstantExpressionNode(123), new ConstantExpressionNode(345)), new ConstantExpressionNode(TestC.class.getName()), true);
        assertThat((String)node.evaluate(context, null), is("C.methodB(a,b)"));
    }

    @Test
    public void testConstructor() throws Throwable
    {
        ConstructorExpressionNode node = new ConstructorExpressionNode(Arrays.<IExpressionNode>asList(),
            new ConstantExpressionNode(TestA.class.getName()));
        ExpressionContext context = new ExpressionContext();
        assertThat(((TestA)node.evaluate(context, null)).fieldA, is(321));
        assertThat(((TestA)node.evaluate(context, null)).fieldA, is(321));
        
        node = new ConstructorExpressionNode(Arrays.<IExpressionNode>asList(
            new ConstantExpressionNode(123), new ConstantExpressionNode(345)), new ConstantExpressionNode(TestA.class.getName()));
        assertThat(((TestA)node.evaluate(context, null)).fieldA, is(123));
        
        node = new ConstructorExpressionNode(Arrays.<IExpressionNode>asList(),
            new ConstantExpressionNode(TestB.class.getName()));
        assertThat(((TestB)node.evaluate(context, null)).fieldA, is((Object)432));
        
        node = new ConstructorExpressionNode(Arrays.<IExpressionNode>asList(
            new ConstantExpressionNode(123), new ConstantExpressionNode(345)), new ConstantExpressionNode(TestB.class.getName()));
        assertThat(((TestB)node.evaluate(context, null)).fieldA, is((Object)567));
        
        node = new ConstructorExpressionNode(Arrays.<IExpressionNode>asList(),
            new ConstantExpressionNode(TestC.class.getName()));
        assertThat(((TestC)node.evaluate(context, null)).fieldA, is("fieldA"));
        
        node = new ConstructorExpressionNode(Arrays.<IExpressionNode>asList(
            new ConstantExpressionNode(123), new ConstantExpressionNode(345)), new ConstantExpressionNode(TestC.class.getName()));
        assertThat(((TestC)node.evaluate(context, null)).fieldA, is("fieldA(a,b)"));
    }

    @Test
    public void testSelection() throws Throwable
    {
        SelectionExpressionNode node = new SelectionExpressionNode(new BinaryExpressionNode(new SelfExpressionNode(), new ConstantExpressionNode(345),
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.EQ), Operation.ALL);
        ExpressionContext context = new ExpressionContext();
        assertThat((List)node.evaluate(context, null), is(Collections.emptyList()));
        assertThat((List)node.evaluate(context, Arrays.asList(123, 345)), is(Arrays.asList(345)));
        assertThat((List)node.evaluate(context, new Object[]{123, 345}), is(Arrays.asList(345)));
        assertThat((List)node.evaluate(context, new int[]{123, 345}), is(Arrays.asList(345)));
        
        node = new SelectionExpressionNode(new BinaryExpressionNode(new SelfExpressionNode(), new ConstantExpressionNode(
            Collections.singletonMap(345, 123).entrySet().iterator().next()),
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.EQ), Operation.ALL);
        assertThat((List)node.evaluate(context, Collections.singletonMap(345, 123)), is(
            com.exametrika.common.utils.Collections.toList(Collections.singletonMap(345, 123).entrySet().iterator())));
        
        node = new SelectionExpressionNode(new BinaryExpressionNode(new SelfExpressionNode(), new ConstantExpressionNode('a'),
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.EQ), Operation.ALL);
        assertThat((List)node.evaluate(context, "abc"), is(Arrays.asList('a')));
        
        node = new SelectionExpressionNode(new BinaryExpressionNode(new SelfExpressionNode(), new ConstantExpressionNode(123),
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.EQ), Operation.FIRST);
        assertThat(node.evaluate(context, null), nullValue());
        assertThat((Integer)node.evaluate(context, Arrays.asList(123, 345)), is(123));
        assertThat((Integer)node.evaluate(context, new Object[]{123, 345}), is(123));
        assertThat((Integer)node.evaluate(context, new int[]{123, 345}), is(123));
        node = new SelectionExpressionNode(new BinaryExpressionNode(new SelfExpressionNode(), new ConstantExpressionNode(
            Collections.singletonMap(123, 345).entrySet().iterator().next()),
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.EQ), Operation.FIRST);
        assertThat((Map.Entry)node.evaluate(context, Collections.singletonMap(123, 345)), is(
            Collections.singletonMap(123, 345).entrySet().iterator().next()));
        
        node = new SelectionExpressionNode(new BinaryExpressionNode(new SelfExpressionNode(), new ConstantExpressionNode('a'),
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.EQ), Operation.FIRST);
        assertThat((Character)node.evaluate(context, "abc"), is('a'));
        
        node = new SelectionExpressionNode(new BinaryExpressionNode(new SelfExpressionNode(), new ConstantExpressionNode(123),
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.EQ), Operation.LAST);
        assertThat(node.evaluate(context, null), nullValue());
        assertThat((Integer)node.evaluate(context, Arrays.asList(123, 345)), is(123));
        assertThat((Integer)node.evaluate(context, new Object[]{123, 345}), is(123));
        assertThat((Integer)node.evaluate(context, new int[]{123, 345}), is(123));
        node = new SelectionExpressionNode(new BinaryExpressionNode(new SelfExpressionNode(), new ConstantExpressionNode(
            Collections.singletonMap(345, 123).entrySet().iterator().next()),
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.EQ), Operation.LAST);
        assertThat((Map.Entry)node.evaluate(context, Collections.singletonMap(345, 123)), is(
            Collections.singletonMap(345, 123).entrySet().iterator().next()));
        
        node = new SelectionExpressionNode(new BinaryExpressionNode(new SelfExpressionNode(), new ConstantExpressionNode('a'),
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.EQ), Operation.LAST);
        assertThat((Character)node.evaluate(context, "abc"), is('a'));
    }
    
    @Test
    public void testStatement() throws Throwable
    {
        StatementExpressionNode node = new StatementExpressionNode(Arrays.<IExpressionNode>asList(new ConstantExpressionNode(123),
            new ConstantExpressionNode(345)));
        ExpressionContext context = new ExpressionContext();
        assertThat((Integer)node.evaluate(context, null), is(345));
        
        node = new StatementExpressionNode(Arrays.<IExpressionNode>asList(new ConstantExpressionNode(123),
            new ReturnExpressionNode(null), new ConstantExpressionNode(345)));
        context = new ExpressionContext();
        assertThat(node.evaluate(context, null), nullValue());
        
        node = new StatementExpressionNode(Arrays.<IExpressionNode>asList(new ConstantExpressionNode(123),
            new ReturnExpressionNode(new ConstantExpressionNode(456)), new ConstantExpressionNode(345)));
        context = new ExpressionContext();
        assertThat((Integer)node.evaluate(context, null), is(456));
        
        node = new StatementExpressionNode(Arrays.<IExpressionNode>asList(new ConstantExpressionNode(123),
            new BreakExpressionNode(), new ConstantExpressionNode(345)));
        context = new ExpressionContext();
        assertThat(node.evaluate(context, null), nullValue());
        
        node = new StatementExpressionNode(Arrays.<IExpressionNode>asList(new ConstantExpressionNode(123),
            new ContinueExpressionNode(), new ConstantExpressionNode(345)));
        context = new ExpressionContext();
        assertThat(node.evaluate(context, null), nullValue());
    }
    
    @Test
    public void testIf() throws Throwable
    {
        IfExpressionNode node = new IfExpressionNode(new ConstantExpressionNode(true),
            new ConstantExpressionNode("first"), new ConstantExpressionNode("second"));
        ExpressionContext context = new ExpressionContext();
        assertThat((String)node.evaluate(context, null), is("first"));
        
        node = new IfExpressionNode(new ConstantExpressionNode(false),
            new ConstantExpressionNode("first"), new ConstantExpressionNode("second"));
        assertThat((String)node.evaluate(context, null), is("second"));
        
        node = new IfExpressionNode(new ConstantExpressionNode(null),
            new ConstantExpressionNode("first"), new ConstantExpressionNode("second"));
        assertThat((String)node.evaluate(context, null), is("second"));
        
        node = new IfExpressionNode(new ConstantExpressionNode(null),
            new ConstantExpressionNode("first"), null);
        assertThat(node.evaluate(context, null), nullValue());
    }
    
    @Test
    public void testTernary() throws Throwable
    {
        TernaryExpressionNode node = new TernaryExpressionNode(new ConstantExpressionNode(true),
            new ConstantExpressionNode("first"), new ConstantExpressionNode("second"));
        ExpressionContext context = new ExpressionContext();
        assertThat((String)node.evaluate(context, null), is("first"));
        
        node = new TernaryExpressionNode(new ConstantExpressionNode(false),
            new ConstantExpressionNode("first"), new ConstantExpressionNode("second"));
        assertThat((String)node.evaluate(context, null), is("second"));
        
        node = new TernaryExpressionNode(new ConstantExpressionNode(null),
            new ConstantExpressionNode("first"), new ConstantExpressionNode("second"));
        assertThat((String)node.evaluate(context, null), is("second"));
    }
    
    @Test
    public void testWhile() throws Throwable
    {
        WhileExpressionNode node = new WhileExpressionNode(new ConstantExpressionNode(true),
            new BreakExpressionNode());
        ExpressionContext context = new ExpressionContext();
        assertThat(node.evaluate(context, null), nullValue());
        assertThat(context.isBreakRequested(), is(false));
        
        node = new WhileExpressionNode(new ConstantExpressionNode(false),
            new ConstantExpressionNode("first"));
        assertThat(node.evaluate(context, null), nullValue());
    }
    
    @Test
    public void testFor() throws Throwable
    {
        ForExpressionNode node = new ForExpressionNode(new VariableExpressionNode("test", 0),
            new ConstantExpressionNode(Arrays.asList(1, 2)), new VariableAssignmentExpressionNode(
                new VariableExpressionNode("test", 1), new BinaryExpressionNode(new VariableExpressionNode("test", 1), 
                new VariableExpressionNode("test", 0), 
                com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.ADDITION)));
        ExpressionContext context = new ExpressionContext();
        context.setVariableValue(1, 0);
        assertThat(node.evaluate(context, null), nullValue());
        assertThat((Integer)context.getVariableValue(0), is(2));
        assertThat((Long)context.getVariableValue(1), is(3l));
        
        node = new ForExpressionNode(new VariableExpressionNode("test", 0),
            new ConstantExpressionNode(Arrays.asList(1, 2)), new BreakExpressionNode());
        assertThat(node.evaluate(context, null), nullValue());
        assertThat((Integer)context.getVariableValue(0), is(1));
        assertThat(context.isBreakRequested(), is(false));
        
        node = new ForExpressionNode(new VariableExpressionNode("test", 0),
            new ConstantExpressionNode(Arrays.asList(1, 2)), new ContinueExpressionNode());
        assertThat(node.evaluate(context, null), nullValue());
        assertThat((Integer)context.getVariableValue(0), is(2));
        assertThat(context.isContinueRequested(), is(false));
    }
    
    @Test
    public void testUnary() throws Throwable
    {
        UnaryExpressionNode node = new UnaryExpressionNode(new ConstantExpressionNode(123), 
            com.exametrika.common.expression.impl.nodes.UnaryExpressionNode.Operation.PLUS);
        ExpressionContext context = new ExpressionContext();
        assertThat((Long)node.evaluate(context, null), is(123l));
        
        node = new UnaryExpressionNode(new ConstantExpressionNode(123), 
            com.exametrika.common.expression.impl.nodes.UnaryExpressionNode.Operation.MINUS);
        assertThat((Long)node.evaluate(context, null), is(-123l));
        
        node = new UnaryExpressionNode(new ConstantExpressionNode(true), 
            com.exametrika.common.expression.impl.nodes.UnaryExpressionNode.Operation.NOT);
        assertThat((Boolean)node.evaluate(context, null), is(false));
        
        node = new UnaryExpressionNode(new ConstantExpressionNode(123), 
            com.exametrika.common.expression.impl.nodes.UnaryExpressionNode.Operation.BNOT);
        assertThat((Long)node.evaluate(context, null), is(~123l));
    }
    
    @Test
    public void testVariable() throws Throwable
    {
        VariableExpressionNode node = new VariableExpressionNode("test", 1);
        ExpressionContext context = new ExpressionContext();
        assertThat(node.evaluate(context, null), nullValue());
        node.setValue(context, "test");
        assertThat((String)node.evaluate(context, null), is("test"));
    }
    
    @Test
    public void testStandardConversionProvider() throws Throwable
    {
        StandardConversionProvider provider = new StandardConversionProvider();
        assertThat(provider.asBoolean(null), is(false));
        assertThat(provider.asBoolean(true), is(true));
        assertThat(provider.asBoolean(1), is(true));
        assertThat(provider.asBoolean(0), is(false));
        assertThat(provider.asBoolean((char)0), is(false));
        assertThat(provider.asBoolean(new Object()), is(true));
        
        assertThat(provider.cast(null, String.class), nullValue());
        assertThat((String)provider.cast(123, String.class), is("123"));
        assertThat((Long)provider.cast("123", Long.class), is(123l));
        assertThat((Long)provider.cast(123.3d, Long.class), is(123l));
        assertThat((Long)provider.cast(true, Long.class), is(1l));
        
        assertThat((Double)provider.cast("123", Double.class), is(123d));
        assertThat((Double)provider.cast(123l, Double.class), is(123d));
        assertThat((Double)provider.cast(true, Double.class), is(1d));
        
        assertThat((Boolean)provider.cast("true", Boolean.class), is(true));
        assertThat((Boolean)provider.cast(123l, Boolean.class), is(true));
        assertThat((Boolean)provider.cast(0, Boolean.class), is(false));
        
        assertThat((Integer)provider.cast(123, Number.class), is(123));
    }
    
    public static class TestA
    {
        public int fieldA = 321;
        public static int fieldC = 345;
        public int b = 123;
        public static int d = 567;
        
        public TestA()
        {
        }
        
        public TestA(int a, int b)
        {
            fieldA = 123;
        }
        
        public int getFieldB()
        {
            return b;
        }
        
        public void setFieldB(int value)
        {
            b = value;
        }
        
        public static int getFieldD()
        {
            return d;
        }
        
        public static void setFieldD(int value)
        {
            d = value;
        }
        
        public String methodA()
        {
            return "A.methodA";
        }
        
        public String methodA(int a, int b)
        {
            return "A.methodA(a,b)";
        }
        
        public static String methodB()
        {
            return "A.methodB";
        }
        
        public static String methodB(int a, int b)
        {
            return "A.methodB(a,b)";
        }
    }
    
    public static class TestB extends TestA
    {
        public Object fieldA = 432;
        public static int fieldC = 789;
        public int b2 = 345;
        public static int d2 = 765;
        
        public TestB()
        {
        }
        
        public TestB(int a, int b)
        {
            super(a, b);
            fieldA = 567;
        }
        
        @Override
        public int getFieldB()
        {
            return b2;
        }
        
        @Override
        public void setFieldB(int value)
        {
            b2 = value;
        }
        
        public static int getFieldD()
        {
            return d2;
        }
        
        public static void setFieldD(int value)
        {
            d2 = value;
        }
        
        @Override
        public String methodA()
        {
            return "B.methodA";
        }
        
        @Override
        public String methodA(int a, int b)
        {
            return "B.methodA(a,b)";
        }
        
        public static String methodB()
        {
            return "B.methodB";
        }
        
        public static String methodB(int a, int b)
        {
            return "B.methodB(a,b)";
        }
    }
    
    public static class TestC
    {
        public String fieldA = "fieldA";
        public static String fieldC = "fieldC";
        public String b3 = "fieldB";
        public static String d3 = "fieldD";
        
        public TestC()
        {
        }
        
        public TestC(int a, int b)
        {
            fieldA = "fieldA(a,b)";
        }
        
        public String getFieldB()
        {
            return b3;
        }
        
        public void setFieldB(String value)
        {
            b3 = value;
        }
        
        public static String getFieldD()
        {
            return d3;
        }
        
        public static void setFieldD(String value)
        {
            d3 = value;
        }
        
        public String methodA()
        {
            return "C.methodA";
        }
        
        public String methodA(int a, int b)
        {
            return "C.methodA(a,b)";
        }
        
        public static String methodB()
        {
            return "C.methodB";
        }
        
        public static String methodB(int a, int b)
        {
            return "C.methodB(a,b)";
        }
    }
}
