package org.gsma.joyn.contacts;

import org.gsma.joyn.contacts.JoynContact;

/**
 * Contacts service API
 */
interface IContactsService {
	List<JoynContact> getJoynContacts();

	List<JoynContact> getJoynContactsOnline();

	List<JoynContact> getJoynContactsSupporting(in String tag);
}
