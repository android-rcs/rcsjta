/*******************************************************************************
 * Software Name : RCS
 *
 * Copyright © 2010 France Telecom S.A.
 ******************************************************************************/

package com.orangelabs.rcs.eab;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Contacts;
import android.provider.Contacts.Phones;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.style.UnderlineSpan;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import com.orangelabs.rcs.common.ContactUtils;
import com.orangelabs.rcs.common.DateUtils;
import com.orangelabs.rcs.common.EabUtils;
import com.orangelabs.rcs.common.IntentUtils;
import com.orangelabs.rcs.common.PhoneNumber;
import com.orangelabs.rcs.common.UrlUtils;
import com.orangelabs.rcs.common.dialog.DialogUtils;
import com.orangelabs.rcs.common.dialog.EabDialogUtils;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.api.client.ClientApiException;
import com.orangelabs.rcs.service.api.client.ClientApiListener;
import com.orangelabs.rcs.service.api.client.ImsEventListener;
import com.orangelabs.rcs.service.api.client.capability.CapabilityApi;
import com.orangelabs.rcs.service.api.client.capability.CapabilityApiIntents;
import com.orangelabs.rcs.service.api.client.contacts.ContactInfo;
import com.orangelabs.rcs.service.api.client.contacts.ContactsApi;
import com.orangelabs.rcs.service.api.client.presence.PhotoIcon;
import com.orangelabs.rcs.service.api.client.presence.PresenceApi;
import com.orangelabs.rcs.service.api.client.presence.PresenceApiIntents;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Displays the details of a specific contact.
 * 
 * <br>Also displays RCS infos, and RCS actions are available depending to contact capabilities and activities installed on phone.
 */
