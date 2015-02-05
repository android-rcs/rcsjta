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

package com.orangelabs.rcs.service.api;

import android.os.RemoteException;

/**
 * Server API exception
 * 
 * @author Jean-Marc AUFFRET
 */
public class ServerApiException extends RemoteException {
    static final long serialVersionUID = 1L;

    /**
     * Constructor
     * 
     * @param e Exception
     */
    public ServerApiException(Exception e) {
        setStackTrace(e.getStackTrace());
    }

    /**
     * Constructor
     * 
     * @param error Error message
     */
    public ServerApiException(String error) {
        Exception e = new Exception(error);
        this.setStackTrace(e.getStackTrace());
    }
}
