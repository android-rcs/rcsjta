package org.gsma.joyn.ish;

import org.gsma.joyn.ish.IImageSharing;
import org.gsma.joyn.ish.IImageSharingListener;
import org.gsma.joyn.ish.INewImageSharingListener;

/**
 * Image sharing service API
 */
interface IImageSharingService {
	List<IBinder> getImageSharings();
	
	IImageSharing getImageSharing(in String sharingId);

	IImageSharing shareImage(in String contact, in String filename, in IImageSharingListener listener);
	
	void addNewImageSharingListener(in INewImageSharingListener listener);

	void removeNewImageSharingListener(in INewImageSharingListener listener);
}