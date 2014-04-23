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
import java.util.Date;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

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
		FtHttpSelection where = new FtHttpSelection();
		ArrayList<FtHttpResume> result = new ArrayList<FtHttpResume>();
		FtHttpCursor cursor = where.query(cr);
		if (cursor != null) {
			while (cursor.moveToNext()) {
				if (cursor.getDirection() == FtHttpDirection.INCOMING) {
					result.add(new FtHttpResumeDownload(cursor));
				} else {
					result.add(new FtHttpResumeUpload(cursor));
				}
			}
			cursor.close();
		}
		return result;
	}

	@Override
	public Uri insert(FtHttpResume ftHttpResume) {
		FtHttpContentValues values = new FtHttpContentValues();
		values.putDate(new Date(System.currentTimeMillis()));
		values.putDirection(ftHttpResume.getDirection());
		values.putFilename(ftHttpResume.getFilename());
        values.putType(ftHttpResume.getMimetype());
        values.putSize(ftHttpResume.getSize());
		values.putThumbnail(ftHttpResume.getThumbnail());
		values.putContact(ftHttpResume.getContact());
		values.putDisplayName(ftHttpResume.getDisplayName());
		values.putChatid(ftHttpResume.getChatId());
		values.putSessionId(ftHttpResume.getSessionId());
		values.putChatSessionId(ftHttpResume.getChatSessionId());
		values.putIsGroup(ftHttpResume.isGroup());
		if (ftHttpResume instanceof FtHttpResumeDownload) {
			FtHttpResumeDownload download = (FtHttpResumeDownload) ftHttpResume;
			values.putInUrl(download.getUrl());
			values.putMessageId(download.getMessageId());
			if (logger.isActivated()) {
				logger.debug("insert " + download + ")");
			}
		} else if (ftHttpResume instanceof FtHttpResumeUpload) {
			FtHttpResumeUpload upload = (FtHttpResumeUpload) ftHttpResume;
			values.putOuTid(upload.getTid());
			if (logger.isActivated()) {
			    logger.debug("insert " + upload + ")");
			}
		} else {
			return null;
		}
		return cr.insert(FtHttpColumns.CONTENT_URI, values.values());
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
		FtHttpSelection where = new FtHttpSelection();
		where.sessionId(ftHttpResume.getSessionId());
		return where.delete(cr);
	}

	@Override
	public FtHttpResumeUpload queryUpload(String tid) {
		FtHttpSelection where = new FtHttpSelection();
		FtHttpResumeUpload result = null;
		where.ouTid(tid).and().direction(FtHttpDirection.OUTGOING);
		FtHttpCursor cursor = where.query(cr, null, "_ID LIMIT 1");
		if (cursor != null) {
			if (cursor.moveToNext()) {
				result = new FtHttpResumeUpload(cursor);
			}
			cursor.close();
		}
		return result;
	}

	@Override
	public FtHttpResumeDownload queryDownload(String url) {
		FtHttpSelection where = new FtHttpSelection();
		FtHttpResumeDownload result = null;
		where.inUrl(url).and().direction(FtHttpDirection.INCOMING);
		FtHttpCursor cursor = where.query(cr, null, "_ID LIMIT 1");
		if (cursor != null) {
			if (cursor.moveToNext()) {
				result = new FtHttpResumeDownload(cursor);
			}
			cursor.close();
		}
		return result;
	}
}
