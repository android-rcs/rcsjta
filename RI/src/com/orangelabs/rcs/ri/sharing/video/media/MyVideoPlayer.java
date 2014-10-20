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
import java.util.Iterator;

import android.hardware.Camera;
import android.os.RemoteException;
import android.os.SystemClock;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.vsh.IVideoPlayerListener;
import com.gsma.services.rcs.vsh.VideoCodec;
import com.gsma.services.rcs.vsh.VideoPlayer;
import com.orangelabs.rcs.core.ims.protocol.rtp.VideoRtpSender;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.JavaPacketizer;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.encoder.NativeH264Encoder;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.encoder.NativeH264EncoderParams;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.profiles.H264Profile1b;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.CameraOptions;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.H264VideoFormat;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.Orientation;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.VideoOrientation;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaException;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaInput;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.VideoSample;
import com.orangelabs.rcs.core.ims.protocol.rtp.stream.RtpStreamListener;

/**
 * Live RTP video player based on H264 QCIF format
 */
public class MyVideoPlayer extends VideoPlayer implements Camera.PreviewCallback, RtpStreamListener {
    /**
     * Default video codec
     */
    private VideoCodec defaultVideoCodec;

    /**
     * Is player opened
     */
    private boolean opened = false;

    /**
     * Is player started
     */
    private boolean started = false;    
    
    /**
     * Local RTP port
     */
    private int localRtpPort;

    /**
     * RTP sender session
     */
    private VideoRtpSender rtpSender = null;

    /**
     * RTP media input
     */
    private MediaRtpInput rtpInput = null;

    /**
     * Video start time
     */
    private long videoStartTime = 0L;

    /**
     * Temporary connection to reserve the port
     */
    private DatagramConnection temporaryConnection = null;

    /**
     * NAL SPS
     */
    private byte[] sps = new byte[0];
    
    /**
     * NAL PPS
     */
    private byte[] pps = new byte[0];

    /**
     * Timestamp increment
     */
    private int timestampInc;
    
    /***
     * Current time stamp
     */
    private long timeStamp = 0;
    
    /**
	 * NAL initialization
	 */
	private boolean nalInit = false;

    /**
     * NAL repeat
     */
    private int nalRepeat = 0;

    /**
     * NAL repeat MAX value
     */
    private static final int NALREPEATMAX = 20;

    /**
     * Scaling factor for encoding
     */
    private float scaleFactor = 1;

    /**
     * Source width used for resizing
     */
    private int srcWidth = 0;

    /**
     * Source height used for resizing
     */
    private int srcHeight = 0;
    
    /**
     * Mirroring (horizontal and vertival) for encoding
     */
    private boolean mirroring = false;

    /**
     * Video Orientation
     */
    private Orientation mOrientation = Orientation.NONE;    
    
    /**
     * Orientation header id.
     */
    private int orientationHeaderId = -1;

    /**
     * Camera ID
     */
    private int cameraId = CameraOptions.BACK.getValue();

    /**
     * Frame process
     */
    private FrameProcess frameProcess;

    /**
     * Frame buffer
     */
    private FrameBuffer frameBuffer = new FrameBuffer();

