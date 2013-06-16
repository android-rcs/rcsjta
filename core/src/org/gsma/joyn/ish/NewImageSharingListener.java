package org.gsma.joyn.ish;

/**
 * New image sharing invitation event listener
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class NewImageSharingListener extends INewImageSharingListener.Stub {
	/**
	 * Callback called when a new image share invitation has been received
	 * 
	 * @param sharingId Sharing ID
	 */
	public abstract void onNewImageSharing(String sharingId);
}
