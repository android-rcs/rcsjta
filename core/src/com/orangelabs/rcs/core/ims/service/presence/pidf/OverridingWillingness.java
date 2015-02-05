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

package com.orangelabs.rcs.core.ims.service.presence.pidf;

import com.orangelabs.rcs.utils.DateUtils;

public class OverridingWillingness {

    private Basic basic = null;

    private long until = -1;

    public OverridingWillingness(Basic basic) {
        this.basic = basic;
    }

    public OverridingWillingness() {
    }

    public Basic getBasic() {
        return basic;
    }

    public void setBasic(Basic basic) {
        this.basic = basic;
    }

    public long getUntilTimestamp() {
        return until;
    }

    public void setUntilTimestamp(String ts) {
        this.until = DateUtils.decodeDate(ts);
    }
}
