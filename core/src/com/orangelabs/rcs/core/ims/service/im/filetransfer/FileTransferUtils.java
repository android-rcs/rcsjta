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
package com.orangelabs.rcs.core.ims.service.im.filetransfer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.InputSource;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;

import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.network.sip.Multipart;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.FileTransferMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoParser;
import com.orangelabs.rcs.platform.file.FileDescription;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.utils.Base64;
import com.orangelabs.rcs.utils.CloseableUtils;
import com.orangelabs.rcs.utils.MimeManager;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Utility class to manage File Transfer
 * 
 * @author YPLO6403
 * 
 */
public class FileTransferUtils {

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(FileTransferUtils.class.getName());

	/**
	 * Is a file transfer HTTP event type
	 * 
	 * @param mime
	 *            MIME type
	 * @return Boolean
	 */
	public static boolean isFileTransferHttpType(String mime) {
		if ((mime != null) && mime.toLowerCase().startsWith(FileTransferHttpInfoDocument.MIME_TYPE)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Create a content of thumbnail from a filepath
	 * 
	 * @param filepath
	 *            the file path of the image
	 * @param thumbnailId
	 *            the identifier of the thumbnail
	 * @return the content of the thumbnail
	 */
	public static MmContent createFileThumbnail(String filepath, String thumbnailId) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		InputStream in = null;
		try {

			File file = new File(filepath);
			in = new FileInputStream(file);
			Bitmap bitmap = BitmapFactory.decodeStream(in);
			int width = bitmap.getWidth();
			int height = bitmap.getHeight();
			long size = file.length();

			// Resize the bitmap
			float scale = 0.05f;
			Matrix matrix = new Matrix();
			matrix.postScale(scale, scale);

			// Recreate the new bitmap
			Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);

			// Compress the file to be under the limit (10KBytes)
			int quality = 90;
			int maxSize = 1024 * 10;
			while (size > maxSize) {
				out = new ByteArrayOutputStream();
				resizedBitmap.compress(CompressFormat.JPEG, quality, out);
				out.flush();
				out.close();
				size = out.size();
				quality -= 10;
			}
			// Create thumbnail URL
			String thumbnailName = builThumbnaiUrl(thumbnailId, "image/jpeg");
			// Get the thumbnail data
			byte[] thumbnailData = out.toByteArray();

			// Create the thumbnail content
			String url = ContentManager.generateUrlForReceivedContent(thumbnailName, "image/jpeg");

			// Generate thumbnail content
			MmContent thumbnail = ContentManager.createMmContentFromUrl(url, thumbnailData.length);
			// Save the thumbnail data
			thumbnail.setData(thumbnailData);
			// persist the thumbnail content
			thumbnail.writeData2File(thumbnailData);
			thumbnail.closeFile();
			if (logger.isActivated()) {
				logger.debug("Generate Icon " + thumbnailName + " for image " + filepath);
			}
			return thumbnail;
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error(e.getMessage(), e);
			}
			return null;
		} finally {
			CloseableUtils.close(in);
		}
	}

	/**
	 * Generate a filename for the thumbnail
	 * 
	 * @param msgId
	 *            the message ID of the File Transfer
	 * @param mimeType
	 *            the mime-type
	 * @return the filename of the thumnail
	 */
	public static String builThumbnaiUrl(String msgId, String mimeType) {
		StringBuilder iconName = new StringBuilder("thumnail_");
		iconName.append(msgId);
		String extension = MimeManager.getInstance().getExtensionFromMimeType(mimeType);
		if (extension != null) {
			iconName.append(".");
			iconName.append(extension);
			return iconName.toString();
		}
		throw new IllegalArgumentException("Invalid mime type for image");
	}

	/**
	 * Extract thumbnail from incoming INVITE request
	 * 
	 * @param request
	 *            Request
	 * @return Thumbnail the thumbnail content persisted on disk
	 */
	public static MmContent extractFileThumbnail(SipRequest request) {
		try {
			// Extract message from content/CPIM
			String content = request.getContent();
			String boundary = request.getBoundaryContentType();
			Multipart multi = new Multipart(content, boundary);
			if (multi.isMultipart()) {
				String mimeType = "image/jpeg";
				// Get image/jpeg content
				String data = multi.getPart(mimeType);
				if (data == null) {
					// Get image/png content
					mimeType = "image/png";
					data = multi.getPart(mimeType);
				}
				if (data != null) {
					// Build thumbnail name
					String iconName = builThumbnaiUrl(ChatUtils.getContributionId(request), mimeType);
					// Generate URL
					String url = ContentManager.generateUrlForReceivedContent(iconName, mimeType);
					// Get binary data
					byte[] thumbnailData = Base64.decodeBase64(mimeType.getBytes());
					// Generate thumbnail content
					MmContent result = ContentManager.createMmContentFromMime(iconName, url, mimeType, thumbnailData.length);
					result.setData(thumbnailData);
					// Decode the content and persist on disk
					result.writeData2File(thumbnailData);
					result.closeFile();
					return result;
				}
			}
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error(e.getMessage(), e);
			}
		}
		return null;
	}

	/**
	 * Parse a file transfer over HTTP document
	 * 
	 * @param xml
	 *            XML document
	 * @return File transfer document
	 */
	public static FileTransferHttpInfoDocument parseFileTransferHttpDocument(byte[] xml) {
		try {
			InputSource ftHttpInput = new InputSource(new ByteArrayInputStream(xml));
			FileTransferHttpInfoParser ftHttpParser = new FileTransferHttpInfoParser(ftHttpInput);
			return ftHttpParser.getFtInfo();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Get the HTTP file transfer info document
	 * 
	 * @param request
	 *            Request
	 * @return FT HTTP info
	 */
	public static FileTransferHttpInfoDocument getHttpFTInfo(SipRequest request) {
		InstantMessage message = ChatUtils.getFirstMessage(request);
		if ((message != null) && (message instanceof FileTransferMessage)) {
			FileTransferMessage ftMsg = (FileTransferMessage) message;
			byte[] xml = ftMsg.getFileInfo().getBytes();
			return parseFileTransferHttpDocument(xml);
		} else {
			return null;
		}
	}

	/**
	 * Create a content object from URL description
	 * 
	 * @param url
	 *            Content URL
	 * @return Content instance
	 */
	public static MmContent createMmContentFromUrl(String url) {
		try {
			FileDescription desc = FileFactory.getFactory().getFileDescription(url);
			return ContentManager.createMmContentFromUrl(url, desc.getSize());
		} catch (IOException e) {
		}
		return null;
	}
}
