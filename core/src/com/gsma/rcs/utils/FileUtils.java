/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.utils;

import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.utils.logger.Logger;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Process;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * File utilities
 * 
 * @author YPLO6403
 */
public class FileUtils {

    private static final Logger sLogger = Logger.getLogger(FileUtils.class.getSimpleName());

    /**
     * Copy a file to a directory
     * 
     * @param srcFile the source file (may not be null)
     * @param destDir the destination directory (may not be null)
     * @param preserveFileDate whether to preserve the file date
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public static void copyFileToDirectory(File srcFile, File destDir, boolean preserveFileDate)
            throws IOException, IllegalArgumentException {
        if (srcFile == null) {
            throw new IllegalArgumentException("Source is null");
        }
        if (srcFile.exists() == false) {
            throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
        }
        if (srcFile.isDirectory()) {
            throw new IOException("Source '" + srcFile + "' is a directory");
        }
        if (destDir == null) {
            throw new IllegalArgumentException("Destination is null");
        }
        if (destDir.exists() == false) {
            // Create directory if it does not exist
            if (destDir.mkdir() == false) {
                throw new IOException("Destination '" + destDir + "' directory cannot be created");
            }
        } else {
            if (destDir.isDirectory() == false) {
                throw new IllegalArgumentException("Destination '" + destDir
                        + "' is not a directory");
            }
        }
        File destFile = new File(destDir, srcFile.getName());
        if (destFile.exists() && destFile.canWrite() == false) {
            throw new IOException("Destination '" + destFile + "' file exists but is read-only");
        }
        FileInputStream input = new FileInputStream(srcFile);
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(destFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
        } finally {
            CloseableUtils.tryToClose(input);
            CloseableUtils.tryToClose(output);
        }
        // check if full content is copied
        if (srcFile.length() != destFile.length()) {
            throw new IOException("Failed to copy from '" + srcFile + "' to '" + destFile + "'");
        }
        // preserve the file date
        if (preserveFileDate)
            destFile.setLastModified(srcFile.lastModified());
    }

    /**
     * get the oldest file from the list
     * 
     * @param files list of files
     * @return the oldest one or null
     */
    public static File getOldestFile(final File[] files) {
        if (files == null || files.length == 0) {
            return null;
        }
        File result = null;
        for (File file : files) {
            if (result == null) {
                result = file;
            } else {
                if (file.lastModified() < result.lastModified()) {
                    result = file;
                }
            }
        }
        return result;
    }

    /**
     * Delete a directory recursively
     * 
     * @param dir the directory
     * @throws IOException
     */
    public static void deleteDirectory(File dir) throws IOException {
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(new StringBuilder(dir.getPath()).append(
                    " should always be a directory!").toString());
        }
        String[] children = dir.list();
        for (String childname : children) {
            File child = new File(dir, childname);
            if (child.isDirectory()) {
                deleteDirectory(child);
                if (!child.delete()) {
                    throw new IOException(new StringBuilder("Failed to delete file : ").append(
                            child.getPath()).toString());
                }
            } else {
                if (!child.delete()) {
                    throw new IOException(new StringBuilder("Failed to delete file : ").append(
                            child.getPath()).toString());
                }
            }
        }
        if (!dir.delete()) {
            throw new IOException(new StringBuilder("Failed to delete directory : ").append(
                    dir.getPath()).toString());
        }
    }

    /**
     * Fetch the file name from URI
     * 
     * @param ctx Context
     * @param file URI
     * @return fileName String
     */
    public static String getFileName(Context ctx, Uri file) {
        String scheme = file.getScheme();
        Cursor cursor = null;
        try {
            cursor = ctx.getContentResolver().query(file, null, null, null, null);
            if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
                if (cursor != null && cursor.moveToFirst()) {
                    String displayName = cursor.getString(cursor
                            .getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                    return displayName;
                }
                throw new IllegalArgumentException("Error in retrieving file name from the URI");

            } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
                return file.getLastPathSegment();

            } else {
                throw new IllegalArgumentException("Unsupported URI scheme '" + scheme + "'!");
            }
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Fetch the file size from URI
     * 
     * @param ctx Context
     * @param file URI
     * @return fileSize long
     */
    public static long getFileSize(Context ctx, Uri file) {
        String scheme = file.getScheme();
        Cursor cursor = null;
        try {
            cursor = ctx.getContentResolver().query(file, null, null, null, null);
            if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
                if (cursor != null && cursor.moveToFirst()) {
                    return Long.valueOf(
                            cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)))
                            .longValue();
                }
                throw new IllegalArgumentException("Error in retrieving file size form the URI");

            } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
                return (new File(file.getPath())).length();

            } else {
                throw new IllegalArgumentException("Unsupported URI scheme '" + scheme + "'!");
            }
        } finally {
            CursorUtil.close(cursor);
        }
    }

    /**
     * Test if the stack can read data from this Uri.
     * 
     * @param file
     * @return
     */
    public static boolean isReadFromUriPossible(Context ctx, Uri file) {
        String scheme = file.getScheme();
        if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            InputStream stream = null;
            try {
                if (PackageManager.PERMISSION_GRANTED == ctx.checkUriPermission(file,
                        Process.myPid(), Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION)) {
                    return true;
                }
                stream = ctx.getContentResolver().openInputStream(file);
                stream.read();
                return true;

            } catch (SecurityException e) {
                sLogger.error(new StringBuilder("Failed to read from uri :").append(file)
                        .toString(), e);
                return false;

            } catch (IOException e) {
                if (sLogger.isActivated()) {
                    sLogger.debug(new StringBuilder("Failed to read from uri :").append(file)
                            .append(", Message=").append(e.getMessage()).toString());
                }
                return false;

            } finally {
                CloseableUtils.tryToClose(stream);
            }
        } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            String path = file.getPath();
            if (path == null) {
                sLogger.error("Failed to read from uri :".concat(file.toString()));
                return false;
            }
            try {
                return new File(path).canRead();

            } catch (SecurityException e) {
                sLogger.error(new StringBuilder("Failed to read from uri :").append(file)
                        .toString(), e);
                return false;
            }

        } else {
            throw new IllegalArgumentException("Unsupported URI scheme '" + scheme + "'!");
        }
    }
}
