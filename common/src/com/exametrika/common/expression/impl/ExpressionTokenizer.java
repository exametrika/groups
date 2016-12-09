/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.expression.impl;

import com.exametrika.common.expression.ExpressionException;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;

/**
 * The {@link ExpressionTokenizer} is an expression tokenizer. Expression can be one of the following:
 *
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev_A
 */
public class ExpressionTokenizer
{
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final int SAMPLE_SIZE = 40;
    private static final int ELLIPSIS_SIZE = 3;
    private final String text;
    private int[] buffer = new int[10];
    private int[] posBuffer = new int[10];
    private int index = -1;
    private int offset;
    private int pos = -1;
    private Token token;
    private Object value;
    private boolean peekToken;
    private int startPos = -1;
    private int endPos = -1;

    public enum Token
    {
        ID,
        STRING,
        CLASS_STRING,
        TEMPLATE_STRING,
        NUMBER,
        COMMA,
        PERIOD,
        SEMICOLON,
        COLON,
        ROUND_OPEN_BRACKET,
        ROUND_CLOSE_BRACKET,
        SQUARE_OPEN_BRACKET,
        SQUARE_CLOSE_BRACKET,
        CURLY_OPEN_BRACKET,
        CURLY_CLOSE_BRACKET,
        PLUS,
        MINUS,
        ASTERISK,
        SLASH,
        PERCENT,
        EXCLAMATION,
        TILDE,
        TILDE_SQUARE_OPEN_BRACKET,
        VERTICAL_BAR,
        DOUBLE_VERTICAL_BAR,
        AMPERSAND,
        DOUBLE_AMPERSAND,
        AMPERSAND_SQUARE_OPEN_BRACKET,
        EQUAL,
        DOUBLE_EQUAL,
        EXCLAMATION_SQUARE_OPEN_BRACKET,
        EXCLAMATION_EQUAL,
        LESS,
        LESS_EQUAL,
        LESS_GREATER,
        DOUBLE_LESS,
        GREATER,
        GREATER_EQUAL,
        DOUBLE_GREATER,
        TRIPLE_GREATER,
        CIRCUMFLEX,
        CIRCUMFLEX_SQUARE_OPEN_BRACKET,
        HASH,
        QUESTION,
        QUESTION_COLON,
        QUESTION_PERIOD,
        DOLLAR
    }
    
    public ExpressionTokenizer(String text)
    {
        this.text = text;
    }

    public String getText()
    {
        return text;
    }
    
    public int getPos()
    {
        return pos;
    }
    
    public Token getToken()
    {
        return token;
    }
    
    public Object getValue()
    {
        return value;
    }
    
    public String getValueText()
    {
        if (startPos >= 0 && endPos < text.length())
            return text.substring(startPos, endPos + 1);
        else
            return "";
    }
    
    public int getStartPos()
    {
        return startPos;
    }
    
    public int getEndPos()
    {
        return endPos;
    }
    
    public static int[] calculateLineCol(String text, int pos)
    {
        int line = 1, col = 1;
        for (int i = 0; i < text.length(); i++)
        {
            if (i == pos)
                break;
            
            int c = text.charAt(i);
            if (c == '\n')
            {
                col = 1;
                line++;
            }
            else if (c == '\t')
                col += 4;
            else
                col++; 
        }
        
        return new int[]{line, col};
    }
    
    public Token peekNextToken()
    {
        if (peekToken)
            return token;
        
        Token token = readNextToken();
        peekToken = true;
        return token;
    }
    
