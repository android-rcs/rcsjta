/**
 * 
 */

package com.orangelabs.rcs.ri.messaging;

import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfoLog;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;

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
        long deliver = cursor.getLong(holder.columnDeliver);
        // Set the deliver date/time field
        long display = cursor.getLong(holder.columnDisplay);
        // Set the status text
        GroupDeliveryInfo.Status status = GroupDeliveryInfo.Status.valueOf(cursor
                .getInt(holder.columnStatus));
        // Set the reason text
        GroupDeliveryInfo.ReasonCode reason = GroupDeliveryInfo.ReasonCode.valueOf(cursor
                .getInt(holder.columnReason));
        // Set the display name
        String number = cursor.getString(holder.columnContact);
        String displayName = RcsDisplayName.getInstance(mContext).getDisplayName(number);
        holder.contactText.setText(mContext.getString(R.string.label_from_args, displayName));

        if (deliver == 0) {
            holder.deliverText.setVisibility(View.GONE);
        } else {
            holder.deliverText.setVisibility(View.VISIBLE);
            holder.deliverText.setText(mContext.getString(R.string.label_state_delivered_at,
                    df.format(deliver)));
        }
        /* Display information is only applicable to file transfers */
        if (display == 0) {
            holder.displayText.setVisibility(View.GONE);
        } else {
            holder.displayText.setVisibility(View.VISIBLE);
            holder.displayText.setText(mContext.getString(R.string.label_state_displayed_at,
                    df.format(display)));
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

        int columnDeliver;

        int columnDisplay;

        int columnStatus;

        int columnReason;

        /**
         * Constructor
         * 
         * @param base the view
         * @param cursor
         */
        ViewHolder(View base, Cursor cursor) {
            columnContact = cursor.getColumnIndexOrThrow(GroupDeliveryInfoLog.CONTACT);
            columnDeliver = cursor.getColumnIndexOrThrow(GroupDeliveryInfoLog.TIMESTAMP_DELIVERED);
            columnDisplay = cursor.getColumnIndexOrThrow(GroupDeliveryInfoLog.TIMESTAMP_DISPLAYED);
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
