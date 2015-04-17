package com.gsma.services.rcs.sharing.video;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.sharing.video.IVideoSharing;
import com.gsma.services.rcs.sharing.video.IVideoSharingListener;
import com.gsma.services.rcs.sharing.video.IVideoPlayer;
import com.gsma.services.rcs.sharing.video.IVideoSharingServiceConfiguration;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.RcsServiceRegistration;

/**
 * Video sharing service API
 */
interface IVideoSharingService {

	boolean isServiceRegistered();
	
	int getServiceRegistrationReasonCode();

	void addEventListener(IRcsServiceRegistrationListener listener);

	void removeEventListener(IRcsServiceRegistrationListener listener);

	IVideoSharingServiceConfiguration getConfiguration();

	IVideoSharing getVideoSharing(in String sharingId);

	IVideoSharing shareVideo(in ContactId contact, in IVideoPlayer player);

	void addEventListener2(in IVideoSharingListener listener);

	void removeEventListener2(in IVideoSharingListener listener);

	int getServiceVersion();
	
	ICommonServiceConfiguration getCommonConfiguration();

	void deleteVideoSharings();

	void deleteVideoSharings2(in ContactId contact);

	void deleteVideoSharing(in String sharingId);
}