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

package com.orangelabs.rcs.core.ims.protocol.rtp.util;

import java.io.IOException;


/**
 * Network ressource manager
 *
 * @author Jean-Marc AUFFRET
 */
public class NetworkRessourceManager {
    /**
     * Default RTP port base
     */
    public static final int DEFAULT_LOCAL_RTP_PORT_BASE = 5000;

    /**
     * Generate a default free RTP port number
     *
     * @return Local RTP port
     */
    public static synchronized int generateLocalRtpPort() {
    	return generateLocalUdpPort(DEFAULT_LOCAL_RTP_PORT_BASE);
    }

    /**
     * Generate a free UDP port number from a specific port base
     *
     * @param portBase UDP port base
     * @return Local UDP port
     */
    private static int generateLocalUdpPort(int portBase) {
    	int resp = -1;
		int port = portBase;
		while((resp == -1) && (port < Integer.MAX_VALUE)) {
			if (isLocalUdpPortFree(port)) {
				// Free UDP port found
				resp = port;
			} else {
                // +2 needed for RTCP port
                port += 2;
			}
		}
    	return resp;
    }

	/**
     * Test if the given local UDP port is really free (not used by
     * other applications)
     *
     * @param port Port to check
     * @return Boolean
     */
    private static boolean isLocalUdpPortFree(int port) {
    	boolean res = false;
    	try {
    		DatagramConnection conn = NetworkRessourceManager.createDatagramConnection();
    		conn.open(port);
            conn.close();
    		res = true;
    	} catch(IOException e) {
    		res = false;
    	}
    	return res;
    }
    
    /**
     * Create a datagram connection
     * 
     * @return Datagram connection
     */
	public static DatagramConnection createDatagramConnection() {
		return new AndroidDatagramConnection();
	}

    /**
     * Create a datagram connection with a specific SO timeout
     *
     * @param timeout SO timeout
     * @return Datagram connection
     */
    public static DatagramConnection createDatagramConnection(int timeout) {
        return new AndroidDatagramConnection(timeout);
    }    
}
