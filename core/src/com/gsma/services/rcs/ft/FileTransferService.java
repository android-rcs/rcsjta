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
package com.gsma.services.rcs.ft;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.IBinder;
import android.os.IInterface;

import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * This class offers the main entry point to transfer files and to
 * receive files. Several applications may connect/disconnect to the API.
 * 
 * The parameter contact in the API supports the following formats:
 * MSISDN in national or international format, SIP address, SIP-URI
 * or Tel-URI.
 * 
 * @author Jean-Marc AUFFRET 
 */
public class FileTransferService extends RcsService {

	private static final int KITKAT_VERSION_CODE = 19;

	private static final String TAKE_PERSISTABLE_URI_PERMISSION_METHOD_NAME = "takePersistableUriPermission";

	private static final Class<?>[] TAKE_PERSISTABLE_URI_PERMISSION_PARAM_TYPES = new Class[] {
			Uri.class, int.class
	};

	/**
	 * API
	 */
	private IFileTransferService mApi;
	
	private static final String ERROR_CNX = "FileTransfer service not connected";
	
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
     */
    public void connect() {
    	mCtx.bindService(new Intent(IFileTransferService.class.getName()), apiConnection, 0);
    }
    
    /**
     * Disconnects from the API
     */
    public void disconnect() {
    	try {
    		mCtx.unbindService(apiConnection);
        } catch(IllegalArgumentException e) {
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
        mApi = (IFileTransferService)api;
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
        	if (mListener != null) {
        		mListener.onServiceDisconnected(RcsService.Error.CONNECTION_LOST);
        	}
        }
    };

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
			} catch(Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
	}

	private void grantUriPermissionToStackServices(Uri file) {
		Intent fileTransferServiceIntent = new Intent(IFileTransferService.class.getName());
		List<ResolveInfo> stackServices = mCtx.getPackageManager().queryIntentServices(
				fileTransferServiceIntent, 0);
		for (ResolveInfo stackService : stackServices) {
			mCtx.grantUriPermission(stackService.serviceInfo.packageName, file,
					Intent.FLAG_GRANT_READ_URI_PERMISSION);
		}
	}

	/**
	 * Using reflection to persist Uri permission in order to support backward
	 * compatibility since this API is available only from Kitkat onwards.
	 *
	 * @param file Uri of file to transfer
	 * @throws RcsServiceException
	 */
	private void takePersistableUriPermission(Uri file) throws RcsServiceException {
		try {
			ContentResolver contentResolver = mCtx.getContentResolver();
			Method takePersistableUriPermissionMethod = contentResolver.getClass().getMethod(
					TAKE_PERSISTABLE_URI_PERMISSION_METHOD_NAME,
					TAKE_PERSISTABLE_URI_PERMISSION_PARAM_TYPES);
			Object[] methodArgs = new Object[] {
					file,
					Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
			};
			takePersistableUriPermissionMethod.invoke(contentResolver, methodArgs);
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Grant permission to the stack and persist access permission
	 * @param file the file URI
	 * @throws RcsServiceException
	 */
	private void tryToGrantAndPersistUriPermission(Uri file) throws RcsServiceException {
		if (ContentResolver.SCHEME_CONTENT.equals(file.getScheme())) {
			// Granting temporary read Uri permission from client to
			// stack service if it is a content URI
			grantUriPermissionToStackServices(file);
			// Try to persist Uri access permission for the client
			// to be able to read the contents from this Uri even
			// after the client is restarted after device reboot.
			if (android.os.Build.VERSION.SDK_INT >= KITKAT_VERSION_CODE) {
				takePersistableUriPermission(file);
			}
		}
	}
    
	/**
     * Transfers a file to a contact. The parameter file contains the URI of the
     * file to be transferred (for a local or a remote file). The parameter
     * contact supports the following formats: MSISDN in national or
     * international format, SIP address, SIP-URI or Tel-URI. If the format of
     * the contact is not supported an exception is thrown.
	 * 
	 * @param contact the remote contact Identifier
	 * @param file
	 *            Uri of file to transfer
	 * @param attachFileIcon
	 *            File icon option. If true, the stack tries to attach fileicon. Fileicon may not be attached if file is not an
	 *            image or if local or remote contact does not support fileicon.
	 * @return File transfer
	 * @throws RcsServiceException
	 */
	public FileTransfer transferFile(ContactId contact, Uri file, boolean attachFileIcon) throws RcsServiceException {
    	if (mApi != null) {
			try {
				tryToGrantAndPersistUriPermission(file);

				IFileTransfer ftIntf = mApi.transferFile(contact, file, attachFileIcon);
				if (ftIntf != null) {
					return new FileTransfer(ftIntf);
				} else {
					return null;
				}
			} catch(Exception e) {
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
	 * @param attachFileIcon Attach file icon option. If true, the stack tries
	 *            to attach fileIcon. FileIcon may not be attached if file is
	 *            not an image or if local or remote contact does not support
	 *            fileIcon.
	 * @return FileTransfer
	 * @throws RcsServiceException
	 */
	public FileTransfer transferFileToGroupChat(String chatId, Uri file, boolean attachFileIcon)
			throws RcsServiceException {
		if (mApi != null) {
			try {
				tryToGrantAndPersistUriPermission(file);
				
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
     * Mark a received file transfer as read (i.e. the invitation or the file has been displayed in the UI).
     *
     * @param transferId
     * @throws RcsServiceException
     */
    public void markFileTransferAsRead(String transferId) throws RcsServiceException {
        if (mApi != null) {
            try {
                mApi.markFileTransferAsRead(transferId);
            } catch(Exception e) {
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
			} catch(Exception e) {
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
	 * 
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
			} catch(Exception e) {
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
				mApi.addEventListener2(listener);
			} catch(Exception e) {
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
	public void removeEventListener(OneToOneFileTransferListener listener) throws RcsServiceException {
		if (mApi != null) {
			try {
				mApi.removeEventListener2(listener);
			} catch(Exception e) {
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
				mApi.addEventListener3(listener);
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
				mApi.removeEventListener3(listener);
			} catch (Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
	}

	/**
	 * set the Auto Accept Mode of a File Transfer configuration.
	 * <p>
	 * The Auto Accept Mode can only be modified by client application if isAutoAcceptModeChangeable (see
	 * FileTransferServiceConfiguration class) is true
	 * 
	 * @param enable
	 *            true to enable else false
	 * @throws RcsServiceException
	 */
    public void setAutoAccept(boolean enable) throws RcsServiceException {
		if (mApi != null) {
			try {
				mApi.setAutoAccept(enable);
			} catch(Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
	}
	
	/**
	 * set the Auto Accept Mode of a File Transfer configuration while roaming.
	 * <p>
	 * The AutoAcceptModeInRoaming can only be modified by client application if isAutoAcceptModeChangeable (@see
	 * FileTransferServiceConfiguration class) is true and if the Auto Accept Mode in normal conditions is true
	 * 
	 * @param enable
	 *            true to enable else false
	 * @throws RcsServiceException
	 */
	public void setAutoAcceptInRoaming(boolean enable) throws RcsServiceException {
		if (mApi != null) {
			try {
				mApi.setAutoAcceptInRoaming(enable);
			} catch (Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
	}
    
	/**
	 * set the image resize option for file transfer.
	 * 
	 * @param option
	 *            the image resize option (0: ALWAYS_PERFORM, 1: ONLY_ABOVE_MAX_SIZE, 2: ASK)
	 * @throws RcsServiceException
	 */
    public void setImageResizeOption(int option) throws RcsServiceException {
		if (mApi != null) {
			try {
				mApi.setImageResizeOption(option);
			} catch(Exception e) {
				throw new RcsServiceException(e);
			}
		} else {
			throw new RcsServiceNotAvailableException(ERROR_CNX);
		}
	}
}
