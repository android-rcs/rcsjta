/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.sonymobile.rcs.imap;

import static com.sonymobile.rcs.imap.ImapUtil.CRLF;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

public class HeaderTest {

    @Test
    public void testCleanComments() {
        String s = "From: Pete(A nice \\) chap) <pete(his account)@silly.test(his host)>";
        assertEquals("From: Pete <pete@silly.test>", Header.cleanComments(s));
        assertEquals("xxxyyyzzz", Header.cleanComments("xxx(nnn)yyy(oooo)zzz"));
    }

    @Test
    public void testCreateHeader() {

        assertNull(Header.createHeader("bla bla bla").getValue());

        String headerSpec = "Content-type: multipart/mixed; boundary=\"simple boundary\"";

        Header h = Header.createHeader(headerSpec);

        assertEquals("Content-type", h.getKey());

        assertEquals("multipart/mixed; boundary=\"simple boundary\"", h.getValue());

        assertEquals("simple boundary", h.getValueAttribute("boundary"));

    }

    @Test
    public void createHeaders() {
        String[] h = {
                "From: Pete(A nice \\) chap) <pete(his account)@silly.test(his host)>",
                "To:A Group(Some people)", "     :Chris Jones <c@(Chris's host.)public.example>,",
                "        joe@example.org,",
                "  John <jdoe@one.test> (my dear friend); (the end of the group)",
                "Cc:(Empty list)(start)Hidden recipients  :(nobody(that I know))  ;", "Date: Mon,",
                "      13", "        Feb", "          1989", "      23:32",
                "               -0330 (Newfoundland Time)",
                "Message-ID:              <testabcd.1234@silly.test>"
        };
        String headerString = "";
        for (int i = 0; i < h.length; i++) {
            headerString += h[i] + CRLF;
        }

        Map<String, Header> headerMap = Header.parseHeaders(headerString);
        assertEquals(5, headerMap.size());
        assertEquals("Pete <pete@silly.test>", headerMap.get("From").getValue());
        assertEquals(
                "A Group    :Chris Jones <c@public.example>,       joe@example.org, John <jdoe@one.test> ;",
                headerMap.get("To").getValue());
        assertEquals("Hidden recipients  :  ;", headerMap.get("Cc").getValue());
        assertEquals("Mon,     13       Feb         1989     23:32              -0330", headerMap
                .get("Date").getValue());
        assertEquals("<testabcd.1234@silly.test>", headerMap.get("Message-ID").getValue());

    }

    @Test
    public void testSpecialHeaderTests() throws ParseException {

        String normalized = Header
                .normalizeSpace("Mon,     13       Feb         1989     23:32              -0330");
        assertEquals("Mon, 13 Feb 1989 23:32 -0330", normalized);

        String spec = "Content-type: multipart/mixed; boundary=\"simple boundary\"";

        assertEquals("multipart/mixed", Header.createHeader(spec).getMimeType());

        spec = " Content-Type: text/plain; charset=\"UTF-8\"";

        assertEquals("text/plain", Header.createHeader(spec).getMimeType());

        spec = "Date: Mon,     13       Feb         1989     23:32:01              -0330";
        Date d = Header.createHeader(spec).getValueAsDate();

        long time = d.getTime();
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT-3:30"));
        c.setTime(d);

        assertEquals(1989, c.get(Calendar.YEAR));
        assertEquals(32, c.get(Calendar.MINUTE));
        assertEquals(23, c.get(Calendar.HOUR_OF_DAY));
        assertEquals(1, c.get(Calendar.SECOND));

        assertEquals(Calendar.FEBRUARY, c.get(Calendar.MONTH));

        assertEquals(13, c.get(Calendar.DAY_OF_MONTH));
        assertEquals(Calendar.MONDAY, c.get(Calendar.DAY_OF_WEEK));

    }

}
