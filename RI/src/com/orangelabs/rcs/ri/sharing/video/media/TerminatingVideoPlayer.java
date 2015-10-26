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

import com.gsma.services.rcs.sharing.video.VideoCodec;
import com.gsma.services.rcs.sharing.video.VideoPlayer;

import com.orangelabs.rcs.api.connection.utils.ExceptionUtil;
import com.orangelabs.rcs.core.ims.protocol.rtp.DummyPacketGenerator;
import com.orangelabs.rcs.core.ims.protocol.rtp.RtpException;
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
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.NetworkRessourceManager;

import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;

/**
 * Live video RTP renderer based on H264 QCIF format
 * 
 * @author Jean-Marc AUFFRET
 */
public class TerminatingVideoPlayer extends VideoPlayer implements RtpStreamListener {

    private VideoCodec mDefaultVideoCodec;

    private int mLocalRtpPort;

    /**
     * RTP receiver session
     */
    private VideoRtpReceiver mRtpReceiver;

    /**
     * RTP dummy packet generator
     */
    private DummyPacketGenerator mRtpDummySender;

    /**
     * RTP media output
     */
    private MediaRtpOutput mRtpOutput;

    /**
     * Is player opened
     */
    private boolean mOpened = false;

    /**
     * Is player started
     */
    private boolean mStarted = false;

    private long mVideoStartTime = 0L;

    /**
     * Video surface
     */
    private VideoSurface mSurface;

    /**
     * Temporary connection to reserve the port
     */
    private DatagramConnection mTemporaryConnection;

    private int mOrientationHeaderId = -1;

    private VideoPlayerListener mEventListener;

    private String mRemoteHost;

    private int mRemotePort;

