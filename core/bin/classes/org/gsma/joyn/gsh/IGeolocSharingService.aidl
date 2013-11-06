package org.gsma.joyn.gsh;

import org.gsma.joyn.IJoynServiceRegistrationListener;
import org.gsma.joyn.gsh.IGeolocSharing;
import org.gsma.joyn.gsh.IGeolocSharingListener;
import org.gsma.joyn.gsh.INewGeolocSharingListener;
import org.gsma.joyn.chat.Geoloc;

/**
 * Geoloc sharing service API
 */
interface IGeolocSharingService {
	boolean isServiceRegistered();

	void addServiceRegistrationListener(IJoynServiceRegistrationListener listener);

	void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener); 

	List<IBinder> getGeolocSharings();
	
	IGeolocSharing getGeolocSharing(in String sharingId);

	IGeolocSharing shareGeoloc(in String contact, in Geoloc geoloc, in IGeolocSharingListener listener);
	
	void addNewGeolocSharingListener(in INewGeolocSharingListener listener);

	void removeNewGeolocSharingListener(in INewGeolocSharingListener listener);
	
	int getServiceVersion();
}