/*******************************************************************************
 * Software Name : RCS
 *
 * Copyright © 2010 France Telecom S.A.
 ******************************************************************************/

package com.orangelabs.rcs.eab.edit.model;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.ContentProviderOperation.Builder;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;
import android.provider.ContactsContract.AggregationExceptions;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;

import java.util.ArrayList;

import com.orangelabs.rcs.eab.edit.Entity;
import com.orangelabs.rcs.eab.edit.EntityIterator;
import com.orangelabs.rcs.eab.edit.Lists;
import com.orangelabs.rcs.eab.edit.model.EntityDelta.ValuesDelta;

/**
 * Container for multiple {@link EntityDelta} objects, usually when editing
 * together as an entire aggregate. Provides convenience methods for parceling
 * and applying another {@link EntitySet} over it.
 */
public class EntitySet extends ArrayList<EntityDelta> implements Parcelable {
    private boolean mSplitRawContacts;

    /**
     * The package name to use when creating {@link Resources} objects for
     * this data row. This value is only designed for use when building user
     * interfaces, and should not be used to infer the owner.
     *
     * @hide
     */
    public static final String RES_PACKAGE = "res_package";

    public static final String IS_RESTRICTED = "is_restricted";
    
    private static final String[] DATA_KEYS = new String[]{
        Data.DATA1,
        Data.DATA2,
        Data.DATA3,
        Data.DATA4,
        Data.DATA5,
        Data.DATA6,
        Data.DATA7,
        Data.DATA8,
        Data.DATA9,
        Data.DATA10,
        Data.DATA11,
        Data.DATA12,
        Data.DATA13,
        Data.DATA14,
        Data.DATA15,
        Data.SYNC1,
        Data.SYNC2,
        Data.SYNC3,
        Data.SYNC4};
    
    public static final String[] PROJECTION = new String[]{
        RawContacts.ACCOUNT_NAME,
        RawContacts.ACCOUNT_TYPE,
        RawContacts.SOURCE_ID,
        RawContacts.VERSION,
        RawContacts.DIRTY,
        RawContacts.Entity.DATA_ID,
        RES_PACKAGE,
        Data.MIMETYPE,
        Data.DATA1,
        Data.DATA2,
        Data.DATA3,
        Data.DATA4,
        Data.DATA5,
        Data.DATA6,
        Data.DATA7,
        Data.DATA8,
        Data.DATA9,
        Data.DATA10,
        Data.DATA11,
        Data.DATA12,
        Data.DATA13,
        Data.DATA14,
        Data.DATA15,
        Data.SYNC1,
        Data.SYNC2,
        Data.SYNC3,
        Data.SYNC4,
        RawContacts._ID,
        Data.IS_PRIMARY,
        Data.IS_SUPER_PRIMARY,
        Data.DATA_VERSION,
        GroupMembership.GROUP_SOURCE_ID,
        RawContacts.SYNC1,
        RawContacts.SYNC2,
        RawContacts.SYNC3,
        RawContacts.SYNC4,
        RawContacts.DELETED,
        RawContacts.CONTACT_ID,
        RawContacts.STARRED,
        IS_RESTRICTED};

    private EntitySet() {
    }

    /**
     * Create an {@link EntitySet} that contains the given {@link EntityDelta},
     * usually when inserting a new {@link Contacts} entry.
     */
    public static EntitySet fromSingle(EntityDelta delta) {
        final EntitySet state = new EntitySet();
        state.add(delta);
        return state;
    }

