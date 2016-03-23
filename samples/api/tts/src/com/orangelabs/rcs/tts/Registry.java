/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.orangelabs.rcs.tts;

import android.content.SharedPreferences;

/**
 * Application registry
 * 
 * @author jexa7410
 */
public class Registry {
    /**
     * Registry name
     */
    public static final String REGISTRY = "RcsTTS";

    /**
     * Registry key names
     */
    public static final String ACTIVATE_TTS = "Activated";

    /**
     * Read a string value in the registry
     * 
     * @param preferences Preferences
     * @param key Key name to be read
     * @param defaultValue Default value
     * @return String
     */
    public static String readString(SharedPreferences preferences, String key, String defaultValue) {
        return preferences.getString(key, defaultValue);
    }

    /**
     * Write a string value in the registry
     * 
     * @param preferences Preferences
     * @param key Key name to be updated
     * @param value New value
     */
    public static void writeString(SharedPreferences preferences, String key, String value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    /**
     * Read a boolean value in the registry
     * 
     * @param preferences Preferences
     * @param key Key name to be read
     * @param defaultValue Default value
     * @return Boolean
     */
    public static boolean readBoolean(SharedPreferences preferences, String key,
            boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }

    /**
     * Write a boolean value in the registry
     * 
     * @param preferences Preferences
     * @param key Key name to be updated
     * @param value New value
     */
    public static void writeBoolean(SharedPreferences preferences, String key, boolean value) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }
}
