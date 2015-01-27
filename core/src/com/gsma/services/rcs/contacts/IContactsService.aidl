package com.gsma.services.rcs.contacts;

import com.gsma.services.rcs.contacts.RcsContact;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ICommonServiceConfiguration;

/**
 * Contacts service API
 */
interface IContactsService {

	RcsContact getRcsContact(in ContactId contact);

	List<RcsContact> getRcsContacts();

	List<RcsContact> getRcsContactsOnline();

	List<RcsContact> getRcsContactsSupporting(in String tag);
	
	int getServiceVersion();
	
	ICommonServiceConfiguration getCommonConfiguration();
	
	void blockContact(in ContactId contact);

	void unblockContact(in ContactId contact);
}
