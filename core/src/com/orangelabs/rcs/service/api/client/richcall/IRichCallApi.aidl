package com.orangelabs.rcs.service.api.client.richcall;

import com.orangelabs.rcs.service.api.client.richcall.IVideoSharingSession;
import com.orangelabs.rcs.service.api.client.media.IMediaPlayer;

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
}


