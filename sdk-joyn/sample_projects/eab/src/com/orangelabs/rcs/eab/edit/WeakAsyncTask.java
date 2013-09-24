/*******************************************************************************
 * Software Name : RCS
 *
 * Copyright © 2010 France Telecom S.A.
 ******************************************************************************/

package com.orangelabs.rcs.eab.edit;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;

public abstract class WeakAsyncTask<Params, Progress, Result, WeakTarget> extends
        AsyncTask<Params, Progress, Result> {
    protected WeakReference<WeakTarget> mTarget;

    public WeakAsyncTask(WeakTarget target) {
        mTarget = new WeakReference<WeakTarget>(target);
    }

    /** {@inheritDoc} */
    @Override
    protected final void onPreExecute() {
        final WeakTarget target = mTarget.get();
        if (target != null) {
            this.onPreExecute(target);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected final Result doInBackground(Params... params) {
        final WeakTarget target = mTarget.get();
        if (target != null) {
            return this.doInBackground(target, params);
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected final void onPostExecute(Result result) {
        final WeakTarget target = mTarget.get();
        if (target != null) {
            this.onPostExecute(target, result);
        }
    }

    protected void onPreExecute(WeakTarget target) {
        // No default action
    }

    protected abstract Result doInBackground(WeakTarget target, Params... params);

    protected void onPostExecute(WeakTarget target, Result result) {
        // No default action
    }
}
