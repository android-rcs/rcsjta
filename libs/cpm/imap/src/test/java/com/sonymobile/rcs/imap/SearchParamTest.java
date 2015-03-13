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

import java.util.Calendar;

public class SearchParamTest {

    @Test
    public void testSearchParam() {
        Search search = new Search();
        assertEquals("ALL", search.all().toString().trim());
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2014);
        cal.set(Calendar.MONTH, 10);
        cal.set(Calendar.DAY_OF_MONTH, 4);
        search = new Search().answered().before(cal).draft().to("<me@localhost>");
        assertEquals("ANSWERED BEFORE 4-Nov-2014 DRAFT TO <me@localhost>", search.toString().trim());
    }

}
