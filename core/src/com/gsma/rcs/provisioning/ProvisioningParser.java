/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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

package com.gsma.rcs.provisioning;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData;
import com.gsma.rcs.provider.settings.RcsSettingsData.AuthenticationProcedure;
import com.gsma.rcs.provider.settings.RcsSettingsData.EnableRcseSwitch;
import com.gsma.rcs.provider.settings.RcsSettingsData.FileTransferProtocol;
import com.gsma.rcs.provider.settings.RcsSettingsData.GsmaRelease;
import com.gsma.rcs.provider.settings.RcsSettingsData.ImMsgTech;
import com.gsma.rcs.provider.settings.RcsSettingsData.ImSessionStartMode;
import com.gsma.rcs.utils.CloseableUtils;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.DeviceUtils;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.CommonServiceConfiguration.MessagingMethod;
import com.gsma.services.rcs.CommonServiceConfiguration.MessagingMode;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax2.sip.ListeningPoint;

/**
 * Provisioning parser
 * 
 * @author jexa7410
 */
public class ProvisioningParser {
    /**
     * Parameter type text
     */
    private static final int TYPE_TXT = 0;

    /**
     * Parameter type integer
     */
    private static final int TYPE_INT = 1;

    private static final long SECONDS_TO_MILLISECONDS_CONVERSION_RATE = 1000;

    private static final String UUID_VALUE = "uuid_Value";

    private static final String PROTOCOL_HTTPS = "https";

    private ProvisioningInfo provisioningInfo = new ProvisioningInfo();

    private String mContent;

    private RcsSettings mRcsSettings;

    private boolean mFirst = false;

    private static final String STRING_BOOLEAN_TRUE = "1";

    private static final Logger sLogger = Logger.getLogger(ProvisioningParser.class.getName());

    /**
     * Enumerated type for the root node
     */
    private enum RootNodeType {
        VERS, TOKEN, MSG, APPLICATION, IMS, PRESENCE, XDMS, IM, CAPDISCOVERY, APN, OTHER, SERVICES, SUPL, SERVICEPROVIDEREXT, UX
    }

    /**
     * Enumerated type for the IMS server version
     */
    enum ImsServerVersion {
        JOYN, NON_JOYN
    }

