/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

package com.gsma.rcs.core.ims.protocol.msrp;

import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession.TypeMsrpChunk;
import com.gsma.rcs.provider.contact.ContactManagerException;

/**
 * MSRP event listener
 * 
 * @author jexa7410
 * @author Deutsche Telekom AG
 */
public interface MsrpEventListener {
    /**
     * Data has been transferred
     * 
     * @param msgId Message ID
     */
    void msrpDataTransferred(String msgId);

    /**
     * Data has been received
     * 
     * @param msgId Message ID
     * @param data Received data
     * @param mimeType Data mime-type
     * @throws PayloadException
     * @throws NetworkException
     * @throws ContactManagerException
     */
    void receiveMsrpData(String msgId, byte[] data, String mimeType) throws PayloadException,
            NetworkException, ContactManagerException;

    /**
     * Data transfer in progress
     * 
     * @param currentSize Current transfered size in bytes
     * @param totalSize Total size in bytes
     */
    void msrpTransferProgress(long currentSize, long totalSize);

    /**
     * Data transfer in progress
     * 
     * @param currentSize Current transfered size in bytes
     * @param totalSize Total size in bytes
     * @param data received data chunk
     * @return true if data are processed and can be delete in cache. If false, so data were stored
     *         in MsrpSession cache until msrpDataReceived is called.
     */
    boolean msrpTransferProgress(long currentSize, long totalSize, byte[] data);

    /**
     * Data transfer has been aborted
     */
    void msrpTransferAborted();

    // Changed by Deutsche Telekom
    /**
     * Data transfer error
     * 
     * @param msgId Message ID
     * @param error Error code
     * @param typeMsrpChunk Type of MSRP chunk
     */
    void msrpTransferError(String msgId, String error, TypeMsrpChunk typeMsrpChunk);
}
