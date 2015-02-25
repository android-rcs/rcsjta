package com.gsma.rcs.service.ipcalldraft;

import com.gsma.rcs.service.ipcalldraft.IIPCallPlayer;
import com.gsma.rcs.service.ipcalldraft.IIPCallRenderer;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.rcs.service.ipcalldraft.VideoCodec;
import com.gsma.rcs.service.ipcalldraft.AudioCodec;

/**
 * IP call interface
 */
interface IIPCall {

	String getCallId();

	ContactId getRemoteContact();

	int getState();

	int getReasonCode();

	int getDirection();

	long getTimestamp();

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
