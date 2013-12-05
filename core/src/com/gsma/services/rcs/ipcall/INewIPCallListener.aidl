package com.gsma.services.rcs.ipcall;

/**
 * Callback method for new IP call invitations
 */
interface INewIPCallListener {
	void onNewCall(in String callId);
}