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

package com.gsma.services.rcs;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * A utility class to control the activation of the RCS service.
 */
public class RcsServiceControl {

    /**
     * RCS stack package name
     */
    public final static String RCS_STACK_PACKAGENAME = "com.gsma.rcs";

    /**
     * Singleton of RcsServiceControl
     */
    private static volatile RcsServiceControl sInstance;

    private final Context mContext;

    private final Handler mHandler;

    private static final long INTENT_RESPONSE_TIMEOUT = 2000;

    private final static String LOG_TAG = "[RCS][" + RcsServiceControl.class.getSimpleName() + "]";

    private RcsServiceControl(Context ctx) {
        mContext = ctx;
        // Create a dedicate handler to schedule the Broadcast onReceiver callback
        HandlerThread handlerThread = new HandlerThread("ht");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        mHandler = new Handler(looper);
    }

    /**
     * Gets an instance of RcsServiceControl
     * 
     * @param ctx the context.
     * @return RcsServiceControl the singleton instance.
     */
    public static RcsServiceControl getInstance(Context ctx) {
        if (sInstance != null) {
            return sInstance;

        }
        synchronized (RcsServiceControl.class) {
            if (sInstance == null) {
                if (ctx == null) {
                    throw new IllegalArgumentException("Context is null");

                }
                sInstance = new RcsServiceControl(ctx);
            }
        }
        return sInstance;
    }

    /**
     * IntentUtils class sets appropriate flags to an intent using reflection
     */
    static class IntentUtils {

        private static final int HONEYCOMB_MR1_VERSION_CODE = 12;

        private static final int JELLY_BEAN_VERSION_CODE = 16;

        private static final String ADD_FLAGS_METHOD_NAME = "addFlags";

        private static final Class<?>[] ADD_FLAGS_PARAM = new Class[] {
            int.class
        };

        private static final String FLAG_EXCLUDE_STOPPED_PACKAGES = "FLAG_EXCLUDE_STOPPED_PACKAGES";

        private static final String FLAG_RECEIVER_FOREGROUND = "FLAG_RECEIVER_FOREGROUND";

        /**
         * Using reflection to add FLAG_EXCLUDE_STOPPED_PACKAGES support backward compatibility.
         * 
         * @param intent Intent to set flags
         */
        static void tryToSetExcludeStoppedPackagesFlag(Intent intent) {

            if (Build.VERSION.SDK_INT < HONEYCOMB_MR1_VERSION_CODE) {
                /*
                 * Since FLAG_EXCLUDE_STOPPED_PACKAGES is introduced only from API level
                 * HONEYCOMB_MR1_VERSION_CODE we need to do nothing if we are running on a version
                 * prior that so we just return then.
                 */
                return;
            }

            try {
                Method addflagsMethod = intent.getClass().getDeclaredMethod(ADD_FLAGS_METHOD_NAME,
                        ADD_FLAGS_PARAM);
                Field flagExcludeStoppedPackages = intent.getClass().getDeclaredField(
                        FLAG_EXCLUDE_STOPPED_PACKAGES);
                addflagsMethod.invoke(intent, flagExcludeStoppedPackages.getInt(IntentUtils.class));
            } catch (Exception e) {
                // Do nothing
            }
        }

