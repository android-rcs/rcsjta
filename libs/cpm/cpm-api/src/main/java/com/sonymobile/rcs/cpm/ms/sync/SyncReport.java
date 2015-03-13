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

import java.io.Serializable;

public interface SyncReport extends Serializable {

    public String getStrategyName();

    public String getMessage();

    public long getStartedTime();

    public long getStoppedTime();

    public boolean isSuccess();

    public boolean isStopped();

    public boolean isCancelled();

    public boolean isPaused();

    public int getDeletedRemote();

    public int getDeletedLocal();

    public int getUpdatedRemote();

    public int getUpdatedLocal();

    public int getAddedRemote();

    public int getAddedLocal();

    public int getProgress();

    public int getProgressMax();

    public Exception getException();

}
