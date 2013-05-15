package org.gsma.joyn.capability;

import org.gsma.joyn.capability.Capabilities;

/**
 * Capabilities listener
 */
interface ICapabilitiesListener {
	void onCapabilitiesReceived(in String contact, in Capabilities capabilities);
}