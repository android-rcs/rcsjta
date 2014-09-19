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

public class RcsCommon {

	/**
	 * Read status of the message
	 */
	public static class ReadStatus {
		/**
		 * The message has not yet been displayed in the UI.
		 */
		public static final int UNREAD = 0;

		/**
		 * The message has been displayed in the UI.
		 */
		public static final int READ = 1;
	}

	/**
	 * Direction of the message
	 */
	public static class Direction {
		/**
		 * Incoming message
		 */
		public static final int INCOMING = 0;

		/**
		 * Outgoing message
		 */
		public static final int OUTGOING = 1;

		/**
		 * Irrelevant or not applicable (e.g. for a system message)
		 */
		public static final int IRRELEVANT = 2;
	}
}
