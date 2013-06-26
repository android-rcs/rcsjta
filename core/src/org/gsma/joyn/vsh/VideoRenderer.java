package org.gsma.joyn.vsh;

import org.gsma.joyn.JoynServiceException;

/**
 * Video renderer offers an interface to manage the video renderer instance
 * independently of the joyn service. The video renderer is implemented in
 * the application side. The video renderer receives the video streaming over
 * the network in RTP, decodes the video frames and displays the decoded
 * picture on the device screen.
 *  
 * @author Jean-Marc AUFFRET
 */
public class VideoRenderer {
    /**
     * Video renderer interface
     */
	protected IVideoRenderer rendererInf;
    
    /**
     * Constructor
     * 
     * @param rendererInf Video renderer interface
     */
    VideoRenderer(IVideoRenderer rendererInf) {
    	this.rendererInf = rendererInf;
    }

    /**
	 * Opens the renderer and prepares resources (e.g. decoder)
	 * 
	 * @param codec Video codec
	 * @param remoteHost Remote RTP host
	 * @param remotePort Remote RTP port
	 * @throws JoynServiceException
	 */
	public void open(VideoCodec codec, String remoteHost, int remotePort) throws JoynServiceException {
		try {
			rendererInf.open(codec, remoteHost, remotePort);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Closes the renderer and deallocates resources
	 * 
	 * @throws JoynServiceException
	 */
	public void close() throws JoynServiceException {
		try {
			rendererInf.close();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Starts the renderer
	 * 
	 * @throws JoynServiceException
	 */
	public void start() throws JoynServiceException {
		try {
			rendererInf.start();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Stops the renderer
	 * 
	 * @throws JoynServiceException
	 */
	public void stop() throws JoynServiceException {
		try {
			rendererInf.stop();
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
			return rendererInf.getLocalRtpPort();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	
	/**
	 * Returns the list of codecs supported by the renderer
	 * 
	 * @return List of codecs
	 * @throws JoynServiceException
	 */
	public VideoCodec[] getSupportedCodecs() throws JoynServiceException {
		try {
			return rendererInf.getSupportedCodecs();
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Adds a listener on video renderer events
	 * 
	 * @param listener Listener
	 * @throws JoynServiceException
	 */
	public void addEventListener(VideoRendererListener listener) throws JoynServiceException {
		try {
			rendererInf.addEventListener(listener);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}
	
	/**
	 * Removes a listener from video renderer
	 * 
	 * @param listener Listener
	 * @throws JoynServiceException
	 */
	public void removeEventListener(VideoRendererListener listener) throws JoynServiceException {
		try {
			rendererInf.removeEventListener(listener);
		} catch(Exception e) {
			throw new JoynServiceException(e.getMessage());
		}
	}	
}
