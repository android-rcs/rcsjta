/*******************************************************************************
 * Software Name : RCS
 *
 * Copyright © 2010 France Telecom S.A.
 ******************************************************************************/

package com.orangelabs.rcs.eab;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriMatcher;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.provider.Contacts.ContactMethods;
import android.provider.Contacts.People;
import android.provider.Contacts.PeopleColumns;
import android.provider.Contacts.Phones;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Contacts.AggregationSuggestions;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.Intents.Insert;
import android.provider.ContactsContract.RawContacts;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AlphabetIndexer;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.ResourceCursorAdapter;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.orangelabs.rcs.common.ContactUtils;
import com.orangelabs.rcs.common.EabUtils;
import com.orangelabs.rcs.common.PhoneNumber;
import com.orangelabs.rcs.common.SortCursor;
import com.orangelabs.rcs.common.dialog.DialogUtils;
import com.orangelabs.rcs.common.dialog.EabDialogUtils;
import com.orangelabs.rcs.eab.Intents.UI;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.api.client.ClientApiListener;
import com.orangelabs.rcs.service.api.client.ImsEventListener;
import com.orangelabs.rcs.service.api.client.capability.CapabilityApiIntents;
import com.orangelabs.rcs.service.api.client.contacts.ContactInfo;
import com.orangelabs.rcs.service.api.client.contacts.ContactsApi;
import com.orangelabs.rcs.service.api.client.presence.PhotoIcon;
import com.orangelabs.rcs.service.api.client.presence.PresenceApi;
import com.orangelabs.rcs.service.api.client.presence.PresenceApiIntents;
import com.orangelabs.rcs.service.api.client.presence.PresenceInfo;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Displays a list of contacts. Usually is embedded into the ContactsActivity.
 * 
 * <li>RCS specific infos are added on contacts, and a "Me" item is present at first position on the list
 */
