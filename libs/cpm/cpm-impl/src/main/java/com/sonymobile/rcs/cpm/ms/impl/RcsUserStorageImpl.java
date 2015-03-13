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

import com.sonymobile.rcs.cpm.ms.CpmMessageStoreException;
import com.sonymobile.rcs.cpm.ms.CpmObjectFolder;
import com.sonymobile.rcs.cpm.ms.RcsUserStorage;
import com.sonymobile.rcs.cpm.ms.UserFolder;
import com.sonymobile.rcs.imap.ImapFolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RcsUserStorageImpl extends RootFolderImpl implements RcsUserStorage {

    public static final String PERMANENT_FOLDERNAME = "RCSMessageStore";

    public RcsUserStorageImpl(CommonMessageStoreImpl store) {
        super(PERMANENT_FOLDERNAME, store);
    }

    @Override
    public void remove(CpmObjectFolder folder) throws CpmMessageStoreException {
        try {
            getImapService().unselect();
            getImapService().delete(folder.getName());
        } catch (Exception e) {
            throw new CpmMessageStoreException("Cannot remove folder " + folder, e);
        }

    }

    @Override
    public Set<UserFolder> getUserFolders() throws CpmMessageStoreException {
        // TODO How to identify the user folders ?
        try {
            List<ImapFolder> li = getImapService().getFolders(getPath(), false);
            List<UserFolder> r = new ArrayList<UserFolder>();
            for (ImapFolder f : li) {
                String n = f.getName();
                if (n.length() < 15) {
                    r.add(getUserFolder(n));
                }
            }

        } catch (Exception e) {
            throw new CpmMessageStoreException("", e);
        }
        return null;
    }

    @Override
    public UserFolder addUserFolder(String folderName) throws CpmMessageStoreException {
        try {
            UserFolderImpl uf = new UserFolderImpl(this, null);
            uf.create();
            return uf;
        } catch (Exception e) {
            throw new CpmMessageStoreException("", e);
        }
    }

    @Override
    public UserFolder getUserFolder(String folderName) throws CpmMessageStoreException {
        return new UserFolderImpl(this, null);
    }

}
