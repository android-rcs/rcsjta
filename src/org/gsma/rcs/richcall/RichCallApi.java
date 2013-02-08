/*
 * Copyright 2013, France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.rcs.richcall;

import android.content.Context;
import org.gsma.rcs.ClientApiException;
import org.gsma.rcs.media.IMediaPlayer;


/**
 * Class RichCallApi.
 */
public class RichCallApi extends org.gsma.rcs.ClientApi {

    /**
     * @param ctx Application context
     */
    public RichCallApi(Context ctx) {
        super ((Context) null);
    }

    public void connectApi() {

    }

    public void disconnectApi() {

    }

    /**
     * Returns the remote phone number.
     *
     * @return  The remote phone number.
     */
    public String getRemotePhoneNumber() throws ClientApiException {
        return (java.lang.String) null;
    }

    /**
     *
     * @param contact
     * @param player
     * @return The i video sharing session.
     */
    public IVideoSharingSession initiateLiveVideoSharing(String contact, IMediaPlayer player) throws ClientApiException {
        return (IVideoSharingSession) null;
    }

    /**
     *
     * @param contact
     * @param file
     * @param player
     * @return  The i video sharing session.
     */
    public IVideoSharingSession initiateVideoSharing(String contact, String file, IMediaPlayer player) throws ClientApiException {
        return (IVideoSharingSession) null;
    }

    /**
     * Returns the video sharing session.
     *
     * @param id The session id
     * @return  The video sharing session.
     */
    public IVideoSharingSession getVideoSharingSession(String id) throws ClientApiException {
        return (IVideoSharingSession) null;
    }

    /**
     *
     * @param contact
     * @param file
     * @return  The i image sharing session.
     */
    public IImageSharingSession initiateImageSharing(String contact, String file) throws ClientApiException {
        return (IImageSharingSession) null;
    }

    /**
     * Returns the image sharing session.
     *
     * @param id The session id
     * @return  The image sharing session.
     */
    public IImageSharingSession getImageSharingSession(String id) throws ClientApiException {
        return (IImageSharingSession) null;
    }

    /**
     * Sets the multi party call.
     *
     * @param arg1 The multi party call.
     */
    public void setMultiPartyCall(boolean state) throws ClientApiException {

    }

    /**
     * Sets the call hold.
     *
     * @param arg1 The call hold.
     */
    public void setCallHold(boolean state) throws ClientApiException {

    }

} // end RichCallApi