    public Token readNextToken()
    {
        if (peekToken)
        {
            peekToken = false;
            return token;
        }
        
        int c = peek(true);
        if (c == -1)
        {
            value = null;
            token = null;
            startPos = text.length();
            endPos = startPos;
            return null;
        }
        
        startPos = getLastPeekPos();
        if (c == '\"' || c == '\'')
        {
            value = parseString();
            token = Token.STRING;
        }
        else if (c == '\\')
        {
            value = parseNonEscapedString('\\');
            token = Token.STRING;
        }
        else if (c == '@')
        {
            value = parseNonEscapedString('@');
            token = Token.CLASS_STRING;
        }
        else if (c == '`')
        {
            value = parseNonEscapedString('`');
            token = Token.TEMPLATE_STRING;
        }
        else if (Character.isDigit(c))
        {
            value = parseNumber(c);
            token = Token.NUMBER;
        }
        else if (isIdentifierStart(c))
        {
            value = parseIdentifier();
            token = Token.ID;
        }
        else
        {
            switch (c)
            {
            case ',':
                token = Token.COMMA;
                next(true);
                break;
            case '.':
                token = Token.PERIOD;
                next(true);
                break;
            case ';':
                token = Token.SEMICOLON;
                next(true);
                break;
            case ':':
                token = Token.COLON;
                next(true);
                break;
            case '(':
                token = Token.ROUND_OPEN_BRACKET;
                next(true);
                break;
            case ')':
                token = Token.ROUND_CLOSE_BRACKET;
                next(true);
                break;
            case '[':
                token = Token.SQUARE_OPEN_BRACKET;
                next(true);
                break;
            case ']':
                token = Token.SQUARE_CLOSE_BRACKET;
                next(true);
                break;
            case '{':
                token = Token.CURLY_OPEN_BRACKET;
                next(true);
                break;
            case '}':
                token = Token.CURLY_CLOSE_BRACKET;
                next(true);
                break;
            case '+':
                token = Token.PLUS;
                next(true);
                break;
            case '-':
                token = Token.MINUS;
                next(true);
                break;
            case '*':
                token = Token.ASTERISK;
                next(true);
                break;
            case '/':
                token = Token.SLASH;
                next(true);
                break;
            case '%':
                token = Token.PERCENT;
                next(true);
                break;
            case '!':
                token = Token.EXCLAMATION;
                next(true);
                c = peek(true);
                switch (c)
                {
                case '[':
                    token = Token.EXCLAMATION_SQUARE_OPEN_BRACKET;
                    next(true);
                    break;
                case '=':
                    token = Token.EXCLAMATION_EQUAL;
                    next(true);
                    break;
                }
                break;
            case '~':
                token = Token.TILDE;
                next(true);
                c = peek(true);
                switch (c)
                {
                case '[':
                    token = Token.TILDE_SQUARE_OPEN_BRACKET;
                    next(true);
                    break;
                }
                break;
            case '|':
                token = Token.VERTICAL_BAR;
                next(true);
                c = peek(true);
                switch (c)
                {
                case '|':
                    token = Token.DOUBLE_VERTICAL_BAR;
                    next(true);
                    break;
                }
                break;
            case '&':
                token = Token.AMPERSAND;
                next(true);
                c = peek(true);
                switch (c)
                {
                case '&':
                    token = Token.DOUBLE_AMPERSAND;
                    next(true);
                    break;
                case '[':
                    token = Token.AMPERSAND_SQUARE_OPEN_BRACKET;
                    next(true);
                    break;
                }
                break;
            case '=':
                token = Token.EQUAL;
                next(true);
                c = peek(true);
                switch (c)
                {
                case '=':
                    token = Token.DOUBLE_EQUAL;
                    next(true);
                    break;
                }
                break;
            case '<':
                token = Token.LESS;
                next(true);
                c = peek(true);
                switch (c)
                {
                case '=':
                    token = Token.LESS_EQUAL;
                    next(true);
                    break;
                case '<':
                    token = Token.DOUBLE_LESS;
                    next(true);
                    break;
                case '>':
                    token = Token.LESS_GREATER;
                    next(true);
                    break;
                }
                break;
            case '>':
                token = Token.GREATER;
                next(true);
                c = peek(true);
                switch (c)
                {
                case '=':
                    token = Token.GREATER_EQUAL;
                    next(true);
                    break;
                case '>':
                    next(true);
                    token = Token.DOUBLE_GREATER;
                    c = peek(true);
                    if (c == '>')
                    {
                        token = Token.TRIPLE_GREATER;
                        next(true);
                    }
                    break;
                }
                break;
            case '^':
                token = Token.CIRCUMFLEX;
                next(true);
                c = peek(true);
                switch (c)
                {
                case '[':
                    next(true);
                    token = Token.CIRCUMFLEX_SQUARE_OPEN_BRACKET;
                    break;
                }
                break;
            case '#':
                token = Token.HASH;
                next(true);
                break;
            case '?':
                token = Token.QUESTION;
                next(true);
                c = peek(true);
                switch (c)
                {
                case ':':
                    next(true);
                    token = Token.QUESTION_COLON;
                    break;
                case '.':
                    next(true);
                    token = Token.QUESTION_PERIOD;
                    break;
                }
                break;
            case '$':
                token = Token.DOLLAR;
                next(true);
                break;
            default:
                throwError(messages.unknownToken((char)c));
            }
            value = null;
        }
        
        endPos = pos;
        
        return token;
    }
    
