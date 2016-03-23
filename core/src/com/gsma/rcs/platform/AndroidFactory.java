/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.platform;

import android.content.Context;

import com.gsma.rcs.platform.file.FileFactory;
import com.gsma.rcs.platform.network.NetworkFactory;
import com.gsma.rcs.platform.registry.RegistryFactory;
import com.gsma.rcs.provider.settings.RcsSettings;

/**
 * Android platform
 * 
 * @author jexa7410
 */
public class AndroidFactory {

    /**
     * Android application context
     */
    private static Context mContext;

    /**
     * Returns the application context
     * 
     * @return Context
     */
    public static Context getApplicationContext() {
        return mContext;
    }

    /**
     * Load factory
     * 
     * @param context Context
     * @param rcsSettings
     */
    public static void setApplicationContext(Context context, RcsSettings rcsSettings) {
        mContext = context;
        try {
            NetworkFactory.loadFactory("com.gsma.rcs.platform.network.AndroidNetworkFactory",
                    rcsSettings);
            RegistryFactory.loadFactory("com.gsma.rcs.platform.registry.AndroidRegistryFactory");
            FileFactory.loadFactory("com.gsma.rcs.platform.file.AndroidFileFactory");
        } catch (FactoryException e) {
            throw new IllegalStateException("Failed to load factory instance!", e);
        }
    }
}
