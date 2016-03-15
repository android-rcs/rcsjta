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

package com.orangelabs.rcs.ri.messaging.adapter;

import com.gsma.services.rcs.history.HistoryLog;

import android.database.Cursor;
import android.view.View;

/**
 * A ViewHolder class keeps references to children views to avoid unnecessary calls to
 * findViewById() or getColumnIndex() on each row.
 */
public class RcsFileTransferOutViewHolder extends RcsFileTransferInViewHolder {
    private final int mColumnExpiredDeliveryIdx;

    /**
     * Constructor
     *
     * @param base view
     * @param cursor cursor
     */
    RcsFileTransferOutViewHolder(View base, Cursor cursor) {
        super(base, cursor);
        /* Save column indexes */
        mColumnExpiredDeliveryIdx = cursor.getColumnIndexOrThrow(HistoryLog.EXPIRED_DELIVERY);
    }

    public int getColumnExpiredDeliveryIdx() {
        return mColumnExpiredDeliveryIdx;
    }

}
