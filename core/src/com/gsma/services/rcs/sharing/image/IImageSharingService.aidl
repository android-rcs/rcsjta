package com.gsma.services.rcs.sharing.image;

import android.net.Uri;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.sharing.image.IImageSharing;
import com.gsma.services.rcs.sharing.image.IImageSharingListener;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.sharing.image.IImageSharingServiceConfiguration;
import com.gsma.services.rcs.RcsServiceRegistration;

/**
 * Image sharing service API
 */
interface IImageSharingService {

	boolean isServiceRegistered();

	int getServiceRegistrationReasonCode();
	
	void addEventListener(IRcsServiceRegistrationListener listener);

	void removeEventListener(IRcsServiceRegistrationListener listener);

	IImageSharingServiceConfiguration getConfiguration();

	IImageSharing getImageSharing(in String sharingId);

	IImageSharing shareImage(in ContactId contact, in Uri file);

	void addEventListener2(in IImageSharingListener listener);

	void removeEventListener2(in IImageSharingListener listener);

	int getServiceVersion();
	
	ICommonServiceConfiguration getCommonConfiguration();

	void deleteImageSharings();

	void deleteImageSharings2(in ContactId contact);

	void deleteImageSharing(in String sharingId);
}