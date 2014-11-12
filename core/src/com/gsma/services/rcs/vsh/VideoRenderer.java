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
 * Video renderer offers an interface to manage the video renderer instance
 * independently of the rcs service. The video renderer is implemented in
 * the application side. The video renderer receives the video streaming over
 * the network in RTP, decodes the video frames and displays the decoded
 * picture on the device screen.
 *  
 * @author Jean-Marc AUFFRET
 */
public abstract class VideoRenderer extends IVideoRenderer.Stub {
    /**
     * Video renderer error
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
     * Video renderer event listeners
     */
    private Set<IVideoRendererListener> listeners = new HashSet<IVideoRendererListener>();    
    
    /**
     * Constructor
     */
    public VideoRenderer() {
    }

    /**
	 * Opens the renderer and prepares resources (e.g. decoder)
	 * 
	 * @param codec Video codec
	 * @param remoteHost Remote RTP host
	 * @param remotePort Remote RTP port
	 */
	public abstract void open(VideoCodec codec, String remoteHost, int remotePort);
	
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
	 * Returns the local RTP port used to stream video
	 * 
	 * @return Port number
	 */
	public abstract int getLocalRtpPort();
	
	/**
	 * Returns the current codec
	 * 
	 * @return Codec
	 */
	public abstract VideoCodec getCodec();
	
	/**
	 * Returns the list of codecs supported by the renderer
	 * 
	 * @return List of codecs
	 */
	public abstract VideoCodec[] getSupportedCodecs();
	
	/**
	 * Returns the list of renderer event listeners
	 * 
	 * @return Listeners
	 */
	public Set<IVideoRendererListener> getEventListeners() {
		return listeners;
	}
	
	/**
	 * Adds a listener on video renderer events
	 * 
	 * @param listener Listener
	 */
	public void addEventListener(IVideoRendererListener listener) {
		listeners.add(listener);
	}

	/**
	 * Removes a listener on video renderer events
	 * 
	 * @param listener Listener
	 */
	public void removeEventListener(IVideoRendererListener listener) {
		listeners.remove(listener);
	}	

	/**
	 * Removes all listeners from renderer events
	 */
	public void removeAllEventListeners() {
		listeners.clear();
	}	
}
