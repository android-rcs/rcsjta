package com.gsma.services.rcs.ipcall;

import com.gsma.services.rcs.ipcall.IIPCallPlayer;
import com.gsma.services.rcs.ipcall.IIPCallRenderer;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ipcall.VideoCodec;
import com.gsma.services.rcs.ipcall.AudioCodec;

/**
 * IP call interface
 */
interface IIPCall {

	String getCallId();

	ContactId getRemoteContact();

	int getState();

	int getReasonCode();

	int getDirection();
	
	void acceptInvitation(IIPCallPlayer player, IIPCallRenderer renderer);

	void rejectInvitation();

	void abortCall();

	boolean isVideo();

	void addVideo();

	void removeVideo();

	boolean isOnHold();

	void holdCall();

	void continueCall();

	VideoCodec getVideoCodec();

	AudioCodec getAudioCodec();
}
