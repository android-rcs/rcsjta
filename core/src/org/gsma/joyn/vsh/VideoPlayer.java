package org.gsma.joyn.vsh;

import java.util.HashSet;
import java.util.Set;

import org.gsma.joyn.JoynServiceException;


/**
 * Video player offers an interface to manage the video player instance
 * independently of the joyn service. The video player is implemented in
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
	 * 
	 * @throws JoynServiceException
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
	 * Returns the list of event listeners
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
		addEventListener(listener);
	}

	/**
	 * Removes a listener from video player
	 * 
	 * @param listener Listener
	 */
	public void removeEventListener(IVideoPlayerListener listener) {
		removeEventListener(listener);
	}
	
	/**
	 * Removes all listeners from video player
	 */
	public void removeAllEventListeners() {
		listeners.clear();
	}
}
