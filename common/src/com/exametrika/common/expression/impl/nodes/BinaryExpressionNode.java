/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl.nodes;

import com.exametrika.common.expression.IConversionProvider;
import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.utils.Assert;





/**
 * The {@link BinaryExpressionNode} is a binary expression node.
 * 
 * @threadsafety This class and its methods are thread safe.
 * @author Medvedev-A
 */
public final class BinaryExpressionNode implements IExpressionNode
{
    private static final double EPSILON = 0.000001;
    private final IExpressionNode firstExpression;
    private final IExpressionNode secondExpression;
    private final Operation operation;

    public enum Operation
    {
        OR, AND, BOR, BAND, XOR,
        EQ, NEQ, LT, LTE, GT, GTE,
        SHL, SHR, USHR,
        ADDITION, SUBTRACTION, MULTIPLICATION, DIVISION, REMAINDER,
        IN, NOT_IN
    }
    
    public BinaryExpressionNode(IExpressionNode firstExpression, IExpressionNode secondExpression, Operation operation)
    {
        Assert.notNull(firstExpression);
        Assert.notNull(secondExpression);
        Assert.notNull(operation);
        
        this.firstExpression = firstExpression;
        this.secondExpression = secondExpression;
        this.operation = operation;
    }
    
    @Override
    public Object evaluate(ExpressionContext context, Object self)
    {
        Object first = this.firstExpression.evaluate(context, self);
        Object second = null;
        if (operation != Operation.OR && operation != Operation.AND)
            second = this.secondExpression.evaluate(context, self);
        
        IConversionProvider conversionProvider = context.getConversionProvider();
        switch (operation)
        {
        case OR:
            if (conversionProvider.asBoolean(first))
                return true;
            second = this.secondExpression.evaluate(context, self);
            return conversionProvider.asBoolean(second);
        case AND:
            if (!conversionProvider.asBoolean(first))
                return false;
            second = this.secondExpression.evaluate(context, self);
            return conversionProvider.asBoolean(second);
        case BOR:
            return ((Number)first).longValue() | ((Number)second).longValue();
        case BAND:
            return ((Number)first).longValue() & ((Number)second).longValue();
        case XOR:
            return ((Number)first).longValue() ^ ((Number)second).longValue();
        case EQ:
            if (first == null)
                return first == second;
            else if (first instanceof Number && second instanceof Number)
            {
                if (first instanceof Double || second instanceof Double)
                    return Math.abs(((Number)first).doubleValue() - ((Number)second).doubleValue()) <= EPSILON;
                else
                    return ((Number)first).longValue() == ((Number)second).longValue();
            }
            else
                return first.equals(second);
        case NEQ:
            if (first == null)
                return first != second;
            else if (first instanceof Number && second instanceof Number)
            {
                if (first instanceof Double || second instanceof Double)
                    return Math.abs(((Number)first).doubleValue() - ((Number)second).doubleValue()) > EPSILON;
                else
                    return ((Number)first).longValue() != ((Number)second).longValue();
            }
            else
                return !first.equals(second);
        case LT:
            if (first instanceof Number && second instanceof Number)
            {
                if (first instanceof Double || second instanceof Double)
                    return ((Number)first).doubleValue() < ((Number)second).doubleValue();
                else
                    return ((Number)first).longValue() < ((Number)second).longValue();
            }
            else
                return ((Comparable)first).compareTo(second) < 0;
        case LTE:
            if (first instanceof Number && second instanceof Number)
            {
                if (first instanceof Double || second instanceof Double)
                    return ((Number)first).doubleValue() <= ((Number)second).doubleValue();
                else
                    return ((Number)first).longValue() <= ((Number)second).longValue();
            }
            else
                return ((Comparable)first).compareTo(second) <= 0;
        case GT:
            if (first instanceof Number && second instanceof Number)
            {
                if (first instanceof Double || second instanceof Double)
                    return ((Number)first).doubleValue() > ((Number)second).doubleValue();
                else
                    return ((Number)first).longValue() > ((Number)second).longValue();
            }
            else
                return ((Comparable)first).compareTo(second) > 0;
        case GTE:
            if (first instanceof Number && second instanceof Number)
            {
                if (first instanceof Double || second instanceof Double)
                    return ((Number)first).doubleValue() >= ((Number)second).doubleValue();
                else
                    return ((Number)first).longValue() >= ((Number)second).longValue();
            }
            else
                return ((Comparable)first).compareTo(second) >= 0;
        case SHL:
            return ((Number)first).longValue() << ((Number)second).longValue();
        case SHR:
            return ((Number)first).longValue() >> ((Number)second).longValue();
        case USHR:
            return ((Number)first).longValue() >>> ((Number)second).longValue();
        case ADDITION:
            if (first instanceof Number && second instanceof Number)
            {
                if (first instanceof Double || second instanceof Double)
                    return ((Number)first).doubleValue() + ((Number)second).doubleValue();
                else
                    return ((Number)first).longValue() + ((Number)second).longValue();
            }
            else
            {
                if (first == null)
                    first = "null";
                if (second == null)
                    second = "null";
                return first.toString() + second.toString();
            }
        case SUBTRACTION:
            if (first instanceof Double || second instanceof Double)
                return ((Number)first).doubleValue() - ((Number)second).doubleValue();
            else
                return ((Number)first).longValue() - ((Number)second).longValue();
        case MULTIPLICATION:
            if (first instanceof Double || second instanceof Double)
                return ((Number)first).doubleValue() * ((Number)second).doubleValue();
            else
                return ((Number)first).longValue() * ((Number)second).longValue();
        case DIVISION:
            if (first instanceof Double || second instanceof Double)
                return ((Number)first).doubleValue() / ((Number)second).doubleValue();
            else
                return ((Number)first).longValue() / ((Number)second).longValue();
        case REMAINDER:
            if (first instanceof Double || second instanceof Double)
                return ((Number)first).doubleValue() % ((Number)second).doubleValue();
            else
                return ((Number)first).longValue() % ((Number)second).longValue();
        case IN:
            return context.getCollectionProvider().contains(second, first);
        case NOT_IN:
            return !context.getCollectionProvider().contains(second, first);
        default:
            return Assert.error();
        }
    }
    
    @Override
    public String toString()
    {
        return firstExpression.toString() + " " + toString(operation) + " " + secondExpression.toString();
    }

    private String toString(Operation operation)
    {
        switch (operation)
        {
        case OR:
            return "||";
        case AND:
            return "&&";
        case BOR:
            return "|";
        case BAND:
            return "&";
        case XOR:
            return "^";
        case EQ:
            return "==";
        case NEQ:
            return "!=";
        case LT:
            return "<";
        case LTE:
            return "<=";
        case GT:
            return ">";
        case GTE:
            return ">=";
        case SHL:
            return "<<";
        case SHR:
            return ">>";
        case USHR:
            return ">>>";
        case ADDITION:
            return "+";
        case SUBTRACTION:
            return "-";
        case MULTIPLICATION:
            return "*";
        case DIVISION:
            return "/";
        case REMAINDER:
            return "%";
        case IN:
            return "in";
        case NOT_IN:
            return "not in";
        default:
            return Assert.error();
        }
    }
}
