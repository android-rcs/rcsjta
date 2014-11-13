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

import java.util.Random;

import com.gsma.services.rcs.chat.ChatServiceConfiguration;

import android.os.Parcel;
import android.test.AndroidTestCase;

public class ChatServiceConfigurationTest extends AndroidTestCase {

	boolean imAlwaysOn;
	boolean warnSF;
	int chatTimeout;
	int isComposingTimeout;
	int maxGroupChatParticipants;
	int minGroupChatParticipants;
	int maxMsgLengthSingleChat;
	int maxMsgLengthGroupChat;
	int groupChatSubjectMaxLength;
	boolean smsFallback;
	boolean respondToDisplayReports;
	int maxGeolocLabelLength;
	int geolocExpireTim;

	protected void setUp() throws Exception {
		super.setUp();
		Random random = new Random();
		imAlwaysOn = random.nextBoolean();
		warnSF = random.nextBoolean();
		chatTimeout = random.nextInt();
		isComposingTimeout = random.nextInt();
		maxGroupChatParticipants = random.nextInt();
		minGroupChatParticipants = random.nextInt();
		maxMsgLengthGroupChat = random.nextInt();
		maxMsgLengthSingleChat = random.nextInt();
		groupChatSubjectMaxLength = random.nextInt();
		smsFallback = random.nextBoolean();
		respondToDisplayReports = random.nextBoolean();
		maxGeolocLabelLength = random.nextInt();
		geolocExpireTim = random.nextInt();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	private boolean chatServiceConfigurationisEqual(ChatServiceConfiguration conf1, ChatServiceConfiguration conf2) {
		if (conf1.isChatSf() != conf2.isChatSf())
			return false;
		if (conf1.isChatWarnSF() != conf2.isChatWarnSF())
			return false;
		if (conf1.getChatTimeout() != conf2.getChatTimeout())
			return false;
		if (conf1.getIsComposingTimeout() != conf2.getIsComposingTimeout())
			return false;
		if (conf1.getGroupChatMaxParticipants() != conf2.getGroupChatMaxParticipants())
			return false;
		if (conf1.getGroupChatMinParticipants() != conf2.getGroupChatMinParticipants())
			return false;
		if (conf1.getOneToOneChatMessageMaxLength() != conf2.getOneToOneChatMessageMaxLength())
			return false;
		if (conf1.getGroupChatMessageMaxLength() != conf2.getGroupChatMessageMaxLength())
			return false;
		if (conf1.getGroupChatSubjectMaxLength() != conf2.getGroupChatSubjectMaxLength())
			return false;
		if (conf1.isSmsFallback() != conf2.isSmsFallback())
			return false;
		if (conf1.isRespondToDisplayReportsEnabled() != conf2.isRespondToDisplayReportsEnabled())
			return false;
		if (conf1.getGeolocLabelMaxLength() != conf2.getGeolocLabelMaxLength())
			return false;
		if (conf1.getGeolocExpirationTime() != conf2.getGeolocExpirationTime())
			return false;
		return true;
	}

	public void testChatServiceConfiguration() {
		ChatServiceConfiguration config = new ChatServiceConfiguration(imAlwaysOn, warnSF, chatTimeout, isComposingTimeout,
				maxGroupChatParticipants, minGroupChatParticipants, maxMsgLengthSingleChat, maxMsgLengthGroupChat,
				groupChatSubjectMaxLength, smsFallback, respondToDisplayReports, maxGeolocLabelLength, geolocExpireTim);
		Parcel parcel = Parcel.obtain();
		config.writeToParcel(parcel, 0);
		// done writing, now reset parcel for reading
		parcel.setDataPosition(0);
		// finish round trip
		ChatServiceConfiguration createFromParcel = ChatServiceConfiguration.CREATOR.createFromParcel(parcel);
		assertTrue(chatServiceConfigurationisEqual(createFromParcel, config));
	}
}