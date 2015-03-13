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

package com.sonymobile.rcs.cpm.ms.impl.sync;

import com.sonymobile.rcs.cpm.ms.CpmMessageStoreException;
import com.sonymobile.rcs.cpm.ms.CpmObjectType;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class DefaultSynchronizationMediatorTest {

    CpmObjectType t = CpmObjectType.ChatMessage;
    BasicMessageStore localStore = new BasicMessageStore();
    BasicMessageStore remoteStore = new BasicMessageStore();
    DefaultSyncMediator mgr = new DefaultSyncMediator(localStore, remoteStore);

    @Before
    public void init() throws CpmMessageStoreException {
        localStore.clear();
        remoteStore.clear();
    }

    @Test
    public void testExecuteSimple() throws Exception {

        // ItemGroup group = remoteStore.createGroup(new GroupInfo("123", "",
        // Arrays.asList("a@test.com"), 0, null));
        // group.addItem(new CpmObjectMetadata("1", t));
        //
        // assertFalse(remoteStore.getGroupById("123").getItemById("1").isDeleted());
        //
        // // EXECUTE 1
        //
        // mgr.executeUnsafe();
        //
        // Collection<ItemGroup> lgroups = localStore.getGroups();
        //
        // assertEquals(1, lgroups.size());
    }

    @Ignore
    // FIXME!!
    @Test
    public void testPropagateToServer() throws Exception {
        // localStore.createGroup(new GroupInfo("123", "", Arrays.asList("a@test.com"), 0, null))
        // .addItem(new CpmObjectMetadata("1", CpmObjectType.ChatMessage));
        //
        // remoteStore.createGroup(new GroupInfo("123", "", Arrays.asList("a@test.com"), 0, null))
        // .addItem(new CpmObjectMetadata("1", CpmObjectType.ChatMessage));
        //
        // CpmObjectMetadata bi = localStore.getGroupById("123").getItemById("1");
        // bi.setDeleted(true);
        // bi.setRead(true);
        //
        // remoteStore.applyChanges();
        //
        // assertTrue(localStore.getGroupById("123").getItemById("1").isDeleted());
        // assertTrue(localStore.getGroupById("123").getItemById("1").isRead());
        //
        // assertFalse(remoteStore.getGroupById("123").getItemById("1").isDeleted());
        // assertFalse(remoteStore.getGroupById("123").getItemById("1").isRead());
        //
        // // EXECUTE 2
        // mgr.executeUnsafe(CpmObjectType.ChatMessage);
        //
        // assertTrue(remoteStore.getGroupById("123").getItemById("1").isDeleted());
        // assertTrue(remoteStore.getGroupById("123").getItemById("1").isRead());
    }

}