@SuppressWarnings("deprecation")
public class ContactsListActivity extends ListActivity implements
        View.OnCreateContextMenuListener, ClientApiListener, ImsEventListener {

    private static final String LIST_STATE_KEY = "liststate";
    private static final String FOCUS_KEY = "focused";

    static final int MENU_ITEM_VIEW_CONTACT = 1;
    static final int MENU_ITEM_CALL = 2;
    static final int MENU_ITEM_EDIT_BEFORE_CALL = 3;
    static final int MENU_ITEM_SEND_SMS = 4;
    static final int MENU_ITEM_SEND_IM = 5;
    static final int MENU_ITEM_EDIT = 6;
    static final int MENU_ITEM_DELETE = 7;
    static final int MENU_ITEM_TOGGLE_STAR = 8;
    static final int MENU_ITEM_VIEW_EVENT_LOG = 9;
    
    
    static final int MENU_NEW_CONTACT = 9;
    static final int MENU_SEARCH = 10;

    private static final int SUBACTIVITY_NEW_CONTACT = 1;
    private static final int SUBACTIVITY_VIEW_CONTACT = 2;
    private static final int SUBACTIVITY_DISPLAY_GROUP = 3;

    /**
     * The action for the join contact activity.
     * <p>
     * Input: extra field {@link #EXTRA_AGGREGATE_ID} is the aggregate ID.
     *
     * TODO: move to {@link ContactsContract}.
     */
    public static final String JOIN_AGGREGATE =
            "com.android.contacts.action.JOIN_AGGREGATE";

    /**
     * Used with {@link #JOIN_AGGREGATE} to give it the target for aggregation.
     * <p>
     * Type: LONG
     */
    public static final String EXTRA_AGGREGATE_ID =
            "com.android.contacts.action.AGGREGATE_ID";

    /**
     * Used with {@link #JOIN_AGGREGATE} to give it the name of the aggregation target.
     * <p>
     * Type: STRING
     */
    @Deprecated
    public static final String EXTRA_AGGREGATE_NAME =
            "com.android.contacts.action.AGGREGATE_NAME";

    public static final String AUTHORITIES_FILTER_KEY = "authorities";

    /** Mask for picker mode */
    static final int MODE_MASK_PICKER = 0x80000000;
    /** Mask for no presence mode */
    static final int MODE_MASK_NO_PRESENCE = 0x40000000;
    /** Mask for enabling list filtering */
    static final int MODE_MASK_NO_FILTER = 0x20000000;
    /** Mask for having a "create new contact" header in the list */
    static final int MODE_MASK_CREATE_NEW = 0x10000000;
    /** Mask for showing photos in the list */
    static final int MODE_MASK_SHOW_PHOTOS = 0x08000000;
    /** Mask for hiding additional information e.g. primary phone number in the list */
    static final int MODE_MASK_NO_DATA = 0x04000000;
    /** Mask for showing a call button in the list */
    static final int MODE_MASK_SHOW_CALL_BUTTON = 0x02000000;
    /** Mask to disable quickcontact (images will show as normal images) */
    static final int MODE_MASK_DISABLE_QUIKCCONTACT = 0x01000000;
    /** Mask to show the total number of contacts at the top */
    static final int MODE_MASK_SHOW_NUMBER_OF_CONTACTS = 0x00800000;

    /** Unknown mode */
    static final int MODE_UNKNOWN = 0;
    /** Default mode */
    static final int MODE_DEFAULT = 4 | MODE_MASK_SHOW_PHOTOS | MODE_MASK_SHOW_NUMBER_OF_CONTACTS;
    /** Custom mode */
    static final int MODE_CUSTOM = 8;
    /** Show all starred contacts */
    static final int MODE_STARRED = 20 | MODE_MASK_SHOW_PHOTOS;
    /** Show frequently contacted contacts */
    static final int MODE_FREQUENT = 30 | MODE_MASK_SHOW_PHOTOS;
    /** Show starred and the frequent */
    static final int MODE_STREQUENT = 35 | MODE_MASK_SHOW_PHOTOS | MODE_MASK_SHOW_CALL_BUTTON;
    /** Show all contacts and pick them when clicking */
    static final int MODE_PICK_CONTACT = 40 | MODE_MASK_PICKER | MODE_MASK_SHOW_PHOTOS
            | MODE_MASK_DISABLE_QUIKCCONTACT;
    /** Show all contacts as well as the option to create a new one */
    static final int MODE_PICK_OR_CREATE_CONTACT = 42 | MODE_MASK_PICKER | MODE_MASK_CREATE_NEW
            | MODE_MASK_SHOW_PHOTOS | MODE_MASK_DISABLE_QUIKCCONTACT;
    /** Show all people through the legacy provider and pick them when clicking */
    static final int MODE_LEGACY_PICK_PERSON = 43 | MODE_MASK_PICKER | MODE_MASK_SHOW_PHOTOS
            | MODE_MASK_DISABLE_QUIKCCONTACT;
    /** Show all people through the legacy provider as well as the option to create a new one */
    static final int MODE_LEGACY_PICK_OR_CREATE_PERSON = 44 | MODE_MASK_PICKER
            | MODE_MASK_CREATE_NEW | MODE_MASK_SHOW_PHOTOS | MODE_MASK_DISABLE_QUIKCCONTACT;
    /** Show all contacts and pick them when clicking, and allow creating a new contact */
    static final int MODE_INSERT_OR_EDIT_CONTACT = 45 | MODE_MASK_PICKER | MODE_MASK_CREATE_NEW;
    /** Show all phone numbers and pick them when clicking */
    static final int MODE_PICK_PHONE = 50 | MODE_MASK_PICKER | MODE_MASK_NO_PRESENCE;
    /** Show all phone numbers through the legacy provider and pick them when clicking */
    static final int MODE_LEGACY_PICK_PHONE =
            51 | MODE_MASK_PICKER | MODE_MASK_NO_PRESENCE | MODE_MASK_NO_FILTER;
    /** Show all postal addresses and pick them when clicking */
    static final int MODE_PICK_POSTAL =
            55 | MODE_MASK_PICKER | MODE_MASK_NO_PRESENCE | MODE_MASK_NO_FILTER;
    /** Show all postal addresses and pick them when clicking */
    static final int MODE_LEGACY_PICK_POSTAL =
            56 | MODE_MASK_PICKER | MODE_MASK_NO_PRESENCE | MODE_MASK_NO_FILTER;
    static final int MODE_GROUP = 57 | MODE_MASK_SHOW_PHOTOS;
    /** Run a search query */
    static final int MODE_QUERY = 60 | MODE_MASK_NO_FILTER | MODE_MASK_SHOW_NUMBER_OF_CONTACTS;
    /** Run a search query in PICK mode, but that still launches to VIEW */
    static final int MODE_QUERY_PICK_TO_VIEW = 65 | MODE_MASK_NO_FILTER | MODE_MASK_PICKER;

    /** Show join suggestions followed by an A-Z list */
    static final int MODE_JOIN_CONTACT = 70 | MODE_MASK_PICKER | MODE_MASK_NO_PRESENCE
            | MODE_MASK_NO_DATA | MODE_MASK_SHOW_PHOTOS | MODE_MASK_DISABLE_QUIKCCONTACT;

    /** Show all RCS contacts */
    static final int MODE_PHOTO_SHARE_VIA_FILE_TRANSFERT = 75 | MODE_MASK_PICKER;
    
    /** Maximum number of suggestions shown for joining aggregates */
    static final int MAX_SUGGESTIONS = 4;

    static final String NAME_COLUMN = Contacts.DISPLAY_NAME;
    //static final String SORT_STRING = People.SORT_STRING;

    static final String[] CONTACTS_SUMMARY_PROJECTION = new String[] {
        Contacts._ID, // 0
        Contacts.DISPLAY_NAME, // 1
        Contacts.STARRED, //2
        Contacts.TIMES_CONTACTED, //3
        Contacts.CONTACT_PRESENCE, //4
        Contacts.PHOTO_ID, //5
        Contacts.LOOKUP_KEY, //6
        Contacts.HAS_PHONE_NUMBER, //7
    };
    static final String[] CONTACTS_SUMMARY_PROJECTION_FROM_EMAIL = new String[] {
        Contacts._ID, // 0
        Contacts.DISPLAY_NAME, // 1
        Contacts.STARRED, //2
        Contacts.TIMES_CONTACTED, //3
        Contacts.CONTACT_PRESENCE, //4
        Contacts.PHOTO_ID, //5
        Contacts.LOOKUP_KEY, //6
        // email lookup doesn't included HAS_PHONE_NUMBER OR LOOKUP_KEY in projection
    };
    static final String[] LEGACY_PEOPLE_PROJECTION = new String[] {
        People._ID, // 0
        People.DISPLAY_NAME, // 1
        People.STARRED, //2
        PeopleColumns.TIMES_CONTACTED, //3
        PeopleColumns.TIMES_CONTACTED        //People.PRESENCE_STATUS, //4
    };
    static final int SUMMARY_ID_COLUMN_INDEX = 0;
    static final int SUMMARY_NAME_COLUMN_INDEX = 1;
    static final int SUMMARY_STARRED_COLUMN_INDEX = 2;
    static final int SUMMARY_TIMES_CONTACTED_COLUMN_INDEX = 3;
    static final int SUMMARY_PRESENCE_STATUS_COLUMN_INDEX = 4;
    static final int SUMMARY_PHOTO_ID_COLUMN_INDEX = 5;
    static final int SUMMARY_LOOKUP_KEY = 6;
    static final int SUMMARY_HAS_PHONE_COLUMN_INDEX = 7;

    static final String[] PHONES_PROJECTION = new String[] {
        Phone._ID, //0
        Phone.TYPE, //1
        Phone.LABEL, //2
        Phone.NUMBER, //3
        Phone.DISPLAY_NAME, // 4
        Phone.CONTACT_ID, // 5
    };
    static final String[] LEGACY_PHONES_PROJECTION = new String[] {
        Phones._ID, //0
        Phones.TYPE, //1
        Phones.LABEL, //2
        Phones.NUMBER, //3
        People.DISPLAY_NAME, // 4
    };
    static final int PHONE_ID_COLUMN_INDEX = 0;
    static final int PHONE_TYPE_COLUMN_INDEX = 1;
    static final int PHONE_LABEL_COLUMN_INDEX = 2;
    static final int PHONE_NUMBER_COLUMN_INDEX = 3;
    static final int PHONE_DISPLAY_NAME_COLUMN_INDEX = 4;
    static final int PHONE_CONTACT_ID_COLUMN_INDEX = 5;

    static final String[] POSTALS_PROJECTION = new String[] {
        StructuredPostal._ID, //0
        StructuredPostal.TYPE, //1
        StructuredPostal.LABEL, //2
        StructuredPostal.DATA, //3
        StructuredPostal.DISPLAY_NAME, // 4
    };
    static final String[] LEGACY_POSTALS_PROJECTION = new String[] {
        ContactMethods._ID, //0
        ContactMethods.TYPE, //1
        ContactMethods.LABEL, //2
        ContactMethods.DATA, //3
        People.DISPLAY_NAME, // 4
    };
    static final String[] RAW_CONTACTS_PROJECTION = new String[] {
        RawContacts._ID, //0
        RawContacts.CONTACT_ID, //1
        RawContacts.ACCOUNT_TYPE, //2
    };

    static final int POSTAL_ID_COLUMN_INDEX = 0;
    static final int POSTAL_TYPE_COLUMN_INDEX = 1;
    static final int POSTAL_LABEL_COLUMN_INDEX = 2;
    static final int POSTAL_ADDRESS_COLUMN_INDEX = 3;
    static final int POSTAL_DISPLAY_NAME_COLUMN_INDEX = 4;

    private static final int QUERY_TOKEN = 42;

    static final String KEY_PICKER_MODE = "picker_mode";

    private ContactItemListAdapter mAdapter;

    int mMode = MODE_DEFAULT;

    private QueryHandler mQueryHandler;
    private boolean mJustCreated;
    private boolean mSyncEnabled;
    private Uri mSelectedContactUri;

    private boolean mDisplayOnlyPhones;

    private Uri mGroupUri;

    private long mQueryAggregateId;

    /**
     * Used to keep track of the scroll state of the list.
     */
    private Parcelable mListState = null;
    private boolean mListHasFocus;

    private String mShortcutAction;

    /* File Uri and Type for file transfer coming from share button in gallery */
    private Uri fileUri;

    /**
     * Internal query type when in mode {@link #MODE_QUERY_PICK_TO_VIEW}.
     */
    private int mQueryMode = QUERY_MODE_NONE;

    private static final int QUERY_MODE_NONE = -1;
    private static final int QUERY_MODE_MAILTO = 1;
    private static final int QUERY_MODE_TEL = 2;

    /**
     * Data to use when in mode {@link #MODE_QUERY_PICK_TO_VIEW}. Usually
     * provided by scheme-specific part of incoming {@link Intent#getData()}.
     */
    private String mQueryData;

    private static final String CLAUSE_ONLY_VISIBLE = Contacts.IN_VISIBLE_GROUP + "=1";
    private static final String CLAUSE_ONLY_PHONES = Contacts.HAS_PHONE_NUMBER + "=1";

    /**
     * In the {@link #MODE_JOIN_CONTACT} determines whether we display a list item with the label
     * "Show all contacts" or actually show all contacts
     */
    private boolean mJoinModeShowAllContacts;

    
    private Handler mHandler = new ResultHandler();
  	private PresenceApi presenceApi = null;
	private ContactsApi contactsApi = null;
	private View myProfileView; 
  	boolean apiConnected = false;
  	boolean imsConnected = false;
    private Uri deleteUri = null;
    private final static int PHONE_NUMBER_SELECTOR = 4547;
    
    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    /**
     * Handler also managing presence action results
     */
    private class ResultHandler extends Handler{
		
		public void handleMessage(Message msg){
			switch(msg.what){
			case EabDialogUtils.BLOCK_INVITATION_RESULT_OK :
				mAdapter.notifyDataSetChanged();
				DialogUtils.displayToast(ContactsListActivity.this, this, getString(R.string.rcs_eab_blockContactSuccess));
				break;
			case EabDialogUtils.BLOCK_INVITATION_RESULT_KO :
				DialogUtils.displayToast(ContactsListActivity.this, this, getString(R.string.rcs_eab_blockContactError));
				break;
			case EabDialogUtils.UNBLOCK_INVITATION_RESULT_OK :
				mAdapter.notifyDataSetChanged();
				DialogUtils.displayToast(ContactsListActivity.this, this, getString(R.string.rcs_eab_unblockContactSuccess));
				break;
			case EabDialogUtils.UNBLOCK_INVITATION_RESULT_KO :
				DialogUtils.displayToast(ContactsListActivity.this, this, getString(R.string.rcs_eab_unblockContactError));
				break;
			case EabDialogUtils.ACCEPT_INVITATION_RESULT_OK :
				mAdapter.notifyDataSetChanged();
				DialogUtils.displayToast(ContactsListActivity.this, this, getString(R.string.rcs_eab_acceptInviteContactSuccess));
				break;
			case EabDialogUtils.ACCEPT_INVITATION_RESULT_KO :
				DialogUtils.displayToast(ContactsListActivity.this, this, getString(R.string.rcs_eab_acceptInviteContactError));
				break;
			case EabDialogUtils.DELETE_CONTACT_RESULT_OK :
			case EabDialogUtils.DELETE_MULTIPLE_CONTACTS_RESULT_OK :
        		getContentResolver().delete(deleteUri, null, null);
        		// Update the contact list
        		mHandler.post(new Runnable(){
        			public void run(){
        				// Set flag so query is done again
        				mJustCreated = true;
        				onResume();        				
        			}
        		});
        		DialogUtils.displayToast(ContactsListActivity.this, this, getString(R.string.rcs_eab_revokeContactSuccess));
        		deleteUri = null;
        		break;
			case EabDialogUtils.DELETE_CONTACT_RESULT_KO :
			case EabDialogUtils.DELETE_MULTIPLE_CONTACTS_RESULT_KO :
        		getContentResolver().delete(deleteUri, null, null);
        		// Update the contact list
        		mHandler.post(new Runnable(){
        			public void run(){
        				// Set flag so query is done again
        				mJustCreated = true;
        				onResume();        				
        			}
        		});
				DialogUtils.displayToast(ContactsListActivity.this, this, getString(R.string.rcs_eab_deleteRcsContactFailed));
        		deleteUri = null;
				break;
			}
		}
	};
    
    /**
     * The ID of the special item described above.
     */
    private static final long JOIN_MODE_SHOW_ALL_CONTACTS_ID = -2;

    // Uri matcher for contact id
    private static final int CONTACTS_ID = 1001;
    private static final UriMatcher sContactsIdMatcher;

    private static ExecutorService sImageFetchThreadPool;

    static {
        sContactsIdMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sContactsIdMatcher.addURI(ContactsContract.AUTHORITY, "contacts/#", CONTACTS_ID);
    }

    private class DeleteClickListener implements DialogInterface.OnClickListener {
        private Uri mUri;

        public DeleteClickListener(Uri uri) {
            mUri = uri;
        }

        public void onClick(DialogInterface dialog, int which) {
        	getContentResolver().delete(mUri, null, null);
        	// Update the contact list
        	mHandler.post(new Runnable(){
        		public void run(){
    				// Set flag so query is done again
    				mJustCreated = true;
    				onResume();        				
        		}
        	});
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Resolve the intent
        final Intent intent = getIntent();

        // Register RCS events receivers
        IntentFilter filter = new IntentFilter(PresenceApiIntents.MY_PRESENCE_INFO_CHANGED);
		registerReceiver(myPresenceInfoChangedIntentReceiver, filter, null, mHandler);

		filter = new IntentFilter(PresenceApiIntents.CONTACT_INFO_CHANGED);
		registerReceiver(contactPresenceInfoChangedIntentReceiver, filter, null, mHandler);
		
		filter = new IntentFilter(CapabilityApiIntents.CONTACT_CAPABILITIES);
		registerReceiver(contactCapabilitiesChangedIntentReceiver, filter, null, mHandler);
		
		filter = new IntentFilter(PresenceApiIntents.CONTACT_PHOTO_CHANGED);
		registerReceiver(contactPhotoChangedIntentReceiver, filter, null, mHandler);

		filter = new IntentFilter(PresenceApiIntents.PRESENCE_SHARING_CHANGED);
		registerReceiver(presenceSharingChangedIntentReceiver, filter, null, mHandler);
        
        final String action = intent.getAction();
        mMode = MODE_UNKNOWN;

        if (logger.isActivated()){
        	logger.info("Called with action: " + action);
        }
        if (UI.LIST_DEFAULT.equals(action)) {
            mMode = MODE_DEFAULT;
            // When mDefaultMode is true the mode is set in onResume(), since the preferneces
            // activity may change it whenever this activity isn't running
        } else if (UI.LIST_GROUP_ACTION.equals(action)) {
            mMode = MODE_GROUP;
            String groupName = intent.getStringExtra(UI.GROUP_NAME_EXTRA_KEY);
            if (TextUtils.isEmpty(groupName)) {
                finish();
                return;
            }
            buildUserGroupUri(groupName);
        } else if (UI.LIST_ALL_CONTACTS_ACTION.equals(action)) {
            mMode = MODE_CUSTOM;
            mDisplayOnlyPhones = false;
        } else if (UI.LIST_STARRED_ACTION.equals(action)) {
            mMode = MODE_STARRED;
        } else if (UI.LIST_FREQUENT_ACTION.equals(action)) {
            mMode = MODE_FREQUENT;
        } else if (UI.LIST_STREQUENT_ACTION.equals(action)) {
            mMode = MODE_STREQUENT;
        } else if (UI.LIST_CONTACTS_WITH_PHONES_ACTION.equals(action)) {
            mMode = MODE_CUSTOM;
            mDisplayOnlyPhones = true;
        } else if (Intent.ACTION_PICK.equals(action)) {
            // XXX These should be showing the data from the URI given in
            // the Intent.
            final String type = intent.resolveType(this);
            if (Contacts.CONTENT_TYPE.equals(type)) {
                mMode = MODE_PICK_CONTACT;
            } else if (People.CONTENT_TYPE.equals(type)) {
                mMode = MODE_LEGACY_PICK_PERSON;
            } else if (Phone.CONTENT_TYPE.equals(type)) {
                mMode = MODE_PICK_PHONE;
            } else if (Phones.CONTENT_TYPE.equals(type)) {
                mMode = MODE_LEGACY_PICK_PHONE;
            } else if (StructuredPostal.CONTENT_TYPE.equals(type)) {
                mMode = MODE_PICK_POSTAL;
            } else if (ContactMethods.CONTENT_POSTAL_TYPE.equals(type)) {
                mMode = MODE_LEGACY_PICK_POSTAL;
            }
        } else if (Intent.ACTION_GET_CONTENT.equals(action)) {
            final String type = intent.resolveType(this);
            if (Contacts.CONTENT_ITEM_TYPE.equals(type)) {
                mMode = MODE_PICK_OR_CREATE_CONTACT;
            } else if (Phone.CONTENT_ITEM_TYPE.equals(type)) {
                mMode = MODE_PICK_PHONE;
            } else if (Phones.CONTENT_ITEM_TYPE.equals(type)) {
                mMode = MODE_LEGACY_PICK_PHONE;
            } else if (StructuredPostal.CONTENT_ITEM_TYPE.equals(type)) {
                mMode = MODE_PICK_POSTAL;
            } else if (ContactMethods.CONTENT_POSTAL_ITEM_TYPE.equals(type)) {
                mMode = MODE_LEGACY_PICK_POSTAL;
            }  else if (People.CONTENT_ITEM_TYPE.equals(type)) {
                mMode = MODE_LEGACY_PICK_OR_CREATE_PERSON;
            }

        } else if (Intent.ACTION_INSERT_OR_EDIT.equals(action)) {
            mMode = MODE_INSERT_OR_EDIT_CONTACT;
        } else if (Intent.ACTION_SEARCH.equals(action)) {
            // See if the suggestion was clicked with a search action key (call button)
            if ("call".equals(intent.getStringExtra(SearchManager.ACTION_MSG))) {
                String query = intent.getStringExtra(SearchManager.QUERY);
                if (!TextUtils.isEmpty(query)) {
                    Intent newIntent = new Intent(Intent.ACTION_CALL,
                            Uri.fromParts("tel", query, null));
                    startActivity(newIntent);
                }
                finish();
                return;
            }

            // See if search request has extras to specify query
            if (intent.hasExtra(Insert.EMAIL)) {
                mMode = MODE_QUERY_PICK_TO_VIEW;
                mQueryMode = QUERY_MODE_MAILTO;
                mQueryData = intent.getStringExtra(Insert.EMAIL);
            } else if (intent.hasExtra(Insert.PHONE)) {
                mMode = MODE_QUERY_PICK_TO_VIEW;
                mQueryMode = QUERY_MODE_TEL;
                mQueryData = intent.getStringExtra(Insert.PHONE);
            } else {
                // Otherwise handle the more normal search case
                mMode = MODE_QUERY;
                mQueryData = getIntent().getStringExtra(SearchManager.QUERY);
            }

        // Since this is the filter activity it receives all intents
        // dispatched from the SearchManager for security reasons
        // so we need to re-dispatch from here to the intended target.
        } else if (Intents.SEARCH_SUGGESTION_CLICKED.equals(action)) {
            Uri data = intent.getData();
            Uri telUri = null;
            if (sContactsIdMatcher.match(data) == CONTACTS_ID) {
                long contactId = Long.valueOf(data.getLastPathSegment());
                final Cursor cursor = queryPhoneNumbers(contactId);
                if (cursor != null) {
                    if (cursor.getCount() == 1 && cursor.moveToFirst()) {
                        int phoneNumberIndex = cursor.getColumnIndex(Phone.NUMBER);
                        String phoneNumber = cursor.getString(phoneNumberIndex);
                        telUri = Uri.parse("tel:" + phoneNumber);
                    }
                    cursor.close();
                }
            }
            // See if the suggestion was clicked with a search action key (call button)
            Intent newIntent;
            if ("call".equals(intent.getStringExtra(SearchManager.ACTION_MSG)) && telUri != null) {
                newIntent = new Intent(Intent.ACTION_CALL, telUri);
            } else {
                newIntent = new Intent(Intent.ACTION_VIEW, data);
            }
            startActivity(newIntent);
            finish();
            return;
        } else if (Intents.SEARCH_SUGGESTION_DIAL_NUMBER_CLICKED.equals(action)) {
            Intent newIntent = new Intent(Intent.ACTION_CALL, intent.getData());
            startActivity(newIntent);
            finish();
            return;
        } else if (Intents.SEARCH_SUGGESTION_CREATE_CONTACT_CLICKED.equals(action)) {
            // TODO actually support this in EditContactActivity.
            String number = intent.getData().getSchemeSpecificPart();
            Intent newIntent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
            newIntent.putExtra(Intents.Insert.PHONE, number);
            startActivity(newIntent);
            finish();
            return;
        } else if(com.orangelabs.rcs.eab.Intents.PICK_RCSCONTACT_FOR_FILETRANSFER.equals(action)){
        	// File comes from gallery for File transfer
			Bundle mMap = intent.getExtras();
			if (mMap != null) {
				fileUri = mMap.getParcelable("fileUri");
				mMode = MODE_PHOTO_SHARE_VIA_FILE_TRANSFERT;
			}
        }

        if (JOIN_AGGREGATE.equals(action)) {
            mMode = MODE_JOIN_CONTACT;
            mQueryAggregateId = intent.getLongExtra(EXTRA_AGGREGATE_ID, -1);
            if (mQueryAggregateId == -1) {
            	
            	if (logger.isActivated()){
            		logger.error("Intent " + action + " is missing required extra: " + EXTRA_AGGREGATE_ID);
            	}
                setResult(RESULT_CANCELED);
                finish();
            }
        }

        if (mMode == MODE_UNKNOWN) {
            mMode = MODE_DEFAULT;
        }

        setContentView(R.layout.contacts_list_content);

        // Setup the UI
        final ListView list = getListView();

        // Tell list view to not show dividers. We'll do it ourself so that we can *not* show
        // them when an A-Z headers is visible.
        list.setDividerHeight(0);
        list.setFocusable(true);
        list.setOnCreateContextMenuListener(this);
        if ((mMode & MODE_MASK_NO_FILTER) != MODE_MASK_NO_FILTER) {
            list.setTextFilterEnabled(true);
        }

        // Set the proper empty string
        setEmptyText();
        
        if (RcsSettings.getInstance()==null){
        	RcsSettings.createInstance(this);
        }        
        // Add my profile header
        if (mMode!=MODE_STARRED && mMode!=MODE_PHOTO_SHARE_VIA_FILE_TRANSFERT && RcsSettings.getInstance().isSocialPresenceSupported()){
        	final LayoutInflater inflater = getLayoutInflater();
        	myProfileView = inflater.inflate(R.layout.contacts_list_item, list, false);
        	list.addHeaderView(myProfileView);
        	
        	TextView text = (TextView) myProfileView.findViewById(R.id.name);
        	text.setText(getString(R.string.rcs_eab_myName));
        }
        
        mAdapter = new ContactItemListAdapter(this);
        setListAdapter(mAdapter);
        getListView().setOnScrollListener(mAdapter);

        // We manually save/restore the listview state
        list.setSaveEnabled(false);

        presenceApi = new PresenceApi(getApplicationContext());
        presenceApi.addApiEventListener(this);
        presenceApi.addImsEventListener(this);
        presenceApi.connectApi();
        contactsApi = new ContactsApi(getApplicationContext());
        
        mQueryHandler = new QueryHandler(this);
        mJustCreated = true;

        // TODO(jham) redesign this
        mSyncEnabled = true;
    }
    
    @Override
    protected void onDestroy() {
		if (presenceApi != null) {
			presenceApi.removeAllApiEventListeners();
			presenceApi.disconnectApi();
		}
		
    	// Unregister broadcast receivers
		unregisterReceiver(myPresenceInfoChangedIntentReceiver);
		unregisterReceiver(contactPresenceInfoChangedIntentReceiver);
		unregisterReceiver(contactCapabilitiesChangedIntentReceiver);
		unregisterReceiver(contactPhotoChangedIntentReceiver);
		unregisterReceiver(presenceSharingChangedIntentReceiver);
    	
		super.onDestroy();
    }

	public void handleApiConnected() {
		apiConnected = true;
		if (mMode!=MODE_STARRED && mMode!=MODE_PHOTO_SHARE_VIA_FILE_TRANSFERT && RcsSettings.getInstance().isSocialPresenceSupported()){
			readMyProfile();
		}
	}
    
	public void handleApiDisconnected(){
		apiConnected = false;
	}
	
	public void handleApiDisabled(){
	}
	
	public void handleImsConnected(){
		imsConnected = true;
		mHandler.post(new Runnable(){
			public void run(){
				mAdapter.notifyDataSetChanged();
			}
		});
	}
	
	public void handleImsDisconnected(){
		imsConnected = false;
		mHandler.post(new Runnable(){
			public void run(){
				mAdapter.notifyDataSetChanged();
			}
		});
	}

    private void setEmptyText() {
        if (mMode == MODE_JOIN_CONTACT) {
            return;
        }

        TextView empty = (TextView) findViewById(R.id.emptyText);
        int gravity = Gravity.NO_GRAVITY;

        if (mDisplayOnlyPhones) {
            empty.setText(getText(R.string.noContactsWithPhoneNumbers));
            gravity = Gravity.CENTER;
        } else if (mMode == MODE_STREQUENT || mMode == MODE_STARRED) {
            empty.setText(getText(R.string.rcs_eab_noFavoritesHelpText));
        } else if (mMode == MODE_QUERY) {
             empty.setText(getText(R.string.rcs_eab_noMatchingContacts));
        } else {
            boolean hasSim = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE))
                    .hasIccCard();

            if (hasSim) {
                if (mSyncEnabled) {
                    empty.setText(getText(R.string.noContactsHelpTextWithSync));
                } else {
                    empty.setText(getText(R.string.noContactsHelpText));
                }
            }
        }
        empty.setGravity(gravity);
    }

    private void buildUserGroupUri(String group) {
        mGroupUri = Uri.withAppendedPath(Contacts.CONTENT_GROUP_URI, group);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (RcsSettings.getInstance()==null){
        	RcsSettings.createInstance(this);
        }
    	// If we are not connected, get my profile infos from database
		if (mMode!=MODE_STARRED && mMode!=MODE_PHOTO_SHARE_VIA_FILE_TRANSFERT && RcsSettings.getInstance().isSocialPresenceSupported()){
			readMyProfile();
		}
		// Else try to reconnect
		if (presenceApi != null) {
    		if (!apiConnected) {
    			presenceApi.connectApi();
    			presenceApi.addApiEventListener(this);	
    		}
    	}
        
        // Force cache to reload so we don't show stale photos.
        if (mAdapter.mBitmapCache != null) {
            mAdapter.mBitmapCache.clear();
        }

        boolean runQuery = true;
        Activity parent = getParent();

        // Do this before setting the filter. The filter thread relies
        // on some state that is initialized in setDefaultMode
        if (mMode == MODE_DEFAULT) {
            // If we're in default mode we need to possibly reset the mode due to a change
            // in the preferences activity while we weren't running
//            setDefaultMode();
        }

        // See if we were invoked with a filter
        if (parent != null && parent instanceof DialtactsActivity) {
            String filterText = ((DialtactsActivity) parent).getAndClearFilterText();
            if (filterText != null && filterText.length() > 0) {
                getListView().setFilterText(filterText);
                // Don't start a new query since it will conflict with the filter
                runQuery = false;
            } else if (mJustCreated) {
                getListView().clearTextFilter();
            }
        }

        if ((mJustCreated && runQuery) || (mMode == MODE_STARRED) || (mMode == MODE_PHOTO_SHARE_VIA_FILE_TRANSFERT)) {
            // We need to start a query here the first time the activity is launched, as long
            // as we aren't doing a filter.
        
        	// We also have to query the cursor in starred mode, as its content may have changed (when adding/removing favorite)
        	startQuery();
        }
        mJustCreated = false;
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        // The cursor was killed off in onStop(), so we need to get a new one here
        // We do not perform the query if a filter is set on the list because the
        // filter will cause the query to happen anyway
        if (TextUtils.isEmpty(getListView().getTextFilter())) {
            startQuery();
        } else {
            // Run the filtered query on the adapter
            ((ContactItemListAdapter) getListAdapter()).onContentChanged();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);
        // Save list state in the bundle so we can restore it after the QueryHandler has run
        icicle.putParcelable(LIST_STATE_KEY, getListView().onSaveInstanceState());
        icicle.putBoolean(FOCUS_KEY, getListView().hasFocus());
    }

    @Override
    protected void onRestoreInstanceState(Bundle icicle) {
        super.onRestoreInstanceState(icicle);
        // Retrieve list state. This will be applied after the QueryHandler has run
        mListState = icicle.getParcelable(LIST_STATE_KEY);
        mListHasFocus = icicle.getBoolean(FOCUS_KEY);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // We don't want the list to display the empty state, since when we resume it will still
        // be there and show up while the new query is happening. After the async query finished
        // in response to onRestart() setLoading(false) will be called.
        mAdapter.setLoading(true);
        mAdapter.setSuggestionsCursor(null);
        mAdapter.changeCursor(null);
        mAdapter.clearImageFetching();

        if (mMode == MODE_QUERY) {
            // Make sure the search box is closed
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            searchManager.stopSearch();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // If Contacts was invoked by another Activity simply as a way of
        // picking a contact, don't show the options menu
        if ((mMode & MODE_MASK_PICKER) == MODE_MASK_PICKER) {
            return false;
        }

        // Search
        menu.add(0, MENU_SEARCH, 0, R.string.menu_search)
                .setIcon(android.R.drawable.ic_menu_search);

        // New contact
        menu.add(0, MENU_NEW_CONTACT, 0, R.string.menu_newContact)
                .setIcon(android.R.drawable.ic_menu_add)
                .setIntent(new Intent(getApplicationContext(), EditContactActivity.class).setAction(Intents.Insert.ACTION).setData(android.provider.ContactsContract.Contacts.CONTENT_URI))
            	//.setIntent(new Intent(Intents.Insert.ACTION, People.CONTENT_URI))
                .setAlphabeticShortcut('n');

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case MENU_SEARCH:
                startSearch(null, false, null, false);
                return true;

        }
        return false;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        switch (requestCode) {
            case SUBACTIVITY_NEW_CONTACT:
                if (resultCode == RESULT_OK) {
                    returnPickerResult(null, data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME),
                            data.getData(), 0);
                }
                break;

            case SUBACTIVITY_VIEW_CONTACT:
                if (resultCode == RESULT_OK) {
                    mAdapter.notifyDataSetChanged();
                }
                break;

            case SUBACTIVITY_DISPLAY_GROUP:
                // Mark as just created so we re-run the view query
                mJustCreated = true;
                break;
                
            case PHONE_NUMBER_SELECTOR:
            	if (resultCode==RESULT_OK){
            		int choice = data.getIntExtra("result_choice", 0);
            		String phoneNumber = data.getStringExtra("called_number");
            		if (choice == PhoneNumberSelectorActivity.CALL){
            			EabUtils.initiateCall(this, phoneNumber);
            		}
            		if (choice == PhoneNumberSelectorActivity.TEXT){
            			EabUtils.initiateSms(this, phoneNumber);
            		}

            	}
                break;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        // If Contacts was invoked by another Activity simply as a way of
        // picking a contact, don't show the context menu
        if ((mMode & MODE_MASK_PICKER) == MODE_MASK_PICKER) {
            return;
        }
        
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
        	if (logger.isActivated()){
        		logger.error("bad menuInfo", e);
        	}
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        
        if (mMode!=MODE_STARRED && RcsSettings.getInstance().isSocialPresenceSupported()){
        	// In mode non starred, we have added the "My Profile" item, so must report the translation
        	// Except if position is already 0, in this case we are already at "My Profile" item, so must return (no context menu for it)
        	if (info.position == 0){
        		return;
        	}
        	cursor = (Cursor) getListAdapter().getItem(info.position - 1);
        }
        
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }
        long id = info.id;
        Uri contactUri = ContentUris.withAppendedId(android.provider.ContactsContract.Contacts.CONTENT_URI, id);
        long rawContactId = ContactUtils.queryForRawContactId(getContentResolver(), id);
        Uri rawContactUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId);
        
        // Setup the menu header
        menu.setHeaderTitle(cursor.getString(SUMMARY_NAME_COLUMN_INDEX));

        // View contact details
        menu.add(0, MENU_ITEM_VIEW_CONTACT, 0, R.string.menu_viewContact)
                .setIntent(new Intent(getApplicationContext(), ViewContactActivity.class).setData(contactUri).setAction(Intent.ACTION_VIEW));

        // View event log
        menu.add(0, MENU_ITEM_VIEW_EVENT_LOG, 0, R.string.rcs_eab_menu_view_event_log)
        		.setIntent(new Intent(getApplicationContext(), EventLogListActivity.class).putExtra("incomingContactId",id).setAction(Intent.ACTION_VIEW));
        
        // Phone related menus
        if (cursor.getInt(SUMMARY_HAS_PHONE_COLUMN_INDEX) != 0) {
            // Calling contact
            menu.add(0, MENU_ITEM_CALL, 0,
                    getString(R.string.rcs_eab_menu_call_contact));
            // Texting contact
            menu.add(0, MENU_ITEM_SEND_SMS, 0, getString(R.string.rcs_eab_menu_text_contact));
        }

        // Star toggling, only if not RCS active
        ArrayList<PhoneNumber> rcsNumbers = ContactUtils.getRcsActiveContactNumbers(this, id);
        if (rcsNumbers.size()==0){
        	int starState = cursor.getInt(SUMMARY_STARRED_COLUMN_INDEX);
        	if (starState == 0) {
        		menu.add(0, MENU_ITEM_TOGGLE_STAR, 0, R.string.rcs_eab_menu_add_starred);
        	} else {
        		menu.add(0, MENU_ITEM_TOGGLE_STAR, 0, R.string.rcs_eab_menu_remove_starred);
        	}
        }

        // Contact editing
        menu.add(0, MENU_ITEM_EDIT, 0, R.string.menu_editContact)
        		.setIntent(new Intent(getApplicationContext(), EditContactActivity.class).setData(rawContactUri).setAction(Intent.ACTION_EDIT));
        menu.add(0, MENU_ITEM_DELETE, 0, R.string.menu_deleteContact);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
        	if (logger.isActivated()){
        		logger.error("bad menuInfo", e);
        	}
            return false;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        
        if (mMode!=MODE_STARRED && RcsSettings.getInstance().isSocialPresenceSupported()){
        	// In mode non starred, we have added the "My Profile" item, so must report the translation
        	// Except if position is already 0, in this case we are already at "My Profile" item, so must return (no context menu for it)
        	if (info.position == 0){
        		return false;
        	}
        	cursor = (Cursor) getListAdapter().getItem(info.position - 1);
        }
        
        switch (item.getItemId()) {
            case MENU_ITEM_TOGGLE_STAR: {
                // Toggle the star
                ContentValues values = new ContentValues(1);
                values.put(Contacts.STARRED, cursor.getInt(SUMMARY_STARRED_COLUMN_INDEX) == 0 ? 1 : 0);
                final Uri selectedUri = this.getContactUri(info.position);
                getContentResolver().update(selectedUri, values, null, null);
                if (mMode == MODE_STARRED){
                	// In starred mode, we must do the requery explicitly so the list content is updated 
                	// (ie : the no more starred item is removed from the list) 
                	startQuery();
                }
                return true;
            }

            case MENU_ITEM_CALL: {
                callContact(cursor);
                return true;
            }

            case MENU_ITEM_SEND_SMS: {
                smsContact(cursor);
                return true;
            }

            case MENU_ITEM_DELETE: {
                mSelectedContactUri = getContactUri(info.position);
                final int idPerson = (int)ContentUris.parseId(mSelectedContactUri);
                doContactDelete(cursor, idPerson);
                return true;
            }
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL: {
                if (callSelection()) {
                    return true;
                }
                break;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Prompt the user before deleting the given {@link Contacts} entry.
     */
    protected void doContactDelete(Cursor cursor, int idPerson) {
    	// Get the RCS phone numbers
    	ArrayList<PhoneNumber> rcsPhoneNumbers = ContactUtils.getRcsActiveOrInvitedContactNumbers(this, idPerson);
    	
        if (rcsPhoneNumbers.size()==0) {
            // There is no rcs phone number.
        	deleteNonRcsContactConfirmation(mSelectedContactUri);
        	return;
        }else{
        	String contactName = ContactUtils.getContactDisplayName(this,idPerson);
        	// One or more rcs phone numbers
        	EabDialogUtils.showDeleteContactDialog(ContactsListActivity.this, mHandler, contactName, rcsPhoneNumbers, presenceApi);
        	// Save the uri and the person id so it can be deleted if result of revoke is ok
        	deleteUri = mSelectedContactUri;
        }
    }
        
    private void deleteNonRcsContactConfirmation(Uri uri){
    	// Not an RCS contact
    	new AlertDialog.Builder(ContactsListActivity.this)
    	.setTitle(R.string.deleteConfirmation_title)
    	.setIcon(android.R.drawable.ic_dialog_alert)
    	.setMessage(R.string.deleteConfirmation)
    	.setNegativeButton(android.R.string.cancel, null)
    	.setPositiveButton(android.R.string.ok, new DeleteClickListener(uri))
    	.show();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // Hide soft keyboard, if visible
        InputMethodManager inputMethodManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getListView().getWindowToken(), 0);

        // Click for "My Profile" item
        if (mMode!=MODE_STARRED && position == 0 && mMode!=MODE_PHOTO_SHARE_VIA_FILE_TRANSFERT && RcsSettings.getInstance().isSocialPresenceSupported()){
        	Intent viewMyProfileIntent = new Intent(getApplicationContext(), ViewMyProfileActivity.class);
        	startActivity(viewMyProfileIntent);
        }
        
        if (mMode == MODE_INSERT_OR_EDIT_CONTACT) {
            Intent intent;
            if (position == 0) {
                intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
            } else {
                // Edit. adjusting position by subtracting header view count.
                position -= getListView().getHeaderViewsCount();
                final Uri uri = getSelectedUri(position);
                intent = new Intent(Intent.ACTION_EDIT, uri);
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            Bundle extras = getIntent().getExtras();

            if (extras == null) {
                extras = new Bundle();
            }
            intent.putExtras(extras);
            extras.putBoolean(KEY_PICKER_MODE, (mMode & MODE_MASK_PICKER) == MODE_MASK_PICKER);

            startActivity(intent);
            finish();
        } else if (id != -1) {
            // Subtract one if we have Create Contact at the top
            if ((mMode & MODE_MASK_CREATE_NEW) != 0) {
                position--;
            }
            final Uri uri = getSelectedUri(position);
            if ((mMode & MODE_MASK_PICKER) == 0) {
                final Intent intent = new Intent(getApplicationContext(), ViewContactActivity.class).setAction(Intent.ACTION_VIEW).setData(ContentUris.withAppendedId(android.provider.ContactsContract.Contacts.CONTENT_URI, id));
                startActivityForResult(intent, SUBACTIVITY_VIEW_CONTACT);
            } else if (mMode == MODE_JOIN_CONTACT) {
                if (id == JOIN_MODE_SHOW_ALL_CONTACTS_ID) {
                    mJoinModeShowAllContacts = false;
                    startQuery();
                } else {
                    returnPickerResult(null, null, uri, id);
                }
            } else if (mMode == MODE_QUERY_PICK_TO_VIEW) {
                // Started with query that should launch to view contact
                final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                finish();
            } else if (mMode == MODE_PICK_CONTACT
                    || mMode == MODE_PICK_OR_CREATE_CONTACT
                    || mMode == MODE_LEGACY_PICK_PERSON
                    || mMode == MODE_LEGACY_PICK_OR_CREATE_PERSON) {
                if (mShortcutAction != null) {
                    Cursor c = (Cursor) mAdapter.getItem(position);
                    returnPickerResult(c, c.getString(SUMMARY_NAME_COLUMN_INDEX), uri, id);
                } else {
                    returnPickerResult(null, null, uri, id);
                }
            } else if (mMode == MODE_PICK_PHONE) {
                if (mShortcutAction != null) {
                    Cursor c = (Cursor) mAdapter.getItem(position);
                    returnPickerResult(c, c.getString(PHONE_DISPLAY_NAME_COLUMN_INDEX), uri, id);
                } else {
                    returnPickerResult(null, null, uri, id);
                }
            } else if (mMode == MODE_PICK_POSTAL
                    || mMode == MODE_LEGACY_PICK_POSTAL
                    || mMode == MODE_LEGACY_PICK_PHONE) {
                returnPickerResult(null, null, uri, id);
            } else if (mMode == MODE_PHOTO_SHARE_VIA_FILE_TRANSFERT){
            	final long rawContactId = ContentUris.parseId(uri);
                ArrayList<PhoneNumber> numbers = ContactUtils.getRcsContactNumbers(this.getApplicationContext(), rawContactId);
            	String contact = numbers.get(0).number;
            	String fileName = null;
            	Cursor cursor = getContentResolver().query(fileUri, new String[]{MediaStore.Images.ImageColumns.DATA}, null, null, null);
    			if(cursor.moveToFirst()){
    				fileName = cursor.getString(0);
    			}
    			cursor.close();
    			
    			/* Use this intent instead of ClientApiUtils.RCS_FILE_TRANSFER_APPS 
    			 * to launch a file transfer bypassing the selector */
    			Intent intentFileTransfer = new Intent(com.orangelabs.rcs.eab.Intents.START_FILETRANSFER);
        		intentFileTransfer.putExtra("contact",contact);
        		intentFileTransfer.putExtra("file", fileName);
            	startActivity(intentFileTransfer);
            	finish();
            }
        } else if ((mMode & MODE_MASK_CREATE_NEW) == MODE_MASK_CREATE_NEW
                && position == 0) {
            Intent newContact = new Intent(getApplicationContext(), EditContactActivity.class).setAction(Intents.Insert.ACTION).setData(People.CONTENT_URI);
            startActivityForResult(newContact, SUBACTIVITY_NEW_CONTACT);
        } else {
            signalError();
        }
    }

    /**
     * @param uri In most cases, this should be a lookup {@link Uri}, possibly
     *            generated through {@link Contacts#getLookupUri(long, String)}.
     */
    private void returnPickerResult(Cursor c, String name, Uri uri, long id) {
        final Intent intent = new Intent();
        setResult(RESULT_OK, intent.setData(uri));
        finish();
    }

    Uri getUriToQuery() {
        switch(mMode) {
            case MODE_JOIN_CONTACT:
                return getJoinSuggestionsUri(null);
            case MODE_FREQUENT:
            case MODE_STARRED:
            case MODE_DEFAULT:
            case MODE_INSERT_OR_EDIT_CONTACT:
            case MODE_PICK_CONTACT:
            case MODE_PICK_OR_CREATE_CONTACT:{
                return Contacts.CONTENT_URI;
            }
            case MODE_STREQUENT: {
                return Contacts.CONTENT_STREQUENT_URI;
            }
            case MODE_LEGACY_PICK_PERSON:
            case MODE_LEGACY_PICK_OR_CREATE_PERSON: {
                return People.CONTENT_URI;
            }
            case MODE_PICK_PHONE: {
                return Phone.CONTENT_URI;
            }
            case MODE_LEGACY_PICK_PHONE: {
                return Phones.CONTENT_URI;
            }
            case MODE_PICK_POSTAL: {
                return StructuredPostal.CONTENT_URI;
            }
            case MODE_LEGACY_PICK_POSTAL: {
                return ContactMethods.CONTENT_URI;
            }
            case MODE_QUERY_PICK_TO_VIEW: {
                if (mQueryMode == QUERY_MODE_MAILTO) {
                    return Uri.withAppendedPath(Email.CONTENT_FILTER_URI, Uri.encode(mQueryData));
                } else if (mQueryMode == QUERY_MODE_TEL) {
                    return Uri.withAppendedPath(Phone.CONTENT_FILTER_URI, Uri.encode(mQueryData));
                }
            }
            case MODE_QUERY: {
                return getContactFilterUri(mQueryData);
            }
            case MODE_GROUP: {
                return mGroupUri;
            }
            case MODE_PHOTO_SHARE_VIA_FILE_TRANSFERT : {
            	return Contacts.CONTENT_URI;
            }
            default: {
                throw new IllegalStateException("Can't generate URI: Unsupported Mode.");
            }
        }
    }

    /**
     * Build the {@link Contacts#CONTENT_LOOKUP_URI} for the given
     * {@link ListView} position, using {@link #mAdapter}.
     */
    private Uri getContactUri(int position) {
        if (position == ListView.INVALID_POSITION) {
            throw new IllegalArgumentException("Position not in list bounds");
        }

        Cursor cursor = (Cursor)mAdapter.getItem(position);
        
        if (mMode!=MODE_STARRED && mMode!=MODE_PHOTO_SHARE_VIA_FILE_TRANSFERT && RcsSettings.getInstance().isSocialPresenceSupported()){
        	// In mode non starred, we have added the "My Profile" item, so must report the translation
        	// Except if position is already 0, in this case we are already at "My Profile" item, so must return (no context menu for it)
        	if (position == 0){
        		return null;
        	}
        	cursor = (Cursor) getListAdapter().getItem(position - 1);
        }
        
        switch(mMode) {
            case MODE_LEGACY_PICK_PERSON:
            case MODE_LEGACY_PICK_OR_CREATE_PERSON: {
                final long personId = cursor.getLong(SUMMARY_ID_COLUMN_INDEX);
                return ContentUris.withAppendedId(People.CONTENT_URI, personId);
            }

            default: {
                // Build and return soft, lookup reference
                final long contactId = cursor.getLong(SUMMARY_ID_COLUMN_INDEX);
                final String lookupKey = cursor.getString(SUMMARY_LOOKUP_KEY);
                return Contacts.getLookupUri(contactId, lookupKey);
            }
        }
    }

    /**
     * Build the {@link Uri} for the given {@link ListView} position, which can
     * be used as result when in {@link #MODE_MASK_PICKER} mode.
     */
    private Uri getSelectedUri(int position) {
        if (position == ListView.INVALID_POSITION) {
            throw new IllegalArgumentException("Position not in list bounds");
        }
        long id = mAdapter.getItemId(position);
        
        if (mMode!=MODE_STARRED && mMode!=MODE_PHOTO_SHARE_VIA_FILE_TRANSFERT && RcsSettings.getInstance().isSocialPresenceSupported()){
        	// In mode non starred, we have added the "My Profile" item, so must report the translation
        	// Except if position is already 0, in this case we are already at "My Profile" item, so must return (no context menu for it)
        	if (position == 0){
        		return null;
        	}
        	
        	id = mAdapter.getItemId(position - 1);
        }

        switch(mMode) {
            case MODE_LEGACY_PICK_PERSON:
            case MODE_LEGACY_PICK_OR_CREATE_PERSON: {
                return ContentUris.withAppendedId(People.CONTENT_URI, id);
            }
            case MODE_PICK_PHONE: {
                return ContentUris.withAppendedId(Data.CONTENT_URI, id);
            }
            case MODE_LEGACY_PICK_PHONE: {
                return ContentUris.withAppendedId(Phones.CONTENT_URI, id);
            }
            case MODE_PICK_POSTAL: {
                return ContentUris.withAppendedId(Data.CONTENT_URI, id);
            }
            case MODE_LEGACY_PICK_POSTAL: {
                return ContentUris.withAppendedId(ContactMethods.CONTENT_URI, id);
            }
            default: {
                return getContactUri(position);
            }
        }
    }

    String[] getProjectionForQuery() {
        switch(mMode) {
        	case MODE_PHOTO_SHARE_VIA_FILE_TRANSFERT:
            case MODE_JOIN_CONTACT:
            case MODE_STREQUENT:
            case MODE_FREQUENT:
            case MODE_STARRED:
            case MODE_QUERY:
            case MODE_DEFAULT:
            case MODE_INSERT_OR_EDIT_CONTACT:
            case MODE_GROUP:
            case MODE_PICK_CONTACT:
            case MODE_PICK_OR_CREATE_CONTACT: {
                return CONTACTS_SUMMARY_PROJECTION;
            }
            case MODE_LEGACY_PICK_PERSON:
            case MODE_LEGACY_PICK_OR_CREATE_PERSON: {
                return LEGACY_PEOPLE_PROJECTION ;
            }
            case MODE_PICK_PHONE: {
                return PHONES_PROJECTION;
            }
            case MODE_LEGACY_PICK_PHONE: {
                return LEGACY_PHONES_PROJECTION;
            }
            case MODE_PICK_POSTAL: {
                return POSTALS_PROJECTION;
            }
            case MODE_LEGACY_PICK_POSTAL: {
                return LEGACY_POSTALS_PROJECTION;
            }
            case MODE_QUERY_PICK_TO_VIEW: {
                if (mQueryMode == QUERY_MODE_MAILTO) {
                    return CONTACTS_SUMMARY_PROJECTION_FROM_EMAIL;
                } else if (mQueryMode == QUERY_MODE_TEL) {
                    return PHONES_PROJECTION;
                }
                break;
            }
        }

        // Default to normal aggregate projection
        return CONTACTS_SUMMARY_PROJECTION;
    }

    /**
     * Return the selection arguments for a default query based on the
     * {@link #mDisplayOnlyPhones} flag.
     */
    private String getContactSelection() {
    	// We filter also on "My Profile", as it already appears at top
    	String CLAUSE_NOT_MY_PROFILE = "("+Contacts.DISPLAY_NAME + " <> " + "\"" + getString(R.string.rcs_eab_my_profile) + "\" AND " + Contacts.DISPLAY_NAME + " <> " + "\"" + getString(R.string.rcs_eab_my_profile_escaped) + "\")";
    	
    	if (mDisplayOnlyPhones) {
    		return CLAUSE_ONLY_VISIBLE + " AND " + CLAUSE_ONLY_PHONES + " AND " + CLAUSE_NOT_MY_PROFILE;
    	} else {
    		return CLAUSE_ONLY_VISIBLE + " AND " + CLAUSE_NOT_MY_PROFILE;
    	}    	
    	
    }

    private Uri getContactFilterUri(String filter) {
        if (!TextUtils.isEmpty(filter)) {
            return Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, Uri.encode(filter));
        } else {
            return Contacts.CONTENT_URI;
        }
    }

    private Uri getPeopleFilterUri(String filter) {
        if (!TextUtils.isEmpty(filter)) {
            return Uri.withAppendedPath(People.CONTENT_FILTER_URI, Uri.encode(filter));
        } else {
            return People.CONTENT_URI;
        }
    }

    private Uri getJoinSuggestionsUri(String filter) {
        Builder builder = Contacts.CONTENT_URI.buildUpon();
        builder.appendEncodedPath(String.valueOf(mQueryAggregateId));
        builder.appendEncodedPath(AggregationSuggestions.CONTENT_DIRECTORY);
        if (!TextUtils.isEmpty(filter)) {
            builder.appendEncodedPath(Uri.encode(filter));
        }
        builder.appendQueryParameter("limit", String.valueOf(MAX_SUGGESTIONS));
        return builder.build();
    }

    private static String getSortOrder(String[] projectionType) {
        /* if (Locale.getDefault().equals(Locale.JAPAN) &&
                projectionType == AGGREGATES_PRIMARY_PHONE_PROJECTION) {
            return SORT_STRING + " ASC";
        } else {
            return NAME_COLUMN + " COLLATE LOCALIZED ASC";
        } */

        return NAME_COLUMN + " COLLATE LOCALIZED ASC";
    }

    void startQuery() {
        mAdapter.setLoading(true);

        // Cancel any pending queries
        mQueryHandler.cancelOperation(QUERY_TOKEN);
        mQueryHandler.setLoadingJoinSuggestions(false);

        String[] projection = getProjectionForQuery();
        String callingPackage = getCallingPackage();
        Uri uri = getUriToQuery();
        if (!TextUtils.isEmpty(callingPackage)) {
            uri = uri.buildUpon()
                    .appendQueryParameter("requesting_package",//ContactsContract.REQUESTING_PACKAGE_PARAM_KEY,
                            callingPackage)
                    .build();
        }

        // Kick off the new query
        switch (mMode) {
            case MODE_GROUP:
                mQueryHandler.startQuery(QUERY_TOKEN, null,
                        uri, projection, getContactSelection(), null,
                        getSortOrder(projection));
                break;

            case MODE_DEFAULT:
            case MODE_PICK_CONTACT:
            case MODE_PICK_OR_CREATE_CONTACT:
            case MODE_INSERT_OR_EDIT_CONTACT:
                mQueryHandler.startQuery(QUERY_TOKEN, null, uri,
                        projection, getContactSelection(), null,
                        getSortOrder(projection));
                break;

            case MODE_LEGACY_PICK_PERSON:
            case MODE_LEGACY_PICK_OR_CREATE_PERSON:
                mQueryHandler.startQuery(QUERY_TOKEN, null, uri,
                        projection, null, null,
                        getSortOrder(projection));
                break;

            case MODE_QUERY: {
                mQueryHandler.startQuery(QUERY_TOKEN, null, uri,
                        projection, null, null,
                        getSortOrder(projection));
                break;
            }

            case MODE_QUERY_PICK_TO_VIEW: {
                mQueryHandler.startQuery(QUERY_TOKEN, null, uri, projection, null, null,
                        getSortOrder(projection));
                break;
            }

            case MODE_STARRED:
                mQueryHandler.startQuery(QUERY_TOKEN, null, uri,
                        projection, Contacts.STARRED + "=1", null,
                        getSortOrder(projection));
                break;

            case MODE_FREQUENT:
                mQueryHandler.startQuery(QUERY_TOKEN, null, uri,
                        projection,
                        Contacts.TIMES_CONTACTED + " > 0", null,
                        Contacts.TIMES_CONTACTED + " DESC, "
                        + getSortOrder(projection));
                break;

            case MODE_STREQUENT:
                mQueryHandler.startQuery(QUERY_TOKEN, null, uri, projection, null, null, null);
                break;

            case MODE_PICK_PHONE:
            case MODE_LEGACY_PICK_PHONE:
                mQueryHandler.startQuery(QUERY_TOKEN, null, uri,
                        projection, null, null, getSortOrder(projection));
                break;

            case MODE_PICK_POSTAL:
                mQueryHandler.startQuery(QUERY_TOKEN, null, uri,
                        projection, null, null, getSortOrder(projection));
                break;

            case MODE_LEGACY_PICK_POSTAL:
                mQueryHandler.startQuery(QUERY_TOKEN, null, uri,
                        projection,
                        ContactMethods.KIND + "=" + android.provider.Contacts.KIND_POSTAL, null,
                        getSortOrder(projection));
                break;

            case MODE_JOIN_CONTACT:
                mQueryHandler.setLoadingJoinSuggestions(true);
                mQueryHandler.startQuery(QUERY_TOKEN, null, uri, projection,
                        null, null, null);
                break;
            case MODE_PHOTO_SHARE_VIA_FILE_TRANSFERT:
                mQueryHandler.startQuery(QUERY_TOKEN, null, uri,
                        projection, null, null, null);
                break;
        }
    }

    /**
     * Called from a background thread to do the filter and return the resulting cursor.
     *
     * @param filter the text that was entered to filter on
     * @return a cursor with the results of the filter
     */
    Cursor doFilter(String filter) {
        final ContentResolver resolver = getContentResolver();

        String[] projection = getProjectionForQuery();

        switch (mMode) {
            case MODE_DEFAULT:
            case MODE_PICK_CONTACT:
            case MODE_PICK_OR_CREATE_CONTACT:
            case MODE_INSERT_OR_EDIT_CONTACT: {
                return resolver.query(getContactFilterUri(filter), projection,
                        getContactSelection(), null, getSortOrder(projection));
            }

            case MODE_LEGACY_PICK_PERSON:
            case MODE_LEGACY_PICK_OR_CREATE_PERSON: {
                return resolver.query(getPeopleFilterUri(filter), projection, null, null,
                        getSortOrder(projection));
            }

            case MODE_STARRED: {
                return resolver.query(getContactFilterUri(filter), projection,
                        Contacts.STARRED + "=1", null,
                        getSortOrder(projection));
            }

            case MODE_FREQUENT: {
                return resolver.query(getContactFilterUri(filter), projection,
                        Contacts.TIMES_CONTACTED + " > 0", null,
                        Contacts.TIMES_CONTACTED + " DESC, "
                        + getSortOrder(projection));
            }

            case MODE_STREQUENT: {
                Uri uri;
                if (!TextUtils.isEmpty(filter)) {
                    uri = Uri.withAppendedPath(Contacts.CONTENT_STREQUENT_FILTER_URI,
                            Uri.encode(filter));
                } else {
                    uri = Contacts.CONTENT_STREQUENT_URI;
                }
                return resolver.query(uri, projection, null, null, null);
            }

            case MODE_PICK_PHONE: {
                Uri uri = getUriToQuery();
                if (!TextUtils.isEmpty(filter)) {
                    uri = Uri.withAppendedPath(Phone.CONTENT_FILTER_URI, Uri.encode(filter));
                }
                return resolver.query(uri, projection, null, null,
                        getSortOrder(projection));
            }

            case MODE_LEGACY_PICK_PHONE: {
                //TODO: Support filtering here (bug 2092503)
                break;
            }

            case MODE_JOIN_CONTACT: {

                // We are on a background thread. Run queries one after the other synchronously
                Cursor cursor = resolver.query(getJoinSuggestionsUri(filter), projection, null,
                        null, null);
                mAdapter.setSuggestionsCursor(cursor);
                mJoinModeShowAllContacts = false;
                return resolver.query(getContactFilterUri(filter), projection,
                        Contacts._ID + " != " + mQueryAggregateId + " AND " + CLAUSE_ONLY_VISIBLE,
                        null, getSortOrder(projection));
            }
        }
        throw new UnsupportedOperationException("filtering not allowed in mode " + mMode);
    }

    private Cursor getShowAllContactsLabelCursor(String[] projection) {
        MatrixCursor matrixCursor = new MatrixCursor(projection);
        Object[] row = new Object[projection.length];
        // The only columns we care about is the id
        row[SUMMARY_ID_COLUMN_INDEX] = JOIN_MODE_SHOW_ALL_CONTACTS_ID;
        matrixCursor.addRow(row);
        return matrixCursor;
    }

    /**
     * Calls the currently selected list item.
     * @return true if the call was initiated, false otherwise
     */
    boolean callSelection() {
        ListView list = getListView();
        if (list.hasFocus()) {
            Cursor cursor = (Cursor) list.getSelectedItem();
            return callContact(cursor);
        }
        return false;
    }

    boolean callContact(Cursor cursor) {
        return callOrSmsContact(cursor, false /*call*/);
    }

    boolean smsContact(Cursor cursor) {
        return callOrSmsContact(cursor, true /*sms*/);
    }

    /**
     * Calls the contact which the cursor is point to.
     * @return true if the call was initiated, false otherwise
     */
    boolean callOrSmsContact(Cursor cursor, boolean sendSms) {
        if (cursor != null) {
        	Intent phoneNumberSelectorIntent = new Intent(this, PhoneNumberSelectorActivity.class);
        	phoneNumberSelectorIntent.putExtra("return-data", true);
        	phoneNumberSelectorIntent.putExtra("contactId", cursor.getInt(0));
        	if (sendSms){
        		phoneNumberSelectorIntent.putExtra("action", PhoneNumberSelectorActivity.TEXT);
        	}else{
        		phoneNumberSelectorIntent.putExtra("action", PhoneNumberSelectorActivity.CALL);
        	}
        	startActivityForResult(phoneNumberSelectorIntent, PHONE_NUMBER_SELECTOR);
            return true;
        }

        return false;
    }

    private Cursor queryPhoneNumbers(long contactId) {
        Uri baseUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId);
        Uri dataUri = Uri.withAppendedPath(baseUri, Contacts.Data.CONTENT_DIRECTORY);

        Cursor c = getContentResolver().query(dataUri,
                new String[] {Phone._ID, Phone.NUMBER, Phone.IS_SUPER_PRIMARY},
                Data.MIMETYPE + "=?", new String[] {Phone.CONTENT_ITEM_TYPE}, null);
        if (c != null && c.moveToFirst()) {
            return c;
        }
        return null;
    }

    /**
     * Signal an error to the user.
     */
    void signalError() {
        //TODO play an error beep or something...
    }

    Cursor getItemForView(View view) {
        ListView listView = getListView();
        int index = listView.getPositionForView(view);
        if (index < 0) {
            return null;
        }
        return (Cursor) listView.getAdapter().getItem(index);
    }

    private/* static */class QueryHandler extends AsyncQueryHandler {
        protected final WeakReference<ContactsListActivity> mActivity;
        protected boolean mLoadingJoinSuggestions = false;

        public QueryHandler(Context context) {
            super(context.getContentResolver());
            mActivity = new WeakReference<ContactsListActivity>((ContactsListActivity) context);
        }

        public void setLoadingJoinSuggestions(boolean flag) {
            mLoadingJoinSuggestions = flag;
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            final ContactsListActivity activity = mActivity.get();
            if (activity != null && !activity.isFinishing()) {

                // Whenever we get a suggestions cursor, we need to immediately kick off
                // another query for the complete list of contacts
                if (cursor != null && mLoadingJoinSuggestions) {
                    mLoadingJoinSuggestions = false;
                    if (cursor.getCount() > 0) {
                        activity.mAdapter.setSuggestionsCursor(cursor);
                    } else {
                        cursor.close();
                        activity.mAdapter.setSuggestionsCursor(null);
                    }

                    if (activity.mAdapter.mSuggestionsCursorCount == 0
                            || !activity.mJoinModeShowAllContacts) {
                        startQuery(QUERY_TOKEN, null, activity.getContactFilterUri(
                                        activity.mQueryData),
                                CONTACTS_SUMMARY_PROJECTION,
                                Contacts._ID + " != " + activity.mQueryAggregateId
                                        + " AND " + CLAUSE_ONLY_VISIBLE, null,
                                getSortOrder(CONTACTS_SUMMARY_PROJECTION));
                        return;
                    }

                    cursor = activity.getShowAllContactsLabelCursor(CONTACTS_SUMMARY_PROJECTION);
                }

                
                if (mMode==MODE_STARRED){
                	// In the cursor, we must have the starred contacts AND the RCS contacts
                	
                	// Get numbers that have rich data
                	List<String> rcsNumbers = null;
                	
                	ContactsApi contactsApi = new ContactsApi(activity);
                	rcsNumbers = contactsApi.getRcsContacts();

                	String[] CONTACTS_PROJECTION = new String[] {
                				android.provider.ContactsContract.Contacts._ID, // 0
                				android.provider.ContactsContract.Contacts.DISPLAY_NAME, // 1
                				android.provider.ContactsContract.Contacts.STARRED, //2
                				android.provider.ContactsContract.Contacts.TIMES_CONTACTED, //3
                				android.provider.ContactsContract.Contacts.CONTACT_PRESENCE, //4
                				android.provider.ContactsContract.Contacts.PHOTO_ID, //5
                				android.provider.ContactsContract.Contacts.LOOKUP_KEY, //6
                				android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER //7
                		};
                	
                	MatrixCursor rcsMatrixCursor = new MatrixCursor(CONTACTS_PROJECTION);

                	// We have to store the rcs contacts ids : if the contact is in the RCS cursor, 
                	// there will be no need to put it also in the starred cursor (we have to avoid double entries)
                	ArrayList<Integer>rcsContactsId = new ArrayList<Integer>();
                	if (rcsNumbers!=null){
                		// rcsNumber may be null if RCS core has not been installed
                    	for (int i=0; i<rcsNumbers.size();i++){
                			String richNumber = rcsNumbers.get(i);
                			int richContactId = ContactUtils.getContactId(ContactsListActivity.this, richNumber);
                			if (richContactId!=-1){
                				// It may be -1 if contact is still in EAB database but its number was deleted in address book
                				
                				// Put their info in the rcs matrix cursor
                				Cursor rcsPerson = managedQuery(Uri.withAppendedPath(android.provider.ContactsContract.Contacts.CONTENT_URI, ""+richContactId),
                						CONTACTS_PROJECTION,
                						null,
                						null,
                						null);


                				if (rcsPerson.moveToFirst()){
                					// Store id
                					rcsContactsId.add(rcsPerson.getInt(0));
                					rcsMatrixCursor.addRow(new Object[]{richContactId,
                							rcsPerson.getString(1),
                							rcsPerson.getInt(2),
                							rcsPerson.getInt(3),
                							rcsPerson.getString(4),
                							rcsPerson.getLong(5),
                							rcsPerson.getString(6),
                							rcsPerson.getString(7)});
                				}
                				rcsPerson.close();
                			}
                		}
                	}
                	
                	// Get contacts that are starred                	
                	Cursor starredCursor = cursor;
                	
                	MatrixCursor starredMatrixCursor = new MatrixCursor(CONTACTS_PROJECTION);
                	
                	while (starredCursor.moveToNext()){
                		// Put it in the cursor, except if it is already in the rcs cursor
                		if (!rcsContactsId.contains(starredCursor.getInt(0))){
                			// New API
                			starredMatrixCursor.addRow(new Object[]{starredCursor.getInt(0),
                					starredCursor.getString(1),
                					starredCursor.getInt(2),
                					starredCursor.getInt(3),
                					starredCursor.getString(4),
                					starredCursor.getLong(5),
                					starredCursor.getString(6),
                					starredCursor.getString(7)});
                		}
                	}
                	starredCursor.close();
                	
                	SortCursor sortCursor = new SortCursor(new Cursor[]{rcsMatrixCursor, starredMatrixCursor}, People.DISPLAY_NAME);
                	
                	activity.mAdapter.setLoading(false);
                	activity.getListView().clearTextFilter();
                	activity.mAdapter.changeCursor(sortCursor);
                }else if(mMode == MODE_PHOTO_SHARE_VIA_FILE_TRANSFERT){
                	// Get numbers that have rich data
                	List<String> rcsNumbers = null;
                	
                	ContactsApi contactsApi = new ContactsApi(activity);
                	rcsNumbers = contactsApi.getRcsContacts();

                	String[] CONTACTS_PROJECTION = new String[] {
                				android.provider.ContactsContract.Contacts._ID, // 0
                				android.provider.ContactsContract.Contacts.DISPLAY_NAME, // 1
                				android.provider.ContactsContract.Contacts.STARRED, //2
                				android.provider.ContactsContract.Contacts.TIMES_CONTACTED, //3
                				android.provider.ContactsContract.Contacts.CONTACT_PRESENCE, //4
                				android.provider.ContactsContract.Contacts.PHOTO_ID, //5
                				android.provider.ContactsContract.Contacts.LOOKUP_KEY, //6
                				android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER //7
                		};
                	
                	MatrixCursor rcsMatrixCursor = new MatrixCursor(CONTACTS_PROJECTION);
                	// We have to store the rcs contacts ids : if the contact is in the RCS cursor, 
                	// there will be no need to put it also in the starred cursor (we have to avoid double entries)
                	ArrayList<Integer>rcsContactsId = new ArrayList<Integer>();
                	if (rcsNumbers!=null){
                		// rcsNumber may be null if RCS core has not been installed
                    	for (int i=0; i<rcsNumbers.size();i++){
                			String richNumber = rcsNumbers.get(i);
                			int richContactId = ContactUtils.getContactId(ContactsListActivity.this,richNumber);
                			if (richContactId!=-1){
                				// It may be -1 if contact is still in EAB database but its number was deleted in address book
                				
                				// Put their info in the rcs matrix cursor
                				Cursor rcsPerson = managedQuery(Uri.withAppendedPath(android.provider.ContactsContract.Contacts.CONTENT_URI, ""+richContactId),
                						CONTACTS_PROJECTION,
                						null,
                						null,
                						null);


                				if (rcsPerson.moveToFirst()){
                					// Store id
                					rcsContactsId.add(rcsPerson.getInt(0));
                					rcsMatrixCursor.addRow(new Object[]{richContactId,
                							rcsPerson.getString(1),
                							rcsPerson.getInt(2),
                							rcsPerson.getInt(3),
                							rcsPerson.getString(4),
                							rcsPerson.getLong(5),
                							rcsPerson.getString(6),
                							rcsPerson.getString(7)});
                				}
                				rcsPerson.close();
                			}
                		}
                	}
                	activity.mAdapter.setLoading(false);
                	activity.getListView().clearTextFilter();
                	activity.mAdapter.changeCursor(rcsMatrixCursor);
                	if(rcsMatrixCursor.getCount()==0){
                		AlertDialog alert = new AlertDialog.Builder(ContactsListActivity.this)
                		.setIcon(R.drawable.rcs_common_flower_50x50)
            	        .setTitle(R.string.rcs_eab_title_dialog_filetransfer_from_gallery_nocontacts)
            	        .setCancelable(false)
            	        .setNegativeButton(R.string.rcs_eab_button_dialog_filetransfer_from_gallery_quit, new DialogInterface.OnClickListener() {
            	            public void onClick(DialogInterface dialog, int whichButton) {
            					// Exit activity
            	    			finish();
            	            }})
                		.show();
                	}             	
                }else{
                	// Normal query, do not change cursor
                	activity.mAdapter.setLoading(false);
                    activity.getListView().clearTextFilter();                
                    activity.mAdapter.changeCursor(cursor);
                }
                
                // Now that the cursor is populated again, it's possible to restore the list state
                if (activity.mListState != null) {
                    activity.getListView().onRestoreInstanceState(activity.mListState);
                    if (activity.mListHasFocus) {
                        activity.getListView().requestFocus();
                    }
                    activity.mListHasFocus = false;
                    activity.mListState = null;
                }
            } else {
                cursor.close();
            }
        }
    }

    final static class ContactListItemCache {
        public TextView nameView;
        public CharArrayBuffer nameBuffer = new CharArrayBuffer(128);
        public View header;
        public TextView headerText;
        public TextView freeTextView;
        public QuickContactBadge iconView;
        public ImageView contactStatusView;
        public ImageView hyperAvailabilityView;
    }
    
    final static class PhotoInfo {
        public int position;
        // Photo uri
        public Uri photoUri;
        public boolean isRcsPhoto = false;

        public PhotoInfo(int position, Uri photoUri, boolean isRcsPhoto) {
            this.position = position;
            this.photoUri = photoUri;
            this.isRcsPhoto = isRcsPhoto;
        }
    }

    private final class ContactItemListAdapter extends ResourceCursorAdapter
    implements SectionIndexer, OnScrollListener {
    	private SectionIndexer mIndexer;
    	private String mAlphabet;
        private Context mContext;
    	private boolean mLoading = true;
    	private CharSequence mUnknownNameText;
    	private boolean mDisplayPhotos = false;
    	private HashMap<Uri, SoftReference<Bitmap>> mBitmapCache = null;
    	private HashSet<ImageView> mItemsMissingImages = null;
    	private int mFrequentSeparatorPos = ListView.INVALID_POSITION;
    	private boolean mDisplaySectionHeaders = true;
    	private int[] mSectionPositions;
    	private Cursor mSuggestionsCursor;
    	private int mSuggestionsCursorCount;
    	private ImageFetchHandler mHandler;
    	private ImageDbFetcher mImageFetcher;
    	private static final int FETCH_IMAGE_MSG = 1;
    	private int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;

    	public ContactItemListAdapter(Context context) {
    		super(context, R.layout.contacts_list_item, null, false);

    		this.mContext = context;
    		mHandler = new ImageFetchHandler();
    		mAlphabet = context.getString(R.string.rcs_eab_fast_scroll_alphabet);

    		mUnknownNameText = context.getText(android.R.string.unknownName);
    		switch (mMode) {
    		case MODE_PICK_POSTAL:
    			mDisplaySectionHeaders = false;
    			break;
    		default:
    			break;
    		}

    		mDisplayPhotos = true;
    		setViewResource(R.layout.contacts_list_item);
    		mBitmapCache = new HashMap<Uri, SoftReference<Bitmap>>();
    		mItemsMissingImages = new HashSet<ImageView>();

    		if (mMode == MODE_STREQUENT || mMode == MODE_FREQUENT) {
    			mDisplaySectionHeaders = false;
    		}
    	}

    	private class ImageFetchHandler extends Handler {

    		@Override
    		public void handleMessage(Message message) {
    			if (ContactsListActivity.this.isFinishing()) {
    				return;
    			}
    			switch(message.what) {
    			case FETCH_IMAGE_MSG: {
    				final ImageView imageView = (ImageView) message.obj;
    				if (imageView == null) {
    					break;
    				}

    				final PhotoInfo info = (PhotoInfo)imageView.getTag();
    				if (info == null) {
    					break;
    				}

    				final Uri photoUri = info.photoUri;
    				if (photoUri== null) {
    					// No photo, no need to go further
    					break;
    				}

    				SoftReference<Bitmap> photoRef = mBitmapCache.get(photoUri);
    				if (photoRef == null) {
    					break;
    				}
    				Bitmap photo = photoRef.get();
    				if (photo == null) {
    					mBitmapCache.remove(photoUri);
    					break;
    				}

    				// Make sure the photoId on this image view has not changed
    				// while we were loading the image.
    				synchronized (imageView) {
    					final PhotoInfo updatedInfo = (PhotoInfo)imageView.getTag();
    					Uri currentPhotoUri = updatedInfo.photoUri;
    					if (currentPhotoUri == photoUri) {
    						imageView.setImageBitmap(photo);
    						mItemsMissingImages.remove(imageView);
    					}
    				}
    				break;
    			}
    			}
    		}

    		public void clearImageFecthing() {
    			removeMessages(FETCH_IMAGE_MSG);
    		}
    	}

    	private class ImageDbFetcher implements Runnable {
    		Uri mPhotoUri;
    		boolean isRcsPhoto;
    		private ImageView mImageView;

    		public ImageDbFetcher(Uri photoUri, boolean isRcsPhoto, ImageView imageView) {
    			this.mPhotoUri = photoUri;
    			this.isRcsPhoto = isRcsPhoto;
    			this.mImageView = imageView;
    		}

    		public void run() {
    			if (ContactsListActivity.this.isFinishing()) {
    				return;
    			}

    			if (Thread.interrupted()) {
    				// shutdown has been called.
    				return;
    			}
    			Bitmap photo = null;
    			if (isRcsPhoto){
    				// Get the rcs photo uri
    				try {
						photo = BitmapFactory.decodeStream(getContentResolver().openInputStream(mPhotoUri));
					} catch (FileNotFoundException e) {
        				// Could not decode the RCS photo, do nothing.
					}
    			}else if (mPhotoUri!=null){
    				// Get the address book photo uri
        			try {
        				InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(getContentResolver(), mPhotoUri);  
        				if (input == null) {  
        					photo = BitmapFactory.decodeResource(getResources(), R.drawable.ic_contact_picture);  
        				}else{
        					photo = BitmapFactory.decodeStream(input);
        				}
        			} catch (OutOfMemoryError e) {
        				// Not enough memory for the photo, do nothing.
        			}
    			}

    			if (photo == null) {
    				return;
    			}

    			mBitmapCache.put(mPhotoUri, new SoftReference<Bitmap>(photo));

    			if (Thread.interrupted()) {
    				// shutdown has been called.
    				return;
    			}

    			// Update must happen on UI thread
    			Message msg = new Message();
    			msg.what = FETCH_IMAGE_MSG;
    			msg.obj = mImageView;
    			mHandler.sendMessage(msg);
    		}
    	}

    	public void setSuggestionsCursor(Cursor cursor) {
    		if (mSuggestionsCursor != null) {
    			mSuggestionsCursor.close();
    		}
    		mSuggestionsCursor = cursor;
    		mSuggestionsCursorCount = cursor == null ? 0 : cursor.getCount();
    	}

    	private SectionIndexer getNewIndexer(Cursor cursor) {
    		// if (Locale.getDefault().getLanguage().equals(Locale.JAPAN.getLanguage())) {
        	//		return new JapaneseContactListIndexer(cursor, SORT_STRING_INDEX);
    		// } else { 
    		return new AlphabetIndexer(cursor, SUMMARY_NAME_COLUMN_INDEX, mAlphabet);
    		// } 
    	}

    	/**
    	 * Callback on the UI thread when the content observer on the backing cursor fires.
    	 * Instead of calling requery we need to do an async query so that the requery doesn't
    	 * block the UI thread for a long time.
    	 */
    	@Override
    	protected void onContentChanged() {
    		
    		CharSequence constraint = getListView().getTextFilter();
    		if (!TextUtils.isEmpty(constraint)) {
    			// Reset the filter state then start an async filter operation
    			Filter filter = getFilter();
    			filter.filter(constraint);
    		} else {
    			// Start an async query
//    			startQuery();
    		}
    	}

    	public void setLoading(boolean loading) {
    		mLoading = loading;
    	}

    	@Override
    	public boolean isEmpty() {
    		if ((mMode & MODE_MASK_CREATE_NEW) == MODE_MASK_CREATE_NEW) {
    			// This mode mask adds a header and we always want it to show up, even
    			// if the list is empty, so always claim the list is not empty.
    			return false;
    		} else {
    			if (mLoading || mMode!=MODE_STARRED) {
    				// We don't want the empty state to show when loading 
    				// or when it is not the starred mode (in the latter case, we would not see the "My Profile" entry)
    				return false;
    			} else {
    				return super.isEmpty();
    			}
    		}
    	}

    	@Override
    	public int getItemViewType(int position) {
    		if (position == 0) {
    			return IGNORE_ITEM_VIEW_TYPE;
    		}
    		if (getSeparatorId(position) != 0) {
    			// We don't want the separator view to be recycled.
    			return IGNORE_ITEM_VIEW_TYPE;
    		}
    		return super.getItemViewType(position);
    	}

    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {

    		// Handle the separator specially
    		int separatorId = getSeparatorId(position);
    		if (separatorId != 0) {
    			LayoutInflater inflater =
    				(LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    			TextView view = (TextView) inflater.inflate(R.layout.list_separator, parent, false);
    			view.setText(separatorId);
    			return view;
    		}

    		boolean showingSuggestion;
    		Cursor cursor;
    		if (mSuggestionsCursorCount != 0 && position < mSuggestionsCursorCount + 2) {
    			showingSuggestion = true;
    			cursor = mSuggestionsCursor;
    		} else {
    			showingSuggestion = false;
    			cursor = getCursor();
    		}

    		int realPosition = getRealPosition(position);
    		if (!cursor.moveToPosition(realPosition)) {
    			throw new IllegalStateException("couldn't move cursor to position " + position);
    		}

    		View v;
    		if (convertView == null) {
    			v = newView(mContext, cursor, parent);
    		} else {
    			v = convertView;
    		}
    		bindView(v, mContext, cursor);
    		bindSectionHeader(v, realPosition, mDisplaySectionHeaders && !showingSuggestion);
    		return v;
    	}

    	private int getSeparatorId(int position) {
    		int separatorId = 0;
    		if (position == mFrequentSeparatorPos) {
    			separatorId = R.string.favoritesFrquentSeparator;
    		}
    		if (mSuggestionsCursorCount != 0) {
    			if (position == 0) {
    				separatorId = R.string.separatorJoinAggregateSuggestions;
    			} else if (position == mSuggestionsCursorCount + 1) {
    				separatorId = R.string.separatorJoinAggregateAll;
    			}
    		}
    		return separatorId;
    	}

    	@Override
    	public View newView(Context context, Cursor cursor, ViewGroup parent) {
    		final View view = super.newView(context, cursor, parent);

    		final ContactListItemCache cache = new ContactListItemCache();
    		cache.header = view.findViewById(R.id.header);
    		cache.headerText = (TextView)view.findViewById(R.id.header_text);
    		cache.nameView = (TextView) view.findViewById(R.id.name);
    		cache.freeTextView = (TextView) view.findViewById(R.id.freeTxt);
    		cache.contactStatusView = (ImageView) view.findViewById(R.id.contact_status);
    		cache.iconView = (QuickContactBadge) view.findViewById(R.id.icon);
    		cache.hyperAvailabilityView = (ImageView) view.findViewById(R.id.availability);
    		view.setTag(cache);

    		return view;
    	}

    	@Override
    	public void bindView(View view, Context context, Cursor cursor) {
    		final ContactListItemCache cache = (ContactListItemCache) view.getTag();

    		int pos = cursor.getPosition();
            // Id person always first column
            int idPerson = cursor.getInt(0);
            // Lookup key
            final String lookupKey = cursor.getString(6);

            // Assign uri to quickContactBadge
            cache.iconView.assignContactUri(Contacts.getLookupUri(idPerson, lookupKey));
            if (imsConnected){
            	cache.iconView.setExcludeMimes(new String[]{});
            }else{
            	cache.iconView.setExcludeMimes(contactsApi.getRcsMimeTypes());
            }
            
            // Set the name
            cursor.copyStringToBuffer(SUMMARY_NAME_COLUMN_INDEX, cache.nameBuffer);
            int size = cache.nameBuffer.sizeCopied;
            if (size != 0) {
            	cache.nameView.setText(cache.nameBuffer.data, 0, size);
            } else {
            	cache.nameView.setText(mUnknownNameText);
            }

            ArrayList<PhoneNumber> rcsPhoneNumbers = ContactUtils.getRcsContactNumbers(context, idPerson);
            
            ContactInfo mostRecentRcsInfo = null;
            long mostRecentTimestamp = 0;
			boolean isAvailable = false;
			boolean isRcsContact = false;
            // Search for the most recently updated number
            for (int i=0;i<rcsPhoneNumbers.size();i++){
            	isRcsContact = true;
            	String checkedNumber = rcsPhoneNumbers.get(i).number;
            	ContactsApi contactsApi = new ContactsApi(ContactsListActivity.this);
            	ContactInfo info = contactsApi.getContactInfo(checkedNumber);
            	if (info.getRegistrationState()==ContactInfo.REGISTRATION_STATUS_ONLINE && imsConnected){
                	isAvailable = true;
            	}
                long checkedTimestamp = info.getRcsStatusTimestamp();
                if (info.getRcsStatusTimestamp()>mostRecentTimestamp){
                	//This is the most recent profile for the moment
                	mostRecentTimestamp = checkedTimestamp;
                	mostRecentRcsInfo = info;
                }
            }
            
            // Get presence info
            Uri photoUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, idPerson);;
			boolean isRcsPhoto = false;
			if (mostRecentRcsInfo!=null && mostRecentRcsInfo.getRcsStatus()==ContactInfo.RCS_ACTIVE){
				// contact is RCS
				isRcsContact = true;
				// Get availability
				isAvailable = mostRecentRcsInfo.getPresenceInfo().isOnline();
				// Get free text
				String freeText = mostRecentRcsInfo.getPresenceInfo().getFreetext();
				
				cache.freeTextView.setVisibility(View.VISIBLE);
				cache.freeTextView.setText(freeText);
				// Set unlimited animation
				cache.freeTextView.setMarqueeRepeatLimit(-1);
				cache.freeTextView.setEllipsize(TruncateAt.MARQUEE);
			}else{
				// Contact not RCS
				cache.freeTextView.setVisibility(View.GONE);
				cache.contactStatusView.setImageDrawable(null);
    			try{
    				int index = cursor.getColumnIndexOrThrow(People.STARRED);
    				if (cursor.getInt(index)==1){
    					// Contact is starred
    					cache.contactStatusView.setImageResource(R.drawable.rcs_eab_starred_on);
    				}else{
    					cache.contactStatusView.setImageDrawable(null);
    				}
    			}catch(IllegalArgumentException iae){
    			}   
    			cache.hyperAvailabilityView.setVisibility(View.INVISIBLE);
			}

            // Check normal address book contact photo
            cache.iconView.setTag(new PhotoInfo(pos, photoUri, isRcsPhoto));

            if (photoUri == null) {
            	cache.iconView.setImageResource(R.drawable.rcs_default_portrait_50x50);
            } else {
            	Bitmap photo = null;
            	// Look for the cached bitmap
            	SoftReference<Bitmap> ref = mBitmapCache.get(photoUri);
            	if (ref != null) {
            		photo = ref.get();
            		if (photo == null) {
            			mBitmapCache.remove(photoUri);
            		}
            	}

            	// Bind the photo, or use the fallback no photo resource
            	if (photo != null) {
            		cache.iconView.setImageBitmap(photo);
            	} else {
            		// Cache miss
            		if (isRcsContact){
            			cache.iconView.setImageResource(R.drawable.rcs_default_portrait_50x50);
            		}else{
            			cache.iconView.setImageResource(R.drawable.ic_contact_picture);
            		}
            			
            		// Add it to a set of images that are populated asynchronously.
            		mItemsMissingImages.add(cache.iconView);

            		if (mScrollState != OnScrollListener.SCROLL_STATE_FLING) {
            			// Scrolling is idle or slow, go get the image right now.
            			sendFetchImageMessage(cache.iconView);
            		}
            	}
            }
            

			// Set contact status
			if (isRcsContact){
				// Contact is RCS
				cache.contactStatusView.setImageResource(R.drawable.rcs_flower_black_edge_18x18);
			}			
			
            // Set availability visibility at last, after photo has been set, 
            // will be drawn before photo, so not visible by the user
            if (isAvailable){
            	cache.hyperAvailabilityView.setVisibility(View.VISIBLE);
            	cache.hyperAvailabilityView.setImageResource(R.drawable.rcs_eab_presence_online);
            }else{
            	cache.hyperAvailabilityView.setVisibility(View.INVISIBLE);
            	cache.hyperAvailabilityView.setImageResource(R.drawable.rcs_eab_presence_offline);
            }
    	}

    	private void bindSectionHeader(View view, int position, boolean displaySectionHeaders) {
    		final ContactListItemCache cache = (ContactListItemCache) view.getTag();
    		if (!displaySectionHeaders) {
    			cache.header.setVisibility(View.GONE);
    		} else {
    			final int section = getSectionForPosition(position);
    			if (getPositionForSection(section) == position) {
    				String title = mIndexer.getSections()[section].toString().trim();
    				
    				// Even if the title is empty, we put an empty header separator
    				// This is because we do not want the first section (with the non alpha names) directly 
    				// under the "Me" entry
    				
//    				if (!TextUtils.isEmpty(title)) {
    					cache.headerText.setText(title);
    					cache.header.setVisibility(View.VISIBLE);
//    				} else {
//    					cache.header.setVisibility(View.GONE);
//    				}
    			} else {
    				cache.header.setVisibility(View.GONE);
    			}

    		}
    	}

    	@Override
    	public void changeCursor(Cursor cursor) {

    		// Get the split between starred and frequent items, if the mode is strequent
    		mFrequentSeparatorPos = ListView.INVALID_POSITION;
    		if (cursor != null && (cursor.getCount()) > 0
    				&& mMode == MODE_STREQUENT) {
    			cursor.move(-1);
    			for (int i = 0; cursor.moveToNext(); i++) {
    				int starred = cursor.getInt(SUMMARY_STARRED_COLUMN_INDEX);
    				if (starred == 0) {
    					if (i > 0) {
    						// Only add the separator when there are starred items present
    						mFrequentSeparatorPos = i;
    					}
    					break;
    				}
    			}
    		}

    		super.changeCursor(cursor);
    		// Update the indexer for the fast scroll widget
    		updateIndexer(cursor);
    	}

    	private void updateIndexer(Cursor cursor) {
    		if (mIndexer == null) {
    			mIndexer = getNewIndexer(cursor);
    		} else {
    			if (Locale.getDefault().equals(Locale.JAPAN)) {
    				if (mIndexer instanceof JapaneseContactListIndexer) {
    					((JapaneseContactListIndexer)mIndexer).setCursor(cursor);
    				} else {
    					mIndexer = getNewIndexer(cursor);
    				}
    			} else {
    				if (mIndexer instanceof AlphabetIndexer) {
    					((AlphabetIndexer)mIndexer).setCursor(cursor);
    				} else {
    					mIndexer = getNewIndexer(cursor);
    				}
    			}
    		}

    		int sectionCount = mIndexer.getSections().length;
    		if (mSectionPositions == null || mSectionPositions.length != sectionCount) {
    			mSectionPositions = new int[sectionCount];
    		}
    		for (int i = 0; i < sectionCount; i++) {
    			mSectionPositions[i] = ListView.INVALID_POSITION;
    		}
    	}

    	/**
    	 * Run the query on a helper thread. Beware that this code does not run
    	 * on the main UI thread!
    	 */
    	@Override
    	public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
    		return doFilter(constraint.toString());
    	}

    	public Object [] getSections() {
    		if (mMode == MODE_STARRED) {
    			return new String[] { " " };
    		} else {
    			return mIndexer.getSections();
    		}
    	}

    	public int getPositionForSection(int sectionIndex) {
    		if (mMode == MODE_STARRED) {
    			return -1;
    		}

    		if (sectionIndex < 0 || sectionIndex >= mSectionPositions.length) {
    			return -1;
    		}

    		if (mIndexer == null) {
    			Cursor cursor = mAdapter.getCursor();
    			if (cursor == null) {
    				// No cursor, the section doesn't exist so just return 0
    				return 0;
    			}
    			mIndexer = getNewIndexer(cursor);
    		}

    		int position = mSectionPositions[sectionIndex];
    		if (position == ListView.INVALID_POSITION) {
    			position = mSectionPositions[sectionIndex] =
    				mIndexer.getPositionForSection(sectionIndex);
    		}

    		return position;
    	}

    	public int getSectionForPosition(int position) {
    		// The current implementations of SectionIndexers (specifically the Japanese indexer)
    		// only work in one direction: given a section they can calculate the position.
    		// Here we are using that existing functionality to do the reverse mapping. We are
    		// performing binary search in the mSectionPositions array, which itself is populated
    		// lazily using the "forward" mapping supported by the indexer.

    		int start = 0;
    		int end = mSectionPositions.length;
    		while (start != end) {

    			// We are making the binary search slightly asymmetrical, because the
    			// user is more likely to be scrolling the list from the top down.
    			int pivot = start + (end - start) / 4;

    			int value = getPositionForSection(pivot);
    			if (value <= position) {
    				start = pivot + 1;
    			} else {
    				end = pivot;
    			}
    		}

    		// The variable "start" cannot be 0, as long as the indexer is implemented properly
    		// and actually maps position = 0 to section = 0
    		return start - 1;
    	}

    	@Override
    	public boolean areAllItemsEnabled() {
    		return mMode != MODE_STARRED
    		&& mSuggestionsCursorCount == 0;
    	}

    	@Override
    	public boolean isEnabled(int position) {
    			
    		if (mSuggestionsCursorCount > 0) {
    			return position != 0 && position != mSuggestionsCursorCount + 1;
    		}
    		return position != mFrequentSeparatorPos;
    	}

    	@Override
    	public int getCount() {
    		int superCount = super.getCount();
    			
    		if (mSuggestionsCursorCount != 0) {
    			// When showing suggestions, we have 2 additional list items: the "Suggestions"
    			// and "All contacts" headers.
    			return mSuggestionsCursorCount + superCount + 2;
    		}
    		else if (mFrequentSeparatorPos != ListView.INVALID_POSITION) {
    			// When showing strequent list, we have an additional list item - the separator.
    			return superCount + 1;
    		} else {
    			return superCount;
    		}
    	}

    	private int getRealPosition(int pos) {
    			
    		if (mSuggestionsCursorCount != 0) {
    			// When showing suggestions, we have 2 additional list items: the "Suggestions"
    			// and "All contacts" separators.
    			if (pos < mSuggestionsCursorCount + 2) {
    				// We are in the upper partition (Suggestions). Adjusting for the "Suggestions"
    				// separator.
    				return pos - 1;
    			} else {
    				// We are in the lower partition (All contacts). Adjusting for the size
    				// of the upper partition plus the two separators.
    				return pos - mSuggestionsCursorCount - 2;
    			}
    		} else if (mFrequentSeparatorPos == ListView.INVALID_POSITION) {
    			// No separator, identity map
    			return pos;
    		} else if (pos <= mFrequentSeparatorPos) {
    			// Before or at the separator, identity map
    			return pos;
    		} else {
    			// After the separator, remove 1 from the pos to get the real underlying pos
    			return pos - 1;
    		}
    	}

    	@Override
    	public Object getItem(int pos) {
    		if (mSuggestionsCursorCount != 0 && pos <= mSuggestionsCursorCount) {
    			mSuggestionsCursor.moveToPosition(getRealPosition(pos));
    			return mSuggestionsCursor;
    		} else {
    			return super.getItem(getRealPosition(pos));
    		}
    	}

    	@Override
    	public long getItemId(int pos) {
    		if (mSuggestionsCursorCount != 0 && pos < mSuggestionsCursorCount + 2) {
    			if (mSuggestionsCursor.moveToPosition(pos - 1)) {
    				return mSuggestionsCursor.getLong(0);
    			} else {
    				return 0;
    			}
    		}
    		return super.getItemId(getRealPosition(pos));
    	}

    	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
    			int totalItemCount) {
    		// no op
    	}

    	public void onScrollStateChanged(AbsListView view, int scrollState) {
    		mScrollState = scrollState;
    		if (scrollState == OnScrollListener.SCROLL_STATE_FLING) {
    			// If we are in a fling, stop loading images.
    			clearImageFetching();
    		} else if (mDisplayPhotos) {
    			processMissingImageItems(view);
    		}
    	}

    	private void processMissingImageItems(AbsListView view) {
    		for (ImageView iv : mItemsMissingImages) {
    			sendFetchImageMessage(iv);
    		}
    	}

    	private void sendFetchImageMessage(ImageView view) {
    		final PhotoInfo info = (PhotoInfo) view.getTag();
    		if (info == null) {
    			return;
    		}
    		final Uri photoUri = info.photoUri;
    		if (photoUri == null) {
    			return;
    		}
    		mImageFetcher = new ImageDbFetcher(photoUri, info.isRcsPhoto, view);
    		synchronized (ContactsListActivity.this) {
    			// can't sync on sImageFetchThreadPool.
    			if (sImageFetchThreadPool == null) {
    				// Don't use more than 3 threads at a time to update. The thread pool will be
    				// shared by all contact items.
    				sImageFetchThreadPool = Executors.newFixedThreadPool(3);
    			}
    			sImageFetchThreadPool.execute(mImageFetcher);
    		}
    	}


    	/**
    	 * Stop the image fetching for ALL contacts, if one is in progress we'll
    	 * not query the database.
    	 */
    	public void clearImageFetching() {
    		synchronized (ContactsListActivity.this) {
    			if (sImageFetchThreadPool != null) {
    				sImageFetchThreadPool.shutdownNow();
    				sImageFetchThreadPool = null;
    			}
    		}

    		mHandler.clearImageFecthing();
    	}
    }
    
	/**
	 * Read my profile from Eab provider and update the fields in layout
	 */
    private void readMyProfile() {

    	ImageView photoView = (ImageView) myProfileView.findViewById(R.id.icon);
    	ImageView hyperavailabilityView = (ImageView) myProfileView.findViewById(R.id.availability);
    	TextView freeTextView = (TextView) myProfileView.findViewById(R.id.freeTxt);
    	ImageView contactStatusView = (ImageView) myProfileView.findViewById(R.id.contact_status);

    	boolean isAvailable = false;
    	PresenceInfo info = null;
    	info = presenceApi.getMyPresenceInfo();
        if (info==null){
        	// Core may be not present on phone
        	hyperavailabilityView.setVisibility(View.INVISIBLE);
        	photoView.setImageResource(R.drawable.rcs_default_portrait_50x50);
        }else{
        	isAvailable = info.isOnline();
	
	    	if (isAvailable){
	    		hyperavailabilityView.setVisibility(View.VISIBLE);
	    	}else{
	    		hyperavailabilityView.setVisibility(View.INVISIBLE);
	    	}
	
	    	freeTextView.setVisibility(View.VISIBLE);
	    	freeTextView.setText(info.getFreetext());
			freeTextView.setMarqueeRepeatLimit(-1);
			freeTextView.setEllipsize(TruncateAt.MARQUEE);
	    	
	    	Bitmap tempIcon = null;
	    	// Get presence icon
	    	PhotoIcon photoIcon = info.getPhotoIcon();
	    	if (photoIcon != null) {
	    		byte[] data = info.getPhotoIcon().getContent();
	    		tempIcon = BitmapFactory.decodeByteArray(data, 0, data.length);
	    	}
	    	if (tempIcon!=null){
	    		photoView.setImageBitmap(tempIcon);
	    	}else{
	    		photoView.setImageResource(R.drawable.rcs_default_portrait_50x50);
	    	}

	    	// set the contact status : "Me" can be considered a RCS profile
	    	contactStatusView.setImageResource(R.drawable.rcs_flower_black_edge_18x18);
        }
    }
    
    
	private final BroadcastReceiver myPresenceInfoChangedIntentReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
	        if (RcsSettings.getInstance()==null){
	        	RcsSettings.createInstance(context);
	        }
			if (mMode!=MODE_STARRED && RcsSettings.getInstance().isSocialPresenceSupported()){
				mHandler.post(new Runnable(){
					public void run(){
						readMyProfile();
					}
				});
			}
		}
	};

	private final BroadcastReceiver contactPhotoChangedIntentReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			mHandler.post(new Runnable(){
				public void run(){
					// Clear images caches, or the photos will not be redrawn
					mAdapter.mBitmapCache.clear();
					mAdapter.mItemsMissingImages.clear();
					mAdapter.notifyDataSetChanged();
				}
			});
		}
	};

	private final BroadcastReceiver contactPresenceInfoChangedIntentReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			mHandler.post(new Runnable(){
				public void run(){
					mAdapter.notifyDataSetChanged();
				}
			});
		}
	};
	
	private final BroadcastReceiver contactCapabilitiesChangedIntentReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			mHandler.post(new Runnable(){
				public void run(){
					mAdapter.notifyDataSetChanged();
				}
			});
		}
	};

	private final BroadcastReceiver presenceSharingChangedIntentReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, final Intent intent) {
			mHandler.post(new Runnable(){
				public void run(){
			    	String status = intent.getStringExtra("status");
			    	String reason = intent.getStringExtra("reason");
			    	
					if (status!=null && status.equalsIgnoreCase("terminated")
							&& reason!=null && reason.equalsIgnoreCase("rejected")){
						// On a revocation, we need to clear images caches, or the photo will not be removed
						mAdapter.mBitmapCache.clear();
						mAdapter.mItemsMissingImages.clear();
					}
					mAdapter.notifyDataSetChanged();
				}
			});
		}
	};
	
}
