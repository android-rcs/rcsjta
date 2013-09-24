/*******************************************************************************
 * Software Name : RCS
 *
 * Copyright © 2010 France Telecom S.A.
 ******************************************************************************/

package com.orangelabs.rcs.eab.edit.model;

import com.orangelabs.rcs.eab.edit.model.ContactsSource.DataKind;
import com.orangelabs.rcs.eab.edit.model.EntityDelta.ValuesDelta;

import android.provider.ContactsContract.Data;

/**
 * Generic definition of something that edits a {@link Data} row through an
 * {@link ValuesDelta} object.
 */
public interface Editor {
    /**
     * Listener for an {@link Editor}, usually to handle deleted items.
     */
    public interface EditorListener {
        /**
         * Called when the given {@link Editor} has been deleted.
         */
        public void onDeleted(Editor editor);

        /**
         * Called when the given {@link Editor} has a request, for example it
         * wants to select a photo.
         */
        public void onRequest(int request);

        public static final int REQUEST_PICK_PHOTO = 1;
        public static final int FIELD_CHANGED = 2;
    }

    /**
     * Prepare this editor for the given {@link ValuesDelta}, which
     * builds any needed views. Any changes performed by the user will be
     * written back to that same object.
     */
    public void setValues(DataKind kind, ValuesDelta values, EntityDelta state, boolean readOnly);

    /**
     * Add a specific {@link EditorListener} to this {@link Editor}.
     */
    public void setEditorListener(EditorListener listener);

    /**
     * Called internally when the contents of a specific field have changed,
     * allowing advanced editors to persist data in a specific way.
     */
    public void onFieldChanged(String column, String value);
}
