package com.gsma.services.rcs.gsh;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.gsh.IGeolocSharing;
import com.gsma.services.rcs.gsh.IGeolocSharingListener;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ICommonServiceConfiguration;

/**
 * Geoloc sharing service API
 */
interface IGeolocSharingService {

	boolean isServiceRegistered();

	void addEventListener(IRcsServiceRegistrationListener listener);

	void removeEventListener(IRcsServiceRegistrationListener listener);

	List<IBinder> getGeolocSharings();
	
	IGeolocSharing getGeolocSharing(in String sharingId);

	IGeolocSharing shareGeoloc(in ContactId contact, in Geoloc geoloc);

	void addEventListener2(in IGeolocSharingListener listener);

	void removeEventListener2(in IGeolocSharingListener listener);

	int getServiceVersion();
	
	ICommonServiceConfiguration getCommonConfiguration();
}