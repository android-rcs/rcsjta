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

package com.orangelabs.rcs.utils;

import java.util.UUID;

/**
 * Unique identifier generator
 *
 * @author JF. Jestin
 */
public class IdGenerator {
	
	/**
	 * Every 6 bit are coded using this table (see base64 encoding standard,
	 * except '/' which was replaced by '_')
	 */
	private final static char[] CODE_TABLE =
		{
	    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
	    'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
	    'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
	    'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
	    'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
	    'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
	    'w', 'x', 'y', 'z', '0', '1', '2', '3',
	    '4', '5', '6', '7', '8', '9', '+', '_'
		};

	/**
	 * The counter which get a new value for each subsequent call of increment()
	 */
	private static int cyclicCounter = 0;

	/**
	 * How much digits are used for coding the counter, 3 means 3*6 bits,
	 * that are 18 bits resulting in a max. value of 262144 for the counter which
	 * seems to be enough.
	 */
	private final static int N_COUNTERS_CHARS = 3;

	/**
	 * The number of generated characters for the uniq identifier<p>
	 * System.currentTimeMillis() returns long, but only 42 bit are taken, which
	 * seems to be enough for more than 100 years after 1970, these 42 bits need
	 * 7 chars for their coding, the number of chars for the counter is added:
	 * <pre>maxDigit = 7 + N_COUNTERS_CHARS;</pre>
	 */
	private final static int MAX_DIGIT = 7 + N_COUNTERS_CHARS;

	/**
	 * The central method to increment the cyclic counter, synchronized to achieve
	 * a unique value for each subsequent call<p>
	 * there is no problem if the counter reaches the maximum counter value,
	 * defined by N_COUNTERS_CHARS, only the right number of bits are taken into
	 * account for generating the output
	 *
	 * @return The new counter value
	 */
	private static synchronized int increment() {
		return cyclicCounter++;
	}

	/**
	 * Encode twoumbers into a textual representation using a base64 like coding
	 * table.
	 * @param time This should be System.currentTimeMillis
	 * @param counter The counter value, it is coded into N_COUNTERS_CHARS chars, ie for
	 *        3 chars we have 18 bit and a max. range of 262144
	 * @result The converted String containing (7 + N_COUNTERS_CHARS) characters
	 */
	private static String encode64(long time, int counter) {
		char[] encodedData = new char[MAX_DIGIT];
		int i, idx;

		for (i = 0; i < 7; i++) {
			idx = (int) time & 63;
			time >>= 6;
			encodedData[i] = CODE_TABLE[idx];
		}

		for (; i < MAX_DIGIT; i++) {
			idx = counter & 63;
			counter >>= 6;
			encodedData[i] = CODE_TABLE[idx];
		}

		return new String(encodedData);
	}

	/**
	 * Get a unique local identifier for each subsequent call
	 *
	 * @return Unique identifier
	 */
	public static synchronized String getIdentifier() {
		long time = System.currentTimeMillis();

		int counter = -1;
		if (N_COUNTERS_CHARS > 0)
			counter = increment();

		return encode64(time, counter);
	}
	
	/**
	 * Generate a new unique and random message ID (eg. for SIP or MSRP)
	 * 
	 * @return the messageID
	 * 
	 *         <p>
	 *         <b>Note:</b><br />
	 *         The message ID is a string of 32 characters in the range [a-f0-9] in compliance with RFC 4975
	 *         </p>
	 */
	public static synchronized String generateMessageID() {
		UUID id = UUID.randomUUID();
		return String.format("%016x%016x", id.getMostSignificantBits(), id.getLeastSignificantBits());
	}
}
