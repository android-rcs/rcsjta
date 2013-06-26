package org.gsma.joyn.vsh;

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
public class VideoPlayer {
    /**
     * Video player interface
     */
    protected IVideoPlayer playerInf;
    
    /**
     * Constructor
     * 
     * @param playerInf Video player interface
     */
    VideoPlayer(IVideoPlayer playerInf) {
    	this.playerInf = playerInf;
    }

    /**
	 * Opens the player and prepares resources (e.g. encoder, camera)
	 * 
	 * @param codec Video codec
	 * @param remoteHost Remote RTP host
	 * @param remotePort Remote RTP port
	 * @throws JoynServiceException
	 */
	public void open(VideoCodec codec, String remoteHost, int remotePort) throws JoynServiceException {
		try {
			playerInf.open(codec, remoteHost, remotePort);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Closes the player and deallocates resources
	 * 
	 * @throws JoynServiceException
	 */
	public void close() throws JoynServiceException {
		try {
			playerInf.close();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Starts the player
	 * 
	 * @throws JoynServiceException
	 */
	public void start() throws JoynServiceException {
		try {
			playerInf.start();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Stops the player
	 * 
	 * @throws JoynServiceException
	 */
	public void stop() throws JoynServiceException {
		try {
			playerInf.stop();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Returns the local RTP port used to stream video
	 * 
	 * @return Port number
	 * @throws JoynServiceException
	 */
	public int getLocalRtpPort() throws JoynServiceException {
		try {
			return playerInf.getLocalRtpPort();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	
	/**
	 * Returns the list of codecs supported by the player
	 * 
	 * @return List of codecs
	 * @throws JoynServiceException
	 */
	public VideoCodec[] getSupportedCodecs() throws JoynServiceException {
		try {
			return playerInf.getSupportedCodecs();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Adds a listener on video player events
	 * 
	 * @param listener Listener
	 * @throws JoynServiceException
	 */
	public void addEventListener(VideoPlayerListener listener) throws JoynServiceException {
		try {
			playerInf.addEventListener(listener);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Removes a listener from video player
	 * 
	 * @param listener Listener
	 * @throws JoynServiceException
	 */
	public void removeEventListener(VideoPlayerListener listener) throws JoynServiceException {
		try {
			playerInf.removeEventListener(listener);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
}
