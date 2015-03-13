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

import com.sonymobile.rcs.cpm.ms.sync.SyncMediatorBuilder;

public abstract class AbstractMediatorBuilder implements SyncMediatorBuilder {

    protected String mUrl;
    protected String mUsername;
    protected String mPassword;

    @Override
    public void setRemoteStoreUrl(String url) {
        mUrl = url;
    }

    @Override
    public void setUsername(String username) {
        mUsername = username;
    }

    @Override
    public void setPassword(String password) {
        mPassword = password;
    }

}
