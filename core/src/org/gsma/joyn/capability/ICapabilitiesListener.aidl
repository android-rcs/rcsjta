package org.gsma.joyn.capability;

import org.gsma.joyn.capability.Capabilities;

interface ICapabilitiesListener {
	void handleNewCapabilities(in String contact, in Capabilities capabilities);
}