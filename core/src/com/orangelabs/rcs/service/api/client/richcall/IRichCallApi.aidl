package com.orangelabs.rcs.service.api.client.richcall;

import com.orangelabs.rcs.service.api.client.richcall.IVideoSharingSession;
import com.orangelabs.rcs.service.api.client.richcall.IImageSharingSession;
import com.orangelabs.rcs.service.api.client.richcall.IGeolocSharingSession;
import com.orangelabs.rcs.service.api.client.media.IMediaPlayer;
import com.orangelabs.rcs.service.api.client.messaging.GeolocPush;

/**
 * Rich call API
 */
interface IRichCallApi {

	// Get the remote phone number involved in the current call
	String getRemotePhoneNumber();

	// Initiate a live video sharing session
	IVideoSharingSession initiateLiveVideoSharing(in String contact, in IMediaPlayer player);

	// Initiate a pre-recorded video sharing session
	IVideoSharingSession initiateVideoSharing(in String contact, in String file, in IMediaPlayer player);

	// Get current video sharing session from its session ID
	IVideoSharingSession getVideoSharingSession(in String id);

	// Get list of current video sharing sessions with a contact
	List<IBinder> getVideoSharingSessionsWith(in String contact);	

	// Initiate an image sharing session
	IImageSharingSession initiateImageSharing(in String contact, in String file, in boolean thumbnail);

	// Get current image sharing session from its session ID
	IImageSharingSession getImageSharingSession(in String id);

	// Get list of current image sharing sessions with a contact
	List<IBinder> getImageSharingSessionsWith(in String contact);	

	// Set multiparty call
	void setMultiPartyCall(in boolean flag);

	// Set call hold
	void setCallHold(in boolean flag);

	// Initiate a geoloc sharing session
	IGeolocSharingSession initiateGeolocSharing(in String contact, in GeolocPush geoloc);

	// Get current geoloc sharing session from its session ID
	IGeolocSharingSession getGeolocSharingSession(in String id);
}


