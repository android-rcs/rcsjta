/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.orangelabs.rcs.core.ims.protocol.sdp;

import static com.orangelabs.rcs.utils.StringUtils.UTF8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Vector;

/**
 * Generic parser
 *
 * @author jexa7410
 */
class Parser {
	/**
	 * Buffer
	 */
	private Vector<Integer> buffer = new Vector<Integer>();

	/**
	 * Unget a token
	 *
	 * @param tk Token
	 */
	public void ungetToken(String tk) {
		byte token[] = tk.getBytes(UTF8);
		for (int i = 0; i < token.length; i++) {
			buffer.insertElementAt(Integer.valueOf(token[token.length - i - 1]), 0);
		}
	}

	/**
	 * Get a token
	 *
	 * @param input Input stream
	 * @param tk Token
	 * @return Token value
	 */
	public boolean getToken(ByteArrayInputStream input, String tk) {
		boolean found = false;

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		skipWhitespace(input);

		if (input.available() > 0) {
			int ch = readChar(input);
			while (ch != '=' && ch != '\n' && ch != '\r' && ch != -1) {
				bout.write(ch);
				ch = readChar(input);
			}
			bout.write(ch);
		}

		String token = new String(bout.toByteArray(), UTF8);
		if (tk.equals(token)) {
			found = true;
		} else {
			ungetToken(token);
		}

		return found;
	}

	/**
	 * Get a line
	 *
	 * @param input Input stream
	 * @return Line
	 */
	public String getLine(ByteArrayInputStream input) {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		if (input.available() > 0) {
			int ch = readChar(input);
			while (ch != '\n' && ch != '\r' && ch != -1) {
				bout.write(ch);
				ch = readChar(input);
			}
		}
		return new String(bout.toByteArray(), UTF8);
	}

	/**
	 * Skip whitespace
	 *
	 * @param input Input stream
	 */
	private void skipWhitespace(ByteArrayInputStream input) {
		int ch = readChar(input);
		while (ch == ' ' || ch == '\n' || ch == '\r') {
			ch = readChar(input);
		}
		buffer.insertElementAt(Integer.valueOf(ch), 0);
	}

	/**
	 * Read char
	 *
	 * @param input Input stream
	 */
	private int readChar(ByteArrayInputStream input) {
		int ch;
		if (buffer.size() > 0) {
			ch = ((Integer)buffer.elementAt(0)).intValue();
			buffer.removeElementAt(0);
		} else {
			ch = input.read();
		}
		return ch;
	}
}
