/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.perftests.common.expression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IClassResolver;
import com.exametrika.common.expression.ICollectionProvider;
import com.exametrika.common.expression.IConversionProvider;
import com.exametrika.common.expression.IExpression;
import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.expression.impl.StandardClassResolver;
import com.exametrika.common.expression.impl.StandardCollectionProvider;
import com.exametrika.common.expression.impl.StandardConversionProvider;
import com.exametrika.common.expression.impl.nodes.ArrayExpressionNode;
import com.exametrika.common.expression.impl.nodes.BinaryExpressionNode;
import com.exametrika.common.expression.impl.nodes.BreakExpressionNode;
import com.exametrika.common.expression.impl.nodes.CastExpressionNode;
import com.exametrika.common.expression.impl.nodes.ConstantExpressionNode;
import com.exametrika.common.expression.impl.nodes.ConstructorExpressionNode;
import com.exametrika.common.expression.impl.nodes.ElvisExpressionNode;
import com.exametrika.common.expression.impl.nodes.ForExpressionNode;
import com.exametrika.common.expression.impl.nodes.IfExpressionNode;
import com.exametrika.common.expression.impl.nodes.IsExpressionNode;
import com.exametrika.common.expression.impl.nodes.LikeExpressionNode;
import com.exametrika.common.expression.impl.nodes.MapExpressionNode;
import com.exametrika.common.expression.impl.nodes.MethodExpressionNode;
import com.exametrika.common.expression.impl.nodes.NullSafeExpressionNode;
import com.exametrika.common.expression.impl.nodes.PropertyAssignmentExpressionNode;
import com.exametrika.common.expression.impl.nodes.PropertyExpressionNode;
import com.exametrika.common.expression.impl.nodes.StatementExpressionNode;
import com.exametrika.common.expression.impl.nodes.TernaryExpressionNode;
import com.exametrika.common.expression.impl.nodes.UnaryExpressionNode;
import com.exametrika.common.expression.impl.nodes.VariableAssignmentExpressionNode;
import com.exametrika.common.expression.impl.nodes.VariableExpressionNode;
import com.exametrika.common.expression.impl.nodes.WhileExpressionNode;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.perf.Benchmark;
import com.exametrika.common.perf.Probe;
import com.exametrika.common.utils.Pair;
import com.exametrika.tests.common.expression.ExpressionNodeTests.TestA;


/**
 * The {@link ExpressionNodePerfTests} are tests for {@link IExpressionNode}.
 * 
 * @see Expressions
 * @author Medvedev-A
 */
