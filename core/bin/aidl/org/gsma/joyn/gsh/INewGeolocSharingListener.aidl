package org.gsma.joyn.gsh;

/**
 * Callback method for new geoloc sharing invitations
 */
interface INewGeolocSharingListener {
	void onNewGeolocSharing(in String sharingId);
}