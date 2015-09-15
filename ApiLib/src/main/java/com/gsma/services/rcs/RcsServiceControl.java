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
import android.util.Log;

/**
 * A utility class to control the activation of the RCS service.
 */
public class RcsServiceControl {

    /**
     * RCS stack package name
     */
    public final static String RCS_STACK_PACKAGENAME = "com.gsma.rcs";

    private static final String TIME_SPENT = "dur";

    /**
     * Singleton of RcsServiceControl
     */
    private static volatile RcsServiceControl sInstance;

    private final Handler mHandler;
    private final Context mContext;

    private static final long INTENT_RESPONSE_TIMEOUT = 2000;

    private final static String LOG_TAG = "[RCS][" + RcsServiceControl.class.getSimpleName() + "]";

    private RcsServiceControl(Context ctx) {
        HandlerThread handlerThread = new HandlerThread(getClass().getSimpleName());
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
        mContext = ctx;
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
                sInstance = new RcsServiceControl(ctx.getApplicationContext());
            }
        }
        return sInstance;
    }

    /**
     * IntentUtils class sets appropriate flags to an intent using reflection
     */
    private static class IntentUtils {

        /**
         * Using reflection to add FLAG_EXCLUDE_STOPPED_PACKAGES support backward compatibility.
         *
         * @param intent Intent to set flags
         */
        private static void tryToSetExcludeStoppedPackagesFlag(Intent intent) {
            intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
        }

        /**
         * Using reflection to add FLAG_RECEIVER_FOREGROUND support backward compatibility.
         *
         * @param intent Intent to set flags
         */
        private static void tryToSetReceiverForegroundFlag(Intent intent) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                /*
                 * Since FLAG_RECEIVER_FOREGROUND is introduced only from API level
                 * JELLY_BEAN_VERSION_CODE we need to do nothing if we are running on a version
                 * prior that so we just return then.
                 */
                return;
            }
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }
    }

    private Intent generateIsCompatibeIntent(String service) {
        Intent intent = new Intent(Intents.Service.ACTION_GET_COMPATIBILITY);
        intent.putExtra(Intents.Service.EXTRA_GET_COMPATIBILITY_SERVICE, service);
        intent.putExtra(Intents.Service.EXTRA_GET_COMPATIBILITY_CODENAME,
                RcsService.Build.API_CODENAME);
        intent.putExtra(Intents.Service.EXTRA_GET_COMPATIBILITY_VERSION,
                RcsService.Build.API_VERSION);
        intent.putExtra(Intents.Service.EXTRA_GET_COMPATIBILITY_INCREMENT,
                RcsService.Build.API_INCREMENTAL);
        return intent;
    }

    private Intent generateSetActivationModeIntent(boolean active) {
        Intent intent = new Intent(Intents.Service.ACTION_SET_ACTIVATION_MODE);
        intent.putExtra(Intents.Service.EXTRA_SET_ACTIVATION_MODE, active);
        return intent;
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
    private Bundle queryRcsStackByIntent(Intent intent) throws RcsGenericException {
        final String action = intent.getAction();
        final SyncBroadcastReceiver broadcastReceiver = new SyncBroadcastReceiver();
        intent.setPackage(RCS_STACK_PACKAGENAME);
        trySetIntentForActivePackageAndReceiverInForeground(intent);
        synchronized (sInstance) {
            synchronized (broadcastReceiver) {
                mContext.sendOrderedBroadcast(intent, null, broadcastReceiver, mHandler,
                        Activity.RESULT_OK, null, null);
                long endTime = System.currentTimeMillis() + INTENT_RESPONSE_TIMEOUT;
                while (!broadcastReceiver.mHaveResult) {
                    long delay = endTime - System.currentTimeMillis();
                    if (delay <= 0) {
                        Log.w(LOG_TAG, "Waiting for result for " + action
                                + " has reached deadline!");
                        break;
                    }
                    try {
                        Log.d(LOG_TAG, "Waiting for result for " + action + " during max " + delay
                                + "ms");
                        broadcastReceiver.wait(delay);

                    } catch (InterruptedException e) {
                        Log.w(LOG_TAG, "Waiting for result for " + action + " was interrupted!");
                    }
                }
                Bundle result = broadcastReceiver.getResultExtras(false);
                if (result == null) {
                    throw new RcsGenericException("Failed to get result for " + action + "!");
                }
                result.putLong(TIME_SPENT, System.currentTimeMillis() - endTime
                        + INTENT_RESPONSE_TIMEOUT);
                return result;
            }
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
        Log.d(LOG_TAG, "isActivationModeChangeable: Request()");
        Bundle result = queryRcsStackByIntent(new Intent(
                Intents.Service.ACTION_GET_ACTIVATION_MODE_CHANGEABLE));
        boolean activationModeChangeable = result.getBoolean(
                Intents.Service.EXTRA_GET_ACTIVATION_MODE_CHANGEABLE, false);
        Log.d(LOG_TAG, "isActivationModeChangeable: Response() = " + activationModeChangeable
                + " (in " + result.getLong(TIME_SPENT, -1) + "ms)");
        return activationModeChangeable;
    }

    /**
     * Returns true if the RCS stack is marked as active on the device.
     *
     * @return boolean true if the RCS stack is marked as active on the device.
     * @throws RcsGenericException
     */
    public boolean isActivated() throws RcsGenericException {
        Log.d(LOG_TAG, "isActivated: Request()");
        Bundle result = queryRcsStackByIntent(new Intent(Intents.Service.ACTION_GET_ACTIVATION_MODE));
        boolean activated = result.getBoolean(Intents.Service.EXTRA_GET_ACTIVATION_MODE, false);
        Log.d(LOG_TAG,
                "isActivated: Response() -> " + activated + " (in "
                        + result.getLong(TIME_SPENT, -1) + "ms)");
        return activated;
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
        Log.d(LOG_TAG, "setActivationMode: Request(" + active + ")");
        Bundle result = queryRcsStackByIntent(generateSetActivationModeIntent(active));
        boolean activationMode = result
                .getBoolean(Intents.Service.EXTRA_SET_ACTIVATION_MODE, false);
        Log.d(LOG_TAG, "setActivationMode: Response(" + active + ") -> " + activationMode + " (in "
                + result.getLong(TIME_SPENT, -1) + "ms)");
        if (active != activationMode) {
            throw new RcsPermissionDeniedException("Stack activation mode is not changeable!");
        }
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
        Log.d(LOG_TAG, "isCompatible: Request(" + serviceName + ")");
        Bundle result = queryRcsStackByIntent(generateIsCompatibeIntent(serviceName));
        boolean compatible = result.getBoolean(Intents.Service.EXTRA_GET_COMPATIBILITY_RESPONSE,
                false);
        Log.d(LOG_TAG, "isCompatible: Response(" + serviceName + ") -> " + compatible + " (in "
                + result.getLong(TIME_SPENT, -1) + "ms)");
        return compatible;
    }

    /**
     * Returns true if the RCS stack is started.
     *
     * @return boolean true if the RCS stack is started.
     * @throws RcsGenericException
     */
    public boolean isServiceStarted() throws RcsGenericException {
        Log.d(LOG_TAG, "isServiceStarted: Request()");
        Bundle result = queryRcsStackByIntent(new Intent(
                Intents.Service.ACTION_GET_SERVICE_STARTING_STATE));
        boolean started = result
                .getBoolean(Intents.Service.EXTRA_GET_SERVICE_STARTING_STATE, false);
        Log.d(LOG_TAG,
                "isServiceStarted: Response() -> " + started + " (in "
                        + result.getLong(TIME_SPENT, -1) + "ms)");
        return started;
    }
}