    /**
     * Create an {@link EntitySet} based on {@link Contacts} specified by the
     * given query parameters. This closes the {@link EntityIterator} when
     * finished, so it doesn't subscribe to updates.
     */
    public static EntitySet fromQuery(ContentResolver resolver, String selection,
            String[] selectionArgs, String sortOrder) {
        
        final EntitySet state = new EntitySet();

//        EntityIterator iterator = null;
        
//TODO commented, as queryEntities is not visible        try {
//            // Perform background query to pull contact details
//            iterator = resolver.queryEntities(RawContacts.CONTENT_URI, 
//            		selection, 
//            		selectionArgs,
//                    sortOrder);
//            while (iterator.hasNext()) {
//                // Read all contacts into local deltas to prepare for edits
//                final Entity before = iterator.next();
//                final EntityDelta entity = EntityDelta.fromBefore(before);
//                state.add(entity);
//            }
//        } catch (RemoteException e) {
//            throw new IllegalStateException("Problem querying contact details", e);
//        } finally {
//            if (iterator != null) {
//                iterator.close();
//            }
//        }
        
        final Uri.Builder builder = ContactsContract.RawContactsEntity.CONTENT_URI.buildUpon();
        String query = RawContacts.CONTENT_URI.getQuery();
        builder.encodedQuery(query);
        Cursor cursor = resolver.query(builder.build(),
        		PROJECTION, 
        		selection, 
        		selectionArgs, 
        		sortOrder);
        if (cursor.moveToFirst()){
        	// we expect the cursor is already at the row we need to read from
        	ContentValues contactValues = new ContentValues();

        	long rawContactId = -1;

        	int index = cursor.getColumnIndex(RawContacts._ID);

        	if (index!=-1){
        		rawContactId = cursor.getLong(index);	
        		contactValues.put(RawContacts._ID, rawContactId);
        	}
        	index = cursor.getColumnIndex(RawContacts.ACCOUNT_NAME);
        	if (index!=-1){
        		contactValues.put(RawContacts.ACCOUNT_NAME, cursor.getString(index));
        	}
        	index = cursor.getColumnIndex(RawContacts.ACCOUNT_TYPE);
        	if (index!=-1){
        		contactValues.put(RawContacts.ACCOUNT_TYPE, cursor.getString(index));
        	}
        	index = cursor.getColumnIndex(RawContacts.SYNC1);
        	if (index!=-1){
        		contactValues.put(RawContacts.SYNC1, cursor.getString(index));
        	}
        	index = cursor.getColumnIndex(RawContacts.SYNC2);
        	if (index!=-1){
        		contactValues.put(RawContacts.SYNC2, cursor.getString(index));
        	}
        	index = cursor.getColumnIndex(RawContacts.SYNC3);
        	if (index!=-1){
        		contactValues.put(RawContacts.SYNC3, cursor.getString(index));
        	}
        	index = cursor.getColumnIndex(RawContacts.SYNC4);
        	if (index!=-1){
        		contactValues.put(RawContacts.SYNC4, cursor.getString(index));
        	}
        	index = cursor.getColumnIndex(RawContacts.CONTACT_ID);
        	if (index!=-1){
        		contactValues.put(RawContacts.CONTACT_ID, cursor.getLong(index));
        	}
        	index = cursor.getColumnIndex(RawContacts.STARRED);
        	if (index!=-1){
        		contactValues.put(RawContacts.STARRED, cursor.getLong(index));
        	}
        	index = cursor.getColumnIndex(RawContacts.DIRTY);
        	if (index!=-1){
        		contactValues.put(RawContacts.DIRTY, cursor.getLong(index));
        	}
        	index = cursor.getColumnIndex(RawContacts.DELETED);
        	if (index!=-1){
        		contactValues.put(RawContacts.DELETED, cursor.getLong(index));
        	}
        	index = cursor.getColumnIndex(IS_RESTRICTED);
        	if (index!=-1){
        		contactValues.put(IS_RESTRICTED, cursor.getInt(index));
        	}
        	index = cursor.getColumnIndex(RawContacts.SOURCE_ID);
        	if (index!=-1){
        		contactValues.put(RawContacts.SOURCE_ID, cursor.getString(index));
        	}
        	index = cursor.getColumnIndex(RawContacts.VERSION);
        	if (index!=-1){
        		contactValues.put(RawContacts.VERSION, cursor.getLong(index));
        	}

        	Entity before = new Entity(contactValues);

        	do {
        		// add the data to to the contact
        		ContentValues dataValues = new ContentValues();
        		// Get the data row id (id in the "ContactsContract.Contacts.CONTENT_URI")
        		index = cursor.getColumnIndex("data_id");
        		if (index!=-1){
        			dataValues.put(Data._ID, cursor.getLong(index));
        		}
        		index = cursor.getColumnIndex(RES_PACKAGE);
        		if (index!=-1){
        			dataValues.put(RES_PACKAGE, cursor.getString(index));
        		}else{
        			dataValues.putNull(RES_PACKAGE);
        		}
        		index = cursor.getColumnIndex(Data.MIMETYPE);
        		if (index!=-1){
        			dataValues.put(Data.MIMETYPE, cursor.getString(index));
        		}
        		index = cursor.getColumnIndex(Data.IS_PRIMARY);
        		if (index!=-1){
        			dataValues.put(Data.IS_PRIMARY, cursor.getLong(index));
        		}else{
        			dataValues.putNull(Data.IS_PRIMARY);
        		}
        		index = cursor.getColumnIndex(Data.IS_SUPER_PRIMARY);
        		if (index!=-1){
        			dataValues.put(Data.IS_SUPER_PRIMARY, cursor.getLong(index));
        		}else{
        			dataValues.putNull(Data.IS_SUPER_PRIMARY);
        		}
        		index = cursor.getColumnIndex(Data.DATA_VERSION);
        		if (index!=-1){
        			dataValues.put(Data.DATA_VERSION, cursor.getLong(index));
        		}else{
        			dataValues.putNull(Data.DATA_VERSION);
        		}
        		index = cursor.getColumnIndex(GroupMembership.GROUP_SOURCE_ID);
        		if (index!=-1){
        			dataValues.put(GroupMembership.GROUP_SOURCE_ID, cursor.getString(index));
        		}else{
        			dataValues.putNull(GroupMembership.GROUP_SOURCE_ID);
        		}

        		for (int i = 0; i < DATA_KEYS.length; i++) {
        			String key = DATA_KEYS[i];
        			index = cursor.getColumnIndex(key);
        			if (index!=-1){
        				if (!cursor.isNull(index)){
        					String value = "";
        					boolean isString = false;
        					try{
        						value = cursor.getString(index);
        						isString = true;
        					}catch(Exception e){
        						dataValues.put(key, cursor.getBlob(index));
        					}   
        					if (isString){
        						if (!Phone.CONTENT_ITEM_TYPE.equalsIgnoreCase(dataValues.getAsString(Data.MIMETYPE))){
        							// If the value is for a phone number, we do not try to convert it to long, as the result may be unfortunate
        							try{
        								Long.parseLong(value);
        								dataValues.put(key, cursor.getLong(index));
        							}catch(NumberFormatException nfe){
        								dataValues.put(key, cursor.getString(index));
        							}
        						}else{
        							// This is a phone number, we put it as string 
        							dataValues.put(key, cursor.getString(index));
        						}
        					}
        				}
        			}
        		}
        		before.addSubValue(Data.CONTENT_URI, dataValues);
        	}while(cursor.moveToNext());

        	final EntityDelta entity = EntityDelta.fromBefore(before);
        	state.add(entity);
        }
        return state;
    }

