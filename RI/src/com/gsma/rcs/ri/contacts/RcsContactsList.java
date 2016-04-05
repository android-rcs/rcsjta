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

package com.gsma.rcs.ri.contacts;

import com.gsma.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.gsma.rcs.api.connection.utils.RcsListActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.utils.RcsContactUtil;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.capability.Capabilities;
import com.gsma.services.rcs.contact.RcsContact;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * List of RCS contacts
 *
 * @author Philippe LEMORDANT
 * @author Jean-Marc AUFFRET
 */
public class RcsContactsList extends RcsListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.contacts_rcs_list);
        /* Register to API connection manager */
        if (!isServiceConnected(RcsServiceName.CONTACT)) {
            showMessageThenExit(R.string.label_service_not_available);
            return;
        }
        startMonitorServices(RcsServiceName.CONTACT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isExiting()) {
            return;
        }
        updateList();
    }

    private void updateList() {
        try {
            Set<RcsContact> allContacts = getContactApi().getRcsContacts();
            List<RcsContact> contacts = new ArrayList<>(allContacts);
            if (contacts.size() > 0) {
                ContactArrayAdapter adapter = new ContactArrayAdapter(this,
                        R.layout.contact_list_item, contacts);
                setListAdapter(adapter);
            } else {
                setListAdapter(null);
            }
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    private class ContactArrayAdapter extends ArrayAdapter<RcsContact> {

        private final Context mCtx;
        private final int mResourceRowLayout;
        private final RcsContactUtil mRcsContactUtil;
        private final LayoutInflater mInflater;

        public ContactArrayAdapter(Context ctx, int resourceRowLayout, List<RcsContact> items) {
            super(ctx, 0, items);
            mCtx = ctx;
            mResourceRowLayout = resourceRowLayout;
            mInflater = LayoutInflater.from(mCtx);
            mRcsContactUtil = RcsContactUtil.getInstance(mCtx);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final CapabilitiesItemViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(mResourceRowLayout, parent, false);
                holder = new CapabilitiesItemViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (CapabilitiesItemViewHolder) convertView.getTag();
            }
            RcsContact item = getItem(position);
            if (item != null) {
                String displayName = mRcsContactUtil.getDisplayName(item.getContactId());
                holder.numberText.setText(getString(R.string.label_contact_arg, displayName));
                Capabilities capa = item.getCapabilities();
                holder.imBox.setChecked(capa.hasCapabilities(Capabilities.CAPABILITY_IM));
                holder.ftBox
                        .setChecked(capa.hasCapabilities(Capabilities.CAPABILITY_FILE_TRANSFER));
                holder.ishBox.setChecked(capa
                        .hasCapabilities(Capabilities.CAPABILITY_IMAGE_SHARING));
                holder.vshBox.setChecked(capa
                        .hasCapabilities(Capabilities.CAPABILITY_VIDEO_SHARING));
                holder.geolocBox.setChecked(capa
                        .hasCapabilities(Capabilities.CAPABILITY_GEOLOC_PUSH));
                holder.extsText.setText(capa.getSupportedExtensions().toString());
                holder.automataBox.setChecked(capa.isAutomata());
                long lastRefresh = capa.getTimestamp();
                if (lastRefresh == -1) {
                    holder.lastRefreshText.setVisibility(View.GONE);
                } else {
                    holder.lastRefreshText.setVisibility(View.VISIBLE);
                    holder.lastRefreshText.setText(DateUtils.getRelativeTimeSpanString(lastRefresh,
                            System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE));
                }
                holder.onLineBox.setChecked(item.isOnline());
                boolean blocked = item.isBlocked();
                holder.blockedBox.setChecked(blocked);
                long blockingTime = item.getBlockingTimestamp();
                if (!blocked) {
                    holder.blockingTimeText.setVisibility(View.GONE);
                } else {
                    holder.blockingTimeText.setVisibility(View.VISIBLE);
                    holder.blockingTimeText.setText(DateUtils.getRelativeTimeSpanString(
                            blockingTime, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE));
                }
            }
            return convertView;
        }
    }

    /**
     * A ViewHolder class keeps references to children views to avoid unnecessary calls to
     * findViewById().
     */
    private class CapabilitiesItemViewHolder {
        public final CheckBox onLineBox;
        public final CheckBox blockedBox;
        public final TextView blockingTimeText;
        public final TextView numberText;
        public final CheckBox imBox;
        public final CheckBox ftBox;
        public final CheckBox ishBox;
        public final CheckBox vshBox;
        public final CheckBox geolocBox;
        public final TextView extsText;
        public final CheckBox automataBox;
        public final TextView lastRefreshText;

        CapabilitiesItemViewHolder(View base) {
            numberText = (TextView) base.findViewById(R.id.number);
            imBox = (CheckBox) base.findViewById(R.id.im);
            ftBox = (CheckBox) base.findViewById(R.id.file_transfer);
            ishBox = (CheckBox) base.findViewById(R.id.image_sharing);
            vshBox = (CheckBox) base.findViewById(R.id.video_sharing);
            geolocBox = (CheckBox) base.findViewById(R.id.geoloc_push);
            extsText = (TextView) base.findViewById(R.id.extensions);
            automataBox = (CheckBox) base.findViewById(R.id.automata);
            lastRefreshText = (TextView) base.findViewById(R.id.last_refresh);
            onLineBox = (CheckBox) base.findViewById(R.id.is_online);
            blockedBox = (CheckBox) base.findViewById(R.id.is_blocked);
            blockingTimeText = (TextView) base.findViewById(R.id.block_timestamp);
        }

    }
}
