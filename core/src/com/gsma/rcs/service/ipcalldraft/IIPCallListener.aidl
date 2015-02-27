package com.gsma.rcs.service.ipcalldraft;

import com.gsma.services.rcs.contact.ContactId;

/**
 * Callback methods for IP call events
 */
interface IIPCallListener {

	void onIPCallStateChanged(in ContactId contact, in String callId, in int state, in int reasonCode);
}