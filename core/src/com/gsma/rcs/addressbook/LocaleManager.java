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
 *
 ******************************************************************************/

package com.gsma.rcs.addressbook;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.contact.ContactManagerException;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A class to update the displayed strings of the RCS contact address book when the Locale changes
 * 
 * @author yplo6403
 */
public class LocaleManager {

    private final static Logger sLogger = Logger.getLogger(LocaleManager.class.getSimpleName());

    private ExecutorService mUpdateExecutor;
    private final Context mContext;
    private final RcsSettings mRcsSettings;
    private final ContactManager mContactManager;
    private final LocaleUpdater mLocaleUpdater;
    private LocaleChangedReceiver mLocaleChangedReceiver;
    private final Core mCore;

    /**
     * Constructor
     * 
     * @param context The app context
     * @param core The Core instance
     * @param rcsSettings The RCS settings accessor
     * @param contactManager The contact manager
     */
    public LocaleManager(Context context, Core core, RcsSettings rcsSettings,
            ContactManager contactManager) {
        mContext = context;
        mCore = core;
        mRcsSettings = rcsSettings;
        mContactManager = contactManager;
        mLocaleUpdater = new LocaleUpdater();
    }

    /**
     * Starts Locale manager
     */
    public void start() {
        mUpdateExecutor = Executors.newSingleThreadExecutor();
        String displayLanguage = mRcsSettings.getDisplayLanguage();
        if (!displayLanguage.equals(Locale.getDefault().getDisplayLanguage())) {
            mUpdateExecutor.execute(mLocaleUpdater);
        }
        mUpdateExecutor.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    IntentFilter filter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
                    mLocaleChangedReceiver = new LocaleChangedReceiver();
                    mContext.registerReceiver(mLocaleChangedReceiver, filter);
                } catch (RuntimeException e) {
                    /*
                     * Intentionally catch runtime exceptions as else it will abruptly end the
                     * thread and eventually bring the whole system down, which is not intended.
                     */
                    sLogger.error("Failed to listen to locale change!", e);
                }
            }

        });

    }

    /**
     * Stops Locale manager
     */
    public void stop() {
        if (mLocaleChangedReceiver != null) {
            mContext.unregisterReceiver(mLocaleChangedReceiver);
            mLocaleChangedReceiver = null;
        }
        mUpdateExecutor.shutdownNow();
    }

    private class LocaleUpdater implements Runnable {

        @Override
        public void run() {
            try {
                mContactManager.updateStrings();
                mRcsSettings.setDisplayLanguage(Locale.getDefault().getDisplayLanguage());
            } catch (ContactManagerException e) {
                sLogger.error("Failed to update contact strings!", e);
            } catch (RuntimeException e) {
                /*
                 * Normally we are not allowed to catch runtime exceptions as these are genuine bugs
                 * which should be handled/fixed within the code. However the cases when we are
                 * executing operations on a thread unhandling such exceptions will eventually lead
                 * to exit the system and thus can bring the whole system down, which is not
                 * intended.
                 */
                sLogger.error("Failed to update contact strings!", e);
            }
        }

    }

    private class LocaleChangedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            mCore.scheduleForBackgroundExecution(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (sLogger.isActivated()) {
                            sLogger.debug("The Locale has changed, we update the RCS strings in Contacts");
                        }
                        mUpdateExecutor.execute(mLocaleUpdater);
                    } catch (RuntimeException e) {
                        /*
                         * Normally we are not allowed to catch runtime exceptions as these are
                         * genuine bugs which should be handled/fixed within the code. However the
                         * cases when we are executing operations on a thread unhandling such
                         * exceptions will eventually lead to exit the system and thus can bring the
                         * whole system down, which is not intended.
                         */
                        sLogger.error("Failed to update rcs locale for action : ".concat(intent
                                .getAction()), e);
                    }
                }
            });
        }
    }
}
