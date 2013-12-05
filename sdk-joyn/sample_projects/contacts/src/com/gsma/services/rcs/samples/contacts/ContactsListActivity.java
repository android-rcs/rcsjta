package com.gsma.services.rcs.samples.contacts;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.Vector;

import android.app.Activity;
import android.app.ListActivity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.Contacts.Intents.UI;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.CursorAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.capability.Capabilities;
import com.gsma.services.rcs.capability.CapabilitiesListener;
import com.gsma.services.rcs.capability.CapabilityService;
import com.gsma.services.rcs.contacts.ContactsService;
import com.gsma.services.rcs.contacts.JoynContact;

public class ContactsListActivity extends ListActivity implements
		TextView.OnEditorActionListener, OnKeyListener, OnFocusChangeListener,
		OnTouchListener, TextWatcher, OnClickListener {

	private static final String TAG = "Contacts";

	private static final int QUERY_TOKEN = 42;

	private static final int TEXT_HIGHLIGHTING_ANIMATION_DURATION = 350;

	static final String[] CONTACTS_SUMMARY_PROJECTION = new String[] {
			Contacts._ID, // 0
			Contacts.DISPLAY_NAME_PRIMARY, // 1
			Contacts.DISPLAY_NAME_ALTERNATIVE, // 2
			Contacts.SORT_KEY_PRIMARY, // 3
			Contacts.STARRED, // 4
			Contacts.TIMES_CONTACTED, // 5
			Contacts.CONTACT_PRESENCE, // 6
			Contacts.PHOTO_ID, // 7
			Contacts.LOOKUP_KEY, // 8
			Contacts.PHONETIC_NAME, // 9
			Contacts.HAS_PHONE_NUMBER, // 10
	};

	static final int SUMMARY_ID_COLUMN_INDEX = 0;
	static final int SUMMARY_DISPLAY_NAME_PRIMARY_COLUMN_INDEX = 1;
	static final int SUMMARY_DISPLAY_NAME_ALTERNATIVE_COLUMN_INDEX = 2;
	static final int SUMMARY_SORT_KEY_PRIMARY_COLUMN_INDEX = 3;
	static final int SUMMARY_STARRED_COLUMN_INDEX = 4;
	static final int SUMMARY_TIMES_CONTACTED_COLUMN_INDEX = 5;
	static final int SUMMARY_PRESENCE_STATUS_COLUMN_INDEX = 6;
	static final int SUMMARY_PHOTO_ID_COLUMN_INDEX = 7;
	static final int SUMMARY_LOOKUP_KEY_COLUMN_INDEX = 8;
	static final int SUMMARY_PHONETIC_NAME_COLUMN_INDEX = 9;
	static final int SUMMARY_HAS_PHONE_COLUMN_INDEX = 10;

	private static final Uri CONTACTS_CONTENT_URI_WITH_LETTER_COUNTS = buildSectionIndexerUri(Contacts.CONTENT_URI);

	private static final String CLAUSE_ONLY_VISIBLE = Contacts.IN_VISIBLE_GROUP
			+ "=1";
	private static final String CLAUSE_ONLY_PHONES = Contacts.HAS_PHONE_NUMBER
			+ "=1";

	/**
	 * An approximation of the background color of the pinned header. This color
	 * is used when the pinned header is being pushed up. At that point the
	 * header "fades away". Rather than computing a faded bitmap based on the
	 * 9-patch normally used for the background, we will use a solid color,
	 * which will provide better performance and reduced complexity.
	 */
	private int mPinnedHeaderBackgroundColor;

	private boolean mSearchResultsMode;

	private boolean mShowNumberOfContacts;
	private boolean mSearchMode;

	private ContactItemListAdapter mAdapter;
	private QueryHandler mQueryHandler;

	private String mInitialFilter;

	private int mSortOrder;
	private int mDisplayOrder;

	private boolean mHighlightWhenScrolling;
	private TextHighlightingAnimation mHighlightingAnimation;
	private SearchEditText mSearchEditText;

	private boolean mDisplayOnlyPhones = true;

	/**
	 * Used to keep track of the scroll state of the list.
	 */
	private Parcelable mListState = null;

	private boolean mMode;

	private ContactPhotoLoader mPhotoLoader;

	private int mIconSize;

	private boolean mJustCreated;

	private ContactsService contactsApi;

	private CapabilityService capabilityApi;

	/**
	 * Capabilities listener
	 */
	private MyCapabilitiesListener capabilitiesListener = new MyCapabilitiesListener();

	/**
	 * UI handler
	 */
	private final Handler handler = new Handler();

	private Vector<String> items = new Vector<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		mIconSize = getResources().getDimensionPixelSize(
				android.R.dimen.app_icon_size);
		mPhotoLoader = new ContactPhotoLoader(this,
				R.drawable.ic_contact_list_picture);

		// Resolve the intent
		final Intent intent = getIntent();

		// Allow the title to be set to a custom String using an extra on the
		// intent
		String title = intent.getStringExtra(UI.TITLE_EXTRA_KEY);
		if (title != null) {
			setTitle(title);
		}

		String action = intent.getAction();
		String component = intent.getComponent().getClassName();

		setContentView(R.layout.contacts_list_content);

		mSearchMode = false;

		setupListView();
		setupSearchView();
		// Init APIs
		contactsApi = new ContactsService(getApplicationContext(),
				new MyContactsApiListener());
		contactsApi.connect();
		capabilityApi = new CapabilityService(getApplicationContext(),
				new MyCapabilityApiListener());
		capabilityApi.connect();

		mQueryHandler = new QueryHandler(this);

		mJustCreated = true;
	}

	@Override
	protected void onRestart() {
		super.onRestart();

		// The cursor was killed off in onStop(), so we need to get a new one
		// here
		// We do not perform the query if a filter is set on the list because
		// the
		// filter will cause the query to happen anyway
		if (TextUtils.isEmpty(getTextFilter())) {
			startQuery();
		} else {
			// Run the filtered query on the adapter
			mAdapter.onContentChanged();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		mPhotoLoader.resume();

		Activity parent = getParent();

		// See if we were invoked with a filter
		if (mSearchMode) {
			mSearchEditText.requestFocus();
		}

		if (mJustCreated) {
			// We need to start a query here the first time the activity is
			// launched, as long
			// as we aren't doing a filter.
			startQuery();
		}
		mJustCreated = false;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// Disconnect APIs
		contactsApi.disconnect();
		capabilityApi.disconnect();
	}

	private void setupListView() {
		final ListView list = getListView();
		final LayoutInflater inflater = getLayoutInflater();

		mHighlightingAnimation = new NameHighlightingAnimation(list,
				TEXT_HIGHLIGHTING_ANIMATION_DURATION);

		// Tell list view to not show dividers. We'll do it ourself so that we
		// can *not* show
		// them when an A-Z headers is visible.
		list.setDividerHeight(0);
		list.setOnCreateContextMenuListener(this);

		mAdapter = new ContactItemListAdapter(this);
		mAdapter.setmContext(getApplicationContext());
		setListAdapter(mAdapter);

		if (list instanceof PinnedHeaderListView
				&& mAdapter.getDisplaySectionHeadersEnabled()) {
			mPinnedHeaderBackgroundColor = getResources().getColor(
					R.color.pinned_header_background);
			PinnedHeaderListView pinnedHeaderList = (PinnedHeaderListView) list;
			View pinnedHeader = inflater.inflate(R.layout.list_section, list,
					false);
			pinnedHeaderList.setPinnedHeaderView(pinnedHeader);
		}

		list.setOnScrollListener(mAdapter);
		list.setOnKeyListener(this);
		list.setOnFocusChangeListener(this);
		list.setOnTouchListener(this);

		// We manually save/restore the listview state
		list.setSaveEnabled(false);
	}

	/**
	 * Configures search UI.
	 */
	private void setupSearchView() {
		mSearchEditText = (SearchEditText) findViewById(R.id.search_src_text);
		mSearchEditText.addTextChangedListener(this);
		mSearchEditText.setOnEditorActionListener(this);
		mSearchEditText.setText(mInitialFilter);
	}

	void startQuery() {
		// Set the proper empty string
		setEmptyText();

		if (mSearchResultsMode) {
			TextView foundContactsText = (TextView) findViewById(R.id.search_results_found);
			foundContactsText.setText(R.string.search_results_searching);
		}

		mAdapter.setLoading(true);

		// Cancel any pending queries
		mQueryHandler.cancelOperation(QUERY_TOKEN);
		mQueryHandler.setLoadingJoinSuggestions(false);

		mSortOrder = 0;
		mDisplayOrder = 0;

		// When sort order and display order contradict each other, we want to
		// highlight the part of the name used for sorting.
		mHighlightWhenScrolling = false;

		String[] projection = CONTACTS_SUMMARY_PROJECTION;
		if (!mJustCreated && mSearchMode && TextUtils.isEmpty(getTextFilter())) {
			mAdapter.changeCursor(new MatrixCursor(projection));
			return;
		}

		String callingPackage = getCallingPackage();
		Uri uri = CONTACTS_CONTENT_URI_WITH_LETTER_COUNTS;

		mQueryHandler.startQuery(QUERY_TOKEN, null, uri, projection,
				getContactSelection(), null, CONTACTS_SUMMARY_PROJECTION[1]);

	}

	private static class QueryHandler extends AsyncQueryHandler {
		protected final WeakReference<ContactsListActivity> mActivity;
		protected boolean mLoadingJoinSuggestions = false;

		public QueryHandler(Context context) {
			super(context.getContentResolver());
			mActivity = new WeakReference<ContactsListActivity>(
					(ContactsListActivity) context);
		}

		public void setLoadingJoinSuggestions(boolean flag) {
			mLoadingJoinSuggestions = flag;
		}

		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			final ContactsListActivity activity = mActivity.get();
			if (activity != null && !activity.isFinishing()) {

				activity.mAdapter.changeCursor(cursor);

				// Now that the cursor is populated again, it's possible to
				// restore the list state
				if (activity.mListState != null) {
					// activity.onRestoreInstanceState((Bundle)
					// activity.mListState);
					activity.mListState = null;
				}
			} else {
				if (cursor != null) {
					cursor.close();
				}
			}
		}
	}

	/**
	 * Called from a background thread to do the filter and return the resulting
	 * cursor.
	 * 
	 * @param filter
	 *            the text that was entered to filter on
	 * @return a cursor with the results of the filter
	 */
	Cursor doFilter(String filter) {
		String[] projection = CONTACTS_SUMMARY_PROJECTION;
		if (mSearchMode && TextUtils.isEmpty(getTextFilter())) {
			return new MatrixCursor(projection);
		}
		final ContentResolver resolver = getContentResolver();

		return resolver.query(getContactFilterUri(filter), projection,
				getContactSelection(), null, CONTACTS_SUMMARY_PROJECTION[1]);

	}

	/**
	 * Performs filtering of the list based on the search query entered in the
	 * search text edit.
	 */
	protected void onSearchTextChanged() {
		// Set the proper empty string
		setEmptyText();

		Filter filter = mAdapter.getFilter();
		filter.filter(getTextFilter());
	}

	/**
	 * Return the selection arguments for a default query based on the
	 * {@link #mDisplayOnlyPhones} flag.
	 */
	private String getContactSelection() {
		if (mDisplayOnlyPhones) {
			return CLAUSE_ONLY_VISIBLE + " AND " + CLAUSE_ONLY_PHONES;
		} else {
			return CLAUSE_ONLY_VISIBLE;
		}
	}

	private Uri getContactFilterUri(String filter) {
		Uri baseUri;
		if (!TextUtils.isEmpty(filter)) {
			baseUri = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI,
					Uri.encode(filter));
		} else {
			baseUri = Contacts.CONTENT_URI;
		}

		if (mAdapter.getDisplaySectionHeadersEnabled()) {
			return buildSectionIndexerUri(baseUri);
		} else {
			return baseUri;
		}
	}

	private String getTextFilter() {
		if (mSearchEditText != null) {
			return mSearchEditText.getText().toString();
		}
		return null;
	}

	private void setEmptyText() {
		if (mSearchMode) {
			return;
		}

		TextView empty = (TextView) findViewById(R.id.emptyText);

		empty.setText(getText(R.string.noMatchingContacts));

	}

	/**
	 * A {@link TextHighlightingAnimation} that redraws just the contact display
	 * name in a list item.
	 */
	private static class NameHighlightingAnimation extends
			TextHighlightingAnimation {
		private final ListView mListView;

		private NameHighlightingAnimation(ListView listView, int duration) {
			super(duration);
			this.mListView = listView;
		}

		/**
		 * Redraws all visible items of the list corresponding to contacts
		 */
		@Override
		protected void invalidate() {
			int childCount = mListView.getChildCount();
			for (int i = 0; i < childCount; i++) {
				View itemView = mListView.getChildAt(i);
				if (itemView instanceof ContactListItemView) {
					final ContactListItemView view = (ContactListItemView) itemView;
					view.getNameTextView().invalidate();
				}
			}
		}

		@Override
		protected void onAnimationStarted() {
			mListView.setScrollingCacheEnabled(false);
		}

		@Override
		protected void onAnimationEnded() {
			mListView.setScrollingCacheEnabled(true);
		}
	}

	private void hideSoftKeyboard() {
		// Hide soft keyboard, if visible
		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(getListView()
				.getWindowToken(), 0);
	}

	/**
	 * Dismisses the search UI along with the keyboard if the filter text is
	 * empty.
	 */
	public boolean onKeyPreIme(int keyCode, KeyEvent event) {
		if (mSearchMode && keyCode == KeyEvent.KEYCODE_BACK
				&& TextUtils.isEmpty(getTextFilter())) {
			hideSoftKeyboard();
			onBackPressed();
			return true;
		}
		return false;
	}

	@Override
	public void afterTextChanged(Editable arg0) {
		onSearchTextChanged();

	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
			int arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onTouch(View arg0, MotionEvent arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onFocusChange(View arg0, boolean arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onKey(View arg0, int arg1, KeyEvent arg2) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	/** {@inheritDoc} */
	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
		// TODO a better way of identifying the button
		case android.R.id.button1: {
			final int position = (Integer) v.getTag();
			Cursor c = mAdapter.getCursor();
			if (c != null) {
				c.moveToPosition(position);
				callContact(c);
			}
			break;
		}
		}
	}

	boolean callContact(Cursor cursor) {
		return callOrSmsContact(cursor, false /* call */);
	}

	/**
	 * Calls the contact which the cursor is point to.
	 * 
	 * @return true if the call was initiated, false otherwise
	 */
	boolean callOrSmsContact(Cursor cursor, boolean sendSms) {
		if (cursor == null) {
			return false;
		}

		boolean hasPhone = cursor.getInt(SUMMARY_HAS_PHONE_COLUMN_INDEX) != 0;
		if (!hasPhone) {
			// There is no phone number.
			signalError();
			return false;
		}

		String phone = null;
		Cursor phonesCursor = null;
		phonesCursor = queryPhoneNumbers(cursor
				.getLong(SUMMARY_ID_COLUMN_INDEX));
		if (phonesCursor == null || phonesCursor.getCount() == 0) {
			// No valid number
			signalError();
			return false;
		} else if (phonesCursor.getCount() == 1) {
			// only one number, call it.
			phone = phonesCursor.getString(phonesCursor
					.getColumnIndex(Phone.NUMBER));
		} else {
			phonesCursor.moveToPosition(-1);
			while (phonesCursor.moveToNext()) {
				if (phonesCursor.getInt(phonesCursor
						.getColumnIndex(Phone.IS_SUPER_PRIMARY)) != 0) {
					// Found super primary, call it.
					phone = phonesCursor.getString(phonesCursor
							.getColumnIndex(Phone.NUMBER));
					break;
				}
			}
		}

		if (phone == null) {
			// Display dialog to choose a number to call.
			PhoneDisambigDialog phoneDialog = new PhoneDisambigDialog(this,
					phonesCursor, sendSms, 0);
			phoneDialog.show();
		} else {
			if (sendSms) {
				ContactsUtils.initiateSms(this, phone);
			} else {
				ContactsUtils.initiateCall(this, phone);
			}
		}

		return true;
	}

	private Cursor queryPhoneNumbers(long contactId) {
		Uri baseUri = ContentUris.withAppendedId(Contacts.CONTENT_URI,
				contactId);
		Uri dataUri = Uri.withAppendedPath(baseUri,
				Contacts.Data.CONTENT_DIRECTORY);

		Cursor c = getContentResolver().query(
				dataUri,
				new String[] { Phone._ID, Phone.NUMBER, Phone.IS_SUPER_PRIMARY,
						RawContacts.ACCOUNT_TYPE, Phone.TYPE, Phone.LABEL },
				Data.MIMETYPE + "=?", new String[] { Phone.CONTENT_ITEM_TYPE },
				null);
		if (c != null) {
			if (c.moveToFirst()) {
				return c;
			}
			c.close();
		}
		return null;
	}

	private static Uri buildSectionIndexerUri(Uri uri) {
		return uri.buildUpon()
				.appendQueryParameter("address_book_index_extras", "true")
				.build();
	}

	final static class ContactListItemCache {
		public CharArrayBuffer nameBuffer = new CharArrayBuffer(128);
		public CharArrayBuffer dataBuffer = new CharArrayBuffer(128);
		public CharArrayBuffer highlightedTextBuffer = new CharArrayBuffer(128);
		public CharArrayBuffer phoneticNameBuffer = new CharArrayBuffer(128);
	}

	final static class PinnedHeaderCache {
		public TextView titleView;
		public ColorStateList textColor;
		public Drawable background;
	}

	private final class ContactItemListAdapter extends CursorAdapter implements
			SectionIndexer, OnScrollListener,
			PinnedHeaderListView.PinnedHeaderAdapter {
		private SectionIndexer mIndexer;
		private boolean mLoading = true;
		private CharSequence mUnknownNameText;
		private boolean mDisplayPhotos = false;
		private boolean mDisplayCallButton = false;
		private boolean mDisplayAdditionalData = true;
		private int mFrequentSeparatorPos = ListView.INVALID_POSITION;
		private boolean mDisplaySectionHeaders = true;
		private int mSuggestionsCursorCount;
		private Context mContext;
		

		public void setmContext(Context mContext) {
			this.mContext = mContext;
		}

		public ContactItemListAdapter(Context context) {
			super(context, null, false);

			mUnknownNameText = context.getText(android.R.string.unknownName);

			mDisplaySectionHeaders = true;

			// Do not display the second line of text if in a specific SEARCH
			// query mode, usually for
			// matching a specific E-mail or phone number. Any contact details
			// shown would be identical, and columns might not even be present
			// in the returned cursor.

			mDisplayAdditionalData = true;

			mDisplayCallButton = true;

			mDisplayPhotos = true;
		}

		public boolean getDisplaySectionHeadersEnabled() {
			return mDisplaySectionHeaders;
		}

		/**
		 * Callback on the UI thread when the content observer on the backing
		 * cursor fires. Instead of calling requery we need to do an async query
		 * so that the requery doesn't block the UI thread for a long time.
		 */
		@Override
		protected void onContentChanged() {
			CharSequence constraint = getTextFilter();
			if (!TextUtils.isEmpty(constraint)) {
				// Reset the filter state then start an async filter operation
				Filter filter = getFilter();
				filter.filter(constraint);
			} else {
				// Start an async query
				startQuery();
			}
		}

		public void setLoading(boolean loading) {
			mLoading = loading;
		}

		@Override
		public boolean isEmpty() {

			if (mSearchMode) {
				return TextUtils.isEmpty(getTextFilter());
			} else {
				return false;
			}
		}

		@Override
		public int getItemViewType(int position) {
			if (position == 0 && mShowNumberOfContacts) {
				return IGNORE_ITEM_VIEW_TYPE;
			}

			if (isShowAllContactsItemPosition(position)) {
				return IGNORE_ITEM_VIEW_TYPE;
			}

			if (isSearchAllContactsItemPosition(position)) {
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

			// handle the total contacts item
			if (position == 0 && mShowNumberOfContacts) {
				return getTotalContactCountView(parent);
			}

			if (position == 0) {
				// Add the header for creating a new contact
				return getLayoutInflater().inflate(R.layout.create_new_contact,
						parent, false);
			}

			if (isShowAllContactsItemPosition(position)) {
				return getLayoutInflater().inflate(
						R.layout.contacts_list_show_all_item, parent, false);
			}

			if (isSearchAllContactsItemPosition(position)) {
				return getLayoutInflater().inflate(
						R.layout.contacts_list_search_all_item, parent, false);
			}

			// Handle the separator specially
			int separatorId = getSeparatorId(position);
			if (separatorId != 0) {
				TextView view = (TextView) getLayoutInflater().inflate(
						R.layout.list_separator, parent, false);
				view.setText(separatorId);
				return view;
			}

			boolean showingSuggestion;
			Cursor cursor;

			showingSuggestion = false;
			cursor = getCursor();

			int realPosition = getRealPosition(position);
			if (!cursor.moveToPosition(realPosition)) {
				throw new IllegalStateException(
						"couldn't move cursor to position " + position);
			}

			boolean newView;
			View v;
			if (convertView == null || convertView.getTag() == null) {
				newView = true;
				v = newView(mContext, cursor, parent);
			} else {
				newView = false;
				v = convertView;
			}
			bindView(v, mContext, cursor);
			bindSectionHeader(v, realPosition, mDisplaySectionHeaders
					&& !showingSuggestion);
			return v;
		}

		private View getTotalContactCountView(ViewGroup parent) {
			final LayoutInflater inflater = getLayoutInflater();
			View view = inflater
					.inflate(R.layout.total_contacts, parent, false);

			TextView totalContacts = (TextView) view
					.findViewById(R.id.totalContactsText);

			String text;
			int count = getRealCount();

			if (mSearchMode && !TextUtils.isEmpty(getTextFilter())) {
				text = getQuantityText(count,
						R.string.listFoundAllContactsZero,
						R.plurals.searchFoundContacts);
			} else {
				if (mDisplayOnlyPhones) {
					text = getQuantityText(count,
							R.string.listTotalPhoneContactsZero,
							R.plurals.listTotalPhoneContacts);
				} else {
					text = getQuantityText(count,
							R.string.listTotalAllContactsZero,
							R.plurals.listTotalAllContacts);
				}
			}
			totalContacts.setText(text);
			return view;
		}

		private boolean isShowAllContactsItemPosition(int position) {
			return false;
		}

		private boolean isSearchAllContactsItemPosition(int position) {
			return mSearchMode && position == getCount() - 1;
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
			final ContactListItemView view = new ContactListItemView(context,
					null);
			view.setOnCallButtonClickListener(ContactsListActivity.this);
			view.setTag(new ContactListItemCache());
			return view;
		}

		@Override
		public void bindView(View itemView, Context context, Cursor cursor) {
			final ContactListItemView view = (ContactListItemView) itemView;
			final ContactListItemCache cache = (ContactListItemCache) view
					.getTag();

			int typeColumnIndex;
			int dataColumnIndex;
			int labelColumnIndex;
			int defaultType;
			int nameColumnIndex;
			int phoneticNameColumnIndex;
			boolean displayAdditionalData = mDisplayAdditionalData;
			boolean highlightingEnabled = false;

			nameColumnIndex = SUMMARY_DISPLAY_NAME_PRIMARY_COLUMN_INDEX;

			phoneticNameColumnIndex = SUMMARY_PHONETIC_NAME_COLUMN_INDEX;

			dataColumnIndex = -1;
			typeColumnIndex = -1;
			labelColumnIndex = -1;
			defaultType = Phone.TYPE_HOME;
			displayAdditionalData = false;
			highlightingEnabled = false;

			// Set the name
			cursor.copyStringToBuffer(nameColumnIndex, cache.nameBuffer);
			TextView nameView = view.getNameTextView();
			int size = cache.nameBuffer.sizeCopied;
			if (size != 0) {

				nameView.setText(cache.nameBuffer.data, 0, size);

			} else {
				nameView.setText(mUnknownNameText);
			}

			boolean hasPhone = cursor.getColumnCount() >= SUMMARY_HAS_PHONE_COLUMN_INDEX
					&& cursor.getInt(SUMMARY_HAS_PHONE_COLUMN_INDEX) != 0;

			// Make the call button visible if requested.
			if (mDisplayCallButton && hasPhone) {
				int pos = cursor.getPosition();
				view.showCallButton(android.R.id.button1, pos);
			} else {
				view.hideCallButton();
			}
			final long contactId = cursor
					.getLong(SUMMARY_ID_COLUMN_INDEX);
			// Set the photo, if requested
			if (mDisplayPhotos) {
				boolean useQuickContact = true;

				long photoId = 0;
				if (!cursor.isNull(SUMMARY_PHOTO_ID_COLUMN_INDEX)) {
					photoId = cursor.getLong(SUMMARY_PHOTO_ID_COLUMN_INDEX);
				}
				
				ImageView viewToUse;
				if (useQuickContact) {
					// Build soft lookup reference
					
					final String lookupKey = cursor
							.getString(SUMMARY_LOOKUP_KEY_COLUMN_INDEX);
					QuickContactBadge quickContact = view.getQuickContact();
					quickContact.assignContactUri(Contacts.getLookupUri(
							contactId, lookupKey));
					// quickContact.setSelectedContactsAppTabIndex(StickyTabs.getTab(getIntent()));
					viewToUse = quickContact;
				} else {
					viewToUse = view.getPhotoView();
				}

				final int position = cursor.getPosition();
				mPhotoLoader.loadPhoto(viewToUse, photoId);
			}
			
			//Check for joyn contacts

			view.setPresence(null);
			if(items != null){
				Cursor phonesCursor = queryPhoneNumbers(contactId);
				if (phonesCursor != null && phonesCursor.getCount() >= 1) {					
					phonesCursor.moveToPosition(-1);
					while (phonesCursor.moveToNext()) {
						
						String phone = phonesCursor.getString(phonesCursor
							.getColumnIndex(Phone.NUMBER));
						if(phone.startsWith("0")){
							phone = phone.replaceFirst("0", "+33");
						}
						
						if(items.contains(phone)){
							//found joyn contact

							view.setPresence(getResources().getDrawable(R.drawable.presence_icon));
							
							break;
						}
					}
					
				}
				phonesCursor.close();
				phonesCursor = null;
			}

			view.setSnippet(null);

			if (!displayAdditionalData) {
				if (phoneticNameColumnIndex != -1) {

					// Set the name
					cursor.copyStringToBuffer(phoneticNameColumnIndex,
							cache.phoneticNameBuffer);
					int phoneticNameSize = cache.phoneticNameBuffer.sizeCopied;
					if (phoneticNameSize != 0) {
						view.setLabel(cache.phoneticNameBuffer.data,
								phoneticNameSize);
					} else {
						view.setLabel(null);
					}
				} else {
					view.setLabel(null);
				}
				return;
			}

			// Set the data.
			cursor.copyStringToBuffer(dataColumnIndex, cache.dataBuffer);

			size = cache.dataBuffer.sizeCopied;
			view.setData(cache.dataBuffer.data, size);

			// Set the label.
			if (!cursor.isNull(typeColumnIndex)) {
				final int type = cursor.getInt(typeColumnIndex);
				final String label = cursor.getString(labelColumnIndex);

				// TODO cache
				view.setLabel(Phone.getTypeLabel(context.getResources(), type,
						label));

			} else {
				view.setLabel(null);
			}
		}

		private void bindSectionHeader(View itemView, int position,
				boolean displaySectionHeaders) {
			final ContactListItemView view = (ContactListItemView) itemView;
			final ContactListItemCache cache = (ContactListItemCache) view
					.getTag();
			if (!displaySectionHeaders) {
				view.setSectionHeader(null);
				view.setDividerVisible(true);
			} else {
				final int section = getSectionForPosition(position);
				if (getPositionForSection(section) == position) {
					String title = (String) mIndexer.getSections()[section];
					view.setSectionHeader(title);
				} else {
					view.setDividerVisible(false);
					view.setSectionHeader(null);
				}

				// move the divider for the last item in a section
				if (getPositionForSection(section + 1) - 1 == position) {
					view.setDividerVisible(false);
				} else {
					view.setDividerVisible(true);
				}
			}
		}

		@Override
		public void changeCursor(Cursor cursor) {
			if (cursor != null) {
				setLoading(false);
			}

			// Get the split between starred and frequent items, if the mode is
			// strequent
			mFrequentSeparatorPos = ListView.INVALID_POSITION;
			int cursorCount = 0;

			if (cursor != null && mSearchResultsMode) {
				TextView foundContactsText = (TextView) findViewById(R.id.search_results_found);
				String text = getQuantityText(cursor.getCount(),
						R.string.listFoundAllContactsZero,
						R.plurals.listFoundAllContacts);
				foundContactsText.setText(text);
			}

			super.changeCursor(cursor);
			// Update the indexer for the fast scroll widget
			updateIndexer(cursor);
		}

		private void updateIndexer(Cursor cursor) {
			if (cursor == null) {
				mIndexer = null;
				return;
			}

			Bundle bundle = cursor.getExtras();
			if (bundle.containsKey("address_book_index_titles")) {
				String sections[] = bundle
						.getStringArray("address_book_index_titles");
				int counts[] = bundle.getIntArray("address_book_index_counts");
				mIndexer = new ContactsSectionIndexer(sections, counts);
			} else {
				mIndexer = null;
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

		public Object[] getSections() {
			if (mIndexer == null) {
				return new String[] { " " };
			} else {
				return mIndexer.getSections();
			}
		}

		public int getPositionForSection(int sectionIndex) {
			if (mIndexer == null) {
				return -1;
			}

			return mIndexer.getPositionForSection(sectionIndex);
		}

		public int getSectionForPosition(int position) {
			if (mIndexer == null) {
				return -1;
			}

			return mIndexer.getSectionForPosition(position);
		}

		@Override
		public boolean areAllItemsEnabled() {
			return !mShowNumberOfContacts && mSuggestionsCursorCount == 0;
		}

		@Override
		public boolean isEnabled(int position) {
			if (mShowNumberOfContacts) {
				if (position == 0) {
					return false;
				}
				position--;
			}

			if (mSuggestionsCursorCount > 0) {
				return position != 0 && position != mSuggestionsCursorCount + 1;
			}
			return position != mFrequentSeparatorPos;
		}

		@Override
		public int getCount() {
			int superCount = super.getCount();

			if (mShowNumberOfContacts && (mSearchMode || superCount > 0)) {
				// We don't want to count this header if it's the only thing
				// visible, so that
				// the empty text will display.
				superCount++;
			}

			if (mSearchMode) {
				// Last element in the list is the "Find
				superCount++;
			}

			// We do not show the "Create New" button in Search mode
			if (!mSearchMode) {
				// Count the "Create new contact" line
				superCount++;
			}

			if (mSuggestionsCursorCount != 0) {
				// When showing suggestions, we have 2 additional list items:
				// the "Suggestions"
				// and "All contacts" headers.
				return mSuggestionsCursorCount + superCount + 2;
			} else if (mFrequentSeparatorPos != ListView.INVALID_POSITION) {
				// When showing strequent list, we have an additional list item
				// - the separator.
				return superCount + 1;
			} else {
				return superCount;
			}
		}

		/**
		 * Gets the actual count of contacts and excludes all the headers.
		 */
		public int getRealCount() {
			return super.getCount();
		}

		private int getRealPosition(int pos) {
			if (mShowNumberOfContacts) {
				pos--;
			}

			if (!mSearchMode) {
				return pos - 1;
			} else {
				// After the separator, remove 1 from the pos to get the real
				// underlying pos
				return pos - 1;
			}
		}

		@Override
		public Object getItem(int pos) {
			if (isSearchAllContactsItemPosition(pos)) {
				return null;
			} else {
				int realPosition = getRealPosition(pos);
				if (realPosition < 0) {
					return null;
				}
				return super.getItem(realPosition);
			}
		}

		@Override
		public long getItemId(int pos) {

			int realPosition = getRealPosition(pos);
			if (realPosition < 0) {
				return 0;
			}
			return super.getItemId(realPosition);
		}

		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			if (view instanceof PinnedHeaderListView) {
				((PinnedHeaderListView) view)
						.configureHeaderView(firstVisibleItem);
			}
		}

		public void onScrollStateChanged(AbsListView view, int scrollState) {
			if (mHighlightWhenScrolling) {
				if (scrollState != OnScrollListener.SCROLL_STATE_IDLE) {
					mHighlightingAnimation.startHighlighting();
				} else {
					mHighlightingAnimation.stopHighlighting();
				}
			}

			if (scrollState == OnScrollListener.SCROLL_STATE_FLING) {
				mPhotoLoader.pause();
			} else if (mDisplayPhotos) {
				mPhotoLoader.resume();
			}
		}

		/**
		 * Computes the state of the pinned header. It can be invisible, fully
		 * visible or partially pushed up out of the view.
		 */
		public int getPinnedHeaderState(int position) {
			if (mIndexer == null || getCursor() == null
					|| getCursor().getCount() == 0) {
				return PINNED_HEADER_GONE;
			}

			int realPosition = getRealPosition(position);
			if (realPosition < 0) {
				return PINNED_HEADER_GONE;
			}

			// The header should get pushed up if the top item shown
			// is the last item in a section for a particular letter.
			int section = getSectionForPosition(realPosition);
			int nextSectionPosition = getPositionForSection(section + 1);
			if (nextSectionPosition != -1
					&& realPosition == nextSectionPosition - 1) {
				return PINNED_HEADER_PUSHED_UP;
			}

			return PINNED_HEADER_VISIBLE;
		}

		/**
		 * Configures the pinned header by setting the appropriate text label
		 * and also adjusting color if necessary. The color needs to be adjusted
		 * when the pinned header is being pushed up from the view.
		 */
		public void configurePinnedHeader(View header, int position, int alpha) {
			PinnedHeaderCache cache = (PinnedHeaderCache) header.getTag();
			if (cache == null) {
				cache = new PinnedHeaderCache();
				cache.titleView = (TextView) header
						.findViewById(R.id.header_text);
				cache.textColor = cache.titleView.getTextColors();
				cache.background = header.getBackground();
				header.setTag(cache);
			}

			int realPosition = getRealPosition(position);
			int section = getSectionForPosition(realPosition);

			String title = (String) mIndexer.getSections()[section];
			cache.titleView.setText(title);

			if (alpha == 255) {
				// Opaque: use the default background, and the original text
				// color
				header.setBackgroundDrawable(cache.background);
				cache.titleView.setTextColor(cache.textColor);
			} else {
				// Faded: use a solid color approximation of the background, and
				// a translucent text color
				header.setBackgroundColor(Color.rgb(
						Color.red(mPinnedHeaderBackgroundColor) * alpha / 255,
						Color.green(mPinnedHeaderBackgroundColor) * alpha / 255,
						Color.blue(mPinnedHeaderBackgroundColor) * alpha / 255));

				int textColor = cache.textColor.getDefaultColor();
				cache.titleView.setTextColor(Color.argb(alpha,
						Color.red(textColor), Color.green(textColor),
						Color.blue(textColor)));
			}
		}
	}

	// TODO: fix PluralRules to handle zero correctly and use
	// Resources.getQuantityText directly
	protected String getQuantityText(int count, int zeroResourceId,
			int pluralResourceId) {
		if (count == 0) {
			return getString(zeroResourceId);
		} else {
			String format = getResources().getQuantityText(pluralResourceId,
					count).toString();
			return String.format(format, count);
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		hideSoftKeyboard();
		if (id > 0) {
			final Uri uri = getSelectedUri(position);
			// Started with query that should launch to view contact
			final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(intent);

		} else {
			signalError();
		}

	}

	/**
	 * Signal an error to the user.
	 */
	void signalError() {
		// TODO play an error beep or something...
	}

	/**
	 * Build the {@link Uri} for the given {@link ListView} position, which can
	 * be used as result when in {@link #MODE_MASK_PICKER} mode.
	 */
	private Uri getSelectedUri(int position) {
		if (position == ListView.INVALID_POSITION) {
			throw new IllegalArgumentException("Position not in list bounds");
		}

		final Cursor cursor = (Cursor) mAdapter.getItem(position);
		if (cursor == null) {
			return null;
		}

		// Build and return soft, lookup reference
		final long contactId = cursor.getLong(SUMMARY_ID_COLUMN_INDEX);
		final String lookupKey = cursor
				.getString(SUMMARY_LOOKUP_KEY_COLUMN_INDEX);
		return Contacts.getLookupUri(contactId, lookupKey);

	}

	/**
	 * Capability API listener
	 */
	private class MyCapabilityApiListener implements JoynServiceListener {
		// Service connected
		public void onServiceConnected() {
			try {
				// Register a capability listener
				capabilityApi.addCapabilitiesListener(capabilitiesListener);

				// Refresh capabilities in background
				if (capabilityApi.isServiceRegistered()) {
					RefreshCapabilitiesAsyncTask task = new RefreshCapabilitiesAsyncTask();
					task.execute((Void[]) null);
				}
			} catch (Exception e) {
				Log.e(TAG, "Can't resfresh capabilities", e);
			}
		}

		// Service disconnected
		public void onServiceDisconnected(int error) {
		}
	}

	/**
	 * Contacts API listener
	 */
	private class MyContactsApiListener implements JoynServiceListener {
		// Service connected
		public void onServiceConnected() {
			// Load contact info in the list
			loadDataSet();
		}

		// Service disconnected
		public void onServiceDisconnected(int error) {
			// TODO
		}
	}

	/**
	 * Capabilities event listener
	 */
	private class MyCapabilitiesListener extends CapabilitiesListener {
		/**
		 * Callback called when new capabilities are received for a given
		 * contact
		 * 
		 * @param contact
		 *            Contact
		 * @param capabilities
		 *            Capabilities
		 */
		public void onCapabilitiesReceived(final String contact,
				final Capabilities capabilities) {
			handler.post(new Runnable() {
				public void run() {
					// Update list of displayed contacts
					updateDataSet(contact, capabilities);
				}
			});
		};
	}

	/**
	 * Refresh capabilities task
	 */
	private class RefreshCapabilitiesAsyncTask extends
			AsyncTask<Void, Void, Void> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				Set<JoynContact> rcsContacts = contactsApi.getJoynContacts();
				for (JoynContact contact : rcsContacts) {
					Log.d(TAG,
							"Refresh capabilities for contact "
									+ contact.getContactId());
					capabilityApi.requestContactCapabilities(contact
							.getContactId());
				}
			} catch (Exception e) {
				Log.e(TAG, "Can't refresh capabilities", e);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
		}
	}

	/**
	 * Update contact info in the list
	 * 
	 * @param contact
	 *            Contact
	 * @param capabilities
	 *            Capabilities
	 */
	private synchronized void updateDataSet(String contact,
			Capabilities capabilities) {
		Log.d(TAG, "Update list of contacts");

		// Update list item
		int position = -1;
		for (int i = 0; i < items.size(); i++) {
			String contactItem = items.get(i);
			if (contactItem.equals(contact)) {
				position = i;
			}
		}

		// Get contact info
		JoynContact joynContact = null;
		try {
			joynContact = contactsApi.getJoynContact(contact);
		} catch (Exception e) {
		}

		// Check if Popup is supported by the contact
		if ((joynContact == null) && (position != -1)) {
			// Contact is no more joyn compliant, remove it from the list
			Log.d(TAG, "Remove contact " + contact);
			items.remove(position);
		} else {
			
			boolean online = joynContact.isRegistered();			

			// Add a new contact if the contact not yet in the list and supports
			// the feature
			if ((position == -1)  && online) {
				Log.d(TAG, "Add contact " + contact);
				items.add(contact);
			}

			// Remove the contact if the contact in the list but offline or
			// Popup no more supported
			if ((position != -1) &&  !online) {
				// Remove contact
				Log.d(TAG, "Remove contact " + contact);
				items.remove(position);
			}
		}

		mAdapter.notifyDataSetChanged();
	}

	/**
	 * Load contact info in the list
	 */
	private synchronized void loadDataSet() {
		Log.d(TAG, "Load list of contacts");

		// Init
		items.clear();

		// Load RCS contacts
		try {
			Set<JoynContact> supportedContacts = contactsApi
					.getJoynContactsOnline();
			for (JoynContact contact : supportedContacts) {
				if (contact.isRegistered()) {
					Log.d(TAG, "Add contact " + contact.getContactId());
					items.add(contact.getContactId());
				}
			}
		} catch (Exception e) {
		}
		mAdapter.notifyDataSetChanged();
	}
}
