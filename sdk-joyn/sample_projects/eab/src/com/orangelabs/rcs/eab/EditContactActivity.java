/*******************************************************************************
 * Software Name : RCS
 *
 * Copyright © 2010 France Telecom S.A.
 ******************************************************************************/

package com.orangelabs.rcs.eab;

import com.orangelabs.rcs.eab.R;
import com.orangelabs.rcs.common.ContactUtils;
import com.orangelabs.rcs.common.EabUtils;
import com.orangelabs.rcs.common.PhoneNumber;
import com.orangelabs.rcs.common.dialog.DialogUtils;
import com.orangelabs.rcs.eab.edit.Lists;
import com.orangelabs.rcs.eab.edit.model.ContactsSource;
import com.orangelabs.rcs.eab.edit.model.Editor;
import com.orangelabs.rcs.eab.edit.model.EntityDelta;
import com.orangelabs.rcs.eab.edit.model.EntityModifier;
import com.orangelabs.rcs.eab.edit.model.EntitySet;
import com.orangelabs.rcs.eab.edit.model.GoogleSource;
import com.orangelabs.rcs.eab.edit.model.Sources;
import com.orangelabs.rcs.eab.edit.model.ContactsSource.EditType;
import com.orangelabs.rcs.eab.edit.model.Editor.EditorListener;
import com.orangelabs.rcs.eab.edit.model.EntityDelta.ValuesDelta;
import com.orangelabs.rcs.eab.edit.widget.BaseContactEditorView;
import com.orangelabs.rcs.eab.edit.widget.PhotoEditorView;
import com.orangelabs.rcs.eab.edit.EmptyService;
import com.orangelabs.rcs.eab.edit.WeakAsyncTask;
import com.orangelabs.rcs.service.api.client.ClientApiException;
import com.orangelabs.rcs.service.api.client.contacts.ContactsApi;
import com.orangelabs.rcs.service.api.client.presence.PresenceApi;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts.Data;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Activity for editing or inserting a contact.
 */
