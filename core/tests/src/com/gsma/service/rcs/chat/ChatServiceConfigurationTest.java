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
		assertEquals(createFromParcel.isChatSf(), config.isChatSf());
		assertEquals(createFromParcel.isChatWarnSF(), config.isChatWarnSF());
		assertEquals(createFromParcel.getChatTimeout(), config.getChatTimeout());
		assertEquals(createFromParcel.getIsComposingTimeout(), config.getIsComposingTimeout());
		assertEquals(createFromParcel.getGroupChatMaxParticipants(), config.getGroupChatMaxParticipants());
		assertEquals(createFromParcel.getGroupChatMinParticipants(), config.getGroupChatMinParticipants());
		assertEquals(createFromParcel.getGroupChatMessageMaxLength(), config.getGroupChatMessageMaxLength());
		assertEquals(createFromParcel.getGroupChatSubjectMaxLength(), config.getGroupChatSubjectMaxLength());
		assertEquals(createFromParcel.getOneToOneChatMessageMaxLength(), config.getOneToOneChatMessageMaxLength());
		assertEquals(createFromParcel.isSmsFallback(), config.isSmsFallback());
		assertEquals(createFromParcel.isRespondToDisplayReportsEnabled(), config.isRespondToDisplayReportsEnabled());
		assertEquals(createFromParcel.getGeolocLabelMaxLength(), config.getGeolocLabelMaxLength());
		assertEquals(createFromParcel.getGeolocExpirationTime(), config.getGeolocExpirationTime());
	}
}