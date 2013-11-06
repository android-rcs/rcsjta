package org.gsma.joyn.vsh;

import org.gsma.joyn.vsh.IVideoSharingListener;
import org.gsma.joyn.vsh.IVideoRenderer;
import org.gsma.joyn.vsh.VideoCodec;

/**
 * Video sharing interface
 */
interface IVideoSharing {

	String getSharingId();

	String getRemoteContact();

	VideoCodec getVideoCodec();

	int getState();

	int getDirection();
	
	void acceptInvitation(IVideoRenderer renderer);

	void rejectInvitation();

	void abortSharing();
	
	void addEventListener(in IVideoSharingListener listener);

	void removeEventListener(in IVideoSharingListener listener);
}
