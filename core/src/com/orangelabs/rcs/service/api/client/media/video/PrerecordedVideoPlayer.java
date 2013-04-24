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

import com.orangelabs.rcs.core.ims.protocol.rtp.MediaRegistry;
import com.orangelabs.rcs.core.ims.protocol.rtp.MediaRtpSender;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h263.H263Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h263.decoder.NativeH263Decoder;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h263.decoder.VideoSample;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.H263VideoFormat;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.VideoFormat;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaException;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaInput;
import com.orangelabs.rcs.core.ims.protocol.rtp.media.MediaSample;
import com.orangelabs.rcs.core.ims.protocol.rtp.stream.RtpStreamListener;
import com.orangelabs.rcs.platform.network.DatagramConnection;
import com.orangelabs.rcs.platform.network.NetworkFactory;
import com.orangelabs.rcs.service.api.client.media.IMediaEventListener;
import com.orangelabs.rcs.service.api.client.media.IMediaPlayer;
import com.orangelabs.rcs.service.api.client.media.MediaCodec;
import com.orangelabs.rcs.utils.FifoBuffer;
import com.orangelabs.rcs.utils.NetworkRessourceManager;
import com.orangelabs.rcs.utils.logger.Logger;

import android.graphics.Bitmap;
import android.os.RemoteException;
import android.os.SystemClock;

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

/**
 * Pre-recorded video RTP player. Supports only H.263 QCIF format.
 * 
 * @author jexa7410
 */
public class PrerecordedVideoPlayer extends IMediaPlayer.Stub implements RtpStreamListener {

    /**
     * List of supported video codecs
     */
    public static MediaCodec[] supportedMediaCodecs = {
            new VideoCodec(H263Config.CODEC_NAME,
            		H263VideoFormat.PAYLOAD,
            		H263Config.CLOCK_RATE,
            		H263Config.CODEC_PARAMS,
                    H263Config.FRAME_RATE,
                    H263Config.BIT_RATE,
                    H263Config.VIDEO_WIDTH,
                    H263Config.VIDEO_HEIGHT).getMediaCodec()
    };

    /**
     * Selected video codec
     */
    private VideoCodec selectedVideoCodec = null;

    /**
     * Video filename to be streamed
     */
    private String filename;

    /**
     * Local RTP port
     */
    private int localRtpPort;

    /**
     * Video format
     */
    private VideoFormat videoFormat;

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
     * Video duration
     */
    private long videoDuration = 0L;

    /**
     * Video width
     */
    private int videoWidth;

    /**
     * Video height
     */
    private int videoHeight;

    /**
     * Video player event listener
     */
    private VideoPlayerEventListener listener;

    /**
     * Surface renderer
     */
    private VideoSurfaceView surface = null;

    /**
     * Media event listeners
     */
    private Vector<IMediaEventListener> listeners = new Vector<IMediaEventListener>();

    /**
     * Temporary connection to reserve the port
     */
    private DatagramConnection temporaryConnection = null;

    /**
     * Extension header ID
     */
    private int orientationHeaderId;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param codec Video codec
     * @param filename Video filename
     * @param listener Video player listener
     */
    public PrerecordedVideoPlayer(String filename, VideoPlayerEventListener listener) {
        this.filename = filename;
        this.listener = listener;

        // Set the local RTP port
        localRtpPort = NetworkRessourceManager.generateLocalRtpPort();
        reservePort(localRtpPort);
    }

    /**
     * Constructor with a codec
     * 
     * @param codec Video codec
     * @param filename Video filename
     * @param listener Video player listener
     */
    public PrerecordedVideoPlayer(VideoCodec codec, String filename,
            VideoPlayerEventListener listener) {
        this.filename = filename;
        this.listener = listener;

        // Set the local RTP port
        localRtpPort = NetworkRessourceManager.generateLocalRtpPort();
        reservePort(localRtpPort);

        // Set the media codec
        setMediaCodec(codec.getMediaCodec());
    }