    /**
     * Merge the "after" values from the given {@link EntitySet}, discarding any
     * previous "after" states. This is typically used when re-parenting user
     * edits onto an updated {@link EntitySet}.
     */
    public static EntitySet mergeAfter(EntitySet local, EntitySet remote) {
        if (local == null) local = new EntitySet();

        // For each entity in the remote set, try matching over existing
        for (EntityDelta remoteEntity : remote) {
            final Long rawContactId = remoteEntity.getValues().getId();

            // Find or create local match and merge
            final EntityDelta localEntity = local.getByRawContactId(rawContactId);
            final EntityDelta merged = EntityDelta.mergeAfter(localEntity, remoteEntity);

            if (localEntity == null && merged != null) {
                // No local entry before, so insert
                local.add(merged);
            }
        }

        return local;
    }

    /**
     * Build a list of {@link ContentProviderOperation} that will transform all
     * the "before" {@link Entity} states into the modified state which all
     * {@link EntityDelta} objects represent. This method specifically creates
     * any {@link AggregationExceptions} rules needed to groups edits together.
     */
    public ArrayList<ContentProviderOperation> buildDiff() {
        final ArrayList<ContentProviderOperation> diff = Lists.newArrayList();

        final long rawContactId = this.findRawContactId();
        int firstInsertRow = -1;

        // First pass enforces versions remain consistent
        for (EntityDelta delta : this) {
            delta.buildAssert(diff);
        }

        final int assertMark = diff.size();
        int backRefs[] = new int[size()];

        int rawContactIndex = 0;

        // Second pass builds actual operations
        for (EntityDelta delta : this) {
            final int firstBatch = diff.size();
            backRefs[rawContactIndex++] = firstBatch;
            delta.buildDiff(diff);

            // Only create rules for inserts
            if (!delta.isContactInsert()) continue;

            // If we are going to split all contacts, there is no point in first combining them
            if (mSplitRawContacts) continue;

            if (rawContactId != -1) {
                // Has existing contact, so bind to it strongly
                final Builder builder = beginKeepTogether();

                builder.withValue(AggregationExceptions.RAW_CONTACT_ID1, rawContactId);
                builder.withValueBackReference(AggregationExceptions.RAW_CONTACT_ID2, firstBatch);
                diff.add(builder.build());
                
            } else if (firstInsertRow == -1) {
                // First insert case, so record row
                firstInsertRow = firstBatch;

            } else {
                // Additional insert case, so point at first insert
                final Builder builder = beginKeepTogether();
                builder.withValueBackReference(AggregationExceptions.RAW_CONTACT_ID1, firstInsertRow);
                builder.withValueBackReference(AggregationExceptions.RAW_CONTACT_ID2, firstBatch);
                diff.add(builder.build());
            }
        }

        if (mSplitRawContacts) {
            buildSplitContactDiff(diff, backRefs);
        }

        // No real changes if only left with asserts
        if (diff.size() == assertMark) {
            diff.clear();
        }

        return diff;
    }

