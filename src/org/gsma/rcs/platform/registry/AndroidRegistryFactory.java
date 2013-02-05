/*
 * Copyright 2013, France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.rcs.platform.registry;

/**
 * Class AndroidRegistryFactory.
 */
public class AndroidRegistryFactory extends RegistryFactory {
    /**
     *  
     * @param key The key.
     * @param defaultValue The default value.
     * @return  The long.
     */
    public long readLong(String key, long defaultValue) {
        return (long) 0;
    }

    /**
     *  
     * @param key The key.
     * @param value The value.
     */
    public void writeLong(String key, long value) {

    }

    /**
     *  
     * @param key The key.
     * @param defaultValue The default value.
     * @return  The boolean.
     */
    public boolean readBoolean(String key, boolean defaultValue) {
        return true;
    }

    /**
     *  
     * @param key The key.
     * @param value The value.
     */
    public void writeBoolean(String key, boolean value) {

    }

    /**
     *  
     * @param key The key.
     * @param defaultValue The default value.
     * @return  The string.
     */
    public String readString(String key, String defaultValue) {
        return (String) null;
    }

    /**
     *  
     * @param key The key.
     * @param value The value.
     */
    public void writeString(String key, String value) {

    }

    /**
     * Removes a parameter.
     *  
     * @param key The key.
     */
    public void removeParameter(String key) {

    }

    /**
     *  
     * @param key The key.
     * @param value The value.
     */
    public void writeInteger(String key, int value) {

    }

    /**
     *  
     * @param key The key.
     * @param defaultValue The default value.
     * @return  The int.
     */
    public int readInteger(String key, int defaultValue) {
        return (int) 0;
    }

} // end AndroidRegistryFactory
