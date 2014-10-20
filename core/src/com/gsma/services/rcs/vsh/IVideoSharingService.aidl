package com.gsma.services.rcs.vsh;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.vsh.IVideoSharing;
import com.gsma.services.rcs.vsh.IVideoSharingListener;
import com.gsma.services.rcs.vsh.IVideoPlayer;
import com.gsma.services.rcs.vsh.VideoSharingServiceConfiguration;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Video sharing service API
 */
interface IVideoSharingService {

	boolean isServiceRegistered();

	void addServiceRegistrationListener(IRcsServiceRegistrationListener listener);

	void removeServiceRegistrationListener(IRcsServiceRegistrationListener listener);

	VideoSharingServiceConfiguration getConfiguration();

	List<IBinder> getVideoSharings();
	
	IVideoSharing getVideoSharing(in String sharingId);

	IVideoSharing shareVideo(in ContactId contact, in IVideoPlayer player);

	void addEventListener(in IVideoSharingListener listener);

	void removeEventListener(in IVideoSharingListener listener);

	int getServiceVersion();
}