    public void throwError(ILocalizedMessage message)
    {
        int[] location = calculateLineCol(text, pos);

        StringBuilder sample = new StringBuilder();
        StringBuilder pointer = new StringBuilder();
        if (pos - SAMPLE_SIZE / 2 >= 0)
            sample.append(Strings.duplicate('.', ELLIPSIS_SIZE));
            
        String left = text.substring(Math.max(0, pos - SAMPLE_SIZE / 2), pos + 1);
        int p = left.lastIndexOf('\n');
        if (p != -1)
            left = left.substring(p + 1);
        
        sample.append(left);
        
        if (sample.length() > 1)
            pointer.append(Strings.duplicate(' ', sample.length() - 1));
        pointer.append('^');
        
        String right = text.substring(pos + 1, Math.min(text.length(), pos + SAMPLE_SIZE / 2));
        p = right.indexOf('\n');
        if (p != -1)
            right = right.substring(0, p);

        sample.append(right);
        
        if (pos + SAMPLE_SIZE / 2 < text.length())
            sample.append(Strings.duplicate('.', ELLIPSIS_SIZE));
        
        throw new ExpressionException(messages.parseError(location[0], location[1], message.toString(), sample.toString(),
            pointer.toString()));
    }

    private boolean isIdentifierStart(int c)
    {
        return Character.isLetter(c) || c == '_';
    }
    
    private String parseIdentifier()
    {
        StringBuilder builder = new StringBuilder();
        builder.append((char)next(false));
        while (true)
        {
            int c = peek(false);
            if (isIdentifierStart(c) || Character.isDigit(c))
                builder.append((char)next(false));
            else
                break;
        }
        
        return builder.toString();
    }

    private String parseString()
    {
        String delimiters = "";
        int c = next(true);
        if (c == '\"')
        {
            delimiters = "\"";
            c = next(false);
        }
        else if (c == '\'')
        {
            delimiters = "\'";
            c = next(false);
        }
        
        StringBuilder builder = new StringBuilder();
        while (true)
        {
            if (delimiters.indexOf(c) != -1)
                break;
            
            if (c == '\\')
            {
                c = next(false);
                switch (c)
                {
                case 'b':
                    builder.append('\b');
                    break;
                case 'f':
                    builder.append('\f');
                    break;
                case 'r':
                    builder.append('\r');
                    break;
                case 'n':
                    builder.append('\n');
                    break;
                case 't':
                    builder.append('\t');
                    break;
                case 'u':
                    builder.append(parseUnicode());
                    break;
                case '\r':
                case '\n':
                    c = next(true);
                    back(c);
                    break;
                default:
                    builder.append((char)c);
                }
            }
            else if (c == '\r' || c == '\n')
            {
                c = next(true);
                back(c);
            }
            else if (c != -1)
                builder.append((char)c);
            else
                throwError(messages.unclosedString());
            
            c = next(false);
        }
        
        return builder.toString();
    }
    
    private char parseUnicode()
    {
        int value = parseHexDigit() << 12;
        value |= parseHexDigit() << 8;
        value |= parseHexDigit() << 4;
        value |= parseHexDigit();
        
        return (char)value;
    }
    
    private int parseHexDigit()
    {
        int c = next(false);
        if (c >= '0' && c <= '9')
            return c - '0';
        else if (c >= 'A' && c <= 'F')
            return c - 'A';
        else if (c >= 'a' && c <= 'f')
            return c - 'a';
        else
        {
            throwError(messages.hexDigitExpected());
            return 0;
        }
    }
    
