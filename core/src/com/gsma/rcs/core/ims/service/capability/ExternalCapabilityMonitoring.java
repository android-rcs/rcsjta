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

import com.gsma.rcs.core.ims.service.extension.ServiceExtensionManager;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.capability.CapabilityService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;

/**
 * External capability monitoring
 * 
 * @author jexa7410
 */
public class ExternalCapabilityMonitoring extends BroadcastReceiver {
    /**
     * The logger
     */
    private final static Logger sLogger = Logger.getLogger(ExternalCapabilityMonitoring.class
            .getSimpleName());

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            // Instantiate the settings manager
            LocalContentResolver localContentResolver = new LocalContentResolver(context);
            RcsSettings rcsSettings = RcsSettings.createInstance(localContentResolver);

            // Get Intent parameters
            String action = intent.getAction();
            Integer uid = intent.getIntExtra(Intent.EXTRA_UID, -1);
            if (uid == -1) {
                return;
            }

            if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                // Get extensions associated to the new application
                PackageManager pm = context.getPackageManager();
                String packageName = intent.getData().getSchemeSpecificPart();
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName,
                        PackageManager.GET_META_DATA);
                if (appInfo == null) {
                    // No app info
                    return;
                }
                Bundle appMeta = appInfo.metaData;
                if (appMeta == null) {
                    // No app meta
                    return;
                }

                String exts = appMeta.getString(CapabilityService.INTENT_EXTENSIONS);
                if (exts == null) {
                    // No RCS extension
                    return;
                }

                if (sLogger.isActivated()) {
                    sLogger.debug("Add extensions " + exts + " for application " + uid);
                }

                // Add the new extension in the supported RCS extensions
                ServiceExtensionManager.getInstance(rcsSettings).addNewSupportedExtensions(
                        AndroidFactory.getApplicationContext());
            } else {
                if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Remove extensions for application " + uid);
                    }

                    // Remove the extensions in the supported RCS extensions
                    ServiceExtensionManager.getInstance(rcsSettings).removeSupportedExtensions(
                            AndroidFactory.getApplicationContext());
                }
            }
        } catch (NameNotFoundException e) {
            sLogger.error(
                    "Unable to find application for intent action : ".concat(intent.getAction()), e);
        }
    }
}
