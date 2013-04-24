package com.orangelabs.rcs.service.api.client.messaging;

/**
 * Message delivery listener
 */
interface IMessageDeliveryListener {
	// Message delivery status
	void handleMessageDeliveryStatus(in String contact, in String msgId, in String status);
}
