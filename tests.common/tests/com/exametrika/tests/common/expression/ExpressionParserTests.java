/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.common.expression;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.common.expression.ITemplate;
import com.exametrika.common.expression.ITemplateRegistry;
import com.exametrika.common.expression.Templates;
import com.exametrika.common.expression.impl.DebugContext;
import com.exametrika.common.expression.impl.DebugExpressionException;
import com.exametrika.common.expression.impl.ExpressionContext;
import com.exametrika.common.expression.impl.ExpressionParser;
import com.exametrika.common.expression.impl.ExpressionTokenizer;
import com.exametrika.common.expression.impl.ExpressionTokenizer.Token;
import com.exametrika.common.expression.impl.IExpressionNode;
import com.exametrika.common.expression.impl.ParseContext;
import com.exametrika.common.expression.impl.StandardClassResolver;
import com.exametrika.common.expression.impl.StandardCollectionProvider;
import com.exametrika.common.expression.impl.StandardConversionProvider;
import com.exametrika.common.expression.impl.TemplateRegistry;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.utils.ICondition;
import com.exametrika.common.utils.MapBuilder;


/**
 * The {@link ExpressionParserTests} are tests for {@link ExpressionTokenizer} and {@link ExpressionParser}.
 * 
 * @see ExpressionTokenizer
 * @see ExpressionParser
 * @author Medvedev-A
 */
public class ExpressionParserTests
{
    @Test
    public void testTokenizer() throws Exception
    {
        Object[] array = new Object[]{Token.ID, "_abc123def", Token.STRING, "double quotes", Token.STRING, "single quotes",
            Token.STRING, "non \t\nescaped", Token.CLASS_STRING, "java.lang.String", 
            Token.TEMPLATE_STRING, "template \n<%template%>\nstring", Token.NUMBER, 123l, Token.PLUS, null,  
            Token.NUMBER, 123.456d, Token.PLUS, null, Token.NUMBER, 123.456e-78d, Token.PLUS, null, 
            Token.NUMBER, 123E78d, Token.PLUS, null, Token.NUMBER, 0xAbCdeF1234l,
            Token.PLUS, null, Token.ID, "a", Token.COMMA, null, Token.ID, "a", 
            Token.PERIOD, null, Token.ID, "a", Token.SEMICOLON, null, Token.ID, "a", 
            Token.COLON, null, Token.ID, "a", Token.ROUND_OPEN_BRACKET, null, Token.ID, "a", 
            Token.ROUND_CLOSE_BRACKET, null, Token.ID, "a", Token.SQUARE_OPEN_BRACKET, null, Token.ID, "a", 
            Token.SQUARE_CLOSE_BRACKET, null, Token.ID, "a", Token.CURLY_OPEN_BRACKET, null, Token.ID, "a", 
            Token.CURLY_CLOSE_BRACKET, null, Token.ID, "a", Token.PLUS, null, Token.ID, "a", 
            Token.MINUS, null, Token.ID, "a", Token.ASTERISK, null, Token.ID, "a", 
            Token.SLASH, null, Token.ID, "a", Token.PERCENT, null, Token.ID, "a", 
            Token.EXCLAMATION, null, Token.ID, "a", Token.EXCLAMATION_SQUARE_OPEN_BRACKET, null, Token.ID, "a", 
            Token.EXCLAMATION_EQUAL, null, Token.ID, "a", Token.TILDE, null, Token.ID, "a", 
            Token.VERTICAL_BAR, null, Token.ID, "a", Token.DOUBLE_VERTICAL_BAR, null, Token.ID, "a", 
            Token.AMPERSAND, null, Token.ID, "a", Token.DOUBLE_AMPERSAND, null, Token.ID, "a", 
            Token.EQUAL, null, Token.ID, "a", Token.DOUBLE_EQUAL, null, Token.ID, "a", 
            Token.LESS, null, Token.ID, "a", Token.LESS_EQUAL, null, Token.ID, "a", Token.LESS_GREATER, null, Token.ID, "a",
            Token.DOUBLE_LESS, null, Token.ID, "a", Token.GREATER, null, Token.ID, "a", 
            Token.DOUBLE_GREATER, null, Token.ID, "a", Token.GREATER_EQUAL, null, Token.ID, "a", 
            Token.TRIPLE_GREATER, null, Token.ID, "a", Token.CIRCUMFLEX, null, Token.ID, "a", 
            Token.CIRCUMFLEX_SQUARE_OPEN_BRACKET, null, Token.ID, "a", Token.HASH, null, Token.ID, "a", 
            Token.QUESTION, null, Token.ID, "a", Token.TILDE_SQUARE_OPEN_BRACKET, null, Token.ID, "a", 
            Token.QUESTION_PERIOD, null, Token.ID, "a", Token.QUESTION_COLON, null, Token.ID, "a", 
            Token.DOLLAR, null, Token.ID, "a", Token.AMPERSAND_SQUARE_OPEN_BRACKET, null,
            Token.ID, "true", Token.ID, "or", Token.ID, "false"};
        ExpressionTokenizer tokenizer = new ExpressionTokenizer("_abc123def /*comment*/ // single line\n"
            + "\"double quotes\" 'single quotes' \\non \t\nescaped\\ @java.lang.String@ `template \n<%template%>\nstring` 123 + 123.456 +"
            + "123.456e-78 + 123E78 + 0xAbCdeF1234 + a , a . a ; a : a ( a ) a [ a ] a { a } a + a - a * a / a % a ! a ![ a !="
            + "a ~ a | a || a & a && a = a == a < a <= a <> a << a > a >> a >= a >>> a ^ a ^[ a # a ? a ~[ a ?. a ?: a $ a &[ true or false");
        
        int i = 0;
        while (true)
        {
            Token token1 = tokenizer.peekNextToken();
            Token token2 = tokenizer.readNextToken();
            assertThat(token1, is(token2));
            if (token1 == null)
                break;
            
            assertThat(tokenizer.getToken(), is(array[i++]));
            assertThat(tokenizer.getValue(), is(array[i++]));
        }
    }
    
