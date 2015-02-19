package com.gsma.services.rcs.contact;

import com.gsma.services.rcs.contact.RcsContact;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.ICommonServiceConfiguration;

/**
 * Contacts service API
 */
interface IContactService {

	RcsContact getRcsContact(in ContactId contact);

	List<RcsContact> getRcsContacts();

	List<RcsContact> getRcsContactsOnline();

	List<RcsContact> getRcsContactsSupporting(in String tag);
	
	int getServiceVersion();
	
	ICommonServiceConfiguration getCommonConfiguration();
	
	void blockContact(in ContactId contact);

	void unblockContact(in ContactId contact);
}
