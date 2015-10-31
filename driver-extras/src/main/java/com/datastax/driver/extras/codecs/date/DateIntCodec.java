/*
 *      Copyright (C) 2012-2015 DataStax Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.datastax.driver.extras.codecs.date;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Date;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.datastax.driver.core.*;
import com.datastax.driver.core.exceptions.InvalidTypeException;

import static com.datastax.driver.core.CodecUtils.fromCqlDateToDaysSinceEpoch;
import static com.datastax.driver.core.CodecUtils.fromDaysSinceEpochToCqlDate;
import static com.datastax.driver.core.CodecUtils.fromUnsignedToSignedInt;
import static com.datastax.driver.core.ParseUtils.isLongLiteral;
import static com.datastax.driver.core.ParseUtils.parseDate;
import static com.datastax.driver.core.ParseUtils.quote;
import static com.datastax.driver.core.ParseUtils.unquote;

/**
 * A {@link TypeCodec} that maps CQL dates to Java primitive ints,
 * representing the number of days since the Epoch.
 * <p>
 * This codec can serve as a replacement for the driver's built-in
 * {@link TypeCodec#date() date} codec,
 * when application code prefers to deal with raw days than with
 * {@link LocalDate} instances.
 */
public class DateIntCodec extends TypeCodec.PrimitiveIntCodec {

    public static final DateIntCodec instance = new DateIntCodec();

    private static final String pattern = "yyyy-MM-dd";

    public DateIntCodec() {
        super(DataType.date());
    }

    @Override
    public ByteBuffer serializeNoBoxing(int value, ProtocolVersion protocolVersion) {
        CodecUtils.fromSignedToUnsignedInt(value);
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.putLong(0, value);
        return bb;
    }

    @Override
    public int deserializeNoBoxing(ByteBuffer bytes, ProtocolVersion protocolVersion) {
        if (bytes == null || bytes.remaining() == 0)
            return 0;
        if (bytes.remaining() != 8)
            throw new InvalidTypeException("Invalid 64-bits long value, expecting 8 bytes but got " + bytes.remaining());
        return fromUnsignedToSignedInt(bytes.getInt(bytes.position()));
    }

    @Override
    public Integer parse(String value) {
        if (value == null || value.isEmpty() || value.equalsIgnoreCase("NULL"))
            return null;

        // single quotes are optional for long literals, mandatory for date patterns
        // strip enclosing single quotes, if any
        if (value.charAt(0) == '\'' && value.charAt(value.length() - 1) == '\'')
            value = unquote(value);

        if (isLongLiteral(value)) {
            long unsigned;
            try {
                unsigned = Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new InvalidTypeException(String.format("Cannot parse date value from \"%s\"", value), e);
            }
            try {
                return fromCqlDateToDaysSinceEpoch(unsigned);
            } catch (IllegalArgumentException e) {
                throw new InvalidTypeException(String.format("Cannot parse date value from \"%s\"", value), e);
            }
        }

        try {
            Date date = parseDate(value, pattern);
            return (int)MILLISECONDS.toDays(date.getTime());
        } catch (ParseException e) {
            throw new InvalidTypeException(String.format("Cannot parse date value from \"%s\"", value), e);
        }
    }

    @Override
    public String format(Integer value) {
        if (value == null)
            return "NULL";
        long raw = fromDaysSinceEpochToCqlDate(value);
        return quote(Long.toString(raw));
    }

}
