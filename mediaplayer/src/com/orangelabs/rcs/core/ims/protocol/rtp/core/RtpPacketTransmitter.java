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

package com.orangelabs.rcs.core.ims.protocol.rtp.core;

import java.io.IOException;

import com.orangelabs.rcs.core.ims.protocol.rtp.util.AndroidDatagramConnection;
import com.orangelabs.rcs.core.ims.protocol.rtp.util.Buffer;
import com.orangelabs.rcs.core.ims.protocol.rtp.util.DatagramConnection;
import com.orangelabs.rcs.core.ims.protocol.rtp.util.Packet;

/**
 * RTP packet transmitter
 *
 * @author jexa7410
 */
public class RtpPacketTransmitter {

    /**
     * Sequence number
     */
	private int seqNumber = 0;

    /**
	 * Remote address
	 */
	private String remoteAddress;

    /**
	 * Remote port
	 */
	private int remotePort;

	/**
	 * Statistics
	 */
	private RtpStatisticsTransmitter stats = new RtpStatisticsTransmitter();

	/**
	 * Datagram connection
	 */
	private DatagramConnection datagramConnection = null;

    /**
     * RTCP Session
     */
    private RtcpSession rtcpSession = null;

    /**
     * Constructor
     *
     * @param address Remote address
     * @param port Remote port
     * @param rtcpSession RTCP session
     * @throws IOException
     */
    public RtpPacketTransmitter(String address, int port, RtcpSession rtcpSession)
            throws IOException {
		this.remoteAddress = address;
		this.remotePort = port;
        this.rtcpSession = rtcpSession;
        
        datagramConnection = new AndroidDatagramConnection();
        datagramConnection.open();
	}

    /**
     * Constructor used for symetric RTP
     *
     * @param address Remote address
     * @param port Remote port
     * @param rtcpSession RTCP session
     * @param connection Connection from RTP receiver
     * @throws IOException
     */
    public RtpPacketTransmitter(String address, int port, RtcpSession rtcpSession,
            DatagramConnection connection)
            throws IOException {
        this.remoteAddress = address;
        this.remotePort = port;
        this.rtcpSession = rtcpSession;
        
        if (connection != null) {
            this.datagramConnection = connection;
        } else {
            this.datagramConnection = new AndroidDatagramConnection();
            this.datagramConnection.open();
        }
    }

    /**
     * Close the transmitter
     *
     * @throws IOException
     */
	public void close() throws IOException {
		// Close the datagram connection
		if (datagramConnection != null) {
			datagramConnection.close();
		}
	}

    /**
     * Send a RTP packet
     *
     * @param buffer Input buffer
     * @throws IOException
     */
	public void sendRtpPacket(Buffer buffer) throws IOException {
		// Build a RTP packet
    	RtpPacket packet = buildRtpPacket(buffer);
    	if (packet == null) {
    		return;
    	}

    	// Assemble RTP packet
    	int size = packet.calcLength();
    	packet.assemble(size);

    	// Send the RTP packet to the remote destination
    	transmit(packet);
    }

    /**
     * Build a RTP packet
     *
     * @param buffer Input buffer
     * @return RTP packet
     */
	private RtpPacket buildRtpPacket(Buffer buffer) {
		byte data[] = (byte[])buffer.getData();
		if (data == null) {
			return null;
		}
		Packet packet = new Packet();
		packet.data = data;
		packet.offset = 0;
		packet.length = buffer.getLength();

		RtpPacket rtppacket = new RtpPacket(packet);
		if (buffer.isRTPMarkerSet()) {
			rtppacket.marker = 1;
		} else {
			rtppacket.marker = 0;
		}

		rtppacket.payloadType = buffer.getFormat().getPayload();
		rtppacket.seqnum = seqNumber++;
		rtppacket.timestamp = buffer.getTimeStamp();
        rtppacket.ssrc = rtcpSession.SSRC;
		rtppacket.payloadoffset = buffer.getOffset();
		rtppacket.payloadlength = buffer.getLength();
        if (buffer.getVideoOrientation() != null) {
            rtppacket.extension = true;
            rtppacket.extensionHeader = new RtpExtensionHeader();
            rtppacket.extensionHeader.addElement(
                    buffer.getVideoOrientation().getHeaderId(), 
                    new byte[]{buffer.getVideoOrientation().getVideoOrientation()});
        }
		return rtppacket;
	}

    /**
     * Transmit a RTCP compound packet to the remote destination
     *
     * @param packet RTP packet
     * @throws IOException
     */
	private void transmit(Packet packet) {
		// Prepare data to be sent
		byte[] data = packet.data;
		if (packet.offset > 0) {
			System.arraycopy(data, packet.offset, data = new byte[packet.length], 0, packet.length);
		}

		// Update statistics
		stats.numBytes += packet.length;
		stats.numPackets++;

		// Send data over UDP
		try {
			datagramConnection.send(remoteAddress, remotePort, data);

            RtpSource s = rtcpSession.getMySource();
            s.activeSender = true;
            rtcpSession.timeOfLastRTPSent = rtcpSession.currentTime();
            rtcpSession.packetCount++;
            rtcpSession.octetCount += data.length;
		} catch (IOException e) {
			e.printStackTrace();
        }
    }

    /**
     * Returns the statistics of RTP transmission
     *
     * @return Statistics
     */
	public RtpStatisticsTransmitter getStatistics() {
		return stats;
	}
}
