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

package com.sonymobile.rcs.cpm.ms;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class FileItemTest {

    @Test
    public void testFileItem() {
        FileItem fi = new FileItem("abcd", "a=1\nb=2");

        assertEquals("abcd", fi.getContentId());
        assertEquals("2", fi.getSdpValue('b'));

        Map<Character, String[]> m = new HashMap<Character, String[]>();
        m.put('t', new String[] {
            "TEST"
        });

        fi = new FileItem("werty", m);
        assertEquals("TEST", fi.getSdpValue('t'));

        assertEquals("t=TEST", fi.getSdpMapAsString());

    }

}
