/*
 * Copyright 2013, France Telecom
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *    http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.rcs.richcall;

/**
 * Class RichCallApi.
 */
public class RichCallApi extends org.gsma.rcs.ClientApi {
    /**
     * Creates a new instance of RichCallApi.
     *
     * @param arg1 The arg1.
     */
    public RichCallApi(android.content.Context arg1) {
        super((android.content.Context) null);
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
    public String getRemotePhoneNumber() throws org.gsma.rcs.ClientApiException {
        return (java.lang.String) null;
    }

    /**
     *
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @return  The i video sharing session.
     */
    public IVideoSharingSession initiateLiveVideoSharing(String arg1, org.gsma.rcs.media.IMediaPlayer arg2) throws org.gsma.rcs.ClientApiException {
        return (IVideoSharingSession) null;
    }

    /**
     *
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @param arg3 The arg3.
     * @return  The i video sharing session.
     */
    public IVideoSharingSession initiateVideoSharing(String arg1, String arg2, org.gsma.rcs.media.IMediaPlayer arg3) throws org.gsma.rcs.ClientApiException {
        return (IVideoSharingSession) null;
    }

    /**
     * Returns the video sharing session.
     *
     * @param arg1 The arg1.
     * @return  The video sharing session.
     */
    public IVideoSharingSession getVideoSharingSession(String arg1) throws org.gsma.rcs.ClientApiException {
        return (IVideoSharingSession) null;
    }

    /**
     *
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     * @return  The i image sharing session.
     */
    public IImageSharingSession initiateImageSharing(String arg1, String arg2) throws org.gsma.rcs.ClientApiException {
        return (IImageSharingSession) null;
    }

    /**
     * Returns the image sharing session.
     *
     * @param arg1 The arg1.
     * @return  The image sharing session.
     */
    public IImageSharingSession getImageSharingSession(String arg1) throws org.gsma.rcs.ClientApiException {
        return (IImageSharingSession) null;
    }

    /**
     * Sets the multi party call.
     *
     * @param arg1 The multi party call.
     */
    public void setMultiPartyCall(boolean arg1) throws org.gsma.rcs.ClientApiException {

    }

    /**
     * Sets the call hold.
     *
     * @param arg1 The call hold.
     */
    public void setCallHold(boolean arg1) throws org.gsma.rcs.ClientApiException {

    }

} // end RichCallApi
