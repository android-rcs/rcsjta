/*******************************************************************************
 * Software Name : RCS
 *
 * Copyright © 2010 France Telecom S.A.
 ******************************************************************************/

package com.orangelabs.rcs.eab;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.orangelabs.rcs.eab.R;
import com.orangelabs.rcs.common.DateUtils;
import com.orangelabs.rcs.common.UrlUtils;
import com.orangelabs.rcs.common.dialog.DialogUtils;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.api.client.ClientApiException;
import com.orangelabs.rcs.service.api.client.ClientApiListener;
import com.orangelabs.rcs.service.api.client.contacts.ContactInfo;
import com.orangelabs.rcs.service.api.client.contacts.ContactsApi;
import com.orangelabs.rcs.service.api.client.presence.FavoriteLink;
import com.orangelabs.rcs.service.api.client.presence.PhotoIcon;
import com.orangelabs.rcs.service.api.client.presence.PresenceApi;
import com.orangelabs.rcs.service.api.client.presence.PresenceApiIntents;
import com.orangelabs.rcs.service.api.client.presence.PresenceInfo;
import com.orangelabs.rcs.utils.logger.Logger;


/**
 * Displays end user profile.
 * 
 * <br>Allows the modification of the photo, the free text, the weblink and the availability of the user
 */
public class ViewMyProfileActivity extends Activity implements ClientApiListener {
	public TextView mLastUpdateView;	

	private PresenceInfo info = null;
	private PresenceApi presenceApi = null;
	private final Handler handler = new Handler();
	private boolean apiConnected = false;
	
	// Dialogs
	private static final int DIALOG_POPUP = 2;
    private static final int ID_PRESENCE_PUBLISH_PROGRESS = 3;
    private static final int ID_CONFIRM_QUIT_WITH_CHANGES = 4;
    
	// Menus
	static final int MENU_ITEM_PUBLISH = 1;
	static final int MENU_ITEM_REVERT = 2;
	static final int MENU_ITEM_PHOTO = 3;

	public String dialogPopupMessage = null;
	boolean isUpdating = false;

	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

	
    /** The launch code when picking a photo and the raw data is returned */
    private static final int PHOTO_PICKED_WITH_DATA = 3021;
    private static final int WEBLINK_PICKED_WITH_DATA = 5555;
    private static final int PHOTO_PICKED_FROM_CAMERA = 6666;
    
