/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.sonymobile.rcs.cpm.ms.impl;

import com.sonymobile.rcs.cpm.ms.CpmMessageStore;
import com.sonymobile.rcs.cpm.ms.CpmMessageStoreException;
import com.sonymobile.rcs.cpm.ms.RcsUserStorage;
import com.sonymobile.rcs.cpm.ms.RootFolder;
import com.sonymobile.rcs.imap.ImapException;
import com.sonymobile.rcs.imap.ImapService;

import java.io.IOException;

public class CommonMessageStoreImpl extends AbstractMessageStore implements CpmMessageStore {

    private ImapService mImapService;

    private boolean mStartTlsEnabled;

    private int mConnectionEstablished = 0;

    private final RcsUserStorage mRcsUserStorage;

    private final RootFolder mDefaultFolder;

    public CommonMessageStoreImpl(String defaultRootPath, ImapService imapService) {
        this.mImapService = imapService;
        mDefaultFolder = new RootFolderImpl(defaultRootPath, this);
        mRcsUserStorage = new RcsUserStorageImpl(this);
    }

    @Override
    public void open() throws CpmMessageStoreException {
        try {
            mImapService.init();
            // imapService.login();

            // TODO expose STARTTLS
            if (mStartTlsEnabled) {
                mImapService.startTLS();
            }

            fireConnectionEstablished();
            // imapService.getCapabilities();
            mConnectionEstablished++;
        } catch (Exception e) {
            error(e);
            throw new CpmMessageStoreException("Cannot connect to remote server", e);
        }
        // try {
        // imapService.login();
        // } catch (Exception e) {
        // error(e);
        // throw new CPMMessageStoreException( "Cannot login to remote server" , e);
        // }
    }

    @Override
    public boolean isConnected() {
        return mImapService.isAvailable();
    }

    @Override
    public RootFolder getDefaultFolder() {
        return mDefaultFolder;
    }

    @Override
    public RcsUserStorage getRcsUserStorage() {
        return mRcsUserStorage;
    }

    public ImapService getImapService() {
        return mImapService;
    }

    @Override
    public void close() throws IOException {
        try {
            mImapService.logout();
            fireDisconnected();
        } catch (ImapException e) {
            e.printStackTrace();
        } finally {
            mImapService.close();
        }
    }

    private void fireConnectionEstablished() {

    }

    private void fireDisconnected() {

    }

    private void error(Exception e) {

    }

    @Override
    public void applyChanges() throws CpmMessageStoreException {
        // TODO Auto-generated method stub

    }

    // @Override
    // public void expunge() throws CPMMessageStoreException {
    // try {
    // imapService.expunge();
    // } catch (Exception e) {
    // error(e);
    // throw new CPMMessageStoreException(e);
    // }
    // }

}
