package com.gsma.services.rcs.gsh;

/**
 * Callback method for new geoloc sharing invitations
 */
interface INewGeolocSharingListener {
	void onNewGeolocSharing(in String sharingId);
}