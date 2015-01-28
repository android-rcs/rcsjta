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

package com.orangelabs.rcs.core.ims.protocol.rtp;

import com.orangelabs.rcs.core.ims.protocol.rtp.codec.Codec;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.Format;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaInput;
import com.orangelabs.rcs.core.ims.protocol.rtp.stream.RtpInputStream;
import com.orangelabs.rcs.core.ims.protocol.rtp.stream.RtpOutputStream;
import com.orangelabs.rcs.core.ims.protocol.rtp.stream.RtpStreamListener;
import com.orangelabs.rcs.core.ims.protocol.rtp.stream.VideoCaptureStream;

/**
 * Video RTP sender
 *
 * @author hlxn7157
 */
public class VideoRtpSender extends MediaRtpSender {
    /**
     * Constructor
     *
     * @param format Media format
     */
    public VideoRtpSender(Format format, int localRtpPort) {
        super(format, localRtpPort);
    }

    /**
     * Prepare the RTP session
     *
     * @param player Media player
     * @param remoteAddress Remote address
     * @param remotePort Remote port
     * @param RtpStreamListener rtp stream listener
     * @throws RtpException
     */
    public void prepareSession(MediaInput player, String remoteAddress, int remotePort, RtpStreamListener rtpStreamListener)
            throws RtpException {
    	try {
    		// Create the input stream
            inputStream = new VideoCaptureStream(format, player);
    		inputStream.open();

            // Create the output stream
            outputStream = new RtpOutputStream(remoteAddress, remotePort, localRtpPort, RtpOutputStream.RTCP_SOCKET_TIMEOUT);
            outputStream.addRtpStreamListener(rtpStreamListener);
            outputStream.open();

        	// Create the codec chain
        	Codec[] codecChain = MediaRegistry.generateEncodingCodecChain(format.getCodec());

            // Create the media processor
    		processor = new Processor(inputStream, outputStream, codecChain);
        } catch(Exception e) {
        	throw new RtpException("Can't prepare resources");
        }
    }
    
    /**
     * Prepare the RTP session for a sender associated to a receiver
     *
     * @param player Media player
     * @param remoteAddress Remote address
     * @param remotePort Remote port
     * @param rtpStream rtp input stream
     * @param RtpStreamListener rtp stream listener
     * @throws RtpException
     */
    

    public void prepareSession(MediaInput player, String remoteAddress, int remotePort, RtpInputStream rtpStream, RtpStreamListener rtpStreamListener)
            throws RtpException {
    	try {
    		// Create the input stream
            inputStream = new VideoCaptureStream(format, player);
    		inputStream.open();

            // Create the output stream
            //outputStream = new RtpOutputStream(remoteAddress, remotePort, localRtpPort, RtpOutputStream.RTCP_SOCKET_TIMEOUT);
            outputStream = new RtpOutputStream(remoteAddress, remotePort, rtpStream);
            outputStream.addRtpStreamListener(rtpStreamListener);
            outputStream.open();

        	// Create the codec chain
        	Codec[] codecChain = MediaRegistry.generateEncodingCodecChain(format.getCodec());

            // Create the media processor
    		processor = new Processor(inputStream, outputStream, codecChain);
        } catch(Exception e) {
        	throw new RtpException("Can't prepare resources");
        }
    }
}
