/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.service.api;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.ims.network.ImsNetworkInterface;
import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.core.ims.service.extension.ExtensionManager;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsServiceRegistration;

import java.util.List;

/**
 * Server API utils
 * 
 * @author Jean-Marc AUFFRET
 */
public class ServerApiUtils {

    private final static Logger logger = Logger.getLogger(ServerApiUtils.class.getSimpleName());

    /**
     * Singleton of ServerApiUtils singleton
     */
    private static volatile ServerApiUtils sInstance;

    private final ExtensionManager mExtensionManager;

    private ServerApiUtils(ExtensionManager extensionManager) {
        mExtensionManager = extensionManager;
    }

    /**
     * Gets the instance of ServerApiUtils.
     * 
     * @param extensionManager
     * @return instance of ServerApiUtils singleton
     */
    public static ServerApiUtils getInstance(ExtensionManager extensionManager) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (ServerApiUtils.class) {
            if (sInstance == null) {
                sInstance = new ServerApiUtils(extensionManager);
            }
        }
        return sInstance;
    }

    /**
     * Test core
     */
    public void testCore() {
        if (Core.getInstance() == null) {
            throw new ServerApiGenericException("Core is not instanciated");
        }
    }

    /**
     * Test IMS connection
     */
    public void testIms() {
        if (!isImsConnected()) {
            throw new ServerApiServiceNotRegisteredException("Core is not connected to IMS");
        }
    }

    /**
     * Is connected to IMS
     * 
     * @return Boolean
     */
    public boolean isImsConnected() {
        Core core = Core.getInstance();
        if (core == null) {
            return false;
        }
        ImsNetworkInterface networkInterface = core.getImsModule().getCurrentNetworkInterface();
        if (networkInterface == null) {
            return false;
        }
        return networkInterface.isRegistered();
    }

    /**
     * Gets the reason code for IMS service registration
     * 
     * @return reason code
     */
    public RcsServiceRegistration.ReasonCode getServiceRegistrationReasonCode() {
        Core core = Core.getInstance();
        if (core == null) {
            return RcsServiceRegistration.ReasonCode.UNSPECIFIED;
        }
        ImsNetworkInterface networkInterface = core.getImsModule().getCurrentNetworkInterface();
        if (networkInterface == null) {
            return RcsServiceRegistration.ReasonCode.UNSPECIFIED;
        }
        return networkInterface.getRegistrationReasonCode();
    }

    /**
     * Checks if extension is authorized for an application. Application is identified by its uid
     * 
     * @param packageUid
     * @param serviceId
     * @throws ServerApiPermissionDeniedException
     */
    public void assertExtensionIsAuthorized(Integer packageUid, String serviceId)
            throws ServerApiPermissionDeniedException {
        if (mExtensionManager.isNativeApplication(packageUid)) {
            if (logger.isActivated()) {
                logger.info("assertExtensionIsAuthorized : no control for native application");
            }
            return;
        }
        mExtensionManager.testServicePermission(packageUid, serviceId);
    }

    /**
     * Checks if API access is authorized for an application. Application is identified by its uid
     * 
     * @param packageUid
     * @throws ServerApiPermissionDeniedException
     */
    public void assertApiIsAuthorized(Integer packageUid) throws ServerApiPermissionDeniedException {
        if (mExtensionManager.isNativeApplication(packageUid)) {
            if (logger.isActivated()) {
                logger.info("assertApiIsAuthorized : no control for native application");
            }
            return;
        }
        mExtensionManager.testApiPermission(packageUid);
    }

    /**
     * Add IARI (application Identifier) as features tag in IMS session for third party application
     * 
     * @param featureTags
     * @param callingUid
     */
    public void addApplicationIdAsFeaturesTag(List<String> featureTags, Integer callingUid) {
        boolean isActivated = logger.isActivated();
        if (isActivated) {
            logger.debug("addApplicationIdAsFeaturesTag , callingUid : ".concat(String
                    .valueOf(callingUid)));
        }

        if (mExtensionManager.isNativeApplication(callingUid)) {
            if (isActivated) {
                logger.debug("   --> no control for native application");
            }
            return;
        }

        String iari = mExtensionManager.getApplicationId(callingUid);
        if (iari == null) {
            if (isActivated) {
                logger.debug(" --> no authorization found");
            }
            return;
        }
        iari = new StringBuilder(FeatureTags.FEATURE_RCSE_EXTENSION).append(".").append(iari)
                .toString();

        for (int i = 0; i < featureTags.size(); i++) {
            if (featureTags.get(i).startsWith(FeatureTags.FEATURE_RCSE)) {
                String featureTag = featureTags.get(i);
                featureTags.set(
                        i,
                        new StringBuilder(featureTag).insert(featureTag.length() - 1,
                                ",".concat(iari)).toString());
                return;
            }
        }

        String appRef = new StringBuilder(FeatureTags.FEATURE_RCSE).append("=\"").append(iari)
                .append("\"").toString();

        if (isActivated) {
            logger.debug(" --> iari : ".concat(appRef));
        }
        featureTags.add(appRef);
    }
}