    /**
     * Constructor
     */
    public MyVideoPlayer() {
    	// Set the local RTP port
        localRtpPort = NetworkRessourceManager.generateLocalRtpPort();
        reservePort(localRtpPort);

        // Set the default media codec
    	defaultVideoCodec = new VideoCodec(H264Config.CODEC_NAME,
    			H264VideoFormat.PAYLOAD,
                H264Config.CLOCK_RATE,
                15,
                96000,
                H264Config.QCIF_WIDTH, 
                H264Config.QCIF_HEIGHT,
    			H264Config.CODEC_PARAM_PROFILEID + "=" + H264Profile1b.BASELINE_PROFILE_ID + ";" + H264Config.CODEC_PARAM_PACKETIZATIONMODE + "=" + JavaPacketizer.H264_ENABLED_PACKETIZATION_MODE);
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
	 * Opens the player and prepares resources (e.g. encoder, camera)
	 * 
	 * @param codec Video codec
	 * @param remoteHost Remote RTP host
	 * @param remotePort Remote RTP port
	 */
	public synchronized void open(VideoCodec codec, String remoteHost, int remotePort) {
        if (opened) {
            // Already opened
            return;
        }
        
        // Set the video codec
        defaultVideoCodec = codec;

        // Init video encoder
        try {
            timestampInc = (int)(90000 / defaultVideoCodec.getFrameRate());
            NativeH264EncoderParams nativeH264EncoderParams = new NativeH264EncoderParams();

            // Codec dimensions
            nativeH264EncoderParams.setFrameWidth(defaultVideoCodec.getVideoWidth());
            nativeH264EncoderParams.setFrameHeight(defaultVideoCodec.getVideoHeight());
            nativeH264EncoderParams.setFrameRate(defaultVideoCodec.getFrameRate());
            nativeH264EncoderParams.setBitRate(defaultVideoCodec.getBitRate());

            // Codec profile and level
            nativeH264EncoderParams.setProfilesAndLevel(defaultVideoCodec.getParameters());

            // Codec settings optimization
            nativeH264EncoderParams.setEncMode(NativeH264EncoderParams.ENCODING_MODE_STREAMING);
            nativeH264EncoderParams.setSceneDetection(false);
            nativeH264EncoderParams.setIFrameInterval(15);

            int result = NativeH264Encoder.InitEncoder(nativeH264EncoderParams);
            if (result != 0) {
               notifyPlayerEventError(VideoPlayer.Error.INTERNAL_ERROR);
               return;
            }
        } catch (UnsatisfiedLinkError e) {
            notifyPlayerEventError(VideoPlayer.Error.INTERNAL_ERROR);
            return;
        }

        // Init the RTP layer
        try {
            releasePort();
            rtpSender = new VideoRtpSender(new H264VideoFormat(), localRtpPort);
            rtpInput = new MediaRtpInput();
            rtpInput.open();
            rtpSender.prepareSession(rtpInput, remoteHost, remotePort, this);
        } catch (Exception e) {
        	e.printStackTrace();
            notifyPlayerEventError(VideoPlayer.Error.INTERNAL_ERROR);
            return;
        }

        // Player is opened
        opened = true;
        notifyPlayerEventOpened();
    }

	/**
	 * Closes the player and deallocates resources
	 * 
	 * @throws RcsServiceException
	 */
	public synchronized void close() {
        if (!opened) {
            // Already closed
            return;
        }
        // Close the RTP layer
        rtpInput.close();
        rtpSender.stopSession();

        try {
            // Close the video encoder
            NativeH264Encoder.DeinitEncoder();
        } catch (UnsatisfiedLinkError e) {
        	e.printStackTrace();
        }

        // Player is closed
        opened = false;
        notifyPlayerEventClosed();
        
        // Remove all listeners
        removeAllEventListeners();
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

        // Init NAL
        if (!initNAL()) {
            return;
        }
        nalInit = false;

        timeStamp = 0;
        nalInit = false;
        nalRepeat = 0;

        // Start RTP layer
        rtpSender.startSession();
        
        // Player is started
        videoStartTime = SystemClock.uptimeMillis();
        started = true;
        frameProcess = new FrameProcess((int)defaultVideoCodec.getFrameRate());
        frameProcess.start();
        notifyPlayerEventStarted();
    }

	/**
	 * Stops the player
	 */
	public synchronized void stop() {
        if (!opened) {
            // Player not opened
            return;
        }

        if (!started) {
            // Already stopped
            return;
        }

        // Player is stopped
        videoStartTime = 0L;
        started = false;
        try {
            frameProcess.interrupt();
        } catch (Exception e) {
            // Nothing to do
        }
        notifyPlayerEventStopped();
    }

    /*---------------------------------------------------------------------*/

    /**
     * Reserve a port.
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
     * Return the video start time
     *
     * @return Milliseconds
     */
    public long getVideoStartTime() {
        return videoStartTime;
    }

    /**
     * Init sps and pps
     *
     * @return true if done
     */
    private boolean initNAL() {
        boolean ret = initOneNAL();
        if (ret) {
            ret = initOneNAL();
        }
        return ret;
    }

    /**
     * Init sps or pps
     *
     * @return true if done
     */
    private boolean initOneNAL() {
        byte[] nal = NativeH264Encoder.getNAL();
        if ((nal != null) && (nal.length > 0)) {
            int type = (nal[0] & 0x1f);
            if (type == JavaPacketizer.AVC_NALTYPE_SPS) {
                sps = nal;
                return true;
            } else if (type == JavaPacketizer.AVC_NALTYPE_PPS) {
                pps = nal;
                return true;
            }
        }
        return false;
    }

    /**
     * Get video width
     *
     * @return Width
     */
    public int getVideoWidth() {
        if (defaultVideoCodec == null) {
            return H264Config.VIDEO_WIDTH;
        } else {
            return defaultVideoCodec.getVideoWidth();
        }
    }

    /**
     * Get video height
     *
     * @return Height
     */
    public int getVideoHeight() {
        if (defaultVideoCodec == null) {
            return H264Config.VIDEO_HEIGHT;
        } else {
            return defaultVideoCodec.getVideoHeight();
        }
    }

    /**
     * Set extension header orientation id
     *
     * @param headerId extension header orientation id
     */
    public void setOrientationHeaderId(int headerId) {
        this.orientationHeaderId = headerId;
    }

    /**
     * Set camera ID
     *
     * @param cameraId Camera ID
     */
    public void setCameraId(int cameraId) {
        this.cameraId = cameraId;
    }

    /**
     * Set video orientation
     *
     * @param orientation
     */
    public void setOrientation(Orientation orientation) {
        mOrientation = orientation;
    }

    /**
     * Set the scaling factor
     *
     * @param scaleFactor New scaling factor
     */
    public void setScalingFactor(float scaleFactor) {
        this.scaleFactor = scaleFactor;
        this.srcWidth = 0;
        this.srcHeight = 0;
    }

    /**
     * Set the source dimension for resizing
     *
     * @param srcWidth
     * @param srcHeight
     */
    public void activateResizing(int srcWidth, int srcHeight) {
        this.srcWidth = srcWidth;
        this.srcHeight = srcHeight;
        this.scaleFactor = 1;
    }

    /**
     * Set the mirroring value
     *
     * @param mirroring New mirroring value
     */
    public void setMirroring(boolean mirroring) {
        this.mirroring = mirroring;
    }

    /**
     * Notify RTP aborted
     */
    public void rtpStreamAborted() {
        notifyPlayerEventError(VideoPlayer.Error.NETWORK_FAILURE);
    }

    /**
     * Notify player event started
     */
    private void notifyPlayerEventStarted() {
    	try {
	        Iterator<IVideoPlayerListener> ite = getEventListeners().iterator();
	        while (ite.hasNext()) {
	        	ite.next().onPlayerStarted();
	        }
		} catch(RemoteException e) {
			e.printStackTrace();
		}
    }

    /**
     * Notify player event stopped
     */
    private void notifyPlayerEventStopped() {
    	try {
	        Iterator<IVideoPlayerListener> ite = getEventListeners().iterator();
	        while (ite.hasNext()) {
	        	ite.next().onPlayerStopped();
	        }
    	} catch(RemoteException e) {
			e.printStackTrace();
    	}
    }

    /**
     * Notify player event opened
     */
    private void notifyPlayerEventOpened() {
    	try {
	        Iterator<IVideoPlayerListener> ite = getEventListeners().iterator();
	        while (ite.hasNext()) {
	        	ite.next().onPlayerOpened();
	        }
		} catch(RemoteException e) {
			e.printStackTrace();
		}
    }

    /**
     * Notify player event closed
     */
    private void notifyPlayerEventClosed() {
    	try {
	        Iterator<IVideoPlayerListener> ite = getEventListeners().iterator();
	        while (ite.hasNext()) {
	        	ite.next().onPlayerClosed();
	        }
		} catch(RemoteException e) {
			e.printStackTrace();
		}
    }

    /**
     * Notify player event error
     */
    private void notifyPlayerEventError(int error) {
    	try {
	        Iterator<IVideoPlayerListener> ite = getEventListeners().iterator();
	        while (ite.hasNext()) {
	        	ite.next().onPlayerError(error);
	        }
		} catch(RemoteException e) {
			e.printStackTrace();
		}
    }

    /**
     * Preview frame from the camera
     *
     * @param data Frame
     * @param camera Camera
     */
    public void onPreviewFrame(byte[] data, Camera camera) {
    	if (!started) {
			return;
		}
		
		frameBuffer.setData(data);
    };

    /**
     * encode a buffer and add in RTP input
     *
     * @param data
     */
    private void encode(byte[] data) {
        // Send SPS/PPS if necessary
        nalRepeat++;
        if (nalRepeat > NALREPEATMAX) {
            nalInit = false;
            nalRepeat = 0;
        }
        if (!nalInit) {
            rtpInput.addFrame(sps, timeStamp);
            timeStamp += timestampInc;

            rtpInput.addFrame(pps, timeStamp);
            timeStamp += timestampInc;
            
            nalInit = true;
        } 

        // Encode frame
        byte[] encoded;
        if (frameBuffer.dataSrcWidth != 0 && frameBuffer.dataSrcHeight != 0) {
            encoded = NativeH264Encoder.ResizeAndEncodeFrame(data, timeStamp, mirroring, frameBuffer.dataSrcWidth, frameBuffer.dataSrcHeight);
        } else {
            encoded = NativeH264Encoder.EncodeFrame(data, timeStamp, mirroring, frameBuffer.dataScaleFactor);
        }
        int encodeResult = NativeH264Encoder.getLastEncodeStatus();
        if ((encodeResult == 0) && (encoded.length > 0)) {
            VideoOrientation videoOrientation = null;
            if (orientationHeaderId > 0 ) {
                videoOrientation = new VideoOrientation(
                        orientationHeaderId,
                        CameraOptions.convert(cameraId),
                        mOrientation);
            }
            rtpInput.addFrame(encoded, timeStamp, videoOrientation);
            timeStamp += timestampInc;
        }
    }

    /**
     * Frame process
     */
    private class FrameProcess extends Thread {
        
        /**
         * Time between two frame
         */
        private int interframe = 1000 / 15;

        /**
         * Constructor
         *
         * @param framerate
         */
        public FrameProcess(int framerate) {
            super();
            interframe = 1000 / framerate;
        }

        @Override
        public void run() {
            byte[] frameData = null;
            while (started) {
                long time = System.currentTimeMillis();

                // Encode
                frameData = frameBuffer.getData();
                if (frameData != null) {
                    encode(frameData);
                }

                // Sleep between frames if necessary
                long delta = System.currentTimeMillis() - time;
                if (delta < interframe) {
                    try {
                        Thread.sleep((interframe - delta) - (((interframe - delta) * 10) / 100));
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    /**
     * Frame buffer
     */
    private class FrameBuffer {
        /**
         * Data
         */
        private byte[] data = null;

        /**
         * Scaling factor for encoding
         */
        public float dataScaleFactor = 1;

        /**
         * Source Width - used for resizing
         */
        public int dataSrcWidth = 0;

        /**
         * Source Height - used for resizing
         */
        public int dataSrcHeight = 0;

        /**
         * Get the data
         *
         * @return data
         */
        public synchronized byte[] getData() {
            return data;
        }

        /**
         * Set the data
         *
         * @param data
         */
        public synchronized void setData(byte[] data) {
            this.data = data;

            // Update resizing / scaling values
            this.dataScaleFactor = scaleFactor;
            this.dataSrcWidth = defaultVideoCodec.getVideoWidth();
            this.dataSrcHeight = defaultVideoCodec.getVideoHeight();
        }
    }

    /**
     * Media RTP input
     */
    private static class MediaRtpInput implements MediaInput {
        /**
         * Received frames
         */
        private FifoBuffer fifo = null;

        /**
         * Constructor
         */
        public MediaRtpInput() {
        }

        /**
         * Add a new video frame
         *
         * @param data Data
         * @param timestamp Timestamp
         * @param marker Marker bit 
         */
        public void addFrame(byte[] data, long timestamp, VideoOrientation videoOrientation) {
            if (fifo != null) {
                VideoSample sample = new VideoSample(data, timestamp, videoOrientation);
                fifo.addObject(sample);
            }
        }

        /**
         * Add a new video frame
         *
         * @param data Data
         * @param timestamp Timestamp
         * @param marker Marker bit 
         */
        public void addFrame(byte[] data, long timestamp) {
            addFrame(data, timestamp, null);
        }

        /**
         * Open the player
         */
        public void open() {
            fifo = new FifoBuffer();
        }

        /**
         * Close the player
         */
        public void close() {
            if (fifo != null) {
                fifo.close();
                fifo = null;
            }
        }

        /**
         * Read a media sample (blocking method)
         *
         * @return Media sample
         * @throws MediaException
         */
        public VideoSample readSample() throws MediaException {
            try {
                if (fifo != null) {
                    return (VideoSample)fifo.getObject();
                } else {
                    throw new MediaException("Media input not opened");
                }
            } catch (Exception e) {
                throw new MediaException("Can't read media sample");
            }
        }
    }
}
