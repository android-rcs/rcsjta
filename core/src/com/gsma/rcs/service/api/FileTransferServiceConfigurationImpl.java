/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.rcs.service.api;

import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.filetransfer.FileTransferServiceConfiguration.ImageResizeOption;
import com.gsma.services.rcs.filetransfer.IFileTransferServiceConfiguration;

import android.os.RemoteException;

/**
 * A class that implements interface to allow access to file transfer service configuration from API
 * 
 * @author yplo6403
 */
public class FileTransferServiceConfigurationImpl extends IFileTransferServiceConfiguration.Stub {

    private final Logger mLogger = Logger.getLogger(FileTransferServiceConfigurationImpl.class
            .getSimpleName());

    private final RcsSettings mRcsSettings;

    /**
     * @param rcsSettings RCS settings accessor
     */
    public FileTransferServiceConfigurationImpl(RcsSettings rcsSettings) {
        mRcsSettings = rcsSettings;
    }

    @Override
    public void setImageResizeOption(int option) throws RemoteException {
        try {
            mRcsSettings.setImageResizeOption(ImageResizeOption.valueOf(option));
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public void setAutoAcceptInRoaming(boolean enable) throws RemoteException {
        try {
            if (!mRcsSettings.isFtAutoAcceptedModeChangeable()) {
                throw new ServerApiUnsupportedOperationException(
                        "Auto accept mode in roaming is not changeable");
            }
            if (!mRcsSettings.isFileTransferAutoAccepted()) {
                throw new ServerApiPermissionDeniedException(
                        "Auto accept mode in normal conditions must be enabled");
            }
            mRcsSettings.setFileTransferAutoAcceptedInRoaming(enable);
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public void setAutoAccept(boolean enable) throws RemoteException {
        try {
            if (!mRcsSettings.isFtAutoAcceptedModeChangeable()) {
                throw new ServerApiUnsupportedOperationException(
                        "Auto accept mode is not changeable");
            }
            mRcsSettings.setFileTransferAutoAccepted(enable);
            if (!enable) {
                /* If AA is disabled in normal conditions then it must be disabled while roaming. */
                mRcsSettings.setFileTransferAutoAcceptedInRoaming(false);
            }
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public boolean isGroupFileTransferSupported() throws RemoteException {
        try {
            return mRcsSettings.getMyCapabilities().isFileTransferHttpSupported()
                    && mRcsSettings.isGroupChatActivated();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public boolean isAutoAcceptModeChangeable() throws RemoteException {
        try {
            return mRcsSettings.isFtAutoAcceptedModeChangeable();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public boolean isAutoAcceptInRoamingEnabled() throws RemoteException {
        try {
            return mRcsSettings.isFileTransferAutoAcceptedInRoaming();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public boolean isAutoAcceptEnabled() throws RemoteException {
        try {
            return mRcsSettings.isFileTransferAutoAccepted();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public long getWarnSize() throws RemoteException {
        try {
            return mRcsSettings.getWarningMaxFileTransferSize();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public long getMaxSize() throws RemoteException {
        try {
            return mRcsSettings.getMaxFileTransferSize();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public int getMaxFileTransfers() throws RemoteException {
        try {
            return mRcsSettings.getMaxFileTransferSessions();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public int getImageResizeOption() throws RemoteException {
        try {
            return mRcsSettings.getImageResizeOption().toInt();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                mLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            mLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }
}
