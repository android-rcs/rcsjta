/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.orangelabs.rcs.core.content;

import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.orangelabs.rcs.core.ims.protocol.sdp.MediaDescription;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpParser;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.MimeManager;

import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

/**
 * Multimedia content manager
 * 
 * @author jexa7410
 */
public class ContentManager{
    /**
     * Generate an Uri for the received content
     * 
     * @param fileName File name
     * @param mime MIME type
     * @return Uri
     */
	public static Uri generateUriForReceivedContent(String fileName, String mime) {
		// Generate a file path
		String path;
    	if (mime.startsWith("image")) {
			path = RcsSettings.getInstance().getPhotoRootDirectory();
        } else
    	if (mime.startsWith("video")) {
			path = RcsSettings.getInstance().getVideoRootDirectory();
    	} else {
			path = RcsSettings.getInstance().getFileRootDirectory();
    	}

    	// Check that the fileName will not overwrite existing file
    	// We modify it if a file of the same name exists, by appending _1 before the extension
    	// For example if image.jpeg exists, next file will be image_1.jpeg, then image_2.jpeg etc
    	String extension = "";
    	if ((fileName!=null) && (fileName.indexOf('.')!=-1)){
    		// if extension is present, split it
    		extension = "." + fileName.substring(fileName.lastIndexOf('.')+1);
    		fileName = fileName.substring(0, fileName.lastIndexOf('.'));
    	}
    	String destination = fileName;
    	int i = 1;
    	while(FileFactory.getFactory().fileExists(path + destination + extension)){
    		destination = fileName + '_' + i;
    		i++;
    	}

    	// Return free destination url
    	String filePath = path + destination + extension;
        return Uri.fromFile(new File(filePath));
	}

	/**
	 * Save a content in the local directory of the device
	 *
	 * @param content Content to be saved
	 * @throws IOException
	 */
	public static void saveContent(MmContent content) throws IOException {
		ParcelFileDescriptor pfd = null;
		// Write data
		OutputStream os = null;
		Uri file = content.getUri();
		try {
			pfd = AndroidFactory.getApplicationContext().getContentResolver()
					.openFileDescriptor(file, "w");
			os = new FileOutputStream(pfd.getFileDescriptor());
			os.write(content.getData());
			os.flush();
		} finally {
			if (os != null) {
				os.close();
			}
			if (pfd != null) {
				pfd.close();
			}
		}
		// Update the media storage with uri.getPath() since its always fileUri
		// in the receiver side
		FileFactory.getFactory().updateMediaStorage(file.getPath());
	}

	/**
	 * Create a content object from URI description
	 *
	 * @param uri Content URI
	 * @param size Content size
	 * @param fileName The file name
	 * @return Content instance
	 */
	public static MmContent createMmContent(Uri uri, long size, String fileName) {
		String extension = MimeManager.getFileExtension(fileName);
		String mime = MimeManager.getInstance().getMimeType(extension);
		return createMmContentFromMime(uri, mime, size, fileName);
	}

    /**
	 * Create a content object from MIME type
	 *
	 * @param uri Content URI
	 * @param mime MIME type
	 * @param size Content size
	 * @param fileName The file name
	 * @return Content instance
	 */
	public static MmContent createMmContentFromMime(Uri uri, String mime, long size, String fileName) {
		if (mime != null) {
			if (mime.startsWith(PhotoContent.ENCODING)) {
				// Photo content
				return new PhotoContent(uri, mime, size, fileName);
			}
			if (mime.startsWith(VideoContent.ENCODING)) {
				// Video content
				return new VideoContent(uri, mime, size, fileName);
			}
			if (mime.startsWith(AudioContent.ENCODING)) {
				// Audio content
				return new AudioContent(uri, mime, size, fileName);
			}
			if (mime.equals(VisitCardContent.ENCODING)) {
				// Visit Card content
				return new VisitCardContent(uri, size, fileName);
			}
			if (mime.equals(GeolocContent.ENCODING)) {
				// Geoloc content
				return new GeolocContent(uri, size, fileName);
			}
		}
		// File content
		return new FileContent(uri, size, fileName);
	}

    /**
     * Create a live video content object
     * 
     * @param codec Codec
     * @return Content instance
     */
	public static LiveVideoContent createLiveVideoContent(String codec) {
		return new LiveVideoContent("video/"+codec);
	}
	
    /**
     * Create a live audio content object
     * 
     * @param codec Codec
     * @return Content instance
     */
	public static LiveAudioContent createLiveAudioContent(String codec) {
		return new LiveAudioContent("audio/"+codec);
	}

