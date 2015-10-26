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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.widget.Toast;

import java.util.Random;
import java.util.Set;

/**
 * Utility functions
 * 
 * @author Jean-Marc AUFFRET
 */
public class Utils {

    private static final Random sPendingIntentIdGenerator = new Random();

    private static final String LOGTAG = LogUtils.getTag(Utils.class.getSimpleName());

    /**
     * Gets a unique ID for pending intent
     * 
     * @return unique ID for pending intent
     */
    public static int getUniqueIdForPendingIntent() {
        return sPendingIntentIdGenerator.nextInt();
    }

    /**
     * Returns the application version from manifest file
     * 
     * @param ctx Context
     * @return Application version or null if not found
     */
    public static String getApplicationVersion(Context ctx) {
        String version = null;
        try {
            PackageInfo info = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            version = info.versionName;
        } catch (NameNotFoundException ignored) {
        }
        return version;
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
     * Show a picture and exit activity
     * 
     * @param activity Activity
     * @param uri Picture to be displayed
     */
    public static void showPictureAndExit(final Activity activity, Uri uri) {
        if (activity.isFinishing()) {
            return;
        }
        String filename = FileUtils.getFileName(activity, uri);
        Toast.makeText(activity, activity.getString(R.string.label_receive_image, filename),
                Toast.LENGTH_LONG).show();
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "image/*");
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
        value.append(currentSize / 1024);
        if (totalSize != 0) {
            value.append('/');
            value.append(totalSize / 1024);
        }
        value.append(" Kb");
        return value.toString();
    }

}
