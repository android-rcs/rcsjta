package com.gsma.services.rcs.ish;

import android.net.Uri;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.ish.IImageSharing;
import com.gsma.services.rcs.ish.IImageSharingListener;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.ish.IImageSharingServiceConfiguration;

/**
 * Image sharing service API
 */
interface IImageSharingService {

	boolean isServiceRegistered();

	void addEventListener(IRcsServiceRegistrationListener listener);

	void removeEventListener(IRcsServiceRegistrationListener listener);

	IImageSharingServiceConfiguration getConfiguration();
    
	List<IBinder> getImageSharings();
	
	IImageSharing getImageSharing(in String sharingId);

	IImageSharing shareImage(in ContactId contact, in Uri file);

	void addEventListener2(in IImageSharingListener listener);

	void removeEventListener2(in IImageSharingListener listener);

	int getServiceVersion();
	
	ICommonServiceConfiguration getCommonConfiguration();
}