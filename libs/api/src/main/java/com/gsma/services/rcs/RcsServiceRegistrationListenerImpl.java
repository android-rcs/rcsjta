/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

import android.util.Log;

/**
 * RCS service registration listener implementation
 * 
 * @hide
 */
public class RcsServiceRegistrationListenerImpl extends IRcsServiceRegistrationListener.Stub {

    private final RcsServiceRegistrationListener mListener;

    private final static String LOG_TAG = RcsServiceRegistrationListenerImpl.class.getName();

    RcsServiceRegistrationListenerImpl(RcsServiceRegistrationListener listener) {
        mListener = listener;
    }

    @Override
    public void onServiceRegistered() {
        mListener.onServiceRegistered();
    }

    @Override
    public void onServiceUnregistered(int reasonCode) {
        RcsServiceRegistration.ReasonCode rcsReasonCode;
        try {
            rcsReasonCode = RcsServiceRegistration.ReasonCode.valueOf(reasonCode);
        } catch (IllegalArgumentException e) {
            /*
             * Detected unknown state or reasonCode not part of standard coming from stack which a
             * client application can not handle since it is built only to handle the possible enum
             * values documented and specified in the api standard.
             */
            Log.e(LOG_TAG, e.getMessage());
            return;
        }

        mListener.onServiceUnregistered(rcsReasonCode);
    }
}
