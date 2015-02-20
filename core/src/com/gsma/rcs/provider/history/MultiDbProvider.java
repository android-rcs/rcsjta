/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.rcs.provider.history;

import static com.gsma.rcs.provider.history.HistoryConstants.INTERNAL_MEMBERS;
import static com.gsma.rcs.provider.history.HistoryConstants.INTERNAL_MEMBER_IDS;

import android.content.ContentProvider;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.SparseArray;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* package private */abstract class MultiDbProvider extends ContentProvider {

    private static final String MAX_PROJECTION = new StringBuilder().append("MAX(")
            .append(BaseColumns._ID).append(")").toString();

    private final SparseArray<HistoryMemberDatabase> mHistoryMemberDatabases = new SparseArray<HistoryMemberDatabase>();

    private final Set<String> mForbiddenCanonicalPaths = new HashSet<String>();

    /**
     * This is a flag put to true each time a member is registered so to execute refresh on query.
     */
    private boolean mAllDatabasesAttached = false;

    private DatabaseHelper mOpenHelper;

    protected final QueryHelper mQueryHelper = new QueryHelper();

    private final class DatabaseHelper extends SQLiteOpenHelper {

        private static final int DATABASE_VERSION = 1;

        private DatabaseHelper(Context context) {
            super(context, null, null, DATABASE_VERSION);
        }

        private String getDatabaseAlias(int providerId) {
            return "db".concat(String.valueOf(providerId));
        }

        private void detach(int providerId) {
            getWritableDatabase().execSQL(
                    new StringBuilder("DETACH DATABASE ").append(getDatabaseAlias(providerId))
                            .toString());
            mHistoryMemberDatabases.get(providerId).setAttached(false);
        }

        private void detachAll() {
            for (int i = 0; i < mHistoryMemberDatabases.size(); i++) {
                int providerId = mHistoryMemberDatabases.keyAt(i);
                if (mHistoryMemberDatabases.get(providerId).isAttached()) {
                    detach(providerId);
                }
            }
        }

        private void attach(int providerId) {
            HistoryMemberDatabase memberDatabase = mHistoryMemberDatabases.get(providerId);
            Uri database = memberDatabase.getDatabaseUri();
            String attachCommand = new StringBuilder("ATTACH DATABASE '")
                    .append(database.getPath()).append("' AS ")
                    .append(getDatabaseAlias(providerId)).toString();
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL(attachCommand);
            mHistoryMemberDatabases.get(providerId).setAttached(true);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
        }
    }

    private void registerDatabase(HistoryMemberDatabase memberDatabase) throws IOException {
        mHistoryMemberDatabases.put(memberDatabase.getProviderId(), memberDatabase);

        String canonicalPath = new File(memberDatabase.getDatabaseUri().getPath())
                .getCanonicalPath();
        mForbiddenCanonicalPaths.add(canonicalPath);

        mQueryHelper.generateSubQuery(memberDatabase.getProviderId(),
                memberDatabase.getColumnMapping(), memberDatabase.getTableName());
    }

    protected void ensureDatabaseAttached(int providerId) {
        HistoryMemberDatabase memberDatabase = mHistoryMemberDatabases.get(providerId);
        if (memberDatabase.isAttached()) {
            return;
        }
        synchronized (memberDatabase) {
            Cursor cursor = null;
            try {
                cursor = getContext().getContentResolver().query(
                        memberDatabase.getContentProviderUri(), new String[] {
                            MAX_PROJECTION
                        }, null, null, null);

                if (cursor.moveToNext()) {
                    memberDatabase.setMaxId(cursor.getLong(0));
                }

            } finally {
                cursor.close();
            }
            mOpenHelper.attach(providerId);
        }
    }

    /* package private */void ensureDatabasesAttached(List<String> providerIds) {
        if (mAllDatabasesAttached) {
            return;
        }
        for (String providerId : providerIds) {
            ensureDatabaseAttached(Integer.parseInt(providerId));
        }
        mAllDatabasesAttached = true;
        for (int i = 0; i < mHistoryMemberDatabases.size(); i++) {
            HistoryMemberDatabase member = mHistoryMemberDatabases.valueAt(i);
            if (!member.isAttached()) {
                mAllDatabasesAttached = false;
                break;
            }
        }
    }

    /* package private */long getMaxId(int providerId) {
        ensureDatabaseAttached(providerId);
        return mHistoryMemberDatabases.get(providerId).getMaxId();
    }

    /**
     * Initializes the internal providers. Used by the unit test to reinitialize after shutdown.
     * 
     * @throws IOException
     */
    public void registerInternalProviders() throws IOException {
        if (mHistoryMemberDatabases.size() > 0) {
            return;
        }

        for (String databaseName : HistoryConstants.PROTECTED_INTERNAL_DATABASES) {
            Uri databaseUri = Uri.fromFile(getContext().getDatabasePath(databaseName));
            String canonicalPath = new File(databaseUri.getPath()).getCanonicalPath();
            mForbiddenCanonicalPaths.add(canonicalPath);
        }

        for (HistoryMemberDatabase internalMember : INTERNAL_MEMBERS) {
            Uri databaseUri = Uri.fromFile(getContext().getDatabasePath(
                    internalMember.getDatabaseName()));
            internalMember.setDatabaseUri(databaseUri);
            registerDatabase(internalMember);
        }

    }

    protected Cursor executeReadQuery(String sql, String[] selectionArgs) {
        return mOpenHelper.getReadableDatabase().rawQuery(sql, selectionArgs);
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        try {
            registerInternalProviders();
        } catch (IOException e) {
            throw new IllegalStateException("Problem registering internal history providers!", e);
        }
        return true;
    }

    @Override
    public void shutdown() {
        if (mOpenHelper == null) {
            return;
        }
        mOpenHelper.detachAll();
        mAllDatabasesAttached = false;
        mQueryHelper.clear();
        mForbiddenCanonicalPaths.clear();
        mHistoryMemberDatabases.clear();
    }

    public void registerDatabase(int providerId, Uri contentProviderUri, Uri databaseUri,
            String tableName, Map<String, String> columnMapping) throws IOException {
        if (mHistoryMemberDatabases.get(providerId) != null) {
            /* TODO: This exception handling will be changed with CR037. */
            throw new IllegalArgumentException(new StringBuilder(
                    "Cannot register external database for already registered provider id ")
                    .append(providerId).append("!").toString());
        }

        String canonicalPath = new File(databaseUri.getPath()).getCanonicalPath();
        if (mForbiddenCanonicalPaths.contains(canonicalPath)) {
            /* TODO: This exception handling will be changed with CR037. */
            throw new IllegalArgumentException(new StringBuilder("Forbidden to add '")
                    .append(databaseUri).append("'").append(" as a history log member!").toString());
        }
        registerDatabase(new HistoryMemberDatabase(providerId, contentProviderUri, null,
                databaseUri, tableName, columnMapping));
    }

    public void unregisterDatabaseByProviderId(int providerId) {
        if (INTERNAL_MEMBER_IDS.contains(providerId)) {
            /* TODO: This exception handling will be changed with CR037. */
            throw new IllegalArgumentException(new StringBuilder(
                    "Trying to access history log member with invalid external id:")
                    .append(providerId).append("!").toString());
        }

        synchronized (mOpenHelper) {
            mQueryHelper.clearProvider(providerId);
            if (mHistoryMemberDatabases.get(providerId).isAttached()) {
                mOpenHelper.detach(providerId);
            }

        }
    }
}
