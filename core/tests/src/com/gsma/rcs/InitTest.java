/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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
 ******************************************************************************/

package com.gsma.rcs;

import com.gsma.rcs.utils.ContactUtilMockContext;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.contact.ContactUtil;

import android.content.Context;
import android.content.res.Configuration;
import android.test.AndroidTestCase;

/**
 * Caution: this class is set at the top of the class naming hierarchy in order to be first
 * executed.
 */
public class InitTest extends AndroidTestCase {

    private static final Logger sLogger = Logger.getLogger(InitTest.class.getName());

    private ContactUtil mContactUtils;

    protected void setUp() throws Exception {
        super.setUp();
        Context ctx = getContext();
        mContactUtils = ContactUtil.getInstance(new ContactUtilMockContext(ctx));
        if (sLogger.isActivated()) {
            Configuration config = ctx.getResources().getConfiguration();
            sLogger.warn("mcc='" + config.mcc + "'");
            sLogger.debug("Country Code='" + mContactUtils.getMyCountryCode()
                    + "' Country Area Code='" + mContactUtils.getMyCountryAreaCode() + "'");
        }
    }

    public void testGetMyCountryAreaCode() throws RcsPermissionDeniedException {
        assertEquals("Country Code='" + mContactUtils.getMyCountryCode() + "' Country Area Code='"
                + mContactUtils.getMyCountryAreaCode() + "'",
                ContactUtilMockContext.COUNTRY_AREA_CODE, mContactUtils.getMyCountryAreaCode());
    }

}
