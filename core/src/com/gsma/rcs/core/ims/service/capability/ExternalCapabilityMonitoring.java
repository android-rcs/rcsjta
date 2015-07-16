/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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

package com.gsma.rcs.core.ims.service.capability;

import com.gsma.rcs.core.Core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * External capability monitoring
 * 
 * @author LEMORDANT Philippe
 */
public class ExternalCapabilityMonitoring extends BroadcastReceiver {

    private static final int INVALID_UID = -1;

    @Override
    public void onReceive(Context context, final Intent intent) {
        String action = intent.getAction();
        Integer uid = intent.getIntExtra(Intent.EXTRA_UID, INVALID_UID);
        if (uid == INVALID_UID) {
            return;
        }

        Uri uri = intent.getData();
        String packageName = uri != null ? uri.getSchemeSpecificPart() : null;
        if (packageName == null) {
            return;
        }
        boolean packageRemoved = false;
        if (Intent.ACTION_PACKAGE_ADDED.equals(action)
                || Intent.ACTION_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
        } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
            packageRemoved = true;
        } else {
            return;
        }
        Core core = Core.getInstance();
        if (core != null) {
            core.getListener()
                    .updateSupportedExtensionsForPackage(uid, packageName, packageRemoved);
        }
    }

}
