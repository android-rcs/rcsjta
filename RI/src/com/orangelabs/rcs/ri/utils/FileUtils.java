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
package com.orangelabs.rcs.ri.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

public class FileUtils {

	/**
	 * Fetch the file name from URI
	 * 
	 * @param context
	 *            Context
	 * @param file
	 *            URI
	 * @return fileName String
	 * @throws IllegalArgumentException
	 */
	public static String getFileName(Context context, Uri file) throws IllegalArgumentException {
		return getColumStringValue(context, file, OpenableColumns.DISPLAY_NAME);
	}

	/**
	 * Fetch the file size from URI
	 * 
	 * @param context
	 *            Context
	 * @param file
	 *            URI
	 * @return fileSize long
	 * @throws IllegalArgumentException
	 */
	public static long getFileSize(Context context, Uri file) {
		return Long.valueOf(getColumStringValue(context, file, OpenableColumns.SIZE)).longValue();
	}

	private static String getColumStringValue(Context context, Uri file, String columnName) throws IllegalArgumentException {
		Cursor cursor = null;
		try {
			cursor = context.getContentResolver().query(file, null, null, null, null, null);
			if (ContentResolver.SCHEME_CONTENT.equals(file.getScheme())) {
				if (cursor != null && cursor.moveToFirst()) {
					return cursor.getString(cursor.getColumnIndexOrThrow(columnName));
				} else {
					throw new IllegalArgumentException("Error in information from the URI");
				}
			} else if (ContentResolver.SCHEME_FILE.equals(file.getScheme())) {
				return file.getLastPathSegment();
			} else {
				throw new IllegalArgumentException("Unsupported URI scheme");
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Error in information from the URI");
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}
}
