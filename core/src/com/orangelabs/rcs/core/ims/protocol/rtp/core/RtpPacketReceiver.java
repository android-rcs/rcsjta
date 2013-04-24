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
import java.net.SocketTimeoutException;

import com.orangelabs.rcs.platform.network.DatagramConnection;
import com.orangelabs.rcs.platform.network.NetworkFactory;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * RTP packet receiver
 *
 * @author jexa7410
 */
public class RtpPacketReceiver {
    /**
     * Statistics
     */
	private RtpStatisticsReceiver stats = new RtpStatisticsReceiver();

	/**
     * Buffer size needed to received RTP packet
     */
	private int bufferSize = 64000;

	/**
	 * Datagram connection
	 */
    public DatagramConnection datagramConnection = null;

    /**
     * RTCP Session
     */
    private RtcpSession rtcpSession = null;

    /**
     * Signals that connection is closed
     */
    private boolean isClosed = false;

	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     *
     * @param port Listenning port
     * @param rtcpSession
     * @param socketTimeout
     * @throws IOException
     */
    public RtpPacketReceiver(int port, RtcpSession rtcpSession, int socketTimeout) throws IOException {
        this.rtcpSession = rtcpSession;
        // Create the UDP server
        datagramConnection = NetworkFactory.getFactory().createDatagramConnection(socketTimeout);
        datagramConnection.open(port);
		if (logger.isActivated()) {
            logger.debug("RTP receiver created on port " + port);
		}
	}

    /**
     * Constructor
     *
     * @param port Listenning port
     * @param rtcpSession
     * @throws IOException
     */
    public RtpPacketReceiver(int port, RtcpSession rtcpSession) throws IOException {
        this(port, rtcpSession, 0);
    }

	/**
	 * Close the receiver
	 */
	public void close() {
		// Close the datagram connection
		if (datagramConnection != null) {
			try {
                isClosed = true;
				datagramConnection.close();
			} catch(Exception e) {
				if (logger.isActivated()) {
					logger.warn("Can't close correctly the datagram connection");
				}
			}
			datagramConnection = null;
		}

	}

    /**
     * Read a RTP packet (blocking method)
     *
     * @return RTP packet
     */
    public RtpPacket readRtpPacket() throws SocketTimeoutException {
		try {
			// Wait a new packet
            byte[] data = datagramConnection.receive(bufferSize);

			// Parse the RTP packet
			RtpPacket pkt = parseRtpPacket(data);

			// Drop the keep-alive packets
			if ((pkt != null) && (pkt.payloadType != 20)) {
				// Update statistics
				stats.numPackets++;
                stats.numBytes += data.length;

                RtpSource s = rtcpSession.getMySource();
                s.setSsrc(pkt.ssrc);
                s.activeSender = true;
                s.receiveRtpPacket(pkt);
                pkt.seqnum = s.generateExtendedSequenceNumber(pkt.seqnum);

				return pkt;
			} else {
				return readRtpPacket();
			}

        } catch (SocketTimeoutException ex) {
            throw ex;
		} catch (Exception e) {
            if (!isClosed) {
                if (logger.isActivated()) {
                    logger.error("Can't parse the RTP packet", e);
                }
                stats.numBadRtpPkts++;
            }
			return null;
		}
	}

    /**
     * Parse the RTP packet
     *
     * @param data RTP packet not yet parsed
     * @return RTP packet
     */
	private RtpPacket parseRtpPacket(byte[] data) {
		RtpPacket packet = new RtpPacket();
		try {
			// Read RTP packet length
            packet.length = data.length;

            // Set received timestamp
            packet.receivedAt = System.currentTimeMillis();

            // Read extension bit
            packet.extension = (data[0] & 0x10) > 0;

			// Read marker
			if ((byte)((data[1] & 0xff) & 0x80) == (byte) 0x80){
				packet.marker = 1;
			}else{
				packet.marker = 0;
			}

			// Read payload type
			packet.payloadType = (byte) ((data[1] & 0xff) & 0x7f);

            // Read sequence number (it's a unsigned 16 bit value. Because Java only supports 
            // signed values for int and short we use char to do the correct conversion.) 
            packet.seqnum = (char)((data[2] << 8) | (data[3] & 0xff));

			// Read timestamp
			packet.timestamp = (((data[4] & 0xff) << 24) | ((data[5] & 0xff) << 16)
					| ((data[6] & 0xff) << 8) | (data[7] & 0xff));

			// Read SSRC
			packet.ssrc = (((data[8] & 0xff) << 24) | ((data[9] & 0xff) << 16)
					| ((data[10] & 0xff) << 8) | (data[11] & 0xff));

            // Extract the extension header
            if (packet.extension) {
                int dataId = 11;
                int extensionHeaderId = ((data[++dataId] & 0xff) << 8) | (data[++dataId] & 0xff);
                int length = ((data[++dataId] & 0xff) << 8) | (data[++dataId] & 0xff);
                
                if (extensionHeaderId == RtpExtensionHeader.RTP_EXTENSION_HEADER_ID) {
                    extractExtensionHeader(data, length, dataId, packet);
                }
                
                // increment payload offset = RtpHeader size (12) + Extension Header ID (2) + Header Length (2) +
                // elements * 4 (32 bits each) + 1 (to set at correct index) 
                packet.payloadoffset = 16 + length * 4;
            } else {
                packet.payloadoffset = 12;
            }
			packet.payloadlength = packet.length - packet.payloadoffset;
			packet.data = new byte[packet.payloadlength];
			System.arraycopy(data, packet.payloadoffset, packet.data, 0, packet.payloadlength);
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("RTP packet parsing error", e);
			}
			return null;
		}
        return packet;
	}

    /**
     * Returns the statistics of RTP reception
     *
     * @return Statistics
     */
	public RtpStatisticsReceiver getRtpReceptionStats() {
		return stats;
	}

    /**
     * Returns the DatagramConnection of RTP
     *
     * @return DatagramConnection
     */
    public DatagramConnection getConnection() {
        return datagramConnection;
    }

    /**
     * Extract Extension Header
     *
     * @param data
     * @param length
     * @param dataId
     * @param packet
     */
    private void extractExtensionHeader(byte[] data, int length, int dataId, RtpPacket packet) {
        byte[] extensionHeaderData = new byte[length * 4];
        System.arraycopy(data, ++dataId, extensionHeaderData, 0, extensionHeaderData.length);
        packet.extensionHeader = new RtpExtensionHeader();

        int i = 0;
        while (packet.extensionHeader.elementsCount() < length) {
            byte idAndLength = extensionHeaderData[i];
            if (idAndLength == 0x00) {
                // its a padding byte, skip it
                i = i + 1;
                continue;
            }

            int elementId = (idAndLength & 0xf0) >>> 4;

            // Each extension element id must have a value between 1 and 14 inclusive
            if (elementId > 0 && elementId < 15) {
                int elementLength = (idAndLength & 0x0f);
                byte[] elementData = new byte[elementLength + 1];
                System.arraycopy(extensionHeaderData, i + 1, elementData, 0, elementData.length);
                packet.extensionHeader.addElement(elementId, elementData);
                i = i + elementData.length + 1; 
            } else {
                break;
            }
        }
    }

}
