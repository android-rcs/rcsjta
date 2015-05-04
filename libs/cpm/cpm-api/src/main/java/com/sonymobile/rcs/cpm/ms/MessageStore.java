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

import java.io.Closeable;

/**
 * Message Store representation with common concepts between remote and local.
 */
public interface MessageStore extends Closeable {

    /**
     * Connects to the remote server.
     * 
     * @throws CpmMessageStoreException
     */
    public void open() throws CpmMessageStoreException;

    public boolean isConnected();

    public void addMessageStoreListener(MessageStoreListener listener);

    public void removeMessageStoreListener(MessageStoreListener listener);

    public void applyChanges() throws CpmMessageStoreException;

    public RootFolder getDefaultFolder();

}