    @Test
    public void testExpressions() throws Exception
    {
        Object[] constantExpressions = {"123", 123l, "123.456", 123.456d, "null", null, "true", true, "false", false,
            "'test'", "test"};
        check(constantExpressions, null, null);
        
        Object[] variableExpressions = {"$root", "root", "$self", "self", "$var1=10;$var2=11;$var1", 10l,
            "$var1 = 10 + 20; $var2 = $self + (200 - 100);$var3 = $var1 + $var2", "30self100", "$var1=10;$var2=20;$var3=30;$var1 * ($var2 + $var3)", 500l,
            "$.key", "value", "$['key']", "value"};
        check(variableExpressions, "root", "self");
        
        Object[] unaryExpressions = {"+100", 100l, "-100.1", -100.1d, "!true", false, "~10", ~10l, "$var=+10;-$var", -10l,
            "10 * -(20 + 30)", -500l};
        check(unaryExpressions, "root", "self");
        
        Object[] binaryExpressions = {"+10 * 10 + -20 % 11 - 30 / +3", 81l, "$var1=10;$var2=10 + 20 / 10;$var1 + -$var1 * $var2 / 12", 0l, 
            "10 + 20 < 20 + 20 && 10 * - 20 <= 200 / -1", true, "false or false", false, "true || false", true,
            "true and false", false, "true && true", true, "$var=10;$var == 10", true, "$var=-10;$var != -10", false,
            "10 < 20", true, "10 <= 10", true, "10 > 5", true, "10 >= 10", true, "10 == 10", true, "10 <> 10", false,
            "10 in [10, 20, 30]", true, "10 not in [10, 20, 30]", false, "$var = [10, 20, 30];10 in $var", true,
            "$var = [10, 20, 30];10 not in $var", false, "'test' like '*es*'", true, "'test' # '?es*'", true,
            "$var='test';$var like '#.*es.*'", true, "$var='aa';$var is 'String'", true, "10 is 'long'", true,
            "((10 + 20) * (10 - 20)) / (10 - 5)", -60l};
        check(binaryExpressions, "root", "self");
        
        Object[] complexExpressions = {"$var=10;$var > 5 ? '>5' : '<5'", ">5", "$var ?: 'default'", "default",
            "$var=10;$var1 = $var > 5 ? '>5' : '<5'", ">5", "10 + 20 >= 30 ? '>' : '<'", ">", "10 + 20 >= 30 ? 10 + 20 : 10 - 20", 30l,
            "$var1=new Test2(new Test1('123'));$var1.value.test() != '123' ? $var1.value.value: $var1.value.test('a', 'b')", "123ab"};
        check(complexExpressions, "root", "self");
        
        Object[] arrayExpressions = {"[]", Collections.emptyList(), "$var=10;[10, 20, -30, $var, -20, 20 + 30, (40 + 50) / 10, $var > 5 ? $var : 5]",
            Arrays.asList(10l, 20l, -30l, 10l, -20l, 50l, 9l, 10l), "10 > 5 ? [10, 20] : [20, 30]", Arrays.asList(10l, 20l),
            "$var1=new Test2(new Test1('123'));[new Test1('a').value, $var1.value.value, $var1.test().value]", Arrays.asList("a", "123", "123")};
        check(arrayExpressions, "root", "self");
        
        Object[] mapExpressions = {"{}", Collections.emptyMap(), "$var=10;{'a' : 10, 10 + 20 : 20, 'b':-30, 'c':$var, 'd':-20, 'e':20 + 30, 'f':(40 + 50) / 10, "
            + "'g':$var > 5 ? $var : 5}",
            new MapBuilder().put("a", 10l).put(30l, 20l).put("b", -30l).put("c", 10l).put("d", -20l).put("e", 50l).put("f", 9l).put("g", 10l).toMap(), 
            "10 > 5 ? {'a':10, 'b':20} : {'c':10, 'd':20}", new MapBuilder().put("a", 10l).put("b", 20l).toMap(),
            "$var1=new Test2(new Test1('123'));{'a':new Test1('a').value, 'b':$var1.value.value, 'c':$var1.test().value}", 
            new MapBuilder().put("a", "a").put("b", "123").put("c", "123").toMap()};
        check(mapExpressions, "root", "self");
        
        Object[] selectionExpressions = {"$var=[5, 10, 10, 20, 30];$var.~[$self <= 10]", Arrays.asList(5l, 10l, 10l),
            "$var=[5, 10, 10, 20, 30];$var.^[$self <= 10]", 5l, "$var=[5, 10, 10, 20, 30];$var.&[$self <= 10]", 10l,
            "$var={'a':5, 'b':10, 'c':10, 'd':20, 'e':30};$var.~[$self.value <= 10].![$self.value]", Arrays.asList(5l, 10l, 10l)};
        check(selectionExpressions, "root", "self");
        
        Object[] projectionExpressions = {"$var=[10, 20, 30];$var.![$self.toString()]", Arrays.asList("10", "20", "30")};
        check(projectionExpressions, "root", "self");
        
        Object[] trackExpressions = {"$var1=new Test1('a');$var2=new @com.exametrika.tests.common.expression.ExpressionParserTests$Test1@('a', 'b');"
            + "$var3=new Test2($var2);$var3.value.test('a') + $var3.test().value + $var1.value + @Test1@.staticValue + $var1.test('1') +"
            + "$var1.test('2', '3') + @Test1@.staticTest('4') + @Test1@.staticTest('5', '6') + $root.value.value + value.value + "
            + "$root.test().value + test().value", "abaabaStaticValuea1a23456rootrootrootroot", "@TestEnum@.ONE", TestEnum.ONE, 
            "@TestEnum@.values().![$self.toString()]", Arrays.asList("ONE", "TWO", "THREE"), "$var1=new Test1('123');-@Long@.parseLong($var1.value)", -123l,
            "$var1=new Test2(new Test1('123'));$var1['value']['test']('a', 'b')", "123ab", "@Test1@['staticValue'] + @Test1@['staticTest']('a')", "StaticValuea",
            "$var1=new Test2(new Test1('123'));@long@($var1.test().value) - 10", 113l, "$var1=null;$var1?.test()", null,
            "$var1=new Test2(new Test1('123'));$var1?.value?.value", "123", "$var1=new Test2(new Test1(null));$var1?.value?.value", null,
            "$var=new Test1('');$var.staticValue", "StaticValue", "$var=new Test1('');$var.staticValue1", "StaticValue",
            "$var=new Test1('');$var.staticTest('static')", "static"};
        check(trackExpressions, new Test2(new Test1("root")), null);
        
        Object[] propertyAssignmentExpressions = {"value.value = 1 + 2", "3", "$var={};$var['abc'] = 123;$var['abc']", 123l,
            "$var=[];$var[5]=123;$var[5]", 123l, "$var=new Test1('');$var.staticValue='123'", "123",
            "$var=new Test1('');$var.staticValue1='346'", "346"};
        Test2 root = new Test2(new Test1("root"));
        check(propertyAssignmentExpressions, root, null);
        assertThat(root.value.value, is("3"));
        
        Object[] templateExpressions = {"$var1='a';$var2='1';`Template <%$var1%> - <%@Long@.parseLong($var2) - 123%> - test`", "Template a - -122 - test",
            "$var1='a';$var2='b';$var3=true;`<%# if ($var3){ %>Test true - <%$var1%><%# } else { %>Test false - <%$var2%><%# } %>`",
            "Test true - a", "`<%# for($var : [1,2,3]) { %><%$var%> <%# } %>`", "1 2 3 "};
        check(templateExpressions, "root", "self");
        
        Object[] statementExpressions = {"", null, ";;", null, "if (true) {return }", null, "if (false) {return 1}else{return 2}", 2l, 
            "$var=0;while($var < 5){$var = $var + 1};return $var", 5l, "$var=0;while(true){if ($var >= 5) {return 'ret'};$var = $var + 1};return $var", 
            "ret", "$var=0;while(true){if ($var >= 5) {break};$var = $var + 1};return $var", 5l,
            "$var=0;while(true){$var = $var + 1;if ($var < 5) {continue}else{break};return ;};return $var", 5l, 
            "$var=[1, 2, 3, 4];$res=[];for ($i : $var) {$res.add($i)};return $res", Arrays.asList(1l, 2l, 3l, 4l),
            "$var=[1, 2, 3, 4];$res=[];for ($i : $var) {if ($i == 2){return $res};$res.add($i)};return", Arrays.asList(1l),
            "$var=[1, 2, 3, 4];$res=[];for ($i : $var) {if ($i == 2){break};$res.add($i)};return $res", Arrays.asList(1l),
            "$var=[1, 2, 3, 4];$res=[];for ($i : $var) {if ($i > 2){continue};$res.add($i)};return $res", Arrays.asList(1l, 2l),};
        check(statementExpressions, "root", "self");
    }

