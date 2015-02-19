package com.gsma.services.rcs.sharing.video;

import com.gsma.services.rcs.sharing.video.IVideoPlayer;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.video.VideoDescriptor;

/**
 * Video sharing interface
 */
interface IVideoSharing {

	String getSharingId();

	ContactId getRemoteContact();

	int getState();

	int getReasonCode();

	int getDirection();
	
	void acceptInvitation(IVideoPlayer player);

	void rejectInvitation();

	void abortSharing();
	
	String getVideoEncoding();

	long getTimestamp();

	long getDuration();
	
	VideoDescriptor getVideoDescriptor();
}
