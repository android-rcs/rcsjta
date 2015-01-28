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

import android.graphics.Bitmap;
import android.os.SystemClock;

import com.gsma.services.rcs.vsh.VideoCodec;
import com.gsma.services.rcs.vsh.VideoPlayer;
import com.orangelabs.rcs.core.ims.protocol.rtp.DummyPacketGenerator;
import com.orangelabs.rcs.core.ims.protocol.rtp.VideoRtpReceiver;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.JavaPacketizer;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.decoder.NativeH264Decoder;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.profiles.H264Profile1b;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.CameraOptions;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.H264VideoFormat;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.Orientation;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.VideoOrientation;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaOutput;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaSample;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.VideoSample;
import com.orangelabs.rcs.core.ims.protocol.rtp.stream.RtpStreamListener;
import com.orangelabs.rcs.ri.utils.DatagramConnection;
import com.orangelabs.rcs.ri.utils.NetworkRessourceManager;

/**
 * Live video RTP renderer based on H264 QCIF format
 *
 * @author Jean-Marc AUFFRET
 */
public class TerminatingVideoPlayer extends VideoPlayer implements RtpStreamListener {
    /**
     * Default video codec
     */
    private VideoCodec defaultVideoCodec;

    /**
     * Local RTP port
     */
    private int localRtpPort;

    /**
     * RTP receiver session
     */
    private VideoRtpReceiver rtpReceiver;

    /**
     * RTP dummy packet generator
     */
    private DummyPacketGenerator rtpDummySender;

    /**
     * RTP media output
     */
    private MediaRtpOutput rtpOutput;

    /**
     * Is player opened
     */
    private boolean opened = false;

    /**
     * Is player started
     */
    private boolean started = false;

    /**
     * Video start time
     */
    private long videoStartTime = 0L;

    /**
     * Video surface
     */
    private VideoSurface surface;

    /**
     * Temporary connection to reserve the port
     */
    private DatagramConnection temporaryConnection;

    /**
     * Orientation header id.
     */
    private int orientationHeaderId = -1;

    /**
     * Video player event listener
     */
    private VideoPlayerListener eventListener;

    /**
     * Remote host
     */
    private String remoteHost;
    
    /**
     * Remote port
     */
    private int remotePort;
    
    /**
     * Constructor
     * 
     * @param surface Surface view
     * @param eventListener Player event listener
     */
    public TerminatingVideoPlayer(VideoSurfaceView surface, VideoPlayerListener eventListener) {
    	// Set surface view
    	this.surface = surface;
    	
    	// Set event listener
    	this.eventListener = eventListener;

    	// Set the local RTP port
        localRtpPort = NetworkRessourceManager.generateLocalRtpPort();
        reservePort(localRtpPort);
        
        // Set the default media codec
    	defaultVideoCodec = new VideoCodec(H264Config.CODEC_NAME,
    			H264VideoFormat.PAYLOAD,
                H264Config.CLOCK_RATE,
                15,
                96000,
                H264Config.QCIF_WIDTH, H264Config.QCIF_HEIGHT,
    			H264Config.CODEC_PARAM_PROFILEID + "=" + H264Profile1b.BASELINE_PROFILE_ID + ";" + H264Config.CODEC_PARAM_PACKETIZATIONMODE + "=" + JavaPacketizer.H264_ENABLED_PACKETIZATION_MODE);
    }
    
    /**
	 * Set the remote info
	 * 
	 * @param codec Video codec
	 * @param remoteHost Remote RTP host
	 * @param remotePort Remote RTP port
	 * @param orientationHeaderId Orientation header extension ID. The extension ID is
	 *  a value between 1 and 15 arbitrarily chosen by the sender, as defined in RFC5285
	 */
	public void setRemoteInfo(VideoCodec codec, String remoteHost, int remotePort, int orientationHeaderId) {
        // Set the video codec
        defaultVideoCodec = codec;		
        
        // Set remote host and port
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
        
        // Set the orientation ID
        this.orientationHeaderId = orientationHeaderId;
	}
    
	/**
	 * Returns the list of codecs supported by the player
	 * 
	 * @return List of codecs
	 */
	public VideoCodec[] getSupportedCodecs() {
		VideoCodec[] list = new VideoCodec[1];
		list[0] = defaultVideoCodec;
		return list;
	}    
    
	/**
	 * Returns the current codec
	 * 
	 * @return Codec
	 */
	public VideoCodec getCodec() {
		return defaultVideoCodec;
	}
	
	/**
	 * Opens the player and prepares resources
	 */
	public synchronized void open() {
		if (opened) {
            // Already opened
            return;
        }

        try {
            // Init the video decoder
            int result = NativeH264Decoder.InitDecoder();
            if (result != 0) {
            	// Decoder init failed
        		eventListener.onPlayerError();
                return;
            }

            // Init the RTP layer
            releasePort();
            rtpReceiver = new VideoRtpReceiver(localRtpPort);
            rtpDummySender = new DummyPacketGenerator();
            rtpOutput = new MediaRtpOutput();
            rtpOutput.open();
            rtpReceiver.prepareSession(remoteHost, remotePort, orientationHeaderId, rtpOutput, new H264VideoFormat(), this);
            rtpDummySender.prepareSession(remoteHost, remotePort, rtpReceiver.getInputStream());
            rtpDummySender.startSession();
        } catch (Exception e) {
        	// RTP failed
    		eventListener.onPlayerError();
            return;
        }

        // Player is opened
        opened = true;
		eventListener.onPlayerOpened();
    }

