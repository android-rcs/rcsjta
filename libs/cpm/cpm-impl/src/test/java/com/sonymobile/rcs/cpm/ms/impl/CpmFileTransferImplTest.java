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

package com.sonymobile.rcs.cpm.ms.impl;

import static org.junit.Assert.assertEquals;

import com.sonymobile.rcs.cpm.ms.FileItem;
import com.sonymobile.rcs.imap.ImapMessage;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Set;

public class CpmFileTransferImplTest {

    protected static final String CRLF = "\r\n";

    @Test
    public void testFileTransferContentParsing() throws Exception {
        File f = new File("src/test/dataset/ft-logo-2001");
        FileInputStream is = new FileInputStream(f);
        byte[] b = new byte[(int) f.length()];
        is.read(b);
        is.close();

        String[] lines = new String(b).split("\n");

        String payload = "";
        for (String line : lines) {
            payload += line + CRLF;
        }

        ImapMessage message = new ImapMessage();
        message.fromPayload(payload);

        CpmFileTransferImpl ft = new CpmFileTransferImpl(message, null);
        Set<FileItem> items = ft.getFiles();
        assertEquals(1, items.size());

        FileItem fi = ft.getFile();
        assertEquals("33333", fi.getContentId());
        assertEquals("This is my latest picture", fi.getSdpValue('i'));
        String content = fi.getContent();
        byte[] data = Base64.decodeBase64(content);
        assertEquals(1101, data.length);

    }

}
