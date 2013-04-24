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

package com.orangelabs.rcs.platform.network;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import android.net.ConnectivityManager;

import com.orangelabs.rcs.utils.IpAddressUtils;

/**
 * Android network factory
 * 
 * @author jexa7410
 */
public class AndroidNetworkFactory extends NetworkFactory {

	/**
	 * Returns the local IP address of a given network interface
	 * 
	 * @param type Network interface type
	 * @return Address
	 */
	public String getLocalIpAddress(int type) {
		String ipAddress = null;
		try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); (en != null) && en.hasMoreElements();) {
	            NetworkInterface netIntf = (NetworkInterface)en.nextElement();
	            for (Enumeration<InetAddress> addr = netIntf.getInetAddresses(); addr.hasMoreElements();) {
	                InetAddress inetAddress = (InetAddress)addr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()) {
                    	ipAddress = IpAddressUtils.extractHostAddress(inetAddress.getHostAddress());
                    	String intfName = netIntf.getDisplayName().toLowerCase();
                    	if ((type == ConnectivityManager.TYPE_WIFI) && intfName.startsWith("wlan")) {
                            return ipAddress;
                       } else
                       if ((type == ConnectivityManager.TYPE_MOBILE) && !intfName.startsWith("wlan")) {
                            return ipAddress;
                       }
                    }
	            }
	        }
	        return ipAddress;
		} catch(Exception e) {
			return ipAddress;
		}
	}

    /**
     * Create a datagram connection
     * 
     * @return Datagram connection
     */
	public DatagramConnection createDatagramConnection() {
		return new AndroidDatagramConnection();
	}

    /**
     * Create a datagram connection with a specific SO timeout
     *
     * @param timeout SO timeout
     * @return Datagram connection
     */
    public DatagramConnection createDatagramConnection(int timeout) {
        return new AndroidDatagramConnection(timeout);
    }

    /**
     * Create a socket client connection
     * 
     * @return Socket connection
     */
	public SocketConnection createSocketClientConnection() {
		return new AndroidSocketConnection();
	}

	/**
	 * Create a secure socket client connection
	 * 
	 * @return Socket connection
	 */
	public SocketConnection createSecureSocketClientConnection() {
		return new AndroidSecureSocketConnection();
	}
	
    /**
     * Create a socket server connection
     * 
     * @return Socket server connection
     */
	public SocketServerConnection createSocketServerConnection() {
		return new AndroidSocketServerConnection();
	}

    /**
     * Create an HTTP connection
     * 
     * @return HTTP connection
     */
	public HttpConnection createHttpConnection() {
		return new AndroidHttpConnection();
	}
}
