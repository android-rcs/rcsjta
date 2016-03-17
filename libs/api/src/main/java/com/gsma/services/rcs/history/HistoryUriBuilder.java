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

package com.gsma.services.rcs.history;

import android.net.Uri;

/**
 * Utility builder class to generate the uri used to query the History provider.
 */
public class HistoryUriBuilder {

    private final Uri.Builder mUriBuilder;

    public HistoryUriBuilder(Uri historyLogUri) {
        mUriBuilder = historyLogUri.buildUpon();
    }

    /**
     * Add the provider ids that will be part of the query.
     * 
     * @param providerId the provider ID
     * @return the builder
     */
    public HistoryUriBuilder appendProvider(int providerId) {
        mUriBuilder.appendQueryParameter(HistoryLog.PROVIDER_ID, String.valueOf(providerId));
        return this;
    }

    /**
     * Creates and returns the uri that contains the provider id parameters.
     * 
     * @return the generated uri
     */
    public Uri build() {
        return mUriBuilder.build();
    }
}
