package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.contacts.ContactId;

interface IChatMessage {

	ContactId getContact();

	String getId();

	String getContent();

	String getMimeType();

	int getDirection();

	long getTimestamp();

	long getTimestampSent();

	long getTimestampDelivered();

	long getTimestampDisplayed();

	int getStatus();

	int getReasonCode();

	String getChatId();

	boolean isRead();
}