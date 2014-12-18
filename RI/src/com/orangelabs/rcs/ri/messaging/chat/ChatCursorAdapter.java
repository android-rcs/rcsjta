/**
 * 
 */
package com.orangelabs.rcs.ri.messaging.chat;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gsma.services.rcs.RcsCommon;
import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.contacts.ContactUtils;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.SmileyParser;
import com.orangelabs.rcs.ri.utils.Smileys;

/**
 * @author YPLO6403
 *
 */
public class ChatCursorAdapter extends CursorAdapter {
	
	private boolean mIsSingleChat = true;
	
	private LayoutInflater mInflater;
	
	private final static SimpleDateFormat df = new SimpleDateFormat("yy-MM-dd HH:mm:ss");
	
	private ContactUtils mContactUtils;
	
	/**
	 * A map between contact and display name to minimize queries of RCS settings provider
	 */
	private Map<ContactId, String> mContactIdDisplayNameMap = new HashMap<ContactId, String>();
	
	private Context mContext;
	
	/**
	 * Smileys
	 */
	private Smileys mSmileyResources;

	/**
	 * Constructor
	 * 
	 * @param context
	 * @param cursor
	 * @param flags
	 * @param isSingleChat
	 */
	public ChatCursorAdapter(Context context, Cursor cursor, int flags, boolean isSingleChat) {
		super(context, cursor, flags);
		mInflater = LayoutInflater.from(context);
		mIsSingleChat = isSingleChat;
		mContactUtils = ContactUtils.getInstance(context);
		// Smiley resources
		mSmileyResources = new Smileys(context);
		mContext = context;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		final View view = mInflater.inflate(R.layout.chat_view_item, parent, false);
		view.setTag(new ViewHolder(view, cursor));
		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		final ViewHolder holder = (ViewHolder) view.getTag();

		// Set the type of message
		int direction = cursor.getInt(holder.columnDirection);
		// Set the date/time field
		long date = cursor.getLong(holder.columnTimestamp);
		// Set the status text
		int status = cursor.getInt(holder.columnMessageStatus);
		// Set image if any
		String data = cursor.getString(holder.columnContent);
		// Set display name
		String displayName = null;
		if (!mIsSingleChat && direction != RcsCommon.Direction.OUTGOING) {
			String number = cursor.getString(holder.columnContact);
			if (number != null) {
				try {
					ContactId contact = mContactUtils.formatContact(number);
					if (!mContactIdDisplayNameMap.containsKey(contact)) {
						// Display name is not known, save it into map
						displayName = RcsDisplayName.getInstance(context).getDisplayName(contact);
						mContactIdDisplayNameMap.put(contact, displayName);
					} else {
						displayName = mContactIdDisplayNameMap.get(contact);
					}
				} catch (RcsContactFormatException e) {
					displayName = number;
				}
			}
		}

		String mimeType = cursor.getString(holder.columnMimetype);
		holder.statusText.setText(RiApplication.MESSAGE_STATUSES[status % RiApplication.MESSAGE_STATUSES.length]);

		holder.dateText.setText(df.format(date));

		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		// Retrieve layout elements
		switch (direction) {
		case RcsCommon.Direction.OUTGOING:
			lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			// Set background
			holder.chatItemLayout.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.msg_item_left));
			holder.chatText.setText(formatDataToText(context, mimeType, data));
			holder.contactText.setVisibility(View.GONE);
			holder.statusText.setVisibility(View.VISIBLE);
			break;
		case RcsCommon.Direction.INCOMING:
			lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			// Set background
			holder.chatItemLayout.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.msg_item_right));
			holder.chatText.setText(formatDataToText(context, mimeType, data));
			if (displayName != null) {
				holder.contactText.setVisibility(View.VISIBLE);
				holder.contactText.setText(displayName);
			} else {
				holder.contactText.setVisibility(View.GONE);
			}
			holder.statusText.setVisibility(View.VISIBLE);
			break;
		case RcsCommon.Direction.IRRELEVANT:
			if (ChatLog.Message.MimeType.GROUPCHAT_EVENT.equals(mimeType)) {
				lp.addRule(RelativeLayout.CENTER_IN_PARENT);
				holder.chatItemLayout.setBackgroundDrawable(null);
				String event = RiApplication.GROUP_CHAT_EVENTS[status % RiApplication.GROUP_CHAT_EVENTS.length];
				holder.chatText.setText(context.getString(R.string.label_groupchat_event, event));
				if (displayName != null) {
					holder.contactText.setVisibility(View.VISIBLE);
					holder.contactText.setText(displayName);
				} else {
					holder.contactText.setVisibility(View.GONE);
				}
				holder.statusText.setVisibility(View.INVISIBLE);
			}
			break;
		default:
		}
		holder.chatItemLayout.setLayoutParams(lp);
	}

	/**
	 * Format data to text
	 * 
	 * @param context
	 * @param mimeType
	 * @param data
	 * @return a formatted text
	 */
	private CharSequence formatDataToText(Context context, String mimeType, String data) {
		if (ChatLog.Message.MimeType.TEXT_MESSAGE.equals(mimeType)) {
			return formatMessageWithSmiley(data);
		}
		if (ChatLog.Message.MimeType.GEOLOC_MESSAGE.equals(mimeType)) {
			Geoloc geoloc = ChatLog.getGeoloc(data);
			if (geoloc == null) {
				return null;
			}
			return new StringBuilder(context.getString(R.string.label_geolocation_msg)).append("\n")
					.append(context.getString(R.string.label_location)).append(" ").append(geoloc.getLabel()).append("\n")
					.append(context.getString(R.string.label_latitude)).append(" ").append(geoloc.getLatitude()).append("\n")
					.append(context.getString(R.string.label_longitude)).append(" ").append(geoloc.getLongitude()).append("\n")
					.append(context.getString(R.string.label_accuracy)).append(" ").append(geoloc.getAccuracy()).toString();
		}
		return null;
	}

	/**
	 * A ViewHolder class keeps references to children views to avoid unnecessary calls to findViewById() or getColumnIndex() on
	 * each row.
	 */
	private class ViewHolder {
		RelativeLayout chatItemLayout;
		TextView chatText;
		TextView statusText;
		TextView dateText;
		TextView contactText;
		int columnDirection;
		int columnTimestamp;
		int columnContent;
		int columnMessageStatus;
		int columnContact;
		int columnMimetype;

		/**
		 * Constructor
		 * @param base
		 * @param cursor
		 */
		ViewHolder(View base, Cursor cursor) {
			// Save column indexes
			columnDirection = cursor.getColumnIndex(ChatLog.Message.DIRECTION);
			columnTimestamp = cursor.getColumnIndex(ChatLog.Message.TIMESTAMP);
			columnContent = cursor.getColumnIndex(ChatLog.Message.CONTENT);
			columnMessageStatus = cursor.getColumnIndex(ChatLog.Message.STATUS);
			columnContact = cursor.getColumnIndex(ChatLog.Message.CONTACT);
			columnMimetype = cursor.getColumnIndex(ChatLog.Message.MIME_TYPE);
			// Save children views
			chatItemLayout = (RelativeLayout) base.findViewById(R.id.msg_item);
			chatText = (TextView) base.findViewById(R.id.chat_text);
			statusText = (TextView) base.findViewById(R.id.status_text);
			dateText = (TextView) base.findViewById(R.id.date_text);
			contactText = (TextView) base.findViewById(R.id.contact_text);
		}

	}

	/**
	 * Format text with smiley
	 * 
	 * @param txt
	 *            Text
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
