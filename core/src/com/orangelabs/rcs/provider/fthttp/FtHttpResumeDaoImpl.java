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
package com.orangelabs.rcs.provider.fthttp;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * @author YPLO6403
 * 
 *         Implementation of interface to get access to FT HTTP data objects
 * 
 */
public class FtHttpResumeDaoImpl implements FtHttpResumeDao {

	/**
	 * Current instance
	 */
	private static FtHttpResumeDaoImpl instance = null;

	/**
	 * The logger
	 */
	final private static Logger logger = Logger.getLogger(FtHttpResumeDaoImpl.class.getSimpleName());

	/**
	 * Content resolver
	 */
	private ContentResolver cr;

	private FtHttpResumeDaoImpl(Context context) {
		this.cr = context.getContentResolver();
	}

	/**
	 * Creates an interface to get access to Data Object FtHttpResume
	 * 
	 * @param ctx
	 *            the {@code context} value.
	 * @return Instance of FtHttpResumeDaoImpl
	 */
	public static synchronized FtHttpResumeDaoImpl createInstance(Context ctx) {
		if (instance == null) {
			instance = new FtHttpResumeDaoImpl(ctx);
		}
		return instance;
	}

	/**
	 * Returns instance of DAO FtHttpResume
	 * 
	 * @return Instance
	 */
	public static FtHttpResumeDaoImpl getInstance() {
		return instance;
	}

