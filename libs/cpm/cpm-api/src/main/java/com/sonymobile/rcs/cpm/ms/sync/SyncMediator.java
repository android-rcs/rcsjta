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

package com.sonymobile.rcs.cpm.ms.sync;

import com.sonymobile.rcs.cpm.ms.MessageStore;

/**
 * Based on CPM User’s preferences and service provider policies, the CPM Standalone Messages, CPM
 * File Transfer Histories, CPM Session Histories and/or standalone Media Objects within the Message
 * Storage Client’s local storage SHALL be synchronized with the stored objects in the CPM Message
 * Storage Server. This means that any changes to the Message Storage Server (e.g., arrival of new
 * or deletion of old CPM Standalone Messages, CPM File Transfer Histories, CPM Session Histories
 * and/or standalone Media objects) will be reflected in the Message Storage Client’s local storage
 * and, similarly, any changes to the Message Storage Client’s local storage will be reflected in
 * the Message Storage Server, in accordance with user preferences and service provider policies.
 * Synchronization SHALL always be initiated by the Message Storage Client, e.g. upon the CPM User’s
 * request, a client setting to synchronize periodically, or upon detection of changes to the
 * Message Storage Client’s local storage. The Message Storage Server MAY trigger the Message
 * Storage Client to initiate a synchronization by sending notifications of changes done to the CPM
 * User’s stored resources in the Message Storage Server.
 */
public interface SyncMediator {

    public SyncReport execute();

    public SyncReport getCurrentReport();

    public void setStrategy(SyncStrategy strategy);

    public SyncStrategy getStrategy();

    public MessageStore getRemoteStore();

    public MessageStore getLocalStore();

    public void addSynchronizationListener(SynchronizationListener listener);

    public void removeSynchronizationListener(SynchronizationListener listener);

}
