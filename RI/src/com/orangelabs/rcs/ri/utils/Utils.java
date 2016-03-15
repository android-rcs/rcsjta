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

import com.orangelabs.rcs.ri.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Utility functions
 * 
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class Utils {

    private static final Random sPendingIntentIdGenerator = new Random();

    /**
     * Gets a unique ID for pending intent
     * 
     * @return unique ID for pending intent
     */
    public static int getUniqueIdForPendingIntent() {
        return sPendingIntentIdGenerator.nextInt();
    }

    /**
     * Display a toast
     * 
     * @param ctx Context
     * @param message Message to be displayed
     */
    public static void displayToast(Context ctx, String message) {
        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Display a long toast
     * 
     * @param ctx Context
     * @param message Message to be displayed
     */
    public static void displayLongToast(Context ctx, String message) {
        Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Shows a picture
     * 
     * @param activity Activity
     * @param uri Picture to be displayed
     */
    public static void showPicture(final Activity activity, Uri uri) {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "image/*");
        activity.startActivity(intent);
    }

    /**
     * Checks if mime type is an image type
     *
     * @param mime MIME type
     * @return True if mime type is an image type
     */
    public static boolean isImageType(String mime) {
        return mime.toLowerCase(Locale.getDefault()).startsWith("image/");
    }

    /**
     * Checks if mime type is an audio type
     *
     * @param mime MIME type
     * @return True if mime type is an audio type
     */
    public static boolean isAudioType(String mime) {
        return mime.toLowerCase(Locale.getDefault()).startsWith("audio/");
    }

    /**
     * Checks if mime type is an video type
     *
     * @param mime MIME type
     * @return True if mime type is an video type
     */
    public static boolean isVideoType(String mime) {
        return mime.toLowerCase(Locale.getDefault()).startsWith("video/");
    }

    /**
     * Plays audio document
     * 
     * @param activity Activity
     * @param uri Uri of audio file to be played
     */
    public static void playAudio(final Activity activity, Uri uri) {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "audio/");
        activity.startActivity(intent);
    }

    /**
     * Show an info with a specific title
     * 
     * @param activity Activity
     * @param title Title of the dialog
     * @param items List of items
     * @return dialog
     */
    public static AlertDialog showList(Activity activity, String title, Set<String> items) {
        CharSequence[] chars = items.toArray(new CharSequence[items.size()]);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.label_ok, null);
        builder.setItems(chars, null);
        return builder.show();
    }

    /**
     * Gets the label to show the progress status
     * 
     * @param currentSize Current size in bytes transferred
     * @param totalSize Total size in bytes to be transferred
     * @return progress label
     */
    public static String getProgressLabel(long currentSize, long totalSize) {
        StringBuilder value = new StringBuilder();
        value.append(FileUtils.humanReadableByteCount(currentSize, true));
        if (totalSize != 0) {
            value.append('/');
            value.append(FileUtils.humanReadableByteCount(totalSize, true));
        }
        return value.toString();
    }

}