	/**
	 * Closes the player and deallocates resources
	 */
	public synchronized void close() {
        if (!opened) {
            // Already closed
            return;
        }

        // Close the RTP layer
        rtpOutput.close();
        rtpReceiver.stopSession();
        rtpDummySender.stopSession();

        try {
            // Close the video decoder
            NativeH264Decoder.DeinitDecoder();
        } catch (UnsatisfiedLinkError e) {
        	e.printStackTrace();
        }

        // Player is closed
        opened = false;
		eventListener.onPlayerClosed();
    }

	/**
	 * Starts the player
	 */
	public synchronized void start() {
		if (!opened) {
            // Player not opened
            return;
        }

        if (started) {
            // Already started
            return;
        }
        
        // Start RTP layer
        rtpReceiver.startSession();

        // Player is started
        videoStartTime = SystemClock.uptimeMillis();
        started = true;
		eventListener.onPlayerStarted();
    }

	/**
	 * Stops the player
	 */
	public synchronized void stop() {
		if (!started) {
            return;
        }

        // Stop RTP layer
        if (rtpReceiver != null) {
            rtpReceiver.stopSession();
        }
        if (rtpDummySender != null) {
            rtpDummySender.stopSession();
        }
        if (rtpOutput != null) {
            rtpOutput.close();
        }

        // Force black screen
    	surface.clearImage();

        // Player is stopped
        started = false;
        videoStartTime = 0L;
		eventListener.onPlayerStopped();
    }
    
    /*---------------------------------------------------------------------*/
    
    /**
     * Return the video start time
     *
     * @return Milliseconds
     */
    public long getVideoStartTime() {
        return videoStartTime;
    }

    /**
     * Returns the local RTP port
     *
     * @return Port
     */
    public int getLocalRtpPort() {
        return localRtpPort;
    }

    /**
     * Reserve a port
     *
     * @param port Port to reserve
     */
    private void reservePort(int port) {
        if (temporaryConnection == null) {
            try {
                temporaryConnection = NetworkRessourceManager.createDatagramConnection();
                temporaryConnection.open(port);
            } catch (IOException e) {
                temporaryConnection = null;
            }
        }
    }

    /**
     * Release the reserved port.
     */
    private void releasePort() {
        if (temporaryConnection != null) {
            try {
                temporaryConnection.close();
            } catch (IOException e) {
                temporaryConnection = null;
            }
        }
    }

    /**
     * Is player opened
     *
     * @return Boolean
     */
    public boolean isOpened() {
        return opened;
    }

    /**
     * Is player started
     *
     * @return Boolean
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Notify RTP aborted
     */
    public void rtpStreamAborted() {
		eventListener.onPlayerError();
    }

	/**
	 * Set the video orientation
	 * 
	 * @param orientation Video orientation value (see urn:3gpp:video-orientation)
	 */
	public void setOrientation(int orientation) {
	    this.orientationHeaderId = orientation;
	}    
    
    /**
     * Media RTP output
     */
    private class MediaRtpOutput implements MediaOutput {
        /**
         * Bitmap frame
         */
        private Bitmap rgbFrame = null;

        /**
         * Video orientation
         */
        private VideoOrientation videoOrientation = new VideoOrientation(CameraOptions.BACK, Orientation.NONE);

        /**
         * Frame dimensions
         * Just 2 - width and height
         */
        private int decodedFrameDimensions[] = new int[2];

        /**
         * Constructor
         */
        public MediaRtpOutput() {
            // Init rgbFrame with a default size
            rgbFrame = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
        }

        /**
         * Open the renderer
         */
        public void open() {
            // Nothing to do
        }

        /**
         * Close the renderer
         */
        public void close() {
        }

        /**
         * Write a media sample
         *
         * @param sample Sample
         */
        public void writeSample(MediaSample sample) {
            rtpDummySender.incomingStarted();

            // Init orientation
            VideoOrientation orientation = ((VideoSample)sample).getVideoOrientation();
            if (orientation != null) {
                this.videoOrientation = orientation;
            }

            int[] decodedFrame = NativeH264Decoder.DecodeAndConvert(sample.getData(), videoOrientation.getOrientation().getValue(), decodedFrameDimensions);
            if (NativeH264Decoder.getLastDecodeStatus() == 0) {
                if ((surface != null) && (decodedFrame.length > 0)) {
                    // Init RGB frame with the decoder dimensions
                	if ((rgbFrame.getWidth() != decodedFrameDimensions[0]) || (rgbFrame.getHeight() != decodedFrameDimensions[1])) {
                        rgbFrame = Bitmap.createBitmap(decodedFrameDimensions[0], decodedFrameDimensions[1], Bitmap.Config.RGB_565);
                		eventListener.onPlayerResized(decodedFrameDimensions[0], decodedFrameDimensions[1]);
                    }

                	// Set data in image
                    rgbFrame.setPixels(decodedFrame, 0, decodedFrameDimensions[0], 0, 0,
                            decodedFrameDimensions[0], decodedFrameDimensions[1]);
                    surface.setImage(rgbFrame);
            	}
            }
        }
    }

	/**
	 * Set the surface
	 * @param surface
	 */
	public void setSurface(VideoSurface surface) {
		this.surface = surface;
	}
}
