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

package com.orangelabs.rcs.service.api.client.media.video;

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import android.hardware.Camera;
import android.os.RemoteException;
import android.os.SystemClock;

import com.orangelabs.rcs.core.ims.protocol.rtp.MediaRegistry;
import com.orangelabs.rcs.core.ims.protocol.rtp.MediaRtpSender;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.JavaPacketizer;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.encoder.NativeH264Encoder;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.encoder.NativeH264EncoderParams;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.CameraOptions;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.Orientation;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.VideoFormat;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.VideoOrientation;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaException;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaInput;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaSample;
import com.orangelabs.rcs.core.ims.protocol.rtp.stream.RtpStreamListener;
import com.orangelabs.rcs.platform.network.DatagramConnection;
import com.orangelabs.rcs.platform.network.NetworkFactory;
import com.orangelabs.rcs.service.api.client.media.IMediaEventListener;
import com.orangelabs.rcs.service.api.client.media.IMediaPlayer;
import com.orangelabs.rcs.service.api.client.media.MediaCodec;
import com.orangelabs.rcs.service.api.client.media.video.VideoCodec;
import com.orangelabs.rcs.utils.CodecsUtils;
import com.orangelabs.rcs.utils.FifoBuffer;
import com.orangelabs.rcs.utils.NetworkRessourceManager;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Live RTP video player based on H264 QCIF format
 */
public class LiveVideoPlayer extends IMediaPlayer.Stub implements Camera.PreviewCallback, RtpStreamListener {

    /**
     * List of supported video codecs
     */
    private MediaCodec[] supportedMediaCodecs = null; 

    /**
     * Selected video codec
     */
    private VideoCodec selectedVideoCodec = null;

    /**
     * Video format
     */
    private VideoFormat videoFormat;

    /**
     * Local RTP port
     */
    private int localRtpPort;

    /**
     * RTP sender session
     */
    private MediaRtpSender rtpSender = null;

    /**
     * RTP media input
     */
    private MediaRtpInput rtpInput = null;

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
     * Media event listeners
     */
    private Vector<IMediaEventListener> listeners = new Vector<IMediaEventListener>();

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
     * Mirroring (horizontal and vertival) for encoding
     */
    private boolean mirroring = false;

    /**
     * Orientation header id.
     */
    private int orientationHeaderId = -1;

    /**
     * Camera ID
     */
    private int cameraId = CameraOptions.FRONT.getValue();

    /**
     * Video Orientation
     */
    private Orientation mOrientation = Orientation.NONE;

    /**
     * Frame process
     */
    private FrameProcess frameProcess;

    /**
     * Frame buffer
     */
    private FrameBuffer frameBuffer = new FrameBuffer();

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     */
    public LiveVideoPlayer() {
    	// Set the local RTP port
        localRtpPort = NetworkRessourceManager.generateLocalRtpPort();
        reservePort(localRtpPort);

        // Init codecs
        supportedMediaCodecs = CodecsUtils.getPlayerCodecList();

        // Set the default media codec
        if (supportedMediaCodecs.length > 0) {
            setMediaCodec(supportedMediaCodecs[0]);
        }
    }

