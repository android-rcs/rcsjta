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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ImapUtilTest {

    @Test
    public void testBase64() {
        String token = "\0test1@localhost\0password";
        String encoded = ImapUtil.encodeBase64(token.getBytes()).trim();
        assertEquals("AHRlc3QxQGxvY2FsaG9zdABwYXNzd29yZA==", encoded);
        assertEquals(token, new String(ImapUtil.decodeBase64(encoded.getBytes())));
    }

    @Test
    public void testFlagsAsStringFlagArray() {
        assertEquals("(\\Deleted \\Seen)", ImapUtil.getFlagsAsString(Flag.Deleted, Flag.Seen));
        List<Flag> flags = new ArrayList<Flag>();
        flags.add(Flag.Recent);
        flags.add(Flag.Answered);
        flags.add(Flag.Flagged);
        assertEquals("(\\Recent \\Answered \\Flagged)", ImapUtil.getFlagsAsString(flags));
    }

}
