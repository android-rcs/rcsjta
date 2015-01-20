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

import android.util.SparseArray;

import com.gsma.services.rcs.RcsServiceException;

/**
 * File transfer service configuration
 * 
 * @author LEMORDANT Philippe
 *
 */
public class FileTransferServiceConfiguration {
	
	private IFileTransferServiceConfiguration mIFtServiceConfig;

	/**
	 * Enumerated for the Image Resize Option
	 */
	public enum ImageResizeOption {
		/**
		 * Always ask to resize or not
		 */
		ALWAYS_PERFORM(0),
		/**
		 * Only ask if above maximum size
		 */
		ONLY_ABOVE_MAX_SIZE(1),
		/**
		 * Ask
		 */
		ASK(2);

		private int mValue;

		private static SparseArray<ImageResizeOption> mValueToEnum = new SparseArray<ImageResizeOption>();
		static {
			for (ImageResizeOption entry : ImageResizeOption.values()) {
				mValueToEnum.put(entry.toInt(), entry);
			}
		}

		private ImageResizeOption(int value) {
			mValue = value;
		}

		/**
		 * Returns the value of this ImageResizeOption as an int.
		 * 
		 * @return int value
		 */
		public final int toInt() {
			return mValue;
			/**
			 * @param value
			 * @return
			 */
		}

		/**
		 * Returns a ImageResizeOption instance representing the specified int value.
		 * 
		 * @param value
		 * @return ImageResizeOption instance
		 */
		public static ImageResizeOption valueOf(int value) {
			ImageResizeOption entry = mValueToEnum.get(value);
			if (entry != null) {
				return entry;
			}
			throw new IllegalArgumentException("No enum const class " + ImageResizeOption.class.getName() + "." + value);

		}

	};

	/**
	 * Constructor
	 * 
	 * @param iFtServiceConfig
	 * @hide
	 */
	/* package private */ FileTransferServiceConfiguration(IFileTransferServiceConfiguration iFtServiceConfig) {
		mIFtServiceConfig = iFtServiceConfig;
	}

	/**
	 * Returns the file transfer size threshold when the user should be warned about the potential charges associated to the
	 * transfer of a large file. It returns 0 if there no need to warn.
	 * 
	 * @return Size in kilobytes
	 * @throws RcsServiceException 
	 */
	public long getWarnSize() throws RcsServiceException {
		try {
			return mIFtServiceConfig.getWarnSize();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Returns the file transfer size limit. It returns 0 if there is no limitation.
	 * 
	 * @return Size in kilobytes
	 * @throws RcsServiceException 
	 */
	public long getMaxSize() throws RcsServiceException {
		try {
			return mIFtServiceConfig.getMaxSize();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Is file transfer invitation automatically accepted
	 * 
	 * @return Returns true if File Transfer is automatically accepted else returns false
	 * @throws RcsServiceException 
	 */
	public boolean isAutoAcceptEnabled() throws RcsServiceException {
		try {
			return mIFtServiceConfig.isAutoAcceptEnabled();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Sets the Auto Accept Mode of a File Transfer configuration.<br>
	 * The Auto Accept Mode can only be modified by client application if isAutoAcceptChangeable is true.
	 * 
	 * @param enable
	 * @throws RcsServiceException 
	 */
	public void setAutoAccept(boolean enable) throws RcsServiceException {
		try {
			mIFtServiceConfig.setAutoAccept(enable) ;
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Is file transfer invitation automatically accepted while in roaming.
	 * <p>
	 * This parameter is only applicable if auto accept is active for File Transfer in normal conditions (see isAutoAcceptEnabled).
	 * 
	 * @return Returns true if File Transfer is automatically accepted while in roaming else returns false
	 * @throws RcsServiceException 
	 */
	public boolean isAutoAcceptInRoamingEnabled() throws RcsServiceException {
		try {
			return mIFtServiceConfig.isAutoAcceptInRoamingEnabled();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Sets the Auto Accept Mode of a File Transfer configuration while roaming.<br>
	 * The AutoAcceptInRoaming can only be modified by client application if isAutoAcceptModeChangeable is true and if the
	 * AutoAccept Mode in normal conditions is true.
	 * 
	 * @param enable
	 * @throws RcsServiceException 
	 */
	public void setAutoAcceptInRoaming(boolean enable) throws RcsServiceException {
		try {
			mIFtServiceConfig.setAutoAcceptInRoaming(enable) ;
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * is default Auto Accept mode (both in normal or roaming modes) changeable
	 * 
	 * @return True if client is allowed to change the default Auto Accept mode (both in normal or roaming modes)
	 * @throws RcsServiceException 
	 */
	public boolean isAutoAcceptModeChangeable() throws RcsServiceException {
		try {
			return mIFtServiceConfig.isAutoAcceptModeChangeable();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Returns the max number of simultaneous file transfers
	 * 
	 * @return the max number of simultaneous file transfers
	 * @throws RcsServiceException 
	 */
	public int getMaxFileTransfers() throws RcsServiceException {
		try {
			return mIFtServiceConfig.getMaxFileTransfers();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Returns the image resize option for file transfer in the range: ALWAYS_PERFORM, ONLY_ABOVE_MAX_SIZE, ASK
	 * 
	 * @return ImageResizeOption instance
	 * @throws RcsServiceException 
	 */
	public ImageResizeOption getImageResizeOption() throws RcsServiceException {
		try {
			int option = mIFtServiceConfig.getImageResizeOption();
			return ImageResizeOption.valueOf(option);
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Sets the image resize option for file transfer.
	 * 
	 * @param option
	 * @throws RcsServiceException 
	 */
	public void setImageResizeOption(ImageResizeOption option) throws RcsServiceException {
		try {
			mIFtServiceConfig.setImageResizeOption(option.toInt()) ;
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Returns True if group file transfer is supported, else returns False.
	 * 
	 * @return True if group file transfer is supported, else returns False.
	 * @throws RcsServiceException 
	 */
	public boolean isGroupFileTransferSupported() throws RcsServiceException {
		try {
			return mIFtServiceConfig.isGroupFileTransferSupported();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}
}
