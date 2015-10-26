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

package com.orangelabs.rcs.ri.messaging;

import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfoLog;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.RcsContactUtil;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * @author YPLO6403
 */
public class GroupDeliveryInfoCursorAdapter extends CursorAdapter {

    private LayoutInflater mInflater;

    private final static SimpleDateFormat df = new SimpleDateFormat("yy-MM-dd HH:mm:ss",
            Locale.getDefault());

    private Context mContext;

    /**
     * Constructor
     * 
     * @param context The context
     */
    public GroupDeliveryInfoCursorAdapter(Context context) {
        super(context, null, 0);
        mInflater = LayoutInflater.from(context);
        mContext = context;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final View view = mInflater.inflate(R.layout.delivery_info_item, parent, false);
        // Save columns indexes and children views in cache
        view.setTag(new ViewHolder(view, cursor));
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        // Set the deliver date/time field
        long timestampDeliver = cursor.getLong(holder.columnTimestampDeliver);
        // Set the deliver date/time field
        long timestampDisplay = cursor.getLong(holder.columnTimestampDisplay);
        // Set the status text
        GroupDeliveryInfo.Status status = GroupDeliveryInfo.Status.valueOf(cursor
                .getInt(holder.columnStatus));
        // Set the reason text
        GroupDeliveryInfo.ReasonCode reason = GroupDeliveryInfo.ReasonCode.valueOf(cursor
                .getInt(holder.columnReason));
        // Set the display name
        String number = cursor.getString(holder.columnContact);
        String displayName = RcsContactUtil.getInstance(mContext).getDisplayName(number);
        holder.contactText.setText(mContext.getString(R.string.label_from_args, displayName));

        if (timestampDeliver == 0) {
            holder.deliverText.setVisibility(View.GONE);
        } else {
            holder.deliverText.setVisibility(View.VISIBLE);
            holder.deliverText.setText(mContext.getString(R.string.label_state_delivered_at,
                    df.format(timestampDeliver)));
        }
        /* Display information is only applicable to file transfers */
        if (timestampDisplay == 0) {
            holder.displayText.setVisibility(View.GONE);
        } else {
            holder.displayText.setVisibility(View.VISIBLE);
            holder.displayText.setText(mContext.getString(R.string.label_state_displayed_at,
                    df.format(timestampDisplay)));
        }

        String _status = mContext.getString(R.string.label_status,
                RiApplication.sDeliveryStatuses[status.toInt()]);
        holder.statusText.setText(_status);
        if (reason != GroupDeliveryInfo.ReasonCode.UNSPECIFIED) {
            holder.reasonText.setVisibility(View.VISIBLE);
            String _reason = mContext.getString(R.string.label_reason_code_args,
                    RiApplication.sDeliveryReasonCode[reason.toInt()]);
            holder.reasonText.setText(_reason);
        } else {
            holder.reasonText.setVisibility(View.GONE);
        }

    }

    /**
     * A ViewHolder class keeps references to children views to avoid unnecessary calls to
     * findViewById() or getColumnIndex() on each row.
     */
    private class ViewHolder {
        TextView statusText;

        TextView reasonText;

        TextView deliverText;

        TextView displayText;

        TextView contactText;

        int columnContact;

        int columnTimestampDeliver;

        int columnTimestampDisplay;

        int columnStatus;

        int columnReason;

        /**
         * Constructor
         * 
         * @param base the view
         * @param cursor cursor
         */
        ViewHolder(View base, Cursor cursor) {
            columnContact = cursor.getColumnIndexOrThrow(GroupDeliveryInfoLog.CONTACT);
            columnTimestampDeliver = cursor
                    .getColumnIndexOrThrow(GroupDeliveryInfoLog.TIMESTAMP_DELIVERED);
            columnTimestampDisplay = cursor
                    .getColumnIndexOrThrow(GroupDeliveryInfoLog.TIMESTAMP_DISPLAYED);
            columnStatus = cursor.getColumnIndexOrThrow(GroupDeliveryInfoLog.STATUS);
            columnReason = cursor.getColumnIndexOrThrow(GroupDeliveryInfoLog.REASON_CODE);
            statusText = (TextView) base.findViewById(R.id.status);
            contactText = (TextView) base.findViewById(R.id.contact);
            deliverText = (TextView) base.findViewById(R.id.deliver);
            displayText = (TextView) base.findViewById(R.id.display);
            reasonText = (TextView) base.findViewById(R.id.reason);
        }
    }
}