    /**
     * Start building a {@link ContentProviderOperation} that will keep two
     * {@link RawContacts} together.
     */
    protected Builder beginKeepTogether() {
        final Builder builder = ContentProviderOperation
                .newUpdate(AggregationExceptions.CONTENT_URI);
        builder.withValue(AggregationExceptions.TYPE, AggregationExceptions.TYPE_KEEP_TOGETHER);
        return builder;
    }

    /**
     * Builds {@link AggregationExceptions} to split all constituent raw contacts into
     * separate contacts.
     */
    private void buildSplitContactDiff(final ArrayList<ContentProviderOperation> diff,
            int[] backRefs) {
        int count = size();
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < count; j++) {
                if (i != j) {
                    buildSplitContactDiff(diff, i, j, backRefs);
                }
            }
        }
    }

    /**
     * Construct a {@link AggregationExceptions#TYPE_KEEP_SEPARATE}.
     */
    private void buildSplitContactDiff(ArrayList<ContentProviderOperation> diff, int index1,
            int index2, int[] backRefs) {
        Builder builder =
                ContentProviderOperation.newUpdate(AggregationExceptions.CONTENT_URI);
        builder.withValue(AggregationExceptions.TYPE, AggregationExceptions.TYPE_KEEP_SEPARATE);

        Long rawContactId1 = get(index1).getValues().getAsLong(RawContacts._ID);
        if (rawContactId1 != null && rawContactId1 >= 0) {
            builder.withValue(AggregationExceptions.RAW_CONTACT_ID1, rawContactId1);
        } else {
            builder.withValueBackReference(AggregationExceptions.RAW_CONTACT_ID1, backRefs[index1]);
        }

        Long rawContactId2 = get(index2).getValues().getAsLong(RawContacts._ID);
        if (rawContactId2 != null && rawContactId2 >= 0) {
            builder.withValue(AggregationExceptions.RAW_CONTACT_ID2, rawContactId2);
        } else {
            builder.withValueBackReference(AggregationExceptions.RAW_CONTACT_ID2, backRefs[index2]);
        }
        diff.add(builder.build());
    }

    /**
     * Search all contained {@link EntityDelta} for the first one with an
     * existing {@link RawContacts#_ID} value. Usually used when creating
     * {@link AggregationExceptions} during an update.
     */
    public long findRawContactId() {
        for (EntityDelta delta : this) {
            final Long rawContactId = delta.getValues().getAsLong(RawContacts._ID);
            if (rawContactId != null && rawContactId >= 0) {
                return rawContactId;
            }
        }
        return -1;
    }

    /**
     * Find {@link RawContacts#_ID} of the requested {@link EntityDelta}.
     */
    public Long getRawContactId(int index) {
        if (index >= 0 && index < this.size()) {
            final EntityDelta delta = this.get(index);
            final ValuesDelta values = delta.getValues();
            if (values.isVisible()) {
                return values.getAsLong(RawContacts._ID);
            }
        }
        return null;
    }

    public EntityDelta getByRawContactId(Long rawContactId) {
        final int index = this.indexOfRawContactId(rawContactId);
        return (index == -1) ? null : this.get(index);
    }

    /**
     * Find index of given {@link RawContacts#_ID} when present.
     */
    public int indexOfRawContactId(Long rawContactId) {
        if (rawContactId == null) return -1;
        final int size = this.size();
        for (int i = 0; i < size; i++) {
            final Long currentId = getRawContactId(i);
            if (rawContactId.equals(currentId)) {
                return i;
            }
        }
        return -1;
    }

    public ValuesDelta getSuperPrimaryEntry(final String mimeType) {
        ValuesDelta primary = null;
        ValuesDelta randomEntry = null;
        for (EntityDelta delta : this) {
            final ArrayList<ValuesDelta> mimeEntries = delta.getMimeEntries(mimeType);
            if (mimeEntries == null) return null;

            for (ValuesDelta entry : mimeEntries) {
                if (entry.isSuperPrimary()) {
                    return entry;
                } else if (primary == null && entry.isPrimary()) {
                    primary = entry;
                } else if (randomEntry == null) {
                    randomEntry = entry;
                }
            }
        }
        // When no direct super primary, return something
        if (primary != null) {
            return primary;
        }
        return randomEntry;
    }

    public void splitRawContacts() {
        mSplitRawContacts = true;
    }

    /** {@inheritDoc} */
    public int describeContents() {
        // Nothing special about this parcel
        return 0;
    }

    /** {@inheritDoc} */
    public void writeToParcel(Parcel dest, int flags) {
        final int size = this.size();
        dest.writeInt(size);
        for (EntityDelta delta : this) {
            dest.writeParcelable(delta, flags);
        }
    }

    public void readFromParcel(Parcel source) {
        final int size = source.readInt();
        for (int i = 0; i < size; i++) {
            this.add(source.<EntityDelta> readParcelable(null));
        }
    }

    public static final Parcelable.Creator<EntitySet> CREATOR = new Parcelable.Creator<EntitySet>() {
        public EntitySet createFromParcel(Parcel in) {
            final EntitySet state = new EntitySet();
            state.readFromParcel(in);
            return state;
        }

        public EntitySet[] newArray(int size) {
            return new EntitySet[size];
        }
    };
}
