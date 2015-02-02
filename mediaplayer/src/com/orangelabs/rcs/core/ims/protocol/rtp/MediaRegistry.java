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

package com.orangelabs.rcs.core.ims.protocol.rtp;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.orangelabs.rcs.core.ims.protocol.rtp.codec.Codec;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.Format;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.H264VideoFormat;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.VideoFormat;

/**
 * Media registry that handles the supported codecs
 * 
 * @author jexa7410
 */
public class MediaRegistry {

	/**
	 * Supported codecs
	 */
	private static Hashtable<String, Format> SUPPORTED_CODECS = new Hashtable<String, Format>();
	static {
		SUPPORTED_CODECS.put(H264VideoFormat.ENCODING.toLowerCase(), new H264VideoFormat());
	}

	/**
	 * Returns the list of the supported video format
	 * 
	 * @return List of video formats
	 */
	public static Vector<VideoFormat> getSupportedVideoFormats() {
		Vector<VideoFormat> list = new Vector<VideoFormat>();
    	for (Enumeration<Format> e = SUPPORTED_CODECS.elements() ; e.hasMoreElements() ;) {
	         Format fmt = (Format)e.nextElement();
	         if (fmt instanceof VideoFormat) {
		         list.addElement((VideoFormat)fmt);
	         }
	     }
		return list;
	}

	/**
     * Generate the format associated to the codec name
     * 
     * @param codec Codec name
     * @return Format
     */
    public static Format generateFormat(String codec) {
    	return (Format)SUPPORTED_CODECS.get(codec.toLowerCase());
    }    
    
	/**
     * Is codec supported
     * 
     * @param codec Codec name
     * @return Boolean
     */
    public static boolean isCodecSupported(String codec) {
    	Format format = (Format)SUPPORTED_CODECS.get(codec.toLowerCase());
		return (format != null);
    }    
    
	/**
     * Generate the codec encoding chain
     * 
	 * @param encoding Encoding name
     * @return Codec chain
     */
    public static Codec[] generateEncodingCodecChain(String encoding) {
    	if (encoding.equalsIgnoreCase(H264VideoFormat.ENCODING)) { 
	        // Java H264 packetizer
	        Codec[] chain = {
	            new com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.JavaPacketizer()
	        };
	        return chain;
		} else { 
			// Codec implemented in the native part
			return new Codec[0];
		}
    }

	/**
	 * Generate the decoding codec chain
	 * 
	 * @param encoding Encoding name
	 * @return Codec chain
	 */
	public static Codec[] generateDecodingCodecChain(String encoding) {
		if (encoding.equalsIgnoreCase(H264VideoFormat.ENCODING)) {
	        // Java H264 depacketizer
	        Codec[] chain = {
	            new com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.JavaDepacketizer()
	        };
	        return chain;
		} else {
			// Codec implemented in the native part
			return new Codec[0];
		}
	}
}
