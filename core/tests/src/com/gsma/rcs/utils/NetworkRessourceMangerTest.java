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

package com.gsma.rcs.utils;

import com.gsma.rcs.RcsSettingsMock;
import com.gsma.rcs.platform.network.NetworkFactory;
import com.gsma.rcs.provider.settings.RcsSettings;

import android.test.AndroidTestCase;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.SocketException;

public class NetworkRessourceMangerTest extends AndroidTestCase {

    private RcsSettings mRcsSettings;

    protected void setUp() throws Exception {
        super.setUp();
        mRcsSettings = RcsSettingsMock.getMockSettings(getContext());
        NetworkFactory.loadFactory("com.gsma.rcs.platform.network.AndroidNetworkFactory",
                mRcsSettings);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        RcsSettingsMock.restoreSettings();
    }

    public void testPortSelection() {
        int updPort = NetworkRessourceManager.generateLocalSipPort(mRcsSettings);
        DatagramSocket udpSocket1 = null;
        try {
            udpSocket1 = new DatagramSocket(updPort);
        } catch (SocketException e) {
            assertNull("Error " + e, e);
        }
        assertNotNull("No UPD socket created at " + updPort, udpSocket1);

        int tcpPort = NetworkRessourceManager.generateLocalSipPort(mRcsSettings);
        ServerSocket tcpSocket1 = null;
        try {
            tcpSocket1 = new ServerSocket(tcpPort);
        } catch (IOException e) {
            assertNull("Error " + e, e);
        }
        assertNotNull("No TCP socket created at " + tcpPort, tcpSocket1);

        int udpTcpPort = NetworkRessourceManager.generateLocalSipPort(mRcsSettings);
        DatagramSocket udpSocket2 = null;
        try {
            udpSocket2 = new DatagramSocket(udpTcpPort);
        } catch (SocketException e) {
            assertNull("Error " + e, e);
        }
        assertNotNull("No UPD socket created at " + udpTcpPort, udpSocket2);

        ServerSocket tcpSocket2 = null;
        try {
            tcpSocket2 = new ServerSocket(udpTcpPort);
        } catch (IOException e) {
            assertNull("Error " + e, e);
        }
        assertNotNull("No TCP socket created at " + udpTcpPort, tcpSocket2);
    }

}
