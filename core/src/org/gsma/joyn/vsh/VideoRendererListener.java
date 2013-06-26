package org.gsma.joyn.vsh;

/**
 * This class offers callback methods on video renderer events
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class VideoRendererListener extends IVideoRendererListener.Stub {
	/**
	 * Callback called when the renderer is opened
	 */
	public abstract void onRendererOpened();

	/**
	 * Callback called when the renderer is started
	 */
	public abstract void onRendererStarted();

	/**
	 * Callback called when the renderer is stopped
	 */
	public abstract void onRendererStopped();

	/**
	 * Callback called when the renderer is closed
	 */
	public abstract void onRendererClosed();

	/**
	 * Callback called when the renderer has failed
	 * 
	 * @param error Error
	 */
	public abstract void onRendererError(int error);
}
