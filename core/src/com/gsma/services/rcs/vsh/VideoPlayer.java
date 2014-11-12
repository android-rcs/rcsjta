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
package com.gsma.services.rcs.vsh;

import java.util.HashSet;
import java.util.Set;


/**
 * Video player offers an interface to manage the video player instance
 * independently of the rcs service. The video player is implemented in
 * the application side. The video player captures the video from the device
 * camera, encodes the video into the selected format and streams the encoded
 * video frames over the network in RTP.
 *  
 * @author Jean-Marc AUFFRET
 */
public abstract class VideoPlayer extends IVideoPlayer.Stub {
    /**
     * Video player error
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
     * Video player event listeners
     */
    private Set<IVideoPlayerListener> listeners = new HashSet<IVideoPlayerListener>();
    
    /**
     * Constructor
     */
    public VideoPlayer() {
    }

    /**
	 * Opens the player and prepares resources (e.g. encoder, camera)
	 * 
	 * @param codec Video codec
	 * @param remoteHost Remote RTP host
	 * @param remotePort Remote RTP port
	 */
	public abstract void open(VideoCodec codec, String remoteHost, int remotePort);
	
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
	 * Returns the local RTP port used to stream video
	 * 
	 * @return Port number
	 */
	public abstract int getLocalRtpPort();
	
	/**
	 * Returns the list of codecs supported by the player
	 * 
	 * @return List of codecs
	 */
	public abstract VideoCodec[] getSupportedCodecs();
	
	/**
	 * Returns the current codec
	 * 
	 * @return Codec
	 */
	public abstract VideoCodec getCodec();

	/**
	 * Returns the list of player event listeners
	 * 
	 * @return Listeners
	 */
	public Set<IVideoPlayerListener> getEventListeners() {
		return listeners;
	}

	/**
	 * Adds a listener on video player events
	 * 
	 * @param listener Listener
	 */
	public void addEventListener(IVideoPlayerListener listener) {
		listeners.add(listener);
	}

	/**
	 * Removes a listener on video player events
	 * 
	 * @param listener Listener
	 */
	public void removeEventListener(IVideoPlayerListener listener) {
		listeners.remove(listener);
	}
	
	/**
	 * Removes all listeners from player events
	 */
	public void removeAllEventListeners() {
		listeners.clear();
	}
}
