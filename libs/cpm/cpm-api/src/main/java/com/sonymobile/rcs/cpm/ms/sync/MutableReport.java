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

package com.sonymobile.rcs.cpm.ms.sync;

import java.io.Serializable;

public class MutableReport implements SyncReport, Serializable {

    private static final long serialVersionUID = 1L;

    private final int mProgressMax;

    private int mProgress;

    private final long mStartedTime;

    private long mStoppedTime;

    private boolean mSuccess;

    private boolean mStopped;

    private boolean mCancelled;

    private boolean mPaused;

    private String mMessage;

    private int mDeletedRemote;

    private int mDeletedLocal;

    private int mUpdatedRemote;

    private int mUpdatedLocal;

    private int mAddedRemote;

    private int mAddedLocal;

    private Exception mException;

    private String mStrategyName;

    public MutableReport() {
        mStartedTime = System.currentTimeMillis();
        mProgressMax = 100;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SYNCREPORT[");

        sb.append("strategy=");
        sb.append(mStrategyName);
        sb.append(',');

        sb.append("progress=");
        sb.append(mProgress);
        sb.append('/');
        sb.append(mProgressMax);
        sb.append(',');

        sb.append("startedTime=");
        sb.append(mStartedTime);
        sb.append(',');

        sb.append("stoppedTime=");
        sb.append(mStoppedTime);
        sb.append(',');

        sb.append("success=");
        sb.append(mSuccess);
        sb.append(',');

        sb.append("stopped=");
        sb.append(mStopped);
        sb.append(',');

        sb.append("cancelled=");
        sb.append(mCancelled);
        sb.append(',');

        sb.append("paused=");
        sb.append(mPaused);
        sb.append(',');

        sb.append("message=");
        sb.append(mMessage);
        sb.append(',');

        sb.append("deletedRemote=");
        sb.append(mDeletedRemote);
        sb.append(',');

        sb.append("deletedLocal=");
        sb.append(mDeletedLocal);
        sb.append(',');

        sb.append("updatedRemote=");
        sb.append(mUpdatedRemote);
        sb.append(',');

        sb.append("updatedLocal=");
        sb.append(mUpdatedLocal);
        sb.append(',');

        sb.append("addedRemote=");
        sb.append(mAddedRemote);
        sb.append(',');

        sb.append("addedLocal=");
        sb.append(mAddedLocal);

        sb.append("]");
        return sb.toString();
    }

    public void setStrategyName(String strategyName) {
        this.mStrategyName = strategyName;
    }

    @Override
    public String getStrategyName() {
        return mStrategyName;
    }

    @Override
    public boolean isPaused() {
        return mPaused;
    }

    @Override
    public int getProgress() {
        return mProgress;
    }

    public void setPaused(boolean paused) {
        this.mPaused = paused;
    }

    public void setProgress(int progress) {
        this.mProgress = progress;
    }

    @Override
    public int getProgressMax() {
        return mProgressMax;
    }

    public boolean isStarted() {
        return mStartedTime > 0;
    }

    @Override
    public long getStartedTime() {
        return mStartedTime;
    }

    @Override
    public long getStoppedTime() {
        return mStoppedTime;
    }

    public void setStoppedTime(long stoppedTime) {
        this.mStoppedTime = stoppedTime;
    }

    @Override
    public boolean isSuccess() {
        return mSuccess;
    }

    public void setSuccess(boolean success) {
        this.mSuccess = success;
    }

    @Override
    public boolean isStopped() {
        return mStopped;
    }

    public void setStopped() {
        if (!mStopped) {
            this.mStoppedTime = System.currentTimeMillis();
        }
        this.mStopped = true;
    }

    @Override
    public boolean isCancelled() {
        return mCancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.mCancelled = cancelled;
    }

    @Override
    public String getMessage() {
        return mMessage;
    }

    public void setMessage(String message) {
        this.mMessage = message;
    }

    @Override
    public int getDeletedRemote() {
        return mDeletedRemote;
    }

    public void setDeletedRemote(int deletedRemote) {
        this.mDeletedRemote = deletedRemote;
    }

    @Override
    public int getDeletedLocal() {
        return mDeletedLocal;
    }

    public void setDeletedLocal(int deletedLocal) {
        this.mDeletedLocal = deletedLocal;
    }

    @Override
    public int getUpdatedRemote() {
        return mUpdatedRemote;
    }

    public void setUpdatedRemote(int updatedRemote) {
        this.mUpdatedRemote = updatedRemote;
    }

    @Override
    public int getUpdatedLocal() {
        return mUpdatedLocal;
    }

    public void setUpdatedLocal(int updatedLocal) {
        this.mUpdatedLocal = updatedLocal;
    }

    @Override
    public int getAddedRemote() {
        return mAddedRemote;
    }

    public void setAddedRemote(int addedRemote) {
        this.mAddedRemote = addedRemote;
    }

    @Override
    public int getAddedLocal() {
        return mAddedLocal;
    }

    public void setAddedLocal(int addedLocal) {
        this.mAddedLocal = addedLocal;
    }

    public void setException(Exception e) {
        mException = e;
    }

    @Override
    public Exception getException() {
        return mException;
    }

}
