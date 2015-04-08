/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.services.rcs;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

import android.content.Context;
import android.os.IInterface;
import android.util.SparseArray;

/**
 * Abstract RCS service
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class RcsService {
    /**
     * Action to broadcast when RCS service is up.
     */
    public static final String ACTION_SERVICE_UP = "com.gsma.services.rcs.action.SERVICE_UP";

    /**
     * Action to broadcast when RCS service is provisioned.
     */
    public static final String ACTION_SERVICE_PROVISIONING_DATA_CHANGED = "com.gsma.services.rcs.action.SERVICE_PROVISIONNING_DATA_CHANGED";

    private static final String ERROR_CNX = "Service not connected";

    protected final RcsServiceControl mRcsServiceControl;

    private final Map<RcsServiceRegistrationListener, WeakReference<IRcsServiceRegistrationListener>> mRegistrationListeners = new WeakHashMap<RcsServiceRegistrationListener, WeakReference<IRcsServiceRegistrationListener>>();

    /**
     * Information about the current build
     */
    public static class Build {
        /**
         * List of version codes
         */
        public static class VERSION_CODES {
            /**
             * The original first version of RCS API
             */
            public final static int BASE = 0;

            /**
             * Blackbird version of RCS API
             */
            public final static int BLACKBIRD = 1;
        }

        /**
         * API release implementor name
         */
        public static final String API_CODENAME = "GSMA";

        /**
         * API version number
         * 
         * @see VERSION_CODES
         */
        public static final int API_VERSION = VERSION_CODES.BLACKBIRD;

        /**
         * Internal number used by the underlying source control to represent this build
         */
        public static final int API_INCREMENTAL = 1;

        private Build() {
        }
    }

    /**
     * Direction of the communication for Chat message, Geolocation, Filetransfer, Imageshare,
     * Videoshare etc.
     */
    public enum Direction {

        /**
         * Incoming communication
         */
        INCOMING(0),

        /**
         * Outgoing communication
         */
        OUTGOING(1),

        /**
         * Irrelevant or not applicable (e.g. for group chat event message)
         */
        IRRELEVANT(2);

        private final int mValue;

        private static SparseArray<Direction> mValueToEnum = new SparseArray<Direction>();
        static {
            for (Direction entry : Direction.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        private Direction(int value) {
            mValue = value;
        }

        /**
         * Gets integer value associated to Direction instance
         * 
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * Returns a Direction instance for the specified integer value.
         * 
         * @param value
         * @return instance
         */
        public final static Direction valueOf(int value) {
            Direction entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException(new StringBuilder("No enum const class ")
                    .append(Direction.class.getName()).append(".").append(value).append("!")
                    .toString());
        }
    }

    /**
     * Read status of the message
     */
    public enum ReadStatus {
        /**
         * The message has not yet been displayed in the UI.
         */
        UNREAD(0),
        /**
         * The message has been displayed in the UI.
         */
        READ(1);

        private final int mValue;

        private static SparseArray<ReadStatus> mValueToEnum = new SparseArray<ReadStatus>();
        static {
            for (ReadStatus entry : ReadStatus.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        private ReadStatus(int value) {
            mValue = value;
        }

        /**
         * Gets integer value associated to ReadStatus instance
         * 
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * Returns a ReadStatus instance for the specified integer value.
         * 
         * @param value
         * @return instance
         */
        public final static ReadStatus valueOf(int value) {
            ReadStatus entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException(new StringBuilder("No enum const class ")
                    .append(ReadStatus.class.getName()).append(".").append(value).append("!")
                    .toString());
        }
    }

    /**
     * Application context
     */
    protected Context mCtx;

    /**
     * Service listener
     */
    protected RcsServiceListener mListener;

    /**
     * API interface
     */
    private IInterface mApi;

    /**
     * Service version
     */
    private Integer mVersion;

    /**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public RcsService(Context ctx, RcsServiceListener listener) {
        mCtx = ctx;
        mListener = listener;
        mRcsServiceControl = RcsServiceControl.getInstance(ctx);
    }

    /**
     * Call specific method on the API interface
     * 
     * @param method Method to be called
     * @param param Parameters of the method
     * @param paramClass Class of the parameter passed
     * @return Object
     * @throws RcsServiceException
     */
    private Object callApiMethod(String method, Object param,
            Class<IRcsServiceRegistrationListener> paramClass) throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        Class<? extends IInterface> c = mApi.getClass();
        try {
            if (param != null) {
                Method m = c.getDeclaredMethod(method, paramClass);
                return m.invoke(mApi, param);
            } else {
                Method m = c.getDeclaredMethod(method, (Class[]) null);
                return m.invoke(mApi);
            }
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Set API interface
     * 
     * @param api API interface
     */
    protected void setApi(IInterface api) {
        mApi = api;
    }

    /**
     * Connects to the API
     * 
     * @throws RcsPermissionDeniedException
     */
    public abstract void connect() throws RcsPermissionDeniedException;

    /**
     * Disconnects from the API
     */
    public abstract void disconnect();

    /**
     * Returns true if the service is connected, else returns false
     * 
     * @return Returns true if connected else returns false
     */
    public boolean isServiceConnected() {
        return (mApi != null);
    }

    /**
     * Returns service version
     * 
     * @return Version
     * @see Build.VERSION_CODES
     * @throws RcsServiceException
     */
    public int getServiceVersion() throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        if (mVersion != null) {
            return mVersion;
        }
        try {
            mVersion = (Integer) callApiMethod("getServiceVersion", null, null);
            return mVersion;
        } catch (Exception e) {
            throw new RcsServiceException(e.getMessage());
        }
    }

    /**
     * Returns true if the service is registered to the platform, else returns false
     * 
     * @return Returns true if registered else returns false
     * @throws RcsServiceException
     */
    public boolean isServiceRegistered() throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
        try {
            return (Boolean) callApiMethod("isServiceRegistered", null, null);
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Returns the reason code for the service registration
     * 
     * @return RcsServiceRegistration.ReasonCode
     * @throws RcsServiceException
     */
    public RcsServiceRegistration.ReasonCode getServiceRegistrationReasonCode()
            throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
        try {
            int reasonCode = (Integer) callApiMethod("getServiceRegistrationReasonCode", null, null);
            return RcsServiceRegistration.ReasonCode.valueOf(reasonCode);
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Adds a listener on service registration events
     * 
     * @param listener Service registration listener
     * @throws RcsServiceException
     */
    public void addEventListener(RcsServiceRegistrationListener listener)
            throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
        try {
            IRcsServiceRegistrationListener rcsListener = new RcsServiceRegistrationListenerImpl(
                    listener);
            mRegistrationListeners.put(listener,
                    new WeakReference<IRcsServiceRegistrationListener>(rcsListener));
            callApiMethod("addEventListener", rcsListener, IRcsServiceRegistrationListener.class);
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Removes a listener on service registration events
     * 
     * @param listener Service registration listener
     * @throws RcsServiceException
     */
    public void removeEventListener(RcsServiceRegistrationListener listener)
            throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
        try {
            WeakReference<IRcsServiceRegistrationListener> weakRef = mRegistrationListeners
                    .remove(listener);
            if (weakRef == null) {
                return;
            }
            IRcsServiceRegistrationListener rcsListener = weakRef.get();
            if (rcsListener != null) {
                callApiMethod("removeEventListener", rcsListener,
                        IRcsServiceRegistrationListener.class);
            }
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }

    /**
     * Returns the configuration that is common for all the service APIs
     * 
     * @return the CommonServiceConfiguration instance
     * @throws RcsServiceException
     */
    public CommonServiceConfiguration getCommonConfiguration() throws RcsServiceException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException(ERROR_CNX);
        }
        try {
            ICommonServiceConfiguration configuration = (ICommonServiceConfiguration) callApiMethod(
                    "getCommonConfiguration", null, null);
            return new CommonServiceConfiguration(configuration);
        } catch (Exception e) {
            throw new RcsServiceException(e);
        }
    }
}
