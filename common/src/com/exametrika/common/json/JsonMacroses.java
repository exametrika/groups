/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.json;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.config.property.IPropertyResolver;
import com.exametrika.common.config.property.Properties;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.json.schema.IJsonValidationContext.ArrayPathElement;
import com.exametrika.common.json.schema.IJsonValidationContext.ObjectPathElement;
import com.exametrika.common.json.schema.IJsonValidationContext.PathElement;
import com.exametrika.common.json.schema.JsonDiagnostics;
import com.exametrika.common.json.schema.JsonValidationContext;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.MapBuilder;






/**
 * The {@link JsonMacroses} is a macro utility methods.
 * 
 * @threadsafety Implementations of this class and its methods are thread safe.
 * @author AndreyM
 */
public final class JsonMacroses
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final String MACRO_SUFFIX = "}}";
    private static final String MACRO_PREFIX = "@{{";
    private static final String REFERENCE_DIRECTIVE = "@reference";
    private static final String REMOVE_DIRECTIVE = "@remove";
    private static final String ARGS_DIRECTIVE = "@args";
    private static final String INLINE_DIRECTIVE = "@inline";
    private static final String GENERATE_DIRECTIVE = "@generate";
    
    public static class Argument
    {
        public final String name;
        public final boolean required;
        public final Object defaultValue;
        public final boolean expanded;

        public Argument(String name, boolean required, boolean expanded, Object defaultValue)
        {
            Assert.notNull(name);
            
            this.name = name;
            this.required = required;
            this.expanded = expanded;
            this.defaultValue = JsonUtils.toImmutable(defaultValue);
        }
        
        @Override
        public String toString()
        {
            return name;
        }
    }
    
    public static class MacroDefinition
    {
        public final String name;
        public final Map<String, Argument> arguments;
        public final Object body;

        public MacroDefinition(String name, Map<String, Argument> arguments, Object body)
        {
            Assert.notNull(name);
            Assert.notNull(arguments);
            
            this.name = name;
            this.arguments = Immutables.wrap(arguments);
            this.body = JsonUtils.toImmutable(body);
        }

        @Override
        public String toString()
        {
            return "@" + name + arguments;
        }
    }
    
    public interface IMacro
    {
        Object evaluate(JsonValidationContext context, Map<String, Object> args);
    }

    public static void expandMacroses(JsonObjectBuilder value, String typeName, List<MacroDefinition> macroses)
    {
        Assert.notNull(value);
        Assert.notNull(typeName);
        Assert.notNull(macroses);
        
        JsonDiagnostics diagnostics = new JsonDiagnostics();
        JsonValidationContext context = new JsonValidationContext(diagnostics, value);
        
        diagnostics.beginType(typeName);
        
        ScopeContext scopeContext = new ScopeContext(macroses);
        expandObjectMacroses(context, scopeContext, value);
        
        diagnostics.end();
        
        diagnostics.checkErrors();
    }
    
    private static Object expandObjectMacroses(JsonValidationContext context, ScopeContext scopeContext, JsonObject instance)
    {
        JsonDiagnostics diagnostics = context.getDiagnostics();
        JsonObjectBuilder object = (JsonObjectBuilder)JsonUtils.toBuilder(instance);
        instance = object;
        
        List<MacroDefinition> macroses = JsonMacroses.buildMacroses(diagnostics, object);
        if (macroses != null)
            scopeContext.beginScope(macroses);
        
        JsonObjectBuilder builder = null;
        for (Map.Entry<String, Object> entry : object)
        {
            diagnostics.beginProperty(entry.getKey());
            context.beginObjectElement(null, object, entry.getKey());
            
            Object value = entry.getValue();
            value = expandMacroses(context, scopeContext, value);
            if (value instanceof InlineValue)
            {
                entry.setValue(REMOVE_DIRECTIVE);
                if (builder == null)
                    builder = new JsonObjectBuilder();
                
                if (((InlineValue)value).value instanceof JsonObject)
                    builder.putAll((JsonObject)((InlineValue)value).value);
                else
                    diagnostics.addError(messages.inlineMustBeObject(diagnostics.getPath(), ((InlineValue)value).value));
            }
            else
                entry.setValue(value);
            
            context.endElement();
            diagnostics.end();
        }
        
        if (builder != null)
            object.putAll(builder);
        
        for (Iterator<Map.Entry<String, Object>> it = object.iterator(); it.hasNext();)
        {
            Object value = it.next().getValue();
            if (value instanceof String && value.equals(REMOVE_DIRECTIVE))
                it.remove();
        }
        
        if (macroses != null)
            scopeContext.endScope();
        
        return instance;
    }
    
    private static Object expandArrayMacroses(JsonValidationContext context, ScopeContext scopeContext, JsonArray instance)
    {
        JsonDiagnostics diagnostics = context.getDiagnostics();
        JsonArrayBuilder array = (JsonArrayBuilder)JsonUtils.toBuilder(instance);
        instance = array;

        JsonArrayBuilder builder = null;
        for (int i = 0; i < array.size(); i++)
        {
            diagnostics.beginIndex(i);
            context.beginArrayElement(null, array, i);

            Object element = array.get(i, null);
            element = expandMacroses(context, scopeContext, element);
            if (element instanceof InlineValue)
            {
                array.set(i, REMOVE_DIRECTIVE);
                if (builder == null)
                    builder = new JsonArrayBuilder();
                
                if (((InlineValue)element).value instanceof JsonArray)
                    builder.addAll((JsonArray)((InlineValue)element).value);
                else
                    diagnostics.addError(messages.inlineMustBeArray(diagnostics.getPath(), ((InlineValue)element).value));
            }
            else
                array.set(i, element);
            
            context.endElement();
            diagnostics.end();
        }
        
        if (builder != null)
            array.addAll(builder);
        
        for (Iterator<Object> it = array.iterator(); it.hasNext();)
        {
            Object value = it.next();
            if (value instanceof String && value.equals(REMOVE_DIRECTIVE))
                it.remove();
        }
        
        return instance;
    }

    private static Object expandMacroses(JsonValidationContext context, ScopeContext scopeContext, Object value)
    {
        if (value == null)
            return null;
        
        JsonDiagnostics diagnostics = context.getDiagnostics();
        
        String name;
        Map<String, Object> args = null;
        boolean inline = false;
        IJsonCollection generate = null;
        if (value instanceof String)
        {
            String str = (String)value;
            int posStart = str.indexOf(MACRO_PREFIX);
            if (posStart == -1)
                return value;
            int posEnd = str.indexOf(MACRO_SUFFIX);
            if (posStart != 0 || posEnd != str.length() - MACRO_SUFFIX.length())
                return Properties.expandProperties(null, new MacroPropertyResolver(context, scopeContext), str, false, false, MACRO_PREFIX, MACRO_SUFFIX);
            
            name = str.substring(MACRO_PREFIX.length(), str.length() - MACRO_SUFFIX.length());
        }
        else if (value instanceof JsonObject)
        {
            JsonObject object = (JsonObject)value;
            String reference = object.get(REFERENCE_DIRECTIVE, null);
            if (reference == null)
                return expandObjectMacroses(context, scopeContext, object);
            
            if (!reference.startsWith("@"))
            {
                reference = getReferencePath(reference, context);
                JsonObject referencedObject = ((JsonObject)context.getRoot()).select(reference, null);
                if (referencedObject != null)
                {
                    value = new JsonObjectBuilder((JsonObject)value);
                    ((JsonObjectBuilder)value).remove(REFERENCE_DIRECTIVE);
                    value = JsonUtils.mergeObjects(referencedObject, (JsonObject)value, "", true);
                }
                else
                    diagnostics.addError(messages.referentNotFound(diagnostics.getPath(), reference));
                
                return expandMacroses(context, scopeContext, value);
            }

            name = reference.substring(1, reference.length());
            args = JsonUtils.toMap((JsonObject)object.get(ARGS_DIRECTIVE, null));
            inline = object.get(INLINE_DIRECTIVE, false);
            generate = object.get(GENERATE_DIRECTIVE, null);
        }
        else if (value instanceof JsonArray)
            return expandArrayMacroses(context, scopeContext, (JsonArray)value);
        else
            return value;
        
        MacroDefinition macro = scopeContext.findMacro(name);
        if (macro == null)
        {
            diagnostics.addError(messages.macroNotFound(diagnostics.getPath(), name));
            return value;
        }
        
        if (args == null)
            args = new HashMap<String, Object>();
        
        if (value instanceof JsonObject)
        {
            value = new JsonObjectBuilder((JsonObject)value);
            ((JsonObjectBuilder)value).remove(REFERENCE_DIRECTIVE);
            ((JsonObjectBuilder)value).remove(ARGS_DIRECTIVE);
            ((JsonObjectBuilder)value).remove(INLINE_DIRECTIVE);
            ((JsonObjectBuilder)value).remove(GENERATE_DIRECTIVE);
        }

        if (generate != null)
        {
            IJsonCollection result;
            if (generate instanceof JsonObject)
            {
                JsonObjectBuilder builder = new JsonObjectBuilder();
                for (Map.Entry<String, Object> entry : (JsonObject)generate)
                {
                    Object child = entry.getValue();
                    if (child instanceof Map)
                    {
                        Map<String, Object> generatedArgs = new LinkedHashMap<String, Object>(args);
                        generatedArgs.putAll((Map<String, Object>)child);
                        builder.put(entry.getKey(), expandMacro(context, scopeContext, diagnostics, macro, name, generatedArgs, value));
                    }
                    else
                        diagnostics.addError(messages.invalidGeneratorArgsElement(diagnostics.getPath(), entry));
                }
                
                result = builder.toJson();
            }
            else if (generate instanceof JsonArray)
            {
                JsonArrayBuilder builder = new JsonArrayBuilder();
                for (Object child : (JsonArray)generate)
                {
                    if (child instanceof Map)
                    {
                        Map<String, Object> generatedArgs = new LinkedHashMap<String, Object>(args);
                        generatedArgs.putAll((Map<String, Object>)child);
                        builder.add(expandMacro(context, scopeContext, diagnostics, macro, name, generatedArgs, value));
                    }
                    else
                        diagnostics.addError(messages.invalidGeneratorArgsElement(diagnostics.getPath(), child));
                }
                
                result = builder.toJson();
            }
            else
            {
                diagnostics.addError(messages.invalidGeneratorArgs(diagnostics.getPath(), generate));
                return REMOVE_DIRECTIVE;
            }
            
            if (!inline)
                return result;
            else
                return new InlineValue(result);
        }
        else
            return expandMacro(context, scopeContext, diagnostics, macro, name, args, value);
    }

    private static Object expandMacro(JsonValidationContext context, ScopeContext scopeContext,
        JsonDiagnostics diagnostics, MacroDefinition macro, String name, Map<String, Object> args, Object value)
    {
        for (Argument argument : macro.arguments.values())
        {
            if (args.containsKey(argument.name))
                continue;
            else if (argument.defaultValue != null)
                args.put(argument.name, argument.defaultValue);
            else if (argument.required)
                diagnostics.addError(messages.requiredArgNotSet(diagnostics.getPath(), name, argument.name));
            else 
                args.put(argument.name, null);
        }
        
        if (!args.isEmpty())
        {
            List<MacroDefinition> list = new ArrayList<MacroDefinition>();
            for (Map.Entry<String, Object> entry : args.entrySet())
            {
                Object body;
                Argument arg = macro.arguments.get(entry.getKey());
                if (arg == null || arg.expanded)
                    body = expandMacroses(context, scopeContext, entry.getValue());
                else
                    body = entry.getValue();
                if (body instanceof InlineValue)
                    body = ((InlineValue)body).value;
                entry.setValue(body);
                list.add(new MacroDefinition(entry.getKey(), java.util.Collections.<String, Argument>emptyMap(), body));
            }
            
            scopeContext.beginScope(list);
        }

        Object body;
        if (macro.body instanceof IMacro)
            body = ((IMacro)macro.body).evaluate(context, args);
        else
            body = macro.body;

        if (body instanceof JsonObject && value instanceof JsonObject)
            value = JsonUtils.mergeObjects((JsonObject)body, (JsonObject)value, "", true);
        else if (body instanceof IJsonCollection)
            value = body;
        else
            value = body;
        
        value = expandMacroses(context, scopeContext, value);
        
        if (!args.isEmpty())
            scopeContext.endScope();
        
        return value;
    }
    
    private static List<MacroDefinition> buildMacroses(JsonDiagnostics diagnostics, JsonObjectBuilder value)
    {
        Assert.notNull(diagnostics);
        Assert.notNull(value);
        
        List<MacroDefinition> macroses = null;
        for (Iterator<Map.Entry<String, Object>> it = value.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry<String, Object> entry = it.next();
            if (entry.getKey().charAt(0) != '@')
                continue;
            
            if (macroses == null)
                macroses = new ArrayList<MacroDefinition>();
                
            MacroDefinition macro = buildMacro(diagnostics, entry.getKey().substring(1), entry.getValue());
            if (macro != null)
            {
                macroses.add(macro);
                it.remove();
            }
        }
        
        return macroses;
    }
    
    private static MacroDefinition buildMacro(JsonDiagnostics diagnostics, String name, Object value)
    {
        if (name.equals("reference") || name.equals("remove") || name.equals("args") || name.equals("generate") || name.equals("inline"))
            return null;
        
        if (value instanceof JsonObject && ((JsonObject)value).contains("args"))
        {
            JsonObject object = (JsonObject)value;
            Map<String, Argument> args = buildArgs(object.get("args"));
            Object body = object.get("body");
            return new MacroDefinition(name, args, body);
        }
        else
            return new MacroDefinition(name, java.util.Collections.<String, Argument>emptyMap(), value);
    }

    private static Map<String, Argument> buildArgs(Object value)
    {
        Map<String, Argument> args = new HashMap<String, Argument>();
        if (value instanceof JsonArray)
        {
            JsonArray array = (JsonArray)value;
            for (Object element : array)
            {
                if (element instanceof String)
                {
                    String name = (String)element;
                    args.put(name, new Argument(name, true, true, null));
                }
                else
                {
                    JsonObject argObject = (JsonObject)element;
                    String name = argObject.get("name");
                    args.put(name, new Argument(name, (Boolean)argObject.get("required", true), (Boolean)argObject.get("expanded", true), 
                        argObject.get("default", null)));
                }
            }
        }
        else
        {
            JsonObject object = (JsonObject)value;
            for (Map.Entry<String, Object> entry : object)
            {
                String name = entry.getKey();
                JsonObject argObject = (JsonObject)entry.getValue();
                args.put(name, new Argument(name, (Boolean)argObject.get("required", true), (Boolean)argObject.get("expanded", true),
                    argObject.get("default", null)));
            }
        }
        return args;
    }

    private static String getReferencePath(String reference, JsonValidationContext context)
    {
        if (reference.startsWith("/"))
            return reference.substring(1);
        
        int pos = 0;
        int parentCount = 0;
        while (true)
        {
            if (reference.indexOf("../", pos) == pos)
            {
                parentCount++;
                pos += 3;
            }
            else
            {
                reference = reference.substring(pos);
                break;
            }
        }
        
        List<PathElement> path = context.getPathList();
        path = path.subList(0, path.size() - parentCount - 1);
        
        StringBuilder builder = new StringBuilder();
        for (PathElement element: path)
        {
            if (element instanceof ObjectPathElement)
            {
                builder.append("[");
                builder.append(((ObjectPathElement)element).key);
                builder.append("]");
            }
            else if (element instanceof ArrayPathElement)
            {
                builder.append("[");
                builder.append(((ArrayPathElement)element).index);
                builder.append("]");
            }
        }
        
        reference = reference.replace('/', '.');
        if (reference.charAt(0) != '[' && reference.charAt(0) != '.')
            builder.append(".");
        builder.append(reference);
        return builder.toString();
    }

    private static void registerDefaultMacroses(List<MacroDefinition> macroses)
    {
        macroses.add(new MacroDefinition("evaluate", java.util.Collections.<String, Argument>singletonMap("expression", 
            new Argument("expression", true, true, null)), new IMacro()
        {
            @Override
            public Object evaluate(JsonValidationContext context, Map<String, Object> args)
            {
                return Expressions.evaluate((String)args.get("expression"), null, args);
            }
        }));
        macroses.add(new MacroDefinition("if", new MapBuilder<String, Argument>()
            .put("condition", new Argument("condition", true, true, null))
            .put("then", new Argument("then", false, false, "@remove"))
            .put("else", new Argument("else", false, false, "@remove"))
            .toMap(), new IMacro()
        {
            @Override
            public Object evaluate(JsonValidationContext context, Map<String, Object> args)
            {
                Object condition = args.get("condition");
                boolean res;
                if (condition instanceof Boolean)
                    res = (Boolean)condition;
                else 
                    res = Expressions.evaluate((String)condition, null, args);
        
                if (res)
                    return args.get("then");
                else
                    return args.get("else");
            }
        }));
    }
    
    private JsonMacroses()
    {
    }

    private static class MacroPropertyResolver implements IPropertyResolver
    {
        private final JsonValidationContext context;
        private final ScopeContext scopeContext;

        public MacroPropertyResolver(JsonValidationContext context, ScopeContext scopeContext)
        {
            this.context = context;
            this.scopeContext = scopeContext;
        }
        
        @Override
        public String resolveProperty(String propertyName)
        {
            JsonDiagnostics diagnostics = context.getDiagnostics();
            MacroDefinition macro = scopeContext.findMacro(propertyName);
            if (macro == null)
            {
                diagnostics.addError(messages.macroNotFound(context.getPath(), propertyName));
                return null;
            }
            if (!macro.arguments.isEmpty())
            {
                diagnostics.addError(messages.macroWithArgsInString(context.getPath(), propertyName));
                return null;
            }
            Object body;
            if (macro.body instanceof IMacro)
                body = ((IMacro)macro.body).evaluate(context, java.util.Collections.<String, Object>emptyMap());
            else
                body = macro.body;
            
            if (body != null)
                return expandMacroses(context, scopeContext, body.toString()).toString();
            else
                return "";
        }
    }

    private static class ScopeContext
    {
        private final Map<String, Deque<MacroDefinition>> macroses = new HashMap<String, Deque<MacroDefinition>>();
        private final List<List<MacroDefinition>> levelMacroses = new ArrayList<List<MacroDefinition>>(100);
        private int levelCount;
        
        public ScopeContext(List<MacroDefinition> initialMacroses)
        {
            Assert.notNull(initialMacroses);
            
            initialMacroses = new ArrayList<MacroDefinition>(initialMacroses);
            registerDefaultMacroses(initialMacroses);
            
            for (int i = 0; i < initialMacroses.size(); i++)
            {
                Deque<MacroDefinition> stack = new ArrayDeque<MacroDefinition>();
                stack.push(initialMacroses.get(i));
                macroses.put(initialMacroses.get(i).name, stack);
            }
        }
        
        public void beginScope(List<MacroDefinition> list)
        {
            Assert.notNull(list);
            
            levelCount++;
            if (levelCount > levelMacroses.size())
                levelMacroses.add(list);
            else
                levelMacroses.set(levelCount - 1, list);
            
            for (int i = 0; i < list.size(); i++)
            {
                Deque<MacroDefinition> stack = macroses.get(list.get(i).name);
                if (stack == null)
                {
                    stack = new ArrayDeque<MacroDefinition>();
                    macroses.put(list.get(i).name, stack);
                }
                stack.push(list.get(i));
            }
        }

        public void endScope()
        {
            List<MacroDefinition> list = levelMacroses.get(levelCount - 1);
            for (int i = 0; i < list.size(); i++)
            {
                Deque<MacroDefinition> stack = macroses.get(list.get(i).name);
                stack.pop();
            }
            
            levelMacroses.set(levelCount - 1, null);
            levelCount--;
        }

        public MacroDefinition findMacro(String name)
        {
            Deque<MacroDefinition> stack = macroses.get(name);
            if (!Collections.isEmpty(stack))
                return stack.getFirst();
            else
                return null;
        }
    }

    private static class InlineValue
    {
        private final IJsonCollection value;

        public InlineValue(IJsonCollection value)
        {
            this.value = value;
        }
    }
    
    private interface IMessages
    {
        @DefaultMessage("Validation error of ''{0}''. Macro ''{1}'' is not found in current scope.")
        ILocalizedMessage macroNotFound(String path, String name);
        
        @DefaultMessage("Validation error of ''{0}''. Macro ''{1}'' has arguments and can not be used in string value.")
        ILocalizedMessage macroWithArgsInString(String path, String name);
        
        @DefaultMessage("Validation error of ''{0}''. Required argument ''{2}'' is not set for macro ''{1}''.")
        ILocalizedMessage requiredArgNotSet(String path, String macroName, String argName);
        
        @DefaultMessage("Validation error of ''{0}''. Referenced object is not found on path ''{1}''.")
        ILocalizedMessage referentNotFound(String path, String referentPath);
        
        @DefaultMessage("Validation error of ''{0}''. Invalid generator arguments ''{1}''. Generator arguments value must be array or object.")
        ILocalizedMessage invalidGeneratorArgs(String path, Object args);
        
        @DefaultMessage("Validation error of ''{0}''. Invalid generator arguments element ''{1}''. Generator arguments element must be an object.")
        ILocalizedMessage invalidGeneratorArgsElement(String path, Object element);
        
        @DefaultMessage("Validation error of ''{0}''. Expanded inline value must be an array: {1}.")
        ILocalizedMessage inlineMustBeArray(String path, IJsonCollection value);

        @DefaultMessage("Validation error of ''{0}''. Expanded inline value must be an object: {1}.")
        ILocalizedMessage inlineMustBeObject(String path, IJsonCollection value);
    }
}
