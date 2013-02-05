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

package org.gsma.rcs.utils;

/**
 * Class NetworkRessourceManager.
 */
public class NetworkRessourceManager {
    /**
     * Constant DEFAULT_LOCAL_SIP_PORT_BASE.
     */
    public static final int DEFAULT_LOCAL_SIP_PORT_BASE = 0;

    /**
     * Constant DEFAULT_LOCAL_RTP_PORT_BASE.
     */
    public static final int DEFAULT_LOCAL_RTP_PORT_BASE = 0;

    /**
     * Constant DEFAULT_LOCAL_MSRP_PORT_BASE.
     */
    public static final int DEFAULT_LOCAL_MSRP_PORT_BASE = 0;

    /**
     * Creates a new instance of NetworkRessourceManager.
     */
    public NetworkRessourceManager() {

    }

    /**
     *  
     * @return  The int.
     */
    public static synchronized int generateLocalRtpPort() {
        return 0;
    }

    /**
     *  
     * @return  The int.
     */
    public static synchronized int generateLocalSipPort() {
        return 0;
    }

    /**
     *  
     * @return  The int.
     */
    public static synchronized int generateLocalMsrpPort() {
        return 0;
    }

    /**
     *  
     * @param arg1 The arg1.
     * @return  The boolean.
     */
    public static boolean isValidIpAddress(String arg1) {
        return false;
    }

    /**
     *  
     * @param arg1 The arg1.
     * @return  The int.
     */
    public static int ipToInt(String arg1) {
        return 0;
    }

} // end NetworkRessourceManager
