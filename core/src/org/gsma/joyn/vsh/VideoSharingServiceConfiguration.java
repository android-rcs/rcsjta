package org.gsma.joyn.vsh;

/**
 * Video sharing service configuration
 * 
 * @author Jean-Marc AUFFRET
 */
public class VideoSharingServiceConfiguration {
	/**
	 * Returns the maximum authorized duration of the video sharing. It returns 0 if
	 * there is no limitation.
	 * 
	 * @return Duration in seconds 
	 */
	public long getMaxTime() {
		return 0; // TODO
	}
}
