/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.exametrika.common.expression.impl.ExpressionTokenizer.Token;
import com.exametrika.common.expression.impl.nodes.ArrayExpressionNode;
import com.exametrika.common.expression.impl.nodes.BinaryExpressionNode;
import com.exametrika.common.expression.impl.nodes.BreakExpressionNode;
import com.exametrika.common.expression.impl.nodes.CastExpressionNode;
import com.exametrika.common.expression.impl.nodes.ConstantExpressionNode;
import com.exametrika.common.expression.impl.nodes.ConstructorExpressionNode;
import com.exametrika.common.expression.impl.nodes.ContextExpressionNode;
import com.exametrika.common.expression.impl.nodes.ContinueExpressionNode;
import com.exametrika.common.expression.impl.nodes.DebugExpressionNode;
import com.exametrika.common.expression.impl.nodes.ElvisExpressionNode;
import com.exametrika.common.expression.impl.nodes.ForExpressionNode;
import com.exametrika.common.expression.impl.nodes.GroupExpressionNode;
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
import com.exametrika.common.expression.impl.nodes.RootExpressionNode;
import com.exametrika.common.expression.impl.nodes.SelectionExpressionNode;
import com.exametrika.common.expression.impl.nodes.SelfExpressionNode;
import com.exametrika.common.expression.impl.nodes.StatementExpressionNode;
import com.exametrika.common.expression.impl.nodes.TernaryExpressionNode;
import com.exametrika.common.expression.impl.nodes.TrackExpressionNode;
import com.exametrika.common.expression.impl.nodes.UnaryExpressionNode;
import com.exametrika.common.expression.impl.nodes.VariableAssignmentExpressionNode;
import com.exametrika.common.expression.impl.nodes.VariableExpressionNode;
import com.exametrika.common.expression.impl.nodes.WhileExpressionNode;
import com.exametrika.common.expression.impl.nodes.SelectionExpressionNode.Operation;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;

/**
 * The {@link ExpressionParser} is an expression parser. Expression can be one of the following:
 * <ul>
 * <li> comments: singleline - //text, multiline - /*text{@literal *}/
 * <li> constant or literal:
 *   <ul>
 *   <li> string - "text", 'text', can contain \r, \n, \f, \b, \t, \f, {@literal \}u<xxxx>, \\
 *   <li> unescaped string - \text\, does not interpret escape sequences like \{@literal <x>}
 *   <li> decimal number - 123, 123.456, 123.456e-78, 123.456E+78
 *   <li> hex number - 0x1234567890ABCDEF
 *   <li> boolean - true or false
 *   <li> null 
 *   <li> identifier - [letter or _]+[letter or digit or _]*, abc123, _abc123
 *   <li> qualified identifier - id1.id2...
 *   </ul>
 * <li> statement: expr1;expr2... Result - last expression result
 * <li> group: (expr)
 * <li> unary: {@literal <op>} expr, where op one of:
 *   <ul> 
 *   <li> +/- {@literal <number>}
 *   <li> !/not {@literal <boolean>}
 *   <li> ~/bnot {@literal <number>}
 *   </ul>
 * <li>
 * <li> binary expression: expr1 {@literal <op>} expr2, where op one of:
 *   <ul>
 *     <li> {@literal <boolean>} ||/or {@literal <boolean>}, supports shirt circuiting
 *     <li> {@literal <boolean>} &&/and {@literal <boolean>}, supports shirt circuiting
 *     <li> {@literal <any>} ==/eq {@literal <any>}
 *     <li> {@literal <any>} !=/<>/neq {@literal <any>}
 *     <li> {@literal <number|comparable>} {@literal </lt} {@literal <number|comparable>}
 *     <li> {@literal <number|comparable>} {@literal <=/lte} {@literal <number|comparable>}
 *     <li> {@literal <number|comparable>} {@literal >/gt} {@literal <number|comparable>}
 *     <li> {@literal <number|comparable>} {@literal >=/gte} {@literal <number|comparable>}
 *     <li> {@literal <number|string>} +/add {@literal <number|string>}
 *     <li> {@literal <number>} -/sub {@literal <number>}
 *     <li> {@literal <number>} * /mul {@literal <number>}
 *     <li> {@literal <number>} / /div {@literal <number>}
 *     <li> {@literal <number>} %/rem {@literal <number>}
 *     <li> {@literal <number>} |/bor {@literal <number>}
 *     <li> {@literal <number>} &/band {@literal <number>}
 *     <li> {@literal <number>} ^/xor {@literal <number>}
 *     <li> {@literal <number>} {@literal <</shl} {@literal <number>}
 *     <li> {@literal <number>} {@literal>>/shr} {@literal <number>}
 *     <li> {@literal <number>} {@literal>>>/ushr} {@literal <number>}
 *     <li> {@literal <any>} in {@literal <collection>}
 *     <li> {@literal <any>} not in {@literal <collection>}
 *     <li> {@literal <string>} #/like {@literal <string>}, pattern can be glob/regexp expression
 *   </ul>
 * </li>
 * <li> ternary: expr1 ? expr2 : expr3
 * <li> elvis: expr1 ?: expr2 (calculated as ternary expr1 != null ? expr1 : expr2)
 * <li> array: [expr1, expr2, ...]
 * <li> map: {expr1:expr2, expr3:expr4, ...}
 * <li> variable reference: $identifier
 * <li> assignment: $identifier = expr
 * <li> self: $self, current expression context reference
 * <li> root: $root or root property name (without $root.), root expression context reference
 * <li> context: $ or $context, runtime expression context reference
 * <li> cast: {@literal @expr1@(expr2)}, where expr1: {@literal <qualified identifier>}
 * <li> is: {@literal expr1 is 'expr2'}, where expr2: {@literal <qualified identifier>}
 * <li> property: expr1.expr2 / expr1[expr2], where expr2: {@literal <identifier>}. Can access public field or getter (getXXX/isXXX)
 * <li> static property: {@literal @expr1@.expr2} / {@literal @expr1@[expr2]}, where expr1: {@literal <qualified identifier>}, expr2: {@literal <identifier>}.
 * Can access public static field or getter (getXXX/isXXX) 
 * <li> method: expr1.expr2(expr3, expr4...), where expr2: {@literal <identifier>}. Can access public method
 * <li> static method: {@literal @expr1@.expr2(expr3, expr4...)}, where expr1: {@literal <qualified identifier>}, expr2: {@literal <identifier>}.
 * Can access public static method
 * <li> constructor: {@literal new @expr1@/expr1/'expr1'(expr2, expr3...)}, where expr1: {@literal <qualified identifier>}
 * <li> track: expr1.expr2, result of expr1 becomes self of expr2 
 * <li> nullsafe track: expr1?.expr2 (calculated as ternary expr1 != null ? expr1.expr2 : null)
 * <li> index: expr1[expr2], result of expr1 becomes self of expr2
 * <li> selection: ~[expr] - all, ^[expr] - first, &[expr] - last
 * <li> projection: ![expr]
 * <li> template:`template`, behaves like unescaped string and can contain template expressions delimited by <% or <%# and %>
 * <li> if: if (expr1) {expr2} or if (expr1) {expr2} else {expr3}
 * <li> while: while(expr1) {expr2}
 * <li> for: for($var : iterableExpr) {bodyExpr}
 * <li> return: return or return  expr
 * <li> break, continue: break, continue
 * </ul>
 *
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev_A
 */
