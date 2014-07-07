package com.gsma.services.rcs.capability;

import com.gsma.services.rcs.IJoynServiceRegistrationListener;
import com.gsma.services.rcs.capability.Capabilities;
import com.gsma.services.rcs.capability.ICapabilitiesListener;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Capability service API
 */
interface ICapabilityService {
	boolean isServiceRegistered();

	void addServiceRegistrationListener(IJoynServiceRegistrationListener listener);

	void removeServiceRegistrationListener(IJoynServiceRegistrationListener listener); 

	Capabilities getMyCapabilities();

	Capabilities getContactCapabilities(in ContactId contact);

	void requestContactCapabilities(in ContactId contact);

	void requestAllContactsCapabilities();

	void addCapabilitiesListener(in ICapabilitiesListener listener);

	void removeCapabilitiesListener(in ICapabilitiesListener listener);

	void addContactCapabilitiesListener(in ContactId contactId, in ICapabilitiesListener listener);

	void removeContactCapabilitiesListener(in ContactId contactId, in ICapabilitiesListener listener);
	
	int getServiceVersion();
}
