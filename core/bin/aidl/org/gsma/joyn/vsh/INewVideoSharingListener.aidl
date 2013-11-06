package org.gsma.joyn.vsh;

/**
 * Callback method for new video sharing invitations
 */
interface INewVideoSharingListener {
	void onNewVideoSharing(in String sharingId);
}