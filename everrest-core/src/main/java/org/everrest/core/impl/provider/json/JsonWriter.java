/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.everrest.core.impl.provider.json;

import org.everrest.core.impl.provider.json.JsonUtils.JsonToken;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id$
 */
public class JsonWriter {

    /** Stack for control position in document. */
    private final JsonStack<JsonToken> stack;

    /** Writer. */
    private final Writer writer;

    /** Indicate is comma must be written before next object or value. */
    private boolean commaFirst;

    /**
     * Constructs JsonWriter.
     *
     * @param writer
     *         Writer.
     */
    public JsonWriter(Writer writer) {
        this.writer = writer;
        this.stack = new JsonStack<JsonToken>();
        this.commaFirst = false;
    }

    /**
     * Constructs JsonWriter.
     *
     * @param out
     *         OutputStream.
     */
    public JsonWriter(OutputStream out) {
        this(new OutputStreamWriter(out, JsonUtils.DEFAULT_CHARSET));
    }


    public void writeStartObject() throws JsonException {
        JsonToken token = stack.peek();
        // Object can be stated after key with followed ':' or as array item.
        if (token != null && token != JsonToken.key && token != JsonToken.array) {
            throw new JsonException("Syntax error. Unexpected element '{'.");
        }
        try {
            if (commaFirst) {
                // needed ',' before
                writer.write(',');
            }
            writer.write('{');
            // if at the top of stack is 'key' then remove it.
            if (token == JsonToken.key) {
                stack.pop();
            }
            // remember new object opened
            stack.push(JsonToken.object);
            commaFirst = false;
        } catch (IOException e) {
            throw new JsonException(e.getMessage(), e);
        }
    }


    public void writeEndObject() throws JsonException {
        try {
            JsonToken token = stack.pop();
            if (token != JsonToken.object) {
                //System.out.println(token);
                // wrong JSON structure.
                throw new JsonException("Syntax error. Unexpected element '}'.");
            }
            writer.write('}');
            commaFirst = true;
        } catch (IOException e) {
            throw new JsonException(e.getMessage(), e);
        }
    }


    public void writeStartArray() throws JsonException {
        JsonToken token = stack.peek();
        //if (token != JsonToken.key && token != JsonToken.array)
        if (token != null && token != JsonToken.key && token != JsonToken.array) {
            throw new JsonException("Syntax error. Unexpected element '['.");
        }
        try {
            if (commaFirst) {
                // needed ',' before
                writer.write(',');
            }
            writer.write('[');
            if (token == JsonToken.key) {
                // if at the top of stack is 'key' then remove it.
                stack.pop();
            }
            // remember new array opened
            stack.push(JsonToken.array);
            commaFirst = false;
        } catch (IOException e) {
            throw new JsonException(e.getMessage(), e);
        }
    }


    public void writeEndArray() throws JsonException {
        JsonToken token = stack.pop();
        try {
            if (token != JsonToken.array) {
                // wrong JSON structure
                throw new JsonException("Syntax error. Unexpected element ']'.");
            }
            writer.write(']');
            commaFirst = true;
        } catch (IOException e) {
            throw new JsonException(e.getMessage(), e);
        }
    }


    public void writeKey(String key) throws JsonException {
        if (key == null) {
            throw new JsonException("Key is null.");
        }

        JsonToken token = stack.peek();
        if (token != JsonToken.object) {
            throw new JsonException("Syntax error. Unexpected characters '" + key + "'.");
        }
        try {
            if (commaFirst) {
                writer.write(',');
            }
            // create JSON representation for given string.
            writer.write(JsonUtils.getJsonString(key));
            writer.write(':');
            commaFirst = false;
            stack.push(JsonToken.key);
        } catch (IOException e) {
            throw new JsonException(e.getMessage(), e);
        }
    }


    public void writeString(String value) throws JsonException {
        write(JsonUtils.getJsonString(value));
    }


    public void writeValue(long value) throws JsonException {
        write(Long.toString(value));
    }


    public void writeValue(double value) throws JsonException {
        write(Double.toString(value));
    }


    public void writeValue(boolean value) throws JsonException {
        write(Boolean.toString(value));
    }


    public void writeNull() throws JsonException {
        write("null");
    }

    /**
     * Write single String.
     *
     * @param value
     *         String.
     * @throws JsonException
     *         if any errors occurs.
     */
    private void write(String value) throws JsonException {
        JsonToken token = stack.peek();
        try {
            if (token != JsonToken.key && token != JsonToken.array) {
                throw new JsonException("Syntax error. Unexpected characters '" + value + "'.");
            }
            if (commaFirst) {
                writer.write(',');
            }
            writer.write(value);
            commaFirst = true;
            if (token == JsonToken.key) {
                // if at the top of stack is 'key' then remove it.
                stack.pop();
            }
        } catch (IOException e) {
            throw new JsonException(e.getMessage(), e);
        }
    }


    public void flush() throws JsonException {
        try {
            writer.flush();
        } catch (IOException e) {
            throw new JsonException(e.getMessage(), e);
        }
    }


    public void close() throws JsonException {
        try {
            writer.close();
        } catch (IOException e) {
            throw new JsonException(e.getMessage(), e);
        }
    }

}