    @Test
    public void testTemplates() throws Throwable
    {
        TemplateRegistry registry = new TemplateRegistry();
        registry.addTemplate("for", "<%# for($var : [1,2,3]) { %><%$var%> <%# } %>");
        registry.addTemplate("if", "<%# if ($root){ %>Test true - <%$context['a']%><%# } else { %>Test false - <%$context.b%><%# } %>");
        registry.addTemplate("simple", "Template<%# $var = '10' %> - <%@Long@.parseLong($var) - 123%> - test");
        CompileContext context = new CompileContext();
        ITemplate template = Templates.compile("<%@template(for)%> - <%@template(if)%> - <%@template(simple)%>", context, registry);
        String value = template.execute(true, new MapBuilder<String, Object>().put("a", 1).put("b", 2).toMap());
        assertThat(value, is("1 2 3  - Test true - 1 - Template - -113 - test"));
    }
    
    @Test
    public void testDebug() throws Throwable
    {
        final StandardClassResolver classResolver = new StandardClassResolver();
        classResolver.addResolvedClass("Test1", Test1.class);
        classResolver.addResolvedClass("Test2", Test2.class);
        classResolver.addResolvedClass("TestEnum", TestEnum.class);

        ExpressionParser parser = new ExpressionParser("$var = {'a':[value.exception()]}", new ParseContext(), new DebugContext());
        final IExpressionNode expression = parser.parse();
        final Test2 value = new Test2(new Test1("root"));
        new Expected(new ICondition<Throwable>()
        {
            
            @Override
            public boolean evaluate(Throwable value)
            {
                value.printStackTrace();
                return value instanceof DebugExpressionException;
            }
        }, DebugExpressionException.class, new Runnable()
        {
            @Override
            public void run()
            {
                expression.evaluate(new ExpressionContext(value, null, new StandardCollectionProvider(), classResolver, 
                    new StandardConversionProvider(), 1), value);
            }
        });
    }
    
