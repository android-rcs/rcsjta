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
package com.orangelabs.rcs.core.ims.service.im.filetransfer;

import static com.orangelabs.rcs.utils.StringUtils.UTF8;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.InputSource;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;

import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.network.sip.Multipart;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatMessage;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoParser;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.platform.file.FileDescription;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.utils.Base64;
import com.orangelabs.rcs.utils.CloseableUtils;
import com.orangelabs.rcs.utils.FileUtils;
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
		return mime != null
				&& mime.toLowerCase().startsWith(FileTransferHttpInfoDocument.MIME_TYPE);
	}

	/**
	 * Create a content of fileIcon from a file
	 *
	 * @param file
	 *            Uri of the image
	 * @param fileIconId
	 *            the identifier of the file icon
	 * @return the content of the file icon
	 */
	public static MmContent createFileicon(Uri file, String fileIconId) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		InputStream in = null;
		try {
			in = AndroidFactory.getApplicationContext().getContentResolver().openInputStream(file);
			Bitmap bitmap = BitmapFactory.decodeStream(in);
			if (bitmap == null) {
				if (logger.isActivated()) {
					logger.warn("Cannot decode image " + file);
				}
				return null;
			}
			int width = bitmap.getWidth();
			int height = bitmap.getHeight();

			long size = FileUtils.getFileSize(AndroidFactory.getApplicationContext(), file);
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
			// Create fileIcon URL
			String fileIconName = buildFileiconUrl(fileIconId, "image/jpeg");
			// Get the fileIcon data
			byte[] fileIconData = out.toByteArray();

			// Generate fileIcon content
			Uri fileIconUri = ContentManager.generateUriForReceivedContent(fileIconName, "image/jpeg");
			MmContent fileIcon = ContentManager.createMmContent(fileIconUri, fileIconData.length, fileIconName);
			// Save the fileIcon data
			fileIcon.setData(fileIconData);
			// persist the fileIcon content
			fileIcon.writeData2File(fileIconData);
			fileIcon.closeFile();
			if (logger.isActivated()) {
				logger.debug("Generate Icon " + fileIconName + " for image " + file);
			}
			return fileIcon;
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
	 * Generate a filename for the file icon
	 *
	 * @param msgId
	 *            the message ID of the File Transfer
	 * @param mimeType
	 *            the mime-type
	 * @return the filename of the file icon
	 */
	public static String buildFileiconUrl(String msgId, String mimeType) {
		StringBuilder iconName = new StringBuilder("thumbnail_");
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
	 * Extract file icon from incoming INVITE request
	 *
	 * @param request
	 *            Request
	 * @return fileIcon the file icon content persisted on disk
	 */
	public static MmContent extractFileIcon(SipRequest request) {
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
					// Build fileIcon name
					String iconName = buildFileiconUrl(ChatUtils.getContributionId(request), mimeType);
					// Generate URL
					Uri fileIconUri = ContentManager.generateUriForReceivedContent(iconName, mimeType);
					// Get binary data
					byte[] fileIconData = Base64.decodeBase64(mimeType
							.getBytes(UTF8));
					// Generate fileIcon content
					MmContent result = ContentManager.createMmContent(fileIconUri, fileIconData.length, iconName);
					result.setData(fileIconData);
					// Decode the content and persist on disk
					result.writeData2File(fileIconData);
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
		ChatMessage message = ChatUtils.getFirstMessage(request);
		if (message == null || !FileTransferUtils.isFileTransferHttpType(message.getMimeType())) {
			return null;
		}
		byte[] xml = message.getContent().getBytes(UTF8);
		return parseFileTransferHttpDocument(xml);
	}

	/**
	 * Create a content object from URI
	 *
	 * @param uri Uri of file
	 * @return Content instance
	 */
	public static MmContent createMmContent(Uri uri) {
		if (uri == null) {
			return null;
		}
		try {
			FileDescription desc = FileFactory.getFactory().getFileDescription(uri);
			return ContentManager.createMmContent(uri, desc.getSize(), desc.getName());
		} catch (IOException e) {
			if (logger.isActivated()) {
				logger.error(e.getMessage(), e);
			}
			return null;
		}
	}
}
