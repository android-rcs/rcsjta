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

package com.gsma.services.rcs.filetransfer;

import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceControl;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceListener.ReasonCode;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contact.ContactId;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.IBinder;
import android.os.IInterface;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * This class offers the main entry point to transfer files and to receive files. Several
 * applications may connect/disconnect to the API. The parameter contact in the API supports the
 * following formats: MSISDN in national or international format, SIP address, SIP-URI or Tel-URI.
 * 
 * @author Jean-Marc AUFFRET
 */
public final class FileTransferService extends RcsService {

    /**
     * API
     */
    private IFileTransferService mApi;

    private static final String ERROR_CNX = "FileTransfer service not connected";

    private final Map<OneToOneFileTransferListener, WeakReference<IOneToOneFileTransferListener>> mOneToOneFileTransferListeners = new WeakHashMap<OneToOneFileTransferListener, WeakReference<IOneToOneFileTransferListener>>();

    private final Map<GroupFileTransferListener, WeakReference<IGroupFileTransferListener>> mGroupFileTransferListeners = new WeakHashMap<GroupFileTransferListener, WeakReference<IGroupFileTransferListener>>();

    private static boolean sApiCompatible = false;

    /**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public FileTransferService(Context ctx, RcsServiceListener listener) {
        super(ctx, listener);
    }

    /**
     * Connects to the API
     * 
     * @throws RcsPermissionDeniedException
     */
    public final void connect() throws RcsPermissionDeniedException {
        if (!sApiCompatible) {
            try {
                sApiCompatible = mRcsServiceControl.isCompatible(this);
                if (!sApiCompatible) {
                    throw new RcsPermissionDeniedException(
                            "The TAPI client version of the file transfer service is not compatible with the TAPI service implementation version on this device!");
                }
            } catch (RcsServiceException e) {
                throw new RcsPermissionDeniedException(
                        "The compatibility of TAPI client version with the TAPI service implementation version of this device cannot be checked for the file transfer service!");
            }
        }
        Intent serviceIntent = new Intent(IFileTransferService.class.getName());
        serviceIntent.setPackage(RcsServiceControl.RCS_STACK_PACKAGENAME);
        mCtx.bindService(serviceIntent, apiConnection, 0);
    }

    /**
     * Disconnects from the API
     */
    public void disconnect() {
        try {
            mCtx.unbindService(apiConnection);
        } catch (IllegalArgumentException e) {
            // Nothing to do
        }
    }

    /**
     * Set API interface
     * 
     * @param api API interface
     */
    protected void setApi(IInterface api) {
        super.setApi(api);
        mApi = (IFileTransferService) api;
    }

