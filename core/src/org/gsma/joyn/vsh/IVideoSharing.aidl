package org.gsma.joyn.vsh;

import org.gsma.joyn.vsh.IVideoSharingListener;
import org.gsma.joyn.vsh.IVideoRenderer;

/**
 * Video sharing interface
 */
interface IVideoSharing {

	String getSharingId();

	String getRemoteContact();

	String getVideoEncoding();

	String getVideoFormat();
	
	int getState();
	
	void acceptInvitation(IVideoRenderer renderer);

	void rejectInvitation();

	void abortSharing();
	
	void addEventListener(in IVideoSharingListener listener);

	void removeEventListener(in IVideoSharingListener listener);
}
