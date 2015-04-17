package com.gsma.services.rcs.sharing.geoloc;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.sharing.geoloc.IGeolocSharing;
import com.gsma.services.rcs.sharing.geoloc.IGeolocSharingListener;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.RcsServiceRegistration;

/**
 * Geoloc sharing service API
 */
interface IGeolocSharingService {

	boolean isServiceRegistered();
	
	int getServiceRegistrationReasonCode();

	void addEventListener(IRcsServiceRegistrationListener listener);

	void removeEventListener(IRcsServiceRegistrationListener listener);

	IGeolocSharing getGeolocSharing(in String sharingId);

	IGeolocSharing shareGeoloc(in ContactId contact, in Geoloc geoloc);

	void addEventListener2(in IGeolocSharingListener listener);

	void removeEventListener2(in IGeolocSharingListener listener);

	int getServiceVersion();
	
	ICommonServiceConfiguration getCommonConfiguration();

	void deleteGeolocSharings();

	void deleteGeolocSharings2(in ContactId contact);

	void deleteGeolocSharing(in String sharingId);
}