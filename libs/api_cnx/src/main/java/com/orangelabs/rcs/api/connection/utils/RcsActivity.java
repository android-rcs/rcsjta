/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010 France Telecom S.A.
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

package com.orangelabs.rcs.api.connection.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;

import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.RcsServiceNotRegisteredException;
import com.gsma.services.rcs.capability.CapabilityService;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.contact.ContactService;
import com.gsma.services.rcs.extension.MultimediaSessionService;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingService;
import com.gsma.services.rcs.sharing.image.ImageSharingService;
import com.gsma.services.rcs.sharing.video.VideoSharingService;
import com.gsma.services.rcs.upload.FileUploadService;
import com.orangelabs.rcs.api.connection.ConnectionManager;
import com.orangelabs.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.api.connection.IRcsActivityFinishable;
import com.orangelabs.rcs.api.connection.R;

/**
 * @author LEMORDANT Philippe
 */
public abstract class RcsActivity extends Activity implements DialogUtil.IRegisterCloseDialog,
        IRcsDialog, IConnectionManager {

    private Dialog mOpenedDialog;

    private LockAccess mLockAcces = new LockAccess();

    private ConnectionManager mCnxManager;

    private IRcsActivityFinishable mIFinishable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCnxManager = ConnectionManager.getInstance();
        if (mIFinishable == null) {
            mIFinishable = new IRcsActivityFinishable() {

                @Override
                public void showMessageThenExit(String msg) {
                    DialogUtil.showMessageThenExit(RcsActivity.this, msg, mLockAcces);
                }
            };
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCnxManager.stopMonitorServices(this);
    }

    @Override
    public void closeDialog() {
        if (mOpenedDialog != null && mOpenedDialog.isShowing()) {
            mOpenedDialog.dismiss();
        }
    }

    @Override
    public void registerDialog(Dialog dialog) {
        mOpenedDialog = dialog;
    }

    @Override
    public void showMessageThenExit(String msg) {
        DialogUtil.showMessageThenExit(this, msg, mLockAcces);
    }

    @Override
    public void showMessageThenExit(int resId) {
        DialogUtil.showMessageThenExit(this, getString(resId), mLockAcces);
    }

    @Override
    public void showExceptionThenExit(Exception e) {
        /*
         * Non bug exception should not produce log of stack trace.
         */
        if (e instanceof RcsServiceNotAvailableException) {
            DialogUtil.showMessageThenExit(this, getString(R.string.label_service_not_available),
                    mLockAcces);

        } else if (e instanceof RcsServiceNotRegisteredException) {
            DialogUtil.showMessageThenExit(this, getString(R.string.error_not_registered),
                    mLockAcces);

        } else {
            DialogUtil.showExceptionThenExit(this, e, mLockAcces);
        }
    }

    @Override
    public AlertDialog showMessage(String msg) {
        return DialogUtil.showMessage(this, msg);
    }

    @Override
    public AlertDialog showMessage(int resId) {
        return DialogUtil.showMessage(this, getString(resId));
    }

    @Override
    public AlertDialog showException(Exception e) {
        return DialogUtil.showException(this, e);
    }

    @Override
    public ProgressDialog showProgressDialog(String msg) {
        return DialogUtil.showProgressDialog(this, msg);
    }

    @Override
    public void startMonitorApiCnx(RcsServiceListener listener, RcsServiceName... services) {
        mCnxManager.startMonitorApiCnx(this, listener, services);
    }

    @Override
    public void startMonitorServices(RcsServiceName... services) {
        mCnxManager.startMonitorServices(this, mIFinishable, services);
    }

    @Override
    public void stopMonitorApiCnx() {
        mCnxManager.stopMonitorServices(this);
    }

    @Override
    public boolean isServiceConnected(RcsServiceName... services) {
        return mCnxManager.isServiceConnected(services);
    }

    @Override
    public CapabilityService getCapabilityApi() {
        return mCnxManager.getCapabilityApi();
    }

    @Override
    public ChatService getChatApi() {
        return mCnxManager.getChatApi();
    }

    @Override
    public ContactService getContactApi() {
        return mCnxManager.getContactApi();
    }

    @Override
    public FileTransferService getFileTransferApi() {
        return mCnxManager.getFileTransferApi();
    }

    @Override
    public VideoSharingService getVideoSharingApi() {
        return mCnxManager.getVideoSharingApi();
    }

    @Override
    public ImageSharingService getImageSharingApi() {
        return mCnxManager.getImageSharingApi();
    }

    @Override
    public GeolocSharingService getGeolocSharingApi() {
        return mCnxManager.getGeolocSharingApi();
    }

    @Override
    public FileUploadService getFileUploadApi() {
        return mCnxManager.getFileUploadApi();
    }

    @Override
    public MultimediaSessionService getMultimediaSessionApi() {
        return mCnxManager.getMultimediaSessionApi();
    }

    public boolean isExiting() {
        return mLockAcces.isLocked();
    }
}
