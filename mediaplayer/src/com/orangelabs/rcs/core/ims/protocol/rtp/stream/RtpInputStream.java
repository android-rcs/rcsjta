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

package com.orangelabs.rcs.core.ims.protocol.rtp.stream;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.TimeoutException;

import com.orangelabs.rcs.core.ims.protocol.rtp.RtpUtils;
import com.orangelabs.rcs.core.ims.protocol.rtp.core.RtcpPacketReceiver;
import com.orangelabs.rcs.core.ims.protocol.rtp.core.RtcpPacketTransmitter;
import com.orangelabs.rcs.core.ims.protocol.rtp.core.RtcpSession;
import com.orangelabs.rcs.core.ims.protocol.rtp.core.RtpExtensionHeader.ExtensionElement;
import com.orangelabs.rcs.core.ims.protocol.rtp.core.RtpPacket;
import com.orangelabs.rcs.core.ims.protocol.rtp.core.RtpPacketReceiver;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.Format;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.VideoOrientation;
import com.orangelabs.rcs.core.ims.protocol.rtp.util.Buffer;

/**
 * RTP input stream
 *
 * @author jexa7410
 */
public class RtpInputStream implements ProcessorInputStream {
    /**
     * RTP Socket Timeout
     * Used a 20s timeout value because the RTP packets can have a delay
     */
    private static final int RTP_SOCKET_TIMEOUT = 20000;

    /**
     * Remote address
     */
    private String remoteAddress;

    /**
     * Remote port
     */
    private int remotePort;

    /**
     * Local port
     */
    private int localPort;

	/**
	 * RTP receiver
	 */
	private RtpPacketReceiver rtpReceiver =  null;

	/**
	 * RTCP receiver
	 */
	private RtcpPacketReceiver rtcpReceiver =  null;

   /**
     * RTCP transmitter
     */
    private RtcpPacketTransmitter rtcpTransmitter =  null;

    /**
     * Input buffer
     */
	private Buffer buffer = new Buffer();

    /**
     * Input format
     */
	private Format inputFormat = null;

    /**
     * RTCP Session
     */
    private RtcpSession rtcpSession = null;

    /**
     * RTP stream listener
     */
    private RtpStreamListener rtpStreamListener;

    /**
     * The negotiated orientation extension header id
     */
    private int extensionHeaderId = RtpUtils.RTP_DEFAULT_EXTENSION_ID;

    /**
     * Indicates if the stream was closed
     */
    private boolean isClosed = false;

    /**
     * Sequence RTP packets buffer
     */
    private PriorityQueue<RtpPacket> rtpPacketsBuffer;

    /**
     * Constructor
     *
     * @param localPort Local port
     * @param inputFormat Input format
     */
    public RtpInputStream(String remoteAddress, int remotePort, int localPort, Format inputFormat) {
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
		this.localPort = localPort;
		this.inputFormat = inputFormat;

        rtcpSession = new RtcpSession(false, 16000);

        rtpPacketsBuffer = new PriorityQueue<RtpPacket>(10, new Comparator<RtpPacket>() {
            @Override
            public int compare(RtpPacket object1, RtpPacket object2) {
                if (object1.seqnum == object2.seqnum) {
                    return 0;
                } else if (object1.seqnum < object2.seqnum) {
                    return -1;
                }
                return 1;
            }
        });

    }

    /**
     * Open the input stream
     *
     * @throws Exception
     */
    public void open() throws Exception {
    	// Create the RTP receiver
        rtpReceiver = new RtpPacketReceiver(localPort, rtcpSession, RTP_SOCKET_TIMEOUT);
        rtpReceiver.start();

    	// Create the RTCP receiver
        rtcpReceiver = new RtcpPacketReceiver(localPort + 1, rtcpSession);
        rtcpReceiver.start();

        // Create the RTCP transmitter
        rtcpTransmitter = new RtcpPacketTransmitter(remoteAddress,
                remotePort + 1,
                rtcpSession,
                rtcpReceiver.getConnection());
        rtcpTransmitter.start();

        isClosed = false;
    }

    /**
     * Close the input stream
     */
    public void close() {
		try {
            isClosed = true;

            // Close the RTCP transmitter
            if (rtcpTransmitter != null)
                rtcpTransmitter.close();

			// Close the RTP receiver
			if (rtpReceiver != null) {
				rtpReceiver.close();
			}

			// Close the RTCP receiver
			if (rtcpReceiver != null) {
				rtcpReceiver.close();
			}
            rtpStreamListener = null;
		} catch(Exception e) {
		}
	}

    /**
     * Returns the RTP receiver
     *
     * @return RTP receiver
     */
    public RtpPacketReceiver getRtpReceiver() {
    	return rtpReceiver;
    }

    /**
     * Returns the RTCP receiver
     *
     * @return RTCP receiver
     */
    public RtcpPacketReceiver getRtcpReceiver() {
        return rtcpReceiver;
    }

    /**
     * Read from the input stream without blocking
     *
     * @return Buffer
     * @throws Exception
     */
    public Buffer read() throws Exception {
        try {
            do {
                // Wait and read a RTP packet
                RtpPacket rtpPacket = rtpReceiver.readRtpPacket();
                if (rtpPacket == null) {
                    return null;
                }

                // Add the buffer in queue
                rtpPacketsBuffer.add(rtpPacket);
            } while (rtpPacketsBuffer.size() <= 5);

            RtpPacket packet = rtpPacketsBuffer.poll();

        	// Create a buffer
            buffer.setData(packet.data);
            buffer.setLength(packet.payloadlength);
            buffer.setOffset(0);
            buffer.setFormat(inputFormat);
        	buffer.setSequenceNumber(packet.seqnum);
        	buffer.setRTPMarker(packet.marker!=0);
        	buffer.setTimeStamp(packet.timestamp);

            if (packet.extensionHeader != null) {
                ExtensionElement element = packet.extensionHeader.getElementById(extensionHeaderId);
                if (element != null) {
                    buffer.setVideoOrientation(VideoOrientation.parse(element.data[0]));
                }
            }

        	// Set inputFormat back to null
        	inputFormat = null;
        	return buffer;
        } catch (TimeoutException ex) {
            if (!isClosed) {
                if (rtpStreamListener != null) {
                    rtpStreamListener.rtpStreamAborted();
                }
            }
            return null;
        }
    }

    /**
     * Adds the RTP stream listener
     *
     * @param rtpStreamListener
     */
    public void addRtpStreamListener(RtpStreamListener rtpStreamListener) {
        this.rtpStreamListener = rtpStreamListener;
    }

    /**
     * Sets the negotiated orientation extension header id
     *
     * @param extensionHeaderId Header id
     */
    public void setExtensionHeaderId(int extensionHeaderId) {
        this.extensionHeaderId = extensionHeaderId;
    }

}
