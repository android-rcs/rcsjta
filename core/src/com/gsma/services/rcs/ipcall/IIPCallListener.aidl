package com.gsma.services.rcs.ipcall;

import com.gsma.services.rcs.contacts.ContactId;

/**
 * Callback methods for IP call events
 */
interface IIPCallListener {

	void onIPCallStateChanged(in ContactId contact, in String callId, in int state, in int reasonCode);
}