public final class EditContactActivity extends Activity
        implements View.OnClickListener, Comparator<EntityDelta> {

    /** The launch code when picking a photo and the raw data is returned */
    private static final int PHOTO_PICKED_WITH_DATA = 3021;

    private static final String KEY_EDIT_STATE = "state";
    private static final String KEY_RAW_CONTACT_ID_REQUESTING_PHOTO = "photorequester";

    /** The result code when view activity should close after edit returns */
    public static final int RESULT_CLOSE_VIEW_ACTIVITY = 777;

    public static final int SAVE_MODE_DEFAULT = 0;

    private long mRawContactIdRequestingPhoto = -1;

    private static final int DIALOG_CONFIRM_DELETE = 1;
    private static final int DIALOG_CONFIRM_READONLY_DELETE = 2;
    private static final int DIALOG_CONFIRM_MULTIPLE_DELETE = 3;
    private static final int DIALOG_CONFIRM_READONLY_HIDE = 4;

    String mQuerySelection;

    EntitySet mState;

    /** The linear layout holding the ContactEditorViews */
    LinearLayout mContent;

    private ArrayList<Dialog> mManagedDialogs = Lists.newArrayList();

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Intent intent = getIntent();
        final String action = intent.getAction();

        setContentView(R.layout.act_edit);

        presenceApi = new PresenceApi(getApplicationContext());
		if (presenceApi!=null){
			presenceApi.connectApi();
		}
		
        // Build editor and listen for photo requests
        mContent = (LinearLayout) findViewById(R.id.editors);

        findViewById(R.id.btn_done).setOnClickListener(this);
        findViewById(R.id.btn_discard).setOnClickListener(this);

        // Handle initial actions only when existing state missing
        final boolean hasIncomingState = icicle != null && icicle.containsKey(KEY_EDIT_STATE);

        if (Intent.ACTION_EDIT.equals(action) && !hasIncomingState) {
            // Read initial state from database
            new QueryEntitiesTask(this).execute(intent);
        } else if (Intent.ACTION_INSERT.equals(action) && !hasIncomingState) {
            // Trigger dialog to pick account type
            doAddAction();
        }
    }

    private static class QueryEntitiesTask extends
            WeakAsyncTask<Intent, Void, Void, EditContactActivity> {
        public QueryEntitiesTask(EditContactActivity target) {
            super(target);
        }

        @Override
        protected Void doInBackground(EditContactActivity target, Intent... params) {
            // Load edit details in background
            final Context context = target;
            final Sources sources = Sources.getInstance(context);
            final Intent intent = params[0];

            final ContentResolver resolver = context.getContentResolver();

            // Handle both legacy and new authorities
            final Uri data = intent.getData();
            final String authority = data.getAuthority();
            final String mimeType = intent.resolveType(resolver);

            String selection = "0";
            if (ContactsContract.AUTHORITY.equals(authority)) {
                if (Contacts.CONTENT_ITEM_TYPE.equals(mimeType)) {
                	// Handle selected aggregate
                    final long contactId = ContentUris.parseId(data);
                    selection = RawContacts.CONTACT_ID + "=" + contactId;
                } else if (RawContacts.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    final long rawContactId = ContentUris.parseId(data);
                    final long contactId = ContactUtils.queryForContactId(resolver, rawContactId);
                    selection = RawContacts.CONTACT_ID + "=" + contactId;
                }
            } else if (android.provider.Contacts.AUTHORITY.equals(authority)) {
                final long rawContactId = ContentUris.parseId(data);
                selection = Data.RAW_CONTACT_ID + "=" + rawContactId;
            }

            target.mQuerySelection = selection;
            target.mState = EntitySet.fromQuery(resolver, selection, null, null);

            // Handle any incoming values that should be inserted
            final Bundle extras = intent.getExtras();
            final boolean hasExtras = extras != null && extras.size() > 0;
            final boolean hasState = target.mState.size() > 0;
            if (hasExtras && hasState) {
                // Find source defining the first RawContact found
                final EntityDelta state = target.mState.get(0);
                final String accountType = state.getValues().getAsString(RawContacts.ACCOUNT_TYPE);
                final ContactsSource source = sources.getInflatedSource(accountType,
                        ContactsSource.LEVEL_CONSTRAINTS);
                EntityModifier.parseExtras(context, source, state, extras);
            }

            return null;
        }

        @Override
        protected void onPostExecute(EditContactActivity target, Void result) {
            // Bind UI to new background state
            target.bindEditors();
        }
    }



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (hasValidState()) {
            // Store entities with modifications
            outState.putParcelable(KEY_EDIT_STATE, mState);
        }

        outState.putLong(KEY_RAW_CONTACT_ID_REQUESTING_PHOTO, mRawContactIdRequestingPhoto);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // Read modifications from instance
        mState = savedInstanceState.<EntitySet> getParcelable(KEY_EDIT_STATE);
        mRawContactIdRequestingPhoto = savedInstanceState.getLong(
                KEY_RAW_CONTACT_ID_REQUESTING_PHOTO);
        bindEditors();

        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
		
        if (presenceApi!=null){
			presenceApi.disconnectApi();
		}
		
        for (Dialog dialog : mManagedDialogs) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_CONFIRM_DELETE:
            	// No rcs numbers, normal deletion	            	
            	return new AlertDialog.Builder(this)
            			.setTitle(R.string.deleteConfirmation_title)
            			.setIcon(android.R.drawable.ic_dialog_alert)
            			.setMessage(R.string.deleteConfirmation)
            			.setNegativeButton(android.R.string.cancel, null)
            			.setPositiveButton(android.R.string.ok, new DeleteClickListener())
            			.setCancelable(false)
            			.create();
            case DIALOG_CONFIRM_READONLY_DELETE:
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.deleteConfirmation_title)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.readOnlyContactDeleteConfirmation)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, new DeleteClickListener())
                        .setCancelable(false)
                        .create();
            case DIALOG_CONFIRM_MULTIPLE_DELETE:
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.deleteConfirmation_title)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.multipleContactDeleteConfirmation)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(android.R.string.ok, new DeleteClickListener())
                        .setCancelable(false)
                        .create();
            case DIALOG_CONFIRM_READONLY_HIDE:
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.deleteConfirmation_title)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.readOnlyContactWarning)
                        .setPositiveButton(android.R.string.ok, new DeleteClickListener())
                        .setCancelable(false)
                        .create();
                /*
                 * Added for RCS
                 */
            case ID_REVOKE_PROGRESS:
            	ProgressDialog revokeDialog = new ProgressDialog(this);
            	revokeDialog.setMessage(getString(R.string.rcs_eab_revokeContactInProgress));
            	revokeDialog.setIndeterminate(true);
            	revokeDialog.setCancelable(true);
            	return revokeDialog;
            	/*
            	 * End added for RCS
            	 */
        }
        return null;
    }

    /**
     * Start managing this {@link Dialog} along with the {@link Activity}.
     */
    private void startManagingDialog(Dialog dialog) {
        synchronized (mManagedDialogs) {
            mManagedDialogs.add(dialog);
        }
    }

    /**
     * Show this {@link Dialog} and manage with the {@link Activity}.
     */
    void showAndManageDialog(Dialog dialog) {
        startManagingDialog(dialog);
        dialog.show();
    }

    /**
     * Check if our internal {@link #mState} is valid, usually checked before
     * performing user actions.
     */
    protected boolean hasValidState() {
        return mState != null && mState.size() > 0;
    }

    /**
     * Rebuild the editors to match our underlying {@link #mState} object, usually
     * called once we've parsed {@link Entity} data or have inserted a new
     * {@link RawContacts}.
     */
    protected void bindEditors() {
        if (!hasValidState()) return;

        final LayoutInflater inflater = (LayoutInflater) getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        final Sources sources = Sources.getInstance(this);

        // Sort the editors
        Collections.sort(mState, this);

        // Remove any existing editors and rebuild any visible
        mContent.removeAllViews();
        int size = mState.size();
        for (int i = 0; i < size; i++) {
            // TODO ensure proper ordering of entities in the list
            EntityDelta entity = mState.get(i);
            final ValuesDelta values = entity.getValues();
            if (!values.isVisible()) continue;

            final String accountType = values.getAsString(RawContacts.ACCOUNT_TYPE);
            final ContactsSource source = sources.getInflatedSource(accountType,
                    ContactsSource.LEVEL_CONSTRAINTS);
            final long rawContactId = values.getAsLong(RawContacts._ID);

            BaseContactEditorView editor;
            if (!source.readOnly) {
                editor = (BaseContactEditorView) inflater.inflate(R.layout.item_contact_editor,
                        mContent, false);
            } else {
                editor = (BaseContactEditorView) inflater.inflate(
                        R.layout.item_read_only_contact_editor, mContent, false);
            }
            PhotoEditorView photoEditor = editor.getPhotoEditor();
            photoEditor.setEditorListener(new PhotoListener(rawContactId, source.readOnly,
                    photoEditor));

            mContent.addView(editor);
            editor.setState(entity, source);
        }

        // Show editor now that we've loaded state
        mContent.setVisibility(View.VISIBLE);
    }

    /**
     * Class that listens to requests coming from photo editors
     */
    private class PhotoListener implements EditorListener, DialogInterface.OnClickListener {
        private long mRawContactId;
        private boolean mReadOnly;
        private PhotoEditorView mEditor;

        public PhotoListener(long rawContactId, boolean readOnly, PhotoEditorView editor) {
            mRawContactId = rawContactId;
            mReadOnly = readOnly;
            mEditor = editor;
        }

        public void onDeleted(Editor editor) {
            // Do nothing
        }

        public void onRequest(int request) {
            if (!hasValidState()) return;

            if (request == EditorListener.REQUEST_PICK_PHOTO) {
                if (mEditor.hasSetPhoto()) {
                    // There is an existing photo, offer to remove, replace, or promoto to primary
                    createPhotoDialog().show();
                } else if (!mReadOnly) {
                    // No photo set and not read-only, try to set the photo
                    doPickPhotoAction(mRawContactId);
                }
            }
        }

        /**
         * Prepare dialog for picking a new {@link EditType} or entering a
         * custom label. This dialog is limited to the valid types as determined
         * by {@link EntityModifier}.
         */
        public Dialog createPhotoDialog() {
            Context context = EditContactActivity.this;

            // Wrap our context to inflate list items using correct theme
            final Context dialogContext = new ContextThemeWrapper(context,
                    android.R.style.Theme_Light);

            String[] choices;
            if (mReadOnly) {
                choices = new String[1];
                choices[0] = getString(R.string.use_photo_as_primary);
            } else {
                choices = new String[3];
                choices[0] = getString(R.string.use_photo_as_primary);
                choices[1] = getString(R.string.removePicture);
                choices[2] = getString(R.string.changePicture);
            }
            final ListAdapter adapter = new ArrayAdapter<String>(dialogContext,
                    android.R.layout.simple_list_item_1, choices);

            final AlertDialog.Builder builder = new AlertDialog.Builder(dialogContext);
            builder.setTitle(R.string.attachToContact);
            builder.setSingleChoiceItems(adapter, -1, this);
            return builder.create();
        }

        /**
         * Called when something in the dialog is clicked
         */
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();

            switch (which) {
                case 0:
                    // Set the photo as super primary
                    mEditor.setSuperPrimary(true);

                    // And set all other photos as not super primary
                    int count = mContent.getChildCount();
                    for (int i = 0; i < count; i++) {
                        View childView = mContent.getChildAt(i);
                        if (childView instanceof BaseContactEditorView) {
                            BaseContactEditorView editor = (BaseContactEditorView) childView;
                            PhotoEditorView photoEditor = editor.getPhotoEditor();
                            if (!photoEditor.equals(mEditor)) {
                                photoEditor.setSuperPrimary(false);
                            }
                        }
                    }
                    break;

                case 1:
                    // Remove the photo
                    mEditor.setPhotoBitmap(null);
                    break;

                case 2:
                    // Pick a new photo for the contact
                    doPickPhotoAction(mRawContactId);
                    break;
            }
        }
    }

    /** {@inheritDoc} */
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_done:
                doSaveAction(SAVE_MODE_DEFAULT);
                break;
            case R.id.btn_discard:
                doRevertAction();
                break;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onBackPressed() {
        doSaveAction(SAVE_MODE_DEFAULT);
    }

    /** {@inheritDoc} */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Ignore failed requests
        if (resultCode != RESULT_OK) return;

        switch (requestCode) {
            case PHOTO_PICKED_WITH_DATA: {
                BaseContactEditorView requestingEditor = null;
                for (int i = 0; i < mContent.getChildCount(); i++) {
                    View childView = mContent.getChildAt(i);
                    if (childView instanceof BaseContactEditorView) {
                        BaseContactEditorView editor = (BaseContactEditorView) childView;
                        if (editor.getRawContactId() == mRawContactIdRequestingPhoto) {
                            requestingEditor = editor;
                            break;
                        }
                    }
                }

                if (requestingEditor != null) {
                    final Bitmap photo = data.getParcelableExtra("data");
                    requestingEditor.setPhotoBitmap(photo);
                    mRawContactIdRequestingPhoto = -1;
                } else {
                    // The contact that requested the photo is no longer present.
                    // TODO: Show error message
                }

                break;
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit, menu);


        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_done:
                return doSaveAction(SAVE_MODE_DEFAULT);
            case R.id.menu_discard:
                return doRevertAction();
            case R.id.menu_add:
                return doAddAction();
            case R.id.menu_delete:
                return doDeleteAction();
        }
        return false;
    }

    /**
     * Background task for persisting edited contact data, using the changes
     * defined by a set of {@link EntityDelta}. This task starts
     * {@link EmptyService} to make sure the background thread can finish
     * persisting in cases where the system wants to reclaim our process.
     */
    public static class PersistTask extends
            WeakAsyncTask<EntitySet, Void, Integer, EditContactActivity> {
        private static final int PERSIST_TRIES = 3;

        private static final int RESULT_UNCHANGED = 0;
        private static final int RESULT_SUCCESS = 1;
        private static final int RESULT_FAILURE = 2;

        private int mSaveMode;
        private Uri mContactLookupUri = null;

        public PersistTask(EditContactActivity target, int saveMode) {
            super(target);
            mSaveMode = saveMode;
        }

        /** {@inheritDoc} */
        @Override
        protected void onPreExecute(EditContactActivity target) {
            // Before starting this task, start an empty service to protect our
            // process from being reclaimed by the system.
            final Context context = target;
            context.startService(new Intent(context, EmptyService.class));
        }

        /** {@inheritDoc} */
        @Override
        protected Integer doInBackground(EditContactActivity target, EntitySet... params) {
            final Context context = target;
            final ContentResolver resolver = context.getContentResolver();

            EntitySet state = params[0];

            // Trim any empty fields, and RawContacts, before persisting
            final Sources sources = Sources.getInstance(context);
            EntityModifier.trimEmpty(state, sources);

            // Attempt to persist changes
            int tries = 0;
            Integer result = RESULT_FAILURE;
            while (tries++ < PERSIST_TRIES) {
                try {
                    // Build operations and try applying
                    final ArrayList<ContentProviderOperation> diff = state.buildDiff();
                    ContentProviderResult[] results = null;
                    if (!diff.isEmpty()) {
                         results = resolver.applyBatch(ContactsContract.AUTHORITY, diff);
                         for (int i=0;i<results.length;i++){
                         }
                    }

                    final long rawContactId = getRawContactId(state, diff, results);
                    
                    if (rawContactId != -1) {
                        final Uri rawContactUri = ContentUris.withAppendedId(
                                RawContacts.CONTENT_URI, rawContactId);

                        // convert the raw contact URI to a contact URI
                        mContactLookupUri = RawContacts.getContactLookupUri(resolver,
                                rawContactUri);
                    }
                    result = (diff.size() > 0) ? RESULT_SUCCESS : RESULT_UNCHANGED;
                    break;

                } catch (RemoteException e) {
                    // Something went wrong, bail without success
                   	Log.e("EditContactActivity", "Problem persisting user edits", e);
                    break;

                } catch (OperationApplicationException e) {
                    // Version consistency failed, re-parent change and try again
               		Log.e("EditContactActivity", "Version consistency failed, re-parenting: " + e.toString());
                    final EntitySet newState = EntitySet.fromQuery(resolver,
                            target.mQuerySelection, null, null);
                    state = EntitySet.mergeAfter(newState, state);
                }
            }

            return result;
        }

        private long getRawContactId(EntitySet state,
                final ArrayList<ContentProviderOperation> diff,
                final ContentProviderResult[] results) {
            long rawContactId = state.findRawContactId();
            if (rawContactId != -1) {
                return rawContactId;
            }

            // we gotta do some searching for the id
            final int diffSize = diff.size();
            for (int i = 0; i < diffSize; i++) {
                ContentProviderOperation operation = diff.get(i);
                if (/*operation.getType() == ContentProviderOperation.TYPE_INSERT
                        &&*/ operation.getUri().getEncodedPath().contains(
                                RawContacts.CONTENT_URI.getEncodedPath())) {
                	return ContentUris.parseId(results[i].uri);
                }
            }
            return -1;
        }

        /** {@inheritDoc} */
        @Override
        protected void onPostExecute(EditContactActivity target, Integer result) {
            final Context context = target;

            if (result == RESULT_SUCCESS ) {
                Toast.makeText(context, R.string.contactSavedToast, Toast.LENGTH_SHORT).show();
            } else if (result == RESULT_FAILURE) {
                Toast.makeText(context, R.string.contactSavedErrorToast, Toast.LENGTH_LONG).show();
            }

            // Stop the service that was protecting us
            context.stopService(new Intent(context, EmptyService.class));

            target.onSaveCompleted(result != RESULT_FAILURE, mSaveMode, mContactLookupUri);
        }
    }

    /**
     * Saves or creates the contact based on the mode, and if successful
     * finishes the activity.
     */
    boolean doSaveAction(int saveMode) {
        if (!hasValidState()) return false;
        
        saveAndRevokeIfNeeded(saveMode);

        return true;
    }


    /**
     * Save the contact and revoke the associated numbers if needed
     * 
     * @param saveMode
     */
    private void saveAndRevokeIfNeeded(int saveMode){
    	// Check the is a modified RCS numbers
    	ArrayList<String> modifiedRcsNumbers = new ArrayList<String>();
    	int size = mState.size();
    	for (int i = 0; i < size; i++) {
    		EntityDelta entity = mState.get(i);
    		final ValuesDelta values = entity.getValues();
    		if (!values.isVisible()) continue;
    		// We just verify that a RCS phone number has not changed, so ignore the other data types
    		ArrayList<ValuesDelta> list = entity.getMimeEntries(CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
    		if (list==null){
    			// Given entity is not a phone number, no need to go further
    			continue;
    		}    		
    		for (int j=0;j<list.size();j++){
    			ValuesDelta valuesDelta = list.get(j);
    			
        		// Get the original value for the phone number
        		ContentValues beforeValues = valuesDelta.getBefore(); 
        		if (beforeValues!=null){
        			// If there was an original number (check necessary, it could be a number insertion, in this case there is nothing to do) 
	        		String originalNumber = beforeValues.getAsString(Phone.NUMBER);
	        		String originalInternationalNumber = PhoneUtils.formatNumberToInternational(originalNumber);
	            	ContactsApi contactsApi = new ContactsApi(this);
	        		if (contactsApi.isNumberShared(originalInternationalNumber) 
	        				|| (contactsApi.isNumberInvited(originalInternationalNumber))){
		        		// We have a RCS phone number
        				// Check if it has been changed
        				ContentValues afterValues = valuesDelta.getAfter();
        				if (afterValues!=null){
	        				String afterNumber = afterValues.getAsString(Phone.NUMBER);
	        				String afterInternationalNumber = PhoneUtils.formatNumberToInternational(afterNumber);
	        				if (afterInternationalNumber!=null && !afterInternationalNumber.equalsIgnoreCase(originalInternationalNumber)){
	        					// a rcs number has been changed
	        					modifiedRcsNumbers.add(originalInternationalNumber);
	        				}
        				}else{
        					// a rcs number has been deleted
        					modifiedRcsNumbers.add(originalInternationalNumber);
        				}
	        		}
        		}
        	}
    	}

    	if (modifiedRcsNumbers.size()>0){
    		// If we have modified a RCS number, we have to warn the user
    		showChangingRcsNumberDialog(modifiedRcsNumbers, saveMode);
    	}else{
    		// No RCS number in play, do the normal saving
            final PersistTask task = new PersistTask(this, saveMode);
            task.execute(mState);
    	}
    }
    
    /**
     * Show changing RCS number dialog
     * 
     * <br>We ask for confirmation that the user wants to change the RCS number, which will result in revoking contact
     * 
     * @param modifiedRcsNumbers List of modified RCS numbers
     * @param saveMode
     */
    protected void showChangingRcsNumberDialog(final ArrayList<String> modifiedRcsNumbers, final int saveMode){
    	handler.post(new Runnable(){
    		public void run(){
    	        new AlertDialog.Builder(EditContactActivity.this)
                .setTitle(R.string.rcs_eab_modifyRcsNumber)
                .setIcon(R.drawable.rcs_common_flower_21x21)
                .setMessage(R.string.rcs_eab_modifyRcsNumberRevokeConfirmation)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    	// Confirmation : really save it, but first we have to do a revoke
                		// Do the revoke on the modified numbers
                		revokeContactsAndSave(modifiedRcsNumbers);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    	// Do not save it, so do nothing
                    }
                })
                .show();
    		}
    	});
    }
    
    /**
     * Show delete RCS contact dialog
     * 
     * <br>Confirmation that the user wants to delete the contact (which has RCS numbers), which will result in revoking it
     * 
     * @param rcsNumbers List of associated RCS numbers for the contact
     * @param saveMode
     */
    protected void showDeleteRcsContactDialog(final ArrayList<PhoneNumber> rcsNumbers, final int saveMode){
    	handler.post(new Runnable(){
    		public void run(){
    	        new AlertDialog.Builder(EditContactActivity.this)
    	        .setTitle(R.string.rcs_eab_deleteRcsContact)
                .setIcon(R.drawable.rcs_common_flower_21x21)
                .setMessage(R.string.rcs_eab_deleteRcsContactRevokeConfirmation)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    	// Confirmation : really save it, but first we have to do a revoke
                		// Do the revoke on the modified numbers
                		revokeContactsAndDelete(rcsNumbers);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    	// Do not delete it, so do nothing
                    }
                })
                .show();
    		}
    	});
    }
    
    private class DeleteClickListener implements DialogInterface.OnClickListener {

        public void onClick(DialogInterface dialog, int which) {
            // Mark all raw contacts for deletion
            for (EntityDelta delta : mState) {
                delta.markDeleted();
            }
            // Save the deletes
            doSaveAction(SAVE_MODE_DEFAULT);
            finish();
        }
    }
    
    // RCS needed attributes
	private PresenceApi presenceApi = null;
	private final Handler handler = new Handler();
	private static final int ID_REVOKE_PROGRESS = 12345;
	
	
	/**
	 * Revoke the given numbers and save the changes
	 * 
	 * @param contacts
	 */
    private void revokeContactsAndSave(final ArrayList<String> contacts){
		showDialog(ID_REVOKE_PROGRESS);
		Thread revoke = new Thread(new Runnable(){
			public void run(){
				
				// Revoke all numbers in the list
				for (int i=0;i<contacts.size();i++){
					String rcsContact = contacts.get(i);
					boolean result = false;
					try {
						result = presenceApi.revokeContact(rcsContact);
						if (result){
							DialogUtils.displayToast(EditContactActivity.this, handler, getString(R.string.rcs_eab_revokeContactSuccess));
						}
					} catch (ClientApiException e) {
						if (logger.isActivated()){
							logger.error("Could not revoke contact before deleting it from address book",e);
						}
					}            						
					if (!result){
						if (logger.isActivated()){
							logger.debug("Could not revoke contact before deleting it from address book");
						}						
						DialogUtils.displayToast(EditContactActivity.this, handler, getString(R.string.rcs_eab_revokeContactError));
					}

					// Revoke was ok, save the contact changes
					final PersistTask task = new PersistTask(EditContactActivity.this, SAVE_MODE_DEFAULT);
					task.execute(mState);

					handler.post(new Runnable(){
						public void run(){
							dismissRevokeDialog();
						}
					});
				}
			}
		});
		revoke.start();            		
    }
    
	/**
	 * Revoke the given numbers and delete the contact
	 * 
	 * @param contacts
	 */
    private void revokeContactsAndDelete(final ArrayList<PhoneNumber> contacts){
		showDialog(ID_REVOKE_PROGRESS);
		Thread revoke = new Thread(new Runnable(){
			public void run(){
				
				// Revoke all numbers in the list
				for (int i=0;i<contacts.size();i++){
					String rcsContact = contacts.get(i).number;
					boolean result = false;
					try {
						result = presenceApi.revokeContact(rcsContact);
						if (result){
							DialogUtils.displayToast(EditContactActivity.this, handler, getString(R.string.rcs_eab_revokeContactSuccess));
						}
					} catch (ClientApiException e) {
						if (logger.isActivated()){
							logger.error("Could not revoke contact before deleting it from address book",e);
						}
					}            						
					if (!result){
						if (logger.isActivated()){
							logger.debug("Could not revoke contact before deleting it from address book");
						}
						DialogUtils.displayToast(EditContactActivity.this, handler, getString(R.string.rcs_eab_revokeContactError));
					}
					
					// Revoke was ok, delete the contact
					// Mark all raw contacts for deletion
					for (EntityDelta delta : mState) {
						delta.markDeleted();
					}
					// Save the deletes
					doSaveAction(SAVE_MODE_DEFAULT);
					finish();

					handler.post(new Runnable(){
						public void run(){
							dismissRevokeDialog();
						}
					});
				}
			}
		});
		revoke.start();            		
    }
    
    public void dismissRevokeDialog(){
    	dismissDialog(ID_REVOKE_PROGRESS);
    } 

    private void onSaveCompleted(boolean success, int saveMode, Uri contactLookupUri) {
        switch (saveMode) {
            case SAVE_MODE_DEFAULT:
                if (success && contactLookupUri != null) {
                	final Intent resultIntent = new Intent();

                    final Uri requestData = getIntent().getData();
                    final String requestAuthority = requestData == null ? null : requestData
                            .getAuthority();

                    if (android.provider.Contacts.AUTHORITY.equals(requestAuthority)) {
                        // Build legacy Uri when requested by caller
                        final long contactId = ContentUris.parseId(Contacts.lookupContact(
                                getContentResolver(), contactLookupUri));
                        final Uri legacyUri = ContentUris.withAppendedId(
                                android.provider.Contacts.People.CONTENT_URI, contactId);
                        resultIntent.setData(legacyUri);
                    } else {
                        // Otherwise pass back a lookup-style Uri
                        resultIntent.setData(contactLookupUri);
                    }

                    setResult(RESULT_OK, resultIntent);
                } else {
                    setResult(RESULT_CANCELED, null);
                }
                finish();
                break;
        }
    }

    /**
     * Revert any changes the user has made, and finish the activity.
     */
    private boolean doRevertAction() {
        finish();
        return true;
    }

    /**
     * Create a new {@link RawContacts} which will exist as another
     * {@link EntityDelta} under the currently edited {@link Contacts}.
     */
    private boolean doAddAction() {
        // Adding is okay when missing state
        new AddContactTask(this).execute();
        return true;
    }

    /**
     * Delete the entire contact currently being edited, which usually asks for
     * user confirmation before continuing.
     */
    private boolean doDeleteAction() {
        if (!hasValidState()) return false;
        int readOnlySourcesCnt = 0;
	int writableSourcesCnt = 0;
        Sources sources = Sources.getInstance(EditContactActivity.this);
        for (EntityDelta delta : mState) {
	    final String accountType = delta.getValues().getAsString(RawContacts.ACCOUNT_TYPE);
            final ContactsSource contactsSource = sources.getInflatedSource(accountType,
                    ContactsSource.LEVEL_CONSTRAINTS);
            if (contactsSource != null && contactsSource.readOnly) {
                readOnlySourcesCnt += 1;
            } else {
                writableSourcesCnt += 1;
            }
	}

        if (readOnlySourcesCnt > 0 && writableSourcesCnt > 0) {
	    showDialog(DIALOG_CONFIRM_READONLY_DELETE);
	} else if (readOnlySourcesCnt > 0 && writableSourcesCnt == 0) {
	    showDialog(DIALOG_CONFIRM_READONLY_HIDE);
	} else if (readOnlySourcesCnt == 0 && writableSourcesCnt > 1) {
	    showDialog(DIALOG_CONFIRM_MULTIPLE_DELETE);
    } else {
    	long contactId = ContactUtils.queryForContactId(getContentResolver(), mState.findRawContactId());
    	// Get all rcs numbers for this contact
    	ArrayList<PhoneNumber> rcsNumbers = ContactUtils.getRcsActiveOrInvitedContactNumbers(EditContactActivity.this, contactId );
    	if (rcsNumbers.size()==0){
    		// No rcs numbers, normal deletion
    		showDialog(DIALOG_CONFIRM_DELETE);
    	}else{
    		// At least one rcs number, ask for confirmation
    		showDeleteRcsContactDialog(rcsNumbers, SAVE_MODE_DEFAULT);
    	}
	}
	return true;
    }

    /**
     * Pick a specific photo to be added under the currently selected tab.
     */
    boolean doPickPhotoAction(long rawContactId) {
        if (!hasValidState()) return false;

        try {
            // Launch picker to choose photo for selected contact
            final Intent intent = EabUtils.getPhotoPickIntent();
            startActivityForResult(intent, PHOTO_PICKED_WITH_DATA);
            mRawContactIdRequestingPhoto = rawContactId;
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.photoPickerNotFoundText, Toast.LENGTH_LONG).show();
        }
        return true;
    }

    /** {@inheritDoc} */
    public void onDeleted(Editor editor) {
        // Ignore any editor deletes
    }

    /**
     * Build dialog that handles adding a new {@link RawContacts} after the user
     * picks a specific {@link ContactsSource}.
     */
    private static class AddContactTask extends
            WeakAsyncTask<Void, Void, ArrayList<Account>, EditContactActivity> {

        public AddContactTask(EditContactActivity target) {
            super(target);
        }

        @Override
        protected ArrayList<Account> doInBackground(final EditContactActivity target,
                Void... params) {
            return Sources.getInstance(target).getAccounts(true);
        }

        @Override
        protected void onPostExecute(final EditContactActivity target, ArrayList<Account> accounts) {
            target.selectAccountAndCreateContact(accounts);
        }
    }

    public void selectAccountAndCreateContact(ArrayList<Account> accounts) {
        // No Accounts available.  Create a phone-local contact.
        if (accounts.isEmpty()) {
            createContact(null);
            return;  // Don't show a dialog.
        }

        // In the common case of a single account being writable, auto-select
        // it without showing a dialog.
//        if (accounts.size() == 1) {
//            createContact(accounts.get(0));
//            return;  // Don't show a dialog.
//        }

        
    	// Add a null account so we can always create a phone-local contact
        final String phoneLocalAccountName = "phoneLocalAccountName";
        final String phoneLocalAccountType = "phoneLocalAccountType";
        accounts.add(0, new Account(phoneLocalAccountName, phoneLocalAccountType));
        
        // Wrap our context to inflate list items using correct theme
        final Context dialogContext = new ContextThemeWrapper(this, android.R.style.Theme_Light);
        final LayoutInflater dialogInflater =
            (LayoutInflater)dialogContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        final Sources sources = Sources.getInstance(this);

        final ArrayAdapter<Account> accountAdapter = new ArrayAdapter<Account>(this,
                android.R.layout.simple_list_item_2, accounts) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                if (convertView == null) {
                    convertView = dialogInflater.inflate(android.R.layout.simple_list_item_2,
                            parent, false);
                }

                // TODO: show icon along with title
                final TextView text1 = (TextView)convertView.findViewById(android.R.id.text1);
                final TextView text2 = (TextView)convertView.findViewById(android.R.id.text2);

                final Account account = this.getItem(position);
                
                if (account.type.equalsIgnoreCase(phoneLocalAccountType)){
                	// on the phone-local account
	                text1.setText(getString(R.string.rcs_eab_phoneLocalAccountName));
	                text2.setText(getString(R.string.rcs_eab_phoneOnlyUnsynced));
                }else{
	                // Other account
	                final ContactsSource source = sources.getInflatedSource(account.type,
	                        ContactsSource.LEVEL_SUMMARY);
	
	                text1.setText(account.name);
	                text2.setText(source.getDisplayLabel(EditContactActivity.this));
                }
                
                return convertView;
            }
        };

        final DialogInterface.OnClickListener clickListener =
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                // Create new contact based on selected source
                final Account account = accountAdapter.getItem(which);
                
                if (account.type.equalsIgnoreCase(phoneLocalAccountType)){
                	// on the phone-local account
                	createContact(null);
                }else{
                	createContact(account);
                }
            }
        };

        final DialogInterface.OnCancelListener cancelListener =
                new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                // If nothing remains, close activity
                if (!hasValidState()) {
                    finish();
                }
            }
        };

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_new_contact_account);
        builder.setSingleChoiceItems(accountAdapter, 0, clickListener);
        builder.setOnCancelListener(cancelListener);
        showAndManageDialog(builder.create());
    }

    /**
     * @param account may be null to signal a device-local contact should
     *     be created.
     */
    private void createContact(Account account) {
        final Sources sources = Sources.getInstance(this);
        final ContentValues values = new ContentValues();
        if (account != null) {
            values.put(RawContacts.ACCOUNT_NAME, account.name);
            values.put(RawContacts.ACCOUNT_TYPE, account.type);
        } else {
            values.putNull(RawContacts.ACCOUNT_NAME);
            values.putNull(RawContacts.ACCOUNT_TYPE);
        }

        // Parse any values from incoming intent
        EntityDelta insert = new EntityDelta(ValuesDelta.fromAfter(values));
        final ContactsSource source = sources.getInflatedSource(
            account != null ? account.type : null,
            ContactsSource.LEVEL_CONSTRAINTS);
        final Bundle extras = getIntent().getExtras();
        EntityModifier.parseExtras(this, source, insert, extras);

        // Ensure we have some default fields
        EntityModifier.ensureKindExists(insert, source, Phone.CONTENT_ITEM_TYPE);
        EntityModifier.ensureKindExists(insert, source, Email.CONTENT_ITEM_TYPE);

        // Create "My Contacts" membership for Google contacts
        // TODO: move this off into "templates" for each given source
        if (GoogleSource.ACCOUNT_TYPE.equals(source.accountType)) {
            GoogleSource.attemptMyContactsMembership(insert, this);
        }

        if (mState == null) {
            // Create state if none exists yet
            mState = EntitySet.fromSingle(insert);
        } else {
            // Add contact onto end of existing state
            mState.add(insert);
        }

        bindEditors();
    }


    /**
     * Compare EntityDeltas for sorting the stack of editors.
     */
    public int compare(EntityDelta one, EntityDelta two) {
        // Check direct equality
        if (one.equals(two)) {
            return 0;
        }

        final Sources sources = Sources.getInstance(this);
        String accountType = one.getValues().getAsString(RawContacts.ACCOUNT_TYPE);
        final ContactsSource oneSource = sources.getInflatedSource(accountType,
                ContactsSource.LEVEL_SUMMARY);
        accountType = two.getValues().getAsString(RawContacts.ACCOUNT_TYPE);
        final ContactsSource twoSource = sources.getInflatedSource(accountType,
                ContactsSource.LEVEL_SUMMARY);

        // Check read-only
        if (oneSource.readOnly && !twoSource.readOnly) {
            return 1;
        } else if (twoSource.readOnly && !oneSource.readOnly) {
            return -1;
        }

        // Check account type
        boolean skipAccountTypeCheck = false;
        boolean oneIsGoogle = oneSource instanceof GoogleSource;
        boolean twoIsGoogle = twoSource instanceof GoogleSource;
        if (oneIsGoogle && !twoIsGoogle) {
            return -1;
        } else if (twoIsGoogle && !oneIsGoogle) {
            return 1;
        } else {
            skipAccountTypeCheck = true;
        }

        int value;
        if (!skipAccountTypeCheck) {
            value = oneSource.accountType.compareTo(twoSource.accountType);
            if (value != 0) {
                return value;
            }
        }

        // Check account name
        ValuesDelta oneValues = one.getValues();
        String oneAccount = oneValues.getAsString(RawContacts.ACCOUNT_NAME);
        if (oneAccount == null) oneAccount = "";
        ValuesDelta twoValues = two.getValues();
        String twoAccount = twoValues.getAsString(RawContacts.ACCOUNT_NAME);
        if (twoAccount == null) twoAccount = "";
        value = oneAccount.compareTo(twoAccount);
        if (value != 0) {
            return value;
        }

        // Both are in the same account, fall back to contact ID
        long oneId = oneValues.getAsLong(RawContacts._ID);
        long twoId = twoValues.getAsLong(RawContacts._ID);
        return (int)(oneId - twoId);
    }
}
