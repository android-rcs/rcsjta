package org.gsma.joyn.vsh;

/**
 * Video renderer offers an interface to manage the video renderer instance
 * independently of the joyn service. The video renderer is implemented in
 * the application side. The video renderer receives the video streaming over
 * the network in RTP, decodes the video frames and displays the decoded
 * picture on the device screen.
 *  
 * @author Jean-Marc AUFFRET
 */
public class VideoRenderer extends IVideoRenderer.Stub  {
	/**
	 * Opens the renderer and prepares resources (e.g. decoder)
	 * 
	 * @param codec Video codec
	 * @param remoteHost Remote RTP host
	 * @param remotePort Remote RTP port
	 */
	public void open(VideoCodec codec, String remoteHost, int remotePort) {
		// TODO
	}
	
	/**
	 * closes the renderer and deallocates resources
	 */
	public void close() {
		// TODO
	}
	
	/**
	 * Starts the renderer
	 */
	public void start() {
		// TODO
	}
	
	/**
	 * Stops the renderer
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
	 * Returns the list of codecs supported by the renderer
	 * 
	 * @return List of codecs
	 */
	public VideoCodec[] getSupportedCodecs()  {
		// TODO
		return null;
	}
}
