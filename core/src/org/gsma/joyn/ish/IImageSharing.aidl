package org.gsma.joyn.ish;

import org.gsma.joyn.ish.IImageSharingListener;

/**
 * Image sharing interface
 */
interface IImageSharing {

	String getSharingId();

	String getRemoteContact();

	String getFileName();

	long getFileSize();

	String getFileType();

	int getState();
	
	int getDirection();
		
	void acceptInvitation();

	void rejectInvitation();

	void abortSharing();
	
	void addEventListener(in IImageSharingListener listener);

	void removeEventListener(in IImageSharingListener listener);
	
	int getServiceVersion();
}