public class ExpressionParser
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private final ExpressionTokenizer tokenizer;
    private final ParseContext parseContext;
    private DebugContext debugContext;

    public ExpressionParser(String value)
    {
        this(value, new ParseContext(), null);
    }

    public ExpressionParser(String value, ParseContext parseContext, DebugContext debugContext)
    {
        Assert.notNull(value);
        Assert.notNull(parseContext);
        
        this.tokenizer = new ExpressionTokenizer(value);
        this.parseContext = parseContext;
        this.debugContext = debugContext;
    }
    
    public ParseContext getParseContext()
    {
        return parseContext;
    }
    
    public DebugContext getDebugContext()
    {
        return debugContext;
    }
    
    public IExpressionNode parse()
    {
        IExpressionNode expression = parseStatementsExpression();
        
        if (tokenizer.readNextToken() != null)
            throwError(messages.extraTokenAfterEnd(tokenizer.getValueText()));
        
        return expression;
    }
    
    private IExpressionNode parseBinaryExpressionGroup()
    {
        List<BinaryOperation> operations = new ArrayList<BinaryOperation>();
        BinaryOperationType lastType = null;
        int lastStartPos = -1, lastEndPos = -1;
        while (true)
        {
            IExpressionNode expression = parseUnaryExpression();
            operations.add(new BinaryOperation(lastType, expression, lastStartPos, lastEndPos));
            
            Token token = tokenizer.peekNextToken();
            lastStartPos = tokenizer.getStartPos();
            lastType = getBinaryOperationType(token);
            lastEndPos = tokenizer.getEndPos();
            if (lastType == null)
                break;
            
            tokenizer.readNextToken();
        }
        
        Assert.isTrue(!operations.isEmpty());
        
        while (operations.size() > 1)
        {
            BinaryOperation found = null;
            int index = 0;
            for (int i = 0; i < operations.size(); i++)
            {
                BinaryOperation operation = operations.get(i);
                if (i >= 1 && (found == null || found.type.precedence < operation.type.precedence))
                {
                    found = operation;
                    index = i;
                }
            }
            
            Assert.notNull(found);
            
            IExpressionNode firstExpression = operations.get(index - 1).expression;
            IExpressionNode secondExpression = operations.get(index).expression;
            int startPos = operations.get(index).startPos;
            int endPos = operations.get(index).endPos;
            IExpressionNode combinedExpression;
            switch (operations.get(index).type)
            {
            case MULTIPLICATION:
                combinedExpression = intercept(new BinaryExpressionNode(firstExpression, secondExpression, 
                    BinaryExpressionNode.Operation.MULTIPLICATION), startPos, endPos);
                break;
            case DIVISION:
                combinedExpression = intercept(new BinaryExpressionNode(firstExpression, secondExpression,
                    BinaryExpressionNode.Operation.DIVISION), startPos, endPos);
                break;
            case REMAINDER:
                combinedExpression = intercept(new BinaryExpressionNode(firstExpression, secondExpression, 
                    BinaryExpressionNode.Operation.REMAINDER), startPos, endPos);
                break;
            case ADDITION:
                combinedExpression = intercept(new BinaryExpressionNode(firstExpression, secondExpression, 
                    BinaryExpressionNode.Operation.ADDITION), startPos, endPos);
                break;
            case SUBTRACTION:
                combinedExpression = intercept(new BinaryExpressionNode(firstExpression, secondExpression, 
                    BinaryExpressionNode.Operation.SUBTRACTION), startPos, endPos);
                break;
            case SHL:
                combinedExpression = intercept(new BinaryExpressionNode(firstExpression, secondExpression, 
                    BinaryExpressionNode.Operation.SHL), startPos, endPos);
                break;
            case SHR:
                combinedExpression = intercept(new BinaryExpressionNode(firstExpression, secondExpression,
                    BinaryExpressionNode.Operation.SHR), startPos, endPos);
                break;
            case USHR:
                combinedExpression = intercept(new BinaryExpressionNode(firstExpression, secondExpression, 
                    BinaryExpressionNode.Operation.USHR), startPos, endPos);
                break;
            case LT:
                combinedExpression = intercept(new BinaryExpressionNode(firstExpression, secondExpression, 
                    BinaryExpressionNode.Operation.LT), startPos, endPos);
                break;
            case LTE:
                combinedExpression = intercept(new BinaryExpressionNode(firstExpression, secondExpression, 
                    BinaryExpressionNode.Operation.LTE), startPos, endPos);
                break;
            case GT:
                combinedExpression = intercept(new BinaryExpressionNode(firstExpression, secondExpression, 
                    BinaryExpressionNode.Operation.GT), startPos, endPos);
                break;
            case GTE:
                combinedExpression = intercept(new BinaryExpressionNode(firstExpression, secondExpression, 
                    BinaryExpressionNode.Operation.GTE), startPos, endPos);
                break;
            case IN:
                combinedExpression = intercept(new BinaryExpressionNode(firstExpression, secondExpression, 
                    BinaryExpressionNode.Operation.IN), startPos, endPos);
                break;
            case NOT_IN:
                combinedExpression = intercept(new BinaryExpressionNode(firstExpression, secondExpression, 
                    BinaryExpressionNode.Operation.NOT_IN), startPos, endPos);
                break;
            case LIKE:
                combinedExpression = intercept(new LikeExpressionNode(firstExpression, secondExpression), startPos, endPos);
                break;
            case IS:
                combinedExpression = intercept(new IsExpressionNode(firstExpression, secondExpression), startPos, endPos);
                break;
            case EQ:
                combinedExpression = intercept(new BinaryExpressionNode(firstExpression, secondExpression, 
                    BinaryExpressionNode.Operation.EQ), startPos, endPos);
                break;
            case NEQ:
                combinedExpression = intercept(new BinaryExpressionNode(firstExpression, secondExpression, 
                    BinaryExpressionNode.Operation.NEQ), startPos, endPos);
                break;
            case BAND:
                combinedExpression = intercept(new BinaryExpressionNode(firstExpression, secondExpression, 
                    BinaryExpressionNode.Operation.BAND), startPos, endPos);
                break;
            case XOR:
                combinedExpression = intercept(new BinaryExpressionNode(firstExpression, secondExpression, 
                    BinaryExpressionNode.Operation.XOR), startPos, endPos);
                break;
            case BOR:
                combinedExpression = intercept(new BinaryExpressionNode(firstExpression, secondExpression, 
                    BinaryExpressionNode.Operation.BOR), startPos, endPos);
                break;
            case AND:
                combinedExpression = intercept(new BinaryExpressionNode(firstExpression, secondExpression, 
                    BinaryExpressionNode.Operation.AND), startPos, endPos);
                break;
            case OR:
                combinedExpression = intercept(new BinaryExpressionNode(firstExpression, secondExpression, 
                    BinaryExpressionNode.Operation.OR), startPos, endPos);
                break;
            default:
                return Assert.error();
            }
            
            BinaryOperation combinedOperation = new BinaryOperation(operations.get(index - 1).type, combinedExpression,
                operations.get(index - 1).startPos, operations.get(index - 1).endPos);
            operations.set(index - 1, combinedOperation);
            operations.remove(index);
        }
        
        return operations.get(0).expression;
    }
    
    private IExpressionNode parseUnaryExpression()
    {
        Token token = tokenizer.peekNextToken();
        int startPos = tokenizer.getStartPos();
        UnaryOperationType operation = getUnaryOperationType(token);
        int endPos = tokenizer.getEndPos();
        if (operation != null)
        {
            tokenizer.readNextToken();
            IExpressionNode expression = parseTrackExpression(null);
            switch (operation)
            {
            case PLUS:
                return intercept(new UnaryExpressionNode(expression, UnaryExpressionNode.Operation.PLUS), startPos, endPos);
            case MINUS:
                return intercept(new UnaryExpressionNode(expression, UnaryExpressionNode.Operation.MINUS), startPos, endPos);
            case NOT:
                return intercept(new UnaryExpressionNode(expression, UnaryExpressionNode.Operation.NOT), startPos, endPos);
            case BNOT:
                return intercept(new UnaryExpressionNode(expression, UnaryExpressionNode.Operation.BNOT), startPos, endPos);
            default:
                return Assert.error();
            }
        }
        else
            return parseTrackExpression(null);
    }
    
    private IExpressionNode parseTrackExpression(IExpressionNode prevExpression)
    {
        if (prevExpression == null)
            prevExpression = parseSingleExpression();
        
        Token token = tokenizer.peekNextToken();
        if (token != null)
        {
            int startPos = tokenizer.getStartPos();
            int endPos = tokenizer.getEndPos();
            switch (token)
            {
            case PERIOD:
                {
                    tokenizer.readNextToken();
                    IExpressionNode nextExpression = parseMemberExpression();
                    return intercept(new TrackExpressionNode(true, prevExpression, nextExpression), startPos, endPos);
                }
            case QUESTION_PERIOD:
                {
                    tokenizer.readNextToken();
                    IExpressionNode nextExpression = parseMemberExpression();
                    return intercept(new TrackExpressionNode(false, prevExpression, new NullSafeExpressionNode(nextExpression)), startPos, endPos);
                }
            case SQUARE_OPEN_BRACKET:
                IExpressionNode nextExpression = parseMemberExpression();
                return intercept(new TrackExpressionNode(false, prevExpression, nextExpression), startPos, endPos);
            }
        }
        
        return prevExpression;
    }

    private IExpressionNode parseMemberExpression()
    {
        Token token = tokenizer.readNextToken();
        if (token == null)
            throwError(messages.unexpectedEnd());
        
        IExpressionNode expression;
        int startPos = tokenizer.getStartPos();
        switch (token)
        {
        case ID:
            String name = (String)tokenizer.getValue();
            
            int endPos = tokenizer.getEndPos();
            expression = parsePropertyOrMethodExpression(true, intercept(new ConstantExpressionNode(name), startPos, endPos), null, false, startPos);
            break;
        case SQUARE_OPEN_BRACKET:
            IExpressionNode nameExpression = parseSquareBracketGroupExpression();
            expression = parsePropertyOrMethodExpression(false, nameExpression, null, false, startPos);
            break;
        case TILDE_SQUARE_OPEN_BRACKET:
            expression = parseSelectionExpression(Operation.ALL);
            break;
        case CIRCUMFLEX_SQUARE_OPEN_BRACKET:
            expression = parseSelectionExpression(Operation.FIRST);
            break;
        case AMPERSAND_SQUARE_OPEN_BRACKET:
            expression = parseSelectionExpression(Operation.LAST);
            break;
        case EXCLAMATION_SQUARE_OPEN_BRACKET:
            expression = parseProjectionExpression();
            break;
        default:
            return throwError(messages.unexpected(tokenizer.getValueText()));
        }
        
        return parseTrackExpression(expression);
    }

    private IExpressionNode parsePropertyOrMethodExpression(boolean simple, IExpressionNode nameExpression, IExpressionNode classNameExpression, 
        boolean isStatic, int startPos)
    {
        int endPos = tokenizer.getEndPos();
        Token token = tokenizer.peekNextToken();
        if (token == Token.ROUND_OPEN_BRACKET)
        {
            List<IExpressionNode> parameterExpressions = parseRoundBracketExpressionList();
            endPos = tokenizer.getEndPos();
            return intercept(new MethodExpressionNode(simple, nameExpression, parameterExpressions, classNameExpression, isStatic), startPos, endPos);
        }
        else if (token == Token.EQUAL)
        {
            tokenizer.readNextToken();
            IExpressionNode expression = parseComplexExpression();
            return intercept(new PropertyAssignmentExpressionNode(simple, nameExpression, classNameExpression, isStatic, 
                expression), startPos, endPos);
        }
        else
            return intercept(new PropertyExpressionNode(simple, nameExpression, classNameExpression, isStatic), startPos, endPos);
    }

    private IExpressionNode parseProjectionExpression()
    {
        int startPos = tokenizer.getStartPos();
        IExpressionNode expression = parseSquareBracketGroupExpression();
        int endPos = tokenizer.getEndPos();
        return intercept(new ProjectionExpressionNode(expression), startPos, endPos);
    }

    private IExpressionNode parseSelectionExpression(Operation operation)
    {
        int startPos = tokenizer.getStartPos();
        IExpressionNode expression = parseSquareBracketGroupExpression();
        int endPos = tokenizer.getEndPos();
        return intercept(new SelectionExpressionNode(expression, operation), startPos, endPos);
    }

    private IExpressionNode parseSingleExpression()
    {
        Token token = tokenizer.peekNextToken();
        if (token != null)
        {
            switch (token)
            {
            case DOLLAR:
                return parseVariableReferenceExpression();
            case CLASS_STRING:
                tokenizer.readNextToken();
                return parseCastExpression();
            case ROUND_OPEN_BRACKET:
                return parseRoundBracketGroupExpression(true);
            case SQUARE_OPEN_BRACKET:
                tokenizer.readNextToken();
                return parseArrayExpression();
            case CURLY_OPEN_BRACKET:
                tokenizer.readNextToken();
                return parseMapExpression();
            case STRING:
                tokenizer.readNextToken();
                return intercept(new ConstantExpressionNode(tokenizer.getValue()), tokenizer.getStartPos(), tokenizer.getEndPos());
            case NUMBER:
                tokenizer.readNextToken();
                return intercept(new ConstantExpressionNode(tokenizer.getValue()), tokenizer.getStartPos(), tokenizer.getEndPos());
            case TEMPLATE_STRING:
                tokenizer.readNextToken();
                return parseTemplateExpression();
            case ID:
                String value = (String)tokenizer.getValue();
                switch (value)
                {
                case "true":
                    tokenizer.readNextToken();
                    return intercept(new ConstantExpressionNode(true), tokenizer.getStartPos(), tokenizer.getEndPos());
                case "false":
                    tokenizer.readNextToken();
                    return intercept(new ConstantExpressionNode(false), tokenizer.getStartPos(), tokenizer.getEndPos());
                case "null":
                    tokenizer.readNextToken();
                    return intercept(new ConstantExpressionNode(null), tokenizer.getStartPos(), tokenizer.getEndPos());
                case "new":
                    tokenizer.readNextToken();
                    return parseConstructorExpression();
                default:
                    int pos = tokenizer.getStartPos();
                    IExpressionNode nextExpression = parseMemberExpression();
                    return intercept(new TrackExpressionNode(true, new RootExpressionNode(), nextExpression), pos, pos);
                }
            default:
                return throwError(messages.unexpected(tokenizer.getValueText()));
            }
        }
        else
            return throwError(messages.unexpectedEnd());
    }

    private IExpressionNode parseStaticTrackExpression(String className, int classStartPos, int classEndPos)
    {
        Token token = tokenizer.readNextToken();
        IExpressionNode nameExpression;
        boolean simple;
        switch (token)
        {
        case PERIOD:
            {
                if (tokenizer.readNextToken() != Token.ID)
                    throwError(messages.expected(messages.identifier(), tokenizer.getValueText()));
                
                String name = (String)tokenizer.getValue();
                nameExpression = intercept(new ConstantExpressionNode(name), tokenizer.getStartPos(), tokenizer.getEndPos());
                simple = true;
                break;
            }
        case SQUARE_OPEN_BRACKET:
            nameExpression = parseSquareBracketGroupExpression();
            simple = false;
            break;
        default:
            return throwError(messages.unexpected(tokenizer.getValueText()));
        }
        
        IExpressionNode expression = parsePropertyOrMethodExpression(simple, nameExpression, intercept(new ConstantExpressionNode(className),
            classStartPos, classEndPos), true, classStartPos);
        return parseTrackExpression(expression);
    }
    
    private IExpressionNode parseConstructorExpression()
    {
        int startPos = tokenizer.getStartPos();
        Token token = tokenizer.readNextToken();
        if (token != Token.CLASS_STRING && token != Token.STRING && token != Token.TEMPLATE_STRING && token != Token.ID)
            throwError(messages.expected(messages.stringOrIdentifier(), tokenizer.getValueText()));
        
        String className = (String)tokenizer.getValue();
        int classStartPos = tokenizer.getStartPos();
        int classEndPos = tokenizer.getEndPos();
        List<IExpressionNode> parameterExpressions = parseRoundBracketExpressionList();
        int endPos = tokenizer.getEndPos();
        return intercept(new ConstructorExpressionNode(parameterExpressions, intercept(new ConstantExpressionNode(className),
            classStartPos, classEndPos)), startPos, endPos);
    }

    private IExpressionNode parseTemplateExpression()
    {
        String template = (String)tokenizer.getValue();
        int startPos = tokenizer.getStartPos();
        int endPos = tokenizer.getEndPos();
        TemplateParser templateParser = new TemplateParser(template, parseContext, debugContext, null);
        return intercept(templateParser.parse(), startPos, endPos);
    }

    private IExpressionNode parseCastExpression()
    {
        String className = (String)tokenizer.getValue();
        int classStartPos = tokenizer.getStartPos();
        int classEndPos = tokenizer.getEndPos();
        Token token = tokenizer.peekNextToken();
        if (token == null)
            return throwError(messages.unexpectedEnd());
        else if (token == Token.ROUND_OPEN_BRACKET)
        {
            IExpressionNode instanceExpression = parseRoundBracketGroupExpression(false);
            int endPos = tokenizer.getEndPos();
            return intercept(new CastExpressionNode(instanceExpression, intercept(new ConstantExpressionNode(className), 
                classStartPos, classEndPos)), classStartPos, endPos);
        }
        else
            return parseStaticTrackExpression(className, classStartPos, classEndPos);
    }

    private IExpressionNode parseVariableReferenceExpression()
    {
        if (tokenizer.readNextToken() != Token.DOLLAR)
            throwError(messages.expected("$", tokenizer.getValueText()));
        if (tokenizer.peekNextToken() != Token.ID)
            return new ContextExpressionNode();
        
        tokenizer.readNextToken();
        String variableName = (String)tokenizer.getValue();
        switch (variableName)
        {
        case "self":
            return new SelfExpressionNode();
        case "root":
            return new RootExpressionNode();
        case "context":
            return new ContextExpressionNode();
        default:
            return new VariableExpressionNode(variableName, parseContext.allocateVariable(variableName));
        }
    }

    private IExpressionNode parseRoundBracketGroupExpression(boolean createGroup)
    {
        if (tokenizer.readNextToken() != Token.ROUND_OPEN_BRACKET)
            throwError(messages.expected("(", tokenizer.getValueText()));
        
        IExpressionNode result = parseComplexExpression();
        
        Token token = tokenizer.readNextToken();
        
        if (token != Token.ROUND_CLOSE_BRACKET)
            return throwError(messages.expected(")", tokenizer.getValueText()));
        
        if (createGroup)
            return new GroupExpressionNode(result);
        else
            return result;
    }
    
    private IExpressionNode parseSquareBracketGroupExpression()
    {
        IExpressionNode result = parseComplexExpression();
        
        Token token = tokenizer.readNextToken();
        
        if (token != Token.SQUARE_CLOSE_BRACKET)
            return throwError(messages.expected("]", tokenizer.getValueText()));
        
        return result;
    }

    private IExpressionNode parseCurlyBracketGroupExpression()
    {
        if (tokenizer.readNextToken() != Token.CURLY_OPEN_BRACKET)
            throwError(messages.expected("{", tokenizer.getValueText()));
        
        IExpressionNode result = parseStatementsExpression();
        
        Token token = tokenizer.readNextToken();
        
        if (token != Token.CURLY_CLOSE_BRACKET)
            return throwError(messages.expected("}", tokenizer.getValueText()));
        
        return result;
    }
    
    private IExpressionNode parseStatementsExpression()
    {
        int startPos = tokenizer.getStartPos();
        List<IExpressionNode> statements = new ArrayList<IExpressionNode>();
        while (true)
        {
            IExpressionNode expression = parseStatementExpression();
            if (expression != null)
                statements.add(expression);
            
            if (tokenizer.peekNextToken() != Token.SEMICOLON)
                break;
            
            tokenizer.readNextToken();
        }
        
        if (statements.size() == 1)
            return statements.get(0);

        int endPos = tokenizer.getEndPos();
        return intercept(new StatementExpressionNode(statements), startPos, endPos);
    }
    
    private IExpressionNode parseStatementExpression()
    {
        Token token = tokenizer.peekNextToken();
        if (token == null)
            return null;
        
        switch (token)
        {
        case SEMICOLON:
        case CURLY_CLOSE_BRACKET:
            return null;
        case ID:
            if (tokenizer.getValue().equals("return"))
            {
                tokenizer.readNextToken();
                return parseReturnExpression();
            }
            else if (tokenizer.getValue().equals("if"))
            {
                tokenizer.readNextToken();
                return parseIfExpression();
            }
            else if (tokenizer.getValue().equals("while"))
            {
                tokenizer.readNextToken();
                return parseWhileExpression();
            }
            else if (tokenizer.getValue().equals("for"))
            {
                tokenizer.readNextToken();
                return parseForExpression();
            }
            else if (tokenizer.getValue().equals("break"))
            {
                tokenizer.readNextToken();
                return intercept(new BreakExpressionNode(), tokenizer.getStartPos(), tokenizer.getEndPos());
            }
            else if (tokenizer.getValue().equals("continue"))
            {
                tokenizer.readNextToken();
                return intercept(new ContinueExpressionNode(), tokenizer.getStartPos(), tokenizer.getEndPos());
            }
        }
        
        return parseComplexExpression();
    }
    
    private IExpressionNode parseReturnExpression()
    {
        int startPos = tokenizer.getStartPos();
        IExpressionNode expression = null;
        if (tokenizer.peekNextToken() != null && tokenizer.peekNextToken() != Token.SEMICOLON && tokenizer.peekNextToken() != Token.CURLY_CLOSE_BRACKET)
            expression = parseComplexExpression();
        int endPos = tokenizer.getEndPos();
        
        return intercept(new ReturnExpressionNode(expression), startPos, endPos);
    }

    private IExpressionNode parseIfExpression()
    {
        int startPos = tokenizer.getStartPos();
        IExpressionNode conditionExpression = parseRoundBracketGroupExpression(false);
        IExpressionNode firstExpression = parseCurlyBracketGroupExpression();

        IExpressionNode secondExpression = null;
        if (tokenizer.peekNextToken() == Token.ID && tokenizer.getValue().equals("else"))
        {
            tokenizer.readNextToken();
            secondExpression = parseCurlyBracketGroupExpression();
        }
        
        int endPos = tokenizer.getEndPos();
        
        return intercept(new IfExpressionNode(conditionExpression, firstExpression, secondExpression), startPos, endPos);
    }

    private IExpressionNode parseWhileExpression()
    {
        int startPos = tokenizer.getStartPos();
        IExpressionNode conditionExpression = parseRoundBracketGroupExpression(false);
        IExpressionNode expression = parseCurlyBracketGroupExpression();
        int endPos = tokenizer.getEndPos();

        return intercept(new WhileExpressionNode(conditionExpression, expression), startPos, endPos);
    }

    private IExpressionNode parseForExpression()
    {
        int startPos = tokenizer.getStartPos();
        if (tokenizer.readNextToken() != Token.ROUND_OPEN_BRACKET)
            throwError(messages.expected("(", tokenizer.getValueText()));
        
        IExpressionNode variableExpression = parseVariableReferenceExpression();
        if (!(variableExpression instanceof VariableExpressionNode))
            throwError(messages.expected(messages.variable(), tokenizer.getValueText()));
        
        if (tokenizer.readNextToken() != Token.COLON)
            throwError(messages.expected(":", tokenizer.getValueText()));
        
        IExpressionNode iterableExpression = parseComplexExpression();
        
        Token token = tokenizer.readNextToken();
        if (token != Token.ROUND_CLOSE_BRACKET)
            return throwError(messages.expected(")", tokenizer.getValueText()));
        
        IExpressionNode expression = parseCurlyBracketGroupExpression();
        int endPos = tokenizer.getEndPos();

        return intercept(new ForExpressionNode((VariableExpressionNode)variableExpression, iterableExpression, expression), startPos, endPos);
    }

    private List<IExpressionNode> parseRoundBracketExpressionList()
    {
        if (tokenizer.readNextToken() != Token.ROUND_OPEN_BRACKET)
            throwError(messages.expected("(", tokenizer.getValueText()));
        
        List<IExpressionNode> expressions;
        if (tokenizer.peekNextToken() != Token.ROUND_CLOSE_BRACKET)
            expressions = parseExpressionList();
        else
            expressions = Collections.emptyList();
        
        if (tokenizer.readNextToken() != Token.ROUND_CLOSE_BRACKET)
            throwError(messages.expected(")", tokenizer.getValueText()));
        
        return expressions;
    }
    
    private List<IExpressionNode> parseExpressionList()
    {
        List<IExpressionNode> expressions = new ArrayList<IExpressionNode>();
        while (true)
        {
            expressions.add(parseComplexExpression());
            if (tokenizer.peekNextToken() != Token.COMMA)
                break;
            tokenizer.readNextToken();
        }
        
        return expressions;
    }
    
    private List<Pair<IExpressionNode, IExpressionNode>> parseMapExpressionGroup()
    {
        List<Pair<IExpressionNode, IExpressionNode>> expressions = new ArrayList<Pair<IExpressionNode, IExpressionNode>>();
        while (true)
        {
            IExpressionNode keyExpression = parseComplexExpression();
            if (tokenizer.readNextToken() != Token.COLON)
                throwError(messages.expected(":", tokenizer.getValueText()));
            IExpressionNode valueExpression = parseComplexExpression();
            expressions.add(new Pair(keyExpression, valueExpression));
            
            if (tokenizer.peekNextToken() != Token.COMMA)
                break;
            tokenizer.readNextToken();
        }
        
        return expressions;
    }
    
    private IExpressionNode parseComplexExpression()
    {
        IExpressionNode first = parseBinaryExpressionGroup();
        Token token = tokenizer.peekNextToken();
        if (token != null)
        {
            switch (token)
            {
            case QUESTION:
                tokenizer.readNextToken();
                return parseTernaryExpression(first);
            case QUESTION_COLON:
                tokenizer.readNextToken();
                return parseElvisExpression(first);
            case EQUAL:
                tokenizer.readNextToken();
                return parseVariableAssignmentExpression(first);
            }
        }
        
        return first;
    }

    private IExpressionNode parseTernaryExpression(IExpressionNode conditionExpression)
    {
        int startPos = tokenizer.getStartPos();
        int endPos = tokenizer.getEndPos();
        IExpressionNode firstExpression = parseBinaryExpressionGroup();
        if (tokenizer.readNextToken() != Token.COLON)
            return throwError(messages.expected(":", tokenizer.getValueText()));
        
        IExpressionNode secondExpression = parseBinaryExpressionGroup();
        return intercept(new TernaryExpressionNode(conditionExpression, firstExpression, secondExpression), startPos, endPos);
    }
    
    private IExpressionNode parseElvisExpression(IExpressionNode expression)
    {
        int startPos = tokenizer.getStartPos();
        int endPos = tokenizer.getEndPos();
        IExpressionNode defaultExpression = parseBinaryExpressionGroup();
        return intercept(new ElvisExpressionNode(expression, defaultExpression), startPos, endPos);
    }

    private IExpressionNode parseVariableAssignmentExpression(IExpressionNode variableExpression)
    {
        int startPos = tokenizer.getStartPos();
        int endPos = tokenizer.getEndPos();
        
        if (!(variableExpression instanceof VariableExpressionNode))
            return throwError(messages.expected(messages.variable(), tokenizer.getValueText()));
        
        IExpressionNode expression = parseComplexExpression();
        
        return intercept(new VariableAssignmentExpressionNode((VariableExpressionNode)variableExpression, expression), startPos, endPos);
    }

    private IExpressionNode parseArrayExpression()
    {
        int startPos = tokenizer.getStartPos();
        List<IExpressionNode> expressions;
        if (tokenizer.peekNextToken() != Token.SQUARE_CLOSE_BRACKET)
            expressions = parseExpressionList();
        else
            expressions = Collections.emptyList();
        
        if (tokenizer.readNextToken() != Token.SQUARE_CLOSE_BRACKET)
            throwError(messages.expected("]", tokenizer.getValueText()));
        int endPos = tokenizer.getEndPos();
        
        return intercept(new ArrayExpressionNode(expressions), startPos, endPos);
    }
    
    private IExpressionNode parseMapExpression()
    {
        int startPos = tokenizer.getStartPos();
        List<Pair<IExpressionNode, IExpressionNode>> expressions;
        if (tokenizer.peekNextToken() != Token.CURLY_CLOSE_BRACKET)
            expressions = parseMapExpressionGroup();
        else
            expressions = Collections.emptyList();
        
        if (tokenizer.readNextToken() != Token.CURLY_CLOSE_BRACKET)
            throwError(messages.expected("}", tokenizer.getValueText()));

        int endPos = tokenizer.getEndPos();
        
        return intercept(new MapExpressionNode(expressions), startPos, endPos);
    }
    
    private UnaryOperationType getUnaryOperationType(Token token)
    {
        if (token == null)
            return null;
        
        switch (token)
        {
        case PLUS:
            return UnaryOperationType.PLUS;
        case MINUS:
            return UnaryOperationType.MINUS;
        case EXCLAMATION:
            return UnaryOperationType.NOT;
        case TILDE:
            return UnaryOperationType.BNOT;
        case ID:
            String value = (String)tokenizer.getValue();
            switch (value)
            {
            case "not":
                return UnaryOperationType.NOT;
            case "bnot":
                return UnaryOperationType.BNOT;
            }
        default:
        }
        
        return null;
    }
    
    private BinaryOperationType getBinaryOperationType(Token token)
    {
        if (token == null)
            return null;
        
        switch (token)
        {
        case ASTERISK:
            return BinaryOperationType.MULTIPLICATION;
        case SLASH:
            return BinaryOperationType.DIVISION;
        case PERCENT:
            return BinaryOperationType.REMAINDER;
        case PLUS:
            return BinaryOperationType.ADDITION;
        case MINUS:
            return BinaryOperationType.SUBTRACTION;
        case DOUBLE_LESS:
            return BinaryOperationType.SHL;
        case DOUBLE_GREATER:
            return BinaryOperationType.SHR;
        case TRIPLE_GREATER:
            return BinaryOperationType.USHR;
        case LESS:
            return BinaryOperationType.LT;
        case LESS_EQUAL:
            return BinaryOperationType.LTE;
        case LESS_GREATER:
            return BinaryOperationType.NEQ;
        case GREATER:
            return BinaryOperationType.GT;
        case GREATER_EQUAL:
            return BinaryOperationType.GTE;
        case HASH:
            return BinaryOperationType.LIKE;
        case DOUBLE_EQUAL:
            return BinaryOperationType.EQ;
        case EXCLAMATION_EQUAL:
            return BinaryOperationType.NEQ;
        case AMPERSAND:
            return BinaryOperationType.BAND;
        case CIRCUMFLEX:
            return BinaryOperationType.XOR;
        case VERTICAL_BAR:
            return BinaryOperationType.BOR;
        case DOUBLE_AMPERSAND:
            return BinaryOperationType.AND;
        case DOUBLE_VERTICAL_BAR:
            return BinaryOperationType.OR;
        case ID:
            String value = (String)tokenizer.getValue();
            switch (value)
            {
            case "mul":
                return BinaryOperationType.MULTIPLICATION;
            case "div":
                return BinaryOperationType.DIVISION;
            case "rem":
                return BinaryOperationType.REMAINDER;
            case "add":
                return BinaryOperationType.ADDITION;
            case "sub":
                return BinaryOperationType.SUBTRACTION;
            case "shl":
                return BinaryOperationType.SHL;
            case "shr":
                return BinaryOperationType.SHR;
            case "ushr":
                return BinaryOperationType.USHR;
            case "lt":
                return BinaryOperationType.LT;
            case "lte":
                return BinaryOperationType.LTE;
            case "gt":
                return BinaryOperationType.GT;
            case "gte":
                return BinaryOperationType.GTE;
            case "in":
                return BinaryOperationType.IN;
            case "not":
                tokenizer.readNextToken();
                tokenizer.peekNextToken();
                if (tokenizer.getToken() != Token.ID || !tokenizer.getValue().equals("in"))
                    throwError(messages.expected("in", tokenizer.getValueText()));
                return BinaryOperationType.NOT_IN;
            case "like":
                return BinaryOperationType.LIKE;
            case "is":
                return BinaryOperationType.IS;
            case "eq":
                return BinaryOperationType.EQ;
            case "neq":
                return BinaryOperationType.NEQ;
            case "band":
                return BinaryOperationType.BAND;
            case "xor":
                return BinaryOperationType.XOR;
            case "bor":
                return BinaryOperationType.BOR;
            case "and":
                return BinaryOperationType.AND;
            case "or":
                return BinaryOperationType.OR;
            default:
                throwError(messages.unknownBinaryOperator(value));
                return null;
            }
        default:
            return null;
        }
    }
    
    private IExpressionNode intercept(IExpressionNode expression, int startPos, int endPos)
    {
        if (debugContext == null)
            return expression;
        else
            return new DebugExpressionNode(debugContext, startPos, endPos, expression, tokenizer.getText());
    }
    
    private IExpressionNode throwError(ILocalizedMessage message)
    {
        tokenizer.throwError(message);
        return null;
    }

    private enum UnaryOperationType
    {
        PLUS,
        MINUS,
        NOT,
        BNOT
    }
    
    private enum BinaryOperationType
    {
        MULTIPLICATION(10), DIVISION(10), REMAINDER(10),
        ADDITION(9), SUBTRACTION(9),
        SHL(8), SHR(8), USHR(8),
        LT(7), LTE(7), GT(7), GTE(7), IN(7), NOT_IN(7), LIKE(7), IS(7),
        EQ(6), NEQ(6),
        BAND(5),
        XOR(4),
        BOR(3),
        AND(2),
        OR(1)
        ;
        
        private final int precedence;
        
        private BinaryOperationType(int precedence)
        {
            this.precedence = precedence;
        }
    }

    private static class BinaryOperation
    {
        public final BinaryOperationType type;
        public final IExpressionNode expression;
        public final int startPos;
        public final int endPos;
        
        public BinaryOperation(BinaryOperationType type, IExpressionNode expression, int startPos, int endPos)
        {
            this.type = type;
            this.expression = expression;
            this.startPos = startPos;
            this.endPos = endPos;
        }
    }

    private interface IMessages
    {
        @DefaultMessage("Syntax error. Unexpected token: ''{0}''.")
        ILocalizedMessage unexpected(String value);

        @DefaultMessage("Syntax error. Unexpected end of text.")
        ILocalizedMessage unexpectedEnd();

        @DefaultMessage("Syntax error. Extra token after end of text: ''{0}''.")
        ILocalizedMessage extraTokenAfterEnd(String value);

        @DefaultMessage("Syntax error. Expected: ''{0}'', but got: ''{1}''.")
        ILocalizedMessage expected(Object expectedValue, String value);
        
        @DefaultMessage("Syntax error. Unknown binary operator: ''{0}''.")
        ILocalizedMessage unknownBinaryOperator(String value);
        
        @DefaultMessage("<identifier>")
        ILocalizedMessage identifier();
        
        @DefaultMessage("<string> or <identifier>")
        ILocalizedMessage stringOrIdentifier();
        
        @DefaultMessage("<variable>")
        ILocalizedMessage variable();
    }  
}
