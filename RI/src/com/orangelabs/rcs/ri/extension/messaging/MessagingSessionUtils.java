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

package com.orangelabs.rcs.ri.extension.messaging;

import com.gsma.services.rcs.RcsServiceException;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Messaging service utils
 * 
 * @author Jean-Marc AUFFRET
 */
public class MessagingSessionUtils {
    /**
     * Service ID constant
     */
    final static String SERVICE_ID = "ext.messaging";

    /**
     * Get serviceIds from capabilities if available. Otherwise return a default serviceId.
     * 
     * @param context
     * @return String[]
     */
    public static String[] getServicesIds(Context context) {
        List<String> serviceIds = new ArrayList<String>();
        serviceIds.add(SERVICE_ID);
        try {
            ConnectionManager connectionManager = ConnectionManager.getInstance();
            if (connectionManager != null
                    && connectionManager.isServiceConnected(RcsServiceName.CAPABILITY)) {
                Set<String> extensions = connectionManager.getCapabilityApi().getMyCapabilities()
                        .getSupportedExtensions();
                if (!extensions.isEmpty()) {
                    serviceIds.addAll(extensions);
                }
            }
        } catch (RcsServiceException e1) {
            e1.printStackTrace();
        }
        return serviceIds.toArray(new String[serviceIds.size()]);
    }

}
