/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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
 ******************************************************************************/

package com.gsma.rcs.ri.utils;

import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.extension.MultimediaSession;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing;
import com.gsma.services.rcs.sharing.image.ImageSharing;
import com.gsma.services.rcs.sharing.video.VideoSharing;
import com.gsma.services.rcs.upload.FileUpload;

/**
 * @author LEMORDANT Philippe
 */
public class RcsSessionUtil {

    /**
     * Checks if file transfer session can be aborted
     * 
     * @param fileTransfer file transfer session
     * @return True if file transfer session can be aborted
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    static public boolean isAllowedToAbortFileTransferSession(FileTransfer fileTransfer)
            throws RcsPersistentStorageException, RcsGenericException {
        switch (fileTransfer.getState()) {
            case STARTED:
            case QUEUED:
            case PAUSED:
            case INITIATING:
            case ACCEPTING:
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks if geoloc sharing session can be aborted
     * 
     * @param geolocSharing geoloc sharing session
     * @return True if geoloc sharing session can be aborted
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    static public boolean isAllowedToAbortGeolocSharingSession(GeolocSharing geolocSharing)
            throws RcsPersistentStorageException, RcsGenericException {
        switch (geolocSharing.getState()) {
            case STARTED:
            case INITIATING:
            case ACCEPTING:
            case RINGING:
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks if image sharing session can be aborted
     * 
     * @param imageSharing image sharing session
     * @return True if image sharing session can be aborted
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    static public boolean isAllowedToAbortImageSharingSession(ImageSharing imageSharing)
            throws RcsPersistentStorageException, RcsGenericException {
        switch (imageSharing.getState()) {
            case STARTED:
            case INITIATING:
            case ACCEPTING:
            case RINGING:
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks if video sharing session can be aborted
     * 
     * @param videoSharing video sharing session
     * @return True if image sharing session can be aborted
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    static public boolean isAllowedToAbortVideoSharingSession(VideoSharing videoSharing)
            throws RcsPersistentStorageException, RcsGenericException {
        switch (videoSharing.getState()) {
            case STARTED:
            case INITIATING:
            case ACCEPTING:
            case RINGING:
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks if multimedia session can be aborted
     * 
     * @param multimediaSession multimedia session
     * @return True if multimedia session can be aborted
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    static public boolean isAllowedToAbortMultimediaSession(MultimediaSession multimediaSession)
            throws RcsPersistentStorageException, RcsGenericException {
        switch (multimediaSession.getState()) {
            case STARTED:
            case INITIATING:
            case ACCEPTING:
            case RINGING:
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks if upload session can be aborted
     * 
     * @param session upload session
     * @return True if upload session can be aborted
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    static public boolean isAllowedToAbortFileUploadSession(FileUpload session)
            throws RcsPersistentStorageException, RcsGenericException {
        switch (session.getState()) {
            case STARTED:
            case INITIATING:
                return true;
            default:
                return false;
        }
    }

}
