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

import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.capability.CapabilityService;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.contact.ContactService;
import com.gsma.services.rcs.extension.MultimediaSessionService;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingService;
import com.gsma.services.rcs.sharing.image.ImageSharingService;
import com.gsma.services.rcs.sharing.video.VideoSharingService;
import com.gsma.services.rcs.upload.FileUploadService;
import com.orangelabs.rcs.api.connection.ConnectionManager.RcsServiceName;

/**
 * Interface to access the Connection manager
 *
 * @author LEMORDANT Philippe
 */
public interface IConnectionManager {

    /**
     * Start monitoring the API connection
     */
    void startMonitorApiCnx(RcsServiceListener listener, RcsServiceName... services);

    /**
     * Start monitoring the services
     */
    void startMonitorServices(RcsServiceName... services);

    /**
     * Stop monitoring the API connection
     */
    void stopMonitorApiCnx();

    /**
     * Check if services are connected
     *
     * @param services list of services
     * @return true if all services of the list are connected
     */
    boolean isServiceConnected(RcsServiceName... services);

    /**
     * Get the instance of CapabilityService
     *
     * @return the instance
     */
    CapabilityService getCapabilityApi();

    /**
     * Get the instance of ChatService
     *
     * @return the instance
     */
    ChatService getChatApi();

    /**
     * Get the instance of ContactService
     *
     * @return the instance
     */
    ContactService getContactApi();

    /**
     * Get the instance of FileTransferService
     *
     * @return the instance
     */
    FileTransferService getFileTransferApi();

    /**
     * Get the instance of VideoSharingService
     *
     * @return the instance
     */
    VideoSharingService getVideoSharingApi();

    /**
     * Get the instance of ImageSharingService
     *
     * @return the instance
     */
    ImageSharingService getImageSharingApi();

    /**
     * Get the instance of GeolocSharingService
     *
     * @return the instance
     */
    GeolocSharingService getGeolocSharingApi();

    /**
     * Get the instance of FileUploadService
     *
     * @return the instance
     */
    FileUploadService getFileUploadApi();

    /**
     * Get the instance of MultimediaSessionService
     *
     * @return the instance
     */
    MultimediaSessionService getMultimediaSessionApi();
}