public class ExpressionNodePerfTests
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ExpressionNodePerfTests.class);
    private static final int COUNT = 1000000;

    @Test
    public void testContext() throws Throwable
    {
        final ICollectionProvider collectionProvider = new StandardCollectionProvider();
        final IClassResolver classResolver = new StandardClassResolver();
        final IConversionProvider conversionProvider = new StandardConversionProvider();
        final ConstantExpressionNode node = new ConstantExpressionNode(123);
        
        logger.log(LogLevel.INFO, messages.evaluateContext(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                ExpressionContext context = new ExpressionContext(null, null, collectionProvider, classResolver, conversionProvider, 10);
                node.evaluate(context, null);
            }
        }, COUNT)));
    }
    
    @Test
    public void testArray() throws Throwable
    {
        final ArrayExpressionNode node = new ArrayExpressionNode(Arrays.<IExpressionNode>asList(new ConstantExpressionNode(123),
            new ConstantExpressionNode(null)));
        final ExpressionContext context = new ExpressionContext();
        
        logger.log(LogLevel.INFO, messages.evaluateArray(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.evaluate(context, null);
            }
        }, COUNT)));
    }
    
    @Test
    public void testVariableAssignment() throws Throwable
    {
        VariableExpressionNode var = new VariableExpressionNode("", 0);
        final VariableAssignmentExpressionNode node = new VariableAssignmentExpressionNode(var, new ConstantExpressionNode(123));
        final ExpressionContext context = new ExpressionContext();
        
        logger.log(LogLevel.INFO, messages.evaluateAssignment(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.evaluate(context, null);
            }
        }, COUNT)));
    }
    
    @Test
    public void testBinary() throws Throwable
    {
        final BinaryExpressionNode node = new BinaryExpressionNode(new ConstantExpressionNode(true), new ConstantExpressionNode(false), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.OR);
        final ExpressionContext context = new ExpressionContext();
        
        logger.log(LogLevel.INFO, messages.evaluateOr(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.evaluate(context, null);
            }
        }, COUNT)));
        
        final BinaryExpressionNode node2 = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(123), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.EQ);
        logger.log(LogLevel.INFO, messages.evaluateEq(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node2.evaluate(context, null);
            }
        }, COUNT)));
        
        final BinaryExpressionNode node3 = new BinaryExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode(345), 
            com.exametrika.common.expression.impl.nodes.BinaryExpressionNode.Operation.ADDITION);
        
        logger.log(LogLevel.INFO, messages.evaluateAdd(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node3.evaluate(context, null);
            }
        }, COUNT)));
    }
    
    @Test
    public void testCast() throws Throwable
    {
        final CastExpressionNode node = new CastExpressionNode(new ConstantExpressionNode("test"), new ConstantExpressionNode("String"));
        final ExpressionContext context = new ExpressionContext();
        
        logger.log(LogLevel.INFO, messages.evaluateStringStringCast(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.evaluate(context, null);
            }
        }, COUNT)));
        
        final CastExpressionNode node2 = new CastExpressionNode(new ConstantExpressionNode(123), new ConstantExpressionNode("String"));

        logger.log(LogLevel.INFO, messages.evaluateIntStringCast(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node2.evaluate(context, null);
            }
        }, COUNT)));
    }
    
    @Test
    public void testElvis() throws Throwable
    {
        final ElvisExpressionNode node = new ElvisExpressionNode(new ConstantExpressionNode("test"), new ConstantExpressionNode("default"));
        final ExpressionContext context = new ExpressionContext();
        
        logger.log(LogLevel.INFO, messages.evaluateElvis(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.evaluate(context, null);
            }
        }, COUNT)));
    }
    
    @Test
    public void testIndex() throws Throwable
    {
        final PropertyExpressionNode node = new PropertyExpressionNode(true, new ConstantExpressionNode(0), null, false);
        final ExpressionContext context = new ExpressionContext();
        final List<Integer> array = Arrays.asList(123);
        
        logger.log(LogLevel.INFO, messages.evaluateArrayIndex(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.evaluate(context, array);
            }
        }, COUNT)));
        
        final PropertyExpressionNode node2 = new PropertyExpressionNode(true, new ConstantExpressionNode("test"), null, false);
        final Map<String, Integer> map = Collections.singletonMap("test", 123);
        
        logger.log(LogLevel.INFO, messages.evaluateMapIndex(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node2.evaluate(context, map);
            }
        }, COUNT)));
    }
    
    @Test
    public void testIndexAssignment() throws Throwable
    {
        final PropertyAssignmentExpressionNode node = new PropertyAssignmentExpressionNode(true, new ConstantExpressionNode(0), null, false,
            new ConstantExpressionNode("123"));
        final ExpressionContext context = new ExpressionContext();
        final List<Integer> array = new ArrayList<Integer>();
        
        logger.log(LogLevel.INFO, messages.evaluateArrayIndexAssignment(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.evaluate(context, array);
            }
        }, COUNT)));
        
        final PropertyAssignmentExpressionNode node2 = new PropertyAssignmentExpressionNode(true, new ConstantExpressionNode("test"), null, false,
            new ConstantExpressionNode(123));
        final Map<String, Integer> map = new HashMap<String, Integer>();
        
        logger.log(LogLevel.INFO, messages.evaluateMapIndexAssignment(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node2.evaluate(context, map);
            }
        }, COUNT)));
    }
    
    @Test
    public void testIs() throws Throwable
    {
        final IsExpressionNode node = new IsExpressionNode(new ConstantExpressionNode("test"), new ConstantExpressionNode("java.lang.String"));
        final ExpressionContext context = new ExpressionContext();
        
        logger.log(LogLevel.INFO, messages.evaluateIs(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.evaluate(context, null);
            }
        }, COUNT)));
    }
    
    @Test
    public void testMap() throws Throwable
    {
        final MapExpressionNode node = new MapExpressionNode(Arrays.<Pair<IExpressionNode, IExpressionNode>>asList(
            new Pair(new ConstantExpressionNode(123),new ConstantExpressionNode(123)),
            new Pair(new ConstantExpressionNode(null),new ConstantExpressionNode(null))));
        final ExpressionContext context = new ExpressionContext();
        
        logger.log(LogLevel.INFO, messages.evaluateMap(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.evaluate(context, null);
            }
        }, COUNT)));
    }
    
    @Test
    public void testLike() throws Throwable
    {
        final LikeExpressionNode node = new LikeExpressionNode(new ConstantExpressionNode("Hello world!"),new ConstantExpressionNode("He*ll? w*d?"));
        final ExpressionContext context = new ExpressionContext();
        
        logger.log(LogLevel.INFO, messages.evaluateLike(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.evaluate(context, null);
            }
        }, COUNT)));
    }
    
    @Test
    public void testNullSafe() throws Throwable
    {
        final NullSafeExpressionNode node = new NullSafeExpressionNode(new ConstantExpressionNode("test"));
        final ExpressionContext context = new ExpressionContext();
        
        logger.log(LogLevel.INFO, messages.evaluateNullSafe(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.evaluate(context, "abc");
            }
        }, COUNT)));
    }
    
    @Test
    public void testStatement() throws Throwable
    {
        final StatementExpressionNode node = new StatementExpressionNode(Arrays.<IExpressionNode>asList(new ConstantExpressionNode(123),
            new ConstantExpressionNode(345)));
        final ExpressionContext context = new ExpressionContext();
        
        logger.log(LogLevel.INFO, messages.evaluateStatement(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.evaluate(context, null);
            }
        }, COUNT)));
    }
    
    @Test
    public void testIf() throws Throwable
    {
        final IfExpressionNode node = new IfExpressionNode(new ConstantExpressionNode(true),
            new ConstantExpressionNode("first"), new ConstantExpressionNode("second"));
        final ExpressionContext context = new ExpressionContext();
        
        logger.log(LogLevel.INFO, messages.evaluateIf(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.evaluate(context, null);
            }
        }, COUNT)));
    }
    
    @Test
    public void testTernary() throws Throwable
    {
        final TernaryExpressionNode node = new TernaryExpressionNode(new ConstantExpressionNode(true),
            new ConstantExpressionNode("first"), new ConstantExpressionNode("second"));
        final ExpressionContext context = new ExpressionContext();
        
        logger.log(LogLevel.INFO, messages.evaluateTernary(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.evaluate(context, null);
            }
        }, COUNT)));
    }
    
    @Test
    public void testWhile() throws Throwable
    {
        final WhileExpressionNode node = new WhileExpressionNode(new ConstantExpressionNode(true),
            new BreakExpressionNode());
        final ExpressionContext context = new ExpressionContext();
        
        logger.log(LogLevel.INFO, messages.evaluateWhile(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.evaluate(context, null);
            }
        }, COUNT)));
    }
    
    @Test
    public void testFor() throws Throwable
    {
        final ForExpressionNode node = new ForExpressionNode(new VariableExpressionNode("", 0),
            new ConstantExpressionNode(Collections.singletonList(1)), new ConstantExpressionNode(123));
        final ExpressionContext context = new ExpressionContext();
        context.setVariableValue(1, 0);
        
        logger.log(LogLevel.INFO, messages.evaluateFor(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.evaluate(context, null);
            }
        }, COUNT)));
    }
    
    @Test
    public void testUnary() throws Throwable
    {
        final UnaryExpressionNode node = new UnaryExpressionNode(new ConstantExpressionNode(123), 
            com.exametrika.common.expression.impl.nodes.UnaryExpressionNode.Operation.MINUS);
        final ExpressionContext context = new ExpressionContext();
        
        logger.log(LogLevel.INFO, messages.evaluateMinus(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.evaluate(context, null);
            }
        }, COUNT)));
        
        final UnaryExpressionNode node2 = new UnaryExpressionNode(new ConstantExpressionNode(true), 
            com.exametrika.common.expression.impl.nodes.UnaryExpressionNode.Operation.NOT);
        
        logger.log(LogLevel.INFO, messages.evaluateNot(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node2.evaluate(context, null);
            }
        }, COUNT)));
    }
    
    @Test
    public void testVariable() throws Throwable
    {
        final VariableExpressionNode node = new VariableExpressionNode("", 1);
        final ExpressionContext context = new ExpressionContext();
        
        logger.log(LogLevel.INFO, messages.evaluateVariableSet(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.setValue(context, "test");
            }
        }, COUNT)));
        
        logger.log(LogLevel.INFO, messages.evaluateVariableGet(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.evaluate(context, null);
            }
        }, COUNT)));
    }
    
    @Test
    public void testProperty() throws Throwable
    {
        final PropertyExpressionNode node = new PropertyExpressionNode(true, new ConstantExpressionNode("fieldA"), null, false);
        final ExpressionContext context = new ExpressionContext();
        final Map<String, String> map = Collections.singletonMap("fieldA", "map");
        final TestA a = new TestA();
        
        logger.log(LogLevel.INFO, messages.evaluatePropertyMap(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.evaluate(context, map);
            }
        }, COUNT)));
        
        logger.log(LogLevel.INFO, messages.evaluatePropertyField(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.evaluate(context, a);
            }
        }, COUNT)));
        
        final PropertyExpressionNode node2 = new PropertyExpressionNode(true, new ConstantExpressionNode("fieldB"), null, false);
        logger.log(LogLevel.INFO, messages.evaluatePropertyGetter(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node2.evaluate(context, a);
            }
        }, COUNT)));
        
        final PropertyExpressionNode node3 = new PropertyExpressionNode(true, new ConstantExpressionNode("fieldC"), new ConstantExpressionNode(TestA.class.getName()), true);
        logger.log(LogLevel.INFO, messages.evaluatePropertyStaticField(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node3.evaluate(context, null);
            }
        }, COUNT)));
        
        final PropertyExpressionNode node4 = new PropertyExpressionNode(true, new ConstantExpressionNode("fieldD"), new ConstantExpressionNode(TestA.class.getName()), true);
        logger.log(LogLevel.INFO, messages.evaluatePropertyStaticGetter(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node4.evaluate(context, null);
            }
        }, COUNT)));
    }
    
    @Test
    public void testPropertyAssignment() throws Throwable
    {
        final PropertyAssignmentExpressionNode node = new PropertyAssignmentExpressionNode(true, new ConstantExpressionNode("fieldA"), null, false,
            new ConstantExpressionNode(123));
        final ExpressionContext context = new ExpressionContext();
        final TestA a = new TestA();
        
        logger.log(LogLevel.INFO, messages.evaluatePropertyFieldAssignment(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.evaluate(context, a);
            }
        }, COUNT)));
        
        final PropertyAssignmentExpressionNode node2 = new PropertyAssignmentExpressionNode(true, new ConstantExpressionNode("fieldB"), null, false,
            new ConstantExpressionNode(123));
        logger.log(LogLevel.INFO, messages.evaluatePropertySetterAssignment(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node2.evaluate(context, a);
            }
        }, COUNT)));
        
        final PropertyAssignmentExpressionNode node3 = new PropertyAssignmentExpressionNode(true, new ConstantExpressionNode("fieldC"), 
            new ConstantExpressionNode(TestA.class.getName()), true, new ConstantExpressionNode(123));
        logger.log(LogLevel.INFO, messages.evaluatePropertyStaticFieldAssignment(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node3.evaluate(context, null);
            }
        }, COUNT)));
        
        final PropertyAssignmentExpressionNode node4 = new PropertyAssignmentExpressionNode(true, new ConstantExpressionNode("fieldD"), 
            new ConstantExpressionNode(TestA.class.getName()), true, new ConstantExpressionNode(123));
        logger.log(LogLevel.INFO, messages.evaluatePropertyStaticSetterAssignment(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node4.evaluate(context, null);
            }
        }, COUNT)));
    }
    
    @Test
    public void testMethod() throws Throwable
    {
        final MethodExpressionNode node = new MethodExpressionNode(true, new ConstantExpressionNode("methodA"), Arrays.<IExpressionNode>asList(
            new ConstantExpressionNode(123), new ConstantExpressionNode(345)), null, false);
        final ExpressionContext context = new ExpressionContext();
        final TestA a = new TestA();
        
        logger.log(LogLevel.INFO, messages.evaluateMethod(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.evaluate(context, a);
            }
        }, COUNT)));
        
        final MethodExpressionNode node2 = new MethodExpressionNode(true, new ConstantExpressionNode("methodB"), Arrays.<IExpressionNode>asList(
            new ConstantExpressionNode(123), new ConstantExpressionNode(345)), new ConstantExpressionNode(TestA.class.getName()), true);
        
        logger.log(LogLevel.INFO, messages.evaluateStaticMethod(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node2.evaluate(context, null);
            }
        }, COUNT)));
    }
    
    @Test
    public void testConstructor() throws Throwable
    {
        final ConstructorExpressionNode node = new ConstructorExpressionNode(Arrays.<IExpressionNode>asList(
            new ConstantExpressionNode(123), new ConstantExpressionNode(345)), new ConstantExpressionNode(TestA.class.getName()));
        final ExpressionContext context = new ExpressionContext();
        
        logger.log(LogLevel.INFO, messages.evaluateConstructor(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                node.evaluate(context, null);
            }
        }, COUNT)));
    }
    
    @Test
    public void testComplexExpression() throws Throwable
    {
        CompileContext compileContext = Expressions.createCompileContext(null);
        final IExpression expression = Expressions.compile("$root.fieldB + $context.b", compileContext);
        final TestA a = new TestA();
        final Map<String, Object> runtimeContext = Expressions.createRuntimeContext(null, false);
        runtimeContext.put("b", 100);
        logger.log(LogLevel.INFO, messages.evaluateComplex(new Benchmark(new Probe()
        {
            @Override
            public void runOnce()
            {
                expression.execute(a, runtimeContext);
            }
        }, COUNT)));
    }
    
    private interface IMessages
    {
        @DefaultMessage("Evaluate context creation ''{0}''.")
        ILocalizedMessage evaluateContext(Object benchmark);
        
        @DefaultMessage("Evaluate array ''{0}''.")
        ILocalizedMessage evaluateArray(Object benchmark);
        
        @DefaultMessage("Evaluate assignment ''{0}''.")
        ILocalizedMessage evaluateAssignment(Object benchmark);
        
        @DefaultMessage("Evaluate OR ''{0}''.")
        ILocalizedMessage evaluateOr(Object benchmark);
        
        @DefaultMessage("Evaluate EQ ''{0}''.")
        ILocalizedMessage evaluateEq(Object benchmark);
        
        @DefaultMessage("Evaluate ADD ''{0}''.")
        ILocalizedMessage evaluateAdd(Object benchmark);
        
        @DefaultMessage("Evaluate String to String cast ''{0}''.")
        ILocalizedMessage evaluateStringStringCast(Object benchmark);
        
        @DefaultMessage("Evaluate Integer to String cast ''{0}''.")
        ILocalizedMessage evaluateIntStringCast(Object benchmark);
        
        @DefaultMessage("Evaluate elvis ''{0}''.")
        ILocalizedMessage evaluateElvis(Object benchmark);
        
        @DefaultMessage("Evaluate array index ''{0}''.")
        ILocalizedMessage evaluateArrayIndex(Object benchmark);
        
        @DefaultMessage("Evaluate map index ''{0}''.")
        ILocalizedMessage evaluateMapIndex(Object benchmark);
        
        @DefaultMessage("Evaluate array index assignment ''{0}''.")
        ILocalizedMessage evaluateArrayIndexAssignment(Object benchmark);
        
        @DefaultMessage("Evaluate map index assignment ''{0}''.")
        ILocalizedMessage evaluateMapIndexAssignment(Object benchmark);
        
        @DefaultMessage("Evaluate IS ''{0}''.")
        ILocalizedMessage evaluateIs(Object benchmark);
        
        @DefaultMessage("Evaluate map ''{0}''.")
        ILocalizedMessage evaluateMap(Object benchmark);
        
        @DefaultMessage("Evaluate LIKE ''{0}''.")
        ILocalizedMessage evaluateLike(Object benchmark);
        
        @DefaultMessage("Evaluate null-safe ''{0}''.")
        ILocalizedMessage evaluateNullSafe(Object benchmark);
        
        @DefaultMessage("Evaluate statement ''{0}''.")
        ILocalizedMessage evaluateStatement(Object benchmark);
        
        @DefaultMessage("Evaluate template ''{0}''.")
        ILocalizedMessage evaluateTemplate(Object benchmark);
        
        @DefaultMessage("Evaluate if ''{0}''.")
        ILocalizedMessage evaluateIf(Object benchmark);
        
        @DefaultMessage("Evaluate MINUS ''{0}''.")
        ILocalizedMessage evaluateMinus(Object benchmark);
        
        @DefaultMessage("Evaluate NOT ''{0}''.")
        ILocalizedMessage evaluateNot(Object benchmark);
        
        @DefaultMessage("Evaluate variable get ''{0}''.")
        ILocalizedMessage evaluateVariableGet(Object benchmark);
        
        @DefaultMessage("Evaluate variable set ''{0}''.")
        ILocalizedMessage evaluateVariableSet(Object benchmark);
        
        @DefaultMessage("Evaluate property map ''{0}''.")
        ILocalizedMessage evaluatePropertyMap(Object benchmark);
        
        @DefaultMessage("Evaluate property field ''{0}''.")
        ILocalizedMessage evaluatePropertyField(Object benchmark);
        
        @DefaultMessage("Evaluate property getter ''{0}''.")
        ILocalizedMessage evaluatePropertyGetter(Object benchmark);
        
        @DefaultMessage("Evaluate property static field ''{0}''.")
        ILocalizedMessage evaluatePropertyStaticField(Object benchmark);
        
        @DefaultMessage("Evaluate property static getter ''{0}''.")
        ILocalizedMessage evaluatePropertyStaticGetter(Object benchmark);
        
        @DefaultMessage("Evaluate property field assignment''{0}''.")
        ILocalizedMessage evaluatePropertyFieldAssignment(Object benchmark);
        
        @DefaultMessage("Evaluate property setter assignment''{0}''.")
        ILocalizedMessage evaluatePropertySetterAssignment(Object benchmark);
        
        @DefaultMessage("Evaluate property static field assignment''{0}''.")
        ILocalizedMessage evaluatePropertyStaticFieldAssignment(Object benchmark);
        
        @DefaultMessage("Evaluate property static setter assignment''{0}''.")
        ILocalizedMessage evaluatePropertyStaticSetterAssignment(Object benchmark);
        
        @DefaultMessage("Evaluate method ''{0}''.")
        ILocalizedMessage evaluateMethod(Object benchmark);
        
        @DefaultMessage("Evaluate static method ''{0}''.")
        ILocalizedMessage evaluateStaticMethod(Object benchmark);
        
        @DefaultMessage("Evaluate constructor ''{0}''.")
        ILocalizedMessage evaluateConstructor(Object benchmark);
        
        @DefaultMessage("Evaluate while ''{0}''.")
        ILocalizedMessage evaluateWhile(Object benchmark);
        
        @DefaultMessage("Evaluate for ''{0}''.")
        ILocalizedMessage evaluateFor(Object benchmark);
        
        @DefaultMessage("Evaluate ternary ''{0}''.")
        ILocalizedMessage evaluateTernary(Object benchmark);
        
        @DefaultMessage("Evaluate complex ''{0}''.")
        ILocalizedMessage evaluateComplex(Object benchmark);
    }
}
