/*******************************************************************************
 * Software Name : RCS
 *
 * Copyright © 2010 France Telecom S.A.
 ******************************************************************************/

package com.orangelabs.rcs.eab.edit;

import android.os.RemoteException;

/**
 * @hide
 */
public interface EntityIterator {
    /**
     * Returns whether there are more elements to iterate, i.e. whether the
     * iterator is positioned in front of an element.
     *
     * @return {@code true} if there are more elements, {@code false} otherwise.
     * @see #next
     * @since Android 1.0
     */
    public boolean hasNext() throws RemoteException;

    /**
     * Returns the next object in the iteration, i.e. returns the element in
     * front of the iterator and advances the iterator by one position.
     *
     * @return the next object.
     * @throws java.util.NoSuchElementException
     *             if there are no more elements.
     * @see #hasNext
     * @since Android 1.0
     */
    public Entity next() throws RemoteException;

    public void reset() throws RemoteException;

    /**
     * Indicates that this iterator is no longer needed and that any associated resources
     * may be released (such as a SQLite cursor).
     */
    public void close();
}
