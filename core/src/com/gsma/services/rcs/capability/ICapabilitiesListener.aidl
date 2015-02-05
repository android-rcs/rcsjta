package com.gsma.services.rcs.capability;
import com.gsma.services.rcs.contacts.ContactId;

import com.gsma.services.rcs.capability.Capabilities;

/**
 * Callback method for new capabilities
 */
interface ICapabilitiesListener {
	void onCapabilitiesReceived(in ContactId contact, in Capabilities capabilities);
}