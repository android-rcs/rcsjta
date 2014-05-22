/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/
package com.orangelabs.rcs.core.ims.service.im.chat;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.xml.sax.InputSource;

import com.gsma.services.rcs.chat.ParticipantInfo;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.service.im.chat.resourcelist.ResourceListDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.resourcelist.ResourceListParser;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.StringUtils;
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
	 * Create a set of ParticipantInfo from a list of contacts
	 * 
	 * @param The
	 *            list of contacts
	 * @return The set of ParticipantInfo
	 */
	public static Set<ParticipantInfo> getParticipantInfoFromContacts(List<String> contacts) {
		Set<ParticipantInfo> set = new HashSet<ParticipantInfo>();
		if (contacts == null) {
			return set;
		}
		for (String participant : contacts) {
			ParticipantInfo object = new ParticipantInfo(participant);
			set.add(object);
		}
		return set;
	}

	/**
	 * Create a set of contacts from a set of ParticipantInfo
	 * 
	 * @param contacts
	 *            The set of ParticipantInfo
	 * @return The set of contacts
	 */
	public static Set<String> getContactsFromParticipantInfo(Set<ParticipantInfo> contacts) {
		Set<String> result = new HashSet<String>();
		if (contacts == null) {
			return result;
		}
		for (ParticipantInfo participant : contacts) {
			result.add(participant.getContact());
		}
		return result;
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
			InputSource pidfInput = new InputSource(new ByteArrayInputStream(xml.getBytes()));
			ResourceListParser listParser = new ResourceListParser(pidfInput);
			ResourceListDocument resList = listParser.getResourceList();
			if (resList != null) {
				for (String entry : resList.getEntries()) {
					String number = PhoneUtils.extractNumberFromUri(entry);
					if (!PhoneUtils.compareNumbers(number, ImsModule.IMS_USER_PROFILE.getUsername())) {
						if ((!StringUtils.isEmpty(number))) {
							if (addParticipant(result, number)) {
								if (logger.isActivated()) {
									logger.debug("Add participant " + number + " to the list");
								}
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
	public static boolean addParticipant(Set<ParticipantInfo> set, String participant) {
		String number = PhoneUtils.extractNumberFromUri(participant);
		if (PhoneUtils.isGlobalPhoneNumber(number)) {
			return addParticipant(set, new ParticipantInfo(number, ParticipantInfo.Status.UNKNOWN));
		}
		return false;
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
				item.setStatus(participant.getStatus());
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
	 *            the contact
	 * @return the ParticipantInfo item or null if does not exist
	 */
	public static ParticipantInfo getItem(Set<ParticipantInfo> set, String contact) {
		if (set == null || contact == null)
			return null;
		// Iterate through the set to seek for item
		for (ParticipantInfo item : set) {
			if (item.getContact().equals(contact)) {
				return item;
			}
		}
		return null;
	}

}
