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

/**
 * @author jexa7410
 */
public class IpAddressUtilsTest extends AndroidTestCase {

    public void testExtractHostAddress() {
        assertEquals(IpAddressUtils.extractHostAddress("127.0.0.1"), "127.0.0.1");
        assertEquals(IpAddressUtils.extractHostAddress("domain.com"), "domain.com");
        assertEquals(IpAddressUtils.extractHostAddress("127.0.0.1%2"), "127.0.0.1");
        assertEquals(IpAddressUtils.extractHostAddress("fe80::1234%1"), "fe80::1234");
        assertEquals(IpAddressUtils.extractHostAddress("ff02::5678%pvc1.3"), "ff02::5678");
        assertEquals(IpAddressUtils.extractHostAddress("169.254.0.0/16"), "169.254.0.0");
    }
}
