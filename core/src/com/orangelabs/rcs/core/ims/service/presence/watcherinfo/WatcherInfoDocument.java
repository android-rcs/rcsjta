/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.orangelabs.rcs.core.ims.service.presence.watcherinfo;

import java.util.Vector;

/**
 * Watcher info document
 * 
 * @author jexa7410
 */
public class WatcherInfoDocument {
    private String resource;

    private String packageId;

    private Vector<Watcher> watcherList = new Vector<Watcher>();

    public WatcherInfoDocument(String resource, String packageId) {
        this.resource = resource;
        this.packageId = packageId;
    }

    public void addWatcher(Watcher watcher) {
        watcherList.addElement(watcher);
    }

    public Vector<Watcher> getWatcherList() {
        return watcherList;
    }

    public String getResource() {
        return resource;
    }

    public String getPackageId() {
        return packageId;
    }
}
