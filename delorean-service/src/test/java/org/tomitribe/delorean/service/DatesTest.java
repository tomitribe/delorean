/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.tomitribe.delorean.service;

import org.junit.Assert;
import org.junit.Test;

import java.text.SimpleDateFormat;

public class DatesTest extends Assert {

    @Test
    public void testAsDate() throws Exception {
        assertEquals(0l, Dates.parse("1969-12-31T16:00:00.000-0800").getTime());
//        assertEquals(0l, Dates.parse("1969-12-31 16:00:00 PST").getTime());
//        assertEquals(0l, Dates.parse("1969-12-31 16:00 PST").getTime());
        assertEquals(0l, Dates.parse("1970-01-01 UTC").getTime());
        assertEquals(1494736314000l, Dates.parse(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(1494736314000l)).getTime());
        assertEquals(1494736314000l, Dates.parse(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(1494736314000l)).getTime());
        assertEquals(1494736260000l, Dates.parse(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(1494736260000l)).getTime());
        assertNotNull(Dates.parse("1970-01-01"));
    }

    @Test
    public void failed() {
        try {
            Dates.parse("bad date");
            fail();
        } catch (Dates.UnsupportedDateFormatException e) {
            assertEquals("bad date", e.getDate());
            assertTrue(e.getMessage().startsWith("UnsupportedDateFormatException{date='bad date', " +
                    "supported=[yyyy-MM-dd'T'HH:mm:ss.SSSZ, " +
                    "yyyy-MM-dd'T'HH:mm:ss, " +
                    "yyyy-MM-dd HH:mm:ss z, " +
                    "yyyy-MM-dd HH:mm z, " +
                    "yyyy-MM-dd z, " +
                    "yyyy-MM-dd HH:mm:ss, " +
                    "yyyy-MM-dd HH:mm, " +
                    "yyyy-MM-dd], examples=["));
            assertEquals(8, e.getExamples().size());
            assertEquals(8, e.getSupported().length);
        }
    }

    @Test
    public void offset() {
        final long time = Dates.parse("1976-03-30 UTC").getTime();
        final long offset = time - System.currentTimeMillis();

        assertTrue(offset < 0);
   }

}