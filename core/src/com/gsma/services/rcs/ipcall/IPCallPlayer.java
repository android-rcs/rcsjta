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

package com.gsma.services.rcs.ipcall;

import java.util.HashSet;
import java.util.Set;

/**
 * IP call player offers an interface to manage the IP call player instance independently of the rcs
 * service. The IP call player is implemented in the application side. The IP call player captures
 * the audio/video from the device micro/camera, encodes the audio/video into the selected formats,
 * streams the encoded audio samples and video frames over the network in RTP.
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class IPCallPlayer extends IIPCallPlayer.Stub {
    /**
     * IP call player error
     */
    public static class Error {
        /**
         * Internal error
         */
        public final static int INTERNAL_ERROR = 0;

        /**
         * Network connection failed
         */
        public final static int NETWORK_FAILURE = 1;

        private Error() {
        }
    }

    /**
     * IP call player event listeners
     */
    private Set<IIPCallPlayerListener> listeners = new HashSet<IIPCallPlayerListener>();

    /**
     * Constructor
     */
    public IPCallPlayer() {
    }

    /**
     * Opens the player and prepares resources (e.g. encoder, micro, camera)
     * 
     * @param audiocodec Audio codec
     * @param videocodec Video codec
     * @param remoteHost Remote RTP host
     * @param remoteAudioPort Remote audio RTP port
     * @param remoteVideoPort Remote video RTP port
     */
    public abstract void open(AudioCodec audiocodec, VideoCodec videocodec, String remoteHost,
            int remoteAudioPort, int remoteVideoPort);

    /**
     * Closes the player and deallocates resources
     */
    public abstract void close();

    /**
     * Starts the player
     */
    public abstract void start();

    /**
     * Stops the player
     */
    public abstract void stop();

    /**
     * Returns the local RTP port used to stream audio
     * 
     * @return Port number
     */
    public abstract int getLocalAudioRtpPort();

    /**
     * Returns the current audio codec
     * 
     * @return Audo codec
     */
    public abstract AudioCodec getAudioCodec();

    /**
     * Returns the list of audio codecs supported by the player
     * 
     * @return List of audio codecs
     */
    public abstract AudioCodec[] getSupportedAudioCodecs();

    /**
     * Returns the local RTP port used to stream video
     * 
     * @return Port number
     */
    public abstract int getLocalVideoRtpPort();

    /**
     * Returns the current video codec
     * 
     * @return Video codec
     */
    public abstract VideoCodec getVideoCodec();

    /**
     * Returns the list of video codecs supported by the player
     * 
     * @return List of video codecs
     */
    public abstract VideoCodec[] getSupportedVideoCodecs();

    /**
     * Returns the list of player event listeners
     * 
     * @return Listeners
     */
    public Set<IIPCallPlayerListener> getEventListeners() {
        return listeners;
    }

    /**
     * Adds a listener on player events
     * 
     * @param listener Listener
     */
    public void addEventListener(IIPCallPlayerListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener from player events
     * 
     * @param listener Listener
     */
    public void removeEventListener(IIPCallPlayerListener listener) {
        listeners.remove(listener);
    }

    /**
     * Removes all listeners from player events
     */
    public void removeAllEventListeners() {
        listeners.clear();
    }
}
