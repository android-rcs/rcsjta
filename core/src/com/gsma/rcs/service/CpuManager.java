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

package com.gsma.rcs.service;

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import android.content.Context;
import android.os.PowerManager;

/**
 * CPU manager
 * 
 * @author jexa7410
 */
public class CpuManager {
    /**
     * Power lock
     */
    private PowerManager.WakeLock powerLock = null;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     */
    public CpuManager() {
    }

    /**
     * Init
     */
    public void init() {
        if (RcsSettings.getInstance().isCpuAlwaysOn()) {
            // Activate the always-on procedure even if the device wakes up
            PowerManager pm = (PowerManager) AndroidFactory.getApplicationContext()
                    .getSystemService(Context.POWER_SERVICE);
            powerLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RcsCore");
            powerLock.acquire();
            if (logger.isActivated()) {
                logger.info("Always-on CPU activated");
            }
        }
    }

    /**
     * Stop
     */
    public void close() {
        // Release power manager wave lock
        if (powerLock != null) {
            powerLock.release();
        }
    }
}
