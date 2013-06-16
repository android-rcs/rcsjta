package org.gsma.joyn.vsh;

/**
 * Video player offers an interface to manage the video player instance
 * independently of the joyn service. The video player is implemented in
 * the application side. The video player captures the video from the device
 * camera, encodes the video into the selected format and streams the encoded
 * video frames over the network in RTP.
 *  
 * @author Jean-Marc AUFFRET
 */
public class VideoPlayer extends IVideoPlayer.Stub {
	/**
	 * Opens the player and prepares resources (e.g. encoder, camera)
	 * 
	 * @param codec Video codec
	 * @param remoteHost Remote RTP host
	 * @param remotePort Remote RTP port
	 */
	public void open(VideoCodec codec, String remoteHost, int remotePort) {
		// TODO
	}
	
	/**
	 * closes the player and deallocates resources
	 */
	public void close() {
		// TODO
	}
	
	/**
	 * Starts the player
	 */
	public void start() {
		// TODO
	}
	
	/**
	 * Stops the player
	 */
	public void stop() {
		// TODO		
	}
	
	/**
	 * Returns the local RTP port used to stream video
	 * 
	 * @return Port number
	 */
	public int getLocalRtpPort() {
		return 0; // TODO
	}
	
	
	/**
	 * Returns the list of codecs supported by the player
	 * 
	 * @return List of codecs
	 */
	public VideoCodec[] getSupportedCodecs()  {
		// TODO
		return null;
	}
}
