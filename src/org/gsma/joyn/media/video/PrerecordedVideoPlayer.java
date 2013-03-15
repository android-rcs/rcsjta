/*
 * Copyright 2013, France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.joyn.media.video;

import java.lang.String;

/**
 * Class PrerecordedVideoPlayer.
 */
public class PrerecordedVideoPlayer extends org.gsma.joyn.media.IMediaPlayer.Stub {
    /**
     * The supported media codecs array.
     */
    public static org.gsma.joyn.media.MediaCodec[] supportedMediaCodecs;

    /**
     * Creates a new instance of PrerecordedVideoPlayer.
     *
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     */
    public PrerecordedVideoPlayer(String arg1, VideoPlayerEventListener arg2) {
        super();
    }

    /**
     * Creates a new instance of PrerecordedVideoPlayer.
     *
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @param arg3 The arg3.
     */
    public PrerecordedVideoPlayer(VideoCodec arg1, String arg2, VideoPlayerEventListener arg3) {
        super();
    }

    /**
     * Creates a new instance of PrerecordedVideoPlayer.
     *
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @param arg3 The arg3.
     */
    public PrerecordedVideoPlayer(String arg1, String arg2, VideoPlayerEventListener arg3) {
        super();
    }

    public void start() {

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
    public void addListener(org.gsma.joyn.media.IMediaEventListener arg1) {

    }

    /**
     * Removes a all listeners.
     */
    public void removeAllListeners() {

    }

    /**
     * Returns the video width.
     *
     * @return  The video width.
     */
    public int getVideoWidth() {
        return 0;
    }

    /**
     * Returns the video height.
     *
     * @return  The video height.
     */
    public int getVideoHeight() {
        return 0;
    }

    /**
     * Sets the media codec.
     *
     * @param arg1 The media codec.
     */
    public void setMediaCodec(org.gsma.joyn.media.MediaCodec arg1) {

    }

    /**
     * Sets the video surface.
     *
     * @param arg1 The video surface.
     */
    public void setVideoSurface(VideoSurfaceView arg1) {

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
    public org.gsma.joyn.media.MediaCodec[] getSupportedMediaCodecs() {
        return (org.gsma.joyn.media.MediaCodec []) null;
    }

    /**
     * Returns the media codec.
     *
     * @return  The media codec.
     */
    public org.gsma.joyn.media.MediaCodec getMediaCodec() {
        return (org.gsma.joyn.media.MediaCodec) null;
    }

    /**
     * Returns the video duration.
     *
     * @return  The video duration.
     */
    public long getVideoDuration() {
        return 0l;
    }

} // end PrerecordedVideoPlayer