	@Override
	public List<FtHttpResume> queryAll() {
		ArrayList<FtHttpResume> result = new ArrayList<FtHttpResume>();
		Cursor cursor = null;
		try {
			cursor = cr.query(FtHttpColumns.CONTENT_URI, FtHttpColumns.FULL_PROJECTION, null, null, null);
			if (cursor != null) {
				while (cursor.moveToNext()) {
					long size = cursor.getLong(3);
					String mimeType = cursor.getString(4);
					String contact = cursor.getString(5);
					String chatId = cursor.getString(6);
					String file = cursor.getString(7);
					int direction = cursor.getInt(8);
					String displayName = cursor.getString(10);
					String sessionId = cursor.getString(11);
					String thumbnail = cursor.getString(12);
					boolean isGroup = cursor.getInt(14) != 0;
					String chatSessionId = cursor.getString(15);
					if (FtHttpDirection.values()[direction] == FtHttpDirection.INCOMING) {
						String url = cursor.getString(2);
						MmContent content = ContentManager.createMmContentFromMime(url, mimeType, size);
						String messageId = cursor.getString(13);
						result.add(new FtHttpResumeDownload(file, thumbnail, content, messageId, contact, displayName, chatId,
								sessionId, chatSessionId, isGroup));
					} else {
						String tid = cursor.getString(1);
						MmContent content = ContentManager.createMmContentFromMime(file, mimeType, size);
						result.add(new FtHttpResumeUpload(content, thumbnail, tid, contact, displayName, chatId, sessionId,
								chatSessionId, isGroup));
					}
				}
			}
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error(e.getMessage(), e);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return result;
	}

	@Override
	public Uri insert(FtHttpResume ftHttpResume) {
		ContentValues values = new ContentValues();
		values.put(FtHttpColumns.DATE, System.currentTimeMillis());
		values.put(FtHttpColumns.DIRECTION, ftHttpResume.getDirection().ordinal());
		values.put(FtHttpColumns.FILENAME, ftHttpResume.getFilename());
		values.put(FtHttpColumns.TYPE, ftHttpResume.getMimetype());
		values.put(FtHttpColumns.SIZE, ftHttpResume.getSize());
		values.put(FtHttpColumns.THUMBNAIL, ftHttpResume.getThumbnail());
		values.put(FtHttpColumns.CONTACT, ftHttpResume.getContact());
		values.put(FtHttpColumns.DISPLAY_NAME, ftHttpResume.getDisplayName());
		values.put(FtHttpColumns.CHATID, ftHttpResume.getChatId());
		values.put(FtHttpColumns.SESSION_ID, ftHttpResume.getSessionId());
		values.put(FtHttpColumns.CHAT_SESSION_ID, ftHttpResume.getChatSessionId());
		values.put(FtHttpColumns.IS_GROUP, ftHttpResume.isGroup());
		if (ftHttpResume instanceof FtHttpResumeDownload) {
			FtHttpResumeDownload download = (FtHttpResumeDownload) ftHttpResume;
			values.put(FtHttpColumns.IN_URL, download.getUrl());
			values.put(FtHttpColumns.MESSAGE_ID, download.getMessageId());
			if (logger.isActivated()) {
				logger.debug("insert " + download + ")");
			}
		} else if (ftHttpResume instanceof FtHttpResumeUpload) {
			FtHttpResumeUpload upload = (FtHttpResumeUpload) ftHttpResume;
			values.put(FtHttpColumns.OU_TID, upload.getTid());
			if (logger.isActivated()) {
				logger.debug("insert " + upload + ")");
			}
		} else {
			return null;
		}
		return cr.insert(FtHttpColumns.CONTENT_URI, values);
	}

	@Override
	public int deleteAll() {
		return cr.delete(FtHttpColumns.CONTENT_URI, null, null);
	}

	@Override
	public int delete(FtHttpResume ftHttpResume) {
		if (logger.isActivated()) {
			logger.debug("delete " + ftHttpResume);
		}
		return cr.delete(FtHttpColumns.CONTENT_URI, FtHttpColumns.SESSION_ID + " = " + ftHttpResume.getSessionId(), null);
	}

	@Override
	public FtHttpResumeUpload queryUpload(String tid) {
		String selection = FtHttpColumns.OU_TID + " = ? AND " + FtHttpColumns.DIRECTION + " = ?";
		String[] selectionArgs = { tid, "" + FtHttpDirection.OUTGOING.ordinal() };
		Cursor cursor = null;
		try {
			cursor = cr.query(FtHttpColumns.CONTENT_URI, FtHttpColumns.FULL_PROJECTION, selection, selectionArgs, "_ID LIMIT 1");
			if (cursor != null) {
				if (cursor.moveToNext()) {
					String url = cursor.getString(2);
					long size = cursor.getLong(3);
					String mimeType = cursor.getString(4);
					String contact = cursor.getString(5);
					String chatId = cursor.getString(6);
					String displayName = cursor.getString(10);
					String sessionId = cursor.getString(11);
					String thumbnail = cursor.getString(12);
					boolean isGroup = cursor.getInt(14) != 0;
					String chatSessionId = cursor.getString(15);
					MmContent fileContent = ContentManager.createMmContentFromMime(url, mimeType, size);
					return new FtHttpResumeUpload(fileContent, thumbnail, tid, contact, displayName, chatId, sessionId,
							chatSessionId, isGroup);
				}
			}
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error(e.getMessage(), e);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}

	@Override
	public FtHttpResumeDownload queryDownload(String url) {
		String selection = FtHttpColumns.IN_URL + " = ? AND " + FtHttpColumns.DIRECTION + " = ?";
		String[] selectionArgs = { url, "" + FtHttpDirection.INCOMING.ordinal() };
		Cursor cursor = null;
		try {
			cursor = cr.query(FtHttpColumns.CONTENT_URI, FtHttpColumns.FULL_PROJECTION, selection, selectionArgs, "_ID LIMIT 1");
			if (cursor != null) {
				if (cursor.moveToNext()) {
					long size = cursor.getLong(3);
					String mimeType = cursor.getString(4);
					String contact = cursor.getString(5);
					String chatId = cursor.getString(6);
					String file = cursor.getString(7);
					String displayName = cursor.getString(10);
					String sessionId = cursor.getString(11);
					String thumbnail = cursor.getString(12);
					String messageId = cursor.getString(13);
					boolean isGroup = cursor.getInt(14) != 0;
					String chatSessionId = cursor.getString(15);
					MmContent content = ContentManager.createMmContentFromMime(url, mimeType, size);
					return new FtHttpResumeDownload(file, thumbnail, content, messageId, contact, displayName, chatId, sessionId,
							chatSessionId, isGroup);
				}
			}
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error(e.getMessage(), e);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}
}
