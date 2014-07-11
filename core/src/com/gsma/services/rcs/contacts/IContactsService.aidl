package com.gsma.services.rcs.contacts;

import com.gsma.services.rcs.contacts.JoynContact;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Contacts service API
 */
interface IContactsService {
	JoynContact getJoynContact(in ContactId contact);

	List<JoynContact> getJoynContacts();

	List<JoynContact> getJoynContactsOnline();

	List<JoynContact> getJoynContactsSupporting(in String tag);
	
	int getServiceVersion();
}
