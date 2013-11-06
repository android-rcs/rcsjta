package org.gsma.joyn.ish;

/**
 * Callback method for new image sharing invitations
 */
interface INewImageSharingListener {
	void onNewImageSharing(in String sharingId);
}