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
 * IP call renderer offers an interface to manage the IP call renderer instance independently of the
 * rcs service. The IP call renderer is implemented in the application side. The IP call renderer
 * receives the audio/video streaming over the network in RTP, decodes the audio samples and video
 * frames, plays decoded audio samples and displays the decoded picture on the device screen.
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class IPCallRenderer extends IIPCallRenderer.Stub {
    /**
     * IP call renderer error
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
     * IP call renderer event listeners
     */
    private Set<IIPCallRendererListener> listeners = new HashSet<IIPCallRendererListener>();

    /**
     * Constructor
     */
    public IPCallRenderer() {
    }

    /**
     * Opens the renderer and prepares resources (e.g. decoder)
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
     * Closes the renderer and deallocates resources
     */
    public abstract void close();

    /**
     * Starts the renderer
     */
    public abstract void start();

    /**
     * Stops the renderer
     */
    public abstract void stop();

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
     * Returns the list of renderer event listeners
     * 
     * @return Listeners
     */
    public Set<IIPCallRendererListener> getEventListeners() {
        return listeners;
    }

    /**
     * Adds a listener on renderer events
     * 
     * @param listener Listener
     */
    public void addEventListener(IIPCallRendererListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes a listener from renderer events
     * 
     * @param listener Listener
     */
    public void removeEventListener(IIPCallRendererListener listener) {
        listeners.remove(listener);
    }

    /**
     * Removes all listeners from renderer events
     */
    public void removeAllEventListeners() {
        listeners.clear();
    }
}
