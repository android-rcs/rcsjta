/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 ******************************************************************************/

package com.gsma.iariauth.sample;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ExtensionUtils {

    private final static String EXTENSION_PREFIX = "com.gsma.services.rcs.capability.EXTENSION.";

    private static final String TAG = LogUtils.getTag(ExtensionUtils.class.getSimpleName());

    public static List<String> getExtensions(Context context) {
        List<String> result = new ArrayList<String>();
        try {
            PackageInfo info;
            info = context.getPackageManager().getPackageInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA);
            int i = 0;
            String metaDataValue;
            do {
                String metaDataKey = new StringBuilder(EXTENSION_PREFIX).append(i++).toString();
                metaDataValue = info.applicationInfo.metaData.getString(metaDataKey);
                if (metaDataValue != null) {
                    result.add(metaDataValue);
                }
            } while (metaDataValue != null);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Failed to get package info", e);
        }
        return result;
    }

}
