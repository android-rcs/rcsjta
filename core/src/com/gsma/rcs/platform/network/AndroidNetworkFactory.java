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

package com.gsma.rcs.platform.network;

import com.gsma.rcs.core.ims.network.ImsNetworkInterface.DnsResolvedFields;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.InetAddressUtils;
import com.gsma.rcs.utils.IpAddressUtils;

import android.net.ConnectivityManager;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Android network factory
 * 
 * @author jexa7410
 */
public class AndroidNetworkFactory extends NetworkFactory {

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     * 
     * @param rcsSettings
     */
    public AndroidNetworkFactory(RcsSettings rcsSettings) {
        super();
        mRcsSettings = rcsSettings;
    }

    /**
     * Returns the local IP address of a given network interface
     * 
     * @param dnsEntry remote address to find an according local socket address
     * @param type the type of the network interface, should be either
     *            {@link android.net.ConnectivityManager#TYPE_WIFI} or
     *            {@link android.net.ConnectivityManager#TYPE_MOBILE}
     * @return Address
     */
    // Changed by Deutsche Telekom
    public String getLocalIpAddress(DnsResolvedFields dnsEntry, int type) throws SocketException {
        /* What kind of remote address (P-CSCF) are we trying to reach? */
        boolean isIpv4 = dnsEntry != null ? InetAddressUtils.isIPv4Address(dnsEntry.mIpAddress)
                : true;
        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); (en != null)
                && en.hasMoreElements();) {
            NetworkInterface netIntf = en.nextElement();
            for (Enumeration<InetAddress> addr = netIntf.getInetAddresses(); addr.hasMoreElements();) {
                InetAddress inetAddress = addr.nextElement();
                String ipAddress = IpAddressUtils.extractHostAddress(inetAddress.getHostAddress());
                if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()
                        && (InetAddressUtils.isIPv4Address(ipAddress) == isIpv4)) {
                    String intfName = netIntf.getDisplayName().toLowerCase();
                    /* Some devices do list several interfaces though only one is active */
                    if (((type == ConnectivityManager.TYPE_WIFI) && intfName.startsWith("wlan"))
                            || ((type == ConnectivityManager.TYPE_MOBILE) && !intfName
                                    .startsWith("wlan"))) {
                        return ipAddress;
                    }
                }
            }
        }
        throw new SocketException(new StringBuilder("Unable to fetch local ip address for type : ")
                .append(type).append("!").toString());
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
        return new AndroidSecureSocketConnection(mRcsSettings);
    }

    // Changed by Deutsche Telekom
    /**
     * Create a secure socket client connection w/o checking certificates
     * 
     * @param fingerprint
     * @return Socket connection
     */
    public SocketConnection createSimpleSecureSocketClientConnection(String fingerprint) {
        return new AndroidSecureSocketConnection(fingerprint, mRcsSettings);
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
