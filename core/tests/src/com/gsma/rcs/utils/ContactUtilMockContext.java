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

package com.gsma.rcs.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.test.mock.MockContext;
import android.test.mock.MockResources;

/**
 * A class to mock up a Context object for create singleton for ContactUtil.<br>
 * All ContactUtil.getInstance(...) calls MUST an instance of this class as input parameter.
 * 
 * @author YPLO6403
 */
public class ContactUtilMockContext extends MockContext {

    private final static int MCC_COUNTRY_CODE = 240; // Sweden
    public final static String COUNTRY_CODE = "+46";
    public final static String COUNTRY_AREA_CODE = "0";

    private final Context mDelegatedContext;

    public ContactUtilMockContext(Context context) {
        mDelegatedContext = context;
    }

    @Override
    public Resources getResources() {
        mDelegatedContext.getResources();
        return new MockResources() {

            @Override
            public Configuration getConfiguration() {
                Configuration config = mDelegatedContext.getResources().getConfiguration();
                config.mcc = MCC_COUNTRY_CODE;
                return config;
            }

        };
    }
}
