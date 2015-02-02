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
import java.util.concurrent.TimeoutException;

import com.orangelabs.rcs.core.ims.protocol.rtp.util.AndroidDatagramConnection;
import com.orangelabs.rcs.core.ims.protocol.rtp.util.DatagramConnection;
import com.orangelabs.rcs.core.ims.protocol.rtp.util.FifoBuffer;

/**
 * RTP packet receiver
 *
 * @author jexa7410
 */
public class RtpPacketReceiver extends Thread {
    /**
     * Statistics
     */
	private RtpStatisticsReceiver stats = new RtpStatisticsReceiver();

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
     * Fifo buffer for received packet
     */
    private FifoBuffer fifo = new FifoBuffer();

    /**
     * Max size for the fifo
     */
    private static final int FIFO_MAX_NUMBER = 100; 

    /**
     * Number of element to clean in the fifo
     */
    private static final int FIFO_CLEAN_NUMBER = 20; 

    /**
     * Last sequence number
     */
    private int lastSeqnum = 0;

    /**
     * timeout
     */
    private int timeout = 0;

    /**
     * Constructor
     *
     * @param port Listenning port
     * @param rtcpSession
     * @param socketTimeout
     * @throws IOException
     */
    public RtpPacketReceiver(int port, RtcpSession rtcpSession, int socketTimeout) throws IOException {
        super();

        this.rtcpSession = rtcpSession;
        this.timeout = socketTimeout;
        // Create the UDP server
        datagramConnection = new AndroidDatagramConnection(socketTimeout);
        datagramConnection.open(port);
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
        // Interrupt the current thread processing
        try {
            interrupt();
        } catch(Exception e) {
            // Nothing to do
        }

		// Close the datagram connection
		if (datagramConnection != null) {
			try {
                isClosed = true;
				datagramConnection.close();
			} catch(Exception e) {
			}
			datagramConnection = null;
		}
	}

    /**
     * Background processing
     */
    public void run() {
        try {
            while (datagramConnection != null) {
                // Wait a new packet
                byte[] data = datagramConnection.receive();

                if (data.length >= 12) {
                    // Drop empty packet (payload 20)
                    int payloadType = (byte) ((data[1] & 0xff) & 0x7f);
                    if (payloadType != 20) {
                        // Drop too old packet
                        int seqnum = (char)((data[2] << 8) | (data[3] & 0xff));
                        if (seqnum > lastSeqnum - 10) {
                            // Clean the FIFO if full
                            if (fifo.size() >= FIFO_MAX_NUMBER) {
                                fifo.clean(FIFO_CLEAN_NUMBER);
                            }
                            fifo.addObject(data);
                            lastSeqnum = seqnum;
                        } else {
                            stats.numBadRtpPkts++;
                        }
                    }
                }
            }
        } catch (SocketTimeoutException ex) {
            
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }

    /**
     * Read a RTP packet (blocking method)
     *
     * @return RTP packet
     */
    public RtpPacket readRtpPacket() throws TimeoutException {
		try {
            // Get a new packet in FIFO
            byte[] data = (byte[]) fifo.getObject(timeout);
            if (data == null) {
                throw new TimeoutException();
            }

			// Parse the RTP packet
			RtpPacket pkt = parseRtpPacket(data);

			if (pkt != null) {
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

		} catch (Exception e) {
            if (!isClosed) {
//                if (logger.isActivated()) {
//                    logger.error("Can't parse the RTP packet", e);
//                }
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
