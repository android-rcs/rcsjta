/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
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
 ******************************************************************************/

package com.gsma.rcs.api.connection.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.util.Log;

import com.gsma.rcs.api.connection.R;

/**
 * Dialog utility
 *
 * @author Philippe LEMORDANT
 */
/* package private */class DialogUtil {

    private static final String LOGTAG = LogUtils.getTag(DialogUtil.class.getSimpleName());

    /**
     * Show a message then exit activity
     *
     * @param activity the activity.
     * @param msg the message
     * @param locker the locker
     */
    public static void showMessageThenExit(final Activity activity, String msg, LockAccess locker) {
        showMessageThenExit(activity, msg, locker, null);
    }

    /**
     * Show a message then exit activity
     *
     * @param activity Activity
     * @param msg Message to be displayed
     * @param locker a locker to only execute once
     * @param e the exception
     */
    private static void showMessageThenExit(final Activity activity, String msg, LockAccess locker,
            Exception e) {
        /* Do not execute if already executed once */
        if (locker != null && !locker.tryLock()) {
            return;
        }
        if (e != null) {
            Log.e(LOGTAG, "Exception enforces exit of activity!");
            Log.e(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        } else {
            if (LogUtils.isActive) {
                Log.w(LOGTAG,
                        "Exit activity " + activity.getLocalClassName() + " <" + msg + ">");
            }
        }
        /* Do not execute if activity is Finishing */
        if (activity.isFinishing()) {
            return;
        }
        if (activity instanceof DialogUtil.IRegisterCloseDialog) {
            ((DialogUtil.IRegisterCloseDialog) activity).closeDialog();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(msg);
        builder.setTitle((e == null) ? R.string.title_msg : R.string.error_exception);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                activity.finish();
            }
        });
        builder.show();
    }

    /**
     * Show exception then exit activity
     *
     * @param activity Activity
     * @param e the exception
     * @param locker a locker to only execute once
     */
    public static void showExceptionThenExit(final Activity activity, Exception e, LockAccess locker) {
        showMessageThenExit(activity, e.getMessage(), locker, e);
    }

    /**
     * Show message
     *
     * @param activity Activity
     * @param msg Message to be displayed
     * @return Dialog
     */
    public static AlertDialog showMessage(Activity activity, String msg) {
        if (LogUtils.isActive) {
            Log.w(LOGTAG, "Activity " + activity.getLocalClassName() + " message=" + msg);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(msg);
        builder.setTitle(R.string.title_msg);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.label_ok, null);
        AlertDialog alert = builder.show();
        if (activity instanceof DialogUtil.IRegisterCloseDialog) {
            ((DialogUtil.IRegisterCloseDialog) activity).registerDialog(alert);
        }
        return alert;
    }

    /**
     * @param activity the activity
     * @param e exception to show
     * @return alert dialog
     */
    public static AlertDialog showException(Activity activity, Exception e) {
        Log.e(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        /* Do not execute if activity is Finishing */
        if (activity.isFinishing()) {
            return null;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        String message = e.getMessage();
        builder.setMessage((message != null) ? activity.getString(R.string.error_exception_message,
                message) : activity.getString(R.string.error_exception));
        builder.setTitle(R.string.title_msg);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.label_ok, null);
        AlertDialog alert = builder.show();
        if (activity instanceof DialogUtil.IRegisterCloseDialog) {
            ((DialogUtil.IRegisterCloseDialog) activity).registerDialog(alert);
        }
        return alert;
    }

    /**
     * Show an exception
     *
     * @param activity Activity
     * @param e Exception to be displayed
     * @return Dialog
     */

    /**
     * Show a progress dialog with the given parameters
     *
     * @param activity Activity
     * @param msg Message to be displayed
     * @return Dialog
     */
    public static ProgressDialog showProgressDialog(Activity activity, String msg) {
        ProgressDialog dlg = new ProgressDialog(activity);
        dlg.setMessage(msg);
        dlg.setIndeterminate(true);
        dlg.setCancelable(true);
        dlg.setCanceledOnTouchOutside(false);
        dlg.show();
        if (activity instanceof DialogUtil.IRegisterCloseDialog) {
            ((DialogUtil.IRegisterCloseDialog) activity).registerDialog(dlg);
        }
        return dlg;
    }

    public interface IRegisterCloseDialog {

        /**
         * Closes dialog (if opened).
         */
        void closeDialog();

        /**
         * Registers the dialog. Activities displaying dialog must register it to enable automatic
         * closing upon exception.
         *
         * @param dialog the dialog instance
         */
        void registerDialog(Dialog dialog);
    }

}
