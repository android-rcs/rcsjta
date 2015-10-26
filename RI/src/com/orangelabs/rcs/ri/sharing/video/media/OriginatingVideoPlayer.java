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
import com.orangelabs.rcs.core.ims.protocol.rtp.RtpException;
import com.orangelabs.rcs.core.ims.protocol.rtp.RtpUtils;
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
import com.orangelabs.rcs.ri.utils.DatagramConnection;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.NetworkRessourceManager;

import android.hardware.Camera;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;

/**
 * Live RTP video player based on H264 QCIF format
 */
public class OriginatingVideoPlayer extends VideoPlayer implements Camera.PreviewCallback,
        RtpStreamListener {
    /**
     * Default video codec
     */
    private VideoCodec mDefaultVideoCodec;

    /**
     * Is player opened
     */
    private boolean mOpened = false;

    /**
     * Is player started
     */
    private boolean mStarted = false;

    private int mLocalRtpPort;

    /**
     * RTP sender session
     */
    private VideoRtpSender mRtpSender;

    /**
     * RTP media input
     */
    private MediaRtpInput mRtpInput;

    /**
     * Video start time
     */
    private long mVideoStartTime = 0L;

    /**
     * Temporary connection to reserve the port
     */
    private DatagramConnection mTemporaryConnection;

    /**
     * NAL SPS
     */
    private byte[] mSps = new byte[0];

    /**
     * NAL PPS
     */
    private byte[] mPps = new byte[0];

    /**
     * Timestamp increment
     */
    private int mTimestampInc;

    private long mTimeStamp = 0;

    /**
     * NAL initialization
     */
    private boolean mNalInit = false;

    /**
     * NAL repeat
     */
    private int mNalRepeat = 0;

    /**
     * NAL repeat MAX value
     */
    private static final int NALREPEATMAX = 20;

    /**
     * Scaling factor for encoding
     */
    private float mScaleFactor = 1;

    /**
     * Mirroring (horizontal and vertival) for encoding
     */
    private boolean mMirroring = false;

    private Orientation mOrientation = Orientation.NONE;

    private int mOrientationHeaderId = RtpUtils.RTP_DEFAULT_EXTENSION_ID;

    private int mCameraId = CameraOptions.BACK.getValue();

    private FrameProcess mFrameProcess;

    private FrameBuffer mFrameBuffer = new FrameBuffer();

    private VideoPlayerListener mEventListener;

    private String mRemoteHost;

    private int mRemotePort;

    private static final String LOGTAG = LogUtils.getTag(OriginatingVideoPlayer.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param eventListener Player event listener
     */
    public OriginatingVideoPlayer(VideoPlayerListener eventListener) {
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
     * Returns the local RTP port
     * 
     * @return Port
     */
    public int getLocalRtpPort() {
        return mLocalRtpPort;
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

        // Init video encoder
        try {
            mTimestampInc = (int) (90000 / mDefaultVideoCodec.getFrameRate());
            NativeH264EncoderParams nativeH264EncoderParams = new NativeH264EncoderParams();

            // Codec dimensions
            nativeH264EncoderParams.setFrameWidth(mDefaultVideoCodec.getWidth());
            nativeH264EncoderParams.setFrameHeight(mDefaultVideoCodec.getHeight());
            nativeH264EncoderParams.setFrameRate(mDefaultVideoCodec.getFrameRate());
            nativeH264EncoderParams.setBitRate(mDefaultVideoCodec.getBitRate());

            // Codec profile and level
            nativeH264EncoderParams.setProfilesAndLevel(mDefaultVideoCodec.getParameters());

            // Codec settings optimization
            nativeH264EncoderParams.setEncMode(NativeH264EncoderParams.ENCODING_MODE_STREAMING);
            nativeH264EncoderParams.setSceneDetection(false);
            nativeH264EncoderParams.setIFrameInterval(15);

            int result = NativeH264Encoder.InitEncoder(nativeH264EncoderParams);
            if (result != 0) {
                // Encoder init has failed
                mEventListener.onPlayerError();
                return;
            }
        } catch (UnsatisfiedLinkError e) {
            // Native encoder not found
            mEventListener.onPlayerError();
            return;
        }

        // Init the RTP layer
        try {
            releasePort();
            mRtpSender = new VideoRtpSender(new H264VideoFormat(), mLocalRtpPort);
            mRtpInput = new MediaRtpInput();
            mRtpInput.open();
            mRtpSender.prepareSession(mRtpInput, mRemoteHost, mRemotePort, this);

        } catch (RtpException e) {
            // RTP failure
            Log.d(LOGTAG, ExceptionUtil.getFullStackTrace(e));
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
        mRtpInput.close();
        mRtpSender.stopSession();

        try {
            // Close the video encoder
            NativeH264Encoder.DeinitEncoder();
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

        // Init NAL
        if (!initNAL()) {
            return;
        }
        mNalInit = false;

        mTimeStamp = 0;
        mNalInit = false;
        mNalRepeat = 0;

        // Start RTP layer
        mRtpSender.startSession();

        // Player is started
        mVideoStartTime = SystemClock.uptimeMillis();
        mStarted = true;
        mFrameProcess = new FrameProcess((int) mDefaultVideoCodec.getFrameRate());
        mFrameProcess.start();
        mEventListener.onPlayerStarted();
    }

    /**
     * Stops the player
     */
    public synchronized void stop() {
        if (!mOpened) {
            // Player not opened
            return;
        }

        if (!mStarted) {
            // Already stopped
            return;
        }

        // Player is stopped
        mVideoStartTime = 0L;
        mStarted = false;
        mFrameProcess.interrupt();
        mEventListener.onPlayerStopped();
    }

    /*---------------------------------------------------------------------*/

    /**
     * Reserve a port.
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
     * Return the video start time
     * 
     * @return Milliseconds
     */
    public long getVideoStartTime() {
        return mVideoStartTime;
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
                mSps = nal;
                return true;
            } else if (type == JavaPacketizer.AVC_NALTYPE_PPS) {
                mPps = nal;
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
        if (mDefaultVideoCodec == null) {
            return H264Config.VIDEO_WIDTH;
        } else {
            return mDefaultVideoCodec.getWidth();
        }
    }

    /**
     * Get video height
     * 
     * @return Height
     */
    public int getVideoHeight() {
        if (mDefaultVideoCodec == null) {
            return H264Config.VIDEO_HEIGHT;
        } else {
            return mDefaultVideoCodec.getHeight();
        }
    }

    /**
     * Set extension header orientation id
     * 
     * @param headerId extension header orientation id
     */
    public void setOrientationHeaderId(int headerId) {
        this.mOrientationHeaderId = headerId;
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
     * Set camera ID
     * 
     * @param cameraId Camera ID
     */
    public void setCameraId(int cameraId) {
        this.mCameraId = cameraId;
    }

    /**
     * Set the mirroring value
     * 
     * @param mirroring New mirroring value
     */
    public void setMirroring(boolean mirroring) {
        this.mMirroring = mirroring;
    }

    /**
     * Notify RTP aborted
     */
    public void rtpStreamAborted() {
        // RTP failure
        mEventListener.onPlayerError();
    }

    /**
     * Preview frame from the camera
     * 
     * @param data Frame
     * @param camera Camera
     */
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (!mStarted) {
            return;
        }
        mFrameBuffer.setData(data);
    };

    /**
     * encode a buffer and add in RTP input
     * 
     * @param data
     */
    private void encode(byte[] data) {
        // Send SPS/PPS if necessary
        mNalRepeat++;
        if (mNalRepeat > NALREPEATMAX) {
            mNalInit = false;
            mNalRepeat = 0;
        }
        if (!mNalInit) {
            mRtpInput.addFrame(mSps, mTimeStamp);
            mTimeStamp += mTimestampInc;

            mRtpInput.addFrame(mPps, mTimeStamp);
            mTimeStamp += mTimestampInc;

            mNalInit = true;
        }

        // Encode frame
        byte[] encoded;
        if (mFrameBuffer.dataSrcWidth != 0 && mFrameBuffer.dataSrcHeight != 0) {
            encoded = NativeH264Encoder.ResizeAndEncodeFrame(data, mTimeStamp, mMirroring,
                    mFrameBuffer.dataSrcWidth, mFrameBuffer.dataSrcHeight);
        } else {
            encoded = NativeH264Encoder.EncodeFrame(data, mTimeStamp, mMirroring,
                    mFrameBuffer.dataScaleFactor);
        }
        int encodeResult = NativeH264Encoder.getLastEncodeStatus();
        if ((encodeResult == 0) && (encoded.length > 0)) {
            VideoOrientation videoOrientation = null;
            if (mOrientationHeaderId > 0) {
                videoOrientation = new VideoOrientation(mOrientationHeaderId,
                        CameraOptions.convert(mCameraId), mOrientation);
            }
            mRtpInput.addFrame(encoded, mTimeStamp, videoOrientation);
            mTimeStamp += mTimestampInc;
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
            while (mStarted) {
                long time = System.currentTimeMillis();

                // Encode
                frameData = mFrameBuffer.getData();
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
            this.dataScaleFactor = mScaleFactor;
            this.dataSrcWidth = mDefaultVideoCodec.getWidth();
            this.dataSrcHeight = mDefaultVideoCodec.getHeight();
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
         * @param videoOrientation
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
            if (fifo != null) {
                return (VideoSample) fifo.getObject();
            } else {
                throw new MediaException("Media input not opened");
            }
        }
    }
}
