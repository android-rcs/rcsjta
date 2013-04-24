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

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

/**
 * String utility functions
 */
public class StringUtils {

	/**
	 * UTF-8 charset
	 */
	private final static String CHARSET_UTF8 = "UTF-8"; 
		
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
	 * Encode string into UTF-8 string
	 * 
	 * @param str Input string
	 * @return Encoded string
	 */
	public static String encodeUTF8(String str) {
		if (str == null) {
			return null;
		}
		try {
			return new String(str.getBytes(), CHARSET_UTF8);
		} catch (Exception e){
		    return null;
		}
	}
	
	/**
	 * Encode UTF-8 bytes to a string
	 * 
	 * @param b Input bytes
	 * @return Decoded string
	 */
	public static String encodeUTF8(byte[] b) {
		if (b == null) {
			return null;
		}
		try {
			return new String(b, CHARSET_UTF8);
		} catch (Exception e){
			return null;
		}
	}	
	
	/**
	 * Decode UTF-8 bytes to string
	 * 
	 * @param b Input bytes
	 * @return Decoded string
	 */
	public static String decodeUTF8(byte[] b) {
		if (b == null) {
			return null;
		}
		try {
			return new String(b, CHARSET_UTF8);
		} catch(Exception e) {
			return null;
		}
	}	

	/**
	 * Decode UTF-8 string to a string
	 * 
	 * @param str Input string
	 * @return Decoded string
	 */
	public static String decodeUTF8(String str) {
		if (str == null) {
			return null;
		}
		return decodeUTF8(str.getBytes());
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
}
