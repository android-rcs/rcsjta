package com.gsma.services.rcs.contacts;

import com.gsma.services.rcs.contacts.JoynContact;

/**
 * Contacts service API
 */
interface IContactsService {
	JoynContact getJoynContact(String contactId);

	List<JoynContact> getJoynContacts();

	List<JoynContact> getJoynContactsOnline();

	List<JoynContact> getJoynContactsSupporting(in String tag);
	
	int getServiceVersion();
}
