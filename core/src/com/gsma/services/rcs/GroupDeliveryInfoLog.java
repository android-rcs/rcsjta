/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.gsma.services.rcs;

import android.net.Uri;

/**
 * Delivery info (delivery information on group messages and group file
 * transfers)
 */
public class GroupDeliveryInfoLog {

	/**
	 * Content provider URI for Group Delivery Info
	 */
	public static final Uri CONTENT_URI = Uri
			.parse("content://com.gsma.services.rcs.provider.groupdeliveryinfo/groupdeliveryinfo");

	/**
     * The name of the column containing the status of a group delivery info.
     * <P>Type: INTEGER</P>
     */
	public static final String STATUS = "status";

	/**
     * The name of the column containing the reason code of a group delivery info.
     * <P>Type: INTEGER</P>
     */
	public static final String REASON_CODE = "reason_code";

	/**
     * The name of the column containing the unique ID of the chat message (“msg_id”) or file transfer (“ft_id”).
     * <P>Type: TEXT</P>
     */
	public static final String ID = "id";

	/**
     * The name of the column containing the unique ID of the group chat.
     * <P>Type: TEXT</P>
     */
	public static final String CHAT_ID = "chat_id";

	  /**
     * ContactId formatted number of the inviter of the group chat or the group file transfer.
     * <P>Type: TEXT</P>
     */
	public static final String CONTACT = "contact";

	/**
     * The name of the column containing the time when message or file transfer notification is displayed.
     * <P>Type: LONG</P>
     */
	public static final String TIMESTAMP_DELIVERED = "timestamp_delivered";

	/**
     * The name of the column containing the time when message is displayed or file transfer is done
     * <P>Type: LONG</P>
     */
	public static final String TIMESTAMP_DISPLAYED = "timestamp_displayed";

	/**
	 * Status of the group delivery info
	 */
	public static class Status {

		/**
		 * Delivery notifications were unsupported at the time the message or
		 * file-transfer was sent and no delivery notification has been requested.
		 */
		public static final int UNSUPPORTED = 0;
		/**
		 * The message or file-transfer has not received any delivery report
		 * for the specified contact.
		 */
		public static final int NOT_DELIVERED = 1;

		/**
		 * The message or file-transfer has received a delivery report for the
		 * specified contact
		 */
		public static final int DELIVERED = 2;

		/**
		 * The message or file-transfer has received a displayed report for the
		 * specified contact.
		 */
		public static final int DISPLAYED = 3;

		/**
		 * The message or file-transfer has received a delivery report failure
		 * for the specified contact.
		 */
		public static final int FAILED = 4;
	}

	/**
	 * Group chat delivery status reason
	 */
	public static class ReasonCode {

		/**
		 * No specific reason code specified.
		 */
		public static final int UNSPECIFIED = 0;

		/**
		 * A delivered-error delivery report has been received.
		 */
		public static final int FAILED_DELIVERY = 1;

		/**
		 * A displayed-error delivery report has been received.
		 */
		public static final int FAILED_DISPLAY = 2;
	}
}
