package org.gsma.joyn.vsh;

/**
 * Image sharing event listener
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class VideoSharingListener extends IVideoSharingListener.Stub {
	/**
	 * Callback called when the sharing is started
	 */
	public abstract void onSharingStarted();
	
	/**
	 * Callback called when the sharing has been aborted or terminated
	 */
	public abstract void onSharingAborted();

	/**
	 * Callback called when the sharing has failed
	 * 
	 * @param error Error
	 * @see VideoSharing.Error
	 */
	public abstract void onSharingError(int error);
}
