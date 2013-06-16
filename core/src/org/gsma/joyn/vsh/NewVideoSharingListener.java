package org.gsma.joyn.vsh;

/**
 * New video sharing invitation event listener
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class NewVideoSharingListener extends INewVideoSharingListener.Stub {
	/**
	 * Callback called when a new Video share invitation has been received
	 * 
	 * @param sharingId Sharing ID
	 */
	public abstract void onNewVideoSharing(String sharingId);

}
