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

package com.gsma.rcs.ri.messaging.adapter;

import com.gsma.rcs.api.connection.utils.ExceptionUtil;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.RiApplication;
import com.gsma.rcs.ri.utils.BitmapCache;
import com.gsma.rcs.ri.utils.BitmapLoader;
import com.gsma.rcs.ri.utils.BitmapLoader.BitmapCacheInfo;
import com.gsma.rcs.ri.utils.ContactUtil;
import com.gsma.rcs.ri.utils.FileUtils;
import com.gsma.rcs.ri.utils.ImageBitmapLoader;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.rcs.ri.utils.RcsContactUtil;
import com.gsma.rcs.ri.utils.SmileyParser;
import com.gsma.rcs.ri.utils.Smileys;
import com.gsma.rcs.ri.utils.Utils;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatLog.Message.Content;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.filetransfer.FileTransferService;
import com.gsma.services.rcs.history.HistoryLog;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;
import android.support.v4.util.LruCache;
import android.support.v4.widget.CursorAdapter;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

public class TalkCursorAdapter extends CursorAdapter {

    private static final String LOGTAG = LogUtils.getTag(TalkCursorAdapter.class.getName());

    private static final int MAX_IMAGE_HEIGHT = 100;
    private static final int MAX_IMAGE_WIDTH = 100;
    private static final int VIEW_TYPE_RCS_CHAT_IN = 0;
    private static final int VIEW_TYPE_RCS_CHAT_OUT = 1;
    private static final int VIEW_TYPE_RCS_FILE_TRANSFER_IN = 2;
    private static final int VIEW_TYPE_RCS_FILE_TRANSFER_OUT = 3;
    private static final int VIEW_TYPE_RCS_GROUP_CHAT_EVENT = 4;
    private final ChatService mChatService;
    private final FileTransferService mFileTransferService;
    private BitmapCache bitmapCache;
    private final Activity mActivity;
    private Map<ContactId, String> mContactIdDisplayNameMap;
    private final LayoutParams mImageParams;
    private final LayoutParams mImageParamsDefault;
    private LayoutInflater mInflater;
    private final boolean mSingleChat;
    private Smileys mSmileyResources;

