/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/
package com.orangelabs.rcs.core.ims.service.im.chat;

import static com.orangelabs.rcs.utils.StringUtils.UTF8;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xml.sax.InputSource;

import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.service.im.chat.resourcelist.ResourceListDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.resourcelist.ResourceListParser;
import com.orangelabs.rcs.utils.ContactUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Utilities for ParticipantInfo
 * 
 * @author YPLO6403
 * 
 */
public class ParticipantInfoUtils {

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(ParticipantInfoUtils.class.getSimpleName());

	/**
	 * Create a set of contacts from a set of ParticipantInfo
	 * 
	 * @param participantInfos
	 *            The set of ParticipantInfo
	 * @return The set of contact identifiers
	 */
	public static Set<ContactId> getContacts(Set<ParticipantInfo> participantInfos) {
		Set<ContactId> contacts = new HashSet<ContactId>();
		if (participantInfos == null) {
			return contacts;
		}
		for (ParticipantInfo participant : participantInfos) {
			contacts.add(participant.getContact());
		}
		return contacts;
	}

	/**
	 * Create a set of participant info from XML
	 * 
	 * @param xml
	 *            Resource-list document in XML
	 * @return the set of participants
	 */
	public static Set<ParticipantInfo> parseResourceList(String xml) {
		Set<ParticipantInfo> result = new HashSet<ParticipantInfo>();
		try {
			InputSource pidfInput = new InputSource(new ByteArrayInputStream(
					xml.getBytes(UTF8)));
			ResourceListParser listParser = new ResourceListParser(pidfInput);
			ResourceListDocument resList = listParser.getResourceList();
			if (resList != null) {
				for (String entry : resList.getEntries()) {
					ContactId contact = ContactUtils.createContactId(entry);
					if (!contact.equals(ImsModule.IMS_USER_PROFILE.getUsername())) {
						if (addParticipant(result, contact)) {
							if (logger.isActivated()) {
								logger.debug("Add participant " + contact + " to the list");
							}
						}
					}
				}
			}
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Can't parse resource-list document", e);
			}
		}
		return result;
	}

	/**
	 * Add a participant to a set of ParticipantInfo
	 * 
	 * @param set
	 *            the set of ParticipantInfo
	 * @param participant
	 *            the Participant
	 * @return true if added or if the set is modified
	 */
	public static boolean addParticipant(Set<ParticipantInfo> set, ContactId participant) {
		return addParticipant(set, new ParticipantInfo(participant, ParticipantInfo.Status.UNKNOWN));
	}

	/**
	 * Add a participant to a set of ParticipantInfo
	 * 
	 * @param set
	 *            the set of ParticipantInfo
	 * @param participant
	 *            the participant information to add
	 * @return true if added or if the set is modified
	 */
	public static boolean addParticipant(Set<ParticipantInfo> set, ParticipantInfo participant) {
		if (participant == null)
			return false;
		// Check if participant exists in the set
		ParticipantInfo item = getItem(set, participant.getContact());
		if (item == null) {
			// Insert new participant into the set
			set.add(participant);
			return true;
		} else {
			// Contact already in set: only update status
			if (participant.getStatus() != item.getStatus()) {
				// Update status
				set.remove(item);
				ParticipantInfo updatedParticipant = new ParticipantInfo(participant.getContact(), participant.getStatus());
				set.add(updatedParticipant);
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if contact exists in the set of ParticipantInfo
	 * 
	 * @param set
	 *            the set of ParticipantInfo
	 * @param contact
	 *            the contact identifier
	 * @return the ParticipantInfo item or null if does not exist
	 */
	public static ParticipantInfo getItem(Set<ParticipantInfo> set, ContactId contact) {
		if (set == null || contact == null)
			return null;
		// Iterate through the set to seek for item
		for (ParticipantInfo item : set) {
			if (item != null && item.getContact().equals(contact)) {
				return item;
			}
		}
		return null;
	}

	/**
	 * Convert List of contacts to Set of ParticipantInfo because AIDL does not
	 * support Set type.
	 *
	 * @param contacts list of contact id
	 * @return the corresponding ParticipantInfo set
	 */
	public static Set<ParticipantInfo> getParticipantInfos(List<ContactId> contacts) {
		Set<ParticipantInfo> participants = new HashSet<ParticipantInfo>();
		for (ContactId contact : contacts) {
			ParticipantInfo participant = new ParticipantInfo(contact);
			participants.add(participant);
		}
		return participants;
	}
}