    @Test
    public void testExpressionsFacade() throws Throwable
    {
        CompileContext compileContext = Expressions.createCompileContext(null);
        IExpression expression = Expressions.compile("$root.a + $context.b", compileContext);
        assertThat((Long)expression.execute(Collections.singletonMap("a", 10), Collections.singletonMap("b", 20)), is(30l));
    }
    
    @Test
    public void testTemplatesFacade() throws Throwable
    {
        CompileContext compileContext = Expressions.createCompileContext(null);
        ITemplateRegistry templateRegistry = Templates.createTemplateRegistry(null);
        templateRegistry.addTemplate("testTemplate", "Test template - <%$root.a + $context.b%>.");
        
        ITemplate template = Templates.compile("Template '<%@template(testTemplate)%>' - <%$root.a + $context.b%>", compileContext, templateRegistry);
        assertThat(template.execute(Collections.singletonMap("a", 10), Collections.singletonMap("b", 20)), 
            is("Template 'Test template - 30.' - 30"));
    }
    
    private void check(Object[] expressions, Object value, Object self)
    {
        StandardClassResolver classResolver = new StandardClassResolver();
        classResolver.addResolvedClass("Test1", Test1.class);
        classResolver.addResolvedClass("Test2", Test2.class);
        classResolver.addResolvedClass("TestEnum", TestEnum.class);
        for (int i = 0; i < expressions.length / 2; i++)
        {
            ExpressionParser parser = new ExpressionParser((String)expressions[i * 2]);
            assertThat((String)expressions[i * 2], parser.parse().evaluate(new ExpressionContext(value, 
                new MapBuilder().put("key", "value").toMap(), new StandardCollectionProvider(), classResolver, 
                new StandardConversionProvider(), 1), self), is(expressions[i * 2 + 1]));
        }
    }
    
