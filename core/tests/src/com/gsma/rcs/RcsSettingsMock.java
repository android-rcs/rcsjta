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

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;

public class RcsSettingsMock {

    private static ContactId mOriContact;
    private static RcsSettings sSettings;

    public static RcsSettings getMockSettings(Context context) throws RcsPermissionDeniedException {
        sSettings = RcsSettings.getInstance(new LocalContentResolver(context));
        AndroidFactory.setApplicationContext(context, sSettings);
        mOriContact = sSettings.getUserProfileImsUserName();
        sSettings.setUserProfileImsUserName(ContactUtil
                .createContactIdFromTrustedData("+33601020304"));
        PhoneUtils.initialize(sSettings);
        return sSettings;
    }

    public static void restoreSettings() {
        sSettings.setUserProfileImsUserName(mOriContact);
    }

}