    /**
     * Constructor
     * 
     * @param content The content to be parsed.
     * @param rcsSettings the RCS settings.
     */
    public ProvisioningParser(String content, RcsSettings rcsSettings) {
        mContent = content;
        mRcsSettings = rcsSettings;
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
     * @param release The GSMA release (Albatros, Blackbird, Crane...) before parsing
     * @param messagingMode the messaging mode
     * @param first True if it is a first provisioning
     * @throws SAXException
     */
    public void parse(GsmaRelease release, MessagingMode messagingMode, boolean first)
            throws SAXException {
        ByteArrayInputStream inputStream = null;
        try {
            final boolean logActivated = sLogger.isActivated();
            if (logActivated) {
                sLogger.debug("Start the parsing of content first=".concat(Boolean.toString(first)));
            }
            mFirst = first;
            inputStream = new ByteArrayInputStream(mContent.getBytes(UTF8));
            DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dbuilder = dfactory.newDocumentBuilder();
            Document doc = dbuilder.parse(inputStream);
            if (doc == null) {
                throw new SAXException("The provisioning content document is null!");
            }
            if (logActivated) {
                sLogger.debug("Parsed Doc =" + doc);
            }
            Node rootnode = doc.getDocumentElement();
            Node childnode = rootnode.getFirstChild();
            if (childnode == null) {
                throw new SAXException(
                        "The first chid node in the provisioning content document is null!");
            }
            int nodeNumber = 0;
            do {
                if (childnode.getNodeName().equals("characteristic")) {
                    if (childnode.getAttributes().getLength() > 0) {
                        Node typenode = childnode.getAttributes().getNamedItem("type");
                        if (typenode != null && typenode.getNodeValue() != null) {
                            if (logActivated) {
                                sLogger.debug("Node " + childnode.getNodeName() + " with type "
                                        + typenode.getNodeValue());
                            }
                            nodeNumber++;
                            String nodeType = typenode.getNodeValue().toUpperCase();
                            try {
                                RootNodeType rootNodeType = RootNodeType.valueOf(nodeType);
                                switch (rootNodeType) {
                                    case VERS:
                                        parseVersion(childnode);
                                        break;
                                    case TOKEN:
                                        parseToken(childnode);
                                        break;
                                    case MSG:
                                        parseTermsMessage(childnode);
                                        break;
                                    case APPLICATION:
                                        parseApplication(childnode);
                                        break;
                                    case IMS:
                                        parseIMS(childnode);
                                        break;
                                    case PRESENCE:
                                        parsePresence(childnode);
                                        break;
                                    case XDMS:
                                        parseXDMS(childnode);
                                        break;
                                    case IM:
                                        parseIM(childnode);
                                        break;
                                    case APN:
                                        parseAPN(childnode);
                                        break;
                                    case OTHER:
                                        parseOther(childnode);
                                        break;
                                    case SERVICES:
                                        parseServices(childnode);
                                        break;
                                    case SUPL:
                                        parseSupl(childnode);
                                        break;
                                    case SERVICEPROVIDEREXT:
                                        parseServiceProviderExt(childnode);
                                        break;
                                    case UX:
                                        parseUx(childnode, ImsServerVersion.NON_JOYN);
                                        break;
                                    default:
                                        if (sLogger.isActivated()) {
                                            sLogger.warn("unhandled node type: " + nodeType);
                                        }
                                }
                            } catch (IllegalArgumentException e) {
                                if (sLogger.isActivated()) {
                                    sLogger.warn("invalid node type: " + nodeType);
                                }
                            }
                        }
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
            if (nodeNumber == 1) {
                /*
                 * We received a single node (the version one) ! This is the case if the version
                 * number is negative or in order to extend the validity of the provisioning. In
                 * that case we restore the relevant GSMA release saved before parsing.
                 */
                mRcsSettings.setGsmaRelease(release);
                /* We do the same for the messaging mode */
                mRcsSettings.setMessagingMode(messagingMode);
            }
        } catch (ParserConfigurationException e) {
            throw new SAXException("Can't parse provisioning content document!", e);

        } catch (IOException e) {
            throw new SAXException("Can't parse provisioning content document!", e);

        } finally {
            CloseableUtils.tryToClose(inputStream);
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
        Node versionchild = node.getFirstChild();

        if (versionchild != null) {
            do {
                if (version == null) {
                    if ((version = getValueByParamName("version", versionchild, TYPE_TXT)) != null) {
                        provisioningInfo.setVersion(Integer.parseInt(version));
                        continue;
                    }
                }
                if (validity == null) {
                    if ((validity = getValueByParamName("validity", versionchild, TYPE_INT)) != null) {
                        provisioningInfo.setValidity(Long.parseLong(validity)
                                * SECONDS_TO_MILLISECONDS_CONVERSION_RATE);
                        continue;
                    }
                }
            } while ((versionchild = versionchild.getNextSibling()) != null);
        }
    }

    /**
     * Parse the provisioning Token
     * 
     * @param node Node
     */
    private void parseToken(Node node) {
        String token = null;
        String tokenValidity = null;
        Node tokenChild = node.getFirstChild();

        if (tokenChild != null) {
            do {
                if (token == null) {
                    if ((token = getValueByParamName("token", tokenChild, TYPE_TXT)) != null) {
                        provisioningInfo.setToken(token);
                        continue;
                    }
                }
                if (tokenValidity == null) {
                    if ((tokenValidity = getValueByParamName("validity", tokenChild, TYPE_INT)) != null) {
                        provisioningInfo.setTokenValidity(Long.parseLong(tokenValidity));
                        continue;
                    }
                }
            } while ((tokenChild = tokenChild.getNextSibling()) != null);
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
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (title == null) {
                    if ((title = getValueByParamName("title", childnode, TYPE_TXT)) != null) {
                        mRcsSettings.setProvisioningUserMessageTitle("".equals(title) ? null
                                : title);
                        provisioningInfo.setTitle(title);
                        continue;
                    }
                }
                if (message == null) {
                    if ((message = getValueByParamName("message", childnode, TYPE_TXT)) != null) {
                        mRcsSettings.setProvisioningUserMessageContent("".equals(message) ? null
                                : message);
                        provisioningInfo.setMessage(message);
                        continue;
                    }
                }
                if (acceptBtn == null) {
                    if ((acceptBtn = getValueByParamName("Accept_btn", childnode, TYPE_INT)) != null) {
                        boolean accept = STRING_BOOLEAN_TRUE.equals(acceptBtn);
                        mRcsSettings.setProvisioningAcceptButton(accept);
                        provisioningInfo.setAcceptBtn(accept);
                        continue;
                    }
                }
                if (rejectBtn == null) {
                    if ((rejectBtn = getValueByParamName("Reject_btn", childnode, TYPE_INT)) != null) {
                        boolean reject = STRING_BOOLEAN_TRUE.equals(rejectBtn);
                        mRcsSettings.setProvisioningRejectButton(reject);
                        provisioningInfo.setRejectBtn(reject);
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
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (appId == null) {
                    if ((appId = getValueByParamName("AppID", childnode, TYPE_TXT)) != null) {
                        continue;
                    }
                }
                if (name == null) {
                    if ((name = getValueByParamName("Name", childnode, TYPE_TXT)) != null) {
                        continue;
                    }
                }
                if (appRef == null) {
                    if ((appRef = getValueByParamName("AppRef", childnode, TYPE_TXT)) != null) {
                        continue;
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }

        if (appRef != null
                && (appRef.equalsIgnoreCase("IMS-Settings") || appRef.equalsIgnoreCase("ims-rcse"))) {
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
        // Not supported
    }

    /**
     * Parse presence watcher
     * 
     * @param node Node
     */
    private void parsePresenceWatcher(Node node) {
        // TODO: "FetchAuth"
        // TODO: "ContactCapPresAut"
    }

    /**
     * Parse presentity watcher
     * 
     * @param node Node
     */
    private void parsePresentityWatcher(Node node) {
        // TODO: "WATCHERFETCHAUTH"
    }

    /**
     * Parse presence
     * 
     * @param node Node
     */
    private void parsePresence(Node node) {
        String usePresence = null;
        String presencePrfl = null;
        String iconMaxSize = null;
        String noteMaxSize = null;
        String publishTimer = null;
        Node typenode = null;
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (childnode.getNodeName().equals("characteristic")) {
                    if (childnode.getAttributes().getLength() > 0) {
                        typenode = childnode.getAttributes().getNamedItem("type");
                        if (typenode != null) {
                            if (typenode.getNodeValue().equalsIgnoreCase("FAVLINK")) {
                                parseFavoriteLink(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("SERVCAPWATCH")) {
                                parsePresenceWatcher(childnode);
                            } else if (typenode.getNodeValue()
                                    .equalsIgnoreCase("ServCapPresentity")) {
                                parsePresentityWatcher(childnode);
                            }
                        }
                    }
                }

                if (usePresence == null) {
                    if ((usePresence = getValueByParamName("usePresence", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE,
                                !usePresence.equals("0"));
                        continue;
                    }
                }

                if (presencePrfl == null) {
                    if ((presencePrfl = getValueByParamName("presencePrfl", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY,
                                !presencePrfl.equals("0"));
                        continue;
                    }
                }

                if (iconMaxSize == null) {
                    if ((iconMaxSize = getValueByParamName("IconMaxSize", childnode, TYPE_INT)) != null) {
                        long size = Long.parseLong(iconMaxSize);
                        mRcsSettings.setMaxPhotoIconSize(size);
                        continue;
                    }
                }

                if (noteMaxSize == null) {
                    if ((noteMaxSize = getValueByParamName("NoteMaxSize", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeInteger(RcsSettingsData.MAX_FREETXT_LENGTH,
                                Integer.parseInt(noteMaxSize));
                        continue;
                    }
                }

                if (publishTimer == null) {
                    if ((publishTimer = getValueByParamName("PublishTimer", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeLong(RcsSettingsData.PUBLISH_EXPIRE_PERIOD,
                                Long.parseLong(publishTimer)
                                        * SECONDS_TO_MILLISECONDS_CONVERSION_RATE);
                        continue;
                    }
                }

                // Not supported: "AvailabilityAuth"
                // Not supported: "client-obj-datalimit"
                // Not used for RCS: "content-serveruri"
                // Not supported: "source-throttlepublish"
                // Not supported: "max-number-ofsubscriptions-inpresence-list"
                // TODO: "service-uritemplate"

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
        String groupChatAuth = null;
        String ftAuth = null;
        String geolocPushAuth = null;
        String vsAuth = null;
        String isAuth = null;
        String rcsIPVoiceCallAuth = null;
        String rcsIPVideoCallAuth = null;
        String allowExtensions = null;
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            // Node "SERVICES" is mandatory in GSMA release Blackbird and not present in previous
            // one Albatros.
            // Only if the parsing result contains a SERVICE tree, Blackbird is assumed as release.
            // This trick is used to detect the GSMA release as provisioned by the network.
            mRcsSettings.setGsmaRelease(GsmaRelease.BLACKBIRD);
            do {

                if (chatAuth == null) {
                    if ((chatAuth = getValueByParamName("ChatAuth", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.CAPABILITY_IM_SESSION,
                                chatAuth.equals("1"));
                        continue;
                    }
                }

                if (groupChatAuth == null) {
                    if ((groupChatAuth = getValueByParamName("groupChatAuth", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.CAPABILITY_IM_GROUP_SESSION,
                                groupChatAuth.equals("1"));
                        continue;
                    }
                }

                if (ftAuth == null) {
                    if ((ftAuth = getValueByParamName("ftAuth", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.CAPABILITY_FILE_TRANSFER,
                                ftAuth.equals("1"));
                        continue;
                    }
                }

                if (vsAuth == null) {
                    if ((vsAuth = getValueByParamName("vsAuth", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.CAPABILITY_VIDEO_SHARING,
                                vsAuth.equals("1"));
                        continue;
                    }
                }

                if (isAuth == null) {
                    if ((isAuth = getValueByParamName("isAuth", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.CAPABILITY_IMAGE_SHARING,
                                isAuth.equals("1"));
                        continue;
                    }
                }

                if (geolocPushAuth == null) {
                    if ((geolocPushAuth = getValueByParamName("geolocPushAuth", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.CAPABILITY_GEOLOCATION_PUSH,
                                geolocPushAuth.equals("1"));
                        continue;
                    }
                }

                if (presencePrfl == null) {
                    if ((presencePrfl = getValueByParamName("presencePrfl", childnode, TYPE_INT)) != null) {
                        if (presencePrfl.equals("1")) {
                            sLogger.error("Social presence is not supported in TAPI 1.5.1, ignoring capability received through provisioning.");
                        }
                        mRcsSettings
                                .writeBoolean(RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE, false);
                        continue;
                    }
                }

                if (rcsIPVoiceCallAuth == null) {
                    if ((rcsIPVoiceCallAuth = getValueByParamName("rcsIPVoiceCallAuth", childnode,
                            TYPE_INT)) != null) {
                        int value = Integer.decode(rcsIPVoiceCallAuth);
                        mRcsSettings.writeBoolean(RcsSettingsData.CAPABILITY_IP_VOICE_CALL,
                                (value % 16) != 0);
                        continue;
                    }
                }

                if (rcsIPVideoCallAuth == null) {
                    if ((rcsIPVideoCallAuth = getValueByParamName("rcsIPVideoCallAuth", childnode,
                            TYPE_INT)) != null) {
                        int value = Integer.decode(rcsIPVoiceCallAuth);
                        mRcsSettings.writeBoolean(RcsSettingsData.CAPABILITY_IP_VIDEO_CALL,
                                (value % 16) != 0);
                        continue;
                    }
                }

                if (allowExtensions == null) {
                    if ((allowExtensions = getValueByParamName("allowRCSExtensions", childnode,
                            TYPE_INT)) != null) {
                        int value = Integer.decode(allowExtensions);
                        mRcsSettings.writeBoolean(RcsSettingsData.ALLOW_EXTENSIONS,
                                (value % 16) != 0);
                        continue;
                    }
                }

                // Not used: "standaloneMsgAuth"
                // Not used: "geolocPullAuth"

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
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (revokeTimer == null) {
                    if ((revokeTimer = getValueByParamName("RevokeTimer", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeLong(RcsSettingsData.REVOKE_TIMEOUT,
                                Long.parseLong(revokeTimer)
                                        * SECONDS_TO_MILLISECONDS_CONVERSION_RATE);
                        continue;
                    }
                }

                if (xcapRootURI == null) {
                    if ((xcapRootURI = getValueByParamName("XCAPRootURI", childnode, TYPE_TXT)) != null) {
                        mRcsSettings.setXdmServer("".equals(xcapRootURI) ? null : Uri
                                .parse(xcapRootURI));
                        continue;
                    }
                }

                if (xcapAuthenticationUsername == null) {
                    if ((xcapAuthenticationUsername = getValueByParamName(
                            "XCAPAuthenticationUserName", childnode, TYPE_TXT)) != null) {
                        mRcsSettings.setXdmLogin("".equals(xcapAuthenticationUsername) ? null
                                : xcapAuthenticationUsername);
                        continue;
                    }
                }

                if (xcapAuthenticationSecret == null) {
                    if ((xcapAuthenticationSecret = getValueByParamName("XCAPAuthenticationSecret",
                            childnode, TYPE_TXT)) != null) {
                        mRcsSettings.setXdmPassword("".equals(xcapAuthenticationSecret) ? null
                                : xcapAuthenticationSecret);
                        continue;
                    }
                }

                // Not used (only Digest is used): "XCAPAuthenticationType"

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
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (textMaxLength == null) {
                    if ((textMaxLength = getValueByParamName("TextMaxLength", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeInteger(RcsSettingsData.MAX_GEOLOC_LABEL_LENGTH,
                                Integer.parseInt(textMaxLength));
                        continue;
                    }
                }

                if (locInfoMaxValidTime == null) {
                    if ((locInfoMaxValidTime = getValueByParamName("LocInfoMaxValidTime",
                            childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeLong(RcsSettingsData.GEOLOC_EXPIRATION_TIME,
                                Long.parseLong(locInfoMaxValidTime)
                                        * SECONDS_TO_MILLISECONDS_CONVERSION_RATE);
                        continue;
                    }
                }

                // Not used: "geolocPullOpenValue"
                // Not used: "geolocPullApiGwAddress"
                // Not used: "geolocPullBlockTimer"

            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse service provider ext
     * 
     * @param node Node
     */
    private void parseServiceProviderExt(Node node) {
        Node typenode = null;
        Node childnode = node.getFirstChild();
        if (childnode != null) {
            do {
                if (childnode.getNodeName().equals("characteristic")) {
                    if (childnode.getAttributes().getLength() > 0) {
                        typenode = childnode.getAttributes().getNamedItem("type");
                        if (typenode != null) {
                            if (typenode.getNodeValue().equalsIgnoreCase("joyn")) {
                                parseRcs(childnode);
                            }
                        }
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse RCS
     * 
     * @param node Node
     */
    private void parseRcs(Node node) {
        Node typenode = null;
        if (node == null) {
            return;
        }
        String msgCapValidity = null;
        Node childnode = node.getFirstChild();
        if (childnode != null) {
            do {
                if (childnode.getNodeName().equals("characteristic")) {
                    NamedNodeMap attributes = childnode.getAttributes();
                    if (attributes.getLength() > 0) {
                        typenode = attributes.getNamedItem("type");
                        if (typenode != null) {
                            String nodeValue = typenode.getNodeValue();
                            if (nodeValue.equalsIgnoreCase("UX")) {
                                parseUx(childnode, ImsServerVersion.JOYN);
                            } else if (nodeValue.equalsIgnoreCase("Messaging")) {
                                parseMessaging(childnode);
                            }
                        }
                    }
                }
                if (msgCapValidity == null) {
                    if ((msgCapValidity = getValueByParamName("msgCapValidity", childnode, TYPE_INT)) != null) {
                        long validity = Long.parseLong(msgCapValidity)
                                * SECONDS_TO_MILLISECONDS_CONVERSION_RATE;
                        mRcsSettings.writeLong(RcsSettingsData.MSG_CAP_VALIDITY_PERIOD, validity);
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse Messaging
     * 
     * @param node Node
     */
    private void parseMessaging(Node node) {
        String ftHTTPCapAlwaysOn = null;
        String deliveryTimeout = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (ftHTTPCapAlwaysOn == null) {
                    if ((ftHTTPCapAlwaysOn = getValueByParamName("ftHTTPCapAlwaysOn", childnode,
                            TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.FT_HTTP_CAP_ALWAYS_ON,
                                !ftHTTPCapAlwaysOn.equals("0"));
                        continue;
                    }
                }
                if (deliveryTimeout == null) {
                    if ((deliveryTimeout = getValueByParamName("deliveryTimeout", childnode,
                            TYPE_INT)) != null) {
                        long timeout = Long.parseLong(deliveryTimeout)
                                * SECONDS_TO_MILLISECONDS_CONVERSION_RATE;
                        mRcsSettings.writeLong(RcsSettingsData.MSG_DELIVERY_TIMEOUT, timeout);
                        continue;
                    }
                }
            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse Ux
     * 
     * @param node Node
     * @param isJoyn
     */
    private void parseUx(Node node, ImsServerVersion isJoyn) {
        String messagingUX = null;

        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {

                if (messagingUX == null) {
                    if ((messagingUX = getValueByParamName("messagingUX", childnode, TYPE_INT)) != null) {
                        if (messagingUX.equals("1")) {
                            mRcsSettings.setMessagingMode(MessagingMode.INTEGRATED);
                        } else {
                            if (ImsServerVersion.JOYN.equals(isJoyn)) {
                                mRcsSettings.setMessagingMode(MessagingMode.CONVERGED);
                            } else {
                                mRcsSettings.setMessagingMode(MessagingMode.SEAMLESS);
                            }
                        }
                        continue;
                    }
                }

            } while ((childnode = childnode.getNextSibling()) != null);
        }

        // Not used: oneButtonVoiceCall
        // Not used: oneButtonVideoCall
    }

    /**
     * Parse IM
     * 
     * @param node Node
     */
    private void parseIM(Node node) {
        String imCapAlwaysOn = null;
        String ftCapAlwaysOn = null;
        String imWarnSF = null;
        String imSessionStart = null;
        String ftWarnSize = null;
        String autoAcceptFt = null;
        String ftHttpCsUri = null;
        String ftHttpCsUser = null;
        String ftHttpCsPwd = null;
        String ftDefaultMech = null;
        String ftSF = null;
        String chatAuth = null;
        String smsFallBackAuth = null;
        String autoAcceptChat = null;
        String autoAcceptGroupChat = null;
        String maxSize1to1 = null;
        String maxSize1toM = null;
        String timerIdle = null;
        String maxSizeFileTransfer = null;
        String ftThumb = null;
        String maxAdhocGroupSize = null;
        String confFctyUri = null;
        String groupChatSF = null;
        String groupChatOnlySF = null;
        String maxConcurrentSession = null;
        String imMsgTech = null;
        String firstMessageInvite = null;

        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (imCapAlwaysOn == null) {
                    if ((imCapAlwaysOn = getValueByParamName("imCapAlwaysON", childnode, TYPE_INT)) != null) {
                        boolean _imCapAlwaysOn = !imCapAlwaysOn.equals("0");
                        mRcsSettings.writeBoolean(RcsSettingsData.IM_CAPABILITY_ALWAYS_ON,
                                _imCapAlwaysOn);
                        // set default IM messaging method if first provisioning
                        if (mFirst) {
                            if (_imCapAlwaysOn) {
                                mRcsSettings.setDefaultMessagingMethod(MessagingMethod.RCS);
                            } else {
                                mRcsSettings.setDefaultMessagingMethod(MessagingMethod.AUTOMATIC);
                            }
                        }
                        continue;
                    }
                }

                if (ftCapAlwaysOn == null) {
                    if ((ftCapAlwaysOn = getValueByParamName("ftCapAlwaysON", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.FT_CAPABILITY_ALWAYS_ON,
                                !ftCapAlwaysOn.equals("0"));
                        continue;
                    }
                }

                if (maxConcurrentSession == null) {
                    if ((maxConcurrentSession = getValueByParamName("MaxConcurrentSession",
                            childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeInteger(RcsSettingsData.MAX_CHAT_SESSIONS,
                                Integer.parseInt(maxConcurrentSession));
                        continue;
                    }
                }

                if (groupChatSF == null) {
                    if ((groupChatSF = getValueByParamName("GroupChatFullStandFwd", childnode,
                            TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.CAPABILITY_GROUP_CHAT_SF,
                                !groupChatSF.equals("0"));
                        continue;
                    }
                }

                if (groupChatOnlySF == null) {
                    if ((groupChatOnlySF = getValueByParamName("GroupChatOnlyFStandFwd", childnode,
                            TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.GROUP_CHAT_INVITE_ONLY_FULL_SF,
                                !groupChatOnlySF.equals("0"));
                        continue;
                    }
                }

                if (imWarnSF == null) {
                    if ((imWarnSF = getValueByParamName("imWarnSF", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.WARN_SF_SERVICE,
                                !imWarnSF.equals("0"));
                        continue;
                    }
                }

                if (autoAcceptFt == null) {
                    if ((autoAcceptFt = getValueByParamName("ftAutAccept", childnode, TYPE_INT)) != null) {
                        boolean aaModeChangeable = !autoAcceptFt.equals("0");
                        // Check if first provisioning or transition of MNO setting
                        if (mFirst
                                || (aaModeChangeable != mRcsSettings
                                        .isFtAutoAcceptedModeChangeable())) {
                            // Save first or new setting value
                            mRcsSettings.setFtAutoAcceptedModeChangeable(aaModeChangeable);
                            if (aaModeChangeable) {
                                // Enforce user settings for AA to default value
                                // By default, AA is enabled in normal conditions
                                mRcsSettings.setFileTransferAutoAccepted(true);
                                // By default, AA is disabled in roaming
                                mRcsSettings.setFileTransferAutoAcceptedInRoaming(false);
                            } else {
                                // Enforce user settings for AA for normal conditions and roaming
                                mRcsSettings.setFileTransferAutoAccepted(false);
                                mRcsSettings.setFileTransferAutoAcceptedInRoaming(false);
                            }
                        }
                        continue;
                    }
                }

                if (ftSF == null) {
                    if ((ftSF = getValueByParamName("ftStAndFwEnabled", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.CAPABILITY_FILE_TRANSFER_SF,
                                !ftSF.equals("0"));
                        continue;
                    }
                }

                if (ftHttpCsUri == null) {
                    if ((ftHttpCsUri = getValueByParamName("ftHTTPCSURI", childnode, TYPE_TXT)) != null) {
                        /*
                         * According to "Rich Communication Suite 5.1 Advanced Communications
                         * Services and Client Specification Version 4.0" 3.5.4.8.3 File transfer
                         * procedure 3.5.4.8.3.1 Sender procedures This specification uses the term
                         * 'HTTP POST' and 'HTTP GET' as a generic reference to the action of using
                         * the POST or GET method. However, it is strongly recommended that whenever
                         * the POST action contains sensitive information such as a user ID or
                         * password, the action should take place over a secure connection and/or
                         * via HTTPS explicitly.
                         */
                        Uri ftHttpServAddr = "".equals(ftHttpCsUri) ? null : Uri.parse(ftHttpCsUri);
                        if (ftHttpServAddr != null
                                && !PROTOCOL_HTTPS.equals(ftHttpServAddr.getScheme())) {
                            sLogger.error(new StringBuilder(ftHttpCsUri)
                                    .append(" is not a secure protocol, hence disabling ftHttp capability.")
                                    .toString());
                            continue;
                        }
                        mRcsSettings.setFtHttpServer(ftHttpServAddr);
                        continue;
                    }
                }

                if (ftHttpCsUser == null) {
                    if ((ftHttpCsUser = getValueByParamName("ftHTTPCSUser", childnode, TYPE_TXT)) != null) {
                        mRcsSettings.setFtHttpLogin("".equals(ftHttpCsUser) ? null : ftHttpCsUser);
                        continue;
                    }
                }

                if (ftHttpCsPwd == null) {
                    if ((ftHttpCsPwd = getValueByParamName("ftHTTPCSPwd", childnode, TYPE_TXT)) != null) {
                        mRcsSettings.setFtHttpPassword("".equals(ftHttpCsPwd) ? null : ftHttpCsPwd);
                        continue;
                    }
                }

                if (ftDefaultMech == null) {
                    if ((ftDefaultMech = getValueByParamName("ftDefaultMech", childnode, TYPE_TXT)) != null) {
                        FileTransferProtocol protocol = FileTransferProtocol.valueOf(ftDefaultMech);
                        mRcsSettings.setFtProtocol(protocol);
                        continue;
                    }
                }

                if (imSessionStart == null) {
                    if ((imSessionStart = getValueByParamName("imSessionStart", childnode, TYPE_INT)) != null) {
                        ImSessionStartMode mode = ImSessionStartMode.valueOf(Integer
                                .parseInt(imSessionStart));
                        mRcsSettings.setImSessionStartMode(mode);
                        continue;
                    }
                }

                if (ftWarnSize == null) {
                    if ((ftWarnSize = getValueByParamName("ftWarnSize", childnode, TYPE_INT)) != null) {
                        long size = Long.parseLong(ftWarnSize) * 1024L;
                        mRcsSettings.setWarningMaxFileTransferSize(size);
                        continue;
                    }
                }

                if (chatAuth == null) {
                    if ((chatAuth = getValueByParamName("ChatAuth", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.CAPABILITY_IM_SESSION,
                                !chatAuth.equals("0"));
                        continue;
                    }
                }

                if (smsFallBackAuth == null) {
                    if ((smsFallBackAuth = getValueByParamName("SmsFallBackAuth", childnode,
                            TYPE_INT)) != null) {
                        // Careful:
                        // 0- Indicates authorization is ok
                        // 1- Indicates authorization is non ok
                        mRcsSettings.writeBoolean(RcsSettingsData.SMS_FALLBACK_SERVICE,
                                smsFallBackAuth.equals("0"));
                        continue;
                    }
                }

                if (autoAcceptChat == null) {
                    if ((autoAcceptChat = getValueByParamName("AutAccept", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.AUTO_ACCEPT_CHAT,
                                !autoAcceptChat.equals("0"));
                        continue;
                    }
                }

                if (autoAcceptGroupChat == null) {
                    if ((autoAcceptGroupChat = getValueByParamName("AutAcceptGroupChat", childnode,
                            TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.AUTO_ACCEPT_GROUP_CHAT,
                                !autoAcceptGroupChat.equals("0"));
                        continue;
                    }
                }

                if (maxSize1to1 == null) {
                    if ((maxSize1to1 = getValueByParamName("MaxSize1to1", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeInteger(RcsSettingsData.MAX_CHAT_MSG_LENGTH,
                                Integer.parseInt(maxSize1to1));
                        continue;
                    }
                }

                if (maxSize1toM == null) {
                    if ((maxSize1toM = getValueByParamName("MaxSize1toM", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeInteger(RcsSettingsData.MAX_GROUPCHAT_MSG_LENGTH,
                                Integer.parseInt(maxSize1toM));
                        continue;
                    }
                }

                if (timerIdle == null) {
                    if ((timerIdle = getValueByParamName("TimerIdle", childnode, TYPE_INT)) != null) {
                        mRcsSettings
                                .writeLong(RcsSettingsData.CHAT_IDLE_DURATION,
                                        Long.parseLong(timerIdle)
                                                * SECONDS_TO_MILLISECONDS_CONVERSION_RATE);
                        continue;
                    }
                }

                if (maxSizeFileTransfer == null) {
                    if ((maxSizeFileTransfer = getValueByParamName("MaxSizeFileTr", childnode,
                            TYPE_INT)) != null) {
                        long size = Long.parseLong(maxSizeFileTransfer) * 1024L;
                        mRcsSettings.setMaxFileTransferSize(size);
                        continue;
                    }
                }

                if (ftThumb == null) {
                    if ((ftThumb = getValueByParamName("ftThumb", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(
                                RcsSettingsData.CAPABILITY_FILE_TRANSFER_THUMBNAIL,
                                !ftThumb.equals("0"));
                        continue;
                    }
                }

                if (maxAdhocGroupSize == null) {
                    if ((maxAdhocGroupSize = getValueByParamName("max_adhoc_group_size", childnode,
                            TYPE_INT)) != null) {
                        mRcsSettings.writeInteger(RcsSettingsData.MAX_CHAT_PARTICIPANTS,
                                Integer.parseInt(maxAdhocGroupSize));
                        continue;
                    }
                }

                if (confFctyUri == null) {
                    if ((confFctyUri = getValueByParamName("conf-fcty-uri", childnode, TYPE_TXT)) != null) {
                        mRcsSettings.setImConferenceUri("".equals(confFctyUri) ? null
                                : formatSipUri(confFctyUri.trim()));
                        continue;
                    }
                }

                if (imMsgTech == null) {
                    if ((imMsgTech = getValueByParamName("imMsgTech", childnode, TYPE_INT)) != null) {
                        ImMsgTech value = ImMsgTech.valueOf(Integer.parseInt(imMsgTech));
                        mRcsSettings.setImMsgTech(value);
                        continue;
                    }
                }

                if (firstMessageInvite == null) {
                    if ((firstMessageInvite = getValueByParamName("firstMsgInvite", childnode,
                            TYPE_INT)) != null) {
                        boolean isFirstMessageInvite = !firstMessageInvite.equals("0");
                        /*
                         * Stack only support simple IM now, means isFirstMessageInvite must be set
                         * to true. Specification reference: Rich Communication Suite 5.1 Advanced
                         * Communications Services and Client Specification Version 3.0 Page 182
                         * 3.3.4.2 Technical Realization of 1-to-1 Chat features when using OMA
                         * SIMPLE IM For OMA SIMPLE IM, first message is always included in a
                         * CPIM/IMDN wrapper carried in the SIP INVITE request. So the configuration
                         * parameter FIRST MSG IN INVITE defined in Table 77 is always set to 1. A
                         * client should always include "positive-delivery" in the value for the
                         * Disposition-Notification header field in that message. That means that
                         * the value of the header field is either "positive-delivery" or
                         * "positive-delivery,display" depending on whether display notifications
                         * were requested. The value of "negativedelivery" is not used in RCS for
                         * 1-to-1 Chat. SIP INVITE requests for a one-to-one session that carry a
                         * message in CPIM/IMDN wrapper shall be rejected by the server unless they
                         * carry a Disposition-Notification header that at least includes
                         * "positivedelivery".
                         */
                        if (!isFirstMessageInvite) {
                            sLogger.error("isFirstMessageInInvite is set to false, it is incorrect according"
                                    + " to Blackbird protocol, please check provisioning values. Ignoring the "
                                    + "set to false request.");
                            continue;
                        }
                        mRcsSettings.setFirstMessageInInvite(isFirstMessageInvite);
                        continue;
                    }
                }

                // Not used for RCS: "pres-srv-cap"
                // Not used for RCS: "deferred-msg-func-uri"
                // Not used for RCS: "exploder-uri"

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
        Node typenode = null;
        if (childnode != null) {
            do {
                if (childnode.getNodeName().equals("characteristic")) {
                    NamedNodeMap attributes = childnode.getAttributes();
                    if (attributes.getLength() > 0) {
                        typenode = attributes.getNamedItem("type");
                        if (typenode != null) {
                            if (typenode.getNodeValue().equalsIgnoreCase("Ext")) {
                                parseExt(childnode);
                            }
                        }
                    }
                }
                if (pollingPeriod == null) {
                    if ((pollingPeriod = getValueByParamName("pollingPeriod", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeLong(RcsSettingsData.CAPABILITY_POLLING_PERIOD,
                                Long.parseLong(pollingPeriod)
                                        * SECONDS_TO_MILLISECONDS_CONVERSION_RATE);
                        continue;
                    }
                }

                if (capInfoExpiry == null) {
                    if ((capInfoExpiry = getValueByParamName("capInfoExpiry", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeLong(RcsSettingsData.CAPABILITY_EXPIRY_TIMEOUT,
                                Long.parseLong(capInfoExpiry)
                                        * SECONDS_TO_MILLISECONDS_CONVERSION_RATE);
                        continue;
                    }
                }

                if (presenceDiscovery == null) {
                    if ((presenceDiscovery = getValueByParamName("presenceDisc", childnode,
                            TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY,
                                !presenceDiscovery.equals("0"));
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
        // Not supported: "rcseOnlyAPN"
        String enableRcseSwitch = null;

        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {

                if (enableRcseSwitch == null) {
                    if ((enableRcseSwitch = getValueByParamName("enableRcseSwitch", childnode,
                            TYPE_INT)) != null) {
                        mRcsSettings.setEnableRcseSwitch(EnableRcseSwitch.valueOf(Integer
                                .valueOf(enableRcseSwitch)));
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
                    if ((psSignalling = getValueByParamName("psSignalling", childnode, TYPE_TXT)) != null) {
                        if (psSignalling.equals("SIPoUDP")) {
                            mRcsSettings.writeString(
                                    RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_MOBILE,
                                    ListeningPoint.UDP);
                        } else if (psSignalling.equals("SIPoTCP")) {
                            mRcsSettings.writeString(
                                    RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_MOBILE,
                                    ListeningPoint.TCP);
                        } else if (psSignalling.equals("SIPoTLS")) {
                            mRcsSettings.writeString(
                                    RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_MOBILE,
                                    ListeningPoint.TLS);
                        }
                        continue;
                    }
                }

                if (wifiSignalling == null) {
                    if ((wifiSignalling = getValueByParamName("wifiSignalling", childnode, TYPE_TXT)) != null) {
                        if (wifiSignalling.equals("SIPoUDP")) {
                            mRcsSettings.writeString(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_WIFI,
                                    ListeningPoint.UDP);
                        } else if (wifiSignalling.equals("SIPoTCP")) {
                            mRcsSettings.writeString(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_WIFI,
                                    ListeningPoint.TCP);
                        } else if (wifiSignalling.equals("SIPoTLS")) {
                            mRcsSettings.writeString(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_WIFI,
                                    ListeningPoint.TLS);
                        }
                        continue;
                    }
                }

                if (wifiMedia == null) {
                    if ((wifiMedia = getValueByParamName("wifiMedia", childnode, TYPE_TXT)) != null) {
                        if (wifiMedia.equals("MSRP")) {
                            mRcsSettings.writeBoolean(RcsSettingsData.SECURE_MSRP_OVER_WIFI, false);
                        } else if (wifiMedia.equals("MSRPoTLS")) {
                            mRcsSettings.writeBoolean(RcsSettingsData.SECURE_MSRP_OVER_WIFI, true);
                        }
                        continue;
                    }
                }

                if (wifiRtMedia == null) {
                    if ((wifiRtMedia = getValueByParamName("wifiRTMedia", childnode, TYPE_TXT)) != null) {
                        if ("RTP".equals(wifiMedia)) {
                            mRcsSettings.writeBoolean(RcsSettingsData.SECURE_RTP_OVER_WIFI, false);
                        } else if ("SRTP".equals(wifiMedia)) {
                            mRcsSettings.writeBoolean(RcsSettingsData.SECURE_RTP_OVER_WIFI, true);
                        }
                        continue;
                    }
                }

                // Not supported: "psMedia"
                // Not supported: "psRTMedia"

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
        String uuidValue = null;
        String aaIPCallBreakOut = null;
        String csIPCallBreakOut = null;
        String rcsIPVideoCallUpgradeFromCS = null;
        String rcsIPVideoCallUpgradeOnCapError = null;
        String beIPVideoCallUpgradeAttemptEarly = null;
        String maxMsrpLengthExtensions = null;

        Node typenode = null;
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (childnode.getNodeName().equals("characteristic")) {
                    if (childnode.getAttributes().getLength() > 0) {
                        typenode = childnode.getAttributes().getNamedItem("type");
                        if (typenode != null) {
                            if (typenode.getNodeValue().equalsIgnoreCase("transportProto")) {
                                parseTransportProtocol(childnode);
                            }
                        }
                    }
                }

                if (endUserConfReqId == null) {
                    if ((endUserConfReqId = getValueByParamName("endUserConfReqId", childnode,
                            TYPE_TXT)) != null) {
                        mRcsSettings
                                .setEndUserConfirmationRequestUri("".equals(endUserConfReqId) ? null
                                        : formatSipUri(endUserConfReqId.trim()));
                        continue;
                    }
                }

                if (deviceID == null) {
                    if ((deviceID = getValueByParamName("deviceID", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.USE_IMEI_AS_DEVICE_ID,
                                deviceID.equals("0"));
                        continue;
                    }
                }

                if (uuidValue == null) {
                    if ((uuidValue = getValueByParamName(UUID_VALUE, childnode, TYPE_TXT)) != null) {
                        mRcsSettings.writeString(RcsSettingsData.UUID, "".equals(uuidValue) ? null
                                : uuidValue);
                        continue;
                    }
                }

                if (aaIPCallBreakOut == null) {
                    if ((aaIPCallBreakOut = getValueByParamName("IPCallBreakOut", childnode,
                            TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.IPVOICECALL_BREAKOUT_AA,
                                aaIPCallBreakOut.equals("1"));
                        continue;
                    }
                }

                if (csIPCallBreakOut == null) {
                    if ((csIPCallBreakOut = getValueByParamName("IPCallBreakOutCS", childnode,
                            TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.IPVOICECALL_BREAKOUT_CS,
                                csIPCallBreakOut.equals("1"));
                        continue;
                    }
                }

                if (rcsIPVideoCallUpgradeFromCS == null) {
                    if ((rcsIPVideoCallUpgradeFromCS = getValueByParamName(
                            "rcsIPVideoCallUpgradeFromCS", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.IPVIDEOCALL_UPGRADE_FROM_CS,
                                rcsIPVideoCallUpgradeFromCS.equals("1"));
                        continue;
                    }
                }

                if (rcsIPVideoCallUpgradeOnCapError == null) {
                    if ((rcsIPVideoCallUpgradeOnCapError = getValueByParamName(
                            "rcsIPVideoCallUpgradeOnCapError", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.IPVIDEOCALL_UPGRADE_ON_CAPERROR,
                                rcsIPVideoCallUpgradeOnCapError.equals("1"));
                        continue;
                    }
                }

                if (beIPVideoCallUpgradeAttemptEarly == null) {
                    if ((beIPVideoCallUpgradeAttemptEarly = getValueByParamName(
                            "rcsIPVideoCallUpgradeAttemptEarly", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(
                                RcsSettingsData.IPVIDEOCALL_UPGRADE_ATTEMPT_EARLY,
                                beIPVideoCallUpgradeAttemptEarly.equals("1"));
                        continue;
                    }
                }

                if (maxMsrpLengthExtensions == null) {
                    if ((maxMsrpLengthExtensions = getValueByParamName("extensionsMaxMSRPSize",
                            childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeInteger(RcsSettingsData.MAX_MSRP_SIZE_EXTENSIONS,
                                Integer.parseInt(maxMsrpLengthExtensions));
                        continue;
                    }
                }

                // Not supported: "WarnSizeImageShare"

            } while ((childnode = childnode.getNextSibling()) != null);

            /**
             * Check if UUID value is still null at this point. If NULL,then generate it as per
             * RFC4122, section 4.2.
             */
            if (uuidValue == null) {
                mRcsSettings.writeString(RcsSettingsData.UUID, DeviceUtils.generateUUID()
                        .toString());
            }
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
                    if ((conRef = getValueByParamName("ConRef", childnode, TYPE_TXT)) != null) {
                        mRcsSettings.writeString(RcsSettingsData.RCS_APN, "".equals(conRef) ? null
                                : conRef);
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
                            childnode, TYPE_TXT)) != null) {
                        String username = extractUserNamePart(publicUserIdentity.trim());
                        PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(username);
                        if (number == null) {
                            if (sLogger.isActivated()) {
                                sLogger.error("Invalid public user identity '" + username + "'");
                            }
                            mRcsSettings.setUserProfileImsUserName(null);
                        } else {
                            ContactId contact = ContactUtil
                                    .createContactIdFromValidatedData(number);
                            mRcsSettings.setUserProfileImsUserName(contact);
                        }
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
        /*
         * String voiceCall = null; String chat = null; String sendSms = null; String fileTranfer =
         * null; String videoShare = null; String imageShare = null; String geolocPush = null; if
         * (node == null) { return; } Node childnode = node.getFirstChild(); if (childnode != null)
         * { do { if (chat == null) { if ((chat = getValueByParamName("Chat", childnode, TYPE_INT))
         * != null) { if (chat.equals("0")) { mRcsSettings.writeParameter(
         * RcsSettingsData.CAPABILITY_IM_SESSION, RcsSettingsData.TRUE); } else {
         * mRcsSettings.writeParameter( RcsSettingsData.CAPABILITY_IM_SESSION,
         * RcsSettingsData.FALSE); } continue; } } if (fileTranfer == null) { if ((fileTranfer =
         * getValueByParamName("FileTranfer", childnode, TYPE_INT)) != null) { if
         * (fileTranfer.equals("0")) { mRcsSettings.writeParameter(
         * RcsSettingsData.CAPABILITY_FILE_TRANSFER, RcsSettingsData.TRUE); } else {
         * mRcsSettings.writeParameter( RcsSettingsData.CAPABILITY_FILE_TRANSFER,
         * RcsSettingsData.FALSE); } continue; } } if (videoShare == null) { if ((videoShare =
         * getValueByParamName("VideoShare", childnode, TYPE_INT)) != null) { if
         * (videoShare.equals("0")) { mRcsSettings.writeParameter(
         * RcsSettingsData.CAPABILITY_VIDEO_SHARING, RcsSettingsData.TRUE); } else {
         * mRcsSettings.writeParameter( RcsSettingsData.CAPABILITY_VIDEO_SHARING,
         * RcsSettingsData.FALSE); } continue; } } if (imageShare == null) { if ((imageShare =
         * getValueByParamName("ImageShare", childnode, TYPE_INT)) != null) { if
         * (imageShare.equals("0")) { mRcsSettings.writeParameter(
         * RcsSettingsData.CAPABILITY_IMAGE_SHARING, RcsSettingsData.TRUE); } else {
         * mRcsSettings.writeParameter( RcsSettingsData.CAPABILITY_IMAGE_SHARING,
         * RcsSettingsData.FALSE); } continue; } } if (geolocPush == null) { if ((geolocPush =
         * getValueByParamName("GeoLocPush", childnode, TYPE_INT)) != null) { if
         * (geolocPush.equals("0")) { mRcsSettings.writeParameter(
         * RcsSettingsData.CAPABILITY_GEOLOCATION_PUSH, RcsSettingsData.TRUE); } else {
         * mRcsSettings.writeParameter( RcsSettingsData.CAPABILITY_GEOLOCATION_PUSH,
         * RcsSettingsData.FALSE); } continue; } } // Not used for RCS: "SendSms" // Not used for
         * RCS: "VoiceCall" } while ((childnode = childnode.getNextSibling()) != null); }
         */
    }

    /**
     * Parse ext
     * 
     * @param node Node
     */
    private void parseExt(Node node) {
        String intUrlFmt = null;
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
                            String nodeValue = typenode.getNodeValue();
                            if (nodeValue.equalsIgnoreCase("SecondaryDevicePar")) {
                                parseSecondaryDevicePar(childnode);
                            } else if (nodeValue.equalsIgnoreCase("joyn")) {
                                parseRcs(childnode);
                            }
                        }
                    }
                }

                if (intUrlFmt == null) {
                    if ((intUrlFmt = getValueByParamName("IntUrlFmt", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.TEL_URI_FORMAT,
                                intUrlFmt.equals("0"));
                        continue;
                    }
                }

                if (maxSizeImageShare == null) {
                    if ((maxSizeImageShare = getValueByParamName("MaxSizeImageShare", childnode,
                            TYPE_INT)) != null) {
                        long size = Long.parseLong(maxSizeImageShare);
                        mRcsSettings.setMaxImageSharingSize(size);
                        continue;
                    }
                }

                if (maxTimeVideoShare == null) {
                    if ((maxTimeVideoShare = getValueByParamName("MaxTimeVideoShare", childnode,
                            TYPE_INT)) != null) {
                        mRcsSettings.writeLong(RcsSettingsData.MAX_VIDEO_SHARE_DURATION,
                                Long.parseLong(maxTimeVideoShare)
                                        * SECONDS_TO_MILLISECONDS_CONVERSION_RATE);
                        continue;
                    }
                }

                // Not used (all number are formatted in international format): "NatUrlFmt"
                // Not supported: "Q-Value"

            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse ICSI
     * 
     * @param node Node
     */
    private void parseICSI(Node node) {
        // Not used for RCS: "ICSI"
        // Not used for RCS: "ICSI_Resource_Allocation_Mode"
    }

    /**
     * Parse PCSCF address
     * 
     * @param node Node
     */
    private void parsePcscfAddress(Node node) {
        String addr = null;
        if (node == null) {
            return;
        }
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (addr == null) {
                    if ((addr = getValueByParamName("Address", childnode, TYPE_TXT)) != null) {
                        String[] address = addr.split(":");
                        String proxyAddr = address[0];
                        mRcsSettings.setImsProxyAddrForMobile("".equals(proxyAddr) ? null
                                : proxyAddr);
                        mRcsSettings
                                .setImsProxyAddrForWifi("".equals(proxyAddr) ? null : proxyAddr);
                        if (address.length > 1) {
                            int port = Integer.valueOf(address[1]);
                            mRcsSettings.setImsProxyPortForMobile(port);
                            mRcsSettings.setImsProxyPortForWifi(port);
                        }
                        continue;
                    }
                }

                // Not used: "AddressType"

            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse phone context
     * 
     * @param node Node
     */
    private void parsePhoneContextList(Node node) {
        // Not used: "PhoneContext"
        // Not used: "Public_user_identity"
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
                    if ((authType = getValueByParamName("AuthType", childnode, TYPE_TXT)) != null) {
                        if (authType.equals("EarlyIMS")) {
                            mRcsSettings
                                    .setImsAuthenticationProcedureForMobile(AuthenticationProcedure.GIBA);
                        } else {
                            mRcsSettings
                                    .setImsAuthenticationProcedureForMobile(AuthenticationProcedure.DIGEST);
                        }
                        continue;
                    }
                }

                if (realm == null) {
                    if ((realm = getValueByParamName("Realm", childnode, TYPE_TXT)) != null) {
                        mRcsSettings.setUserProfileImsRealm("".equals(realm) ? null : realm);
                        continue;
                    }
                }

                if (userName == null) {
                    if ((userName = getValueByParamName("UserName", childnode, TYPE_TXT)) != null) {
                        mRcsSettings.setUserProfileImsPrivateId("".equals(userName) ? null
                                : userName);
                        continue;
                    }
                }

                if (userPwd == null) {
                    if ((userPwd = getValueByParamName("UserPwd", childnode, TYPE_TXT)) != null) {
                        mRcsSettings.setUserProfileImsPassword("".equals(userPwd) ? null : userPwd);
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
                    NamedNodeMap attributes = childnode.getAttributes();
                    if (attributes.getLength() > 0) {
                        typenode = attributes.getNamedItem("type");
                        if (typenode != null) {
                            String nodeValue = typenode.getNodeValue();
                            if (nodeValue.equalsIgnoreCase("IMS")) {
                                parseIMS(childnode);
                            } else if (nodeValue.equalsIgnoreCase("PRESENCE")) {
                                parsePresence(childnode);
                            } else if (nodeValue.equalsIgnoreCase("XDMS")) {
                                parseXDMS(childnode);
                            } else if (nodeValue.equalsIgnoreCase("IM")) {
                                parseIM(childnode);
                            } else if (nodeValue.equalsIgnoreCase("CAPDISCOVERY")) {
                                parseCapabilityDiscovery(childnode);
                            } else if (nodeValue.equalsIgnoreCase("APN")) {
                                parseAPN(childnode);
                            } else if (nodeValue.equalsIgnoreCase("OTHER")) {
                                parseOther(childnode);
                            } else if (nodeValue.equalsIgnoreCase("SERVICES")) {
                                parseServices(childnode);
                            } else if (nodeValue.equalsIgnoreCase("SUPL")) {
                                parseSupl(childnode);
                            } else if (nodeValue.equalsIgnoreCase("SERVICEPROVIDEREXT")) {
                                parseServiceProviderExt(childnode);
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
        String timert1 = null;
        String timert2 = null;
        String timert4 = null;
        String privateUserIdentity = null;
        String homeDomain = null;
        String keepAliveEnabled = null;
        String regRetryBasetime = null;
        String regRetryMaxtime = null;
        Node typenode = null;
        Node childnode = node.getFirstChild();

        if (childnode != null) {
            do {
                if (childnode.getNodeName().equals("characteristic")) {
                    if (childnode.getAttributes().getLength() > 0) {
                        typenode = childnode.getAttributes().getNamedItem("type");
                        if (typenode != null) {
                            if (typenode.getNodeValue().equalsIgnoreCase("ConRefs")) {
                                parseConRefs(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase(
                                    "Public_user_identity_List")) {
                                parsePublicUserIdentity(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("Ext")) {
                                parseExt(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("ICSI_List")) {
                                parseICSI(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase(
                                    "LBO_P-CSCF_Address")) {
                                parsePcscfAddress(childnode);
                            } else if (typenode.getNodeValue()
                                    .equalsIgnoreCase("PhoneContext_List")) {
                                parsePhoneContextList(childnode);
                            } else if (typenode.getNodeValue().equalsIgnoreCase("APPAUTH")) {
                                parseAppAuthent(childnode);
                            }
                        }
                    }
                }

                if (timert1 == null) {
                    if ((timert1 = getValueByParamName("Timer_T1", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeLong(RcsSettingsData.SIP_TIMER_T1,
                                Long.parseLong(timert1));
                        continue;
                    }
                }

                if (timert2 == null) {
                    if ((timert2 = getValueByParamName("Timer_T2", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeLong(RcsSettingsData.SIP_TIMER_T2,
                                Long.parseLong(timert2));
                        continue;
                    }
                }

                if (timert4 == null) {
                    if ((timert4 = getValueByParamName("Timer_T4", childnode, TYPE_INT)) != null) {
                        mRcsSettings.writeLong(RcsSettingsData.SIP_TIMER_T4,
                                Long.parseLong(timert4));
                        continue;
                    }
                }

                if (privateUserIdentity == null) {
                    if ((privateUserIdentity = getValueByParamName("Private_User_Identity",
                            childnode, TYPE_TXT)) != null) {
                        mRcsSettings
                                .setUserProfileImsPrivateId("".equals(privateUserIdentity) ? null
                                        : privateUserIdentity);
                        continue;
                    }
                }

                if (homeDomain == null) {
                    if ((homeDomain = getValueByParamName("Home_network_domain_name", childnode,
                            TYPE_TXT)) != null) {
                        mRcsSettings.setUserProfileImsDomain("".equals(homeDomain) ? null
                                : homeDomain);
                        continue;
                    }
                }

                if (keepAliveEnabled == null) {
                    if ((keepAliveEnabled = getValueByParamName("Keep_Alive_Enabled", childnode,
                            TYPE_INT)) != null) {
                        mRcsSettings.writeBoolean(RcsSettingsData.SIP_KEEP_ALIVE,
                                keepAliveEnabled.equals("1"));
                        continue;
                    }
                }

                if (regRetryBasetime == null) {
                    if ((regRetryBasetime = getValueByParamName("RegRetryBaseTime", childnode,
                            TYPE_INT)) != null) {
                        mRcsSettings.writeLong(RcsSettingsData.REGISTER_RETRY_BASE_TIME,
                                Long.parseLong(regRetryBasetime)
                                        * SECONDS_TO_MILLISECONDS_CONVERSION_RATE);
                        continue;
                    }
                }

                if (regRetryMaxtime == null) {
                    if ((regRetryMaxtime = getValueByParamName("RegRetryMaxTime", childnode,
                            TYPE_INT)) != null) {
                        mRcsSettings.writeLong(RcsSettingsData.REGISTER_RETRY_MAX_TIME,
                                Long.parseLong(regRetryMaxtime)
                                        * SECONDS_TO_MILLISECONDS_CONVERSION_RATE);
                        continue;
                    }
                }

                // Not supported under Android: "PDP_ContextOperPref"
                // Not used for RCS: "Voice_Domain_Preference_E_UTRAN"
                // Not used for RCS: "SMS_Over_IP_Networks_Indication"
                // Not used for RCS: "Voice_Domain_Preference_UTRAN"
                // Not used for RCS: "Mobility_Management_IMS_Voice_Termination"

            } while ((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Get value of a parameter
     * 
     * @param paramName Parameter name
     * @param node Node
     * @param type Parameter type
     * @return Value or null
     */
    private String getValueByParamName(String paramName, Node node, int type) {
        Node nameNode = null;
        Node valueNode = null;
        if ((node == null)
                || !(node.getNodeName().equals("parm") || node.getNodeName().equals("param"))) {
            return null;
        }

        if (node.getAttributes().getLength() > 0) {
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
                // Check type
                if (type == TYPE_INT) {
                    try {
                        Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        if (sLogger.isActivated()) {
                            sLogger.warn("Bad value for integer parameter " + paramName);
                        }
                        return null;
                    }
                }

                return value;
            }
            return null;
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
        int indexOfSipHeader = uri.indexOf(PhoneUtils.SIP_URI_HEADER);
        if (indexOfSipHeader != -1) {
            int startIndexOfUriAddress = uri.indexOf("@", indexOfSipHeader);
            return uri.substring(indexOfSipHeader + PhoneUtils.SIP_URI_HEADER.length(),
                    startIndexOfUriAddress);

        }
        return uri;
    }

    /**
     * Format to SIP-URI
     * 
     * @param path Sip Uri path
     * @return SIP-URI
     */
    private Uri formatSipUri(String path) {
        return path.startsWith(PhoneUtils.SIP_URI_HEADER) ? Uri.parse(path) : Uri
                .parse(new StringBuilder(PhoneUtils.SIP_URI_HEADER).append(path).toString());
    }
}
