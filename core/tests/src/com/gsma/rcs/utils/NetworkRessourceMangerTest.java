
package com.gsma.rcs.utils;

import android.test.AndroidTestCase;

import com.gsma.rcs.platform.network.NetworkFactory;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.LocalContentResolver;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.SocketException;

/**
 * Created by andreas on 10.11.14.
 */
public class NetworkRessourceMangerTest extends AndroidTestCase {

    private RcsSettings mRcsSettings;

    protected void setUp() throws Exception {
        super.setUp();
        LocalContentResolver localContentResolver = new LocalContentResolver(getContext());
        mRcsSettings = RcsSettings.getInstance(localContentResolver);
        NetworkFactory.loadFactory("com.gsma.rcs.platform.network.AndroidNetworkFactory",
                mRcsSettings);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
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
