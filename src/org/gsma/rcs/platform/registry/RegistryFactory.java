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
 * Class RegistryFactory.
 */
public abstract class RegistryFactory {
    /**
     * Creates a new instance of RegistryFactory.
     */
    public RegistryFactory() {

    }

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @return  The long.
     */
    public abstract long readLong(String arg1, long arg2);

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     */
    public abstract void writeLong(String arg1, long arg2);

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @return  The boolean.
     */
    public abstract boolean readBoolean(String arg1, boolean arg2);

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     */
    public abstract void writeBoolean(String arg1, boolean arg2);

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @return  The string.
     */
    public abstract String readString(String arg1, String arg2);

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     */
    public abstract void writeString(String arg1, String arg2);

    /**
     * Removes a parameter.
     *  
     * @param arg1 The arg1.
     */
    public abstract void removeParameter(String arg1);

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @return  The int.
     */
    public abstract int readInteger(String arg1, int arg2);

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     */
    public abstract void writeInteger(String arg1, int arg2);

    /**
     * Returns the factory.
     *  
     * @return  The factory.
     */
    public static RegistryFactory getFactory() {
        return (RegistryFactory) null;
    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public static void loadFactory(String arg1) throws org.gsma.rcs.platform.FactoryException {

    }

} // end RegistryFactory