    private String parseNonEscapedString(char delimiter)
    {
        next(false);
        
        StringBuilder builder = new StringBuilder();
        while (true)
        {
            int c = next(false);
            if (c == delimiter)
                break;
            else if (c != -1)
                builder.append((char)c);
            else
                throwError(messages.unclosedString());
        }
        
        return builder.toString();
    }
    
    private Number parseNumber(int c)
    {
        if (c == '0')
        {
            next(false);
            c = peek(false);
            if (c == 'x' || c == 'X')
                return parseHexNumber();
            else
                back('0');
        }
        
        return parseDecimalNumber();
    }
    
    private Number parseDecimalNumber()
    {
        StringBuilder builder = new StringBuilder();
        boolean isDouble = false;
        boolean scientific = false;
        boolean sign = false;
        
        while (true)
        {
            int c = next(false);
            if (!isDouble)
            {
                if (c == '.')
                {
                    builder.append((char)c);
                    isDouble = true;
                    continue;
                }
            }
            if (!scientific)
            {
                if (c == 'E' || c == 'e')
                {
                    builder.append((char)c);
                    isDouble = true;
                    scientific = true;
                    continue;
                }
            }
            if (scientific && !sign)
            {
                if (c == '+' || c == '-')
                {
                    builder.append((char)c);
                    sign = true;
                    continue;
                }
                else
                    sign = true;
            }

            if (Character.isDigit(c))
                builder.append((char)c);
            else
            {
                back(c);
                pos--;
                break;
            }
        }
        
        if (isDouble)
            return Double.valueOf(builder.toString());
        else
            return Long.valueOf(builder.toString());
    }
    
    private Number parseHexNumber()
    {
        next(false);
        StringBuilder builder = new StringBuilder();
        
        while (true)
        {
            int c = next(false);
            
            if (c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f')
                builder.append((char)c);
            else
            {
                back(c);
                pos--;
                break;
            }
        }
         
        return Long.valueOf(builder.toString(), 16);
    }
    
    private int peek(boolean skipWhitespace)
    {
        int prevPos = pos;
        int c = next(skipWhitespace);
        back(c);
        pos = prevPos;
        return c;
    }
    
    private int getLastPeekPos()
    {
        return posBuffer[index];
    }
    
    private int next(boolean skipWhitespace)
    {
        if (!skipWhitespace)
            return next();
        
        while (true)
        {
            int c = next();
            
            if (c == -1)
                return c;
            
            if (c == ' ' || c == '\r' || c == '\n' || c == '\t')
                continue;
            
            if (c == '/')
            {
                int prevPos = pos;
                c = next();
                if (c == '/')
                {
                    skipSingleLineComment();
                    continue;
                }
                else if (c == '*')
                {
                    skipMultiLineComment();
                    continue;
                }
                
                back(c);
                pos = prevPos;
                return '/';
            }
            
            return c;
        }
    }
    
    private void skipSingleLineComment()
    {
        while (true)
        {
            int c = next(false);
            if (c == '\r' || c == '\n' || c == -1)
                break;
        }
    }

    private void skipMultiLineComment()
    {
        while (true)
        {
            int c = next(false);
            if (c == -1)
                break;
            if (c == '*' && peek(false) == '/')
            {
                next(false);
                break;
            }
        }
    }

    private int next()
    {
        int c;
        if (index > -1)
        {
            c = buffer[index];
            pos = posBuffer[index];
            index--;
        }
        else if (offset < text.length())
        {
            c = text.charAt(offset);
            pos = offset;
            offset++;
        }
        else
            c = -1;
        
        return c;
    }
    
    private void back(int c)
    {
        if (c == -1)
            return;
        
        Assert.isTrue(index < buffer.length - 1);
        ++index;
        buffer[index] = c;
        posBuffer[index] = pos;
    }

    private interface IMessages
    {
        @DefaultMessage("Hex digit is expected.")
        ILocalizedMessage hexDigitExpected();
        
        @DefaultMessage("String is not closed.")
        ILocalizedMessage unclosedString();
        
        @DefaultMessage("Unknown token ''{0}''.")
        ILocalizedMessage unknownToken(char c);
        
        @DefaultMessage("Parse error at [ln:{0}, col:{1}]: {2}\nNear: [{3}]\n       {4}")
        ILocalizedMessage parseError(int line, int col, String message, String sample, String pointer);
    }  
}
