/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.ri.messaging.adapter;

import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.utils.RcsContactUtil;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.contact.ContactId;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Conversation cursor adapter
 */
public class TalkListArrayAdapter extends ArrayAdapter<TalkListArrayItem> {

    private final RcsContactUtil mRcsContactUtil;
    private final LayoutInflater mInflater;
    private final Context mCtx;

    private static final int VIEW_TYPE_1TO1_CHAT = 0;
    private static final int VIEW_TYPE_GROUP_CHAT = 1;

    public TalkListArrayAdapter(Context context, List<TalkListArrayItem> messageLogs) {
        super(context, R.layout.chat_list, messageLogs);
        mCtx = context;
        mRcsContactUtil = RcsContactUtil.getInstance(context);
        mInflater = ((Activity) context).getLayoutInflater();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            int viewType = getItemViewType(position);
            if (VIEW_TYPE_1TO1_CHAT == viewType) {
                convertView = mInflater.inflate(R.layout.talk_log_one_to_one_item, parent, false);
                convertView.setTag(new TalkListArrayItem.ViewHolderOneToOne(convertView));
            } else {
                convertView = mInflater.inflate(R.layout.talk_log_group_item, parent, false);
                convertView.setTag(new TalkListArrayItem.ViewHolderGroup(convertView));
            }
        }
        bindView(convertView, position);
        return convertView;
    }

    @Override
    public int getItemViewType(int position) {
        TalkListArrayItem item = getItem(position);
        return item.isGroupChat() ? VIEW_TYPE_GROUP_CHAT : VIEW_TYPE_1TO1_CHAT;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    public void bindView(View view, int position) {
        TalkListArrayItem item = getItem(position);
        int viewType = getItemViewType(position);
        TalkListArrayItem.ViewHolder holder = (TalkListArrayItem.ViewHolder) view.getTag();
        setTimestamp(holder, item.getTimestamp());
        String content = item.getContent();
        if (ChatLog.Message.MimeType.GEOLOC_MESSAGE.equals(item.getMimeType())) {
            content = TalkCursorAdapter.formatGeolocation(mCtx, new Geoloc(item.getContent()));
        }
        setContent(holder, content);
        setStatus(holder, item.getUnreadCount());
        switch (viewType) {
            case VIEW_TYPE_1TO1_CHAT:
                bindViewOneToOneTalk(item, (TalkListArrayItem.ViewHolderOneToOne) holder);
                break;

            case VIEW_TYPE_GROUP_CHAT:
                bindViewGroupChat(item, (TalkListArrayItem.ViewHolderGroup) holder);
                break;

            default:
                throw new IllegalArgumentException("Invalid view type: '" + viewType + "'!");
        }
    }

    private void bindViewGroupChat(TalkListArrayItem item, TalkListArrayItem.ViewHolderGroup holder) {
        holder.getSubjectText().setText(item.getSubject());
    }

    private void bindViewOneToOneTalk(TalkListArrayItem item,
            TalkListArrayItem.ViewHolderOneToOne holder) {
        ImageView avatar = holder.getAvatarImage();
        ContactId contact = item.getContact();
        Bitmap photo = mRcsContactUtil.getPhotoFromContactId(contact);
        if (photo != null) {
            avatar.setImageBitmap(photo);
        } else {
            avatar.setImageResource(R.drawable.person);
        }
        setContact(holder, item.getContact());
    }

    private void setContent(TalkListArrayItem.ViewHolder holder, String content) {
        holder.getContentText().setText(content != null ? content : "");
    }

    private void setTimestamp(TalkListArrayItem.ViewHolder holder, long timestamp) {
        /* Set the date/time field by mixing relative and absolute times */
        holder.getTimestampText().setText(
                DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE));
    }

    private void setStatus(TalkListArrayItem.ViewHolder holder, int unReads) {
        TextView statusText = holder.getStatusText();
        if (unReads == 0) {
            statusText.setVisibility(View.INVISIBLE);
        } else {
            statusText.setVisibility(View.VISIBLE);
            String countUnReads = Integer.valueOf(unReads).toString();
            if (unReads <= 9) {
                countUnReads = " ".concat(countUnReads);
            }
            statusText.setText(countUnReads);
        }
    }

    private void setContact(TalkListArrayItem.ViewHolderOneToOne holder, ContactId contact) {
        String displayName = mRcsContactUtil.getDisplayName(contact);
        holder.getContactText().setText(displayName);
    }

}
