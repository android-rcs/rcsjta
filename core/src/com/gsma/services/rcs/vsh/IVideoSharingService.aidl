package com.gsma.services.rcs.vsh;

import com.gsma.services.rcs.IRcsServiceRegistrationListener;
import com.gsma.services.rcs.vsh.IVideoSharing;
import com.gsma.services.rcs.vsh.IVideoSharingListener;
import com.gsma.services.rcs.vsh.IVideoPlayer;
import com.gsma.services.rcs.vsh.IVideoSharingServiceConfiguration;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ICommonServiceConfiguration;

/**
 * Video sharing service API
 */
interface IVideoSharingService {

	boolean isServiceRegistered();

	void addEventListener(IRcsServiceRegistrationListener listener);

	void removeEventListener(IRcsServiceRegistrationListener listener);

	IVideoSharingServiceConfiguration getConfiguration();

	List<IBinder> getVideoSharings();
	
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