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
package com.gsma.service.rcs.chat;

import java.util.Date;
import java.util.Random;

import android.os.Parcel;
import android.test.AndroidTestCase;

import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;

public class ChatMessageTest extends AndroidTestCase {

	private String messageId;
	private Date receiptAt;
	private String message;
	private ContactId remote;
	
	protected void setUp() throws Exception {
		super.setUp();
		Random random = new Random();
		messageId = String.valueOf (random.nextInt(96) + 32);
		message = String.valueOf (random.nextInt(96) + 32);
		receiptAt = new Date();
		ContactUtils contactUtils = ContactUtils.getInstance(getContext());
		remote = contactUtils.formatContactId("+33123456789");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	private boolean chatMessageIsEqual(ChatMessage chatMessage1, ChatMessage chatMessage2) {
		if (!chatMessage1.getId().equals(chatMessage2.getId()) )
			return false;
		if (!chatMessage1.getMessage().equals(chatMessage2.getMessage())) {
			return false;
		}
		if (!chatMessage1.getReceiptDate().equals(chatMessage2.getReceiptDate())) {
			return false;
		}
		if (chatMessage1.getContact() != null) {
			if (!chatMessage1.getContact().equals(chatMessage2.getContact())) {
				return false;
			}
		} else {
			if (chatMessage2.getContact() != null) {
				return false;
			}
		}
		return true;
	}

	public void testChatMessageContactNull() {
		ChatMessage chatMessage = new ChatMessage(messageId, null, message, receiptAt);
		Parcel parcel = Parcel.obtain();
		chatMessage.writeToParcel(parcel, 0);
		// done writing, now reset parcel for reading
		parcel.setDataPosition(0);
		// finish round trip
		ChatMessage createFromParcel = ChatMessage.CREATOR.createFromParcel(parcel);
		assertTrue(chatMessageIsEqual(createFromParcel, chatMessage));
	}
	
	public void testChatMessageContact() {
		ChatMessage chatMessage = new ChatMessage(messageId, remote, message, receiptAt);
		Parcel parcel = Parcel.obtain();
		chatMessage.writeToParcel(parcel, 0);
		// done writing, now reset parcel for reading
		parcel.setDataPosition(0);
		// finish round trip
		ChatMessage createFromParcel = ChatMessage.CREATOR.createFromParcel(parcel);
		assertTrue(chatMessageIsEqual(createFromParcel, chatMessage));
	}
}