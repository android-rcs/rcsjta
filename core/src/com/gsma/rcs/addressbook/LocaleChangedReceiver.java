/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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

package com.gsma.rcs.addressbook;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;

/**
 * The device's locale has changed
 */
public class LocaleChangedReceiver extends BroadcastReceiver {

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(LocaleChangedReceiver.class.getName());

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Core.getInstance().scheduleForBackgroundExecution(new Runnable() {
            @Override
            public void run() {
                try {
                    if (sLogger.isActivated()) {
                        sLogger.debug("The Locale has changed, we update the RCS strings in Contacts");
                    }
                    /* We have to modify the strings that are used in contacts manager */
                    ContentResolver contentResolver = context.getContentResolver();
                    LocalContentResolver localContentResolver = new LocalContentResolver(context);
                    RcsSettings rcsSettings = RcsSettings.createInstance(localContentResolver);
                    ContactManager contactManager = ContactManager.createInstance(context,
                            contentResolver, localContentResolver, rcsSettings);
                    contactManager.updateStrings();
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error("Unable to handle connection event for intent action : "
                            .concat(intent.getAction()), e);
                }
            }
        });
    }
}
