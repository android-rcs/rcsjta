/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.orangelabs.rcs.provider.messaging;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * This class interfaces the chat table
 *
 */
public class GroupChatLog implements IGroupChatLog {

	/**
	 * Chat database URI
	 */
	private Uri chatDatabaseUri = ChatData.CONTENT_URI;

	private Context context;
	private ContentResolver cr;
	
	private final static String SELECT_CHAT_ID = new StringBuilder(ChatData.KEY_CHAT_ID).append("=?").toString();

	/* TODO: This constant should not use a magic number. */
	private final static String SELECT_CHAT_ID_STATUS_REJECTED = new StringBuilder(
			ChatData.KEY_CHAT_ID).append("=? AND ").append(ChatData.KEY_STATE).append("=")
			.append(GroupChat.State.ABORTED).append(" AND ").append(ChatData.KEY_REASON_CODE)
			.append("=").append(GroupChat.ReasonCode.ABORTED_BY_USER).append(" AND ")
			.append(ChatData.KEY_REJECT_GC).append("=1").toString();

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(GroupChatLog.class.getSimpleName());

	/**
	 * Constructor
	 * 
	 * @param cr
	 *            Content resolver
	 */
	/* package private */GroupChatLog(Context context) {
		this.context = context;
		this.cr = context.getContentResolver();
	}

