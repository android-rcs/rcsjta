package org.gsma.joyn.capability;

import org.gsma.joyn.IJoynServiceRegistrationListener;
import org.gsma.joyn.capability.Capabilities;
import org.gsma.joyn.capability.ICapabilitiesListener;

/**
 * Capability service API
 */
interface ICapabilityService {
	boolean isServiceRegistered();

	void addServiceRegistrationListener(IJoynServiceRegistrationListener listener);

	void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener); 

	Capabilities getMyCapabilities();

	Capabilities getContactCapabilities(in String contact);

	void requestContactCapabilities(in String contact);

	void requestAllContactsCapabilities();

	void addCapabilitiesListener(in ICapabilitiesListener listener);

	void removeCapabilitiesListener(in ICapabilitiesListener listener);

	void addContactCapabilitiesListener(in String contact, in ICapabilitiesListener listener);

	void removeContactCapabilitiesListener(in String contact, in ICapabilitiesListener listener);
	
	int getServiceVersion();
}