    public static class Test1
    {
        private String value;
        public static String staticValue = "StaticValue";

        public Test1(String value)
        {
            this.value = value;
        }
        
        public Test1(String value1, String value2)
        {
            this.value = value1 + value2;
        }
        
        public String getValue()
        {
            return value;
        }
        
        public void setValue(String value)
        {
            this.value = value;
        }
        
        public String test()
        {
            return this.value;
        }
        
        public String test(String value)
        {
            return this.value + value;
        }
        
        public String test(String value1, String value2)
        {
            return this.value + value1 + value2;
        }
        
        public static String staticTest(String value)
        {
            return value;
        }
        
        public static String staticTest(String value1, String value2)
        {
            return value1 + value2;
        }
        
        public static String getStaticValue1()
        {
            return staticValue;
        }
        
        public static void setStaticValue1(String value)
        {
            staticValue = value;
        }
        
        public void exception()
        {
            throw new RuntimeException("Test exception.");
        }
    }
    
    public static class Test2
    {
        private Test1 value;

        public Test2(Test1 value)
        {
            this.value = value;
        }
        
        public Test1 getValue()
        {
            return value;
        }
        
        public Test1 test()
        {
            return value;
        }
    }
    
    public enum TestEnum
    {
        ONE,
        TWO,
        THREE
    }
}