    // Uri for temporary image captured from camera, before it is cropped
    private Uri imageUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(),"tmp_contact.jpg"));
    
    private Bitmap mPhoto;
    private String mChosenFavoriteLink = null;
    private String mFreeText;
    
    private int mFreeTextSelectionEnd;

    private ImageView mPhotoImageView;
    private View mPhotoButton;
    private EditText mFreeTextView;
    private MenuItem mPhotoMenuItem;
    private boolean mPhotoPresent = false;
    
	private TextView mWebLinkTextView;
	private ToggleButton mAvailabilityToggleButton;
	
	private TextView mReceivedInvitationsTextView;
	private TextView mSentInvitationsTextView;
	private TextView mBlockedInvitationsTextView;
	
	private View mReceivedInvitationsLayout;
	private View mSentInvitationsLayout;
	private View mBlockedInvitationsLayout;
	
	private int numberOfReceivedInvitations = 0;
	private int numberOfSentInvitations = 0;
	private int numberOfBlockedInvitations = 0;
	
	// Flags when a field has changed
    private boolean mFreeTextChanged = false;
    private boolean mWebLinkChanged = false;
    private boolean mPhotoChanged = false;
    private boolean userChangedAvailability = false;
    private boolean mUserChoosenAvailability = false;

	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		setContentView(R.layout.rcs_eab_view_my_profile_layout);

		// Register intent receiver
		IntentFilter filter = new IntentFilter(PresenceApiIntents.MY_PRESENCE_INFO_CHANGED);
		registerReceiver(mEndUserChangedIntentReceiver, filter, null, handler);
		
		filter = new IntentFilter(PresenceApiIntents.PRESENCE_SHARING_CHANGED);
		registerReceiver(presenceSharingChangedIntentReceiver, filter, null, handler);

		if (presenceApi == null) {
			presenceApi = new PresenceApi(this);
			presenceApi.addApiEventListener(this);	
			presenceApi.connectApi();
		}

		// References 
		mLastUpdateView = (TextView)findViewById(R.id.my_profile_shared_info_item_right);
		mAvailabilityToggleButton = (ToggleButton)findViewById(R.id.my_profile_availability_item_toggle);
		mAvailabilityToggleButton.setEnabled(false);
		mAvailabilityToggleButton.setOnCheckedChangeListener(new onToggleCheckedListener());
		mAvailabilityToggleButton.setEnabled(false);
		
		mPhotoImageView = (ImageView)findViewById(R.id.my_profile_photo_item_photo);
		mPhotoImageView.setOnClickListener(photoClickListener);
		mPhotoButton = findViewById(R.id.my_profile_photo_item_photoButton);
		mPhotoButton.setOnClickListener(photoClickListener);
		mFreeTextView = (EditText)findViewById(R.id.my_profile_photo_item_free_text);	
		mFreeTextView.setOnLongClickListener(new onLongClickListener());
		mFreeTextView.addTextChangedListener(freeTextWatcher);
		
		// Edit web link
		View view = findViewById(R.id.edit_web_link_layout);
	    ((TextView)view.findViewById(R.id.my_profile_item_main_text)).setText(getString(R.string.rcs_eab_edit_weblink));
	    mWebLinkTextView = (TextView)findViewById(R.id.my_profile_item_detail_text);
		mWebLinkTextView.setText(getString(R.string.rcs_eab_edit_weblink_details));
		mWebLinkTextView.setVisibility(View.VISIBLE);
	    view.setOnClickListener(editWebLinkLayoutListener);
	    view.setOnFocusChangeListener(new OnLayoutFocusChangeListener());
	    
	    // Received invitations
	    mReceivedInvitationsLayout = findViewById(R.id.received_invitations_layout);
	    mReceivedInvitationsLayout.setOnFocusChangeListener(new OnLayoutFocusChangeListener());
	    ((TextView)mReceivedInvitationsLayout.findViewById(R.id.my_profile_item_main_text)).setText(getString(R.string.rcs_eab_profile_sharing_invitations_received));
	    mReceivedInvitationsTextView = (TextView)mReceivedInvitationsLayout.findViewById(R.id.my_profile_item_detail_text);
	    mReceivedInvitationsTextView.setVisibility(View.VISIBLE);
	    
	    // Sent invitations
	    mSentInvitationsLayout = findViewById(R.id.sent_invitations_layout);
	    mSentInvitationsLayout.setOnFocusChangeListener(new OnLayoutFocusChangeListener());
	    ((TextView)mSentInvitationsLayout.findViewById(R.id.my_profile_item_main_text)).setText(getString(R.string.rcs_eab_profile_sharing_invitations_sent));
	    mSentInvitationsTextView = (TextView)mSentInvitationsLayout.findViewById(R.id.my_profile_item_detail_text);
	    mSentInvitationsTextView.setVisibility(View.VISIBLE);
	    
	    // Blocked invitations
	    mBlockedInvitationsLayout = findViewById(R.id.blocked_invitations_layout);
	    mBlockedInvitationsLayout.setOnFocusChangeListener(new OnLayoutFocusChangeListener());
	    ((TextView)mBlockedInvitationsLayout.findViewById(R.id.my_profile_item_main_text)).setText(getString(R.string.rcs_eab_profile_sharing_invitations_declined));
	    mBlockedInvitationsTextView = (TextView)mBlockedInvitationsLayout.findViewById(R.id.my_profile_item_detail_text);
	    mBlockedInvitationsTextView.setVisibility(View.VISIBLE);
	    
	    Intent intent = getIntent();
	    if ((intent.getAction()!=null) 
        		&& intent.getAction().equalsIgnoreCase(Intent.ACTION_ATTACH_DATA)){
        	// We come here after a photo has been chosen from galery
        	Uri photoUri = intent.getData();
        	doResizePhotoAction(photoUri);
        }
	    
		// Get my profile number
		TextView myInfoView = (TextView)findViewById(R.id.my_profile_shared_info_item_left);
		// Format user name
		myInfoView.setText(R.string.rcs_eab_my_profile_info_item);

	}
	
	class OnLayoutFocusChangeListener implements OnFocusChangeListener{
		
		public void onFocusChange(View layout, boolean focused) {
			TextView primaryText = (TextView)layout.findViewById(R.id.my_profile_item_main_text);
			TextView secondaryText = (TextView)layout.findViewById(R.id.my_profile_item_detail_text);
			int primaryColorFocused = Color.BLACK;
			int primaryColorNormal = Color.WHITE;
			int secondaryColorFocused = Color.BLACK;
			int secondaryColorNormal = Color.rgb(204, 204, 204);
			
			if (focused){
				primaryText.setTextColor(primaryColorFocused);
				secondaryText.setTextColor(secondaryColorFocused);
			}else{
				primaryText.setTextColor(primaryColorNormal);
				secondaryText.setTextColor(secondaryColorNormal);
			}			
		}
		
	}
	
	/**
	 * Listener for long click on free text box
	 * 
	 * <br>Permits to select the last chosen free texts
	 */
	class onLongClickListener implements OnLongClickListener{
		
		public boolean onLongClick(View view) {
			
			Vector<String> freeTexts = new Vector<String>();
	        // Get the choices for free text
	  	    for (int i=1;i<=5;i++){
	  	    	// Read last five free texts
	  	    	String freeText = readString("freeTxt"+i,null);
	  	    	if (freeText!=null && (freeText.trim().length()>0)){
	  	    		freeTexts.addElement(freeText);
	  	    	}
	  	    }
	    	RcsSettings.createInstance(getApplicationContext());
	    	RcsSettings rcsSettings = RcsSettings.getInstance();
	    	// Read predefined free texts
	   		String freeText = rcsSettings.getPredefinedFreetext1();
	    	if (freeText!=null){
	    		if (freeText.trim().length()>0){
	    			freeTexts.addElement(freeText);
	    		}
	    	}
	    	freeText = rcsSettings.getPredefinedFreetext2();
	    	if (freeText!=null){
	    		if (freeText.trim().length()>0){
	    			freeTexts.addElement(freeText);
	    		}
	    	}    	   
	    	freeText = rcsSettings.getPredefinedFreetext3();
	    	if (freeText!=null){
	    		if (freeText.trim().length()>0){
	    			freeTexts.addElement(freeText);
	    		}
	    	}
	    	freeText = rcsSettings.getPredefinedFreetext4();
	    	if (freeText!=null){
	    		if (freeText.trim().length()>0){
	    			freeTexts.addElement(freeText);
	  	    	}
	  	    }
	    	final CharSequence[] items = new CharSequence[freeTexts.size()];
	    	for (int i=0;i<freeTexts.size();i++){
	    		items[i] = freeTexts.elementAt(i);
	    	}
	    	AlertDialog.Builder builder = new AlertDialog.Builder(ViewMyProfileActivity.this);
	    	builder.setTitle(getString(R.string.rcs_eab_choose_free_text));
	    	builder.setItems(items, new DialogInterface.OnClickListener(){
	    		public void onClick(DialogInterface dialog, int item){
	    			mFreeTextView.setText(items[item].toString());
	    		}
	    	});
	    	AlertDialog alert = builder.create();
	    	alert.show();
			return true;
		}
		
	}

	/**
	 * Listener on hyper-availability toggle button
	 */
	class onToggleCheckedListener implements OnCheckedChangeListener{
		
		public void onCheckedChanged(CompoundButton arg0, boolean checked) {
			userChangedAvailability = true;
			mUserChoosenAvailability = checked;
		}
		
	}

	protected void onSaveInstanceState(Bundle icicle) {
		super.onSaveInstanceState(icicle);
		icicle.putString("dialogPopupMessage", dialogPopupMessage);
		icicle.putParcelable("photo", mPhoto);
		icicle.putBoolean("photoChanged", mPhotoChanged);
		icicle.putBoolean("freeTextChanged", mFreeTextChanged);
		icicle.putBoolean("webLinkChanged", mWebLinkChanged);
		icicle.putBoolean("userChangedAvailability", userChangedAvailability);
		icicle.putBoolean("mUserChoosenAvailability", mUserChoosenAvailability);
		icicle.putString("favoriteLink", mChosenFavoriteLink);
		icicle.putString("freeText", mFreeText);
		mFreeTextSelectionEnd = mFreeTextView.getSelectionEnd();
		icicle.putInt("freeTextSelectionEnd", mFreeTextSelectionEnd);
		icicle.putString("imageUri",imageUri.toString());
	}

	
	protected void onRestoreInstanceState(Bundle icicle) {
		super.onRestoreInstanceState(icicle);
		dialogPopupMessage = icicle.getString("dialogPopupMessage");
        mPhoto = icicle.getParcelable("photo");
        if (mPhoto != null) {
            mPhotoImageView.setImageBitmap(mPhoto);
            setPhotoPresent(true);
        } else {
            mPhotoImageView.setImageResource(R.drawable.ic_contact_picture);
            setPhotoPresent(false);
        }
        mPhotoChanged = icicle.getBoolean("photoChanged");
        mFreeTextChanged = icicle.getBoolean("freeTextChanged");
        mWebLinkChanged = icicle.getBoolean("webLinkChanged");
        userChangedAvailability = icicle.getBoolean("userChangedAvailability");
        mUserChoosenAvailability = icicle.getBoolean("mUserChoosenAvailability");
        mChosenFavoriteLink = icicle.getString("favoriteLink");
        mFreeText = icicle.getString("freeText");
        mFreeTextSelectionEnd = icicle.getInt("freeTextSelectionEnd");
        imageUri = Uri.parse(icicle.getString("imageUri"));
	}

	
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_POPUP:
			return new AlertDialog.Builder(this).setIcon(
					R.drawable.rcs_common_flower_21x21).setMessage(dialogPopupMessage)
					.setPositiveButton(android.R.string.ok, null).create();
		case ID_PRESENCE_PUBLISH_PROGRESS:
			ProgressDialog progressDialog = new ProgressDialog(this);
    		progressDialog.setMessage(getString(R.string.rcs_eab_presencePublishInProgress));
    		progressDialog.setIndeterminate(true);
    		progressDialog.setCancelable(true);
    		return progressDialog;
		case ID_CONFIRM_QUIT_WITH_CHANGES:
			return new AlertDialog.Builder(this).setIcon(
					R.drawable.rcs_common_flower_21x21).setTitle(
							getString(R.string.rcs_eab_publish_changes_title)).setMessage(
							getString(R.string.rcs_eab_confirm_publish_quit_with_changes)).setPositiveButton(
							R.string.rcs_eab_publish, publishChangesBeforeQuit).setNeutralButton(
							R.string.rcs_common_discard, confirmQuitLoseChanges).setNegativeButton(
							R.string.rcs_common_cancel, cancelQuit).create();
		}
		return super.onCreateDialog(id);
	}

	
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case DIALOG_POPUP:
			((AlertDialog) dialog).setMessage(dialogPopupMessage);
			break;
		}
	}

    public void dismissPublishDialog(){
    	dismissDialog(ID_PRESENCE_PUBLISH_PROGRESS);
    }

	/**
	 * Read my profile from Eab provider and update the fields in layout
	 */
    private void readMyProfile() {

    	Thread readMyProfile = new Thread(){
    		public void run(){
   				// Get end user presence infos
   				info = presenceApi.getMyPresenceInfo();
    			if (info!=null){
    				// Get the availability status
    				final boolean mAvailability = info.isOnline();
    				Bitmap tempIcon = null;
    				if (mPhoto==null){
    					// Get presence icon
    					PhotoIcon photoIcon = info.getPhotoIcon();
    					if (photoIcon != null) {
    						byte[] data = info.getPhotoIcon().getContent();
    						tempIcon = BitmapFactory.decodeByteArray(data, 0, data.length);
    					}
    				}else{
    					tempIcon = mPhoto;
    				}
    				final Bitmap icon = tempIcon;

    				handler.post(new Runnable() {
    					public void run() {
    						// Update the lists
    						updateLists();

    						mAvailabilityToggleButton.setOnCheckedChangeListener(null);
    						// Update the availability button if needed
    						if (userChangedAvailability){
    							// If the user changed it, take from variable
    							mAvailabilityToggleButton.setChecked(mUserChoosenAvailability);
    						}else{
    							// The user did not change it, so take from info
   								mAvailabilityToggleButton.setChecked(mAvailability);
    						}
							mAvailabilityToggleButton.setOnCheckedChangeListener(new onToggleCheckedListener());
								
    						if (icon != null) {
    							// If presence icon not null, we use it
    							mPhotoImageView.setImageBitmap(icon);
    							mPhoto = icon;
    							setPhotoPresent(true);
    						} else {
    							// We use the default no photo icon
    							mPhotoImageView.setImageResource(R.drawable.rcs_default_portrait_90x90);
    							mPhoto = null;
    							setPhotoPresent(false);
    						}

    						if (info != null) {

    							// Free text
    							if (mFreeText!=null){
    								// If free text has been saved, set edit text and cursor position
    								mFreeTextView.setText(mFreeText);
    								Selection.setSelection(mFreeTextView.getText(), mFreeTextSelectionEnd);
    							}else if (info.getFreetext() == null
    									|| info.getFreetext().length() < 1) {
    								mFreeTextView.setText(null);
    							}else{
    								mFreeTextView.setText(info.getFreetext());
    							}
    							
    							// Check if chosen web link is different from last one
    							if (info.getFavoriteLinkUrl()==null){
    								mWebLinkChanged = ((mChosenFavoriteLink!=null) && (mChosenFavoriteLink.length()>0));
    							}else if (mChosenFavoriteLink!=null){
    								// only if we changed our favorite link, else no need to test 
    								mWebLinkChanged = !(info.getFavoriteLinkUrl().equalsIgnoreCase(mChosenFavoriteLink)); 	
    							}
    							
    							if (mChosenFavoriteLink!=null 
    									&& mChosenFavoriteLink.length()>0){
    								mWebLinkTextView.setText(UrlUtils.formatUrlToDomainName(mChosenFavoriteLink));
    							}else if (mChosenFavoriteLink==null){
    								// We did not choose a favorite link, so take the last one
    								if (info.getFavoriteLinkUrl()==null){
    									mWebLinkTextView.setText(getString(R.string.rcs_eab_edit_weblink_details));
    								}else{
    									mWebLinkTextView.setText(UrlUtils.formatUrlToDomainName(info.getFavoriteLinkUrl()));
    								}
    							}else if (mChosenFavoriteLink.length()==0){
    								// We chose a null link in edit web link activity or info.getWebLink was already null
    								mWebLinkTextView.setText(getString(R.string.rcs_eab_edit_weblink_details));
    							}

    							mLastUpdateView.setText(DateUtils.formatLastUpdateTime(getContentResolver(), info.getTimestamp()));
    						}
    					}
    				});
    			}
    		}
    	};
    	readMyProfile.start();
    }


    protected void onResume() {
    	super.onResume();
    	// If we are not connected, get infos from database
    	if (presenceApi != null) {
    		if (!apiConnected) {
    			presenceApi.connectApi();
    			presenceApi.addApiEventListener(this);	
    		}
    	}
    	readMyProfile();
    }

	
	public boolean onCreateOptionsMenu(Menu menu) {

		// Publish
		menu.add(0, MENU_ITEM_PUBLISH, 0, R.string.rcs_eab_menu_publish).setIcon(
				android.R.drawable.ic_menu_save);
		// Revert
		menu.add(0, MENU_ITEM_REVERT, 0, R.string.menu_doNotSave).setIcon(
				android.R.drawable.ic_menu_close_clear_cancel);

        mPhotoMenuItem = menu.add(0, MENU_ITEM_PHOTO, 0, null);
        // Updates the state of the menu item
        setPhotoPresent(mPhotoPresent);
		
		return super.onCreateOptionsMenu(menu);
	}
	
	public boolean onPrepareOptionsMenu(Menu menu){
		menu.clear();

		if (apiConnected){
			// Publish
			menu.add(0, MENU_ITEM_PUBLISH, 0, R.string.rcs_eab_menu_publish).setIcon(
					android.R.drawable.ic_menu_save);
			// Revert
			menu.add(0, MENU_ITEM_REVERT, 0, R.string.rcs_common_cancel).setIcon(
					android.R.drawable.ic_menu_close_clear_cancel);

			// Photo
	        mPhotoMenuItem = menu.add(0, MENU_ITEM_PHOTO, 0, null);

	        // Updates the state of the menu item
	        setPhotoPresent(mPhotoPresent);
		}
		
		return super.onPrepareOptionsMenu(menu);
	}

	/**
	 * Set photo and menus according to its existence
	 * (ie hide or show the corresponding menus)
	 *  
	 * @param present The picture has been set
	 */
    private void setPhotoPresent(boolean present) {
        mPhotoImageView.setVisibility(present ? View.VISIBLE : View.GONE);
        mPhotoButton.setVisibility(present ? View.GONE : View.VISIBLE);
        mPhotoPresent = present;
        if (mPhotoMenuItem != null) {
            if (present) {
                mPhotoMenuItem.setTitle(R.string.removePicture);
                mPhotoMenuItem.setIcon(android.R.drawable.ic_menu_delete);
            } else {
                mPhotoMenuItem.setTitle(R.string.addPicture);
                mPhotoMenuItem.setIcon(R.drawable.ic_menu_add_picture);
            }
        }
    }
	
    /**
     * A menu item has been selected
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ITEM_PUBLISH:
            	doPublish();
            	return true;
    
            case MENU_ITEM_REVERT:
                doRevertAction();
                return true;
    
            case MENU_ITEM_PHOTO:
                if (!mPhotoPresent) {
                	showPhotoChooserDialog();
                } else {
                    doRemovePhotoAction();
                }
                return true;
        }

        return false;
    }

    /**
     * Quit without conserving the modifications that were made
     * <br>If a field has changed, ask the user for a confirmation
     */
    private void doRevertAction() {
    	
    	boolean mAvailabilityChanged = false;
    	if (info!=null){
        	// Check free text changed
    		if (info.getFreetext()!=null){
    			mFreeTextChanged = (!info.getFreetext().equalsIgnoreCase(mFreeTextView.getText().toString()));
    		}else{
    			mFreeTextChanged = (mFreeTextView.getText().toString().length()>0);
    		}
    	
    		if (mAvailabilityToggleButton!=null){
    			// Check availability has changed
    			mAvailabilityChanged = ((info.isOffline() && mAvailabilityToggleButton.isChecked()) 
    					|| (info.isOnline() && !mAvailabilityToggleButton.isChecked()));
    		}
    	}
   		
    	if ((apiConnected) && (mFreeTextChanged || mPhotoChanged || mWebLinkChanged || mAvailabilityChanged)){
    		// If something has changed, show dialog
    		showDialog(ID_CONFIRM_QUIT_WITH_CHANGES);
    	}else{
    		finish();
    	}
    }

    /**
     * Get a new photo icon from the gallery
     */
    private void doPickPhotoFromGalleryAction() {
        try {
        	Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
        	intent.setType("image/*");
        	intent.putExtra("crop", "true");
        	intent.putExtra("aspectX", 1);
        	intent.putExtra("aspectY", 1);
        	intent.putExtra("outputX", 96);
        	intent.putExtra("outputY", 96);
        	intent.putExtra("return-data", true);
        	startActivityForResult(intent, PHOTO_PICKED_WITH_DATA);
        } catch (ActivityNotFoundException e) {
            new AlertDialog.Builder(ViewMyProfileActivity.this)
                .setTitle(R.string.errorDialogTitle)
                .setMessage(R.string.photoPickerNotFoundText)
                .setPositiveButton(android.R.string.ok, null)
                .show();
        }
    }
    
    /**
     * Get a new photo icon from camera
     */
    private void doPickPhotoFromCameraAction(){
    	try{
    		//define the file-name to save photo taken by Camera activity
    		String fileName = "temp_RCS_profile_photo.jpg";
    		//create parameters for Intent with filename
    		ContentValues values = new ContentValues();
    		values.put(MediaStore.Images.Media.TITLE, fileName);
    		values.put(MediaStore.Images.Media.DESCRIPTION,"Image capture by camera for my RCS profile");
    		//imageUri is the current activity attribute, define and save it for later usage (also in onSaveInstanceState)
    		imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    		//create new Intent
    		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    		intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
    		startActivityForResult(intent, PHOTO_PICKED_FROM_CAMERA);
        } catch (ActivityNotFoundException e) {
            new AlertDialog.Builder(ViewMyProfileActivity.this)
                .setTitle(R.string.errorDialogTitle)
                .setMessage(R.string.photoPickerNotFoundText)
                .setPositiveButton(android.R.string.ok, null)
                .show();
        }
    }

    /**
     * Resize the photo icon
     * 
     * @param uri
     */
    private void doResizePhotoAction(Uri uri){
    	if (logger.isActivated()){
    		logger.info("Do the image resizing for "+uri);
    	}
    	try {
    		/* Available for the moment, but might be removed in future releases... */
    		Intent cropIntent = new Intent("com.android.camera.action.CROP");
    		cropIntent.setDataAndType(uri,"image/*");
    		cropIntent.putExtra("aspectX", 1);
    		cropIntent.putExtra("aspectY", 1);
    		cropIntent.putExtra("outputX", 96);
    		cropIntent.putExtra("outputY", 96);
    		cropIntent.putExtra("return-data", true);
    		startActivityForResult(cropIntent, PHOTO_PICKED_WITH_DATA);
    	} catch (Exception e) {
    		new AlertDialog.Builder(ViewMyProfileActivity.this)
    		.setTitle(R.string.errorDialogTitle)
    		.setMessage(R.string.photoPickerNotFoundText)
    		.setPositiveButton(android.R.string.ok, null)
    		.show();
    		if (logger.isActivated()){
    			logger.error("Error while resizing",e);
    		}
    	}    	
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	
    	if (resultCode != RESULT_OK) {
            return;
        }

        switch (requestCode) {
            case PHOTO_PICKED_WITH_DATA: {
            	// A new photo has been picked
                final Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap photo = extras.getParcelable("data");
                    mPhoto = photo;
                    mPhotoChanged = true;
                    mPhotoImageView.setImageBitmap(mPhoto);
                    setPhotoPresent(true);
                }
                break;
            }
            
            case PHOTO_PICKED_FROM_CAMERA:{
            	doResizePhotoAction(imageUri);
            	break;
            }
            
            case WEBLINK_PICKED_WITH_DATA: {
            	// A new weblink has been picked
            	mChosenFavoriteLink = data.getStringExtra("favoriteWebLink");
            	if (mChosenFavoriteLink == null){
            		// If the result is null, it means an empty string has been chosen as new url (as result code is OK)
            		mChosenFavoriteLink = "";
            	}

            	break;
            }
        }
    }


    /**
     * Remove the photo
     */
    private void doRemovePhotoAction() {
        mPhoto = null;
        mPhotoChanged = true;
        mPhotoImageView.setImageResource(R.drawable.ic_contact_picture);
        setPhotoPresent(false);
    }
    
	protected void onDestroy() {
		if (presenceApi != null) {
			presenceApi.removeAllApiEventListeners();
			presenceApi.disconnectApi();
		}
		
		// Unregister broadcast receivers
		unregisterReceiver(mEndUserChangedIntentReceiver);
		unregisterReceiver(presenceSharingChangedIntentReceiver);
		
		super.onDestroy();
	}

    /**
     * Publish the presence information as they are selected for now
     */
    private void doPublish(){
    	if (info!=null){

    		showDialog(ID_PRESENCE_PUBLISH_PROGRESS);

    		Thread publish = new Thread(new Runnable(){
    			public void run(){

    				String selectedFreeText = mFreeTextView.getText().toString();
   					mFreeTextChanged = (!selectedFreeText.equalsIgnoreCase(info.getFreetext()));
    				info.setFreetext(selectedFreeText);
    				if (selectedFreeText.trim().length()>0){
    					// Save free text in registry if necessary
    					if (!isFreeTxtInRegistry(selectedFreeText)){
    						saveFreeTextInRegistry(selectedFreeText);
    					}      			
    				}

    				if (mChosenFavoriteLink!=null 
							&& mChosenFavoriteLink.length()>0){
						info.setFavoriteLink(new FavoriteLink(mChosenFavoriteLink));
					}else if (mChosenFavoriteLink==null){
						// We did not choose a favorite link, so take the last one
						if (info.getFavoriteLinkUrl()==null){
							info.setFavoriteLink(null);
						}else{
							info.setFavoriteLink(info.getFavoriteLink());
						}
					}else if (mChosenFavoriteLink.length()==0){
						// We chose a null link in edit web link activity or info.getWebLink was already null
						info.setFavoriteLink(null);
					}
    				
    				boolean result = false;
    				if (mPhotoChanged) {
    					PhotoIcon photoIcon = null;
    					if (mPhoto != null) {
    						// Photo changed and not null
    						ByteArrayOutputStream stream = new ByteArrayOutputStream();
    						mPhoto.compress(Bitmap.CompressFormat.JPEG, 100, stream);
    						byte[] content = stream.toByteArray();
    						photoIcon = new PhotoIcon(content, mPhoto.getWidth(), mPhoto.getHeight());
    					}
    					info.setPhotoIcon(photoIcon);
    		        }
    				// Set the availability
    				if (mAvailabilityToggleButton.isChecked()){
    					info.setPresenceStatus(PresenceInfo.ONLINE);
    				}else{
    					info.setPresenceStatus(PresenceInfo.OFFLINE);
    				}
		        	// Update presence info
    				try {
    					result = presenceApi.setMyPresenceInfo(info);
    				} catch (ClientApiException e) {
    					DialogUtils.displayToast(ViewMyProfileActivity.this, handler, getString(R.string.rcs_eab_presencePublishError));	
    				} catch (RuntimeException re){
    					DialogUtils.displayToast(ViewMyProfileActivity.this, handler, getString(R.string.rcs_eab_presencePublishError));	
    				}
					if (result){
						DialogUtils.displayToast(ViewMyProfileActivity.this, handler, getString(R.string.rcs_eab_label_user_profile_updated));
					}else{
						DialogUtils.displayToast(ViewMyProfileActivity.this, handler, getString(R.string.rcs_eab_label_user_profile_not_updated));
					}
    				
    				handler.post(new Runnable(){
    					public void run(){
    						dismissPublishDialog();	
    						// Quit profile editing
    						finish();
    					}
    				});
    			}
    		});
    		publish.start();
    	}
    }

    /**
     * Broadcast receiver for receiving end user presence info changes
     */
	private final BroadcastReceiver mEndUserChangedIntentReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			// Reread the profile and refresh it
			readMyProfile();
		}
	};

    /**
     * Broadcast receiver for receiving presence sharing info changes
     */
	private final BroadcastReceiver presenceSharingChangedIntentReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			handler.post(new Runnable(){
				public void run(){
					// Update the lists of presence sharing status
					updateLists();
				}
			});
		}
	};

	public void handleApiConnected() {
		apiConnected = true;
		if (mAvailabilityToggleButton!=null){
			mAvailabilityToggleButton.setEnabled(true);
		}
		readMyProfile();
	}
	
	public void handleApiDisabled(){
		apiConnected = false;
		if (mAvailabilityToggleButton!=null){
			mAvailabilityToggleButton.setEnabled(false);
		}
	}

	public void handleImsConnected(){
		//TODO
	}
	
	public void handleImsDisconnected(){
		//TODO
	}
	
	/**
	 * Update the lists of presence sharing status
	 */
	private void updateLists(){
		// Get number of received invitations (includes cancelled ones)
		ContactsApi contactsApi = new ContactsApi(this);		
		List<String> willingContacts = contactsApi.getRcsWillingContacts();
		List<String> cancelledContacts = contactsApi.getRcsCancelledContacts();
		numberOfReceivedInvitations = willingContacts.size() + cancelledContacts.size();

		// Set the UI accordingly
		if (numberOfReceivedInvitations==0){
			mReceivedInvitationsLayout.setOnClickListener(null);
			(mReceivedInvitationsLayout.findViewById(R.id.my_profile_item_right_button)).setVisibility(View.GONE);
			mReceivedInvitationsTextView.setText(getString(R.string.rcs_eab_noReceivedInvitation));
		}else if (numberOfReceivedInvitations==1){
			(mReceivedInvitationsLayout.findViewById(R.id.my_profile_item_right_button)).setVisibility(View.VISIBLE);
			mReceivedInvitationsLayout.setOnClickListener(receivedInvitationLayoutListener);
			mReceivedInvitationsTextView.setText(getString(R.string.rcs_eab_oneReceivedInvitation));
		}else{
			mReceivedInvitationsLayout.setClickable(true);
			mReceivedInvitationsLayout.setFocusable(true);
			(mReceivedInvitationsLayout.findViewById(R.id.my_profile_item_right_button)).setVisibility(View.VISIBLE);
			mReceivedInvitationsLayout.setOnClickListener(receivedInvitationLayoutListener);
			mReceivedInvitationsTextView.setText(numberOfReceivedInvitations+ " " + getString(R.string.rcs_eab_manyReceivedInvitation));
		}
		
		// Get number of sent invitations
		List<String> invitedContacts = contactsApi.getRcsInvitedContacts();
		numberOfSentInvitations = invitedContacts.size();

		// Set the UI accordingly
		if (numberOfSentInvitations==0){
			mSentInvitationsLayout.setOnClickListener(null);
			(mSentInvitationsLayout.findViewById(R.id.my_profile_item_right_button)).setVisibility(View.GONE);
			mSentInvitationsTextView.setText(getString(R.string.rcs_eab_noSentInvitation));

		}else if (numberOfSentInvitations==1){
			(mSentInvitationsLayout.findViewById(R.id.my_profile_item_right_button)).setVisibility(View.VISIBLE);
			mSentInvitationsLayout.setOnClickListener(sentInvitationLayoutListener);
			mSentInvitationsTextView.setText(getString(R.string.rcs_eab_oneSentInvitation));
			mSentInvitationsTextView.setTextAppearance(getApplicationContext(), R.drawable.rcs_eab_my_profile_text_secondary_color);
		}else{
			(mSentInvitationsLayout.findViewById(R.id.my_profile_item_right_button)).setVisibility(View.VISIBLE);
			mSentInvitationsLayout.setOnClickListener(sentInvitationLayoutListener);
			mSentInvitationsTextView.setText(numberOfSentInvitations+ " " + getString(R.string.rcs_eab_manySentInvitation));
			mSentInvitationsTextView.setTextAppearance(getApplicationContext(), R.drawable.rcs_eab_my_profile_text_secondary_color);
		}
		
		// Get number of blocked contacts
		List<String> blockedContacts = contactsApi.getRcsBlockedContacts();
		numberOfBlockedInvitations = blockedContacts.size();
		
		// Set the UI accordingly
		if (numberOfBlockedInvitations==0){
			mBlockedInvitationsLayout.setOnClickListener(null);
			(mBlockedInvitationsLayout.findViewById(R.id.my_profile_item_right_button)).setVisibility(View.GONE);
			mBlockedInvitationsTextView.setText(getString(R.string.rcs_eab_noDeclinedInvitation));
		}else if (numberOfBlockedInvitations==1){
			(mBlockedInvitationsLayout.findViewById(R.id.my_profile_item_right_button)).setVisibility(View.VISIBLE);
			mBlockedInvitationsLayout.setOnClickListener(blockedInvitationLayoutListener);
			mBlockedInvitationsTextView.setText(getString(R.string.rcs_eab_oneDeclinedInvitation));
		}else{
			(mBlockedInvitationsLayout.findViewById(R.id.my_profile_item_right_button)).setVisibility(View.VISIBLE);
			mBlockedInvitationsLayout.setOnClickListener(blockedInvitationLayoutListener);
			mBlockedInvitationsTextView.setText(numberOfBlockedInvitations+ " " + getString(R.string.rcs_eab_manyDeclinedInvitation));
		}
		
	}

	// Listeners
	/**
	 * We choosed to publish the changes made before leaving the activity
	 */
	private android.content.DialogInterface.OnClickListener publishChangesBeforeQuit = new android.content.DialogInterface.OnClickListener(){
		public void onClick(DialogInterface arg0, int arg1) {
			doPublish();
		}
	};
	
	/**
	 * We choosed to quit the activity without publishing the changes
	 */
	private android.content.DialogInterface.OnClickListener confirmQuitLoseChanges = new android.content.DialogInterface.OnClickListener(){
		public void onClick(DialogInterface arg0, int arg1) {
			finish();
		}
	};

	/**
	 * We choosed to cancel the leaving
	 */
	private android.content.DialogInterface.OnClickListener cancelQuit = new android.content.DialogInterface.OnClickListener(){
		public void onClick(DialogInterface arg0, int arg1) {
			dismissDialog(ID_CONFIRM_QUIT_WITH_CHANGES);
		}
	};
	
	/**
	 * We clicked on the photo
	 */
	private OnClickListener photoClickListener = new OnClickListener(){
		public void onClick(View view) {
			showPhotoChooserDialog();
		}
	};
	
	private void showPhotoChooserDialog(){
		final String takePhotoFromCamera = "takePhotoFromCamera";
		final String takePhotoFromGallery = "takePhotoFromGallery";
		final String removePhoto = "removePhoto";
		
        final Context dialogContext = new ContextThemeWrapper(ViewMyProfileActivity.this, android.R.style.Theme_Light);
        final LayoutInflater dialogInflater = (LayoutInflater)dialogContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        ArrayList<String> choices = new ArrayList<String>();

        choices.add(takePhotoFromCamera);
        choices.add(takePhotoFromGallery);
        if (mPhotoPresent){
        	choices.add(removePhoto);
        }
        
        final ArrayAdapter<String> choiceAdapter = new ArrayAdapter<String>(ViewMyProfileActivity.this,
                android.R.layout.simple_list_item_1, choices) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                if (convertView == null) {
                    convertView = dialogInflater.inflate(android.R.layout.simple_list_item_1,
                            parent, false);
                }

                final TextView text1 = (TextView)convertView.findViewById(android.R.id.text1);

                final String choice = this.getItem(position);
                
                if (choice.equalsIgnoreCase(takePhotoFromCamera)){
                	// Take photo from camera
	                text1.setText(getString(R.string.rcs_eab_take_photo_from_camera));
                }else if (choice.equalsIgnoreCase(takePhotoFromGallery)){
                	// Take photo from gallery
	                text1.setText(getString(R.string.rcs_eab_take_photo_from_gallery));
                }else{
                	// remove photo
	                text1.setText(getString(R.string.rcs_eab_remove_photo));
                }
                
                return convertView;
            }
        };

        final DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                // Create new contact based on selected source
                final String choice = choiceAdapter.getItem(which);
                
                if (choice.equalsIgnoreCase(takePhotoFromCamera)){
                	// Take photo from camera
	                doPickPhotoFromCameraAction();
                }else if (choice.equalsIgnoreCase(takePhotoFromGallery)){
                	// Take photo from gallery
                	doPickPhotoFromGalleryAction();
                }else{
                	// remove photo
	                doRemovePhotoAction();
                }
            }
        };

        final DialogInterface.OnCancelListener cancelListener = new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
            }
        };

        final AlertDialog.Builder builder = new AlertDialog.Builder(ViewMyProfileActivity.this);
        builder.setTitle(R.string.rcs_eab_dialog_choice_title);
        builder.setSingleChoiceItems(choiceAdapter, 0, clickListener);
        builder.setOnCancelListener(cancelListener);
        Dialog dialog = builder.create();
        dialog.show();
	}
	
	
	/**
	 * We clicked on the edit weblink layout
	 */
	private OnClickListener editWebLinkLayoutListener = new OnClickListener(){
		public void onClick(View view) {
	        Intent editWebLinkIntent = new Intent(view.getContext(), EditWebLinkActivity.class);
	        editWebLinkIntent.putExtra("return-data", true);
	        String link = null;
	        if (mChosenFavoriteLink!=null){
	        	link = mChosenFavoriteLink;
	        }else if (info!=null && (info.getFavoriteLink() != null)) {
	        	link = info.getFavoriteLinkUrl();
	        }
	        editWebLinkIntent.putExtra("incomingUrl", link);
	        startActivityForResult(editWebLinkIntent, WEBLINK_PICKED_WITH_DATA);
		}
	};

	
	/**
	 * We clicked on the received invitations layout
	 */
	private OnClickListener receivedInvitationLayoutListener = new OnClickListener(){
		public void onClick(View view) {
	        Intent receivedInvitationsIntent = new Intent(view.getContext(), ReceivedInvitationsActivity.class);
			startActivity(receivedInvitationsIntent);		
		}
	};
	
	/**
	 * We clicked on the sent invitations layout
	 */
	private OnClickListener sentInvitationLayoutListener = new OnClickListener(){
		public void onClick(View view) {
	        Intent sentInvitationsIntent = new Intent(view.getContext(), SentInvitationsActivity.class);
			startActivity(sentInvitationsIntent);		
		}
	};

	/**
	 * We clicked on the blocked invitations layout
	 */
	private OnClickListener blockedInvitationLayoutListener = new OnClickListener(){
		public void onClick(View view) {
	        Intent blockedInvitationsIntent = new Intent(view.getContext(), BlockedInvitationsActivity.class);
			startActivity(blockedInvitationsIntent);		
		}
	};
    
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK: {
            	// Back key : we quit without saving modifications
            	doRevertAction();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
	
	
	public void handleApiDisconnected() {
		apiConnected = false;
		mAvailabilityToggleButton.setEnabled(false);
	}
	
	/**
	 * Check if given free text is in registry 
	 * 
	 * @param value
	 * @return true if given value is in registry
	 */
	private boolean isFreeTxtInRegistry(String value){
		if (value==null){
			return true;
		}
		// Search within the last used free texts
		for (int i=1;i<=5;i++){			
			if(value.equals(readString("freeTxt"+i,null))){
				return true;
			}
		}
		// Search within the settings predefined free text
		if (RcsSettings.getInstance()==null){
			RcsSettings.createInstance(getApplicationContext());
		}
    	RcsSettings rcsSettings = RcsSettings.getInstance();

    	if (rcsSettings.getPredefinedFreetext1().equalsIgnoreCase(value)) return true;
    	if (rcsSettings.getPredefinedFreetext2().equalsIgnoreCase(value)) return true;
    	if (rcsSettings.getPredefinedFreetext3().equalsIgnoreCase(value)) return true;
		if (rcsSettings.getPredefinedFreetext4().equalsIgnoreCase(value)) return true;
		
		return false;
	}


	/**
	 * Save given free text in registry
	 * The last five free texts are conserved, older ones are deleted
	 * 
	 * @param value
	 */
	private void saveFreeTextInRegistry(String value){
		for(int i=5;i>1;i--){
			String lastFreeText = readString("freeTxt"+(i-1), null);
			if (lastFreeText!=null){
				writeString("freeTxt"+i,lastFreeText);
			}
		}
		writeString("freeTxt1",value);
	}
	
	/**
	 * Read a string value in the registry
	 * 
	 * @param key Key name to be read
	 * @param defaultValue Default value
	 * @return String
	 */
	public String readString(String key, String defaultValue) {
		if (getSharedPreferences("myProfileRegistry", MODE_PRIVATE) != null) {
			return getSharedPreferences("myProfileRegistry", MODE_PRIVATE)
					.getString(key, defaultValue);
		} else {
			return null;
		}
	}

	/**
	 * Write a string value in the registry
	 * 
	 * @param key Key name to be updated
	 * @param value New value
	 */
	public void writeString(String key, String value) {
		SharedPreferences.Editor editor = getSharedPreferences("myProfileRegistry",MODE_PRIVATE).edit();
		editor.putString(key, value);
		editor.commit();
	}
	
	/**
	 * Watcher on edit free text
	 */
	private TextWatcher freeTextWatcher = new TextWatcher(){
		
		public void afterTextChanged(Editable editable) {
			mFreeText = editable.toString();
		}
		
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		}
		
		public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		}
	};
	
}
