/*
 * ******************************************************************************
 *  * Software Name : RCS IMS Stack
 *  *
 *  * Copyright (C) 2010 France Telecom S.A.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *****************************************************************************
 */

package com.orangelabs.rcs.ri.messaging.adapter;

import com.gsma.services.rcs.history.HistoryLog;

import com.orangelabs.rcs.ri.R;

import android.database.Cursor;
import android.view.View;
import android.widget.TextView;

/**
 * Created by yplo6403 on 01/12/2015.
 */
public class RcsChatInViewHolder extends BasicViewHolder {

    private final TextView mContentText;

    private final int mColumnContentIdx;

    public RcsChatInViewHolder(View view, Cursor cursor) {
        super(view, cursor);
        /* Save column indexes */
        mColumnContentIdx = cursor.getColumnIndexOrThrow(HistoryLog.CONTENT);
        /* Save children views */
        mContentText = (TextView) view.findViewById(R.id.content_text);
    }

    public TextView getContentText() {
        return mContentText;
    }

    public int getColumnContentIdx() {
        return mColumnContentIdx;
    }

}