    private static final String LOGTAG = LogUtils.getTag(TerminatingVideoPlayer.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param surface Surface view
     * @param eventListener Player event listener
     */
    public TerminatingVideoPlayer(VideoSurfaceView surface, VideoPlayerListener eventListener) {
        // Set surface view
        mSurface = surface;

        // Set event listener
        mEventListener = eventListener;

        // Set the local RTP port
        mLocalRtpPort = NetworkRessourceManager.generateLocalRtpPort();
        reservePort(mLocalRtpPort);

        // Set the default media codec
        mDefaultVideoCodec = new VideoCodec(H264Config.CODEC_NAME, H264VideoFormat.PAYLOAD,
                H264Config.CLOCK_RATE, 15, 96000, H264Config.QCIF_WIDTH, H264Config.QCIF_HEIGHT,
                H264Config.CODEC_PARAM_PROFILEID + "=" + H264Profile1b.BASELINE_PROFILE_ID + ";"
                        + H264Config.CODEC_PARAM_PACKETIZATIONMODE + "="
                        + JavaPacketizer.H264_ENABLED_PACKETIZATION_MODE);
    }

    /**
     * Set the remote info
     * 
     * @param codec Video codec
     * @param remoteHost Remote RTP host
     * @param remotePort Remote RTP port
     * @param orientationHeaderId Orientation header extension ID. The extension ID is a value
     *            between 1 and 15 arbitrarily chosen by the sender, as defined in RFC5285
     */
    public void setRemoteInfo(VideoCodec codec, String remoteHost, int remotePort,
            int orientationHeaderId) {
        // Set the video codec
        mDefaultVideoCodec = codec;

        // Set remote host and port
        mRemoteHost = remoteHost;
        mRemotePort = remotePort;

        // Set the orientation ID
        mOrientationHeaderId = orientationHeaderId;
    }

    /**
     * Returns the list of codecs supported by the player
     * 
     * @return List of codecs
     */
    public VideoCodec[] getSupportedCodecs() {
        VideoCodec[] list = new VideoCodec[1];
        list[0] = mDefaultVideoCodec;
        return list;
    }

    /**
     * Returns the current codec
     * 
     * @return Codec
     */
    public VideoCodec getCodec() {
        return mDefaultVideoCodec;
    }

    /**
     * Opens the player and prepares resources
     */
    public synchronized void open() {
        if (mOpened) {
            // Already opened
            return;
        }

        try {
            // Init the video decoder
            int result = NativeH264Decoder.InitDecoder();
            if (result != 0) {
                // Decoder init failed
                mEventListener.onPlayerError();
                return;
            }

            // Init the RTP layer
            releasePort();
            mRtpReceiver = new VideoRtpReceiver(mLocalRtpPort);
            mRtpDummySender = new DummyPacketGenerator();
            mRtpOutput = new MediaRtpOutput();
            mRtpOutput.open();
            mRtpReceiver.prepareSession(mRemoteHost, mRemotePort, mOrientationHeaderId, mRtpOutput,
                    new H264VideoFormat(), this);
            mRtpDummySender.prepareSession(mRemoteHost, mRemotePort, mRtpReceiver.getInputStream());
            mRtpDummySender.startSession();
        } catch (RtpException e) {
            // RTP failed
            mEventListener.onPlayerError();
            return;
        }

        // Player is opened
        mOpened = true;
        mEventListener.onPlayerOpened();
    }

    /**
     * Closes the player and deallocates resources
     */
    public synchronized void close() {
        if (!mOpened) {
            // Already closed
            return;
        }

        // Close the RTP layer
        mRtpOutput.close();
        mRtpReceiver.stopSession();
        mRtpDummySender.stopSession();

        try {
            // Close the video decoder
            NativeH264Decoder.DeinitDecoder();
        } catch (UnsatisfiedLinkError e) {
            Log.d(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        }

        // Player is closed
        mOpened = false;
        mEventListener.onPlayerClosed();
    }

    /**
     * Starts the player
     */
    public synchronized void start() {
        if (!mOpened) {
            // Player not opened
            return;
        }

        if (mStarted) {
            // Already started
            return;
        }

        // Start RTP layer
        mRtpReceiver.startSession();

        // Player is started
        mVideoStartTime = SystemClock.uptimeMillis();
        mStarted = true;
        mEventListener.onPlayerStarted();
    }

    /**
     * Stops the player
     */
    public synchronized void stop() {
        if (!mStarted) {
            return;
        }

        // Stop RTP layer
        if (mRtpReceiver != null) {
            mRtpReceiver.stopSession();
        }
        if (mRtpDummySender != null) {
            mRtpDummySender.stopSession();
        }
        if (mRtpOutput != null) {
            mRtpOutput.close();
        }

        // Force black screen
        mSurface.clearImage();

        // Player is stopped
        mStarted = false;
        mVideoStartTime = 0L;
        mEventListener.onPlayerStopped();
    }

    /*---------------------------------------------------------------------*/

    /**
     * Return the video start time
     * 
     * @return Milliseconds
     */
    public long getVideoStartTime() {
        return mVideoStartTime;
    }

    /**
     * Returns the local RTP port
     * 
     * @return Port
     */
    public int getLocalRtpPort() {
        return mLocalRtpPort;
    }

    /**
     * Reserve a port
     * 
     * @param port Port to reserve
     */
    private void reservePort(int port) {
        if (mTemporaryConnection == null) {
            try {
                mTemporaryConnection = NetworkRessourceManager.createDatagramConnection();
                mTemporaryConnection.open(port);
            } catch (IOException e) {
                mTemporaryConnection = null;
            }
        }
    }

    /**
     * Release the reserved port.
     */
    private void releasePort() {
        if (mTemporaryConnection != null) {
            try {
                mTemporaryConnection.close();
            } catch (IOException e) {
                mTemporaryConnection = null;
            }
        }
    }

    /**
     * Is player opened
     * 
     * @return Boolean
     */
    public boolean isOpened() {
        return mOpened;
    }

    /**
     * Is player started
     * 
     * @return Boolean
     */
    public boolean isStarted() {
        return mStarted;
    }

    /**
     * Notify RTP aborted
     */
    public void rtpStreamAborted() {
        mEventListener.onPlayerError();
    }

    /**
     * Set the video orientation
     * 
     * @param orientation Video orientation value (see urn:3gpp:video-orientation)
     */
    public void setOrientation(int orientation) {
        this.mOrientationHeaderId = orientation;
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
        private VideoOrientation videoOrientation = new VideoOrientation(CameraOptions.BACK,
                Orientation.NONE);

        /**
         * Frame dimensions Just 2 - width and height
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
            mRtpDummySender.incomingStarted();

            // Init orientation
            VideoOrientation orientation = ((VideoSample) sample).getVideoOrientation();
            if (orientation != null) {
                this.videoOrientation = orientation;
            }

            int[] decodedFrame = NativeH264Decoder.DecodeAndConvert(sample.getData(),
                    videoOrientation.getOrientation().getValue(), decodedFrameDimensions);
            if (NativeH264Decoder.getLastDecodeStatus() == 0) {
                if ((mSurface != null) && (decodedFrame.length > 0)) {
                    // Init RGB frame with the decoder dimensions
                    if ((rgbFrame.getWidth() != decodedFrameDimensions[0])
                            || (rgbFrame.getHeight() != decodedFrameDimensions[1])) {
                        rgbFrame = Bitmap.createBitmap(decodedFrameDimensions[0],
                                decodedFrameDimensions[1], Bitmap.Config.RGB_565);
                        mEventListener.onPlayerResized(decodedFrameDimensions[0],
                                decodedFrameDimensions[1]);
                    }

                    // Set data in image
                    rgbFrame.setPixels(decodedFrame, 0, decodedFrameDimensions[0], 0, 0,
                            decodedFrameDimensions[0], decodedFrameDimensions[1]);
                    mSurface.setImage(rgbFrame);
                }
            }
        }
    }

    /**
     * Set the surface
     * 
     * @param surface
     */
    public void setSurface(VideoSurface surface) {
        this.mSurface = surface;
    }
}
