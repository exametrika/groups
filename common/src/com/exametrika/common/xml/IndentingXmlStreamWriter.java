/**
 * Copyright 2011 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.xml;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.exametrika.common.utils.Assert;


/**
 * The {@link IndentingXmlStreamWriter} is a {@link XMLStreamWriter} implementation that indents output text.
 * 
 * @threadsafety This class and its methods are not thread safe.
 * @author Medvedev-A
 */
public class IndentingXmlStreamWriter extends XmlStreamWriterDelegate
{
    /** Two spaces, the default indentation. */
    public static final String DEFAULT_INDENT = "  ";
    /**
     * "\n"; the default representation of end-of-line in <a
     * href="http://www.w3.org/TR/xml11/#sec-line-ends">XML</a>.
     */
    public static final String DEFAULT_LINE_SEPARATOR = "\n";

    private static final int WROTE_MARKUP = 1;
    private static final int WROTE_DATA = 2;
    private String indent = DEFAULT_INDENT;
    private String lineSeparator = getDefaultLineSeparator();
    private int depth = 0;
    private int[] stack = new int[]{0, 0, 0, 0};

    public IndentingXmlStreamWriter(XMLStreamWriter out)
    {
        super(out);
    }

    /**
     * Set the characters for one level of indentation. The default is {@link #DEFAULT_INDENT}. "\t" is a popular
     * alternative.
     * 
     * @param indent the characters for one level of indentation
     */
    public void setIndent(String indent)
    {
        Assert.notNull(indent);
        
        this.indent = indent;
    }

    /**
     * Set the characters that introduce a new line. The default is {@link #DEFAULT_LINE_SEPARATOR}.
     * {@link #getDefaultLineSeparator}() is a popular alternative.
     * 
     * @param newLine the characters that introduce a new line
     */
    public void setNewLine(String newLine)
    {
        Assert.notNull(newLine);

        this.lineSeparator = newLine;
    }

    /**
     * Returns default line separator.
     * 
     * @return System.getProperty("line.separator"); or {@link #DEFAULT_LINE_SEPARATOR} if that fails.
     */
    public static String getDefaultLineSeparator()
    {
        try
        {
            return System.getProperty("line.separator");
        }
        catch (Exception ignored)
        {
        }
        return DEFAULT_LINE_SEPARATOR;
    }

    @Override
    public void writeStartDocument() throws XMLStreamException
    {
        beforeMarkup();
        out.writeStartDocument();
        afterMarkup();
    }

    @Override
    public void writeStartDocument(String version) throws XMLStreamException
    {
        beforeMarkup();
        out.writeStartDocument(version);
        afterMarkup();
    }

    @Override
    public void writeStartDocument(String encoding, String version) throws XMLStreamException
    {
        beforeMarkup();
        out.writeStartDocument(encoding, version);
        afterMarkup();
    }

    @Override
    public void writeDTD(String dtd) throws XMLStreamException
    {
        beforeMarkup();
        out.writeDTD(dtd);
        afterMarkup();
    }

    @Override
    public void writeProcessingInstruction(String target) throws XMLStreamException
    {
        beforeMarkup();
        out.writeProcessingInstruction(target);
        afterMarkup();
    }

    @Override
    public void writeProcessingInstruction(String target, String data) throws XMLStreamException
    {
        beforeMarkup();
        out.writeProcessingInstruction(target, data);
        afterMarkup();
    }

    @Override
    public void writeComment(String data) throws XMLStreamException
    {
        beforeMarkup();
        out.writeComment(data);
        afterMarkup();
    }

    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException
    {
        beforeMarkup();
        out.writeEmptyElement(localName);
        afterMarkup();
    }

    @Override
    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException
    {
        beforeMarkup();
        out.writeEmptyElement(namespaceURI, localName);
        afterMarkup();
    }

    @Override
    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException
    {
        beforeMarkup();
        out.writeEmptyElement(prefix, localName, namespaceURI);
        afterMarkup();
    }

    @Override
    public void writeStartElement(String localName) throws XMLStreamException
    {
        beforeStartElement();
        out.writeStartElement(localName);
        afterStartElement();
    }

    @Override
    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException
    {
        beforeStartElement();
        out.writeStartElement(namespaceURI, localName);
        afterStartElement();
    }

    @Override
    public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException
    {
        beforeStartElement();
        out.writeStartElement(prefix, localName, namespaceURI);
        afterStartElement();
    }

    @Override
    public void writeCharacters(String text) throws XMLStreamException
    {
        out.writeCharacters(text);
        afterData();
    }

    @Override
    public void writeCharacters(char[] text, int start, int len) throws XMLStreamException
    {
        out.writeCharacters(text, start, len);
        afterData();
    }

    @Override
    public void writeCData(String data) throws XMLStreamException
    {
        out.writeCData(data);
        afterData();
    }

    @Override
    public void writeEntityRef(String name) throws XMLStreamException
    {
        out.writeEntityRef(name);
        afterData();
    }

    @Override
    public void writeEndElement() throws XMLStreamException
    {
        beforeEndElement();
        out.writeEndElement();
        afterEndElement();
    }

    @Override
    public void writeEndDocument() throws XMLStreamException
    {
        while (depth > 0)
            writeEndElement();
            
        out.writeEndDocument();
        afterEndDocument();
    }

    private void beforeMarkup() throws XMLStreamException
    {
        int soFar = stack[depth];
        if ((soFar & WROTE_DATA) == 0 && (depth > 0 || soFar != 0))
        {
            writeNewLine(depth);
            if (depth > 0 && indent.length() > 0)
                afterMarkup();
        }
    }

    private void afterMarkup()
    {
        stack[depth] |= WROTE_MARKUP;
    }

    private void afterData()
    {
        stack[depth] |= WROTE_DATA;
    }

    private void beforeStartElement() throws XMLStreamException
    {
        beforeMarkup();
        if (stack.length <= depth + 1)
        {
            int[] newStack = new int[stack.length * 2];
            System.arraycopy(stack, 0, newStack, 0, stack.length);
            stack = newStack;
        }
        stack[depth + 1] = 0;
    }

    private void afterStartElement()
    {
        afterMarkup();
        depth++;
    }

    private void beforeEndElement() throws XMLStreamException
    {
        if (depth > 0 && stack[depth] == WROTE_MARKUP)
            writeNewLine(depth - 1);
    }

    private void afterEndElement()
    {
        if (depth > 0)
            depth--;
    }

    private void afterEndDocument() throws XMLStreamException
    {
        if (stack[depth = 0] == WROTE_MARKUP)
            writeNewLine(0);

        stack[depth] = 0;
    }

    private void writeNewLine(int indentation) throws XMLStreamException
    {
        StringBuilder builder = new StringBuilder();
        builder.append(lineSeparator);
        
        for (int i = 0 ; i < indentation; i++)
            builder.append(indent);

        out.writeCharacters(builder.toString());
    }
}