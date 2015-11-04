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

package com.orangelabs.rcs.core.control;

import com.gsma.services.rcs.RcsServiceControl;

import com.orangelabs.rcs.api.connection.ConnectionManager;
import com.orangelabs.rcs.api.connection.ConnectionManager.RcsServiceName;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

/**
 * This subclass of Application allows to get a resource content from a static context
 * 
 * @author Philippe LEMORDANT
 */
public class CoreControlApplication extends Application {

    /**
     * Delay (ms) before starting connection manager.
     */
    public static final long DELAY_FOR_STARTING_CNX_MANAGER = 2000;

    public static boolean sCnxManagerStarted = false;

    private static RcsServiceControl mRcsServiceControl;

    @Override
    public void onCreate() {
        super.onCreate();
        Context context = getApplicationContext();
        mRcsServiceControl = RcsServiceControl.getInstance(context);

        final ConnectionManager cnxManager = ConnectionManager.createInstance(context,
                mRcsServiceControl, RcsServiceName.FILE_TRANSFER, RcsServiceName.CHAT,
                RcsServiceName.CONTACT);

        /* Do not execute the ConnectionManager on the main thread */
        Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        mainThreadHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                cnxManager.start();
                sCnxManagerStarted = true;
            }
        }, DELAY_FOR_STARTING_CNX_MANAGER);
    }

    /**
     * Gets the RCS service control singleton
     * 
     * @return the RCS service control singleton
     */
    public static RcsServiceControl getRcsServiceControl() {
        return mRcsServiceControl;
    }

}
