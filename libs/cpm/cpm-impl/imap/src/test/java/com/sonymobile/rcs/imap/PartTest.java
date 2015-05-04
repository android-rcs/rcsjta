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

import org.apache.commons.codec.binary.Base64;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PartTest {

    static byte[] image1 = null;

    static String image1String = null;

    @BeforeClass
    public static void setUp() throws IOException {
        InputStream in = PartTest.class.getResourceAsStream("/T.jpeg");
        ByteArrayOutputStream boo = new ByteArrayOutputStream();
        int b = -1;
        while ((b = in.read()) != -1) {
            boo.write(b);
        }
        image1 = boo.toByteArray();

        image1String = new String(Base64.encodeBase64(image1, true));
        in.close();
    }

    @Test
    public void testPartBinary() {
        Part part = new Part();
        String payload = "Attr1: Val1" + CRLF + CRLF + image1String;
        part.fromPayload(payload);

        assertEquals("Val1", part.getHeader("Attr1"));
        assertEquals(image1.length, part.decodeBinaryContent().length);
    }

    @Test
    public void testPartText() {
        Part p1 = new Part();
        p1.setHeader("SomeKey", "  Some Value Here  ");
        p1.setHeader("Key-1", "   \tSome Other Value Here ");

        p1.setContent("This is my content");

        assertEquals("Some Value Here", p1.getHeader("SomeKey"));
        assertEquals("Some Other Value Here", p1.getHeader("Key-1"));
        assertEquals("This is my content", p1.getContent());

        Part p2 = new Part();
        p2.fromPayload(p1.toPayload());

        assertEquals("Some Value Here", p2.getHeader("SomeKey"));
        assertEquals("Some Other Value Here", p2.getHeader("Key-1"));
        assertEquals("This is my content", p2.getContent());

    }

}
