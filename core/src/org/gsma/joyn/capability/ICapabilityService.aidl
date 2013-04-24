package org.gsma.joyn.capability;

import org.gsma.joyn.capability.Capabilities;
import org.gsma.joyn.capability.ICapabilitiesListener;

/**
 * Capability service API
 */
interface ICapabilityService {
	Capabilities getMyCapabilities();

	Capabilities getContactCapabilities(in String contact);

	void requestCapabilities(in String contact);

	void addCapabilitiesListener(in ICapabilitiesListener listener);

	void removeCapabilitiesListener(in ICapabilitiesListener listener);

	void addContactCapabilitiesListener(in String contact, in ICapabilitiesListener listener);

	void removeContactCapabilitiesListener(in String contact, in ICapabilitiesListener listener);
}
