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

package com.orangelabs.rcs.provider.eventlogs;

import com.orangelabs.rcs.provider.RichProviderHelper;
import com.orangelabs.rcs.provider.messaging.RichMessagingData;
import com.orangelabs.rcs.provider.messaging.RichMessagingProvider;
import com.orangelabs.rcs.provider.sharing.RichCallData;
import com.orangelabs.rcs.provider.sharing.RichCallProvider;
import com.orangelabs.rcs.service.api.client.eventslog.EventsLogApi;
import com.orangelabs.rcs.utils.MimeManager;
import com.orangelabs.rcs.utils.PhoneUtils;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Virtual provider that enables EventLogs queries to be done in Core with access to SQLiteDatabase object.
 *
 * Events are from : SMS, MMS, File Transfer, Chat, Content Sharing tables
 *
 * @author mhsm6403
 */
public class EventLogProvider extends ContentProvider {

	private SQLiteOpenHelper openHelper;
	/**
	 * The uriMatcher that define all cases to be treated. Requests are not made on an unique table in an unique database so we define Uris to implement 
	 * each cases of filters.
	 */
	private static final UriMatcher uriMatcher;
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI("com.orangelabs.rcs.eventlogs", Integer.toString(EventsLogApi.MODE_SPAM_BOX), EventsLogApi.MODE_SPAM_BOX);
		uriMatcher.addURI("com.orangelabs.rcs.eventlogs", Integer.toString(EventsLogApi.MODE_ONE_TO_ONE_CHAT), EventsLogApi.MODE_ONE_TO_ONE_CHAT);
		uriMatcher.addURI("com.orangelabs.rcs.eventlogs", Integer.toString(EventsLogApi.MODE_GROUP_CHAT), EventsLogApi.MODE_GROUP_CHAT);
		uriMatcher.addURI("com.orangelabs.rcs.eventlogs", Integer.toString(EventsLogApi.MODE_RC_CHAT_FT_SMS), EventsLogApi.MODE_RC_CHAT_FT_SMS);
		uriMatcher.addURI("com.orangelabs.rcs.eventlogs", Integer.toString(EventsLogApi.MODE_RC_CHAT_FT), EventsLogApi.MODE_RC_CHAT_FT);
		uriMatcher.addURI("com.orangelabs.rcs.eventlogs", Integer.toString(EventsLogApi.MODE_RC_CHAT_SMS), EventsLogApi.MODE_RC_CHAT_SMS);
		uriMatcher.addURI("com.orangelabs.rcs.eventlogs", Integer.toString(EventsLogApi.MODE_RC_CHAT), EventsLogApi.MODE_RC_CHAT);
		uriMatcher.addURI("com.orangelabs.rcs.eventlogs", Integer.toString(EventsLogApi.MODE_RC_FT_SMS), EventsLogApi.MODE_RC_FT_SMS);
		uriMatcher.addURI("com.orangelabs.rcs.eventlogs", Integer.toString(EventsLogApi.MODE_RC_FT), EventsLogApi.MODE_RC_FT);
		uriMatcher.addURI("com.orangelabs.rcs.eventlogs", Integer.toString(EventsLogApi.MODE_RC_SMS), EventsLogApi.MODE_RC_SMS);
		uriMatcher.addURI("com.orangelabs.rcs.eventlogs", Integer.toString(EventsLogApi.MODE_RC), EventsLogApi.MODE_RC);
		uriMatcher.addURI("com.orangelabs.rcs.eventlogs", Integer.toString(EventsLogApi.MODE_CHAT_FT_SMS), EventsLogApi.MODE_CHAT_FT_SMS);
		uriMatcher.addURI("com.orangelabs.rcs.eventlogs", Integer.toString(EventsLogApi.MODE_CHAT_FT), EventsLogApi.MODE_CHAT_FT);
		uriMatcher.addURI("com.orangelabs.rcs.eventlogs", Integer.toString(EventsLogApi.MODE_CHAT_SMS), EventsLogApi.MODE_CHAT_SMS);
		uriMatcher.addURI("com.orangelabs.rcs.eventlogs", Integer.toString(EventsLogApi.MODE_CHAT), EventsLogApi.MODE_CHAT);
		uriMatcher.addURI("com.orangelabs.rcs.eventlogs", Integer.toString(EventsLogApi.MODE_FT_SMS), EventsLogApi.MODE_FT_SMS);
		uriMatcher.addURI("com.orangelabs.rcs.eventlogs", Integer.toString(EventsLogApi.MODE_FT), EventsLogApi.MODE_FT);
		uriMatcher.addURI("com.orangelabs.rcs.eventlogs", Integer.toString(EventsLogApi.MODE_SMS), EventsLogApi.MODE_SMS);
		uriMatcher.addURI("com.orangelabs.rcs.eventlogs", Integer.toString(EventsLogApi.MODE_NONE), EventsLogApi.MODE_NONE);
	}
	
	@Override 
	public boolean onCreate() {
		if(RichProviderHelper.getInstance()==null){
        	RichProviderHelper.createInstance(getContext());
        }
        this.openHelper = RichProviderHelper.getInstance();
        return true;
	}
	
	/**
	 * Not used
	 */
	@Override
	public String getType(Uri uri) {
		return null;
	}

	/**
	 * Not used
	 */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}
	
	/**
	 * Build a Date sorted Cursor which contains event logs of the mobile for a specific contact.
	 *
	 * This method is only called with uris from uriMatcher.
	 * Each one defines a state of the eventlog filter and therefore aggregates events of selected types. 
	 *
	 * selection parameter is specially build : " IN ('phonenumber1','phonenumber2'...)" Must not be null
	 * sortOrder must be (EventLogData.KEY_EVENT_SESSION_ID+ " DESC , "+EventLogData.KEY_EVENT_DATE + " DESC ")
	 */
	@Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrderOriginal) {
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		SQLiteDatabase db = openHelper.getWritableDatabase();
		String limit = null;
		String sortOrder = EventLogData.KEY_EVENT_SESSION_ID + " DESC , " + EventLogData.KEY_EVENT_DATE + " DESC ";
		Cursor sortCursor = null;
		Cursor unionCursor = null;
		Cursor callCursor = null;
		Cursor smsCursor = null;
		String unionQuery = null;
		String richMessagingSelectQuery = null;
		String richCallSelectQuery = null;
		int match = uriMatcher.match(uri);

        switch(match) {

        case EventsLogApi.MODE_SPAM_BOX:
            String extraSelection = "";
            if (selection != null && selection.length() > 0) {
                extraSelection += " AND ";
            } else {
                selection = "";
            }
            // Do not take the "terminated" entries
            extraSelection += "NOT ((type = " + EventsLogApi.TYPE_CHAT_SYSTEM_MESSAGE + ") AND ((status = " +
            	EventsLogApi.STATUS_TERMINATED + " ) OR (status = " + EventsLogApi.STATUS_TERMINATED_BY_REMOTE + ") OR (status = " + EventsLogApi.STATUS_TERMINATED_BY_USER + ")))";

            // Filter the logs where this contact was involved in a group chat with us
            extraSelection += " AND NOT ( type = " + EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE
                    + " OR type = " + EventsLogApi.TYPE_INCOMING_GROUP_CHAT_MESSAGE
                    + " OR type = " + EventsLogApi.TYPE_OUTGOING_GROUP_CHAT_MESSAGE + " )";
            // Take only the spam messages
            extraSelection += " AND ( " + RichMessagingData.KEY_IS_SPAM + "=" + EventsLogApi.MESSAGE_IS_SPAM + " )";

            if (sortOrderOriginal != null && sortOrderOriginal.length() > 0) {
                sortOrder = sortOrderOriginal;
            } else {
                sortOrder = EventLogData.KEY_EVENT_SESSION_ID+ " DESC , "+EventLogData.KEY_EVENT_DATE + " DESC ";
            }

			richMessagingSelectQuery = buildChatQuery(selection + extraSelection, false, true);
			unionQuery = builder.buildUnionQuery(new String[] { richMessagingSelectQuery },sortOrder,limit);
			sortCursor = db.rawQuery(unionQuery, null);
        	break;

        case EventsLogApi.MODE_ONE_TO_ONE_CHAT:
        	// Do not take the "terminated" entries
        	extraSelection=" AND NOT ((type = "+EventsLogApi.TYPE_CHAT_SYSTEM_MESSAGE+") AND ((status = "+
        		EventsLogApi.STATUS_TERMINATED + ") OR (status = " + EventsLogApi.STATUS_TERMINATED_BY_REMOTE + ") OR (status = " + EventsLogApi.STATUS_TERMINATED_BY_USER + ")))";
			// Filter the logs where this contact was involved in a group chat with us
			extraSelection += " AND NOT ( type = "+EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE
				+ " OR type = "+EventsLogApi.TYPE_INCOMING_GROUP_CHAT_MESSAGE
				+ " OR type = "+EventsLogApi.TYPE_OUTGOING_GROUP_CHAT_MESSAGE+	" )";
			// Do not take the spam messages
			extraSelection +=" AND NOT( "+RichMessagingData.KEY_IS_SPAM+"="+EventsLogApi.MESSAGE_IS_SPAM+ " )";

            if (sortOrderOriginal != null) {
                sortOrder = sortOrderOriginal;
            } else {
                sortOrder = EventLogData.KEY_EVENT_SESSION_ID + " ASC , " + EventLogData.KEY_EVENT_DATE + " ASC ";
            }
			richMessagingSelectQuery = buildChatQuery(selection + extraSelection, false, false);
			unionQuery = builder.buildUnionQuery(new String[] { richMessagingSelectQuery },sortOrder,limit);
			sortCursor = db.rawQuery(unionQuery, null);
        	break;

        case EventsLogApi.MODE_GROUP_CHAT:
            extraSelection = " AND NOT type = "+EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE;
			if (sortOrderOriginal != null)
				sortOrder = sortOrderOriginal;
			else
				sortOrder = EventLogData.KEY_EVENT_SESSION_ID+ " ASC , "+EventLogData.KEY_EVENT_DATE + " ASC ";
			richMessagingSelectQuery = buildChatQuery(selection + extraSelection, false, false);
			unionQuery = builder.buildUnionQuery(new String[] { richMessagingSelectQuery },sortOrder,limit);
			sortCursor = db.rawQuery(unionQuery, null);
        	break;

        case EventsLogApi.MODE_NONE:
			/* Query the sms/mms table */
			smsCursor = queryMmsSmsTable(selection, sortOrder);
			/* Query for Rich Messaging */
			richMessagingSelectQuery = buildRichMessagingQuery(selection, false, false);
			/* Query for RichCALL */
			richCallSelectQuery = buildRichCallQuery(selection);
	        unionQuery = builder.buildUnionQuery(new String[] { richMessagingSelectQuery,richCallSelectQuery },sortOrder,limit);
			unionCursor = db.rawQuery(unionQuery, null);
			/* Build a SortCursor with all cursors sorted by Date */
			sortCursor = new SortCursor(new Cursor[]{unionCursor,callCursor,smsCursor},EventLogData.KEY_EVENT_DATE,SortCursor.TYPE_NUMERIC,false);
			break;

        case EventsLogApi.MODE_RC_CHAT_FT_SMS:
    		/* Query the sms/mms table */
			smsCursor = queryMmsSmsTable(selection, sortOrder);
    		/* Query for Rich Messaging */
			richMessagingSelectQuery = buildRichMessagingQuery(selection, false, false);
			/* Query for RichCALL */
			richCallSelectQuery = buildRichCallQuery(selection);
			unionQuery = builder.buildUnionQuery(new String[] { richMessagingSelectQuery,richCallSelectQuery },sortOrder,limit);
			unionCursor = db.rawQuery(unionQuery, null);
			sortCursor = new SortCursor(new Cursor[]{unionCursor,smsCursor},EventLogData.KEY_EVENT_DATE,SortCursor.TYPE_NUMERIC,false);
			break;

        case EventsLogApi.MODE_RC_CHAT_FT:
			/* Query for Rich Messaging */
			richMessagingSelectQuery = buildRichMessagingQuery(selection, false, false);
			/* Query for RichCALL */
			richCallSelectQuery = buildRichCallQuery(selection);
			unionQuery = builder.buildUnionQuery(new String[] { richMessagingSelectQuery,richCallSelectQuery },sortOrder,limit);
			sortCursor = db.rawQuery(unionQuery, null);
			break;

        case EventsLogApi.MODE_RC_CHAT_SMS:
    		/* Query the sms/mms table */
			smsCursor = queryMmsSmsTable(selection, sortOrder);
    		/* Query for Rich Messaging */
			richMessagingSelectQuery = buildRichMessagingQuery(selection, false, true);
			/* Query for RichCALL */
			richCallSelectQuery = buildRichCallQuery(selection);
			unionQuery = builder.buildUnionQuery(new String[] { richMessagingSelectQuery,richCallSelectQuery },sortOrder,limit);
			unionCursor = db.rawQuery(unionQuery, null);
			sortCursor = new SortCursor(new Cursor[]{unionCursor,smsCursor},EventLogData.KEY_EVENT_DATE,SortCursor.TYPE_NUMERIC,false);
			break;

        case EventsLogApi.MODE_RC_CHAT:
    		/* Query for Rich Messaging */
			richMessagingSelectQuery = buildRichMessagingQuery(selection, false, true);
			/* Query for RichCALL */
			richCallSelectQuery = buildRichCallQuery(selection);
			unionQuery = builder.buildUnionQuery(new String[] { richMessagingSelectQuery,richCallSelectQuery },sortOrder,limit);
			sortCursor = db.rawQuery(unionQuery, null);
			break;

        case EventsLogApi.MODE_RC_FT_SMS:
    		/* Query the sms/mms table */
			smsCursor = queryMmsSmsTable(selection, sortOrder);
    		/* Query for Rich Messaging */
			richMessagingSelectQuery = buildRichMessagingQuery(selection, true, false);
			/* Query for RichCALL */
			richCallSelectQuery = buildRichCallQuery(selection);
			unionQuery = builder.buildUnionQuery(new String[] { richMessagingSelectQuery,richCallSelectQuery },sortOrder,limit);
			unionCursor = db.rawQuery(unionQuery, null);
			sortCursor = new SortCursor(new Cursor[]{unionCursor,smsCursor},EventLogData.KEY_EVENT_DATE,SortCursor.TYPE_NUMERIC,false);
			break;

        case EventsLogApi.MODE_RC_FT:
    		/* Query for Rich Messaging */
			richMessagingSelectQuery = buildRichMessagingQuery(selection, true, false);
			/* Query for RichCALL */
			richCallSelectQuery = buildRichCallQuery(selection);
			unionQuery = builder.buildUnionQuery(new String[] { richMessagingSelectQuery,richCallSelectQuery },sortOrder,limit);
			sortCursor = db.rawQuery(unionQuery, null);
			break;

        case EventsLogApi.MODE_RC_SMS:
			/* Query the sms/mms table */
			smsCursor = queryMmsSmsTable(selection, sortOrder);
			/* Query for Rich Messaging */
			richCallSelectQuery = buildRichCallQuery(selection);
			unionQuery = builder.buildUnionQuery(new String[] { richCallSelectQuery },sortOrder,limit);
			unionCursor = db.rawQuery(unionQuery, null);
			sortCursor = new SortCursor(new Cursor[]{unionCursor,smsCursor},EventLogData.KEY_EVENT_DATE,SortCursor.TYPE_NUMERIC,false);
			break;

        case EventsLogApi.MODE_RC:
			/* Query for RichCALL */
			richCallSelectQuery = buildRichCallQuery(selection);
			unionQuery = builder.buildUnionQuery(new String[] { richCallSelectQuery },sortOrder,limit);
			sortCursor = db.rawQuery(unionQuery, null);
			break;

        case EventsLogApi.MODE_CHAT_FT_SMS:
			/* Query the sms/mms table */
			smsCursor = queryMmsSmsTable(selection, sortOrder);
			/* Query for Rich Messaging */
			richMessagingSelectQuery = buildRichMessagingQuery(selection, false, false);
			unionQuery = builder.buildUnionQuery(new String[] { richMessagingSelectQuery },sortOrder,limit);
			unionCursor = db.rawQuery(unionQuery, null);
			sortCursor = new SortCursor(new Cursor[]{unionCursor,smsCursor},EventLogData.KEY_EVENT_DATE,SortCursor.TYPE_NUMERIC,false);
			break;

        case EventsLogApi.MODE_CHAT_FT:
			/* Query for Rich Messaging */
			richMessagingSelectQuery = buildRichMessagingQuery(selection, false, false);
			unionQuery = builder.buildUnionQuery(new String[] { richMessagingSelectQuery },sortOrder,limit);
			sortCursor = db.rawQuery(unionQuery, null);
			break;
 
        case EventsLogApi.MODE_CHAT_SMS:
    		/* Query the sms/mms table */
			smsCursor = queryMmsSmsTable(selection, sortOrder);
			/* Query for Rich Messaging */
			richMessagingSelectQuery = buildRichMessagingQuery(selection, false, true);
			unionQuery = builder.buildUnionQuery(new String[] { richMessagingSelectQuery },sortOrder,limit);
			unionCursor = db.rawQuery(unionQuery, null);
			sortCursor = new SortCursor(new Cursor[]{unionCursor,smsCursor},EventLogData.KEY_EVENT_DATE,SortCursor.TYPE_NUMERIC,false);
			break;

        case EventsLogApi.MODE_CHAT:
    		richMessagingSelectQuery = buildRichMessagingQuery(selection, false, true);
			unionQuery = builder.buildUnionQuery(new String[] { richMessagingSelectQuery },sortOrder,limit);
			sortCursor = db.rawQuery(unionQuery, null);
    		break;

        case EventsLogApi.MODE_FT_SMS:
    		/* Query the sms/mms table */
			smsCursor = queryMmsSmsTable(selection, sortOrder);
			/* Query for Rich Messaging */
			richMessagingSelectQuery = buildRichMessagingQuery(selection, true, false);
			unionQuery = builder.buildUnionQuery(new String[] { richMessagingSelectQuery },sortOrder,limit);
			unionCursor = db.rawQuery(unionQuery, null);
			sortCursor = new SortCursor(new Cursor[]{unionCursor,smsCursor},EventLogData.KEY_EVENT_DATE,SortCursor.TYPE_NUMERIC,false);
			break;

        case EventsLogApi.MODE_FT:
    		richMessagingSelectQuery = buildRichMessagingQuery(selection, true, false);
			unionQuery = builder.buildUnionQuery(new String[] { richMessagingSelectQuery },sortOrder,limit);
			sortCursor = db.rawQuery(unionQuery, null);
    		break;

        case EventsLogApi.MODE_SMS:
			/* Query the sms/mms table */
			sortCursor = queryMmsSmsTable(selection, sortOrder);
			break;

        default:
	        throw new IllegalArgumentException("Unknown URI " + uri);
		}
        
        // Register the contexts ContentResolver to be notified if
		// the cursor result set changes.
        if (sortCursor != null) {
        	sortCursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        
		// Return a cursor to the query result
		return sortCursor;
	}
	
	/**
	 * Not used
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return 0;
	}

	private static String KEY_SMS_ADDRESS = "address";
	private static String KEY_SMS_DATE = "date";
	private static String KEY_SMS_BODY = "body";
	private static String KEY_SMS_TYPE = "type";
	private static String KEY_SMS_STATUS = "status";
		
	private static String[] smsProjection = new String[]{
		BaseColumns._ID+" AS "+EventLogData.KEY_EVENT_ROW_ID,
		"("+EventsLogApi.TYPE_INCOMING_SMS+"-1+"+KEY_SMS_TYPE+")"+ " AS "+ EventLogData.KEY_EVENT_TYPE,
		BaseColumns._ID+" AS "+EventLogData.KEY_EVENT_SESSION_ID,
		KEY_SMS_DATE+ " AS "+ EventLogData.KEY_EVENT_DATE,
		KEY_SMS_ADDRESS+" AS "+EventLogData.KEY_EVENT_CONTACT,
		KEY_SMS_STATUS+ " AS "+ EventLogData.KEY_EVENT_STATUS,
		KEY_SMS_BODY+ " AS "+ EventLogData.KEY_EVENT_DATA,
		
		KEY_SMS_ADDRESS+" AS "+EventLogData.KEY_EVENT_MESSAGE_ID,
		
		"\'"+EventLogData.SMS_MIMETYPE+ "\' AS "+ EventLogData.KEY_EVENT_MIMETYPE,
		
		KEY_SMS_ADDRESS+" AS "+EventLogData.KEY_EVENT_NAME,
		KEY_SMS_ADDRESS+" AS "+EventLogData.KEY_EVENT_SIZE,
		KEY_SMS_ADDRESS+" AS "+EventLogData.KEY_EVENT_TOTAL_SIZE,
		KEY_SMS_ADDRESS+" AS "+EventLogData.KEY_EVENT_IS_SPAM
	};
		
	/* RCS projections */
	private static String [] unionRichMessagingColumns = new String[]{
			RichMessagingData.KEY_ID+" AS "+EventLogData.KEY_EVENT_ROW_ID,
			RichMessagingData.KEY_TYPE+" AS "+EventLogData.KEY_EVENT_TYPE,
			RichMessagingData.KEY_CHAT_SESSION_ID+" AS "+EventLogData.KEY_EVENT_SESSION_ID,
			RichMessagingData.KEY_TIMESTAMP+" AS "+EventLogData.KEY_EVENT_DATE,
			RichMessagingData.KEY_CONTACT+" AS "+EventLogData.KEY_EVENT_CONTACT,
			RichMessagingData.KEY_STATUS+" AS "+EventLogData.KEY_EVENT_STATUS,
			RichMessagingData.KEY_DATA+" AS "+EventLogData.KEY_EVENT_DATA,
			RichMessagingData.KEY_MESSAGE_ID+" AS "+EventLogData.KEY_EVENT_MESSAGE_ID,
			RichMessagingData.KEY_MIME_TYPE+" AS "+EventLogData.KEY_EVENT_MIMETYPE,
			RichMessagingData.KEY_NAME+" AS "+EventLogData.KEY_EVENT_NAME,
			RichMessagingData.KEY_SIZE+" AS "+EventLogData.KEY_EVENT_SIZE,
			RichMessagingData.KEY_TOTAL_SIZE+" AS "+EventLogData.KEY_EVENT_TOTAL_SIZE,
			RichMessagingData.KEY_IS_SPAM+" AS "+EventLogData.KEY_EVENT_IS_SPAM,
            RichMessagingData.KEY_CHAT_ID + " AS " + EventLogData.KEY_EVENT_CHAT_ID
		};
	
	private static Set<String> columnsPresentInRichMessagingTable = new HashSet<String>(Arrays.asList(new String[]{
			// Fields for chat
			RichMessagingData.KEY_ID,
			RichMessagingData.KEY_TYPE,
			RichMessagingData.KEY_CHAT_SESSION_ID,
			RichMessagingData.KEY_TIMESTAMP,
			RichMessagingData.KEY_CONTACT,
			RichMessagingData.KEY_STATUS,
			RichMessagingData.KEY_DATA,
			RichMessagingData.KEY_MESSAGE_ID,
			// Fields for file transfer
			RichMessagingData.KEY_MIME_TYPE,
			RichMessagingData.KEY_NAME,
			RichMessagingData.KEY_SIZE,
			RichMessagingData.KEY_TOTAL_SIZE,
			RichMessagingData.KEY_IS_SPAM,
			// Additional fields for chat
			RichMessagingData.KEY_CHAT_ID
	}));
	
	private static String [] unionRichCallColumns = new String[]{
			RichCallData.KEY_ID+" AS "+EventLogData.KEY_EVENT_ROW_ID,
			"("+EventsLogApi.TYPE_INCOMING_RICH_CALL+"-1+"+RichCallData.KEY_DESTINATION+")"+" AS "+EventLogData.KEY_EVENT_TYPE,  //TODO direction according to EventsLogApi.TYPE_INCOMING_SMS
			RichCallData.KEY_SESSION_ID+" AS "+EventLogData.KEY_EVENT_SESSION_ID,
			RichCallData.KEY_TIMESTAMP+" AS "+EventLogData.KEY_EVENT_DATE,
			RichCallData.KEY_CONTACT+" AS "+EventLogData.KEY_EVENT_CONTACT,
			RichCallData.KEY_STATUS+" AS "+EventLogData.KEY_EVENT_STATUS,
			RichCallData.KEY_DATA+" AS "+EventLogData.KEY_EVENT_DATA,
			RichCallData.KEY_ID+" AS "+EventLogData.KEY_EVENT_MESSAGE_ID,
			RichCallData.KEY_MIME_TYPE+" AS "+EventLogData.KEY_EVENT_MIMETYPE,
			RichCallData.KEY_NAME+" AS "+EventLogData.KEY_EVENT_NAME,
			RichCallData.KEY_SIZE+" AS "+EventLogData.KEY_EVENT_SIZE,
			RichCallData.KEY_SIZE+" AS "+EventLogData.KEY_EVENT_TOTAL_SIZE,
			RichCallData.KEY_SIZE+" AS "+EventLogData.KEY_EVENT_IS_SPAM,
            RichCallData.KEY_SIZE+" AS "+EventLogData.KEY_EVENT_CHAT_ID
	};
	
	private static Set<String> columnsPresentInRichCallTable = new HashSet<String>(Arrays.asList(new String []{
			RichCallData.KEY_ID,
			RichCallData.KEY_ID,
			RichCallData.KEY_SESSION_ID,
			RichCallData.KEY_TIMESTAMP,
			RichCallData.KEY_CONTACT,
			RichCallData.KEY_STATUS,
			RichCallData.KEY_DATA,
			RichCallData.KEY_ID,
			RichCallData.KEY_MIME_TYPE,
			RichCallData.KEY_NAME,
			RichCallData.KEY_SIZE,
			RichCallData.KEY_SIZE,
			RichCallData.KEY_SIZE,
            RichCallData.KEY_SIZE
	}));

	/**
	 *  Query the Android MmsSms table to get all message for the specified Numbers in selection
	 * @param selection
	 * @param sortOrder
	 * @return
	 */
	private Cursor queryMmsSmsTable(String selection, String sortOrder) {
		selection = getThreadIdSelection(selection);

		Cursor smsCursor = getContext().getContentResolver().query(EventLogData.SMS_URI,smsProjection, selection , null, sortOrder);	
		Cursor mmsCursor = getMMSCursor(selection);
		Cursor sortCursor = new SortCursor(new Cursor[]{smsCursor,mmsCursor},EventLogData.KEY_EVENT_DATE,SortCursor.TYPE_NUMERIC,false);	
		return sortCursor;
	}

	private String getThreadIdSelection(String selection){
		/* Unbuild selection */
		selection = selection.substring(5,selection.length()-1);
		String[] numbers;
		StringBuffer buf = new StringBuffer();
		if(selection!=null){
			numbers = selection.split(",");
			/* Get the threadIds of each number for the contacts */
			Uri.Builder builder;
			Cursor cThreadId;
			List<Integer> threadIds = new ArrayList<Integer>();
			for(int i = 0; i < numbers.length; i++){
				builder = new Builder();
				builder.scheme("content");
				builder.authority("mms-sms");
				builder.path("threadID");
				builder.appendQueryParameter("recipient", numbers[i].substring(1,numbers[i].length()-1));
				cThreadId = getContext().getContentResolver().query(builder.build(),null, null, null, null);
				while(cThreadId.moveToNext()){
					threadIds.add(cThreadId.getInt(0));
				}
				cThreadId.close();
			}

			/* Get SMS and MMS from those ThreadIds */
			buf.append("thread_id IN (");
			for(int i = 0; i <threadIds.size();i++){
				buf.append(threadIds.get(i)+",");
			}
			buf.replace(buf.length()-1, buf.length(), ")");
		}
		return buf.toString();
	}
	
	/**
	 * Get MMS messages info from MMS tables according to the selection.
	 * @param selection, selection must be constructed around thread_id parameter. See getThreadIdSelection().
	 * @return
	 */
	private Cursor getMMSCursor(String selection) {
		MatrixCursor matrixCursor = new MatrixCursor(new String[]{
				EventLogData.KEY_EVENT_ROW_ID,
				EventLogData.KEY_EVENT_DATE,
				EventLogData.KEY_EVENT_MIMETYPE,
				EventLogData.KEY_EVENT_DATA,
				EventLogData.KEY_EVENT_TYPE,
				EventLogData.KEY_EVENT_STATUS,
				EventLogData.KEY_EVENT_CONTACT,
				EventLogData.KEY_EVENT_TYPE,
				EventLogData.KEY_EVENT_SESSION_ID});

		Cursor curPdu = getContext().getContentResolver().query(EventLogData.MMS_URI, null, selection, null, null);
		String id = null;
		int dest;
		String status = null;
		String date = null;
		String mmsText = null;
		String fileName = null;
		String address = null;

		while (curPdu.moveToNext()) {
			id = curPdu.getString(curPdu.getColumnIndex("_id"));
			dest = curPdu.getInt(curPdu.getColumnIndex("msg_box"));
			date = curPdu.getString(curPdu.getColumnIndex("date"));
			status = curPdu.getString(curPdu.getColumnIndex("st"));

			
			/**
			 *  Find addresses related to the message.
			 *  In Addr table, type is represented by PduHeader Constant FROM TO CC etc.. which have different type
			 *  FROM = 137
			 *  TO = 151
			 */
			Uri uriAddr = Uri.parse("content://mms/" + id + "/addr");
			Cursor curAddr = getContext().getContentResolver().query(uriAddr, null,null, null, null);
			while (curAddr.moveToNext()) {
				int type = curAddr.getInt(curAddr.getColumnIndex("type"));
				if((dest==EventLogData.VALUE_EVENT_DEST_INCOMING && type == 137) || 
						(dest==EventLogData.VALUE_EVENT_DEST_OUTGOING && type == 151)) {
					address = curAddr.getString(curAddr.getColumnIndex("address"));
					break;
				}
			}
			curAddr.close();
			
			
			/**
			 *  Find all parts according to the id of the mms in the Pdu table.
			 */
			Cursor curPart = getContext().getApplicationContext().getContentResolver().query(Uri.parse("content://mms/"+id+"/part"), null, null,null, null);
			String mimeType = null ,dataMimeType = null;
			while (curPart.moveToNext()) {
				mimeType = curPart.getString(3);
				if (MimeManager.isTextType(mimeType)) {
					mmsText = curPart.getString(13);
				}
				if (MimeManager.isImageType(mimeType) || MimeManager.isVideoType(mimeType)) {
					fileName = curPart.getString(9);
					dataMimeType = mimeType;
				}
			}
			curPart.close();

			int type = EventsLogApi.TYPE_INCOMING_SMS;
			if (dest==EventLogData.VALUE_EVENT_DEST_OUTGOING){
				type = EventsLogApi.TYPE_OUTGOING_SMS;
			}
			/**
			 *  Build a cursor with all mms entries for the specifics numbers
			 */
			matrixCursor.addRow(new Object[]{
					id,
					date+"000",
					EventLogData.MMS_MIMETYPE,
					mmsText+";"+fileName+";"+dataMimeType,
					dest,
					status,
					PhoneUtils.formatNumberToInternational(address),
					type,
					id});
		}
		curPdu.close();
		
		return matrixCursor;
	}
	
	/**
	 * Build a Sql query for chat
	 *
	 * @param selection
	 * @param chatFiltered True if we do not want chat entries
	 * @param fileTransferFiltered True if we do not want file transfer entries
	 * @return
	 */
	private String buildChatQuery(String selection, boolean chatFiltered, boolean fileTransferFiltered){
		
		String selectionFilter = "";
		if (chatFiltered){
			selectionFilter+=" AND NOT ("+RichMessagingData.KEY_TYPE+">="+EventsLogApi.TYPE_INCOMING_CHAT_MESSAGE
				+" AND "+RichMessagingData.KEY_TYPE+"<=" + EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE+")";
		}
		if (fileTransferFiltered){
			selectionFilter+=" AND NOT ("+RichMessagingData.KEY_TYPE+"=="+EventsLogApi.TYPE_INCOMING_FILE_TRANSFER
				+" OR "+RichMessagingData.KEY_TYPE+"==" + EventsLogApi.TYPE_OUTGOING_FILE_TRANSFER+")";
		}
		
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables("\""+RichMessagingProvider.TABLE+"\"");
		return builder.buildUnionSubQuery(
				EventLogData.KEY_EVENT_TYPE, 
				unionRichMessagingColumns, 
				columnsPresentInRichMessagingTable, 
				unionRichMessagingColumns.length, 
				EventLogData.KEY_EVENT_TYPE, 
				(selection!=null? selection + selectionFilter
						: selectionFilter), 
				null, 
				null, 
				null);
	}

	
	/**
	 * Build a Sql query to be part of a union query on the rcs Table
	 * Get all RichMessaging of type 'type' for the specified Numbers in selection
	 * If no type is specified, get all RichMessaging.
	 * @param selection
	 * @param chatFiltered True if we do not want chat entries
	 * @param fileTransferFiltered True if we do not want file transfer entries
	 * @return
	 */
	private String buildRichMessagingQuery(String selection, boolean chatFiltered, boolean fileTransferFiltered){
		// Do not take the "terminated" rows for chat sessions
		String selectionFilter = " NOT ("+RichMessagingData.KEY_TYPE+">="+EventsLogApi.TYPE_INCOMING_CHAT_MESSAGE + " AND "+
			RichMessagingData.KEY_TYPE+"<="+EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE+" AND ("+ RichMessagingData.KEY_STATUS+" == "+EventsLogApi.STATUS_TERMINATED +
			" OR "+ RichMessagingData.KEY_STATUS+" == "+EventsLogApi.STATUS_TERMINATED_BY_REMOTE +" OR "+ RichMessagingData.KEY_STATUS+" == "+EventsLogApi.STATUS_TERMINATED_BY_USER + "))";
		// Do not take the spam messages
		selectionFilter +=" AND NOT( "+RichMessagingData.KEY_IS_SPAM+"="+EventsLogApi.MESSAGE_IS_SPAM+ " )";
		
		if (chatFiltered){
			selectionFilter+=" AND NOT ("+RichMessagingData.KEY_TYPE+">="+EventsLogApi.TYPE_INCOMING_CHAT_MESSAGE
				+" AND "+RichMessagingData.KEY_TYPE+"<=" + EventsLogApi.TYPE_GROUP_CHAT_SYSTEM_MESSAGE+")";
		}
		if (fileTransferFiltered){
			selectionFilter+=" AND NOT ("+RichMessagingData.KEY_TYPE+"=="+EventsLogApi.TYPE_INCOMING_FILE_TRANSFER
				+" OR "+RichMessagingData.KEY_TYPE+"==" + EventsLogApi.TYPE_OUTGOING_FILE_TRANSFER+")";
		}
		
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables("\""+RichMessagingProvider.TABLE+"\"");
		
		// Group the result so we have just one row per session
		String groupBy=EventLogData.KEY_EVENT_SESSION_ID;
		return builder.buildUnionSubQuery(
				EventLogData.KEY_EVENT_TYPE, 
				unionRichMessagingColumns, 
				columnsPresentInRichMessagingTable, 
				unionRichMessagingColumns.length, 
				EventLogData.KEY_EVENT_TYPE, 
				(selection!=null? RichMessagingData.KEY_CONTACT + selection + " AND "+ selectionFilter
						: selectionFilter), 
				null, 
				groupBy, 
				null);
	}
		
	/**
	 * Build a Sql query to be part of a union query on the rcs Table
	 * Get all RichCall for the specified Numbers in selection
	 * @param selection
	 * @return
	 */
	private String buildRichCallQuery(String selection){
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		builder.setTables("\""+RichCallProvider.TABLE+"\"");
		
		return builder.buildUnionSubQuery(
				EventLogData.KEY_EVENT_TYPE, 
				unionRichCallColumns, 
				columnsPresentInRichCallTable, 
				unionRichCallColumns.length, 
				"("+Integer.toString(EventsLogApi.TYPE_OUTGOING_RICH_CALL) + "||" + Integer.toString(EventsLogApi.TYPE_INCOMING_RICH_CALL) +")", 
				(selection!=null?RichCallData.KEY_CONTACT+selection:null), 
				null, 
				null, 
				null);
	}	

	/**
	 * Delete all events from the selected mode.
	 * selection parameter is specially build : " IN ('phonenumber1','phonenumber2'...)" Must not be null
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		
		int deletedRows = 0;
		
		// Generate the body of the query 
        int match = uriMatcher.match(uri);

        switch(match) {      
        case EventsLogApi.MODE_NONE:
        	deletedRows+=deleteRichCalls(selection);
        	deletedRows+=deleteRichMessaging(selection);
        	deletedRows+=deleteSMSMMS(selection);
        	break;
    	case EventsLogApi.MODE_RC_CHAT_FT_SMS:
    		deletedRows+=deleteRichCalls(selection);
        	deletedRows+=deleteRichMessaging(selection);
        	deletedRows+=deleteSMSMMS(selection);
    		break;
		case EventsLogApi.MODE_RC_CHAT_FT:
			deletedRows+=deleteRichCalls(selection);
        	deletedRows+=deleteRichMessaging(selection);
			break;
    	case EventsLogApi.MODE_RC_CHAT_SMS:
    		deletedRows+=deleteRichCalls(selection);
        	deletedRows+=deleteChat(selection);
        	deletedRows+=deleteSMSMMS(selection);
    		break;
    	case EventsLogApi.MODE_RC_CHAT:
    		deletedRows+=deleteRichCalls(selection);
        	deletedRows+=deleteChat(selection);
    		break;
    	case EventsLogApi.MODE_RC_FT_SMS:
    		deletedRows+=deleteRichCalls(selection);
        	deletedRows+=deleteFT(selection);
        	deletedRows+=deleteSMSMMS(selection);
    		break;
    	case EventsLogApi.MODE_RC_FT:
    		deletedRows+=deleteRichCalls(selection);
        	deletedRows+=deleteFT(selection);
    		break;
		case EventsLogApi.MODE_RC_SMS:
    		deletedRows+=deleteRichCalls(selection);
        	deletedRows+=deleteSMSMMS(selection);
			break;
		case EventsLogApi.MODE_RC:
    		deletedRows+=deleteRichCalls(selection);
			break;
		case EventsLogApi.MODE_CHAT_FT_SMS:
        	deletedRows+=deleteRichMessaging(selection);
        	deletedRows+=deleteSMSMMS(selection);
			break;
		case EventsLogApi.MODE_CHAT_FT: 
        	deletedRows+=deleteRichMessaging(selection);
			break;
    	case EventsLogApi.MODE_CHAT_SMS:
        	deletedRows+=deleteChat(selection);
        	deletedRows+=deleteSMSMMS(selection);
    		break;
    	case EventsLogApi.MODE_CHAT:
        	deletedRows+=deleteChat(selection);
    		break;
    	case EventsLogApi.MODE_FT_SMS:
        	deletedRows+=deleteFT(selection);
        	deletedRows+=deleteSMSMMS(selection);
    		break;
    	case EventsLogApi.MODE_FT:
        	deletedRows+=deleteFT(selection);
    		break;
		case EventsLogApi.MODE_SMS:
        	deletedRows+=deleteSMSMMS(selection);
        	break;
		default:
	        throw new IllegalArgumentException("Unknown URI " + uri);
		}
        return deletedRows;
	}
	
	private int deleteChat(String selection){
		String chatSelection = " AND (("+RichMessagingData.KEY_TYPE+" = "+EventsLogApi.TYPE_INCOMING_CHAT_MESSAGE + 
				"OR ("+RichMessagingData.KEY_TYPE+" = "+EventsLogApi.TYPE_OUTGOING_CHAT_MESSAGE + ")" +
				"OR ("+RichMessagingData.KEY_TYPE+" = "+EventsLogApi.TYPE_CHAT_SYSTEM_MESSAGE + ")" +
				"OR ("+RichMessagingData.KEY_TYPE+" = "+EventsLogApi.TYPE_INCOMING_GROUP_CHAT_MESSAGE + ")" +
				"OR ("+RichMessagingData.KEY_TYPE+" = "+EventsLogApi.TYPE_OUTGOING_GROUP_CHAT_MESSAGE + "))";
		
        return getContext().getContentResolver().delete(
        		RichMessagingData.CONTENT_URI,
        		(selection!=null?
        				RichMessagingData.KEY_CONTACT+selection+chatSelection
        				:chatSelection)
        				,null);
	}
	
	private int deleteFT(String selection){
		String ftSelection = " AND (("+RichMessagingData.KEY_TYPE+" = "+EventsLogApi.TYPE_INCOMING_FILE_TRANSFER + 
		"OR ("+RichMessagingData.KEY_TYPE+" = "+EventsLogApi.TYPE_OUTGOING_FILE_TRANSFER + "))";
		
        return getContext().getContentResolver().delete(
        		RichMessagingData.CONTENT_URI,
        		(selection!=null?
        				RichMessagingData.KEY_CONTACT+selection+ftSelection
        				:ftSelection)
        				,null);
	}
	
	private int deleteSMSMMS(String selection){
		int deletedRows = 0;
		deletedRows+=getContext().getContentResolver().delete(EventLogData.SMS_URI,(selection!=null?KEY_SMS_ADDRESS+selection:null),null);
    	deletedRows+=getContext().getContentResolver().delete(EventLogData.MMS_URI,(selection!=null?getThreadIdSelection(selection):null),null);
		return deletedRows;
	}

	private int deleteRichCalls(String selection){
		return getContext().getContentResolver().delete(RichCallData.CONTENT_URI,(selection!=null?RichCallData.KEY_CONTACT+selection:null),null);
	}
	
	private int deleteRichMessaging(String selection){
		return getContext().getContentResolver().delete(RichMessagingData.CONTENT_URI,(selection!=null?RichMessagingData.KEY_CONTACT+selection:null),null);
	}
}
