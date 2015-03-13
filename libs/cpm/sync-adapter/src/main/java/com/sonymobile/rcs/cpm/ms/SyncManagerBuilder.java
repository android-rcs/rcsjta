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

package com.sonymobile.rcs.cpm.ms;

import com.sonymobile.rcs.cpm.ms.MessageStore;
import com.sonymobile.rcs.cpm.ms.impl.CommonMessageStoreImpl;
import com.sonymobile.rcs.cpm.ms.impl.sync.DefaultSyncMediator;
import com.sonymobile.rcs.cpm.ms.sync.SyncMediator;
import com.sonymobile.rcs.imap.DefaultImapService;
import com.sonymobile.rcs.imap.ImapService;
import com.sonymobile.rcs.imap.IoService;
import com.sonymobile.rcs.imap.SocketIoService;

public class SyncManagerBuilder {

    private String username;

    private String password;

    private String url;

    private String defaultFolder;

    public void setDefaultFolder(String defaultFolder) {
        this.defaultFolder = defaultFolder;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public SyncMediator getSyncManager() {
        MessageStore localStore = null;// new LocalStore(new SyncDatabaseDelegate());

        IoService io = new SocketIoService(url.toString());

        ImapService imap = new DefaultImapService(io);

        imap.setAuthenticationDetails(username, password, null, null, false);

        MessageStore remoteStore = new CommonMessageStoreImpl(defaultFolder, imap);

        SyncMediator syncManager = new DefaultSyncMediator(localStore, remoteStore);

        return syncManager;
    }

}
