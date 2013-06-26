package org.gsma.joyn.vsh;

/**
 * This class offers callback methods on video player events
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class VideoPlayerListener extends IVideoPlayerListener.Stub {
	/**
	 * Callback called when the player is opened
	 */
	public abstract void onPlayerOpened();

	/**
	 * Callback called when the player is started
	 */
	public abstract void onPlayerStarted();

	/**
	 * Callback called when the player is stopped
	 */
	public abstract void onPlayerStopped();

	/**
	 * Callback called when the player is closed
	 */
	public abstract void onPlayerClosed();

	/**
	 * Callback called when the player has failed
	 * 
	 * @param error Error
	 */
	public abstract void onPlayerError(int error);
}
