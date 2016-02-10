/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.utils;

import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
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
        if (!srcFile.exists()) {
            throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
        }
        if (srcFile.isDirectory()) {
            throw new IOException("Source '" + srcFile + "' is a directory");
        }
        if (destDir == null) {
            throw new IllegalArgumentException("Destination is null");
        }
        if (!destDir.exists()) {
            // Create directory if it does not exist
            if (!destDir.mkdir()) {
                throw new IOException("Destination '" + destDir + "' directory cannot be created");
            }
        } else {
            if (!destDir.isDirectory()) {
                throw new IllegalArgumentException("Destination '" + destDir
                        + "' is not a directory");
            }
        }
        File destFile = new File(destDir, srcFile.getName());
        if (destFile.exists() && !destFile.canWrite()) {
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
            throw new IllegalArgumentException(dir.getPath() + " should always be a directory!");
        }
        String[] children = dir.list();
        for (String childname : children) {
            File child = new File(dir, childname);
            if (child.isDirectory()) {
                deleteDirectory(child);
                if (!child.delete()) {
                    throw new IOException("Failed to delete file : " + child.getPath());
                }
            } else {
                if (!child.delete()) {
                    throw new IOException("Failed to delete file : " + child.getPath());
                }
            }
        }
        if (!dir.delete()) {
            throw new IOException("Failed to delete directory : " + dir.getPath());
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
                    /*
                     * Warning: OpenableColumns.DISPLAY_NAME does not have to be a filename (eg. for
                     * audio files)
                     */
                    return cursor.getString(cursor
                            .getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
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
                    return Long.valueOf(cursor.getString(cursor
                            .getColumnIndexOrThrow(OpenableColumns.SIZE)));
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
     * @param file the file Uri
     * @return True is the stack can read data from this Uri.
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
                sLogger.error("Failed to read from uri :" + file, e);
                return false;

            } catch (IOException e) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Failed to read from uri :" + file + ", Message="
                            + e.getMessage());
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
                sLogger.error("Failed to read from uri :" + file, e);
                return false;
            }

        } else {
            throw new IllegalArgumentException("Unsupported URI scheme '" + scheme + "'!");
        }
    }

    /**
     * Copies a file. The destination is overwritten if it already exists.
     *
     * @param source Uri of the source file
     * @param destination Uri of the destination file
     * @throws IOException if the copy operation fails
     */
    private static void copyFile(Uri source, Uri destination) throws IOException {
        FileInputStream sourceStream = null;
        FileOutputStream destStream = null;
        try {
            sourceStream = (FileInputStream) AndroidFactory.getApplicationContext()
                    .getContentResolver().openInputStream(source);
            destStream = (FileOutputStream) AndroidFactory.getApplicationContext()
                    .getContentResolver().openOutputStream(destination);
            byte buffer[] = new byte[1024];
            int length;

            while ((length = sourceStream.read(buffer)) > 0) {
                destStream.write(buffer, 0, length);
            }
        } finally {
            CloseableUtils.tryToClose(sourceStream);
            CloseableUtils.tryToClose(destStream);
        }
    }

    private static String getMimeTypeFromFile(Context ctx, Uri file) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(ctx, file);
        return mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
    }

    /**
     * Create copy of sent file in respective sent directory.
     *
     * @param file The file Uri to copy
     * @param rcsSettings The RcsSettings accessor
     * @return Uri of copied file
     * @throws IOException
     */
    public static Uri createCopyOfSentFile(Uri file, RcsSettings rcsSettings) throws IOException {
        String mimeType = getMimeType(file);
        Context ctx = AndroidFactory.getApplicationContext();
        String fileName = getFileName(ctx, file);
        String extension = MimeManager.getFileExtension(fileName);
        /*
         * Checks if filename contains extension.
         */
        if (extension == null) {
            /*
             * If extension is not provided by filename then guess extension from MimeType.
             */
            extension = MimeManager.getInstance().getExtensionFromMimeType(mimeType);
            if (extension == null) {
                throw new RuntimeException("Cannot retrieve file extension for Uri='" + file + "'!");
            }
            fileName = fileName + "." + extension;
        }
        Uri destination = ContentManager.generateUriForSentContent(fileName, mimeType, rcsSettings);
        copyFile(file, destination);
        return destination;
    }

    /**
     * Gets the mime-type from file Uri
     * 
     * @param file the file Uri
     * @return the mime-type or null
     */
    public static String getMimeType(Uri file) {
        String scheme = file.getScheme();
        Context ctx = AndroidFactory.getApplicationContext();
        ContentResolver cr = ctx.getContentResolver();
        if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            return cr.getType(file);
        }
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            String path = file.getPath();
            if (path == null) {
                throw new RuntimeException("Invalid file path for Uri='" + file + "'!");
            }
            String extension = MimeManager.getFileExtension(path);
            if (extension == null) {
                throw new IllegalArgumentException("No file extension Uri='" + file + "'!");
            }
            String mimeType = MimeManager.getInstance().getMimeType(extension);
            if (MimeManager.isVideoType(mimeType)) {
                /*
                 * Warning: Audio and Video files share the same extensions so we need to retrieve
                 * mime type directly from file.
                 */
                String mimeTypeFromMediaFile = getMimeTypeFromFile(ctx, file);
                if (mimeTypeFromMediaFile != null) {
                    mimeType = mimeTypeFromMediaFile;
                }
            }
            return mimeType;
        }
        throw new IllegalArgumentException("Unsupported URI scheme '" + scheme + "'!");
    }

    /**
     * Gets mime type from extesion pparsed from path or filename
     * 
     * @param pathOrFilename the path or filename
     * @return the mime type or null if not found
     */

    public static String getMimeTypeFromExtension(String pathOrFilename) {
        String ext = MimeManager.getFileExtension(pathOrFilename);
        return MimeManager.getInstance().getMimeType(ext);
    }
}