    /**
     * Create a generic live video content object
     * 
     * @return Content instance
     */
    public static LiveVideoContent createGenericLiveVideoContent() {
        return new LiveVideoContent("video/*");
    }
    
    /**
     * Create a generic live audio content object
     * 
     * @return Content instance
     */
    public static LiveAudioContent createGenericLiveAudioContent() {
        return new LiveAudioContent("audio/*");
    }

	/**
     * Create a live video content object
     * 
     * @param sdp SDP part
     * @return Content instance
     */
	public static LiveVideoContent createLiveVideoContentFromSdp(byte[] sdp) {
		 // Parse the remote SDP part
        SdpParser parser = new SdpParser(sdp);
    	Vector<MediaDescription> media = parser.getMediaDescriptions();
    	if (media.size()==0) { // there is no media in SDP
    		return null;
    	}
    	MediaDescription desc = media.elementAt(0);
    	if (media.size()==1) { // if only one media in SDP, test if 'video', if not then return null 		
    		if (!desc.name.equals("video")) {
    			return null;
    		}	
    	}	
    	if (media.size()==2) { // if two media in SDP, test if first 'video', if not then choose second and test if video, if not return null
    		if (!desc.name.equals("video")) {
    			desc = media.elementAt(1);
    			if (!desc.name.equals("video")) {
    				return null;
    			}
    		}	
    	}		
	
        String rtpmap = desc.getMediaAttribute("rtpmap").getValue();

        // Extract the video encoding
        String encoding = rtpmap.substring(rtpmap.indexOf(desc.payload)+desc.payload.length()+1);
        String codec = encoding.toLowerCase().trim();
        int index = encoding.indexOf("/");
		if (index != -1) {
			codec = encoding.substring(0, index);
        }
		return createLiveVideoContent(codec);
	}
	
	/**
     * Create a live audio content object
     * 
     * @param sdp SDP part
     * @return Content instance
     */
	public static LiveAudioContent createLiveAudioContentFromSdp(byte[] sdp) {
		 // Parse the remote SDP part
        SdpParser parser = new SdpParser(sdp);
    	Vector<MediaDescription> media = parser.getMediaDescriptions(); // TODO replace with getMediaDescriptions(audio)
    	if (media.size()==0) {
    		return null;
    	}
		MediaDescription desc = media.elementAt(0);
    	if (media.size()==1) { // if only one media in SDP, test if 'audio', if not then return null 		
    		if (!desc.name.equals("audio")) {
    			return null;
    		}	
    	}	
    	if (media.size()==2) { // if two media in SDP, test if first 'audio', if not then choose second and test if 'audio', if not return null
    		if (!desc.name.equals("audio")) {
    			desc = media.elementAt(1);
    			if (!desc.name.equals("audio")) {
    				return null;
    			}
    		}	
    	}	
		if (!desc.name.equals("audio")) {
			return null;
		}	
        String rtpmap = desc.getMediaAttribute("rtpmap").getValue();

        // Extract the audio encoding
        String encoding = rtpmap.substring(rtpmap.indexOf(desc.payload)+desc.payload.length()+1);
        String codec = encoding.toLowerCase().trim();        
        int index = encoding.indexOf("/");
		if (index != -1) {
			codec = encoding.substring(0, index);
        }
		return createLiveAudioContent(codec);
	}

	/**
     * Create a content object from SDP description of a SIP invite request
     * 
     * @param invite SIP invite request
     * @return Content instance
     */
	public static MmContent createMmContentFromSdp(SipRequest invite) {
		try {
			String remoteSdp = invite.getSdpContent();
	    	SdpParser parser = new SdpParser(remoteSdp.getBytes());
			Vector<MediaDescription> media = parser.getMediaDescriptions();
			MediaDescription desc = media.elementAt(0);
			MediaAttribute attr1 = desc.getMediaAttribute("file-selector");
			String fileSelectorValue = attr1.getValue();
			String mime = SipUtils.extractParameter(fileSelectorValue, "type:", "application/octet-stream");
			long size = Long.parseLong(SipUtils.extractParameter(fileSelectorValue, "size:", "-1"));
			String filename = SipUtils.extractParameter(fileSelectorValue, "name:", "");
			Uri file = ContentManager.generateUriForReceivedContent(filename, mime);
			MmContent mContent = ContentManager.createMmContent(file, size, filename);
			return mContent;
		} catch(Exception e) {
			return null;
		}
	}
}