	/**
	 * Convert a set of ParticipantInfo into a string
	 * 
	 * @param participants
	 *            the participant information
	 * @return the string with comma separated values of key pairs formatted as follows: "key=value"
	 */
	private static String writeParticipantInfo(Set<ParticipantInfo> participants) {
		if (participants == null || participants.size() == 0) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		int size = participants.size();
		for (ParticipantInfo participant : participants) {
			// set key
			sb.append(participant.getContact());
			sb.append('=');
			// set value
			sb.append(participant.getStatus());
			if (--size != 0) {
				// Not last item : add separator
				sb.append(',');
			}
		}
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#addGroupChat(java.lang.String, java.lang.String, java.util.Set,
	 * int, int)
	 */
	public void addGroupChat(String chatId, String subject, Set<ParticipantInfo> participants,
			int state, int reasonCode, int direction) {
		if (logger.isActivated()) {
			logger.debug("addGroupChat (chatID=" + chatId + ") (subject=" + subject + ") (status="
					+ state + ") (reasonCode=" + reasonCode + ") (dir=" + direction					+ ")");
		}
		ContentValues values = new ContentValues();
		values.put(ChatData.KEY_CHAT_ID, chatId);
		values.put(ChatData.KEY_STATE, state);
		values.put(ChatData.KEY_REASON_CODE, reasonCode);
		values.put(ChatData.KEY_SUBJECT, subject);
		values.put(ChatData.KEY_PARTICIPANTS, writeParticipantInfo(participants));
		values.put(ChatData.KEY_DIRECTION, direction);
		values.put(ChatData.KEY_TIMESTAMP, Calendar.getInstance().getTimeInMillis());
		cr.insert(chatDatabaseUri, values);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#acceptGroupChatNextInvitation(java.lang.String)
	 */
	@Override
	public void acceptGroupChatNextInvitation(String chatId) {
		if (logger.isActivated()) {
			logger.debug("acceptGroupChatNextInvitation (chatId=" + chatId + ")");
		}
		ContentValues values = new ContentValues();
		values.put(ChatData.KEY_REJECT_GC, "0");
		String[] selectionArgs = { chatId };
		cr.update(chatDatabaseUri, values, SELECT_CHAT_ID_STATUS_REJECTED, selectionArgs);
		if (logger.isActivated()) {
			logger.debug("acceptGroupChatNextInvitation (chatID=" + chatId + ")");
		}
	}

	@Override
	public void updateGroupChatStateAndReasonCode(String chatId, int state, int reasonCode) {
		if (logger.isActivated()) {
			logger.debug("updateGroupChatStatus (chatId=" + chatId + ") (state=" + state
					+ ") (reasonCode=" + reasonCode + ")");
		}
		ContentValues values = new ContentValues();
		values.put(ChatData.KEY_STATE, state);
		values.put(ChatData.KEY_REASON_CODE, reasonCode);
		String selectionArgs[] = new String[] {
			chatId
		};
		cr.update(chatDatabaseUri, values, SELECT_CHAT_ID, selectionArgs);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#updateGroupChatParticipant(java.lang.String, java.util.Set)
	 */
	@Override
	public void updateGroupChatParticipant(String chatId, Set<ParticipantInfo> participants) {
		String encodedParticipants = writeParticipantInfo(participants);
		if (logger.isActivated()) {
			logger.debug("updateGroupChatParticipant (chatId=" + chatId + ") (participants=" + encodedParticipants + ")");
		}
		ContentValues values = new ContentValues();
		values.put(ChatData.KEY_PARTICIPANTS, encodedParticipants);
		cr.update(chatDatabaseUri, values, ChatData.KEY_CHAT_ID + " = '" + chatId + "'", null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#updateGroupChatRejoinIdOnSessionStart(java.lang.String, java.lang.String)
	 */
	@Override
	public void updateGroupChatRejoinIdOnSessionStart(String chatId, String rejoinId) {
		if (logger.isActivated()) {
			logger.debug("Update group chat rejoin ID to " + rejoinId);
		}
		ContentValues values = new ContentValues();
		values.put(ChatData.KEY_REJOIN_ID, rejoinId);
		values.put(ChatData.KEY_STATE, GroupChat.State.STARTED);
		cr.update(chatDatabaseUri, values, ChatData.KEY_CHAT_ID + " = '" + chatId + "'", null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#getGroupChatInfo(java.lang.String)
	 */
	@Override
	public GroupChatInfo getGroupChatInfo(String chatId) {
		if (logger.isActivated()) {
			logger.debug("Get group chat info for " + chatId);
		}
		GroupChatInfo result = null;
		Cursor cursor = null;

		// @formatter:off
		String[] projection = new String[] { ChatData.KEY_CHAT_ID, ChatData.KEY_REJOIN_ID, ChatData.KEY_PARTICIPANTS,
				ChatData.KEY_SUBJECT };
		// @formatter:on
		String[] selArgs = new String[] { chatId };
		try {
			cursor = cr.query(chatDatabaseUri, projection, SELECT_CHAT_ID, selArgs, ChatData.KEY_TIMESTAMP + " DESC");
			if (cursor.moveToFirst()) {
				// Decode list of participants
				Set<ParticipantInfo> participants = ChatLog.GroupChat.getParticipantInfo(context, cursor.getString(2));
				result = new GroupChatInfo(cursor.getString(0), cursor.getString(1), chatId, participants, cursor.getString(3));
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#getGroupChatConnectedParticipants(java.lang.String)
	 */
	@Override
	public Set<ParticipantInfo> getGroupChatConnectedParticipants(String chatId) {
		if (logger.isActivated()) {
			logger.debug("Get connected participants for " + chatId);
		}
		Set<ParticipantInfo> result = new HashSet<ParticipantInfo>();
		String[] projection = new String[] { ChatData.KEY_PARTICIPANTS };
		String[] selArgs = new String[] { chatId };
		Cursor cursor = null;
		try {
			cursor = cr.query(chatDatabaseUri, projection, SELECT_CHAT_ID, selArgs, ChatData.KEY_TIMESTAMP + " DESC");
			if (cursor.moveToFirst()) {
				// Decode list of participants
				Set<ParticipantInfo> participants = ChatLog.GroupChat.getParticipantInfo(context, cursor.getString(0));
				if (participants != null) {
					for (ParticipantInfo participantInfo : participants) {
						// Only consider participants who have not declined or left GC
						switch (participantInfo.getStatus()) {
						case ParticipantInfo.Status.DEPARTED:
						case ParticipantInfo.Status.DECLINED:
							break;
						default:
							result.add(participantInfo);
						}
					}
				}
			}
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Exception",e);
			}
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#isGroupChatNextInviteRejected(java.lang.String)
	 */
	@Override
	public boolean isGroupChatNextInviteRejected(String chatId) {
		String[] projection = { ChatData.KEY_CHAT_ID };
		String[] selectionArgs = { chatId };
		Cursor cursor = null;
		try {
			cursor = cr.query(chatDatabaseUri, projection, SELECT_CHAT_ID_STATUS_REJECTED, selectionArgs, ChatData.KEY_TIMESTAMP + " DESC");
			if (cursor.getCount() != 0) {
				return true;
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return false;
	}
}
