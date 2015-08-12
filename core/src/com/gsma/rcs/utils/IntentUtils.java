/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.rcs.utils;

import android.content.Intent;
import android.os.Build;

/**
 * IntentUtils class sets appropriate flags to an intent using reflection
 */
public class IntentUtils {

    /**
     * Using reflection to add FLAG_EXCLUDE_STOPPED_PACKAGES support backward compatibility.
     *
     * @param intent Intent to set flags
     */
    public static void tryToSetExcludeStoppedPackagesFlag(Intent intent) {
        intent.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
    }

    /**
     * Using reflection to add FLAG_RECEIVER_FOREGROUND support backward compatibility.
     *
     * @param intent Intent to set flags
     */
    public static void tryToSetReceiverForegroundFlag(Intent intent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            /*
             * Since FLAG_RECEIVER_FOREGROUND is introduced only from API level
             * JELLY_BEAN_VERSION_CODE we need to do nothing if we are running on a version prior
             * that so we just return then.
             */
            return;
        }
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
    }
}
