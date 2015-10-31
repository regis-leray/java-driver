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

import java.text.ParseException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

import static com.datastax.driver.core.CodecUtils.fromCqlDateToDaysSinceEpoch;
import static com.datastax.driver.core.ParseUtils.parseDate;

public class DateIntCodecTest {

    @DataProvider(name = "DateIntCodecTest")
    public Object[][] parseParameters() throws ParseException {
        return new Object[][]{
            { "0"                 , fromCqlDateToDaysSinceEpoch(0) },
            { "'2147483648'"      , 0 },
            // SimpleDateFormat is unable to parse year -5877641
            //{ "'-5877641-06-23'"  , fromCqlDateToDaysSinceEpoch(0) },
            { "'1970-01-01'"      , 0 },
            { "'2014-01-01'"      , (int)MILLISECONDS.toDays(parseDate("2014-01-01", "yyyy-MM-dd").getTime()) },
            { "'1951-06-24'"      , (int)MILLISECONDS.toDays(parseDate("1951-06-24", "yyyy-MM-dd").getTime()) }
        };
    }

    @Test(groups = "unit", dataProvider = "DateIntCodecTest")
    public void should_parse_valid_formats(String input, int expected) {
        // when
        Integer actual = DateIntCodec.instance.parse(input);
        // then
        assertThat(actual).isEqualTo(expected);
    }

    @Test(groups = "unit")
    public void should_format_valid_object() {
        // given
        Integer input = 0; // epoch
        // when
        String actual = DateIntCodec.instance.format(input);
        // then
        assertThat(actual).isEqualTo("'2147483648'");
    }

}
