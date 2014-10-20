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

package com.orangelabs.rcs.ri.sharing.video.media;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Android datagram server connection
 * 
 * @author Jean-Marc AUFFRET
 */
public class AndroidDatagramConnection implements DatagramConnection {
	/**
	 * Datagram connection
	 */
	private DatagramSocket connection = null;

    /**
     * Datagram Packet
     */
    private DatagramPacket packet = null;

    /**
     * Connection timeout
     */
    private int timeout = 0;

	/**
	 * Constructor
	 */
	public AndroidDatagramConnection() {
        packet = new DatagramPacket(new byte[DatagramConnection.DEFAULT_DATAGRAM_SIZE],
                DatagramConnection.DEFAULT_DATAGRAM_SIZE);
	}

    /**
     * Constructor
     *
     * @param timeout SO Timeout
     */
    public AndroidDatagramConnection(int timeout) {
        this();
        this.timeout = timeout;
    }

	/**
	 * Open the datagram connection
	 *
	 * @throws IOException
	 */
	public void open() throws IOException {
		connection = new DatagramSocket();
        connection.setSoTimeout(timeout);
	}

	/**
	 * Open the datagram connection
	 * 
	 * @param port Local port
	 * @throws IOException
	 */
	public void open(int port) throws IOException {
		connection = new DatagramSocket(port);
        connection.setSoTimeout(timeout);
	}

	/**
	 * Close the datagram connection
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		if (connection != null) {
			connection.close();
			connection = null;
		}
	}
	
	/**
	 * Receive data with a specific buffer size
	 *
	 * @return Byte array
	 * @throws IOException
	 */
	public byte[] receive() throws IOException {
		if (connection != null) {
            packet.setLength(DatagramConnection.DEFAULT_DATAGRAM_SIZE);
            connection.receive(packet);

            int packetLength = packet.getLength();
            byte[] data = new byte[packetLength];
            System.arraycopy(packet.getData(), 0, data, 0, packetLength);
			return data;
		} else {
			throw new IOException("Connection not openned");
		}
	}

	/**
	 * Send data
	 * 
	 * @param remoteAddr Remote address
	 * @param remotePort Remote port
	 * @param data Data as byte array
	 * @throws IOException
	 */
	public void send(String remoteAddr, int remotePort, byte[] data) throws IOException {
		if (data == null) {
			return;
		}
		
		if (connection != null) {
			InetAddress address = InetAddress.getByName(remoteAddr);
			DatagramPacket packet = new DatagramPacket(data, data.length, address, remotePort);
			connection.send(packet);
		} else {
			throw new IOException("Connection not openned");
		}
	}
	
	/**
	 * Returns the local address
	 * 
	 * @return Address
	 * @throws IOException
	 */
	public String getLocalAddress() throws IOException {
		if ((connection != null) && (connection.getLocalAddress() != null)) {
			return connection.getLocalAddress().getHostAddress();
		} else {
			throw new IOException("Connection not openned");
		}
	}

	/**
	 * Returns the local port
	 * 
	 * @return Port
	 * @throws IOException
	 */
	public int getLocalPort() throws IOException {
		if (connection != null) {
			return connection.getLocalPort();
		} else {
			throw new IOException("Connection not openned");
		}
	}
}
