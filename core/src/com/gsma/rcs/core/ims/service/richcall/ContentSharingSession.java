/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.richcall;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsServiceSession;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

/**
 * Content sharing session
 * 
 * @author jexa7410
 */
public abstract class ContentSharingSession extends ImsServiceSession {
    /**
     * Content to be shared
     */
    private MmContent mContent;

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param content Content to be shared
     * @param contact Remote contactId
     * @param rcsSettings
     * @param timestamp Local timestamp for the session
     * @param contactManager
     */
    public ContentSharingSession(ImsService parent, MmContent content, ContactId contact,
            RcsSettings rcsSettings, long timestamp, ContactsManager contactManager) {
        super(parent, contact, PhoneUtils.formatContactIdToUri(contact), rcsSettings, timestamp,
                contactManager);

        mContent = content;
    }

    /**
     * Returns the content
     * 
     * @return Content
     */
    public MmContent getContent() {
        return mContent;
    }

    /**
     * Set the content
     * 
     * @param content Content
     */
    public void setContent(MmContent content) {
        mContent = content;
    }

    /**
     * Returns the "file-selector" attribute
     * 
     * @return String
     */
    public String getFileSelectorAttribute() {
        return "name:\"" + mContent.getName() + "\"" + " type:" + mContent.getEncoding() + " size:"
                + mContent.getSize();
    }

    /**
     * Returns the "file-location" attribute
     * 
     * @return Uri
     */
    public Uri getFileLocationAttribute() {
        Uri file = mContent.getUri();
        if ((file != null) && file.getScheme().startsWith("http")) {
            return file;
        } else {
            return null;
        }
    }

    /**
     * Returns the "file-transfer-id" attribute
     * 
     * @return String
     */
    public String getFileTransferId() {
        return "CSh" + IdGenerator.generateMessageID();
    }

    @Override
    public void receiveBye(SipRequest bye) {
        super.receiveBye(bye);

        // Request capabilities to the remote
        getImsService().getImsModule().getCapabilityService()
                .requestContactCapabilities(getRemoteContact());
    }

    @Override
    public void receiveCancel(SipRequest cancel) {
        super.receiveCancel(cancel);

        // Request capabilities to the remote
        getImsService().getImsModule().getCapabilityService()
                .requestContactCapabilities(getRemoteContact());
    }
}