    /**
     * Constructor withe a codec
     * 
     * @param codec Video codec name
     * @param filename Video filename
     * @param listener Video player listener
     */
    public PrerecordedVideoPlayer(String codec, String filename, VideoPlayerEventListener listener) {
        this.filename = filename;
        this.listener = listener;

        // Set the local RTP port
        localRtpPort = NetworkRessourceManager.generateLocalRtpPort();
        reservePort(localRtpPort);

        // Set the media codec
        for (int i = 0; i < supportedMediaCodecs.length; i++) {
            if (codec.toLowerCase().contains(supportedMediaCodecs[i].getCodecName().toLowerCase())) {
                setMediaCodec(supportedMediaCodecs[i]);
                break;
            }
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
     * Reserve a port
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
     * Release the reserved port
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
     * Set the surface to render video
     *
     * @param surface Video surface
     */
    public void setVideoSurface(VideoSurfaceView surface) {
        this.surface = surface;
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
     * Get video duration
     *
     * @return Milliseconds
     */
    public long getVideoDuration() {
        return videoDuration;
    }

    /**
     * Get video width
     *
     * @return Milliseconds
     */
    public int getVideoWidth() {
        return videoWidth;
    }

    /**
     * Get video height
     *
     * @return Milliseconds
     */
    public int getVideoHeight() {
        return videoHeight;
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

        try {
            // only H263 supported
            int result = NativeH263Decoder.InitParser(filename);
            if (result != 1) {
                notifyPlayerEventError("Video file parser init failed with error code " + result);
                return;
            }

            // Get video properties
            videoDuration = NativeH263Decoder.getVideoLength();
            videoWidth = NativeH263Decoder.getVideoWidth();
            videoHeight = NativeH263Decoder.getVideoHeight();

            // Check video properties
            if ((videoWidth != selectedVideoCodec.getWidth())
                    || (videoHeight != selectedVideoCodec.getHeight())) {
                notifyPlayerEventError("Not supported video format");
                return;
            }
        } catch (UnsatisfiedLinkError e) {
            notifyPlayerEventError(e.getMessage());
            return;
        }

        try {
            // Init the RTP layer
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
            // Close the video file parser. Only H263 supported
            NativeH263Decoder.DeinitParser();
        } catch (UnsatisfiedLinkError e) {
            if (logger.isActivated()) {
                logger.error("Can't deallocate correctly the video file parser", e);
            }
        }

        // Player is closed
        opened = false;
        notifyPlayerEventClosed();
    }

    /**
     * Start the player
     */
    public void start() {
        if (!opened) {
            // Player not opened
            return;
        }

        if (started) {
            // Already started
            return;
        }

        // Start RTP layer
        rtpSender.startSession();

        // Start reading file
        readingThread.start();

        // Player is started
        videoStartTime = SystemClock.uptimeMillis();
        started = true;
        notifyPlayerEventStarted();
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

        // Stop reading file
        try {
            readingThread.interrupt();
        } catch (Exception e) {
        }

        // Player is stopped
        videoStartTime = 0L;
        started = false;
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
        if (selectedVideoCodec == null)
            return null;
        else
            return selectedVideoCodec.getMediaCodec();
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
     * Set media codec
     * 
     * @param mediaCodec Media codec
     */
    public void setMediaCodec(MediaCodec mediaCodec) {
        if (VideoCodec.checkVideoCodec(supportedMediaCodecs, new VideoCodec(mediaCodec))) {
            selectedVideoCodec = new VideoCodec(mediaCodec);
            videoFormat = (VideoFormat) MediaRegistry.generateFormat(mediaCodec.getCodecName());
        } else {
            notifyPlayerEventError("Codec not supported");
        }
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
     * Video reading thread
     */
    private Thread readingThread = new Thread() {
        /**
         * Duration
         */
        private long totalDuration = 0;

        /**
         * End of media
         */
        private boolean endOfMedia = false;

        /**
         * Processing
         */
        public void run() {
            if (rtpInput == null) {
                return;
            }

            Bitmap image = Bitmap.createBitmap(videoWidth, videoHeight, Bitmap.Config.RGB_565);
            int[] decodedFrame = new int[videoWidth * videoHeight];

            while (!endOfMedia && started) {
                // Set timestamp
                long timestamp = System.currentTimeMillis();

                // Get video sample from file
                VideoSample sample = null;
                // Only H263 supported
                sample = NativeH263Decoder.getVideoSample(decodedFrame);
                if (sample != null) {
                    // Display decoded frame
                    image.setPixels(decodedFrame, 0, videoWidth, 0, 0, videoWidth, videoHeight);
                    surface.setImage(image);

                    // Send encoded frame
                    rtpInput.addFrame(sample.data, timestamp);

                    // Update duration
                    totalDuration = totalDuration + sample.timestamp;
                    listener.updateDuration(totalDuration);

                    // Wait before next frame
                    long delta = System.currentTimeMillis() - timestamp;
                    if (delta < sample.timestamp) {
                        try {
                            Thread.sleep(sample.timestamp - delta);
                        } catch (InterruptedException e) {
                        }
                    }
                } else {
                    if (logger.isActivated()) {
                        logger.debug("End of media");
                    }
                    endOfMedia = true;
                }
            }

            // Notify listener
            listener.endOfStream();
        }
    };

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
         */
        public void addFrame(byte[] data, long timestamp) {
            if (fifo != null) {
                fifo.addObject(new MediaSample(data, timestamp));
            }
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
            fifo.close();
            fifo = null;
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
