package com.gsma.services.rcs.ipcall;

/**
 * Callback methods for IP call events
 */
interface IIPCallListener {
	void onCallRinging();

	void onCallStarted();
	
	void onCallAborted();

	void onCallHeld();

	void onCallContinue();

	void onCallError(in int error);
}