    /**
     * Service connection
     */
    private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            setApi(IFileTransferService.Stub.asInterface(service));
            if (mListener != null) {
                mListener.onServiceConnected();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            setApi(null);
            if (mListener == null) {
                return;
            }
            ReasonCode reasonCode = ReasonCode.CONNECTION_LOST;
            try {
                if (!mRcsServiceControl.isActivated()) {
                    reasonCode = ReasonCode.SERVICE_DISABLED;
                }
            } catch (RcsServiceException e) {
                // Do nothing
            }
            mListener.onServiceDisconnected(reasonCode);
        }
    };

    /**
     * Granting temporary read Uri permission from client to stack service if it is a content URI
     * 
     * @param file Uri of file to grant permission
     */
    private void tryToGrantUriPermissionToStackServices(Uri file) {
        if (!ContentResolver.SCHEME_CONTENT.equals(file.getScheme())) {
            return;
        }
        Intent fileTransferServiceIntent = new Intent(IFileTransferService.class.getName());
        List<ResolveInfo> stackServices = mCtx.getPackageManager().queryIntentServices(
                fileTransferServiceIntent, 0);
        for (ResolveInfo stackService : stackServices) {
            mCtx.grantUriPermission(stackService.serviceInfo.packageName, file,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    /**
     * Returns the configuration of the file transfer service
     * 
     * @return Configuration
     * @throws RcsServiceException
     */
    public FileTransferServiceConfiguration getConfiguration() throws RcsServiceException {
        if (mApi != null) {
            try {
                return new FileTransferServiceConfiguration(mApi.getConfiguration());
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Returns true if it is possible to initiate file transfer to the contact specified by the
     * contact parameter, else returns false.
     * 
     * @param contact
     * @return boolean
     * @throws RcsServiceException
     */
    public boolean isAllowedToTransferFile(ContactId contact) throws RcsServiceException {
        if (mApi != null) {
            try {
                return mApi.isAllowedToTransferFile(contact);
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Transfers a file to a contact. The parameter file contains the URI of the file to be
     * transferred (for a local or a remote file). The parameter contact supports the following
     * formats: MSISDN in national or international format, SIP address, SIP-URI or Tel-URI. If the
     * format of the contact is not supported an exception is thrown.
     * 
     * @param contact the remote contact Identifier
     * @param file Uri of file to transfer
     * @param attachFileIcon File icon option. If true, the stack tries to attach fileicon. Fileicon
     *            may not be attached if file is not an image or if local or remote contact does not
     *            support fileicon.
     * @return File transfer
     * @throws RcsServiceException
     */
    public FileTransfer transferFile(ContactId contact, Uri file, boolean attachFileIcon)
            throws RcsServiceException {
        if (mApi != null) {
            try {
                /* Only grant permission for content Uris */
                tryToGrantUriPermissionToStackServices(file);
                IFileTransfer ftIntf = mApi.transferFile(contact, file, attachFileIcon);
                if (ftIntf != null) {
                    return new FileTransfer(ftIntf);
                } else {
                    return null;
                }
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Returns true if it is possible to initiate file transfer to the group chat specified by the
     * chatId parameter, else returns false.
     * 
     * @param chatId
     * @return boolean
     * @throws RcsServiceException
     */
    public boolean isAllowedToTransferFileToGroupChat(String chatId) throws RcsServiceException {
        if (mApi != null) {
            try {
                return mApi.isAllowedToTransferFileToGroupChat(chatId);
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Transfers a file to a group chat with an optional file icon.
     * 
     * @param chatId
     * @param file Uri of file to transfer
     * @param attachFileIcon Attach file icon option. If true, the stack tries to attach fileIcon.
     *            FileIcon may not be attached if file is not an image or if local or remote contact
     *            does not support fileIcon.
     * @return FileTransfer
     * @throws RcsServiceException
     */
    public FileTransfer transferFileToGroupChat(String chatId, Uri file, boolean attachFileIcon)
            throws RcsServiceException {
        if (mApi != null) {
            try {
                /* Only grant permission for content Uris */
                tryToGrantUriPermissionToStackServices(file);
                IFileTransfer ftIntf = mApi.transferFileToGroupChat(chatId, file, attachFileIcon);
                if (ftIntf != null) {
                    return new FileTransfer(ftIntf);
                } else {
                    return null;
                }
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Mark a received file transfer as read (i.e. the invitation or the file has been displayed in
     * the UI).
     * 
     * @param transferId
     * @throws RcsServiceException
     */
    public void markFileTransferAsRead(String transferId) throws RcsServiceException {
        if (mApi != null) {
            try {
                mApi.markFileTransferAsRead(transferId);
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Returns the list of file transfers in progress
     * 
     * @return List of file transfers
     * @throws RcsServiceException
     */
    public Set<FileTransfer> getFileTransfers() throws RcsServiceException {
        if (mApi != null) {
            try {
                Set<FileTransfer> result = new HashSet<FileTransfer>();
                List<IBinder> ftList = mApi.getFileTransfers();
                for (IBinder binder : ftList) {
                    FileTransfer ft = new FileTransfer(IFileTransfer.Stub.asInterface(binder));
                    result.add(ft);
                }
                return result;
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Returns a current file transfer from its unique ID
     * 
     * @param transferId
     * @return File transfer or null if not found
     * @throws RcsServiceException
     */
    public FileTransfer getFileTransfer(String transferId) throws RcsServiceException {
        if (mApi != null) {
            try {
                IFileTransfer ftIntf = mApi.getFileTransfer(transferId);
                if (ftIntf != null) {
                    return new FileTransfer(ftIntf);
                } else {
                    return null;
                }
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Adds a listener on file transfer events
     * 
     * @param listener One-to-one file transfer listener
     * @throws RcsServiceException
     */
    public void addEventListener(OneToOneFileTransferListener listener) throws RcsServiceException {
        if (mApi != null) {
            try {
                IOneToOneFileTransferListener rcsListener = new OneToOneFileTransferListenerImpl(
                        listener);
                mOneToOneFileTransferListeners.put(listener,
                        new WeakReference<IOneToOneFileTransferListener>(rcsListener));
                mApi.addEventListener2(rcsListener);
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Removes a listener on file transfer events
     * 
     * @param listener File transfer listener
     * @throws RcsServiceException
     */
    public void removeEventListener(OneToOneFileTransferListener listener)
            throws RcsServiceException {
        if (mApi != null) {
            try {
                WeakReference<IOneToOneFileTransferListener> weakRef = mOneToOneFileTransferListeners
                        .remove(listener);
                if (weakRef == null) {
                    return;
                }
                IOneToOneFileTransferListener rcsListener = weakRef.get();
                if (rcsListener != null) {
                    mApi.removeEventListener2(rcsListener);
                }
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Adds a listener on group file transfer events
     * 
     * @param listener Group file transfer listener
     * @throws RcsServiceException
     */
    public void addEventListener(GroupFileTransferListener listener) throws RcsServiceException {
        if (mApi != null) {
            try {
                IGroupFileTransferListener rcsListener = new GroupFileTransferListenerImpl(listener);
                mGroupFileTransferListeners.put(listener,
                        new WeakReference<IGroupFileTransferListener>(rcsListener));
                mApi.addEventListener3(rcsListener);
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Removes a listener on group file transfer events
     * 
     * @param listener Group file transfer listener
     * @throws RcsServiceException
     */
    public void removeEventListener(GroupFileTransferListener listener) throws RcsServiceException {
        if (mApi != null) {
            try {
                WeakReference<IGroupFileTransferListener> weakRef = mGroupFileTransferListeners
                        .remove(listener);
                if (weakRef == null) {
                    return;
                }
                IGroupFileTransferListener rcsListener = weakRef.get();
                if (rcsListener != null) {
                    mApi.removeEventListener3(rcsListener);
                }
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Deletes all one to one file transfer from history and abort/reject any associated ongoing
     * session if such exists.
     * 
     * @throws RcsServiceException
     */
    public void deleteOneToOneFileTransfers() throws RcsServiceException {
        if (mApi != null) {
            try {
                mApi.deleteOneToOneFileTransfers();
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Deletes all group file transfer from history and abort/reject any associated ongoing session
     * if such exists.
     * 
     * @throws RcsServiceException
     */
    public void deleteGroupFileTransfers() throws RcsServiceException {
        if (mApi != null) {
            try {
                mApi.deleteGroupFileTransfers();
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Deletes file transfer corresponding to a given one to one chat specified by contact from
     * history and abort/reject any associated ongoing session if such exists.
     * 
     * @param contact
     * @throws RcsServiceException
     */
    public void deleteOneToOneFileTransfers(ContactId contact) throws RcsServiceException {
        if (mApi != null) {
            try {
                mApi.deleteOneToOneFileTransfers2(contact);
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Deletes file transfer corresponding to a given group chat specified by chat id from history
     * and abort/reject any associated ongoing session if such exists.
     * 
     * @param chatId
     * @throws RcsServiceException
     */
    public void deleteGroupFileTransfers(String chatId) throws RcsServiceException {
        if (mApi != null) {
            try {
                mApi.deleteGroupFileTransfers2(chatId);
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Deletes a file transfer by its unique id from history and abort/reject any associated ongoing
     * session if such exists.
     * 
     * @param transferId
     * @throws RcsServiceException
     */
    public void deleteFileTransfer(String transferId) throws RcsServiceException {
        if (mApi != null) {
            try {
                mApi.deleteFileTransfer(transferId);
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }

    /**
     * Marks undelivered file transfers to indicate the specified file transfers have been
     * processed.
     * 
     * @param transferIds
     * @throws RcsServiceException
     */
    public void markUndeliveredFileTransfersAsProcessed(Set<String> transferIds)
            throws RcsServiceException {
        if (mApi != null) {
            try {
                mApi.markUndeliveredFileTransfersAsProcessed(new ArrayList<String>(transferIds));
            } catch (Exception e) {
                throw new RcsServiceException(e);
            }
        } else {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
    }
}
