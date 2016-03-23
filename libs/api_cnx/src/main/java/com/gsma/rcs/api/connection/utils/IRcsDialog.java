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

import android.app.AlertDialog;
import android.app.ProgressDialog;

/**
 * Interface to show an alert dialog to display message or exception and optionally to exit
 * activity.
 *
 * @author LEMORDANT Philippe
 */
public interface IRcsDialog {

    /**
     * Show message in alert dialog then exit activity
     *
     * @param msg The message to show
     */
    void showMessageThenExit(String msg);

    /**
     * Show message in alert dialog then exit activity
     *
     * @param resId The message to show
     */
    void showMessageThenExit(int resId);

    /**
     * Show exception in alert dialog then exit activity
     *
     * @param e The exception to show
     */
    void showExceptionThenExit(Exception e);

    /**
     * Show message in alert dialog
     *
     * @param msg The message to show
     * @return the alert dialog
     */
    AlertDialog showMessage(String msg);

    /**
     * Show message in alert dialog
     *
     * @param resId The message to show
     * @return the alert dialog
     */
    AlertDialog showMessage(int resId);

    /**
     * Show exception in alert dialog
     *
     * @param e The exception to show
     * @return the alert dialog
     */
    AlertDialog showException(Exception e);

    /**
     * Show progress in alert dialog
     *
     * @param progress The progress to show
     * @return the alert dialog
     */
    ProgressDialog showProgressDialog(String progress);
}
