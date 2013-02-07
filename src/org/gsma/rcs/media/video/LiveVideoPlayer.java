/*
 * Copyright 2013, France Telecom
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *    http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.rcs.media.video;

/**
 * Class LiveVideoPlayer.
 */
public class LiveVideoPlayer extends org.gsma.rcs.media.IMediaPlayer.Stub implements android.hardware.Camera.PreviewCallback {
    /**
     * Creates a new instance of LiveVideoPlayer.
     */
    public LiveVideoPlayer() {
        super();
    }

    /**
     * Creates a new instance of LiveVideoPlayer.
     *
     * @param arg1 The arg1 array.
     */
    public LiveVideoPlayer(org.gsma.rcs.media.MediaCodec[] arg1) {
        super();
    }

    public synchronized void start() {

    }

    public void stop() {

    }

    public void close() {

    }

    /**
     *
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     */
    public void open(String arg1, int arg2) {

    }

    /**
     *
     * @return  The boolean.
     */
    public boolean isStarted() {
        return false;
    }

    /**
     * Adds a listener.
     *
     * @param arg1 The arg1.
     */
    public void addListener(org.gsma.rcs.media.IMediaEventListener arg1) {

    }

    /**
     * Removes a all listeners.
     */
    public void removeAllListeners() {

    }

    /**
     * Sets the media codec.
     *
     * @param arg1 The media codec.
     */
    public void setMediaCodec(org.gsma.rcs.media.MediaCodec arg1) {

    }

    /**
     * Returns the video start time.
     *
     * @return  The video start time.
     */
    public long getVideoStartTime() {
        return 0l;
    }

    /**
     * Returns the local rtp port.
     *
     * @return  The local rtp port.
     */
    public int getLocalRtpPort() {
        return 0;
    }

    /**
     *
     * @return  The boolean.
     */
    public boolean isOpened() {
        return false;
    }

    /**
     * Returns the supported media codecs.
     *
     * @return  The supported media codecs array.
     */
    public org.gsma.rcs.media.MediaCodec[] getSupportedMediaCodecs() {
        return (org.gsma.rcs.media.MediaCodec []) null;
    }

    /**
     * Returns the media codec.
     *
     * @return  The media codec.
     */
    public org.gsma.rcs.media.MediaCodec getMediaCodec() {
        return (org.gsma.rcs.media.MediaCodec) null;
    }

    /**
     * Returns the media codec width.
     *
     * @return  The media codec width.
     */
    public int getMediaCodecWidth() {
        return 0;
    }

    /**
     * Returns the media codec height.
     *
     * @return  The media codec height.
     */
    public int getMediaCodecHeight() {
        return 0;
    }

    /**
     * Sets the scaling factor.
     *
     * @param arg1 The scaling factor.
     */
    public void setScalingFactor(float arg1) {

    }

    /**
     * Sets the scaling factor.
     *
     * @param arg1 The scaling factor.
     */
    public void setScalingFactor(boolean arg1) {

    }

    /**
     *
     * @param arg1 The arg1 array.
     * @param arg2 The arg2.
     */
    public void onPreviewFrame(byte[] arg1, android.hardware.Camera arg2) {

    }

} // end LiveVideoPlayer
