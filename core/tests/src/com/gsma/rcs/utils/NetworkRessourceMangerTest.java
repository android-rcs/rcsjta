
package com.gsma.rcs.utils;

import android.test.AndroidTestCase;

import com.gsma.rcs.platform.network.NetworkFactory;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.NetworkRessourceManager;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.SocketException;

/**
 * Created by andreas on 10.11.14.
 */
public class NetworkRessourceMangerTest extends AndroidTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        RcsSettings.createInstance(getContext());
        NetworkFactory.loadFactory("com.gsma.rcs.platform.network.AndroidNetworkFactory");
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testPortSelection() {
        int updPort = NetworkRessourceManager.generateLocalSipPort();
        DatagramSocket udpSocket1 = null;
        try {
            udpSocket1 = new DatagramSocket(updPort);
        } catch (SocketException e) {
            assertNull("Error " + e, e);
        }
        assertNotNull("No UPD socket created at " + updPort, udpSocket1);

        int tcpPort = NetworkRessourceManager.generateLocalSipPort();
        ServerSocket tcpSocket1 = null;
        try {
            tcpSocket1 = new ServerSocket(tcpPort);
        } catch (IOException e) {
            assertNull("Error " + e, e);
        }
        assertNotNull("No TCP socket created at " + tcpPort, tcpSocket1);

        int udpTcpPort = NetworkRessourceManager.generateLocalSipPort();
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