    /**
     * Constructor
     *
     * @param activity The activity
     * @param singleChat True if single chat
     * @param chatService the chat service
     * @param fileTransferService the file transfer service
     */
    public TalkCursorAdapter(Activity activity, boolean singleChat, ChatService chatService,
            FileTransferService fileTransferService) {
        super(activity, null, 0);
        mContactIdDisplayNameMap = new HashMap<>();
        mActivity = activity;
        mInflater = LayoutInflater.from(activity);
        int size100Dp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100.0f,
                mContext.getResources().getDisplayMetrics());
        mImageParams = new LayoutParams(size100Dp, size100Dp);
        Options opt = new Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.ri_filetransfer_off, opt);
        mImageParamsDefault = new LayoutParams(opt.outWidth * 2, opt.outHeight * 2);
        bitmapCache = BitmapCache.getInstance();
        mSmileyResources = new Smileys(activity);
        mSingleChat = singleChat;
        mChatService = chatService;
        mFileTransferService = fileTransferService;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int viewType = getItemViewType(cursor);
        switch (viewType) {
            case VIEW_TYPE_RCS_CHAT_IN:
                View view = mInflater.inflate(mSingleChat ? R.layout.talk_item_rcs_chat_in
                        : R.layout.gchat_item_rcs_chat_in, parent, false);
                view.setTag(new RcsChatInViewHolder(view, cursor));
                return view;

            case VIEW_TYPE_RCS_CHAT_OUT:
                view = mInflater.inflate(mSingleChat ? R.layout.talk_item_rcs_chat_out
                        : R.layout.gchat_item_rcs_chat_out, parent, false);
                view.setTag(new RcsChatOutViewHolder(view, cursor));
                return view;

            case VIEW_TYPE_RCS_FILE_TRANSFER_IN:
                view = mInflater.inflate(mSingleChat ? R.layout.talk_item_rcs_file_transfer_in
                        : R.layout.gchat_item_rcs_file_transfer_in, parent, false);
                view.setTag(new RcsFileTransferInViewHolder(view, cursor));
                return view;

            case VIEW_TYPE_RCS_FILE_TRANSFER_OUT:
                view = mInflater.inflate(mSingleChat ? R.layout.talk_item_rcs_file_transfer_out
                        : R.layout.gchat_item_rcs_file_transfer_out, parent, false);
                view.setTag(new RcsFileTransferOutViewHolder(view, cursor));
                return view;

            case VIEW_TYPE_RCS_GROUP_CHAT_EVENT:
                view = mInflater.inflate(R.layout.groupchat_event_view_item, parent, false);
                view.setTag(new BasicViewHolder(view, cursor));
                return view;
            default:
                throw new IllegalArgumentException("Invalid view type: '" + viewType + "'!");
        }
    }

    private void bindRemoteContact(View view, Context ctx, Cursor cursor) {
        BasicViewHolder holder = (BasicViewHolder) view.getTag();
        String displayName = null;
        if (Direction.OUTGOING != Direction.valueOf(cursor.getInt(holder.getColumnDirectionIdx()))) {
            String number = cursor.getString(holder.getColumnContactIdx());
            if (number != null) {
                ContactId contact = ContactUtil.formatContact(number);
                if (mContactIdDisplayNameMap.containsKey(contact)) {
                    displayName = mContactIdDisplayNameMap.get(contact);
                } else {
                    displayName = RcsContactUtil.getInstance(ctx).getDisplayName(contact);
                    mContactIdDisplayNameMap.put(contact, displayName);
                }
            }
            holder.getContactText().setText(displayName);
        }
    }

    @Override
    public void bindView(View view, Context ctx, Cursor cursor) {
        if (!mSingleChat) {
            bindRemoteContact(view, ctx, cursor);
        }
        int viewType = getItemViewType(cursor);
        switch (viewType) {
            case VIEW_TYPE_RCS_CHAT_IN:
                bindRcsChatInView(view, cursor);
                break;
            case VIEW_TYPE_RCS_CHAT_OUT:
                bindRcsChatOutView(view, cursor);
                break;
            case VIEW_TYPE_RCS_FILE_TRANSFER_IN:
                bindRcsFileTransferInView(view, cursor);
                break;
            case VIEW_TYPE_RCS_FILE_TRANSFER_OUT:
                bindRcsFileTransferOutView(view, cursor);
                break;
            case VIEW_TYPE_RCS_GROUP_CHAT_EVENT:
                bindRcsGroupChatEvent(view, cursor);
                break;
            default:
                throw new IllegalArgumentException("Invalid view type: '" + viewType + "'!");
        }
    }

    public int getItemViewType(Cursor cursor) {
        int providerId = cursor.getInt(cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID));
        Direction direction = Direction.valueOf(cursor.getInt(cursor
                .getColumnIndexOrThrow(HistoryLog.DIRECTION)));
        String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(HistoryLog.MIME_TYPE));
        switch (providerId) {
            case ChatLog.Message.HISTORYLOG_MEMBER_ID:
                switch (mimeType) {
                    case ChatLog.Message.MimeType.GROUPCHAT_EVENT:
                        return VIEW_TYPE_RCS_GROUP_CHAT_EVENT;

                    case ChatLog.Message.MimeType.GEOLOC_MESSAGE:
                    case ChatLog.Message.MimeType.TEXT_MESSAGE:
                        if (Direction.INCOMING == direction) {
                            return VIEW_TYPE_RCS_CHAT_IN;
                        }
                        return VIEW_TYPE_RCS_CHAT_OUT;
                }
                throw new IllegalArgumentException("Invalid mime type: '" + mimeType + "'!");

            case FileTransferLog.HISTORYLOG_MEMBER_ID:
                if (Direction.INCOMING == direction) {
                    return VIEW_TYPE_RCS_FILE_TRANSFER_IN;
                }
                return VIEW_TYPE_RCS_FILE_TRANSFER_OUT;
        }
        throw new IllegalArgumentException("Invalid provider ID: '" + providerId + "'!");
    }

    @Override
    public int getItemViewType(int position) {
        return getItemViewType((Cursor) getItem(position));
    }

    @Override
    public int getViewTypeCount() {
        return 5;
    }

    private void bindRcsGroupChatEvent(View view, Cursor cursor) {
        BasicViewHolder holder = (BasicViewHolder) view.getTag();
        holder.getTimestampText().setText(
                DateUtils.getRelativeTimeSpanString(cursor.getLong(holder.getColumnTimestampIdx()),
                        System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE));
        String event = RiApplication.sGroupChatEvents[cursor.getInt(holder.getColumnStatusIdx())];
        holder.getStatusText().setText(mActivity.getString(R.string.label_groupchat_event, event));
    }

    private void bindRcsFileTransferOutView(View view, Cursor cursor) {
        RcsFileTransferOutViewHolder holder = (RcsFileTransferOutViewHolder) view.getTag();
        holder.getTimestampText().setText(
                DateUtils.getRelativeTimeSpanString(cursor.getLong(holder.getColumnTimestampIdx()),
                        System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE));
        String mimeType = cursor.getString(holder.getColumnMimetypeIdx());
        StringBuilder sb = new StringBuilder(cursor.getString(holder.getColumnFilenameIdx()));
        long filesize = cursor.getLong(holder.getColumnFilesizeIdx());
        long transferred = cursor.getLong(holder.getColumnTransferredIdx());
        final ImageView imageView = holder.getFileImageView();
        imageView.setOnClickListener(null);
        imageView.setLayoutParams(mImageParamsDefault);
        imageView.setImageResource(R.drawable.ri_filetransfer_on);
        if (filesize != transferred) {
            holder.getProgressText().setText(
                    sb.append(" : ").append(Utils.getProgressLabel(transferred, filesize))
                            .toString());
        } else {
            holder.getProgressText().setText(
                    sb.append(" (").append(FileUtils.humanReadableByteCount(filesize, true))
                            .append(")").toString());
        }
        final Uri file = Uri.parse(cursor.getString(holder.getColumnContentIdx()));
        if (Utils.isImageType(mimeType)) {
            String filePath = FileUtils.getPath(mContext, file);
            Bitmap imageBitmap = null;
            if (filePath != null) {
                LruCache<String, BitmapCacheInfo> memoryCache = bitmapCache.getMemoryCache();
                BitmapCacheInfo bitmapCacheInfo = memoryCache.get(filePath);
                if (bitmapCacheInfo == null) {
                    ImageBitmapLoader loader = new ImageBitmapLoader(mContext, memoryCache,
                            MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT, new BitmapLoader.SetViewCallback() {
                                @Override
                                public void loadView(BitmapCacheInfo cacheInfo) {
                                    imageView.setImageBitmap(cacheInfo.getBitmap());
                                    imageView.setLayoutParams(mImageParams);
                                }
                            });
                    loader.execute(filePath);
                } else {
                    imageBitmap = bitmapCacheInfo.getBitmap();
                }
                if (imageBitmap != null) {
                    imageView.setImageBitmap(imageBitmap);
                    imageView.setLayoutParams(mImageParams);
                }
                imageView.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Utils.showPicture(mActivity, file);
                    }
                });
            }
        } else if (Utils.isAudioType(mimeType)) {
            imageView.setImageResource(R.drawable.headphone);
            imageView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Utils.playAudio(mActivity, file);
                }
            });
        }
        holder.getStatusText().setText(getRcsFileTransferStatus(cursor, holder));
        boolean undeliveredExpiration = cursor.getInt(holder.getColumnExpiredDeliveryIdx()) == 1;
        holder.getStatusText().setCompoundDrawablesWithIntrinsicBounds(
                undeliveredExpiration ? R.drawable.chat_view_undelivered : 0, 0, 0, 0);
    }

    private void bindRcsFileTransferInView(View view, Cursor cursor) {
        final RcsFileTransferInViewHolder holder = (RcsFileTransferInViewHolder) view.getTag();
        holder.getTimestampText().setText(
                DateUtils.getRelativeTimeSpanString(cursor.getLong(holder.getColumnTimestampIdx()),
                        System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE));
        String mimeType = cursor.getString(holder.getColumnMimetypeIdx());
        StringBuilder sb = new StringBuilder(cursor.getString(holder.getColumnFilenameIdx()));
        long filesize = cursor.getLong(holder.getColumnFilesizeIdx());
        long transferred = cursor.getLong(holder.getColumnTransferredIdx());
        final ImageView imageView = holder.getFileImageView();
        imageView.setOnClickListener(null);
        imageView.setLayoutParams(mImageParamsDefault);
        imageView.setImageResource(R.drawable.ri_filetransfer_off);
        if (filesize != transferred) {
            holder.getProgressText().setText(
                    sb.append(" : ").append(Utils.getProgressLabel(transferred, filesize))
                            .toString());
        } else {
            imageView.setImageResource(R.drawable.ri_filetransfer_on);
            final Uri file = Uri.parse(cursor.getString(holder.getColumnContentIdx()));
            final RcsService.ReadStatus readStatus = RcsService.ReadStatus.valueOf(cursor
                    .getInt(holder.getColumnReadStatusIdx()));
            final String id = cursor.getString(cursor.getColumnIndexOrThrow(HistoryLog.ID));
            if (Utils.isImageType(mimeType)) {
                final String filePath = FileUtils.getPath(mContext, file);
                if (filePath != null) {
                    LruCache<String, BitmapCacheInfo> memoryCache = bitmapCache.getMemoryCache();
                    BitmapCacheInfo bitmapCacheInfo = memoryCache.get(filePath);
                    if (bitmapCacheInfo == null) {
                        ImageBitmapLoader loader = new ImageBitmapLoader(mContext, memoryCache,
                                MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT,
                                new BitmapLoader.SetViewCallback() {
                                    @Override
                                    public void loadView(BitmapCacheInfo cacheInfo) {
                                        imageView.setImageBitmap(cacheInfo.getBitmap());
                                        imageView.setLayoutParams(mImageParams);
                                    }
                                });
                        loader.execute(filePath);
                    } else {
                        Bitmap imageBitmap = bitmapCacheInfo.getBitmap();
                        imageView.setImageBitmap(imageBitmap);
                        imageView.setLayoutParams(mImageParams);
                    }
                    imageView.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            Utils.showPicture(mActivity, file);
                            markFileTransferAsRead(id, readStatus);
                        }
                    });
                }
            } else if (Utils.isAudioType(mimeType)) {
                imageView.setImageResource(R.drawable.headphone);
                imageView.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        Utils.playAudio(mActivity, file);
                        markFileTransferAsRead(id, readStatus);
                    }
                });
            }
            holder.getProgressText().setText(
                    sb.append(" (").append(FileUtils.humanReadableByteCount(filesize, true))
                            .append(")").toString());
        }
        holder.getStatusText().setText(getRcsFileTransferStatus(cursor, holder));
    }

    private void markFileTransferAsRead(String ftId, RcsService.ReadStatus readStatus) {
        try {
            if (RcsService.ReadStatus.UNREAD == readStatus) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Mark file transfer " + ftId + " as read");
                }
                mFileTransferService.markFileTransferAsRead(ftId);
            }
        } catch (RcsServiceNotAvailableException e) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "Cannot mark message as read: service not available");
            }
        } catch (RcsGenericException | RcsPersistentStorageException e) {
            Log.e(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        }
    }

    private RcsChatInViewHolder bindRcsChatView(View view, Cursor cursor) {
        RcsChatInViewHolder holder = (RcsChatInViewHolder) view.getTag();
        holder.getTimestampText().setText(
                DateUtils.getRelativeTimeSpanString(cursor.getLong(holder.getColumnTimestampIdx()),
                        System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE));
        String mimeType = cursor.getString(holder.getColumnMimetypeIdx());
        TextView contentText = holder.getContentText();
        String data = cursor.getString(holder.getColumnContentIdx());
        if (MimeType.TEXT_MESSAGE.equals(mimeType)) {
            contentText.setText(formatMessageWithSmiley(data));
        } else {
            contentText.setText(formatGeolocation(mContext, new Geoloc(data)));
        }
        holder.getStatusText().setText(getRcsChatStatus(cursor, holder));
        return holder;
    }

    private void bindRcsChatOutView(View view, Cursor cursor) {
        RcsChatOutViewHolder holder = (RcsChatOutViewHolder) bindRcsChatView(view, cursor);
        boolean undeliveredExpiration = cursor.getInt(holder.getColumnExpiredDeliveryIdx()) == 1;
        holder.getStatusText().setCompoundDrawablesWithIntrinsicBounds(
                undeliveredExpiration ? R.drawable.chat_view_undelivered : 0, 0, 0, 0);
    }

    private void bindRcsChatInView(View view, Cursor cursor) {
        RcsChatInViewHolder holder = bindRcsChatView(view, cursor);
        // Only mark message as read when actually displayed on screen
        markChatMessageAsRead(cursor, holder);
    }

    private void markChatMessageAsRead(Cursor cursor, RcsChatInViewHolder holder) {
        try {
            RcsService.ReadStatus readStatus = RcsService.ReadStatus.valueOf(cursor.getInt(holder
                    .getColumnReadStatusIdx()));
            if (RcsService.ReadStatus.UNREAD == readStatus) {
                String msgId = cursor.getString(holder.getColumnIdIdx());
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Mark message " + msgId + " as read");
                }
                mChatService.markMessageAsRead(msgId);
            }
        } catch (RcsServiceNotAvailableException e) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG, "Cannot mark message as read: service not available");
            }
        } catch (RcsGenericException | RcsPersistentStorageException e) {
            Log.e(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        }
    }

    private String getRcsFileTransferStatus(Cursor cursor, RcsFileTransferInViewHolder holder) {
        FileTransfer.State state = FileTransfer.State.valueOf((int) cursor.getLong(holder
                .getColumnStatusIdx()));
        StringBuilder status = new StringBuilder(RiApplication.sFileTransferStates[state.toInt()]);
        ReasonCode reason = ReasonCode
                .valueOf((int) cursor.getLong(holder.getColumnReasonCodeIdx()));
        if (ReasonCode.UNSPECIFIED != reason) {
            status.append(" / ");
            status.append(RiApplication.sFileTransferReasonCodes[reason.toInt()]);
        }
        return status.toString();
    }

    private String getRcsChatStatus(Cursor cursor, RcsChatInViewHolder holder) {
        ChatLog.Message.Content.Status state = ChatLog.Message.Content.Status.valueOf((int) cursor
                .getLong(holder.getColumnStatusIdx()));
        StringBuilder status = new StringBuilder(RiApplication.sMessagesStatuses[state.toInt()]);
        Content.ReasonCode reason = Content.ReasonCode.valueOf((int) cursor.getLong(holder
                .getColumnReasonCodeIdx()));
        if (Content.ReasonCode.UNSPECIFIED != reason) {
            status.append(" / ");
            status.append(RiApplication.sMessageReasonCodes[reason.toInt()]);
        }
        return status.toString();
    }

    /**
     * Format geolocation
     *
     * @param context context
     * @param geoloc The geolocation
     * @return a formatted text
     */
    public static String formatGeolocation(Context context, Geoloc geoloc) {
        StringBuilder result = new StringBuilder(context.getString(R.string.label_geolocation_msg))
                .append("\n");
        String label = geoloc.getLabel();
        if (label != null) {
            result.append(context.getString(R.string.label_location)).append(" ").append(label)
                    .append("\n");
        }
        return result.append(context.getString(R.string.label_latitude)).append(" ")
                .append(geoloc.getLatitude()).append("\n")
                .append(context.getString(R.string.label_longitude)).append(" ")
                .append(geoloc.getLongitude()).append("\n")
                .append(context.getString(R.string.label_accuracy)).append(" ")
                .append(geoloc.getAccuracy()).toString();
    }

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
