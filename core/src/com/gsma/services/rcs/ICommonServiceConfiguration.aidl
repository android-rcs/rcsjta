package com.gsma.services.rcs;

import com.gsma.services.rcs.contacts.ContactId;

/**
 * Common service configuration interface
 */

interface ICommonServiceConfiguration {

	int getDefaultMessagingMethod();
	
	int getMessagingUX();
	
	ContactId getMyContactId();
	
	String getMyDisplayName();
	 
	boolean isConfigValid();
	
	void setDefaultMessagingMethod(in int method);
	
	void setMyDisplayName(in String name);
}
