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

public class CpmObjectMetadata {

    private final String id;

    private final CpmObjectType type;

    // unresolved if null
    private String groupId;

    private boolean read;

    private boolean deleted;

    private boolean persisted;

    public CpmObjectMetadata(String id, CpmObjectType type) {
        super();
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupId() {
        return groupId;
    }

    public CpmObjectType getType() {
        return type;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public CpmObjectMetadata copy() {
        CpmObjectMetadata hi = new CpmObjectMetadata(id, type);
        hi.deleted = deleted;
        hi.read = read;
        hi.groupId = groupId;
        hi.persisted = persisted;
        return hi;
    }

    public void reset() {
        deleted = read = false;
        groupId = null;
    }

    public void update(CpmObjectMetadata aState) {
        groupId = aState.groupId;
        read = aState.read;
        deleted = aState.deleted;
    }

    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    public boolean isPersisted() {
        return persisted;
    }

}