    /**
     * Constructor with a list of video codecs
     *
     * @param codecs Ordered list of codecs (preferred codec in first)
     */
    public LiveVideoPlayer(MediaCodec[] codecs) {
        // Set the local RTP port
        localRtpPort = NetworkRessourceManager.generateLocalRtpPort();
        reservePort(localRtpPort);

        // Init codecs
        supportedMediaCodecs = codecs;

        // Set the default media codec
        if (supportedMediaCodecs.length > 0) {
            setMediaCodec(supportedMediaCodecs[0]);
        }
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
     * Reserve a port.
     *
     * @param port Port to reserve
     */
    private void reservePort(int port) {
        if (temporaryConnection == null) {
            try {
                temporaryConnection = NetworkFactory.getFactory().createDatagramConnection();
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
     * Open the player
     *
     * @param remoteHost Remote host
     * @param remotePort Remote port
     */
    public void open(String remoteHost, int remotePort) {
        if (opened) {
            // Already opened
            return;
        }

        // Check video codec
        if (selectedVideoCodec == null) {
            notifyPlayerEventError("Video Codec not selected");
            return;
        }

        // Init video encoder
        try {
            timestampInc = 90000 / selectedVideoCodec.getFramerate();
            NativeH264EncoderParams nativeH264EncoderParams = new NativeH264EncoderParams();

            // Codec dimensions
            nativeH264EncoderParams.setFrameWidth(selectedVideoCodec.getWidth());
            nativeH264EncoderParams.setFrameHeight(selectedVideoCodec.getHeight());
            nativeH264EncoderParams.setFrameRate(selectedVideoCodec.getFramerate());
            nativeH264EncoderParams.setBitRate(selectedVideoCodec.getBitrate());

            // Codec profile and level
            nativeH264EncoderParams.setProfilesAndLevel(selectedVideoCodec.getCodecParams());

            // Codec settings optimization
            nativeH264EncoderParams.setEncMode(NativeH264EncoderParams.ENCODING_MODE_STREAMING);
            nativeH264EncoderParams.setSceneDetection(false);
            nativeH264EncoderParams.setIFrameInterval(15);

            if (logger.isActivated()) {
                logger.info("Init H264Encoder " + selectedVideoCodec.getCodecParams() + " " +
                        selectedVideoCodec.getWidth() + "x" + selectedVideoCodec.getHeight() + " " +
                        selectedVideoCodec.getFramerate() + " "+ selectedVideoCodec.getBitrate());
            }
            int result = NativeH264Encoder.InitEncoder(nativeH264EncoderParams);
            if (result != 0) {
               notifyPlayerEventError("Encoder init failed with error code " + result);
               return;
            }
        } catch (UnsatisfiedLinkError e) {
            notifyPlayerEventError(e.getMessage());
            return;
        }

        // Init the RTP layer
        try {
            releasePort();
            rtpSender = new MediaRtpSender(videoFormat, localRtpPort);
            rtpInput = new MediaRtpInput();
            rtpInput.open();
            rtpSender.prepareSession(rtpInput, remoteHost, remotePort, this);
        } catch (Exception e) {
            notifyPlayerEventError(e.getMessage());
            return;
        }

        // Player is opened
        opened = true;
        notifyPlayerEventOpened();
    }

    /**
     * Close the player
     */
    public void close() {
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
            if (logger.isActivated()) {
                logger.error("Can't close correctly the video encoder", e);
            }
        }

        // Player is closed
        opened = false;
        notifyPlayerEventClosed();
        listeners.clear();
    }

    /**
     * Start the player
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
        frameProcess = new FrameProcess(selectedVideoCodec.getFramerate());
        frameProcess.start();
        notifyPlayerEventStarted();
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
     * Stop the player
     */
    public void stop() {
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

    /**
     * Add a media event listener
     *
     * @param listener Media event listener
     */
    public void addListener(IMediaEventListener listener) {
        listeners.addElement(listener);
    }

    /**
     * Remove all media event listeners
     */
    public void removeAllListeners() {
        listeners.removeAllElements();
    }

    /**
     * Get supported media codecs
     *
     * @return media Codecs list
     */
    public MediaCodec[] getSupportedMediaCodecs() {
        return supportedMediaCodecs;
    }

    /**
     * Get media codec
     *
     * @return Media codec
     */
    public MediaCodec getMediaCodec() {
        if (selectedVideoCodec == null) {
            return null;
        } else {
            return selectedVideoCodec.getMediaCodec();
        }
    }

    /**
     * Get media codec width
     *
     * @return Width
     */
    public int getMediaCodecWidth() {
        if (selectedVideoCodec == null) {
            return H264Config.VIDEO_WIDTH;
        } else {
            return new VideoCodec(selectedVideoCodec.getMediaCodec()).getWidth();
        }
    }

    /**
     * Get media codec height
     *
     * @return Height
     */
    public int getMediaCodecHeight() {
        if (selectedVideoCodec == null) {
            return H264Config.VIDEO_HEIGHT;
        } else {
            return new VideoCodec(selectedVideoCodec.getMediaCodec()).getHeight();
        }
    }

    /**
     * Set media codec
     *
     * @param mediaCodec Media codec
     */
    public void setMediaCodec(MediaCodec mediaCodec) {
        if (VideoCodec.checkVideoCodec(supportedMediaCodecs, new VideoCodec(mediaCodec))) {
            VideoCodec codec = new VideoCodec(mediaCodec);
            if (codec.getHeight() == 0 || codec.getWidth() == 0) {
                selectedVideoCodec = new VideoCodec(codec.getCodecName(),
                        codec.getPayload(),
                        codec.getClockRate(),
                        codec.getCodecParams(),
                        codec.getFramerate(),
                        codec.getBitrate(),
                        H264Config.QCIF_WIDTH,
                        H264Config.QCIF_HEIGHT);
            } else {
                selectedVideoCodec = codec;
            }
            videoFormat = (VideoFormat) MediaRegistry.generateFormat(mediaCodec.getCodecName());
        } else {
            notifyPlayerEventError("Codec not supported");
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
        notifyPlayerEventError("RTP session aborted");
    }

    /**
     * Notify player event started
     */
    private void notifyPlayerEventStarted() {
        if (logger.isActivated()) {
            logger.debug("Player is started");
        }
        Iterator<IMediaEventListener> ite = listeners.iterator();
        while (ite.hasNext()) {
            try {
                ((IMediaEventListener)ite.next()).mediaStarted();
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
    }

    /**
     * Notify player event stopped
     */
    private void notifyPlayerEventStopped() {
        if (logger.isActivated()) {
            logger.debug("Player is stopped");
        }
        Iterator<IMediaEventListener> ite = listeners.iterator();
        while (ite.hasNext()) {
            try {
                ((IMediaEventListener)ite.next()).mediaStopped();
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
    }

    /**
     * Notify player event opened
     */
    private void notifyPlayerEventOpened() {
        if (logger.isActivated()) {
            logger.debug("Player is opened");
        }
        Iterator<IMediaEventListener> ite = listeners.iterator();
        while (ite.hasNext()) {
            try {
                ((IMediaEventListener)ite.next()).mediaOpened();
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
    }

    /**
     * Notify player event closed
     */
    private void notifyPlayerEventClosed() {
        if (logger.isActivated()) {
            logger.debug("Player is closed");
        }
        Iterator<IMediaEventListener> ite = listeners.iterator();
        while (ite.hasNext()) {
            try {
                ((IMediaEventListener)ite.next()).mediaClosed();
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
    }

    /**
     * Notify player event error
     */
    private void notifyPlayerEventError(String error) {
        if (logger.isActivated()) {
            logger.debug("Player error: " + error);
        }

        Iterator<IMediaEventListener> ite = listeners.iterator();
        while (ite.hasNext()) {
            try {
                ((IMediaEventListener)ite.next()).mediaError(error);
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
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
            if (logger.isActivated()) {
                logger.debug("Send SPS");
            }
            rtpInput.addFrame(sps, timeStamp);
            timeStamp += timestampInc;

            if (logger.isActivated()) {
                logger.debug("Send PPS");
            }
            rtpInput.addFrame(pps, timeStamp);
            timeStamp += timestampInc;
            
            nalInit = true;
        } 

        // Encode frame
        byte[] encoded = NativeH264Encoder.EncodeFrame(data, timeStamp, mirroring, scaleFactor);
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
                MediaSample sample = new MediaSample(data, timestamp);
                if (videoOrientation != null) {
                    sample.setVideoOrientation(videoOrientation);
                }
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
        public MediaSample readSample() throws MediaException {
            try {
                if (fifo != null) {
                    return (MediaSample)fifo.getObject();
                } else {
                    throw new MediaException("Media input not opened");
                }
            } catch (Exception e) {
                throw new MediaException("Can't read media sample");
            }
        }
    }
}