        /**
         * Using reflection to add FLAG_RECEIVER_FOREGROUND support backward compatibility.
         * 
         * @param intent Intent to set flags
         */
        static void tryToSetReceiverForegroundFlag(Intent intent) {

            if (Build.VERSION.SDK_INT < JELLY_BEAN_VERSION_CODE) {
                /*
                 * Since FLAG_RECEIVER_FOREGROUND is introduced only from API level
                 * JELLY_BEAN_VERSION_CODE we need to do nothing if we are running on a version
                 * prior that so we just return then.
                 */
                return;
            }

            try {
                Method addflagsMethod = intent.getClass().getDeclaredMethod(ADD_FLAGS_METHOD_NAME,
                        ADD_FLAGS_PARAM);
                Field flagReceiverForeground = intent.getClass().getDeclaredField(
                        FLAG_RECEIVER_FOREGROUND);
                addflagsMethod.invoke(intent, flagReceiverForeground.getInt(IntentUtils.class));
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    private class SyncBroadcastReceiver extends BroadcastReceiver {
        public volatile boolean mHaveResult = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (this) {
                mHaveResult = true;
                notify();
            }
        }
    }

    /**
     * Query RCS stack by sending broadcast intent.
     * 
     * @param action of the intent
     * @return the result extra data bundle or null if no response is received due to timeout
     * @throws RcsGenericException raised if timeout
     */
    private Bundle queryRcsStackByIntent(String action) throws RcsGenericException {
        return queryRcsStackByIntent(new Intent(action));
    }

    private Bundle queryRcsStackByIntent(Intent intent) throws RcsGenericException {
        final SyncBroadcastReceiver broadcastReceiver = new SyncBroadcastReceiver();
        final Intent broadcastIntent = intent.setPackage(RCS_STACK_PACKAGENAME);

        /*
         * Update flags of the broadcast intent to increase performance
         */
        trySetIntentForActivePackageAndReceiverInForeground(broadcastIntent);

        synchronized (broadcastReceiver) {
            mContext.sendOrderedBroadcast(broadcastIntent, null, broadcastReceiver, mHandler,
                    Activity.RESULT_OK, null, null);

            long endTime = System.currentTimeMillis() + INTENT_RESPONSE_TIMEOUT;

            while (!broadcastReceiver.mHaveResult) {
                long delay = endTime - System.currentTimeMillis();
                if (delay <= 0) {
                    if (!broadcastReceiver.mHaveResult) {
                        throw new RcsGenericException(
                                "No response to broadcast intent ".concat(intent.getAction()));
                    }
                    break;
                }

                try {
                    /* Wait to receive callback response */
                    broadcastReceiver.wait(delay);
                } catch (InterruptedException e) {
                    /* do nothing */
                }
            }
            return broadcastReceiver.getResultExtras(false);
        }
    }

    /**
     * Update flags of the broadcast intent to increase performance
     * 
     * @param intent
     */
    private void trySetIntentForActivePackageAndReceiverInForeground(Intent intent) {
        IntentUtils.tryToSetExcludeStoppedPackagesFlag(intent);
        IntentUtils.tryToSetReceiverForegroundFlag(intent);
    }

    /**
     * Returns true if the RCS stack is installed and not disabled on the device.
     * 
     * @return boolean true if the RCS stack is installed and not disabled on the device.
     */
    public boolean isAvailable() {
        try {
            ApplicationInfo appInfo = mContext.getPackageManager().getApplicationInfo(
                    RCS_STACK_PACKAGENAME, 0);
            return (appInfo.flags & ApplicationInfo.FLAG_STOPPED) == 0;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Returns true if the RCS stack de-activation/activation is allowed by the client.
     * 
     * @return boolean true if the RCS stack de-activation/activation is allowed by the client.
     * @throws RcsGenericException
     */
    public boolean isActivationModeChangeable() throws RcsGenericException {
        Log.d(LOG_TAG, "Query activation mode changeable");
        Bundle resultExtraData = queryRcsStackByIntent(Intents.Service.ACTION_GET_ACTIVATION_MODE_CHANGEABLE);
        if (resultExtraData == null) {
            // No response
            throw new RcsGenericException("Failed to read stack activation mode changeable!");
        }
        return resultExtraData.getBoolean(Intents.Service.EXTRA_GET_ACTIVATION_MODE_CHANGEABLE,
                false);
    }

    /**
     * Returns true if the RCS stack is marked as active on the device.
     * 
     * @return boolean true if the RCS stack is marked as active on the device.
     * @throws RcsGenericException
     */
    public boolean isActivated() throws RcsGenericException {
        Log.d(LOG_TAG, "Query activation mode");
        Bundle resultExtraData = queryRcsStackByIntent(Intents.Service.ACTION_GET_ACTIVATION_MODE);
        if (resultExtraData == null) {
            // No response
            throw new RcsGenericException("Failed to read stack activation mode!");
        }
        return resultExtraData.getBoolean(Intents.Service.EXTRA_GET_ACTIVATION_MODE, false);
    }

    /**
     * Deactive/Activate the RCS stack in case these operations are allowed (see
     * isStackActivationStatusChangeable) or else throws an RcsPermissionDeniedException.
     * 
     * @param active True is activation is enabled.
     * @throws RcsPermissionDeniedException
     * @throws RcsGenericException
     */
    public void setActivationMode(boolean active) throws RcsPermissionDeniedException,
            RcsGenericException {
        Log.d(LOG_TAG, "Query activation mode changeable");
        Bundle resultExtraData = queryRcsStackByIntent(Intents.Service.ACTION_GET_ACTIVATION_MODE_CHANGEABLE);
        if (resultExtraData == null) {
            // No response to check if change is allowed
            throw new RcsPermissionDeniedException("Failed to set stack activation mode!");
        }
        boolean activationChangeable = resultExtraData.getBoolean(
                Intents.Service.EXTRA_GET_ACTIVATION_MODE_CHANGEABLE, false);
        if (!activationChangeable) {
            throw new RcsPermissionDeniedException("Stack activation mode not changeable");
        }
        final Intent broadcastIntent = new Intent(Intents.Service.ACTION_SET_ACTIVATION_MODE);
        broadcastIntent.setPackage(RCS_STACK_PACKAGENAME);
        broadcastIntent.putExtra(Intents.Service.EXTRA_SET_ACTIVATION_MODE, active);

        /* Update flags of the broadcast intent to increase performance */
        trySetIntentForActivePackageAndReceiverInForeground(broadcastIntent);

        Log.d(LOG_TAG, "Set activation mode ".concat(Boolean.toString(active)));
        mContext.sendBroadcast(broadcastIntent);
    }

    /**
     * Returns true if the client RCS API and core RCS stack are compatible for the given service.
     * 
     * @param service the RCS service
     * @return boolean true if the client RCS stack and RCS API are compatible for the given
     *         service.
     * @throws RcsGenericException
     * @hide
     */
    public boolean isCompatible(RcsService service) throws RcsGenericException {
        String serviceName = service.getClass().getSimpleName();
        Intent intent = new Intent(Intents.Service.ACTION_GET_COMPATIBILITY);
        intent.putExtra(Intents.Service.EXTRA_GET_COMPATIBILITY_SERVICE, serviceName);
        intent.putExtra(Intents.Service.EXTRA_GET_COMPATIBILITY_CODENAME,
                RcsService.Build.API_CODENAME);
        intent.putExtra(Intents.Service.EXTRA_GET_COMPATIBILITY_VERSION,
                RcsService.Build.API_VERSION);
        intent.putExtra(Intents.Service.EXTRA_GET_COMPATIBILITY_INCREMENT,
                RcsService.Build.API_INCREMENTAL);
        Log.d(LOG_TAG, "Query compatibility for service ".concat(serviceName));
        Bundle resultExtraData = queryRcsStackByIntent(intent);
        if (resultExtraData == null) {
            // No response
            throw new RcsGenericException(
                    "Failed to check client RCS API compatibility for service " + serviceName
                            + " !");
        }
        return resultExtraData.getBoolean(Intents.Service.EXTRA_GET_COMPATIBILITY_RESPONSE, false);
    }

    /**
     * Returns true if the RCS stack is started.
     * 
     * @return boolean true if the RCS stack is started.
     * @throws RcsGenericException
     */
    public boolean isServiceStarted() throws RcsGenericException {
        Log.d(LOG_TAG, "Query service started");
        Bundle resultExtraData = queryRcsStackByIntent(Intents.Service.ACTION_GET_SERVICE_STARTING_STATE);
        if (resultExtraData == null) {
            // No response
            throw new RcsGenericException("Failed to query service started!");
        }
        return resultExtraData.getBoolean(Intents.Service.EXTRA_GET_SERVICE_STARTING_STATE, false);
    }
}
