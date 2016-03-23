/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.ri.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.util.Locale;

public class FileUtils {

    private static final String[] PROJECTION_DATA = {
        MediaStore.MediaColumns.DATA
    };

    private static final String SELECTION_ID = BaseColumns._ID + "=?";

    public static String getPath(final Context context, final Uri uri) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // DocumentProvider
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    String docId = DocumentsContract.getDocumentId(uri);
                    String[] split = docId.split(":");
                    String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }

                } else if (isDownloadsDocument(uri)) { // DownloadsProvider
                    String id = DocumentsContract.getDocumentId(uri);
                    Uri contentUri = ContentUris.withAppendedId(
                            Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                    return getDataColumn(context, contentUri, null, null);

                } else if (isMediaDocument(uri)) { // MediaProvider
                    String docId = DocumentsContract.getDocumentId(uri);
                    String[] split = docId.split(":");
                    String type = split[0];
                    Uri contentUri = null;
                    switch (type) {
                        case "image":
                            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                            break;
                        case "video":
                            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                            break;
                        case "audio":
                            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                            break;
                    }

                    String[] selectionArgs = new String[] {
                        split[1]
                    };
                    return getDataColumn(context, contentUri, SELECTION_ID, selectionArgs);
                }
            }
        }
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            /* MediaStore (and general) */
            // Return the remote address
            if (isGooglePhotosUri(uri)) {
                return uri.getLastPathSegment();
            }
            return getDataColumn(context, uri, null, null);

        } else if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
            /* File */
            return uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for MediaStore Uris, and other
     * file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private static String getDataColumn(Context context, Uri uri, String selection,
            String[] selectionArgs) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, PROJECTION_DATA, selection,
                    selectionArgs, null);
            if (cursor == null) {
                throw new SQLException("Failed to query URI=" + uri);
            }
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));
            }
            return null;
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is <span class="IL_AD" id="IL_AD3">Google</span> Photos.
     */
    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    /**
     * Fetch the file name from URI
     *
     * @param context Context
     * @param file URI
     * @return fileName String
     */
    public static String getFileName(Context context, Uri file) {
        String scheme = file.getScheme();
        switch (scheme) {
            case ContentResolver.SCHEME_FILE:
                return file.getLastPathSegment();

            case ContentResolver.SCHEME_CONTENT:
                Cursor cursor = null;
                try {
                    cursor = context.getContentResolver().query(file, null, null, null, null);
                    if (cursor == null) {
                        throw new SQLException("Failed to query file " + file);
                    }
                    if (cursor.moveToFirst()) {
                        return cursor.getString(cursor
                                .getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                    } else {
                        throw new IllegalArgumentException(
                                "Error in retrieving file name from the URI");
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            default:
                throw new IllegalArgumentException("Unsupported URI scheme");
        }
    }

    /**
     * Fetch the file size from URI
     *
     * @param ctx Context
     * @param file URI
     * @return fileSize long
     * @throws IllegalArgumentException
     */
    public static long getFileSize(Context ctx, Uri file) throws IllegalArgumentException {
        String scheme = file.getScheme();
        switch (scheme) {
            case ContentResolver.SCHEME_FILE:
                File f = new File(file.getPath());
                return f.length();

            case ContentResolver.SCHEME_CONTENT:
                Cursor cursor = null;
                try {
                    cursor = ctx.getContentResolver().query(file, null, null, null, null);
                    if (cursor == null) {
                        throw new SQLException("Failed to query file " + file);
                    }
                    if (cursor.moveToFirst()) {
                        return Long.valueOf(cursor.getString(cursor
                                .getColumnIndexOrThrow(OpenableColumns.SIZE)));
                    } else {
                        throw new IllegalArgumentException(
                                "Error in retrieving file size form the URI");
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            default:
                throw new IllegalArgumentException("Unsupported URI scheme");
        }
    }

    private static String getMimeTypeFromFile(Context ctx, Uri file) {
        MediaMetadataRetriever mmr = null;
        try {
            mmr = new MediaMetadataRetriever();
            mmr.setDataSource(ctx, file);
            return mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);

        } finally {
            if (mmr != null) {
                mmr.release();
            }
        }
    }

    /**
     * Gets the mime-type from file Uri
     *
     * @param ctx the context
     * @param file the file Uri
     * @return the mime-type
     */
    public static String getMimeType(Context ctx, Uri file) {
        String scheme = file.getScheme();
        if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            return ctx.getContentResolver().getType(file);
        }
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            String path = file.getPath();
            if (path == null) {
                throw new RuntimeException("Invalid file path for Uri='" + file + "'!");
            }
            String extension = MimeTypeMap.getFileExtensionFromUrl(path);
            if (extension != null) {
                String result = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        extension.toLowerCase(Locale.getDefault()));
                if (result == null) {
                    throw new IllegalArgumentException("Invalid mime type for extension='"
                            + extension + "'!");
                }
                if (Utils.isVideoType(result)) {
                    /*
                     * Warning: Audio and Video files share the same extensions so we need to
                     * retrieve mime type directly from file.
                     */
                    String mimeTypeFromMediaFile = getMimeTypeFromFile(ctx, file);
                    if (mimeTypeFromMediaFile != null) {
                        return mimeTypeFromMediaFile;
                    }
                }
                return result;
            }
            throw new IllegalArgumentException("Invalid extension for URI='" + file + "'!");
        }
        throw new IllegalArgumentException("Unsupported URI scheme '" + scheme + "'!");
    }

    private static Intent forgeIntentToOpenFile() {
        Intent intent;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        return intent;
    }

    /**
     * Open file
     *
     * @param activity the activity
     * @param mimeType the mime type
     * @param action the action
     */
    public static void openFile(Activity activity, String mimeType, int action) {
        Intent intent = forgeIntentToOpenFile();
        intent.setType(mimeType);
        activity.startActivityForResult(intent, action);
    }

    public static void openFiles(Activity activity, String[] mimeTypes, int action) {
        Intent intent = forgeIntentToOpenFile();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        } else {
            intent.setType(mimeTypes[0]);
        }
        activity.startActivityForResult(intent, action);
    }

    /**
     * Saves the read/write permission for later use by the stack.
     *
     * @param file Uri of file to transfer
     */
    public static void takePersistableContentUriPermission(Context context, Uri file) {
        if (!(ContentResolver.SCHEME_CONTENT.equals(file.getScheme()))) {
            return;
        }
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ContentResolver contentResolver = context.getContentResolver();
            contentResolver.takePersistableUriPermission(file,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
    }

    /**
     * Converts byte size into human readable format
     *
     * @param bytes number of bytes to display
     * @param si True is binary units or SI units else.
     * @return String
     */
    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit)
            return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