public class ViewContactActivity extends ListActivity 
        implements View.OnCreateContextMenuListener, View.OnClickListener,
        DialogInterface.OnClickListener, ClientApiListener, ImsEventListener {
    private static final String SHOW_BARCODE_INTENT = "com.google.zxing.client.android.ENCODE";

    private static final boolean SHOW_SEPARATORS = false;
    
    private static final String[] PHONE_KEYS = {
        Contacts.Intents.Insert.PHONE,
        Contacts.Intents.Insert.SECONDARY_PHONE,
        Contacts.Intents.Insert.TERTIARY_PHONE
    };

    private static final String[] EMAIL_KEYS = {
        Contacts.Intents.Insert.EMAIL,
        Contacts.Intents.Insert.SECONDARY_EMAIL,
        Contacts.Intents.Insert.TERTIARY_EMAIL
    };

    private static final int DIALOG_CONFIRM_DELETE = 1;
    
    public static final int MENU_ITEM_DELETE = 1;
    public static final int MENU_ITEM_MAKE_DEFAULT = 2;
    public static final int MENU_ITEM_SHOW_BARCODE = 3;

    private Uri mUri;
    private ContentResolver mResolver;
    private ViewAdapter mAdapter;
    private int mNumPhoneNumbers = 0;

    /* package */ ArrayList<ViewEntry> mPhoneEntries = new ArrayList<ViewEntry>();
    /* package */ ArrayList<ViewEntry> mSmsEntries = new ArrayList<ViewEntry>();
    /* package */ ArrayList<ViewEntry> mEmailEntries = new ArrayList<ViewEntry>();
    /* package */ ArrayList<ViewEntry> mPostalEntries = new ArrayList<ViewEntry>();
    /* package */ ArrayList<ViewEntry> mImEntries = new ArrayList<ViewEntry>();
    /* package */ ArrayList<ViewEntry> mOrganizationEntries = new ArrayList<ViewEntry>();
    /* package */ ArrayList<ViewEntry> mOtherEntries = new ArrayList<ViewEntry>();
    /* package */ ArrayList<ViewEntry> mCallLogEntries = new ArrayList<ViewEntry>();
    /* package */ ArrayList<ViewEntry> mManageProfileEntries = new ArrayList<ViewEntry>();
    /*Added for RCS *//* package */ ArrayList<ViewEntry> mRcsEntries = new ArrayList<ViewEntry>();
    /*Added for RCS *//* package */ ArrayList<ViewEntry> mRcsProfileEntries = new ArrayList<ViewEntry>();
    /* package */ ArrayList<ArrayList<ViewEntry>> mSections = new ArrayList<ArrayList<ViewEntry>>();

    // Launch code
    private static final int PROFILE_OPTIONS = 6541;
    
    private Cursor mCursor;
    private boolean mObserverRegistered;
    
    private long contactId;
    private boolean imsConnected = false;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private class ResultHandler extends Handler{
		
		public void handleMessage(Message msg){
			switch(msg.what){
			case EabDialogUtils.BLOCK_INVITATION_RESULT_OK :
				dataChanged();
				DialogUtils.displayToast(ViewContactActivity.this, this, getString(R.string.rcs_eab_blockContactSuccess));
				break;
			case EabDialogUtils.BLOCK_INVITATION_RESULT_KO :
				DialogUtils.displayToast(ViewContactActivity.this, this, getString(R.string.rcs_eab_blockContactError));
				break;
			case EabDialogUtils.UNBLOCK_INVITATION_RESULT_OK :
				dataChanged();
				DialogUtils.displayToast(ViewContactActivity.this, this, getString(R.string.rcs_eab_unblockContactSuccess));
				break;
			case EabDialogUtils.UNBLOCK_INVITATION_RESULT_KO :
				DialogUtils.displayToast(ViewContactActivity.this, this, getString(R.string.rcs_eab_unblockContactError));
				break;
			case EabDialogUtils.INITIATE_INVITATION_RESULT_OK :
				dataChanged();
				DialogUtils.displayToast(ViewContactActivity.this, this, getString(R.string.rcs_eab_inviteContactSuccess));
				break;
			case EabDialogUtils.INITIATE_INVITATION_RESULT_KO :
				DialogUtils.displayToast(ViewContactActivity.this, this, getString(R.string.rcs_eab_inviteContactError));
				break;
			case EabDialogUtils.REVOKE_INVITATION_RESULT_OK :
				dataChanged();
				DialogUtils.displayToast(ViewContactActivity.this, this, getString(R.string.rcs_eab_revokeContactSuccess));
				break;
			case EabDialogUtils.REVOKE_INVITATION_RESULT_KO :
				DialogUtils.displayToast(ViewContactActivity.this, this, getString(R.string.rcs_eab_inviteContactError));
				break;
			case EabDialogUtils.DELETE_CONTACT_RESULT_OK :
			case EabDialogUtils.DELETE_MULTIPLE_CONTACTS_RESULT_OK :
        		getContentResolver().delete(mUri, null, null);
        		DialogUtils.displayToast(ViewContactActivity.this, this, getString(R.string.rcs_eab_revokeContactSuccess));
        		break;
			case EabDialogUtils.DELETE_CONTACT_RESULT_KO :
			case EabDialogUtils.DELETE_MULTIPLE_CONTACTS_RESULT_KO :
        		getContentResolver().delete(mUri, null, null);
				DialogUtils.displayToast(ViewContactActivity.this, this, getString(R.string.rcs_eab_deleteRcsContactFailed));
				break;
			}
		}
	};
    
    private ContentObserver mObserver = new ContentObserver(new Handler()) {
         
        public boolean deliverSelfNotifications() {
            return true;
        }

        public void onChange(boolean selfChange) {
            if (mCursor != null && !mCursor.isClosed()){
                dataChanged();
            }
        }
    };

    public void onClick(DialogInterface dialog, int which) {
    	// Only dialog is delete contact
    	
        if (mCursor != null) {
            if (mObserverRegistered) {
                mCursor.unregisterContentObserver(mObserver);
                mObserverRegistered = false;
            }
            mCursor.close();
            mCursor = null;
        }
        // Delete contact
        getContentResolver().delete(mUri, null, null);
        finish();
    }

    public void onClick(View view) {
        if (!mObserverRegistered) {
            return;
        }
        switch (view.getId()) {
            case R.id.star: {
                int oldStarredState = mCursor.getInt(2);
                ContentValues values = new ContentValues(1);
                values.put(/*People.STARRED*/ContactsContract.Contacts.STARRED, oldStarredState == 1 ? 0 : 1);
                getContentResolver().update(mUri, values, null, null);
                break;
            }
        }
    }

    private TextView mNameView;
    private TextView mPhoneticNameView;  // may be null in some locales
    private QuickContactBadge mPhotoView;
    private int mNoPhotoResource;
    private CheckBox mStarView;
    private boolean mShowSmsLinksForAllPhones;

    private ImageView mRcsIndicatorView;
    private ImageView mPresentIndicatorView;

	private PresenceApi presenceApi = null;
	private ContactsApi contactsApi = null;
	private CapabilityApi capabilityApi = null;
	private final Handler handler = new ResultHandler();
     
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

		// Register intent receiver
		IntentFilter filter = new IntentFilter(PresenceApiIntents.CONTACT_INFO_CHANGED);
		registerReceiver(mContactInfosChangedIntentReceiver, filter, null, handler);
		filter = new IntentFilter(CapabilityApiIntents.CONTACT_CAPABILITIES);
		registerReceiver(mContactCapabilitiesChangedIntentReceiver, filter, null, handler);
		filter = new IntentFilter(PresenceApiIntents.CONTACT_PHOTO_CHANGED);
		registerReceiver(mContactPhotoChangedIntentReceiver, filter, null, handler);
		filter = new IntentFilter(PresenceApiIntents.PRESENCE_SHARING_CHANGED);
		registerReceiver(mContactStatusChangedIntentReceiver, filter, null, handler);
		
		
		setContentView(R.layout.view_contact);
        getListView().setOnCreateContextMenuListener(this);
        
        final Intent intent = getIntent();
        Uri data = intent.getData();
        String authority = data.getAuthority();

        if (ContactsContract.AUTHORITY.equals(authority)) {
        	mUri = data;
        } else if (android.provider.Contacts.AUTHORITY.equals(authority)) {
        	final long rawContactId = ContentUris.parseId(data);
        	mUri = RawContacts.getContactLookupUri(getContentResolver(),
        			ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId));
        }

        if (data!=null && data.getPathSegments().contains("data")){
        	// We come from custom mime-type
            // Select the corresponding contact from the intent
            Cursor cursor = managedQuery(data, null, null, null, null);
            if (cursor.moveToNext()) {
            	long contactId = cursor.getLong(cursor.getColumnIndex(Data.CONTACT_ID));
            	if (contactId!=-1){
            		mUri = ContentUris.withAppendedId(android.provider.ContactsContract.Contacts.CONTENT_URI, contactId);
            	}
            }
            cursor.close();
        }
        
        mNameView = (TextView) findViewById(R.id.name);
        mPhoneticNameView = (TextView) findViewById(R.id.phonetic_name);
        mPhotoView = (QuickContactBadge) findViewById(R.id.photo);

        mStarView = (CheckBox) findViewById(R.id.star);
        mStarView.setOnClickListener(this);

        mRcsIndicatorView = (ImageView) findViewById(R.id.rcs_indicator);
        mPresentIndicatorView = (ImageView) findViewById(R.id.rcs_present);

        // Instanciate APIs
        presenceApi = new PresenceApi(getApplicationContext());
		if (presenceApi!=null){
			presenceApi.connectApi();
		}
		
		capabilityApi = new CapabilityApi(getApplicationContext());
		capabilityApi.addApiEventListener(this);
		capabilityApi.addImsEventListener(this);
		capabilityApi.connectApi();
		
		contactsApi = new ContactsApi(getApplicationContext());
		
        // Set the photo with a random "no contact" image
        long now = SystemClock.elapsedRealtime();
        int num = (int) now & 0xf;
        if (num < 9) {
            // Leaning in from right, common
            mNoPhotoResource = R.drawable.ic_contact_picture;
        } else if (num < 14) {
            // Leaning in from left uncommon
            mNoPhotoResource = R.drawable.ic_contact_picture_2;
        } else {
            // Coming in from the top, rare
            mNoPhotoResource = R.drawable.ic_contact_picture_3;
        }

        mResolver = getContentResolver();

        // Build the list of sections. The order they're added to mSections dictates the
        // order they are displayed in the list.
        mSections.add(mRcsProfileEntries);

        mSections.add(mPhoneEntries);
        mSections.add(mSmsEntries);
        mSections.add(mEmailEntries);
        mSections.add(mImEntries);
        mSections.add(mPostalEntries);
        mSections.add(mOrganizationEntries);
        mSections.add(mOtherEntries);

        mSections.add(mCallLogEntries);
        mSections.add(mRcsEntries);
        mSections.add(mManageProfileEntries);
        
        mShowSmsLinksForAllPhones = true;
        
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

        mCursor = mResolver.query(mUri, CONTACTS_PROJECTION, null, null, null);

        dataChanged();
    }

     
    protected void onResume() {
        super.onResume();
        mObserverRegistered = true;
        mCursor.registerContentObserver(mObserver);
        dataChanged();
    }

     
    protected void onPause() {
        super.onPause();
        if (mCursor != null) {
            if (mObserverRegistered) {
                mObserverRegistered = false;
                mCursor.unregisterContentObserver(mObserver);
            }
            mCursor.deactivate();
        }
    }

    protected void onDestroy() {
		unregisterReceiver(mContactInfosChangedIntentReceiver);
		unregisterReceiver(mContactCapabilitiesChangedIntentReceiver);
		unregisterReceiver(mContactPhotoChangedIntentReceiver);
		unregisterReceiver(mContactStatusChangedIntentReceiver);

		if (mCursor != null) {
            if (mObserverRegistered) {
                mCursor.unregisterContentObserver(mObserver);
                mObserverRegistered = false;
            }
            mCursor.close();
        }

		if (presenceApi!=null){
			presenceApi.disconnectApi();
		}

		super.onDestroy();
    }

	protected void onSaveInstanceState(Bundle icicle) {
		super.onSaveInstanceState(icicle);
	}
	 
	protected void onRestoreInstanceState(Bundle icicle) {
		super.onRestoreInstanceState(icicle);
	}
     
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_CONFIRM_DELETE:
            	// There is no rcs phone number.
            	return new AlertDialog.Builder(this)
            	.setTitle(R.string.deleteConfirmation_title)
            	.setIcon(android.R.drawable.ic_dialog_alert)
            	.setMessage(R.string.deleteConfirmation)
            	.setNegativeButton(android.R.string.cancel, null)
            	.setPositiveButton(android.R.string.ok, this)
            	.setCancelable(false)
            	.create();
        }
        return null;
    }
    
    private void dataChanged() {
        mCursor.requery();
        if (mCursor.moveToFirst()) {
			final String name =  mCursor.getString(1);
			final String phoneticName = mCursor.getString(1);
			
			final int idPerson = mCursor.getInt(0);
			contactId = idPerson;
			
	        // Lookup key
	        final String lookupKey = mCursor.getString(6);

	        // Assign uri to quickContactBadge
	        mPhotoView.assignContactUri(ContactsContract.Contacts.getLookupUri(idPerson, lookupKey));
	        if (imsConnected){
	        	mPhotoView.setExcludeMimes(new String[]{});
	        }else{
	        	mPhotoView.setExcludeMimes(contactsApi.getRcsMimeTypes());
	        }
			
			Thread enhancedPart = new Thread(){
				public void run(){
					handler.post(new Runnable(){
						public void run(){
							// Set the name
							if (TextUtils.isEmpty(name)) {
								mNameView.setText(getText(android.R.string.unknownName));
							} else {
								mNameView.setText(name);
							}

							if (mPhoneticNameView != null) {
								mPhoneticNameView.setText(phoneticName);
							}            
							
							Bitmap bitmap = BitmapFactory.decodeResource(getResources(), mNoPhotoResource);
							// Load the local photo
						     Uri photoUri = Uri.withAppendedPath(mUri, android.provider.ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);     
						     Cursor photoCursor = getContentResolver().query(photoUri,          
						    		 new String[] {Photo.PHOTO},
						    		 null, 
						    		 null, 
						    		 null);     
						     if (photoCursor != null) {         
						     try {         
						    	 if (photoCursor.moveToFirst()) {
						    		 byte[] data = photoCursor.getBlob(0);             
							    	 if (data != null) {  
							    		 try{
							    			 bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
							    		 } catch(Exception e) {
							    			 if (logger.isActivated()) {
							    				 logger.error("Can't read picture " + photoUri);
							    			 }
							    		 } catch(OutOfMemoryError e) {
							    			 if (logger.isActivated()) {
							    				 logger.error("Out of memory exception while reading "+photoUri);
							    			 }
							    		 }
							    	 }         
						    	 }
							     } finally {         
							    	 photoCursor.close();     
							     }
						     }
							
							mPhotoView.setImageBitmap(bitmap);
							
							// Remove RCS entries of the list
							removeRcsProfileFromList();
							removeRcsEntriesFromList();
							// Remove RCS indicator
							mRcsIndicatorView.setVisibility(View.INVISIBLE);
							// Remove presence
							mPresentIndicatorView.setVisibility(View.INVISIBLE);
							mPresentIndicatorView.setImageResource(R.drawable.rcs_eab_presence_offline);
							
							boolean hasRcsCapableNumber = false;
							boolean isPresent = false;
							ArrayList<PhoneNumber> phoneNumbers= ContactUtils.getContactNumbers(ViewContactActivity.this, idPerson);
							for (int i=0;i<phoneNumbers.size();i++){
								String number = phoneNumbers.get(i).number;
								String label = phoneNumbers.get(i).label;
								
								ContactInfo info = contactsApi.getContactInfo(number);
								boolean numberNotRcsActive = true;
								if (info!=null){
									// Get presence status
									int presenceStatus = info.getRcsStatus();
									if ((presenceStatus!=ContactInfo.NOT_RCS) && (presenceStatus!=ContactInfo.NO_INFO)){
										hasRcsCapableNumber = true;
									}

									if (presenceStatus==ContactInfo.RCS_ACTIVE){
										numberNotRcsActive = false; // We have shared presence with this number, we do not need to issue anonymous fetches
										// Add RCS profile to the list
										addRcsProfileToList(info, label);
										// Set RCS indicator
										mRcsIndicatorView.setVisibility(View.VISIBLE);
									}

									if (presenceStatus==ContactInfo.RCS_CAPABLE){
										// Set RCS indicator
										mRcsIndicatorView.setVisibility(View.VISIBLE);
										// Set availability
										if ((info.getRegistrationState()==ContactInfo.REGISTRATION_STATUS_ONLINE && imsConnected) || (info.getPresenceInfo()!=null && info.getPresenceInfo().isOnline())){
											isPresent = true;
											mPresentIndicatorView.setVisibility(View.VISIBLE);
											mPresentIndicatorView.setImageResource(R.drawable.rcs_eab_presence_online);
										}
									}
								}
								// Add RCS entries to the list
								addRcsEntriesToList(number, label, isPresent);
							}
					        // Build the RCS Manage profile entry if there is at least a RCS capable phone number and we are in social presence mode
							if (RcsSettings.getInstance()==null){
								RcsSettings.createInstance(ViewContactActivity.this);
							}
							if (hasRcsCapableNumber && RcsSettings.getInstance().isSocialPresenceSupported()){
								mManageProfileEntries.clear();
								ViewEntry entry = new ViewEntry();
								entry.label = getString(R.string.rcs_eab_manage_profile_sharing);
								entry.kind = ViewEntry.KIND_MANAGE_PROFILE;
								entry.actionIcon = R.drawable.rcs_common_flower_30x30;
								mManageProfileEntries.add(entry);
							}

						}
					});
				}
			};
			enhancedPart.start();
            
            // Set the star
            mStarView.setChecked(mCursor.getInt(2) == 1 ? true : false);

            // Build up the contact entries
            buildEntries(mCursor);
            mAdapter = new ViewAdapter(this, mSections);
            setListAdapter(mAdapter);
   			mAdapter.setSections(mSections, SHOW_SEPARATORS);
        } else {
            if (logger.isActivated()){
            	logger.error("invalid contact uri: " + mUri);
            }
            finish();
        }
    }
	
	private void visitWebSite(String contact, String weblink){
		// Change last visited link for contact
		contactsApi.setWeblinkVisitedForContact(contact);
		// Force adapter to redraw for the link to appear visited
		mAdapter.notifyDataSetChanged();
		if (!weblink.startsWith("http://")){
			//put "http://" as prefix if it has not it already, so the uri can be launched as intent data (it must have http:// prefix, www.domain.com uri are not recognized as http pages)
			weblink = "http://" + weblink;	
		}
		Uri uri = Uri.parse(weblink);
		startActivity( new Intent( Intent.ACTION_VIEW, uri ) );
	}
     
    public boolean onCreateOptionsMenu(Menu menu) {
    	
        menu.add(0, 0, 0, R.string.menu_editContact)
                .setIcon(android.R.drawable.ic_menu_edit)
                .setIntent(new Intent(getApplicationContext(), EditContactActivity.class).setData(mUri).setAction(Intent.ACTION_EDIT)) // Commented for RCS .setIntent(new Intent(Intent.ACTION_EDIT, mUri))
                .setAlphabeticShortcut('e');
        menu.add(0, MENU_ITEM_DELETE, 0, R.string.menu_deleteContact)
                .setIcon(android.R.drawable.ic_menu_delete);

        return true;
    }
     
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // Perform this check each time the menu is about to be shown, because the Barcode Scanner
        // could be installed or uninstalled at any time.
        if (isBarcodeScannerInstalled()) {
            if (menu.findItem(MENU_ITEM_SHOW_BARCODE) == null) {
                menu.add(0, MENU_ITEM_SHOW_BARCODE, 0, R.string.menu_showBarcode)
                        .setIcon(R.drawable.ic_menu_show_barcode);
            }
        } else {
            menu.removeItem(MENU_ITEM_SHOW_BARCODE);
        }
        return true;
    }

    private boolean isBarcodeScannerInstalled() {
        final Intent intent = new Intent(SHOW_BARCODE_INTENT);
        ResolveInfo ri = getPackageManager().resolveActivity(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return ri != null;
    }

    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
             info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            if (logger.isActivated()){
            	logger.error("bad menuInfo", e);
            }
            return;
        }

        // This can be null sometimes, don't crash...
        if (info == null) {
            if (logger.isActivated()){
            	logger.error("bad menuInfo");
            }
            return;
        }

        ViewEntry entry = ContactEntryAdapter.getEntry(mSections, info.position, SHOW_SEPARATORS);
        switch (entry.kind) {
            case Contacts.KIND_PHONE: {
                menu.add(0, 0, 0, R.string.menu_call).setIntent(entry.intent);
                menu.add(0, 0, 0, R.string.menu_sendSMS).setIntent(entry.auxIntent);
                if (entry.primaryIcon == -1) {
                    menu.add(0, MENU_ITEM_MAKE_DEFAULT, 0, R.string.menu_makeDefaultNumber);
                }
                break;
            }

            case Contacts.KIND_EMAIL: {
                menu.add(0, 0, 0, R.string.menu_sendEmail).setIntent(entry.intent);
                break;
            }

            case Contacts.KIND_POSTAL: {
                menu.add(0, 0, 0, R.string.menu_viewAddress).setIntent(entry.intent);
                break;
            }
        }
    }
     
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ITEM_DELETE: {
    			// Set the message and OnClickListener according to presence sharing status
    			int idPerson = (int)ContentUris.parseId(mUri);
    	    	ArrayList<PhoneNumber> rcsPhoneNumbers = ContactUtils.getRcsActiveContactNumbers(this, idPerson);
    	    	
    	        if (rcsPhoneNumbers.size()==0) {
    	        	// No rcs number, show regular delete dialog
                    showDialog(DIALOG_CONFIRM_DELETE);
    	        }else{
    	        	String contactName = ContactUtils.getContactDisplayName(this, idPerson);
    	        	// One or more rcs phone numbers
    	        	EabDialogUtils.showDeleteContactDialog(this, handler, contactName, rcsPhoneNumbers, presenceApi);
    	        }
                return true;
            }
            case MENU_ITEM_SHOW_BARCODE:
                if (mCursor.moveToFirst()) {
                    Intent intent = new Intent(SHOW_BARCODE_INTENT);
                    intent.putExtra("ENCODE_TYPE", "CONTACT_TYPE");
                    Bundle bundle = new Bundle();
                    String name = mCursor.getString(1/*CONTACT_NAME_COLUMN*/);
                    if (!TextUtils.isEmpty(name)) {
                        // Correctly handle when section headers are hidden
                        int sepAdjust = SHOW_SEPARATORS ? 1 : 0;
                        
                        bundle.putString(Contacts.Intents.Insert.NAME, name);
                        // The 0th ViewEntry in each ArrayList below is a separator item
                        int entriesToAdd = Math.min(mPhoneEntries.size() - sepAdjust, PHONE_KEYS.length);
                        for (int x = 0; x < entriesToAdd; x++) {
                            ViewEntry entry = mPhoneEntries.get(x + sepAdjust);
                            bundle.putString(PHONE_KEYS[x], entry.data);
                        }
                        entriesToAdd = Math.min(mEmailEntries.size() - sepAdjust, EMAIL_KEYS.length);
                        for (int x = 0; x < entriesToAdd; x++) {
                            ViewEntry entry = mEmailEntries.get(x + sepAdjust);
                            bundle.putString(EMAIL_KEYS[x], entry.data);
                        }
                        if (mPostalEntries.size() >= 1 + sepAdjust) {
                            ViewEntry entry = mPostalEntries.get(sepAdjust);
                            bundle.putString(Contacts.Intents.Insert.POSTAL, entry.data);
                        }
                        intent.putExtra("ENCODE_DATA", bundle);
                        try {
                            startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            // The check in onPrepareOptionsMenu() should make this impossible, but
                            // for safety I'm catching the exception rather than crashing. Ideally
                            // I'd call Menu.removeItem() here too, but I don't see a way to get
                            // the options menu.
                            if (logger.isActivated()){
                            	logger.error("Show barcode menu item was clicked but Barcode Scanner " +                            
                                    "was not installed.");
                            }
                        }
                        return true;
                    }
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ITEM_MAKE_DEFAULT: {
                AdapterView.AdapterContextMenuInfo info;
                try {
                     info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                } catch (ClassCastException e) {
                    if (logger.isActivated()){
                    	logger.error("bad menuInfo", e);
                    }
                    break;
                }

                ViewEntry entry = ContactEntryAdapter.getEntry(mSections, info.position,
                        SHOW_SEPARATORS);
                
                // Update the primary values in the data record.
                ContentValues values = new ContentValues(1);
                values.put(Data.IS_SUPER_PRIMARY, 1);
                getContentResolver().update(ContentUris.withAppendedId(Data.CONTENT_URI, entry.id),
                        values, null, null);
                dataChanged();
                return true;
            }
        }
        return super.onContextItemSelected(item);
    }

     
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL: {
            	
                int index = getListView().getSelectedItemPosition();
                if (index != -1) {
                    ViewEntry entry = ViewAdapter.getEntry(mSections, index, SHOW_SEPARATORS);
                    if (entry.kind == Contacts.KIND_PHONE) {
                        Intent intent = new Intent(Intent.ACTION_CALL, entry.uri);
                        startActivity(intent);
                    }
                } else if (mNumPhoneNumbers != 0) {
                    // There isn't anything selected, call the default number
                    Intent intent = new Intent(Intent.ACTION_CALL, mUri);
                    startActivity(intent);
                }
                return true;
            }

            case KeyEvent.KEYCODE_DEL: {
                showDialog(DIALOG_CONFIRM_DELETE);
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }
    
    protected void onListItemClick(ListView l, View v, int position, long id) {
        ViewEntry entry = ViewAdapter.getEntry(mSections, position, SHOW_SEPARATORS);
        if (entry != null) {
            Intent intent = entry.intent;
            if (intent != null && entry.isPresent) {
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    if (logger.isActivated()){
                    	logger.error("No activity found for intent: " + intent, e);
                    }
                    signalError();
                }
            } else {
            	switch(entry.kind){
            		
            	case ViewEntry.KIND_CALL_LOG:
            		Intent intentCallLog = new Intent(v.getContext(), RecentCallsListActivity.class);
            		intentCallLog.putExtra("incomingNumber",entry.data);
                	startActivity(intentCallLog);
            		return;
            		
            	case ViewEntry.KIND_AGGREGATED_CALL_LOG:
            		Intent intentAggCallLog = new Intent(v.getContext(), EventLogListActivity.class);
            		intentAggCallLog.putExtra("incomingContactId",(long)entry.contactId);
                	startActivity(intentAggCallLog);
            		return;
            		
            	case ViewEntry.KIND_RCS_PROFILE:
        	        Intent profileOptionsIntent = new Intent(v.getContext(), ProfileOptionsActivity.class);
        	        profileOptionsIntent.putExtra("return-data", true);
        	        if (entry.rcsinfo.getPresenceInfo().getFavoriteLink()!=null){
        	        	// Only if there is a weblink
        	        	profileOptionsIntent.putExtra("linkUrl",entry.rcsinfo.getPresenceInfo().getFavoriteLink().getLink());
        	        	profileOptionsIntent.putExtra("linkName",entry.rcsinfo.getPresenceInfo().getFavoriteLink().getName());
        	        }

        	        profileOptionsIntent.putExtra("number",entry.data);
        	        profileOptionsIntent.putExtra("label",entry.label);
        	        profileOptionsIntent.putExtra("availability",entry.rcsinfo.getPresenceInfo().isOnline());
        	        startActivityForResult(profileOptionsIntent, PROFILE_OPTIONS);
        			
            		return;
            	case ViewEntry.KIND_MANAGE_PROFILE:
            		Intent manageProfileIntent = new Intent(v.getContext(), ManageProfileSharingActivity.class);
            		manageProfileIntent.setData(getIntent().getData());
                	startActivity(manageProfileIntent);
            		return;
            	}
                signalError();
            }
        } else {
            signalError();
        }
    }

    /**
     * When coming back from subactivity
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	
    	if (resultCode != RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case PROFILE_OPTIONS: {
            	int result_choice = data.getIntExtra("result_choice", 0);
            	String calledNumber = data.getStringExtra("called_number");
            	switch (result_choice){
            	case ProfileOptionsActivity.VISIT_WEB_LINK:
            		visitWebSite(calledNumber, data.getStringExtra("weblink_url"));
            		break;
//            	case ProfileOptionsActivity.SAVE_IMAGE:
//            		DialogUtils.displayToast(ViewContactActivity.this, handler, getString(R.string.rcs_common_not_yet_implemented));
//            		break;
            	case ProfileOptionsActivity.MANAGE_PROFILE:
            		Intent manageProfileIntent = new Intent(this, ManageProfileSharingActivity.class);
            		manageProfileIntent.setData(getIntent().getData());
                	startActivity(manageProfileIntent);
            		break;
            	case ProfileOptionsActivity.CALL:
            		EabUtils.initiateCall(this, calledNumber);
            		break;
            	case ProfileOptionsActivity.TEXT:
            		ContactInfo contactInfo = contactsApi.getContactInfo(calledNumber);
        			if (contactInfo!=null 
        					&& contactInfo.getCapabilities()!=null
        					&& contactInfo.getCapabilities().isImSessionSupported()){
        				// If chat is possible with contact, initiate one
        				// Create IM intent
        				List<ResolveInfo> list = IntentUtils.getInstantMessagingSessionApplications(this);
        				if (list.size()>0){
        					// We use the first activity that supports im_session
        					ResolveInfo info = (ResolveInfo)list.get(0);
        					Intent intent = new Intent();
        					intent.setClassName(info.activityInfo.packageName,info.activityInfo.name);
        					intent.setFlags(                        
        							Intent.FLAG_ACTIVITY_NEW_TASK
        							| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        					intent.putExtra("contact", calledNumber);
        					intent.putExtra("displayName", calledNumber);
        					startActivity(intent);
        				}
        			}else{
        				// No chat possible, we use SMS native application
        				Intent intent = new Intent(Intent.ACTION_SENDTO).setData(Uri.parse("sms:"+calledNumber));
        				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
        							| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        				startActivity(intent);
        			}
            		break;
            		
            	}
                break;
            }
        }
    }
    
    /**
     * Signal an error to the user via a beep, or some other method.
     */
    private void signalError() {
        //TODO: implement this when we have the sonification APIs
    }

    /**
     * Build separator entries for all of the sections.
     */
    private void buildSeparators() {
        ViewEntry separator;
        
        separator = new ViewEntry();
        separator.kind = ViewEntry.KIND_SEPARATOR;
        separator.data = getString(R.string.listSeparatorCallNumber);
        mPhoneEntries.add(separator);

        separator = new ViewEntry();
        separator.kind = ViewEntry.KIND_SEPARATOR;
        separator.data = getString(R.string.listSeparatorSendSmsMms);
        mSmsEntries.add(separator);

        separator = new ViewEntry();
        separator.kind = ViewEntry.KIND_SEPARATOR;
        separator.data = getString(R.string.listSeparatorSendEmail);
        mEmailEntries.add(separator);

        separator = new ViewEntry();
        separator.kind = ViewEntry.KIND_SEPARATOR;
        separator.data = getString(R.string.listSeparatorSendIm);
        mImEntries.add(separator);

        separator = new ViewEntry();
        separator.kind = ViewEntry.KIND_SEPARATOR;
        separator.data = getString(R.string.listSeparatorMapAddress);
        mPostalEntries.add(separator);

        separator = new ViewEntry();
        separator.kind = ViewEntry.KIND_SEPARATOR;
        separator.data = getString(R.string.listSeparatorOrganizations);
        mOrganizationEntries.add(separator);

        separator = new ViewEntry();
        separator.kind = ViewEntry.KIND_SEPARATOR;
        separator.data = getString(R.string.listSeparatorOtherInformation);
        mCallLogEntries.add(separator);
        
        separator = new ViewEntry();
        separator.kind = ViewEntry.KIND_SEPARATOR;
        separator.data = getString(R.string.listSeparatorOtherInformation);
        mOtherEntries.add(separator);
    }

    /**
     * Build up the entries to display on the screen.
     * 
     * @param personCursor the URI for the contact being displayed
     */
    private final void buildEntries(Cursor personCursor) {
        // Clear out the old entries
        final int numSections = mSections.size();
        for (int i = 0; i < numSections; i++) {
            mSections.get(i).clear();
        }

        if (SHOW_SEPARATORS) {
            buildSeparators();
        }

        String personId = ""+ContentUris.parseId(mUri);
        
        if (Integer.parseInt(personCursor.getString(personCursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
        	Cursor phonesCursor = mResolver.query(
        			ContactsContract.CommonDataKinds.Phone.CONTENT_URI, 
        			new String[]{Phone._ID, Phone.NUMBER, Phone.IS_PRIMARY, Phone.LABEL, Phone.TYPE}, 
        			ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?", 
        			new String[]{personId}, null);
    		// List of unique number that were added
    		ArrayList<String> treatedNumbers = new ArrayList<String>();
        	while (phonesCursor.moveToNext()) {
				// Keep a trace of already treated row. Key is (phone number in international format)
				String phoneNumber = PhoneUtils.formatNumberToInternational(phonesCursor.getString(1));
				if (!treatedNumbers.contains(phoneNumber)){					
					treatedNumbers.add(phoneNumber);
					final int type = phonesCursor.getInt(4);
					final String number = phonesCursor.getString(1);
					final String label = phonesCursor.getString(3);
					final boolean isPrimary = phonesCursor.getInt(2) == 1;
					final long id = phonesCursor.getLong(0);
					final Uri uri = ContentUris.withAppendedId(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, id);

					// Don't crash if the number is bogus
					if (TextUtils.isEmpty(number)) {
						if (logger.isActivated()){
							logger.warn("empty number for phone " + id);
						}
						continue;
					}

					mNumPhoneNumbers++;

					// Add a phone number entry
					final ViewEntry entry = new ViewEntry();
					final CharSequence displayLabel = Phones.getDisplayLabel(this, type, label);
					entry.label = buildActionString(R.string.actionCall, displayLabel, true);
					entry.data = number;
					entry.id = id;
					entry.uri = uri;
					entry.intent = new Intent(Intent.ACTION_CALL, entry.uri);
					entry.auxIntent = new Intent(Intent.ACTION_SENDTO).setData(Uri.parse("sms:"+number));
					entry.kind = Contacts.KIND_PHONE;
					if (isPrimary) {
						entry.primaryIcon = R.drawable.ic_default_number;
					}
					entry.actionIcon = android.R.drawable.sym_action_call;
					mPhoneEntries.add(entry);

					if (type == Phones.TYPE_MOBILE || mShowSmsLinksForAllPhones) {
						// Add an SMS entry
						ViewEntry smsEntry = new ViewEntry();
						smsEntry.label = buildActionString(R.string.actionText, displayLabel, true);
						smsEntry.data = number;
						smsEntry.id = id;
						smsEntry.uri = uri;
						smsEntry.intent = entry.auxIntent;
						smsEntry.kind = ViewEntry.KIND_SMS;
						smsEntry.actionIcon = R.drawable.sym_action_sms;
						mSmsEntries.add(smsEntry);
					}
				} 
	        }
	        phonesCursor.close();
	        
	        /* Add Events Log entry */
	        ViewEntry callLogEntry = new ViewEntry();
            callLogEntry.label = getString(R.string.rcs_eab_view_aggregated_call_log);
            callLogEntry.kind = ViewEntry.KIND_AGGREGATED_CALL_LOG;
            callLogEntry.actionIcon = R.drawable.rcs_eab_call_log_30x30;
            callLogEntry.contactId = Integer.parseInt(personId);
			mCallLogEntries.add(callLogEntry);
	    }
        

        // Build the contact method entries
        
        // Postal addresses
    	String addrWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?"; 
    	String[] addrWhereParams = new String[]{personId, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE}; 
    	Cursor addrCursor = getContentResolver().query(ContactsContract.Data.CONTENT_URI, 
                    null, 
                    addrWhere, 
                    addrWhereParams, 
                    null); 
    	while(addrCursor.moveToNext()) {
    		long id = addrCursor.getLong(
    				addrCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal._ID));
    		String formattedAddress = addrCursor.getString(
    				addrCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));
    		String poBox = addrCursor.getString(
    				addrCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POBOX));
     		String street = addrCursor.getString(
     				addrCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET));
     		String city = addrCursor.getString(
     				addrCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY));
     		String state = addrCursor.getString(
     				addrCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION));
     		String postalCode = addrCursor.getString(
     				addrCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE));
     		String country = addrCursor.getString(
     				addrCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY));
     		int type = addrCursor.getInt(
     				addrCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE));
     		
     		ViewEntry entry = new ViewEntry();
     		entry.id = id;
     		entry.uri = ContentUris.withAppendedId(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI, id);
     		entry.kind = type;
     		entry.label = buildActionString(R.string.actionMap,
     				ContactsContract.CommonDataKinds.StructuredPostal.getTypeLabel(getResources(), type, formattedAddress), true);
     		entry.data = formattedAddress;
     		entry.maxLines = 4;
     		entry.intent = new Intent(Intent.ACTION_VIEW, entry.uri);
     		entry.actionIcon = R.drawable.sym_action_map;
     		mPostalEntries.add(entry);
    	} 
    	addrCursor.close();
        
    	// Emails
    	Cursor emailCursor = getContentResolver().query( 
    			ContactsContract.CommonDataKinds.Email.CONTENT_URI, 
    			null,
    			ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?", 
    			new String[]{personId}, 
    			null); 
    	while (emailCursor.moveToNext()) { 
    		String email = emailCursor.getString(
    				emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
    		String label = emailCursor.getString(
    				emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.LABEL));
    		int emailType = emailCursor.getInt(
    				emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE)); 

    		ViewEntry entry = new ViewEntry();
     		entry.kind = emailType;
     		entry.label = buildActionString(R.string.actionEmail,
     				ContactsContract.CommonDataKinds.Email.getTypeLabel(getResources(), emailType, label), true);
     		entry.data = email;
     		entry.intent = new Intent(Intent.ACTION_SENDTO,
     				Uri.fromParts("mailto", email, null));
     		entry.actionIcon = android.R.drawable.sym_action_email;
     		mEmailEntries.add(entry);
    	} 
    	emailCursor.close();
    	
    	// IM
    	String imWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?"; 
     	String[] imWhereParams = new String[]{personId, ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE}; 
     	Cursor imCursor = getContentResolver().query(
     			ContactsContract.Data.CONTENT_URI,
     			null, 
     			imWhere, 
     			imWhereParams, 
     			null); 
     	while (imCursor.moveToNext()) { 
     		String imData = imCursor.getString(imCursor.getColumnIndex(ContactsContract.CommonDataKinds.Im.DATA));
     		int imProtocol = imCursor.getInt(imCursor.getColumnIndex(ContactsContract.CommonDataKinds.Im.PROTOCOL));
     		String imCustomProtocol = imCursor.getString(imCursor.getColumnIndex(ContactsContract.CommonDataKinds.Im.CUSTOM_PROTOCOL));
     		String imMimeType = imCursor.getString(imCursor.getColumnIndex(ContactsContract.CommonDataKinds.Im.MIMETYPE));

     		ViewEntry entry = new ViewEntry();
     		if (imProtocol!=Im.PROTOCOL_CUSTOM){
	     		entry.label = getString(R.string.actionChat,
	     				Im.getProtocolLabel(getResources(), imProtocol, "").toString());
     		}else{
     			entry.label = getString(R.string.actionChat, imCustomProtocol);
     		}
     		ContentValues values = new ContentValues();
     		values.put(Im.PROTOCOL, imProtocol);
     		values.put(Data.MIMETYPE, imMimeType);
     		values.put(Im.CUSTOM_PROTOCOL,imCustomProtocol);
     		values.put(Im.DATA, imData);
     		values.put(Email.DATA, imData);

     		entry.intent = EabUtils.buildImIntent(values);
     		entry.actionIcon = android.R.drawable.sym_action_chat;
     		mImEntries.add(entry);
     	} 
     	imCursor.close();
    	
    	// Build the other entries
    	// Note
    	String noteWhere = ContactsContract.Data.CONTACT_ID + " = ? AND " + ContactsContract.Data.MIMETYPE + " = ?"; 
    	String[] noteWhereParams = new String[]{personId, 
    			ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE}; 
    	Cursor noteCur = getContentResolver().query(ContactsContract.Data.CONTENT_URI, 
    			null, 
    			noteWhere, 
    			noteWhereParams, 
    			null); 
    	if (noteCur.moveToFirst()) { 
    		String note = noteCur.getString(noteCur.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE));
    		 if (!TextUtils.isEmpty(note)) {
    	          ViewEntry entry = new ViewEntry();
    	          entry.label = getString(R.string.label_notes);
    	          entry.data = note;
    	          entry.id = 0;
    	          entry.kind = ViewEntry.KIND_CONTACT;
    	          entry.uri = null;
    	          entry.intent = null;
    	          entry.maxLines = 10;
    	          entry.actionIcon = R.drawable.sym_note;
    	          mOtherEntries.add(entry);
    	      }
    	} 
    	noteCur.close();    	
    }

    /**
     * Add a RCS profile to the contact
     * 
     * @param info
     * @param label
     */
    private void addRcsProfileToList(ContactInfo info, String label){
    	ViewEntry entry = new ViewEntry();
    	entry.kind = ViewEntry.KIND_RCS_PROFILE;
    	entry.rcsinfo = info;
    	entry.label = label;
    	entry.data = info.getContact();
		mRcsProfileEntries.add(entry);
	    mAdapter.notifyDataSetChanged();
    }
    
    /**
     * Remove RCS profile from list
     */
    private void removeRcsProfileFromList(){
    	mRcsProfileEntries.clear();
	    mAdapter.notifyDataSetChanged();
    }
    
    /**
     * Add a RCS entry to the list
     * 
     * @param number
     * @param label
     * @param present flag If the contact is currently present
     */
    private void addRcsEntriesToList(String number, String label, boolean isPresent){
    	// Remove call log entry
    	// mCallLogEntries.clear();
    	
		if (number!=null && number.length()>0){
			// Do not search RCS entries if contact has no number

			// Create call log entry
			ViewEntry entry = new ViewEntry();
			entry.isPresent = isPresent;
			entry.label = getString(R.string.rcs_eab_view_call_log, label);
			entry.data = number;
			entry.kind = ViewEntry.KIND_CALL_LOG;
			entry.actionIcon = R.drawable.rcs_eab_call_log_30x30;
			// Search the last position for a file transfer in the list, and insert this entry just after
			// If we do not find any, we will put it at the end of the list
			int correctPosition = mRcsEntries.size();
			for (int i=0;i<mRcsEntries.size();i++){
				ViewEntry entryToCheck = mRcsEntries.get(i);
				if (entryToCheck.kind == ViewEntry.KIND_CALL_LOG){
					correctPosition=i+1;
				}
			}
			mRcsEntries.add(correctPosition, entry);
			
			PackageManager packageManager = getPackageManager();
			
			// If number can do file transfer, add entries
			
    		ContactInfo contactInfo = contactsApi.getContactInfo(number);
			if (contactInfo!=null 
					&& contactInfo.getCapabilities()!=null
					&& contactInfo.getCapabilities().isFileTransferSupported()){

				// Create file transfer intent
				List<ResolveInfo> list = IntentUtils.getFileTransferApplications(this);
				for(int i=0; i < list.size(); i++) {
					ResolveInfo info = (ResolveInfo)list.get(i);
					entry = new ViewEntry();
					entry.isPresent = isPresent;
					entry.label = info.loadLabel(packageManager)+ " " + getString(R.string.rcs_eab_with, label);
					entry.data = number;
					entry.kind = ViewEntry.KIND_EXTERNAL_RCS_APP;
					entry.drawable = info.loadIcon(packageManager);
					Intent intent = new Intent();
					intent.setClassName(info.activityInfo.packageName,info.activityInfo.name);
					intent.setFlags(                        
							Intent.FLAG_ACTIVITY_NEW_TASK
							| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
					intent.putExtra("contact", number);
					intent.putExtra("displayName", label);

					entry.intent = intent;
					// Search the last position for a file transfer in the list, and insert this entry just after
					// If we do not find any, we will put it at the end of the list
					correctPosition = mRcsEntries.size();
					for (int j=0;j<mRcsEntries.size();j++){
						ViewEntry entryToCheck = mRcsEntries.get(i);
						if (entryToCheck.label.equalsIgnoreCase(entry.label)){
							correctPosition=i+1;
						}
					}	        
					mRcsEntries.add(correctPosition, entry);
				}
			}

			// If number can do IM sessions, add entries
			if (contactInfo!=null 
					&& contactInfo.getCapabilities()!=null
					&& contactInfo.getCapabilities().isImSessionSupported()){
				// Create IM Sessions entry
				List<ResolveInfo> list = IntentUtils.getInstantMessagingSessionApplications(this);
				for(int i=0; i < list.size(); i++) {
					ResolveInfo info = (ResolveInfo)list.get(i);
					entry = new ViewEntry();
					if (RcsSettings.getInstance()==null){
						RcsSettings.createInstance(ViewContactActivity.this);
					}
					entry.isPresent = isPresent || RcsSettings.getInstance().isImAlwaysOn();
					entry.label = info.loadLabel(packageManager)+ " " + getString(R.string.rcs_eab_with, label);
					entry.data = number;
					entry.kind = ViewEntry.KIND_EXTERNAL_RCS_APP;
					entry.drawable = info.loadIcon(packageManager);
					Intent intent = new Intent();
					intent.setClassName(info.activityInfo.packageName,info.activityInfo.name);
					intent.setFlags(                        
							Intent.FLAG_ACTIVITY_NEW_TASK
							| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
					intent.putExtra("contact", number);
					intent.putExtra("displayName", label);

					entry.intent = intent;
					// Search the last position for a file transfer in the list, and insert this entry just after
					// If we do not find any, we will put it at the end of the list
					correctPosition = mRcsEntries.size();
					for (int j=0;j<mRcsEntries.size();j++){
						ViewEntry entryToCheck = mRcsEntries.get(i);
						if (entryToCheck.label.equalsIgnoreCase(entry.label)){
							correctPosition=i+1;
						}
					}	        
					mRcsEntries.add(correctPosition, entry);
				}
			}

		}
	    mAdapter.notifyDataSetChanged();

    }

    /**
     * Remote the RCS entries
     */
    private void removeRcsEntriesFromList(){
       mRcsEntries.clear();
    }

    String buildActionString(int actionResId, CharSequence type, boolean lowerCase) {
        // If there is no type just display an empty string
        if (type == null) {
            type = "";
        }

        if (lowerCase) {
            return getString(actionResId, type.toString().toLowerCase());
        } else {
            return getString(actionResId, type.toString());
        }
    }
    
    /**
     * A basic structure with the data for a contact entry in the list.
     */
    final static class ViewEntry extends ContactEntryAdapter.Entry {
        public int primaryIcon = -1;
        public Intent intent;
        public Intent auxIntent = null;
        public int presenceIcon = -1;
        public int actionIcon = -1;
        public int maxLabelLines = 1;
        
        public static final int KIND_FILE_TRANSFER = -4;
        public static final int KIND_CALL_LOG = -5;
        public static final int KIND_MANAGE_PROFILE = -6;
        public static final int KIND_RCS_PROFILE = -7;
        public static final int KIND_EXTERNAL_RCS_APP = -8;
        public static final int KIND_AGGREGATED_CALL_LOG = -9;
        
        public ContactInfo rcsinfo;
        public int contactId = -1;
        public Drawable drawable;
        public boolean isPresent = true;
    }

    private /*static*/ final class ViewAdapter extends ContactEntryAdapter<ViewEntry> {
        /** Cache of the children views of a row */
        /*static*/ class ViewCache {
            public TextView label;
            public TextView data;
            public ImageView actionIcon;
            public ImageView presenceIcon;
            
            // Need to keep track of this too
            public ViewEntry entry;
        }
        
        ViewAdapter(Context context, ArrayList<ArrayList<ViewEntry>> sections) {
            super(context, sections, SHOW_SEPARATORS);
        }

         
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewEntry entry = getEntry(mSections, position, false); 
            View v;

            if (entry.kind == ViewEntry.KIND_RCS_PROFILE){
            	final Resources resources = mContext.getResources();
            	final ContentResolver resolver = mContext.getContentResolver();
            	ImageView mAvailabilityView;
            	TextView mFreeTxtView;
            	TextView mLastUpdateTimeView;
            	ImageView mRcsPhotoView;
            	TextView mRcsWebLinkView;
            	
            	View mRcsPanel = (View) mInflater.inflate(
                        R.layout.rcs_eab_rcs_profile_view_contact_layout, parent, SHOW_SEPARATORS);

            	mAvailabilityView = (ImageView) mRcsPanel.findViewById(R.id.rcs_availability);
        		mFreeTxtView = (TextView) mRcsPanel.findViewById(R.id.rcs_free_text);
                // Set unlimited animation
        		mFreeTxtView.setMarqueeRepeatLimit(-1);
        		mFreeTxtView.setEllipsize(TruncateAt.MARQUEE);
        		
        		mLastUpdateTimeView = (TextView) mRcsPanel.findViewById(R.id.rcs_last_update_time);
        		mRcsPhotoView = (ImageView) mRcsPanel.findViewById(R.id.rcs_picture);
        		mRcsWebLinkView = (TextView)mRcsPanel.findViewById(R.id.rcs_weblink);
                // Set unlimited animation
        		mRcsWebLinkView.setMarqueeRepeatLimit(-1);
        		mRcsWebLinkView.setEllipsize(TruncateAt.MARQUEE);

        		if (entry.rcsinfo.getPresenceInfo().getFreetext()!=null){
        			mFreeTxtView.setText(entry.rcsinfo.getPresenceInfo().getFreetext());
        		}
        		if (entry.rcsinfo.getPresenceInfo().getFreetext()==null || entry.rcsinfo.getPresenceInfo().getFreetext().length()<1){
        			mFreeTxtView.setText(resources.getString(R.string.rcs_eab_noFreeText));
        		}

        		mLastUpdateTimeView.setText(DateUtils.formatLastUpdateTime(resolver, entry.rcsinfo.getPresenceInfo().getTimestamp()));
        		if ((entry.rcsinfo.getPresenceInfo().getFavoriteLink()!=null) && (entry.rcsinfo.getPresenceInfo().getFavoriteLink().getLink()!=null) && (entry.rcsinfo.getPresenceInfo().getFavoriteLink().getLink().length()>0)){
        			String contactWebLink = entry.rcsinfo.getPresenceInfo().getFavoriteLink().getLink();
        			
        			// Render the url human readable (keep only host name)
        			String humanReadableWebLink = UrlUtils.formatUrlToDomainName(contactWebLink);
        			Spannable text = new SpannableString(humanReadableWebLink);
        			text.setSpan(new UnderlineSpan(), 0, humanReadableWebLink.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        			mRcsWebLinkView.setText(text);
        			// Check if favorite link has been updated to set the typeface (bold for updated, else normal)
        			boolean hasWebLinkBeenUpdated= contactsApi.hasWeblinkBeenUpdatedForContact(entry.data);
        			if (hasWebLinkBeenUpdated){
        				mRcsWebLinkView.setTypeface(Typeface.DEFAULT_BOLD);
        			}else{
        				mRcsWebLinkView.setTypeface(Typeface.DEFAULT);
        			}
        		}else{
        			mRcsWebLinkView.setVisibility(View.GONE);
        			mRcsWebLinkView.setOnClickListener(null);
        		}
        		
        		// Get presence icon
        		Bitmap icon = null;
        		PhotoIcon photoIcon = entry.rcsinfo.getPresenceInfo().getPhotoIcon();
        		if (photoIcon != null) {
                    byte[] data = entry.rcsinfo.getPresenceInfo().getPhotoIcon().getContent();
                    icon = BitmapFactory.decodeByteArray(data, 0, data.length);
        		}
        		
        		if (icon!=null){
        			// If presence icon not null, we use it
        			mRcsPhotoView.setImageBitmap(icon);
        		}else{
        			// Load the default photo
        			mRcsPhotoView.setImageResource(R.drawable.rcs_default_portrait_90x90);
        		}

        		if (entry.rcsinfo.getPresenceInfo().isOnline()){
        			mAvailabilityView.setVisibility(View.VISIBLE);
        		}else{
        			mAvailabilityView.setVisibility(View.INVISIBLE);
        		}
                return mRcsPanel;
            }
            
            if (entry.kind == ViewEntry.KIND_EXTERNAL_RCS_APP){
            	View rcsAppView = mInflater.inflate(R.layout.list_item_text_icons, parent, false);
                TextView label = (TextView) rcsAppView.findViewById(android.R.id.text1);
                TextView data = (TextView) rcsAppView.findViewById(android.R.id.text2);
                ImageView actionIcon = (ImageView) rcsAppView.findViewById(R.id.icon1);
                
                // Set icon
                if (entry.drawable!=null){
                	actionIcon.setVisibility(View.VISIBLE);
                	actionIcon.setImageDrawable(entry.drawable);
                }else{
                	actionIcon.setVisibility(View.INVISIBLE);
                	actionIcon.setImageDrawable(null);
                }
                // Set label
               	label.setText(entry.label);
                // Set data
               	data.setText(entry.data);
               	
               	if (!entry.isPresent){
               		label.setEnabled(false);
               		data.setEnabled(false);
               	}else{
               		label.setEnabled(true);
               		data.setEnabled(true);
               	}
               	
                return rcsAppView;
            }
            
            // Handle separators specially
            if (entry.kind == ViewEntry.KIND_SEPARATOR) {
                TextView separator = (TextView) mInflater.inflate(
                        R.layout.list_separator, parent, SHOW_SEPARATORS);
                separator.setText(entry.data);
                return separator;
            }

            ViewCache views;

            // Create a new view if needed
            v = mInflater.inflate(R.layout.list_item_text_icons, parent, false);

            // Cache the children
            views = new ViewCache();
            views.label = (TextView) v.findViewById(android.R.id.text1);
            views.data = (TextView) v.findViewById(android.R.id.text2);
            views.actionIcon = (ImageView) v.findViewById(R.id.icon1);
            views.presenceIcon = (ImageView) v.findViewById(R.id.icon2);
            v.setTag(views);
            
            // Update the entry in the view cache
           	views.entry = entry;
           	
            // Bind the data to the view
            bindView(v, entry);
            return v;
        }

         
        protected View newView(int position, ViewGroup parent) {
            // getView() handles this
            throw new UnsupportedOperationException();
        }

         
        protected void bindView(View view, ViewEntry entry) {
            final Resources resources = mContext.getResources();
            ViewCache views = (ViewCache) view.getTag();

            // Set the label
            TextView label = views.label;
            setMaxLines(label, entry.maxLabelLines);
            label.setText(entry.label);

            // Set the data
            TextView data = views.data;
            if (data != null) {
            	data.setText(entry.data);
                setMaxLines(data, entry.maxLines);
            }
            if (entry.data!=null){
            	data.setVisibility(View.VISIBLE);
            }else{
            	data.setVisibility(View.GONE);
            }
            
            // Set the action icon
            ImageView action = views.actionIcon;
            if (entry.actionIcon != -1) {
                action.setImageDrawable(resources.getDrawable(entry.actionIcon));
                action.setVisibility(View.VISIBLE);
            } else {
                // Things should still line up as if there was an icon, so make it invisible
                action.setVisibility(View.INVISIBLE);
            }

            // Set the presence icon
            Drawable presenceIcon = null;
            if (entry.primaryIcon != -1) {
                presenceIcon = resources.getDrawable(entry.primaryIcon);
            } else if (entry.presenceIcon != -1) {
                presenceIcon = resources.getDrawable(entry.presenceIcon);
            }

            ImageView presence = views.presenceIcon;
            if (presenceIcon != null) {
                presence.setImageDrawable(presenceIcon);
                presence.setVisibility(View.VISIBLE);
            } else {
                presence.setVisibility(View.GONE);
            }
        }

        private void setMaxLines(TextView textView, int maxLines) {
            if (maxLines == 1) {
                textView.setSingleLine(true);
                textView.setEllipsize(TextUtils.TruncateAt.END);
            } else {
                textView.setSingleLine(false);
                textView.setMaxLines(maxLines);
                textView.setEllipsize(null);
            }
        }
    }

	private final BroadcastReceiver mContactInfosChangedIntentReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			handler.post(new Runnable(){
				public void run(){
					dataChanged();
				}
			});
		}
	};    
	
	private final BroadcastReceiver mContactCapabilitiesChangedIntentReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			handler.post(new Runnable(){
				public void run(){
					dataChanged();
				}
			});
		}
	};   
    
	private final BroadcastReceiver mContactPhotoChangedIntentReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			handler.post(new Runnable(){
				public void run(){
					dataChanged();
				}
			});
		}
	};    

	private final BroadcastReceiver mContactStatusChangedIntentReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, final Intent intent) {
			handler.post(new Runnable(){
				public void run(){
					String contact = intent.getStringExtra("contact");
					long contactId = ContentUris.parseId(mUri);
					ArrayList<PhoneNumber> phoneNumbers = ContactUtils.getRcsContactNumbers(ViewContactActivity.this, contactId);
					// Check if event is for one of our numbers
					for (int i=0;i<phoneNumbers.size();i++){
						String number = phoneNumbers.get(i).number;
						if (PhoneUtils.extractNumberFromUri(contact).equalsIgnoreCase(PhoneUtils.extractNumberFromUri(number))){
							// This event is for this number
							dataChanged();
						}
					}
				}
			});
		}
	};

	@Override
	public void handleApiDisabled() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleApiConnected() {
		
		if (logger.isActivated()){
			logger.debug("Querying options on all RCS-e numbers");
		}
		if (mCursor==null || mCursor.isClosed()){
			return;
		}
		final int idPerson = mCursor.getInt(0);		

		Thread thread = new Thread(new Runnable(){
			public void run(){
				ArrayList<PhoneNumber> phoneNumbers= ContactUtils.getContactNumbers(ViewContactActivity.this, idPerson);
				for (int i=0;i<phoneNumbers.size();i++){
					String number = phoneNumbers.get(i).number;
					ContactInfo info = contactsApi.getContactInfo(number);
					if (info!=null 
							&& (info.getRcsStatus()!=ContactInfo.NOT_RCS) 
							&& (info.getRcsStatus()!=ContactInfo.RCS_ACTIVE)){
						// For each RCS capable and non RCS active number, we issue a capability query
						try {
							capabilityApi.requestCapabilities(number);
						} catch (ClientApiException e) {
							if (logger.isActivated()){
								logger.error("Could not query the capabilities for "+number,e);
							}
						}
					}
				}
			}
		});
		thread.start();

	}

	@Override
	public void handleApiDisconnected() {
		// TODO Auto-generated method stub
	}    

	@Override
	public void handleImsConnected() {
		imsConnected = true;
		dataChanged();
	} 
	
	@Override
	public void handleImsDisconnected() {
		imsConnected = false;
		dataChanged();
	} 
	
}
