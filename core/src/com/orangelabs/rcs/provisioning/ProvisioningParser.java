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

package com.orangelabs.rcs.provisioning;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax2.sip.ListeningPoint;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.settings.RcsSettingsData;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Provisioning parser
 *
 * @author jexa7410
 */
public class ProvisioningParser {
    /**
     * Provisioning info
     */
    private ProvisioningInfo provisioningInfo = new ProvisioningInfo();

    /**
     * Content
     */
    private String content;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     *
     * @param content Content
     */
    public ProvisioningParser(String content) {
        this.content = content;
    }

    /**
     * Returns provisioning info
     *
     * @return Provisioning info
     */
    public ProvisioningInfo getProvisioningInfo() {
        return provisioningInfo;
    }

    /**
     * Parse the provisioning document
     *
     * @return Boolean result
     */
    public boolean parse() {
        try {
            if (logger.isActivated()) {
                logger.debug("Start the parsing of content");
            }
            ByteArrayInputStream mInputStream = new ByteArrayInputStream(content.getBytes());
            DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dbuilder = dfactory.newDocumentBuilder();
            Document doc = dbuilder.parse(mInputStream);
            mInputStream.close();
            mInputStream = null;
            if (doc == null) {
                if (logger.isActivated()) {
                    logger.debug("The document is null");
                }
                return false;
            }

            Node rootnode = doc.getDocumentElement();
            Node childnode = rootnode.getFirstChild();
            if (childnode == null) {
                if (logger.isActivated()) {
                    logger.debug("The first chid node is null");
                }
                return false;
            }

            do {
                if (childnode.getNodeName().equals("characteristic")) {
                    if (childnode.getAttributes().getLength() > 0) {
                        Node typenode = childnode.getAttributes().getNamedItem("type");
                        if (typenode != null) {
                            if (logger.isActivated()) {
                                logger.debug("Node " + childnode.getNodeName() + " with type "
                                        + typenode.getNodeValue());
                            }
                            if (typenode.getNodeValue().equalsIgnoreCase("VERS")) {
                                parseVersion(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("MSG")) {
                                parseTermsMessage(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("APPLICATION")) {
                                parseApplication(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("IMS")) {
                                parseIMS(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("PRESENCE")) {
                                parsePresence(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("XDMS")) {
                                parseXDMS(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("IM")) {
                                parseIM(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("CAPDISCOVERY")) {
                                parseCapabilityDiscovery(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("APN")) {
                                parseAPN(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("OTHER")) {
                                parseOther(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("SERVICES")) {
                                parseServices(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("SUPL")) {
                                parseSupl(childnode);
                            }
                        }
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
            return true;
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Can't parse content", e);
            }
            return false;
        }
    }

    /**
     * Parse the provisioning version
     *
     * @param node Node
     */
    private void parseVersion(Node node) {
        String version = null;
        String validity = null;
        if (node == null) {
            return;
        }
        Node versionchild = node.getFirstChild();

        if (versionchild != null) {
            do {
                if (version == null) {
                    if ((version = getValueByParamName("version", versionchild)) != null) {
                        provisioningInfo.setVersion(version);
                        continue;
                    }
                }
                if (validity == null) {
                    if ((validity = getValueByParamName("validity", versionchild)) != null) {
                        provisioningInfo.setValidity(Long.parseLong(validity));
                        continue;
                    }
                }
            } while ((versionchild = versionchild.getNextSibling()) != null);
        }
    }

    /**
     * Parse terms message
     *
     * @param node Node
     */
    private void parseTermsMessage(Node node) {
        String title = null;
        String message = null;
        String acceptBtn = null;
        String rejectBtn = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (title == null) {
                    if ((title = getValueByParamName("title", childnode)) != null) {
                        provisioningInfo.setTitle(title);
                        continue;
                    }
                }
                if (message == null) {
                    if ((message = getValueByParamName("message", childnode)) != null) {
                        provisioningInfo.setMessage(message);
                        continue;
                    }
                }
                if (acceptBtn == null) {
                    if ((acceptBtn = getValueByParamName("Accept_btn", childnode)) != null) {
                        if (acceptBtn.equals("1")) {
                            provisioningInfo.setAcceptBtn(true);
                        } else {
                            provisioningInfo.setAcceptBtn(false);
                        }
                        continue;
                    }
                }
                if (rejectBtn == null) {
                    if ((rejectBtn = getValueByParamName("Reject_btn", childnode)) != null) {
                        if (rejectBtn.equals("1")) {
                            provisioningInfo.setRejectBtn(true);
                        } else {
                            provisioningInfo.setRejectBtn(false);
                        }
                        continue;
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse the application infos
     *
     * @param node Node
     */
    private void parseApplication(Node node) {
        String appId = null;
        String name = null;
        String appRef = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (appId == null) {
                    if ((appId = getValueByParamName("AppID", childnode)) != null) {
                        if (logger.isActivated()) {
                            logger.debug("App ID: " + appId);
                        }
                        continue;
                    }
                }
                if (name == null) {
                    if ((name = getValueByParamName("Name", childnode)) != null) {
                        if (logger.isActivated()) {
                            logger.debug("App name: " + name);
                        }
                        continue;
                    }
                }
                if (appRef == null) {
                    if ((appRef = getValueByParamName("AppRef", childnode)) != null) {
                        if (logger.isActivated()) {
                            logger.debug("App ref: " + appRef);
                        }
                        continue;
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }

        if (appRef != null && (appRef.equalsIgnoreCase("IMS-Settings") || appRef.equalsIgnoreCase("ims-rcse"))) {
            parseIMS(node);
        }

        if (appRef != null && appRef.equalsIgnoreCase("RCSe-Settings")) {
            parseRCSe(node);
        }
    }

    /**
     * Parse presence favorite link
     *
     * @param node Node
     */
    private void parseFavoriteLink(Node node) {
        if (node == null) {
            return;
        }
        // Not supported
    }

    /**
     * Parse presence watcher
     *
     * @param node Node
     */
    private void parsePresenceWatcher(Node node) {
        String fetchAuth = null;
        String contactCapPresAuth = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (fetchAuth == null) {
                    if ((fetchAuth = getValueByParamName("FetchAuth", childnode)) != null) {
                        // TODO
                        continue;
                    }
                }

                if (contactCapPresAuth == null) {
                    if ((contactCapPresAuth = getValueByParamName("ContactCapPresAut", childnode)) != null) {
                        // TODO
                        continue;
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse presentity watcher
     * 
     * @param node Node
     */
    private void parsePresentityWatcher(Node node) {
        String watcherFetchAuth = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (watcherFetchAuth == null) {
                    if ((watcherFetchAuth = getValueByParamName("WATCHERFETCHAUTH", childnode)) != null) {
                        // TODO
                        continue;
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse presence
     *
     * @param node Node
     */
    private void parsePresence(Node node) {
        String usePresence = null;
        String presencePrfl = null;
        String availabilityAuth = null;
        String iconMaxSize = null;
        String noteMaxSize = null;
        String publishTimer = null;
        String clientObjDataLimit = null;
        String contentServerUri = null;
        String sourceThrottlePublish = null;
        String maxSubscriptionsPresenceList = null;
        String serviceUriTemplate = null;
        Node typenode = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (childnode.getNodeName().equals("characteristic")) {
                    if (childnode.getAttributes().getLength() > 0) {
                        typenode = childnode.getAttributes().getNamedItem("type");
                        if (typenode != null) {
                            if (logger.isActivated()) {
                                logger.debug("Node " + childnode.getNodeName() + " with type "
                                        + typenode.getNodeValue());
                            }
                            if (typenode.getNodeValue().equalsIgnoreCase("FAVLINK")) {
                                parseFavoriteLink(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("SERVCAPWATCH")) {
                                parsePresenceWatcher(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("ServCapPresentity")) {
                                parsePresentityWatcher(childnode);
                            }
                        }
                    }
                }

                if (usePresence == null) {
                    if ((usePresence = getValueByParamName("usePresence", childnode)) != null) {
                        if (usePresence.equals("0")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE,
                                    RcsSettingsData.FALSE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE,
                                    RcsSettingsData.TRUE);
                        }
                        continue;
                    }
                }

                if (presencePrfl == null) {
                    if ((presencePrfl = getValueByParamName("presencePrfl", childnode)) != null) {
                        if (presencePrfl.equals("0")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY,
                                    RcsSettingsData.FALSE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY,
                                    RcsSettingsData.TRUE);
                        }
                        continue;
                    }
                }

                if (availabilityAuth == null) {
                    if ((availabilityAuth = getValueByParamName("AvailabilityAuth", childnode)) != null) {
                        // Not supported
                        continue;
                    }
                }

                if (iconMaxSize == null) {
                    if ((iconMaxSize = getValueByParamName("IconMaxSize", childnode)) != null) {
        				long kb = Long.parseLong(iconMaxSize) / 1024;
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.MAX_PHOTO_ICON_SIZE, ""+kb);
                        continue;
                    }
                }

                if (noteMaxSize == null) {
                    if ((noteMaxSize = getValueByParamName("NoteMaxSize", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.MAX_FREETXT_LENGTH, noteMaxSize);
                        continue;
                    }
                }

                if (publishTimer == null) {
                    if ((publishTimer = getValueByParamName("PublishTimer", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.PUBLISH_EXPIRE_PERIOD, publishTimer);
                        continue;
                    }
                }

                if (clientObjDataLimit == null) {
                    if ((clientObjDataLimit = getValueByParamName("client-obj-datalimit",
                            childnode)) != null) {
                        // Not supported
                        continue;
                    }
                }

                if (contentServerUri == null) {
                    if ((contentServerUri = getValueByParamName("content-serveruri", childnode)) != null) {
                        // Not used for RCS
                        continue;
                    }
                }

                if (sourceThrottlePublish == null) {
                    if ((sourceThrottlePublish = getValueByParamName("source-throttlepublish",
                            childnode)) != null) {
                        // Not supported
                        continue;
                    }
                }

                if (maxSubscriptionsPresenceList == null) {
                    if ((maxSubscriptionsPresenceList = getValueByParamName(
                            "max-number-ofsubscriptions-inpresence-list", childnode)) != null) {
                        // Not supported
                        continue;
                    }
                }

                if (serviceUriTemplate == null) {
                    if ((serviceUriTemplate = getValueByParamName("service-uritemplate",
                            childnode)) != null) {
                        // TODO
                        continue;
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse services
     *
     * @param node Node
     */
    private void parseServices(Node node) {
        String presencePrfl = null;
        String chatAuth = null;        
        String ftAuth = null;
        String standaloneMsgAuth = null;
        String geolocPullAuth = null;
        String geolocPushAuth = null;
        String vsAuth = null;
        String isAuth = null;
        String beIPVoiceCallAuth = null;
        String beIPVideoCallAuth = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {

                if (chatAuth == null) {
                    if ((chatAuth = getValueByParamName("ChatAuth", childnode)) != null) {
                        if (chatAuth.equals("1")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.CAPABILITY_IM_SESSION, RcsSettingsData.TRUE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.CAPABILITY_IM_SESSION, RcsSettingsData.FALSE);
                        }
                        continue;
                    }
                }

                if (ftAuth == null) {
                    if ((ftAuth = getValueByParamName("ftAuth", childnode)) != null) {
                        if (ftAuth.equals("1")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.CAPABILITY_FILE_TRANSFER, RcsSettingsData.TRUE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                            		RcsSettingsData.CAPABILITY_FILE_TRANSFER, RcsSettingsData.FALSE);
                        }
                        continue;
                    }
                }

                if (vsAuth == null) {
                    if ((vsAuth = getValueByParamName("vsAuth", childnode)) != null) {
                        if (vsAuth.equals("1")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.CAPABILITY_VIDEO_SHARING, RcsSettingsData.TRUE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                            		RcsSettingsData.CAPABILITY_VIDEO_SHARING, RcsSettingsData.FALSE);
                        }
                        continue;
                    }
                }

                if (isAuth == null) {
                    if ((isAuth = getValueByParamName("isAuth", childnode)) != null) {
                        if (isAuth.equals("1")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.CAPABILITY_IMAGE_SHARING, RcsSettingsData.TRUE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                            		RcsSettingsData.CAPABILITY_IMAGE_SHARING, RcsSettingsData.FALSE);
                        }
                        continue;
                    }
                }
                
                if (geolocPushAuth == null) {
                    if ((geolocPushAuth = getValueByParamName("geolocPushAuth", childnode)) != null) {
                        if (geolocPushAuth.equals("1")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.CAPABILITY_GEOLOCATION_PUSH, RcsSettingsData.TRUE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                            		RcsSettingsData.CAPABILITY_GEOLOCATION_PUSH, RcsSettingsData.FALSE);
                        }
                        continue;
                    }
                }
                
                if (standaloneMsgAuth == null) {
                    if ((standaloneMsgAuth = getValueByParamName("standaloneMsgAuth", childnode)) != null) {
                    	// Not used
                        continue;
                    }
                }
                
                if (presencePrfl == null) {
                    if ((presencePrfl = getValueByParamName("presencePrfl", childnode)) != null) {
                        if (presencePrfl.equals("1")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE, RcsSettingsData.TRUE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                            		RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE, RcsSettingsData.FALSE);
                        }
                        continue;
                    }
                }
                
                if (geolocPullAuth == null) {
                    if ((geolocPullAuth = getValueByParamName("geolocPullAuth", childnode)) != null) {
                        // Not used
                        continue;
                    }
                }
                
                if (beIPVoiceCallAuth == null) {
                    if ((beIPVoiceCallAuth = getValueByParamName("beIPVoiceCallAuth", childnode)) != null) {
                        // TODO
                        continue;
                    }
                }
                
                if (beIPVideoCallAuth == null) {
                    if ((beIPVideoCallAuth = getValueByParamName("beIPVideoCallAuth", childnode)) != null) {
                        // TODO
                        continue;
                    }
                }
                
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }
    
    /**
     * Parse XDMS
     *
     * @param node Node
     */
    private void parseXDMS(Node node) {
        String revokeTimer = null;
        String xcapRootURI = null;
        String xcapAuthenticationUsername = null;
        String xcapAuthenticationSecret = null;
        String xcapAuthenticationType = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (revokeTimer == null) {
                    if ((revokeTimer = getValueByParamName("RevokeTimer", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(RcsSettingsData.REVOKE_TIMEOUT,
                                revokeTimer);
                        continue;
                    }
                }

                if (xcapRootURI == null) {
                    if ((xcapRootURI = getValueByParamName("XCAPRootURI", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(RcsSettingsData.XDM_SERVER, xcapRootURI);
                        continue;
                    }
                }

                if (xcapAuthenticationUsername == null) {
                    if ((xcapAuthenticationUsername = getValueByParamName(
                            "XCAPAuthenticationUserName", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.XDM_LOGIN,
                                xcapAuthenticationUsername);
                        continue;
                    }
                }

                if (xcapAuthenticationSecret == null) {
                    if ((xcapAuthenticationSecret = getValueByParamName(
                            "XCAPAuthenticationSecret", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.XDM_PASSWORD,
                                xcapAuthenticationSecret);
                        continue;
                    }
                }

                if (xcapAuthenticationType == null) {
                    if ((xcapAuthenticationType = getValueByParamName("XCAPAuthenticationType",
                            childnode)) != null) {
                        // Not used (only Digest is used)
                        continue;
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse supl services
     *
     * @param node Node
     */
    private void parseSupl(Node node) {
        String textMaxLength = null;
        String locInfoMaxValidTime = null;
        String geolocPullOpenValue = null;
        String geolocPullApiGwAddress = null;
        String geolocPullBlockTimer = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (textMaxLength == null) {
                    if ((textMaxLength = getValueByParamName("TextMaxLength", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(RcsSettingsData.MAX_GEOLOC_LABEL_LENGTH,
                        		textMaxLength);
                        continue;
                    }
                }

                if (locInfoMaxValidTime == null) {
                    if ((locInfoMaxValidTime = getValueByParamName("LocInfoMaxValidTime", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(RcsSettingsData.GEOLOC_EXPIRATION_TIME, locInfoMaxValidTime);
                        continue;
                    }
                }

                if (geolocPullOpenValue == null) {
                    if ((geolocPullOpenValue = getValueByParamName("geolocPullOpenValue", childnode)) != null) {
                    	// Not used
                        continue;
                    }
                }

                if (geolocPullApiGwAddress == null) {
                    if ((geolocPullApiGwAddress = getValueByParamName("geolocPullApiGwAddress", childnode)) != null) {
                    	// Not used
                        continue;
                    }
                }

                if (geolocPullBlockTimer == null) {
                    if ((geolocPullBlockTimer = getValueByParamName("geolocPullBlockTimer", childnode)) != null) {
                        // Not used 
                        continue;
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }
    
    /**
     * Parse IM
     *
     * @param node Node
     */
    private void parseIM(Node node) {
        String imCapAlwaysOn = null;
        String imWarnSF = null;
        String imSessionStart = null;
        String ftWarnSize = null;
        String autoAcceptFt = null;
        String chatAuth = null;
        String smsFallBackAuth = null;
        String autoAcceptChat = null;
        String autoAcceptGroupChat = null;
        String maxSize1to1 = null;
        String maxSize1toM = null;
        String timerIdle = null;
        String maxSizeFileTransfer = null;
        String ftThumb = null;
        String presSrvCap = null;
        String deferredMsgFuncUri = null;
        String maxAdhocGroupSize = null;
        String confFctyUri = null;
        String exploderUri = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (imCapAlwaysOn == null) {
                    if ((imCapAlwaysOn = getValueByParamName("imCapAlwaysON", childnode)) != null) {
                        if (imCapAlwaysOn.equals("0")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.IM_CAPABILITY_ALWAYS_ON,
                                    RcsSettingsData.FALSE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.IM_CAPABILITY_ALWAYS_ON,
                                    RcsSettingsData.TRUE);
                        }
                        continue;
                    }
                }

                if (imWarnSF == null) {
                    if ((imWarnSF = getValueByParamName("imWarnSF", childnode)) != null) {
                        if (imWarnSF.equals("0")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.WARN_SF_SERVICE,
                                    RcsSettingsData.FALSE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.WARN_SF_SERVICE,
                                    RcsSettingsData.TRUE);
                        }
                        continue;
                    }
                }

                if (autoAcceptFt == null) {
                    if ((autoAcceptFt = getValueByParamName("ftAutAccept", childnode)) != null) {
                        if (autoAcceptFt.equals("0")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.AUTO_ACCEPT_FILE_TRANSFER,
                                    RcsSettingsData.FALSE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.AUTO_ACCEPT_FILE_TRANSFER,
                                    RcsSettingsData.TRUE);
                        }
                        continue;
                    }
                }

                if (imSessionStart == null) {
                    if ((imSessionStart = getValueByParamName("imSessionStart", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(RcsSettingsData.IM_SESSION_START,
                                imSessionStart);
                        continue;
                    }
                }

                if (ftWarnSize == null) {
                    if ((ftWarnSize = getValueByParamName("ftWarnSize", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.WARN_FILE_TRANSFER_SIZE, ftWarnSize);
                        continue;
                    }
                }

                if (chatAuth == null) {
                    if ((chatAuth = getValueByParamName("ChatAuth", childnode)) != null) {
                        if (chatAuth.equals("0")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.CAPABILITY_IM_SESSION, RcsSettingsData.FALSE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.CAPABILITY_IM_SESSION, RcsSettingsData.TRUE);
                        }
                        continue;
                    }
                }

                if (smsFallBackAuth == null) {
                    if ((smsFallBackAuth = getValueByParamName("SmsFallBackAuth", childnode)) != null) {
                        if (smsFallBackAuth.equals("0")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.SMS_FALLBACK_SERVICE, RcsSettingsData.TRUE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.SMS_FALLBACK_SERVICE, RcsSettingsData.FALSE);
                        }
                        continue;
                    }
                }

                if (autoAcceptChat == null) {
                    if ((autoAcceptChat = getValueByParamName("AutAccept", childnode)) != null) {
                        if (autoAcceptChat.equals("0")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.AUTO_ACCEPT_CHAT,
                                    RcsSettingsData.FALSE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.AUTO_ACCEPT_CHAT,
                                    RcsSettingsData.TRUE);
                        }
                        continue;
                    }
                }

                if (autoAcceptGroupChat == null) {
                    if ((autoAcceptGroupChat = getValueByParamName("AutAcceptGroupChat", childnode)) != null) {
                        if (autoAcceptGroupChat.equals("0")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.AUTO_ACCEPT_GROUP_CHAT,
                                    RcsSettingsData.FALSE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.AUTO_ACCEPT_GROUP_CHAT,
                                    RcsSettingsData.TRUE);
                        }
                        continue;
                    }
                }

                if (maxSize1to1 == null) {
                    if ((maxSize1to1 = getValueByParamName("MaxSize1to1", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.MAX_CHAT_MSG_LENGTH, maxSize1to1);
                        continue;
                    }
                }

                if (maxSize1toM == null) {
                    if ((maxSize1toM = getValueByParamName("MaxSize1toM", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.MAX_GROUPCHAT_MSG_LENGTH, maxSize1toM);
                        continue;
                    }
                }

                if (timerIdle == null) {
                    if ((timerIdle = getValueByParamName("TimerIdle", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.CHAT_IDLE_DURATION, timerIdle);
                        continue;
                    }
                }

                if (maxSizeFileTransfer == null) {
                    if ((maxSizeFileTransfer = getValueByParamName("MaxSizeFileTr", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.MAX_FILE_TRANSFER_SIZE, maxSizeFileTransfer);
                        continue;
                    }
                }

                if (ftThumb == null) {
                    if ((ftThumb = getValueByParamName("ftThumb", childnode)) != null) {
                        if (ftThumb.equals("0")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.CAPABILITY_FILE_TRANSFER_THUMBNAIL, RcsSettingsData.FALSE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.CAPABILITY_FILE_TRANSFER_THUMBNAIL, RcsSettingsData.TRUE);
                        }
                        continue;
                    }
                }
                
                if (presSrvCap == null) {
                    if ((presSrvCap = getValueByParamName("pres-srv-cap", childnode)) != null) {
                        // Not used for RCS
                        continue;
                    }
                }

                if (deferredMsgFuncUri == null) {
                    if ((deferredMsgFuncUri = getValueByParamName("deferred-msg-func-uri",
                            childnode)) != null) {
                        // Not used for RCS
                        continue;
                    }
                }

                if (maxAdhocGroupSize == null) {
                    if ((maxAdhocGroupSize = getValueByParamName("max_adhoc_group_size",
                            childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.MAX_CHAT_PARTICIPANTS, maxAdhocGroupSize);
                        continue;
                    }
                }

                if (confFctyUri == null) {
                    if ((confFctyUri = getValueByParamName("conf-fcty-uri", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.IM_CONF_URI, formatSipUri(confFctyUri));
                        continue;
                    }
                }

                if (exploderUri == null) {
                    if ((exploderUri = getValueByParamName("exploder-uri", childnode)) != null) {
                        // Not used for RCS
                        continue;
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse capability discovery
     * 
     * @param node Node
     */
    private void parseCapabilityDiscovery(Node node) {
        String pollingPeriod = null;
        String capInfoExpiry = null;
        String presenceDiscovery = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (pollingPeriod == null) {
                    if ((pollingPeriod = getValueByParamName("pollingPeriod", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.CAPABILITY_POLLING_PERIOD, pollingPeriod);
                        continue;
                    }
                }

                if (capInfoExpiry == null) {
                    if ((capInfoExpiry = getValueByParamName("capInfoExpiry", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.CAPABILITY_EXPIRY_TIMEOUT, capInfoExpiry);
                        continue;
                    }
                }

                if (presenceDiscovery == null) {
                    if ((presenceDiscovery = getValueByParamName("presenceDisc", childnode)) != null) {
                        if (presenceDiscovery.equals("0")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY,
                                    RcsSettingsData.FALSE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY,
                                    RcsSettingsData.TRUE);
                        }
                        continue;
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse APN
     *
     * @param node Node
     */
    private void parseAPN(Node node) {
        String rcseApn = null;
        String enableRcseSwitch = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (rcseApn == null) {
                    if ((rcseApn = getValueByParamName("rcseOnlyAPN", childnode)) != null) {
                        // Not supported
                        continue;
                    }
                }

                if (enableRcseSwitch == null) {
                    if ((enableRcseSwitch = getValueByParamName("enableRcseSwitch", childnode)) != null) {
                        // Not supported
                        continue;
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse transport protocol
     *
     * @param node Node
     */
    private void parseTransportProtocol(Node node) {
        String psSignalling = null;
        String psMedia = null;
        String psRtMedia = null;
        String wifiSignalling = null;
        String wifiMedia = null;
        String wifiRtMedia = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (psSignalling == null) {
                    if ((psSignalling = getValueByParamName("psSignalling", childnode)) != null) {
                        if (psSignalling.equals("SIPoUDP")) {
	                        RcsSettings.getInstance().writeParameter(
	                        		RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_MOBILE,
	                                ListeningPoint.UDP);
                        } else if (psSignalling.equals("SIPoTCP")) {
	                        RcsSettings.getInstance().writeParameter(
	                        		RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_MOBILE,
	                                ListeningPoint.TCP);
                        } else if (psSignalling.equals("SIPoTLS")) {
	                        RcsSettings.getInstance().writeParameter(
	                        		RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_MOBILE,
	                                ListeningPoint.TLS);
                        }
                        continue;
                    }
                }

                if (psMedia == null) {
                    if ((psMedia = getValueByParamName("psMedia", childnode)) != null) {
                        // Not supported
                        continue;
                    }
                }

                if (psRtMedia == null) {
                    if ((psRtMedia = getValueByParamName("psRTMedia", childnode)) != null) {
                        // Not supported
                        continue;
                    }
                }

                if (wifiSignalling == null) {
                    if ((wifiSignalling = getValueByParamName("wifiSignalling", childnode)) != null) {
                        if (wifiSignalling.equals("SIPoUDP")) {
	                        RcsSettings.getInstance().writeParameter(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_WIFI,
	                                ListeningPoint.UDP);
                        } else if (wifiSignalling.equals("SIPoTCP")) {
	                        RcsSettings.getInstance().writeParameter(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_WIFI,
	                                ListeningPoint.TCP);
                        } else if (wifiSignalling.equals("SIPoTLS")) {
	                        RcsSettings.getInstance().writeParameter(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_WIFI,
	                                ListeningPoint.TLS);
                        }
                        continue;
                    }
                }

                if (wifiMedia == null) {
                    if ((wifiMedia = getValueByParamName("wifiMedia", childnode)) != null) {
                        if (wifiMedia.equals("MSRP")) {
	                        RcsSettings.getInstance().writeParameter(RcsSettingsData.SECURE_MSRP_OVER_WIFI,
	                        		RcsSettingsData.FALSE);
                        } else if (wifiMedia.equals("MSRPoTLS")) {
	                        RcsSettings.getInstance().writeParameter(RcsSettingsData.SECURE_MSRP_OVER_WIFI,
	                        		RcsSettingsData.TRUE);
                        }
                        continue;
                    }
                }

                if (wifiRtMedia == null) {
                    if ((wifiRtMedia = getValueByParamName("wifiRTMedia", childnode)) != null) {
                        if (wifiMedia.equals("RTP")) {
	                        RcsSettings.getInstance().writeParameter(RcsSettingsData.SECURE_RTP_OVER_WIFI,
	                        		RcsSettingsData.FALSE);
                        } else if (wifiMedia.equals("SRTP")) {
	                        RcsSettings.getInstance().writeParameter(RcsSettingsData.SECURE_RTP_OVER_WIFI,
	                        		RcsSettingsData.TRUE);
                        }
                        continue;
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse other
     *
     * @param node Node
     */
    private void parseOther(Node node) {
        String endUserConfReqId = null;
        String deviceID = null;
        String warnSizeImageShare = null;
        Node typenode = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (childnode.getNodeName().equals("characteristic")) {
                    if (childnode.getAttributes().getLength() > 0) {
                        typenode = childnode.getAttributes().getNamedItem("type");
                        if (typenode != null) {
                            if (logger.isActivated()) {
                                logger.debug("Node " + childnode.getNodeName() + " with type "
                                        + typenode.getNodeValue());
                            }
                            if (typenode.getNodeValue().equalsIgnoreCase("transportProto")) {
                                parseTransportProtocol(childnode);
                            }
                        }
                    }
                }

                if (endUserConfReqId == null) {
                    if ((endUserConfReqId = getValueByParamName("endUserConfReqId", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.ENDUSER_CONFIRMATION_URI, formatSipUri(endUserConfReqId));
                        continue;
                    }
                }

                if (deviceID == null) {
                    if ((deviceID = getValueByParamName("deviceID", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.USE_IMEI_AS_DEVICE_ID, deviceID.equals("0") ?
                                RcsSettingsData.TRUE : RcsSettingsData.FALSE);
                        continue;
                    }
                }

                if (warnSizeImageShare == null) {
                    if ((warnSizeImageShare = getValueByParamName("WarnSizeImageShare",
                            childnode)) != null) {
                        // Not supported
                        continue;
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse connection reference
     *
     * @param node Node
     */
    private void parseConRefs(Node node) {
        String conRef = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (conRef == null) {
                    if ((conRef = getValueByParamName("ConRef", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(RcsSettingsData.RCS_APN,
                                conRef);
                        continue;
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse public user identity
     *
     * @param node Node
     */
    private void parsePublicUserIdentity(Node node) {
        String publicUserIdentity = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (publicUserIdentity == null) {
                    if ((publicUserIdentity = getValueByParamName("Public_User_Identity",
                            childnode)) != null) {
                    	String username = extractUserNamePart(publicUserIdentity);
                    	RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.USERPROFILE_IMS_USERNAME, username);
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME, username);
                        continue;
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse the secondary device parameter
     *
     * @param node Node
     */
    private void parseSecondaryDevicePar(Node node) {
    	// TODO: to be managed thks to a flag main/secondary device
        /*String voiceCall = null;
        String chat = null;
        String sendSms = null;
        String fileTranfer = null;
        String videoShare = null;
        String imageShare = null;
        String geolocPush = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (voiceCall == null) {
                    if ((voiceCall = getValueByParamName("VoiceCall", childnode)) != null) {
                        // Not used for RCS
                        continue;
                    }
                }

                if (chat == null) {
                    if ((chat = getValueByParamName("Chat", childnode)) != null) {
                        if (chat.equals("0")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.CAPABILITY_IM_SESSION, RcsSettingsData.TRUE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.CAPABILITY_IM_SESSION, RcsSettingsData.FALSE);
                        }
                        continue;
                    }
                }

                if (sendSms == null) {
                    if ((sendSms = getValueByParamName("SendSms", childnode)) != null) {
                    	// Not used for RCS
                        continue;
                    }
                }

                if (fileTranfer == null) {
                    if ((fileTranfer = getValueByParamName("FileTranfer", childnode)) != null) {
                        if (fileTranfer.equals("0")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.CAPABILITY_FILE_TRANSFER, RcsSettingsData.TRUE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                            		RcsSettingsData.CAPABILITY_FILE_TRANSFER, RcsSettingsData.FALSE);
                        }
                        continue;
                    }
                }

                if (videoShare == null) {
                    if ((videoShare = getValueByParamName("VideoShare", childnode)) != null) {
                        if (videoShare.equals("0")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.CAPABILITY_VIDEO_SHARING, RcsSettingsData.TRUE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                            		RcsSettingsData.CAPABILITY_VIDEO_SHARING, RcsSettingsData.FALSE);
                        }
                        continue;
                    }
                }

                if (imageShare == null) {
                    if ((imageShare = getValueByParamName("ImageShare", childnode)) != null) {
                        if (imageShare.equals("0")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.CAPABILITY_IMAGE_SHARING, RcsSettingsData.TRUE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                            		RcsSettingsData.CAPABILITY_IMAGE_SHARING, RcsSettingsData.FALSE);
                        }
                        continue;
                    }
                }
                
                if (geolocPush == null) {
                    if ((geolocPush = getValueByParamName("GeoLocPush", childnode)) != null) {
                        if (geolocPush.equals("0")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.CAPABILITY_GEOLOCATION_PUSH, RcsSettingsData.TRUE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                            		RcsSettingsData.CAPABILITY_GEOLOCATION_PUSH, RcsSettingsData.FALSE);
                        }
                        continue;
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }*/
    }

    /**
     * Parse ext
     *
     * @param node Node
     */
    private void parseExt(Node node) {
        String natUrlFmt = null;
        String intUrlFmt = null;
        String qValue = null;
        String maxSizeImageShare = null;
        String maxTimeVideoShare = null;
        Node typenode = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (childnode.getNodeName().equals("characteristic")) {
                    if (childnode.getAttributes().getLength() > 0) {
                        typenode = childnode.getAttributes().getNamedItem("type");
                        if (typenode != null) {
                            if (logger.isActivated()) {
                                logger.debug("Node " + childnode.getNodeName() + " with type "
                                        + typenode.getNodeValue());
                            }
                            if (typenode.getNodeValue().equalsIgnoreCase("SecondaryDevicePar")) {
                                parseSecondaryDevicePar(childnode);
                            }
                        }
                    }
                }

                if (natUrlFmt == null) {
                    if ((natUrlFmt = getValueByParamName("NatUrlFmt", childnode)) != null) {
                        // Not used (all number are formatted in international format)
                        continue;
                    }
                }

                if (intUrlFmt == null) {
                    if ((intUrlFmt = getValueByParamName("IntUrlFmt", childnode)) != null) {
                        if (intUrlFmt.equals("0")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.TEL_URI_FORMAT, RcsSettingsData.TRUE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.TEL_URI_FORMAT, RcsSettingsData.FALSE);
                        }
                        continue;
                    }
                }

                if (qValue == null) {
                    if ((qValue = getValueByParamName("Q-Value", childnode)) != null) {
                        // Not supported
                        continue;
                    }
                }

                if (maxSizeImageShare == null) {
                    if ((maxSizeImageShare = getValueByParamName("MaxSizeImageShare", childnode)) != null) {
                    	long kb = Long.parseLong(maxSizeImageShare) / 1024;
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.MAX_IMAGE_SHARE_SIZE, ""+kb);
                        continue;
                    }
                }

                if (maxTimeVideoShare == null) {
                    if ((maxTimeVideoShare = getValueByParamName("MaxTimeVideoShare", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.MAX_VIDEO_SHARE_DURATION, maxTimeVideoShare);
                        continue;
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse ICSI
     *
     * @param node Node
     */
    private void parseICSI(Node node) {
        String icsi = null;
        String icsiResourceAllocationMode = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (icsi == null) {
                    if ((icsi = getValueByParamName("ICSI", childnode)) != null) {
                        // Not used for RCS
                        continue; 
                    }
                }

                if (icsiResourceAllocationMode == null) {
                    if ((icsiResourceAllocationMode = getValueByParamName(
                            "ICSI_Resource_Allocation_Mode", childnode)) != null) {
                        // Not used for RCS
                        continue;
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse PCSCF address
     *
     * @param node Node
     */
    private void parsePcscfAddress(Node node) {
        String addr = null;
        String addrType = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (addr == null) {
                    if ((addr = getValueByParamName("Address", childnode)) != null) {
                        String[] address = addr.split(":");
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.IMS_PROXY_ADDR_MOBILE, address[0]);
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.IMS_PROXY_ADDR_WIFI, address[0]);
                        if (address.length > 1) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.IMS_PROXY_PORT_MOBILE, address[1]);
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.IMS_PROXY_PORT_WIFI, address[1]);
                        }
                        continue;
                    }
                }

                if (addrType == null) {
                    if ((addrType = getValueByParamName("AddressType", childnode)) != null) {
                        // Not used
                        continue;
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse phone context
     *
     * @param node Node
     */
    private void parsePhoneContextList(Node node) {
        String PhoneContextvalue = null;
        String Publicuseridentityvalue = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (PhoneContextvalue == null) {
                    if ((PhoneContextvalue = getValueByParamName("PhoneContext", childnode)) != null) {
                        // Not used
                        continue;
                    }
                }

                if (Publicuseridentityvalue == null) {
                    if ((Publicuseridentityvalue = getValueByParamName("Public_user_identity",
                            childnode)) != null) {
                        // Not used
                        continue;
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse application authentication
     *
     * @param node Node
     */
    private void parseAppAuthent(Node node) {
        String authType = null;
        String realm = null;
        String userName = null;
        String userPwd = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (authType == null) {
                    if ((authType = getValueByParamName("AuthType", childnode)) != null) {
                        if (authType.equals("EarlyIMS")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.IMS_AUTHENT_PROCEDURE_MOBILE,
                                    RcsSettingsData.GIBA_AUTHENT);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.IMS_AUTHENT_PROCEDURE_MOBILE,
                                    RcsSettingsData.DIGEST_AUTHENT);
                        }
                        continue;
                    }
                }

                if (realm == null) {
                    if ((realm = getValueByParamName("Realm", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.USERPROFILE_IMS_REALM, realm);
                        continue;
                    }
                }

                if (userName == null) {
                    if ((userName = getValueByParamName("UserName", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID,
                                userName);
                        continue;
                    }
                }

                if (userPwd == null) {
                    if ((userPwd = getValueByParamName("UserPwd", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.USERPROFILE_IMS_PASSWORD, userPwd);
                        continue;
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse RCSe settings
     *
     * @param node Node
     */
    private void parseRCSe(Node node) {
        Node typenode = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (childnode.getNodeName().equals("characteristic")) {
                    if (childnode.getAttributes().getLength() > 0) {
                        typenode = childnode.getAttributes().getNamedItem("type");
                        if (typenode != null) {
                            if (logger.isActivated()) {
                                logger.debug("Node " + childnode.getNodeName() + " with type "
                                        + typenode.getNodeValue());
                            }
                            if (typenode.getNodeValue().equalsIgnoreCase("IMS")) {
                                parseIMS(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("PRESENCE")) {
                                parsePresence(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("XDMS")) {
                                parseXDMS(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("IM")) {
                                parseIM(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("CAPDISCOVERY")) {
                                parseCapabilityDiscovery(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("APN")) {
                                parseAPN(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("OTHER")) {
                                parseOther(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("SERVICES")) {
                                parseServices(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("SUPL")) {
                                parseSupl(childnode);
                            }
                        }
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse IMS settings
     *
     * @param node IMS settings node
     */
    private void parseIMS(Node node) {
        String pdpContextOperPref = null;
        String timert1 = null;
        String timert2 = null;
        String timert4 = null;
        String privateUserIdentity = null;
        String homeDomain = null;
        String voiceDomain = null;
        String smsOverIp = null;
        String keepAliveEnabled = null;
        String voiceDomainUtran = null;
        String mobilityMgtImsVoiceTermination = null;
        String regRetryBasetime = null;
        String regRetryMaxtime = null;
        Node typenode = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (childnode.getNodeName().equals("characteristic")) {
                    if (childnode.getAttributes().getLength() > 0) {
                        typenode = childnode.getAttributes().getNamedItem("type");
                        if (typenode != null) {
                            if (logger.isActivated()) {
                                logger.debug("Node " + childnode.getNodeName() + " with type "
                                        + typenode.getNodeValue());
                            }
                            if (typenode.getNodeValue().equalsIgnoreCase("ConRefs")) {
                                parseConRefs(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("Public_user_identity_List")) {
                                parsePublicUserIdentity(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("Ext")) {
                                parseExt(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("ICSI_List")) {
                                parseICSI(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("LBO_P-CSCF_Address")) {
                                parsePcscfAddress(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("PhoneContext_List")) {
                                parsePhoneContextList(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("APPAUTH")) {
                                parseAppAuthent(childnode);
                            }
                        }
                    }
                }

                if (pdpContextOperPref == null) {
                    if ((pdpContextOperPref = getValueByParamName("PDP_ContextOperPref",
                            childnode)) != null) {
                        // Not supported under Android
                        continue;
                    }
                }

                if (timert1 == null) {
                    if ((timert1 = getValueByParamName("Timer_T1", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(RcsSettingsData.SIP_TIMER_T1,
                                timert1);
                        continue;
                    }
                }

                if (timert2 == null) {
                    if ((timert2 = getValueByParamName("Timer_T2", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(RcsSettingsData.SIP_TIMER_T2,
                                timert2);
                        continue;
                    }
                }

                if (timert4 == null) {
                    if ((timert4 = getValueByParamName("Timer_T4", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(RcsSettingsData.SIP_TIMER_T4,
                                timert4);
                        continue;
                    }
                }

                if (privateUserIdentity == null) {
                    if ((privateUserIdentity = getValueByParamName("Private_User_Identity",
                            childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID,
                                privateUserIdentity);
                        continue;
                    }
                }

                if (homeDomain == null) {
                    if ((homeDomain = getValueByParamName(
                            "Home_network_domain_name", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.USERPROFILE_IMS_HOME_DOMAIN,
                                homeDomain);
                        continue;
                    }
                }

                if (voiceDomain == null) {
                    if ((voiceDomain = getValueByParamName(
                            "Voice_Domain_Preference_E_UTRAN", childnode)) != null) {
                        // Not used for RCS
                        continue;
                    }
                }

                if (smsOverIp == null) {
                    if ((smsOverIp = getValueByParamName(
                            "SMS_Over_IP_Networks_Indication", childnode)) != null) {
                        // Not used for RCS
                        continue;
                    }
                }

                if (keepAliveEnabled == null) {
                    if ((keepAliveEnabled = getValueByParamName("Keep_Alive_Enabled", childnode)) != null) {
                        if (keepAliveEnabled.equals("1")) {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.SIP_KEEP_ALIVE, RcsSettingsData.TRUE);
                        } else {
                            RcsSettings.getInstance().writeParameter(
                                    RcsSettingsData.SIP_KEEP_ALIVE, RcsSettingsData.FALSE);
                        }
                        continue;
                    }
                }

                if (voiceDomainUtran == null) {
                    if ((voiceDomainUtran = getValueByParamName(
                            "Voice_Domain_Preference_UTRAN", childnode)) != null) {
                        // Not used for RCS
                        continue;
                    }
                }

                if (mobilityMgtImsVoiceTermination == null) {
                    if ((mobilityMgtImsVoiceTermination = getValueByParamName(
                            "Mobility_Management_IMS_Voice_Termination", childnode)) != null) {
                        // Not used for RCS
                        continue;
                    }
                }

                if (regRetryBasetime == null) {
                    if ((regRetryBasetime = getValueByParamName("RegRetryBaseTime", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.REGISTER_RETRY_BASE_TIME, regRetryBasetime);
                        continue;
                    }
                }

                if (regRetryMaxtime == null) {
                    if ((regRetryMaxtime = getValueByParamName("RegRetryMaxTime", childnode)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.REGISTER_RETRY_MAX_TIME, regRetryMaxtime);
                        continue;
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Get value of a parameter
     *
     * @param paramName Parameter name
     * @param node Node
     * @return Value or null
     */
    private String getValueByParamName(String paramName, Node node) {
        Node nameNode = null;
        Node valueNode = null;
        
        if ((node == null) ||
        		!(node.getNodeName().equals("parm") ||
        				node.getNodeName().equals("param"))) {
            return null;
        }

        if ((node != null) && (node.getAttributes().getLength() > 0)) {
            nameNode = node.getAttributes().getNamedItem("name");
            if (nameNode == null) {
                return null;
            }
            valueNode = node.getAttributes().getNamedItem("value");
            if (valueNode == null) {
                return null;
            }
            if (nameNode.getNodeValue().equalsIgnoreCase(paramName)) {
            	String value = valueNode.getNodeValue();
                if (logger.isActivated()) {
                    // For debug only: logger.debug("Read parameter " + paramName + ": " + value);
                    logger.debug("Read parameter " + paramName);
                }
                if (value.equalsIgnoreCase("x")){
                	// If the value is containing "x" or "X", we simply ignore the parameter
                	return null;
                }else{
                	return value;
                }
            } else {
                return null;
            }
        }
        return null;
    }
    
    /**
     * Extract the username part of the SIP-URI
     * 
     * @param uri SIP-URI
     * @return Username
     */
    private String extractUserNamePart(String uri) {
    	if ((uri == null) || (uri.trim().length() == 0)) {
    		return "";
    	}

    	try {
    		uri = uri.trim();
    		int index1 = uri.indexOf("sip:");
    		if (index1 != -1) {
				int index2 = uri.indexOf("@", index1);
				String result = uri.substring(index1+4, index2);
				return result;
    		} else {
    			return uri;
    		}
		} catch(Exception e) {
			return "";
		}
    }
    
    /**
     * Format to SIP-URI
     * 
     * @param uri URI
     * @return SIP-URI
     */
    private String formatSipUri(String uri) {
    	if ((uri == null) || (uri.trim().length() == 0)) {
    		return "";
    	}

    	try {
    		uri = uri.trim();
	    	if (!uri.startsWith("sip:")) {
	    		uri = "sip:" + uri;
	    	}
	    	return uri;
		} catch(Exception e) {
			return "";
		}
    }
}
