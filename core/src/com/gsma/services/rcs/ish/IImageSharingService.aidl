package com.gsma.services.rcs.ish;

import com.gsma.services.rcs.IJoynServiceRegistrationListener;
import com.gsma.services.rcs.ish.IImageSharing;
import com.gsma.services.rcs.ish.IImageSharingListener;
import com.gsma.services.rcs.ish.INewImageSharingListener;
import com.gsma.services.rcs.ish.ImageSharingServiceConfiguration;

/**
 * Image sharing service API
 */
interface IImageSharingService {
	boolean isServiceRegistered();

	void addServiceRegistrationListener(IJoynServiceRegistrationListener listener);

	void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener); 

	ImageSharingServiceConfiguration getConfiguration();
    
	List<IBinder> getImageSharings();
	
	IImageSharing getImageSharing(in String sharingId);

	IImageSharing shareImage(in String contact, in String filename, in IImageSharingListener listener);
	
	void addNewImageSharingListener(in INewImageSharingListener listener);

	void removeNewImageSharingListener(in INewImageSharingListener listener);
	
	int getServiceVersion();
}