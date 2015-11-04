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

package com.orangelabs.rcs.ri.messaging.chat;

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.history.HistoryLog;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.ContactUtil;
import com.orangelabs.rcs.ri.utils.RcsContactUtil;
import com.orangelabs.rcs.ri.utils.SmileyParser;
import com.orangelabs.rcs.ri.utils.Smileys;
import com.orangelabs.rcs.ri.utils.Utils;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author YPLO6403
 */
public class ChatCursorAdapter extends CursorAdapter {

    private boolean mIsSingleChat = true;

    private LayoutInflater mInflater;

    private final static SimpleDateFormat df = new SimpleDateFormat("yy-MM-dd HH:mm:ss",
            Locale.getDefault());

    private static final int CHAT_MESSAGE = 0;
    private static final int GROUPCHAT_EVENT = 1;
    private static final int FILETRANSFER_MESSAGE = 2;

    /**
     * A map between contact and display name to minimize queries of RCS settings provider
     */
    private Map<ContactId, String> mContactIdDisplayNameMap = new HashMap<>();

    private Context mContext;

    private Smileys mSmileyResources;

    /**
     * Constructor
     * 
     * @param context The context
     * @param isSingleChat True if single chat
     */
    public ChatCursorAdapter(Context context, boolean isSingleChat) {
        super(context, null, 0);
        mInflater = LayoutInflater.from(context);
        mIsSingleChat = isSingleChat;
        mSmileyResources = new Smileys(context);
        mContext = context;
    }

