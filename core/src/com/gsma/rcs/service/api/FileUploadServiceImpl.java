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

package com.gsma.rcs.service.api;

import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.upload.FileUploadSession;
import com.gsma.rcs.platform.file.FileDescription;
import com.gsma.rcs.platform.file.FileFactory;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.broadcaster.FileUploadEventBroadcaster;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.ICommonServiceConfiguration;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Build.VERSION_CODES;
import com.gsma.services.rcs.upload.IFileUpload;
import com.gsma.services.rcs.upload.IFileUploadListener;
import com.gsma.services.rcs.upload.IFileUploadService;
import com.gsma.services.rcs.upload.IFileUploadServiceConfiguration;

import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * File upload service implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileUploadServiceImpl extends IFileUploadService.Stub {

    private final FileUploadEventBroadcaster mBroadcaster = new FileUploadEventBroadcaster();

    private final InstantMessagingService mImService;

    private final Map<String, IFileUpload> mFileUploadCache = new HashMap<String, IFileUpload>();

    /**
     * The sLogger
     */
    private static final Logger sLogger = Logger.getLogger(FileUploadServiceImpl.class
            .getSimpleName());

    /**
     * Lock used for synchronization
     */
    private Object lock = new Object();

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     * 
     * @param imService InstantMessagingService
     * @param rcsSettings
     */
    public FileUploadServiceImpl(InstantMessagingService imService, RcsSettings rcsSettings) {
        if (sLogger.isActivated()) {
            sLogger.info("File upload service API is loaded");
        }
        mImService = imService;
        mRcsSettings = rcsSettings;
    }

    /**
     * Close API
     */
    public void close() {
        mFileUploadCache.clear();

        if (sLogger.isActivated()) {
            sLogger.info("File upload service API is closed");
        }
    }

    /**
     * Add a file upload in the list
     * 
     * @param filUpload File upload
     * @throws RemoteException
     */
    private void addFileUpload(FileUploadImpl filUpload) throws RemoteException {
        if (sLogger.isActivated()) {
            sLogger.debug("Add a file upload in the list (size=" + mFileUploadCache.size() + ")");
        }

        mFileUploadCache.put(filUpload.getUploadId(), filUpload);
    }

    /**
     * Remove a file upload from the list
     * 
     * @param uploadId Upload ID
     */
    /* package private */void removeFileUpload(String sessionId) {
        if (sLogger.isActivated()) {
            sLogger.debug("Remove a file upload from the list (size=" + mFileUploadCache.size()
                    + ")");
        }

        mFileUploadCache.remove(sessionId);
    }

    /**
     * Returns the configuration of the file upload service
     * 
     * @return Configuration
     * @throws RemoteException
     */
    public IFileUploadServiceConfiguration getConfiguration() throws RemoteException {
        try {
            return new IFileUploadServiceConfigurationImpl(mRcsSettings);

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Uploads a file to the RCS content server. The parameter file contains the URI of the file to
     * be uploaded (for a local or a remote file).
     * 
     * @param file Uri of file to upload
     * @param fileicon File icon option. If true and if it's an image, a file icon is attached.
     * @return File upload
     * @throws RemoteException
     */
    public IFileUpload uploadFile(Uri file, boolean fileicon) throws RemoteException {
        if (file == null) {
            throw new ServerApiIllegalArgumentException("file must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Initiate a file upload session (thumbnail option " + fileicon + ")");
        }
        ServerApiUtils.testCore();
        ServerApiUtils.testIms();
        try {
            mImService.assertAvailableFileTransferSession("Max file transfer sessions achieved.");

            FileDescription desc = FileFactory.getFactory().getFileDescription(file);
            MmContent content = ContentManager
                    .createMmContent(file, desc.getSize(), desc.getName());

            mImService.assertFileSizeNotExceedingMaxLimit(content.getSize(),
                    "File exceeds max size.");

            final FileUploadSession session = new FileUploadSession(content, fileicon, mRcsSettings);

            FileUploadImpl fileUpload = new FileUploadImpl(session.getUploadID(), mBroadcaster,
                    mImService, this, file);
            session.addListener(fileUpload);

            session.startSession();

            addFileUpload(fileUpload);
            return fileUpload;

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Can a file be uploaded now
     * 
     * @return Returns true if a file can be uploaded, else returns false
     * @throws RemoteException
     */
    public boolean canUploadFile() throws RemoteException {
        try {
            if (!ServerApiUtils.isImsConnected()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Cannot upload file now as IMS is not connected.");
                }
                return false;
            }
            if (!mImService.isFileTransferSessionAvailable()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Cannot upload file now as no sessions available.");
                }
                return false;
            }
            return true;

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns the list of file uploads in progress
     * 
     * @return List of file uploads
     * @throws RemoteException
     */
    public List<IBinder> getFileUploads() throws RemoteException {
        if (sLogger.isActivated()) {
            sLogger.info("Get file upload sessions");
        }
        try {
            List<IBinder> fileUploads = new ArrayList<IBinder>(mFileUploadCache.size());
            for (IFileUpload fileUpload : mFileUploadCache.values()) {
                fileUploads.add(fileUpload.asBinder());
            }
            return fileUploads;

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns a current file upload from its unique ID
     * 
     * @param uploadId
     * @return File upload
     * @throws RemoteException
     */
    public IFileUpload getFileUpload(String uploadId) throws RemoteException {
        if (TextUtils.isEmpty(uploadId)) {
            throw new ServerApiIllegalArgumentException("uploadId must not be null or empty!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Get file upload ".concat(uploadId));
        }
        try {
            return mFileUploadCache.get(uploadId);

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Adds a listener on file upload events
     * 
     * @param listener Listener
     * @throws RemoteException
     */
    public void addEventListener(IFileUploadListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Add a file upload event listener");
        }
        try {
            synchronized (lock) {
                mBroadcaster.addEventListener(listener);
            }
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Removes a listener on file upload events
     * 
     * @param listener Listener
     * @throws RemoteException
     */
    public void removeEventListener(IFileUploadListener listener) throws RemoteException {
        if (listener == null) {
            throw new ServerApiIllegalArgumentException("listener must not be null!");
        }
        if (sLogger.isActivated()) {
            sLogger.info("Remove a file upload event listener");
        }
        try {
            synchronized (lock) {
                mBroadcaster.removeEventListener(listener);
            }
        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    /**
     * Returns service version
     * 
     * @return Version
     * @see VERSION_CODES
     */
    public int getServiceVersion() {
        return RcsService.Build.API_VERSION;
    }

    /**
     * Returns the common service configuration
     * 
     * @return the common service configuration
     */
    public ICommonServiceConfiguration getCommonConfiguration() {
        return new CommonServiceConfigurationImpl(mRcsSettings);
    }
}
