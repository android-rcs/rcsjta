package org.gsma.joyn.capability;

import org.gsma.joyn.capability.Capabilities;

/**
 * Callback method for new capabilities
 */
interface ICapabilitiesListener {
	void onCapabilitiesReceived(in String contact, in Capabilities capabilities);
}