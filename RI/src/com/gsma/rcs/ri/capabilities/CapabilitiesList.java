/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.ri.capabilities;

import com.gsma.rcs.api.connection.utils.RcsActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.utils.RcsContactUtil;
import com.gsma.services.rcs.capability.CapabilitiesLog;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

/**
 * List capabilities from the content provider
 * 
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class CapabilitiesList extends RcsActivity {

    // @formatter:off
    private static final String[] PROJECTION = new String[] {
        CapabilitiesLog.BASECOLUMN_ID,
        CapabilitiesLog.CONTACT,
        CapabilitiesLog.CAPABILITY_IM_SESSION,
        CapabilitiesLog.CAPABILITY_FILE_TRANSFER,
        CapabilitiesLog.CAPABILITY_IMAGE_SHARE,
        CapabilitiesLog.CAPABILITY_VIDEO_SHARE,
        CapabilitiesLog.CAPABILITY_GEOLOC_PUSH,
        CapabilitiesLog.CAPABILITY_EXTENSIONS,
        CapabilitiesLog.AUTOMATA,
        CapabilitiesLog.TIMESTAMP
    };
    // @formatter:on

    private static final String SORT_ORDER = CapabilitiesLog.CONTACT + " DESC";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.capabilities_list);

        // Set list adapter
        ListView view = (ListView) findViewById(android.R.id.list);
        TextView emptyView = (TextView) findViewById(android.R.id.empty);
        view.setEmptyView(emptyView);
        CapabilitiesListAdapter adapter = createListAdapter();
        view.setAdapter(adapter);
    }

    private CapabilitiesListAdapter createListAdapter() {
        Cursor cursor = getContentResolver().query(CapabilitiesLog.CONTENT_URI, PROJECTION, null,
                null, SORT_ORDER);
        if (cursor == null) {
            showMessageThenExit(R.string.label_db_failed);
            return null;
        }
        return new CapabilitiesListAdapter(this, cursor);
    }

    /**
     * List adapter
     */
    private class CapabilitiesListAdapter extends CursorAdapter {
        private RcsContactUtil rcsDisplayName;

        /**
         * Constructor
         * 
         * @param context Context
         * @param c Cursor
         */
        public CapabilitiesListAdapter(Context context, Cursor c) {
            // TODO use CursorLoader
            super(context, c);
            rcsDisplayName = RcsContactUtil.getInstance(context);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.capabilities_list_item, parent, false);

            CapabilitiesItemViewHolder holder = new CapabilitiesItemViewHolder(view, cursor);
            view.setTag(holder);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            CapabilitiesItemViewHolder holder = (CapabilitiesItemViewHolder) view.getTag();

            // Set display name from number
            String number = cursor.getString(holder.columnContact);
            String displayName = rcsDisplayName.getDisplayName(number);
            holder.numberText.setText(getString(R.string.label_contact_arg, displayName));

            holder.imBox
                    .setChecked(cursor.getInt(holder.columnCapabilityIm) == CapabilitiesLog.SUPPORTED);
            holder.ftBox
                    .setChecked(cursor.getInt(holder.columnCapabilityFileTransfer) == CapabilitiesLog.SUPPORTED);
            holder.ishBox
                    .setChecked(cursor.getInt(holder.columnCapabilityImageSharing) == CapabilitiesLog.SUPPORTED);
            holder.vshBox
                    .setChecked(cursor.getInt(holder.columnCapabilityVideoSharing) == CapabilitiesLog.SUPPORTED);
            holder.geolocBox
                    .setChecked(cursor.getInt(holder.columnCapabilityGeolocPush) == CapabilitiesLog.SUPPORTED);
            String exts = cursor.getString(holder.columnCapabilityExtensions);
            if (exts != null) {
                exts = exts.replace(';', '\n');
            }
            holder.extsText.setText(exts);
            holder.automataBox
                    .setChecked(cursor.getInt(holder.columnAutomata) == CapabilitiesLog.SUPPORTED);
            long lastRefresh = cursor.getLong(holder.columnTimestamp);
            if (lastRefresh == -1) {
                holder.lastRefreshText.setVisibility(View.GONE);
            } else {
                holder.lastRefreshText.setVisibility(View.VISIBLE);
                holder.lastRefreshText.setText(DateUtils.getRelativeTimeSpanString(lastRefresh,
                        System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE));
            }
        }
    }

    /**
     * A ViewHolder class keeps references to children views to avoid unnecessary calls to
     * findViewById() or getColumnIndex() on each row.
     */
    private class CapabilitiesItemViewHolder {
        public TextView numberText;

        public final CheckBox imBox;

        public final CheckBox ftBox;

        public final CheckBox ishBox;

        public final CheckBox vshBox;

        public final CheckBox geolocBox;

        public final TextView extsText;

        public final CheckBox automataBox;

        public final TextView lastRefreshText;

        public final int columnContact;

        public final int columnCapabilityIm;

        public final int columnCapabilityImageSharing;

        public final int columnCapabilityFileTransfer;

        public final int columnCapabilityVideoSharing;

        public final int columnCapabilityGeolocPush;

        public final int columnCapabilityExtensions;

        public final int columnAutomata;

        public final int columnTimestamp;

        CapabilitiesItemViewHolder(View base, Cursor cursor) {
            columnContact = cursor.getColumnIndexOrThrow(CapabilitiesLog.CONTACT);
            columnCapabilityIm = cursor
                    .getColumnIndexOrThrow(CapabilitiesLog.CAPABILITY_IM_SESSION);
            columnCapabilityFileTransfer = cursor
                    .getColumnIndexOrThrow(CapabilitiesLog.CAPABILITY_FILE_TRANSFER);
            columnCapabilityImageSharing = cursor
                    .getColumnIndexOrThrow(CapabilitiesLog.CAPABILITY_IMAGE_SHARE);
            columnCapabilityVideoSharing = cursor
                    .getColumnIndexOrThrow(CapabilitiesLog.CAPABILITY_VIDEO_SHARE);
            columnCapabilityGeolocPush = cursor
                    .getColumnIndexOrThrow(CapabilitiesLog.CAPABILITY_GEOLOC_PUSH);
            columnCapabilityExtensions = cursor
                    .getColumnIndexOrThrow(CapabilitiesLog.CAPABILITY_EXTENSIONS);
            columnAutomata = cursor.getColumnIndexOrThrow(CapabilitiesLog.AUTOMATA);
            columnTimestamp = cursor.getColumnIndexOrThrow(CapabilitiesLog.TIMESTAMP);

            numberText = (TextView) base.findViewById(R.id.number);
            imBox = (CheckBox) base.findViewById(R.id.im);
            ftBox = (CheckBox) base.findViewById(R.id.file_transfer);
            ishBox = (CheckBox) base.findViewById(R.id.image_sharing);
            vshBox = (CheckBox) base.findViewById(R.id.video_sharing);
            geolocBox = (CheckBox) base.findViewById(R.id.geoloc_push);
            extsText = (TextView) base.findViewById(R.id.extensions);
            automataBox = (CheckBox) base.findViewById(R.id.automata);
            lastRefreshText = (TextView) base.findViewById(R.id.last_refresh);
        }

    }
}
