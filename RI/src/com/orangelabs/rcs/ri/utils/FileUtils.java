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

import com.gsma.services.rcs.RcsServiceException;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FileUtils {

    private static final int KITKAT_VERSION_CODE = 19;

    private static final String TAKE_PERSISTABLE_URI_PERMISSION_METHOD_NAME = "takePersistableUriPermission";

    private static final Class<?>[] TAKE_PERSISTABLE_URI_PERMISSION_PARAM_TYPES = new Class[] {
            Uri.class, int.class
    };

    /**
     * Fetch the file name from URI
     * 
     * @param context Context
     * @param file URI
     * @return fileName String
     * @throws IllegalArgumentException
     */
    public static String getFileName(Context context, Uri file) throws IllegalArgumentException {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(file, null, null, null, null);
            if (ContentResolver.SCHEME_CONTENT.equals(file.getScheme())) {
                if (cursor != null && cursor.moveToFirst()) {
                    return cursor.getString(cursor
                            .getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                } else {
                    throw new IllegalArgumentException("Error in retrieving file name from the URI");
                }
            } else if (ContentResolver.SCHEME_FILE.equals(file.getScheme())) {
                return file.getLastPathSegment();
            }
            throw new IllegalArgumentException("Unsupported URI scheme");

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Fetch the file size from URI
     * 
     * @param context Context
     * @param file URI
     * @return fileSize long
     * @throws IllegalArgumentException
     */
    public static long getFileSize(Context context, Uri file) throws IllegalArgumentException {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(file, null, null, null, null);
            if (ContentResolver.SCHEME_CONTENT.equals(file.getScheme())) {
                if (cursor != null && cursor.moveToFirst()) {
                    return Long.valueOf(cursor.getString(cursor
                            .getColumnIndexOrThrow(OpenableColumns.SIZE)));
                } else {
                    throw new IllegalArgumentException("Error in retrieving file size form the URI");
                }
            } else if (ContentResolver.SCHEME_FILE.equals(file.getScheme())) {
                return (new File(file.getPath())).length();
            }
            throw new IllegalArgumentException("Unsupported URI scheme");

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Open file
     * 
     * @param activity the activity
     * @param mimeType the mime type
     * @param action the action
     */
    public static void openFile(Activity activity, String mimeType, int action) {
        Intent intent;
        if (Build.VERSION.SDK_INT < KITKAT_VERSION_CODE) {
            intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        } else {
            intent = new Intent("android.intent.action.OPEN_DOCUMENT");
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        activity.startActivityForResult(intent, action);
    }

    /**
     * Using reflection to persist Uri permission in order to support backward compatibility since
     * this API is available only from Kitkat onwards. Try to persist Uri access permission for the
     * client to be able to read the contents from this Uri even after the client is restarted after
     * device reboot.
     * 
     * @param file Uri of file to transfer
     * @throws RcsServiceException
     */
    public static void tryToTakePersistableContentUriPermission(Context context, Uri file)
            throws RcsServiceException {
        if (!(ContentResolver.SCHEME_CONTENT.equals(file.getScheme()))) {
            return;
        }
        if (android.os.Build.VERSION.SDK_INT < KITKAT_VERSION_CODE) {
            return;
        }
        try {
            ContentResolver contentResolver = context.getContentResolver();
            Method takePersistableUriPermissionMethod = contentResolver.getClass().getMethod(
                    TAKE_PERSISTABLE_URI_PERMISSION_METHOD_NAME,
                    TAKE_PERSISTABLE_URI_PERMISSION_PARAM_TYPES);
            Object[] methodArgs = new Object[] {
                    file,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            };
            takePersistableUriPermissionMethod.invoke(contentResolver, methodArgs);

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RcsServiceException(e);
        }
    }
}
