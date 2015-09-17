package com.gsma.services.rcs.chat;

import com.gsma.services.rcs.contact.ContactId;

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

	boolean isExpiredDelivery();
}