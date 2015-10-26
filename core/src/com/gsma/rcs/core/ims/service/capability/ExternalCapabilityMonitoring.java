/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

package com.gsma.rcs.core.ims.service.capability;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.ims.service.extension.ServiceExtensionManager;
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

    private final static Logger sLogger = Logger.getLogger(ExternalCapabilityMonitoring.class
            .getSimpleName());

    private ServiceExtensionManager mExtensionManager;

    private final Core mCore;

    public ExternalCapabilityMonitoring(Core core, ServiceExtensionManager extensionManager) {
        mExtensionManager = extensionManager;
        mCore = core;
    }

    @Override
    public void onReceive(final Context ctx, final Intent intent) {
        mCore.scheduleCoreOperation(new Runnable() {
            @Override
            public void run() {
                try {
                    String action = intent.getAction();
                    int uid = intent.getIntExtra(Intent.EXTRA_UID, -1);
                    if (uid == -1) {
                        return;
                    }
                    if (Intent.ACTION_PACKAGE_ADDED.equals(action)
                            || Intent.ACTION_PACKAGE_REPLACED.equals(action)
                            || Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
                        PackageManager pm = ctx.getPackageManager();
                        String packageName = intent.getData().getSchemeSpecificPart();
                        ApplicationInfo appInfo = pm.getApplicationInfo(packageName,
                                PackageManager.GET_META_DATA);
                        if (appInfo == null) {
                            return;
                        }
                        Bundle appMeta = appInfo.metaData;
                        if (appMeta == null) {
                            return;
                        }
                        String exts = appMeta.getString(CapabilityService.INTENT_EXTENSIONS);
                        if (exts == null) {
                            return;
                        }
                        if (sLogger.isActivated()) {
                            sLogger.debug(new StringBuilder("Add extensions ").append(exts)
                                    .append(" for application ").append(uid).toString());
                        }
                        mExtensionManager.addNewSupportedExtensions();
                    } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                        if (sLogger.isActivated()) {
                            sLogger.debug(new StringBuilder("Remove extensions for application ")
                                    .append(uid).append("with action ").append(action).toString());
                        }
                        mExtensionManager.removeSupportedExtensions();
                    }
                } catch (NameNotFoundException e) {
                    sLogger.error(
                            new StringBuilder("Unable to find application for intent action : ")
                                    .append(intent.getAction()).toString(), e);
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error(
                            new StringBuilder(
                                    "Unable to handle connection event for intent action : ")
                                    .append(intent.getAction()).toString(), e);
                }
            }
        });
    }
}
