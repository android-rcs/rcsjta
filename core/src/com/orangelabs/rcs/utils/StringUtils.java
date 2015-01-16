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

package com.orangelabs.rcs.utils;

import java.nio.charset.Charset;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Iterator;

/**
 * String utility functions
 */
public class StringUtils {

	public static final String UTF8_STR = "utf-8";

    public static final Charset UTF8 = Charset.forName(UTF8_STR);

    /**
     * Truncate a string to a max length
     *
     * @param text String to truncate
     * @param truncatedLength Max length
     * @return Truncated string
     */
    public static String truncate(String text, int truncatedLength) {
        if (text == null) {
            return null;
        }

        if ((truncatedLength < 0) || (truncatedLength > text.length())) {
            return text;
        }
        return text.substring(0, truncatedLength);
    }

	/**
	 * Escape characters for text appearing as XML data, between tags.
	 *
	 * The following characters are replaced :
	 * <br> <
	 * <br> >
	 * <br> &
	 * <br> "
	 * <br> '
	 *
	 * @param text Input text
	 * @return Encoded string
	 */
	public static String encodeXML(String text) {
		if (text == null) {
			return null;
		}

	    final StringBuilder result = new StringBuilder();
	    final StringCharacterIterator iterator = new StringCharacterIterator(text);
	    char character =  iterator.current();
	    while (character != CharacterIterator.DONE ){
	      if (character == '<') {
	        result.append("&lt;");
	      }
	      else if (character == '>') {
	        result.append("&gt;");
	      }
	      else if (character == '\"') {
	        result.append("&quot;");
	      }
	      else if (character == '\'') {
	        result.append("&#039;");
	      }
	      else if (character == '&') {
	         result.append("&amp;");
	      }
	      else {
	        //the char is not a special one
	        //add it to the result as is
	        result.append(character);
	      }
	      character = iterator.next();
	    }
	    return result.toString();
	}

	/**
	 * Decode XML string
	 *
	 * @param text Input text
	 * @return Decoded string
	 */
	public static String decodeXML(String text) {
		if (text == null) {
			return null;
		}

	    text = text.replaceAll("&lt;", "<");
	    text = text.replaceAll("&gt;", ">");
	    text = text.replaceAll("&quot;", "\"");
	    text = text.replaceAll("&#039;", "\'");
	    text = text.replaceAll("&amp;", "&");

	    return text;
	}

	/**
	 * Remove quotes delimiters
	 *
	 * @param input Input
	 * @return String without quotes
	 */
	public static String removeQuotes(String input) {
		if ((input != null) && input.startsWith("\"") && input.endsWith("\"")) {
			input = input.substring(1, input.length()-1);
		}
		return input;
	}

	/**
	 * Is empty string
	 *
	 * @param str String
	 * @return Boolean
	 */
	public static boolean isEmpty(String str) {
		return (str == null) || (str.trim().length() == 0);
	}

	/**
	 * Build a string of delimited items
	 *
	 * @param s
	 *            an iterator over a CharSequence
	 * @param delimiter
	 *            a delimiter
	 * @return the string of delimited items
	 */
	public static String join(Iterable<? extends CharSequence> s, String delimiter) {
		Iterator<? extends CharSequence> iter = s.iterator();
		if (!iter.hasNext())
			return "";
		StringBuilder buffer = new StringBuilder(iter.next());
		while (iter.hasNext())
			buffer.append(delimiter).append(iter.next());
		return buffer.toString();
	}

	/**
	 * compares two strings null-safe
	 * @param str1
	 * @param str2
	 * @return true if equals
	 */
	public static boolean equals(String str1, String str2) {
	    return str1 == null ? str2 == null : str1.equals(str2);
	}
}
