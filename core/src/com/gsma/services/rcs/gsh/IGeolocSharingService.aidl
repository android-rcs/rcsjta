package com.gsma.services.rcs.gsh;

import com.gsma.services.rcs.IJoynServiceRegistrationListener;
import com.gsma.services.rcs.gsh.IGeolocSharing;
import com.gsma.services.rcs.gsh.IGeolocSharingListener;
import com.gsma.services.rcs.gsh.INewGeolocSharingListener;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Geoloc sharing service API
 */
interface IGeolocSharingService {
	boolean isServiceRegistered();

	void addServiceRegistrationListener(IJoynServiceRegistrationListener listener);

	void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener); 

	List<IBinder> getGeolocSharings();
	
	IGeolocSharing getGeolocSharing(in String sharingId);

	IGeolocSharing shareGeoloc(in ContactId contact, in Geoloc geoloc, in IGeolocSharingListener listener);
	
	void addNewGeolocSharingListener(in INewGeolocSharingListener listener);

	void removeNewGeolocSharingListener(in INewGeolocSharingListener listener);
	
	int getServiceVersion();
}