    private int getItemViewType(Cursor cursor) {
        String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(HistoryLog.MIME_TYPE));
        switch (mimeType) {
            case ChatLog.Message.MimeType.GEOLOC_MESSAGE:
            case ChatLog.Message.MimeType.TEXT_MESSAGE:
                return CHAT_MESSAGE;

            case ChatLog.Message.MimeType.GROUPCHAT_EVENT:
                return GROUPCHAT_EVENT;

            default:
                return FILETRANSFER_MESSAGE;
        }
    }

    @Override
    public int getItemViewType(int position) {
        Cursor cursor = (Cursor) getItem(position);
        return getItemViewType(cursor);
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view;
        String mimetype = cursor.getString(cursor.getColumnIndexOrThrow(HistoryLog.MIME_TYPE));

        switch (mimetype) {
            case ChatLog.Message.MimeType.GEOLOC_MESSAGE:
            case ChatLog.Message.MimeType.TEXT_MESSAGE:
                view = mInflater.inflate(R.layout.chat_view_item, parent, false);
                view.setTag(new ViewHolderChatMessage(view, cursor));
                break;

            case ChatLog.Message.MimeType.GROUPCHAT_EVENT:
                view = mInflater.inflate(R.layout.groupchat_event_view_item, parent, false);
                view.setTag(new ViewHolder(view, cursor));
                break;

            default:
                view = mInflater.inflate(R.layout.filetransfer_view_item, parent, false);
                view.setTag(new ViewHolderFileTransfer(view, cursor));
                break;
        }
        return view;
    }

    @Override
    public void bindView(View view, Context ctx, Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();
        int itemType = getItemViewType(cursor);
        Direction direction = Direction.valueOf(cursor.getInt(holder.columnDirection));
        long date = cursor.getLong(holder.columnTimestamp);
        int status = cursor.getInt(holder.columnStatus);

        String displayName = null;
        if (!mIsSingleChat && Direction.OUTGOING != direction) {
            String number = cursor.getString(holder.columnContact);
            if (number != null) {
                ContactId contact = ContactUtil.formatContact(number);
                if (!mContactIdDisplayNameMap.containsKey(contact)) {
                    // Display name is not known, save it into map
                    displayName = RcsContactUtil.getInstance(ctx).getDisplayName(contact);
                    mContactIdDisplayNameMap.put(contact, displayName);
                } else {
                    displayName = mContactIdDisplayNameMap.get(contact);
                }
            }
        }
        holder.dateText.setText(df.format(date));
        if (itemType == CHAT_MESSAGE) {
            bindChatMessage(ctx, cursor, direction, displayName, status,
                    (ViewHolderChatMessage) holder);

        } else if (itemType == GROUPCHAT_EVENT) {
            bindGroupChatEvent(ctx, displayName, status, holder);

        } else {
            bindFileTransferMessage(cursor, direction, displayName, status,
                    (ViewHolderFileTransfer) holder);
        }
    }

    private void bindFileTransferMessage(Cursor cursor, Direction dir, String displayName,
            int status, ViewHolderFileTransfer holder) {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        String mimeType = cursor.getString(holder.columnMimetype);
        // TODO display distinct icon or thumbnail depending on the mimetype
        holder.statusText.setText(RiApplication.sFileTransferStates[status]);
        StringBuilder filename = new StringBuilder(cursor.getString(holder.columnFilename));
        long filesize = cursor.getLong(holder.columnFilesize);
        long transferred = cursor.getLong(holder.columnTransferred);

        if (filesize != transferred) {
            holder.progressText.setText(filename.append(" : ")
                    .append(Utils.getProgressLabel(transferred, filesize)).toString());
            holder.fileImageOutgoing.setImageResource(R.drawable.ri_filetransfer_off);
            holder.fileImageIncoming.setImageResource(R.drawable.ri_filetransfer_off);
        } else {
            int reason = cursor.getInt(holder.columnReasonCode);
            FileTransfer.ReasonCode reasonCode = FileTransfer.ReasonCode.valueOf(reason);
            if (FileTransfer.ReasonCode.UNSPECIFIED == reasonCode) {
                holder.fileImageOutgoing.setImageResource(R.drawable.ri_filetransfer_on);
                holder.fileImageIncoming.setImageResource(R.drawable.ri_filetransfer_on);
            } else {
                holder.fileImageOutgoing.setImageResource(R.drawable.ri_filetransfer_off);
                holder.fileImageIncoming.setImageResource(R.drawable.ri_filetransfer_off);
            }
            holder.progressText.setText(filename.append(" (").append(filesize / 1024)
                    .append(" Kb)").toString());
        }
        if (Direction.OUTGOING == dir) {
            boolean undeliveredExpiration = cursor.getInt(holder.columnExpiredDelivery) == 1;
            holder.undeliveredIcon.setVisibility(undeliveredExpiration ? View.VISIBLE : View.GONE);
            holder.contactText.setVisibility(View.INVISIBLE);
            holder.fileImageIncoming.setVisibility(View.GONE);
            holder.fileImageOutgoing.setVisibility(View.VISIBLE);
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        } else {
            holder.undeliveredIcon.setVisibility(View.GONE);
            holder.fileImageIncoming.setVisibility(View.VISIBLE);
            holder.fileImageOutgoing.setVisibility(View.GONE);
            if (displayName != null) {
                holder.contactText.setVisibility(View.VISIBLE);
                holder.contactText.setText(displayName);
            } else {
                holder.contactText.setVisibility(View.INVISIBLE);
            }
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        }
        holder.filetransferItemLayout.setLayoutParams(lp);
    }

    private void bindGroupChatEvent(Context ctx, String displayName, int status, ViewHolder holder) {
        String event = RiApplication.sGroupChatEvents[status];
        holder.statusText.setText(ctx.getString(R.string.label_groupchat_event, event));
        if (displayName != null) {
            holder.contactText.setVisibility(View.VISIBLE);
            holder.contactText.setText(displayName);
        } else {
            holder.contactText.setVisibility(View.GONE);
        }
    }

    private void bindChatMessage(Context ctx, Cursor cursor, Direction dir, String displayName,
            int status, ViewHolderChatMessage holder) {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        String mimeType = cursor.getString(holder.columnMimetype);
        holder.statusText.setText(RiApplication.sMessagesStatuses[status]);
        String data = cursor.getString(holder.columnContent);
        holder.chatText.setText(formatDataToText(ctx, mimeType, data));
        if (Direction.OUTGOING == dir) {
            boolean undeliveredExpiration = cursor.getInt(holder.columnExpiredDelivery) == 1;
            holder.undeliveredIcon.setVisibility(undeliveredExpiration ? View.VISIBLE : View.GONE);
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            /* Set background bubble for outgoing */
            holder.chatItemLayout.setBackgroundDrawable(ctx.getResources().getDrawable(
                    R.drawable.msg_item_left));
            holder.contactText.setVisibility(View.GONE);
        } else {
            holder.undeliveredIcon.setVisibility(View.GONE);
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            /* Set background for incoming */
            holder.chatItemLayout.setBackgroundDrawable(ctx.getResources().getDrawable(
                    R.drawable.msg_item_right));
            if (displayName != null) {
                holder.contactText.setVisibility(View.VISIBLE);
                holder.contactText.setText(displayName);
            } else {
                holder.contactText.setVisibility(View.GONE);
            }
        }
        holder.chatItemLayout.setLayoutParams(lp);
    }

    /**
     * Format data to text
     * 
     * @param context context
     * @param mimeType mime type
     * @param data data
     * @return a formatted text
     */
    private CharSequence formatDataToText(Context context, String mimeType, String data) {
        if (ChatLog.Message.MimeType.TEXT_MESSAGE.equals(mimeType)) {
            return formatMessageWithSmiley(data);

        }
        if (ChatLog.Message.MimeType.GEOLOC_MESSAGE.equals(mimeType)) {
            Geoloc geoloc = new Geoloc(data);
            StringBuilder result = new StringBuilder(
                    context.getString(R.string.label_geolocation_msg)).append("\n");
            String label = geoloc.getLabel();
            if (label != null) {
                result.append(context.getString(R.string.label_location)).append(" ")
                        .append(geoloc.getLabel()).append("\n");
            }
            return result.append(context.getString(R.string.label_latitude)).append(" ")
                    .append(geoloc.getLatitude()).append("\n")
                    .append(context.getString(R.string.label_longitude)).append(" ")
                    .append(geoloc.getLongitude()).append("\n")
                    .append(context.getString(R.string.label_accuracy)).append(" ")
                    .append(geoloc.getAccuracy()).toString();
        }
        return null;
    }

    /**
     * A ViewHolder class keeps references to children views to avoid unnecessary calls to
     * findViewById() or getColumnIndex() on each row.
     */
    private class ViewHolder {
        TextView statusText;
        TextView dateText;
        TextView contactText;

        int columnDirection;
        int columnTimestamp;
        int columnStatus;
        int columnContact;
        int columnMimetype;

        /**
         * Constructor
         * 
         * @param base view
         * @param cursor cursor
         */
        ViewHolder(View base, Cursor cursor) {
            /* Save column indexes */
            columnDirection = cursor.getColumnIndexOrThrow(HistoryLog.DIRECTION);
            columnTimestamp = cursor.getColumnIndexOrThrow(HistoryLog.TIMESTAMP);
            columnStatus = cursor.getColumnIndexOrThrow(HistoryLog.STATUS);
            columnContact = cursor.getColumnIndexOrThrow(HistoryLog.CONTACT);
            columnMimetype = cursor.getColumnIndexOrThrow(HistoryLog.MIME_TYPE);
            /* Save children views */
            statusText = (TextView) base.findViewById(R.id.status_text);
            dateText = (TextView) base.findViewById(R.id.date_text);
            contactText = (TextView) base.findViewById(R.id.contact_text);
        }

    }

    /**
     * A ViewHolder class keeps references to children views to avoid unnecessary calls to
     * findViewById() or getColumnIndex() on each row.
     */
    private class ViewHolderChatMessage extends ViewHolder {
        RelativeLayout chatItemLayout;
        TextView chatText;
        ImageView undeliveredIcon;

        int columnContent;
        int columnExpiredDelivery;

        /**
         * Constructor
         * 
         * @param base view
         * @param cursor cursor
         */
        ViewHolderChatMessage(View base, Cursor cursor) {
            super(base, cursor);
            /* Save column indexes */
            columnContent = cursor.getColumnIndexOrThrow(HistoryLog.CONTENT);
            columnExpiredDelivery = cursor.getColumnIndexOrThrow(HistoryLog.EXPIRED_DELIVERY);
            /* Save children views */
            chatItemLayout = (RelativeLayout) base.findViewById(R.id.msg_item);
            chatText = (TextView) base.findViewById(R.id.chat_text);
            undeliveredIcon = (ImageView) base.findViewById(R.id.undelivered);
        }

    }

    private class ViewHolderFileTransfer extends ViewHolder {
        RelativeLayout filetransferItemLayout;
        TextView progressText;
        ImageView fileImageOutgoing;
        ImageView fileImageIncoming;
        ImageView undeliveredIcon;

        int columnExpiredDelivery;
        int columnFilename;
        int columnFilesize;
        int columnTransferred;
        int columnReasonCode;

        /**
         * Constructor
         * 
         * @param base view
         * @param cursor cursor
         */
        ViewHolderFileTransfer(View base, Cursor cursor) {
            super(base, cursor);
            /* Save column indexes */
            columnExpiredDelivery = cursor.getColumnIndexOrThrow(HistoryLog.EXPIRED_DELIVERY);
            columnFilename = cursor.getColumnIndexOrThrow(HistoryLog.FILENAME);
            columnFilesize = cursor.getColumnIndexOrThrow(HistoryLog.FILESIZE);
            columnTransferred = cursor.getColumnIndexOrThrow(HistoryLog.TRANSFERRED);
            columnReasonCode = cursor.getColumnIndexOrThrow(HistoryLog.REASON_CODE);
            /* Save children views */
            filetransferItemLayout = (RelativeLayout) base.findViewById(R.id.rl_file_item);
            fileImageOutgoing = (ImageView) base.findViewById(R.id.file_image_outgoing);
            fileImageIncoming = (ImageView) base.findViewById(R.id.file_image_incoming);
            progressText = (TextView) base.findViewById(R.id.progress_text);
            undeliveredIcon = (ImageView) base.findViewById(R.id.undelivered);
        }

    }

    /**
     * Format text with smiley
     * 
     * @param txt Text
     * @return String
     */
    private CharSequence formatMessageWithSmiley(String txt) {
        SpannableStringBuilder buf = new SpannableStringBuilder();
        if (!TextUtils.isEmpty(txt)) {
            SmileyParser smileyParser = new SmileyParser(txt, mSmileyResources);
            smileyParser.parse();
            buf.append(smileyParser.getSpannableString(mContext));
        }
        return buf;
    }
}
