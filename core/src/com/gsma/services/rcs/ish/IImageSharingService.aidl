package com.gsma.services.rcs.ish;

import com.gsma.services.rcs.IJoynServiceRegistrationListener;
import com.gsma.services.rcs.ish.IImageSharing;
import com.gsma.services.rcs.ish.IImageSharingListener;
import com.gsma.services.rcs.ish.ImageSharingServiceConfiguration;
import com.gsma.services.rcs.contacts.ContactId;

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

	IImageSharing shareImage(in ContactId contact, in Uri file);

	void addEventListener(in IImageSharingListener listener);

	void removeEventListener(in IImageSharingListener listener);

	int getServiceVersion();
}