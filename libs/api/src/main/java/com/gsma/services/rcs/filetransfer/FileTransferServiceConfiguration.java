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

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsUnsupportedOperationException;

import android.util.SparseArray;

/**
 * File transfer service configuration
 * 
 * @author LEMORDANT Philippe
 */
public class FileTransferServiceConfiguration {

    private IFileTransferServiceConfiguration mIFtServiceConfig;

    /**
     * Enumerated for the Image Resize Option
     */
    public enum ImageResizeOption {
        /**
         * Always resize
         */
        ALWAYS_RESIZE(0),
        /**
         * Always ask if resize or not
         */
        ALWAYS_ASK(1),
        /**
         * Never resize
         */
        NEVER_RESIZE(2);

        private int mValue;

        private static SparseArray<ImageResizeOption> mValueToEnum = new SparseArray<>();
        static {
            for (ImageResizeOption entry : ImageResizeOption.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        ImageResizeOption(int value) {
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
         * @param value the int value representing the ImageResizeOption
         * @return ImageResizeOption instance
         */
        public static ImageResizeOption valueOf(int value) {
            ImageResizeOption entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class "
                    + ImageResizeOption.class.getName() + "" + value);

        }

    }

    /**
     * Constructor
     * 
     * @param iFtServiceConfig the file transfer service configuration interface
     * @hide
     */
    /* package private */FileTransferServiceConfiguration(
            IFileTransferServiceConfiguration iFtServiceConfig) {
        mIFtServiceConfig = iFtServiceConfig;
    }

    /**
     * Returns the maximum audio message duration.
     *
     * @return long maximum audio message duration
     * @throws RcsGenericException
     */
    public long getMaxAudioMessageDuration() throws RcsGenericException {
        try {
            return mIFtServiceConfig.getMaxAudioMessageDuration();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the file transfer size threshold when the user should be warned about the potential
     * charges associated to the transfer of a large file. It returns 0 if there no need to warn.
     *
     * @return long Size in bytes
     * @throws RcsGenericException
     */
    public long getWarnSize() throws RcsGenericException {
        try {
            return mIFtServiceConfig.getWarnSize();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the file transfer size limit. It returns 0 if there is no limitation.
     * 
     * @return long Size in bytes
     * @throws RcsGenericException
     */
    public long getMaxSize() throws RcsGenericException {
        try {
            return mIFtServiceConfig.getMaxSize();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Is file transfer invitation automatically accepted
     * 
     * @return boolean Returns true if File Transfer is automatically accepted else returns false
     * @throws RcsGenericException
     */
    public boolean isAutoAcceptEnabled() throws RcsGenericException {
        try {
            return mIFtServiceConfig.isAutoAcceptEnabled();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Sets the Auto Accept Mode of a File Transfer configuration.<br>
     * The Auto Accept Mode can only be modified by client application if isAutoAcceptChangeable is
     * true.
     * 
     * @param enable True if file transfer is auto accepted
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public void setAutoAccept(boolean enable) throws RcsPersistentStorageException,
            RcsGenericException {
        try {
            mIFtServiceConfig.setAutoAccept(enable);
        } catch (Exception e) {
            RcsUnsupportedOperationException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Is file transfer invitation automatically accepted while in roaming.
     * <p>
     * This parameter is only applicable if auto accept is active for File Transfer in normal
     * conditions (see isAutoAcceptEnabled).
     * 
     * @return boolean Returns true if File Transfer is automatically accepted while in roaming else
     *         returns false
     * @throws RcsGenericException
     */
    public boolean isAutoAcceptInRoamingEnabled() throws RcsGenericException {
        try {
            return mIFtServiceConfig.isAutoAcceptInRoamingEnabled();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Sets the Auto Accept Mode of a File Transfer configuration while roaming.<br>
     * The AutoAcceptInRoaming can only be modified by client application if
     * isAutoAcceptModeChangeable is true and if the AutoAccept Mode in normal conditions is true.
     * 
     * @param enable True if file transfer is auto accepted in roaming
     * @throws RcsPermissionDeniedException
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public void setAutoAcceptInRoaming(boolean enable) throws RcsPermissionDeniedException,
            RcsPersistentStorageException, RcsGenericException {
        try {
            mIFtServiceConfig.setAutoAcceptInRoaming(enable);

        } catch (Exception e) {
            RcsUnsupportedOperationException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * is default Auto Accept mode (both in normal or roaming modes) changeable
     * 
     * @return boolean True if client is allowed to change the default Auto Accept mode (both in
     *         normal or roaming modes)
     * @throws RcsGenericException
     */
    public boolean isAutoAcceptModeChangeable() throws RcsGenericException {
        try {
            return mIFtServiceConfig.isAutoAcceptModeChangeable();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the max number of simultaneous file transfers
     * 
     * @return int the max number of simultaneous file transfers
     * @throws RcsGenericException
     */
    public int getMaxFileTransfers() throws RcsGenericException {
        try {
            return mIFtServiceConfig.getMaxFileTransfers();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the image resize option for file transfer in the range: ALWAYS_PERFORM,
     * ONLY_ABOVE_MAX_SIZE, ASK
     * 
     * @return ImageResizeOption instance
     * @throws RcsGenericException
     */
    public ImageResizeOption getImageResizeOption() throws RcsGenericException {
        try {
            return ImageResizeOption.valueOf(mIFtServiceConfig.getImageResizeOption());

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Sets the image resize option for file transfer.
     * 
     * @param option the image resize option for file transfer.
     * @throws RcsGenericException
     */
    public void setImageResizeOption(ImageResizeOption option) throws RcsGenericException {
        try {
            mIFtServiceConfig.setImageResizeOption(option.toInt());
        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns True if group file transfer is supported, else returns False.
     * 
     * @return boolean True if group file transfer is supported, else returns False.
     * @throws RcsGenericException
     */
    public boolean isGroupFileTransferSupported() throws RcsGenericException {
        try {
            return mIFtServiceConfig.isGroupFileTransferSupported();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }
}
