/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2016 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.utils;

import android.test.AndroidTestCase;

import java.util.Date;

public class DateUtilsTest extends AndroidTestCase {

    @SuppressWarnings("deprecation")
    public void testEncodeDecode() {
        long t = System.currentTimeMillis();
        String encoded = DateUtils.encodeDate(t);
        long decoded = DateUtils.decodeDate(encoded);

        assertEquals(new Date(t).getYear(), new Date(decoded).getYear());
        assertEquals(new Date(t).getMonth(), new Date(decoded).getMonth());
        assertEquals(new Date(t).getDay(), new Date(decoded).getDay());
        assertEquals(new Date(t).getHours(), new Date(decoded).getHours());
        assertEquals(new Date(t).getMinutes(), new Date(decoded).getMinutes());
        assertEquals(new Date(t).getSeconds(), new Date(decoded).getSeconds());
        assertEquals(new Date(t).getTimezoneOffset(), new Date(decoded).getTimezoneOffset());
    }
}
