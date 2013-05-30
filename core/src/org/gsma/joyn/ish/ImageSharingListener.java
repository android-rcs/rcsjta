package org.gsma.joyn.ish;

/**
 * Image sharing event listener
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class ImageSharingListener extends IImageSharingListener.Stub {
	/**
	 * Callback called when the sharing is started
	 */
	public abstract void onSharingStarted();
	
	/**
	 * Callback called when the sharing has been aborted
	 */
	public abstract void onSharingAborted();

	/**
	 * Callback called when the sharing has failed
	 * 
	 * @param error Error
	 * @see ImageSharing.Error
	 */
	public abstract void onSharingError(int error);
	
	/**
	 * Callback called during the sharing progress
	 * 
	 * @param currentSize Current transfered size in bytes
	 * @param totalSize Total size to transfer in bytes
	 */
	public abstract void onSharingProgress(long currentSize, long totalSize);

	/**
	 * Callback called when the image has been shared
	 * 
	 * @param filename Filename including the path of the transfered file
	 */
	public abstract void onImageShared(String filename);
}
