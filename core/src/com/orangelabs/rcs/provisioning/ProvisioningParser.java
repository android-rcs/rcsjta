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
import com.orangelabs.rcs.provider.settings.RcsSettingsData.AuthenticationProcedure;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.DefaultMessagingMethod;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.FileTransferProtocol;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.GsmaRelease;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.MessagingMode;
import com.orangelabs.rcs.utils.logger.Logger;

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
	
	/**
     * Provisioning info
     */
    private ProvisioningInfo provisioningInfo = new ProvisioningInfo();

    /**
     * Content
     */
    private String content;
    
    private boolean first = false;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());
    
	/**
	 * Enumerated type for the root node
	 *
	 */
	private enum RootNodeType {
		VERS, TOKEN, MSG, APPLICATION, IMS, PRESENCE, XDMS, IM, CAPDISCOVERY, APN, OTHER, SERVICES, SUPL, SERVICEPROVIDEREXT, UX
	};

	/**
	 * Enumerated type for the IMS server version
	 *
	 */
	enum ImsServerVersion {
		JOYN, NON_JOYN
	};
	
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
	 * @param release
	 *            The GSMA release (Albatros, Blackbird, Crane...) before parsing
	 * @param first
	 *            True if it is a first provisioning
	 * @return Boolean result
	 * 
	 *         <p>
	 *         <b>Be Careful:</b><br />
	 *         GSMA release is set to blackbird if SERVICES node is present, otherwise release is unchanged
	 *         </p>
	 */
	public boolean parse(GsmaRelease release, boolean first) {
        try {
            if (logger.isActivated()) {
                logger.debug("Start the parsing of content first="+first);
            }
            this.first = first;
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

            if (logger.isActivated()) {
                logger.debug("Parsed Doc ="+doc);
            }
            
            Node rootnode = doc.getDocumentElement();
            Node childnode = rootnode.getFirstChild();
            if (childnode == null) {
                if (logger.isActivated()) {
                    logger.debug("The first chid node is null");
                }
                return false;
            }

            int nodeNumber = 0;
            do {
                if (childnode.getNodeName().equals("characteristic")) {
                    if (childnode.getAttributes().getLength() > 0) {
                        Node typenode = childnode.getAttributes().getNamedItem("type");
                        if (typenode != null && typenode.getNodeValue() != null) {
                            if (logger.isActivated()) {
                                logger.debug("Node " + childnode.getNodeName() + " with type "
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
									if (logger.isActivated()) {
										logger.warn("unhandled node type: " + nodeType);
									}
								}
							} catch (IllegalArgumentException e) {
								if (logger.isActivated()) {
									logger.warn("invalid node type: " + nodeType);
								}
							}
                        }
                    }
                }
            } while((childnode = childnode.getNextSibling()) != null);
            if (nodeNumber ==1) {
				// We received a single node (the version one) !
				// This is the case if the version number is negative or in order to extend the validity of the provisioning.
				// In that case we restore the relevant GSMA release saved before parsing.
				RcsSettings.getInstance().setGsmaRelease(release);
            }
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
        Node versionchild = node.getFirstChild();

        if (versionchild != null) {
            do {
                if (version == null) {
                    if ((version = getValueByParamName("version", versionchild, TYPE_TXT)) != null) {
                        provisioningInfo.setVersion(version);
                        continue;
                    }
                }
                if (validity == null) {
                    if ((validity = getValueByParamName("validity", versionchild, TYPE_INT)) != null) {
                        provisioningInfo.setValidity(Long.parseLong(validity));
                        continue;
                    }
                }
            } while((versionchild = versionchild.getNextSibling()) != null);
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
            } while((tokenChild = tokenChild.getNextSibling()) != null);
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
                        provisioningInfo.setTitle(title);
                        continue;
                    }
                }
                if (message == null) {
                    if ((message = getValueByParamName("message", childnode, TYPE_TXT)) != null) {
                        provisioningInfo.setMessage(message);
                        continue;
                    }
                }
                if (acceptBtn == null) {
                    if ((acceptBtn = getValueByParamName("Accept_btn", childnode, TYPE_INT)) != null) {
                        if (acceptBtn.equals("1")) {
                            provisioningInfo.setAcceptBtn(true);
                        } else {
                            provisioningInfo.setAcceptBtn(false);
                        }
                        continue;
                    }
                }
                if (rejectBtn == null) {
                    if ((rejectBtn = getValueByParamName("Reject_btn", childnode, TYPE_INT)) != null) {
                        if (rejectBtn.equals("1")) {
                            provisioningInfo.setRejectBtn(true);
                        } else {
                            provisioningInfo.setRejectBtn(false);
                        }
                        continue;
                    }
                }
            } while((childnode = childnode.getNextSibling()) != null);
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
            } while((childnode = childnode.getNextSibling()) != null);
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
                            } else
                            if (typenode.getNodeValue().equalsIgnoreCase("SERVCAPWATCH")) {
                                parsePresenceWatcher(childnode);
                            } else
                            if (typenode.getNodeValue().equalsIgnoreCase("ServCapPresentity")) {
                                parsePresentityWatcher(childnode);
                            }
                        }
                    }
                }

                if (usePresence == null) {
                    if ((usePresence = getValueByParamName("usePresence", childnode, TYPE_INT)) != null) {
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE, !usePresence.equals("0"));
                        continue;
                    }
                }

                if (presencePrfl == null) {
					if ((presencePrfl = getValueByParamName("presencePrfl", childnode, TYPE_INT)) != null) {
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY,
								!presencePrfl.equals("0"));
						continue;
					}
                }

                if (iconMaxSize == null) {
                    if ((iconMaxSize = getValueByParamName("IconMaxSize", childnode, TYPE_INT)) != null) {
        				int kb = Integer.parseInt(iconMaxSize) / 1024;
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.MAX_PHOTO_ICON_SIZE, ""+kb);
                        continue;
                    }
                }

                if (noteMaxSize == null) {
                    if ((noteMaxSize = getValueByParamName("NoteMaxSize", childnode, TYPE_INT)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.MAX_FREETXT_LENGTH, noteMaxSize);
                        continue;
                    }
                }

                if (publishTimer == null) {
                    if ((publishTimer = getValueByParamName("PublishTimer", childnode, TYPE_INT)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.PUBLISH_EXPIRE_PERIOD, publishTimer);
                        continue;
                    }
                }

                // Not supported: "AvailabilityAuth"
                // Not supported: "client-obj-datalimit"
                // Not used for RCS: "content-serveruri"
                // Not supported: "source-throttlepublish"
                // Not supported: "max-number-ofsubscriptions-inpresence-list"
                // TODO: "service-uritemplate"
                
            } while((childnode = childnode.getNextSibling()) != null);
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
			// Node "SERVICES" is mandatory in GSMA release Blackbird and not present in previous one Albatros.
			// Only if the parsing result contains a SERVICE tree, Blackbird is assumed as release.
			// This trick is used to detect the GSMA release as provisioned by the network.
			RcsSettings.getInstance().setGsmaRelease(GsmaRelease.BLACKBIRD);
            do {

                if (chatAuth == null) {
					if ((chatAuth = getValueByParamName("ChatAuth", childnode, TYPE_INT)) != null) {
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.CAPABILITY_IM_SESSION, chatAuth.equals("1"));
						continue;
					}
                }

            	 if (groupChatAuth == null) {
					if ((groupChatAuth = getValueByParamName("groupChatAuth", childnode, TYPE_INT)) != null) {
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.CAPABILITY_IM_GROUP_SESSION,
								groupChatAuth.equals("1"));
						continue;
					}
                 }

                if (ftAuth == null) {
					if ((ftAuth = getValueByParamName("ftAuth", childnode, TYPE_INT)) != null) {
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.CAPABILITY_FILE_TRANSFER, ftAuth.equals("1"));
						continue;
					}
                }

                if (vsAuth == null) {
					if ((vsAuth = getValueByParamName("vsAuth", childnode, TYPE_INT)) != null) {
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.CAPABILITY_VIDEO_SHARING, vsAuth.equals("1"));
						continue;
					}
                }

                if (isAuth == null) {
					if ((isAuth = getValueByParamName("isAuth", childnode, TYPE_INT)) != null) {
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.CAPABILITY_IMAGE_SHARING, isAuth.equals("1"));
						continue;
					}
                }
                
				if (geolocPushAuth == null) {
					if ((geolocPushAuth = getValueByParamName("geolocPushAuth", childnode, TYPE_INT)) != null) {
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.CAPABILITY_GEOLOCATION_PUSH,
								geolocPushAuth.equals("1"));
						continue;
					}
				}

                if (presencePrfl == null) {
					if ((presencePrfl = getValueByParamName("presencePrfl", childnode, TYPE_INT)) != null) {
						RcsSettings.getInstance()
								.writeBoolean(RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE, presencePrfl.equals("1"));
						continue;
					}
                }
                
                if (rcsIPVoiceCallAuth == null) {
					if ((rcsIPVoiceCallAuth = getValueByParamName("rcsIPVoiceCallAuth", childnode, TYPE_INT)) != null) {
						int value = Integer.decode(rcsIPVoiceCallAuth);
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.CAPABILITY_IP_VOICE_CALL, (value % 16) != 0);
						continue;
					}
                }
                
                if (rcsIPVideoCallAuth == null) {
					if ((rcsIPVideoCallAuth = getValueByParamName("rcsIPVideoCallAuth", childnode, TYPE_INT)) != null) {
						int value = Integer.decode(rcsIPVoiceCallAuth);
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.CAPABILITY_IP_VIDEO_CALL, (value % 16) != 0);
						continue;
					}
                }
                
                if (allowExtensions == null) {
					if ((allowExtensions = getValueByParamName("allowRCSExtensions", childnode, TYPE_INT)) != null) {
						int value = Integer.decode(allowExtensions);
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.ALLOW_EXTENSIONS, (value % 16) != 0);
						continue;
					}
                }

                // Not used: "standaloneMsgAuth"
                // Not used: "geolocPullAuth"
                
            } while((childnode = childnode.getNextSibling()) != null);
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
                        RcsSettings.getInstance().writeParameter(RcsSettingsData.REVOKE_TIMEOUT,
                                revokeTimer);
                        continue;
                    }
                }

                if (xcapRootURI == null) {
                    if ((xcapRootURI = getValueByParamName("XCAPRootURI", childnode, TYPE_TXT)) != null) {
                        RcsSettings.getInstance().writeParameter(RcsSettingsData.XDM_SERVER, xcapRootURI);
                        continue;
                    }
                }

                if (xcapAuthenticationUsername == null) {
                    if ((xcapAuthenticationUsername = getValueByParamName(
                            "XCAPAuthenticationUserName", childnode, TYPE_TXT)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.XDM_LOGIN,
                                xcapAuthenticationUsername);
                        continue;
                    }
                }

                if (xcapAuthenticationSecret == null) {
                    if ((xcapAuthenticationSecret = getValueByParamName(
                            "XCAPAuthenticationSecret", childnode, TYPE_TXT)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.XDM_PASSWORD,
                                xcapAuthenticationSecret);
                        continue;
                    }
                }

                // Not used (only Digest is used): "XCAPAuthenticationType"
                
            } while((childnode = childnode.getNextSibling()) != null);
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
                        RcsSettings.getInstance().writeParameter(RcsSettingsData.MAX_GEOLOC_LABEL_LENGTH,
                        		textMaxLength);
                        continue;
                    }
                }

                if (locInfoMaxValidTime == null) {
                    if ((locInfoMaxValidTime = getValueByParamName("LocInfoMaxValidTime", childnode, TYPE_INT)) != null) {
                        RcsSettings.getInstance().writeParameter(RcsSettingsData.GEOLOC_EXPIRATION_TIME, locInfoMaxValidTime);
                        continue;
                    }
                }

                // Not used: "geolocPullOpenValue"
                // Not used: "geolocPullApiGwAddress"
                // Not used: "geolocPullBlockTimer"
                
            } while((childnode = childnode.getNextSibling()) != null);
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
     * Parse rcs
     * 
     * @param node Node
     */
    private void parseRcs(Node node) {
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
                            if (typenode.getNodeValue().equalsIgnoreCase("UX")) {
                                parseUx(childnode, ImsServerVersion.JOYN);
                            }
                        }
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
							RcsSettings.getInstance().setMessagingMode(MessagingMode.INTEGRATED);
						} else {
							if (ImsServerVersion.JOYN.equals(isJoyn)) {
								RcsSettings.getInstance().setMessagingMode(MessagingMode.CONVERGED);
							} else {
								RcsSettings.getInstance().setMessagingMode(MessagingMode.SEAMLESS);
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
        String maxConcurrentSession = null;
        RcsSettings rcsSettings = RcsSettings.getInstance();
        Node childnode = node.getFirstChild();
        
        if (childnode != null) {
            do {
            	if (imCapAlwaysOn == null) {
					if ((imCapAlwaysOn = getValueByParamName("imCapAlwaysON", childnode, TYPE_INT)) != null) {
						boolean _imCapAlwaysOn = !imCapAlwaysOn.equals("0");
						rcsSettings.writeBoolean(RcsSettingsData.IM_CAPABILITY_ALWAYS_ON, _imCapAlwaysOn);
						// set default IM messaging method if first provisioning
						if (first) {
							if (_imCapAlwaysOn) {
								rcsSettings.setDefaultMessagingMethod(DefaultMessagingMethod.RCS);
							} else {
								rcsSettings.setDefaultMessagingMethod(DefaultMessagingMethod.AUTOMATIC);
							}
						}
						continue;
					}
                }
            	
            	if (ftCapAlwaysOn == null) {
					if ((ftCapAlwaysOn = getValueByParamName("ftCapAlwaysON", childnode, TYPE_INT)) != null) {
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.FT_CAPABILITY_ALWAYS_ON, !ftCapAlwaysOn.equals("0"));
						continue;
					}
                }

                if (maxConcurrentSession == null) {
                    if ((maxConcurrentSession = getValueByParamName("MaxConcurrentSession", childnode, TYPE_INT)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.MAX_CHAT_SESSIONS,
                                maxConcurrentSession);
                        continue;
                    }
                }

                if (groupChatSF == null) {
					if ((groupChatSF = getValueByParamName("GroupChatFullStandFwd", childnode, TYPE_INT)) != null) {
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.CAPABILITY_GROUP_CHAT_SF, !groupChatSF.equals("0"));
						continue;
					}
                }
                
                if (imWarnSF == null) {
					if ((imWarnSF = getValueByParamName("imWarnSF", childnode, TYPE_INT)) != null) {
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.WARN_SF_SERVICE, !imWarnSF.equals("0"));
						continue;
					}
                }

                if (autoAcceptFt == null) {
                    if ((autoAcceptFt = getValueByParamName("ftAutAccept", childnode, TYPE_INT)) != null) {
                        boolean aaModeChangeable = !autoAcceptFt.equals("0");
                        // Check if first provisioning or transition of MNO setting
                        if (first || (aaModeChangeable != rcsSettings.isFtAutoAcceptedModeChangeable())) {
                        	// Save first or new setting value
                        	rcsSettings.setFtAutoAcceptedModeChangeable(aaModeChangeable);
                        	if (aaModeChangeable) {
                        		// Enforce user settings for AA to default value
            					// By default, AA is enabled in normal conditions
            					rcsSettings.setFileTransferAutoAccepted(true);
            					// By default, AA is disabled in roaming
            					rcsSettings.setFileTransferAutoAcceptedInRoaming(false);
                        	} else {
                        		// Enforce user settings for AA for normal conditions and roaming
                				rcsSettings.setFileTransferAutoAccepted(false);
                				rcsSettings.setFileTransferAutoAcceptedInRoaming(false);
                        	}
                        }
                        continue;
                    }
                }

                if (ftSF == null) {
					if ((ftSF = getValueByParamName("ftStAndFwEnabled", childnode, TYPE_INT)) != null) {
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.CAPABILITY_FILE_TRANSFER_SF, !ftSF.equals("0"));
						continue;
					}
                }

                if (ftHttpCsUri == null) {
                    if ((ftHttpCsUri = getValueByParamName("ftHTTPCSURI", childnode, TYPE_TXT)) != null) {
                        RcsSettings.getInstance().writeParameter(RcsSettingsData.FT_HTTP_SERVER, ftHttpCsUri);
                        continue;
                    }
                }

                if (ftHttpCsUser == null) {
                    if ((ftHttpCsUser = getValueByParamName("ftHTTPCSUser", childnode, TYPE_TXT)) != null) {
                        RcsSettings.getInstance().writeParameter(RcsSettingsData.FT_HTTP_LOGIN, ftHttpCsUser);
                        continue;
                    }
                }

                if (ftHttpCsPwd == null) {
                    if ((ftHttpCsPwd = getValueByParamName("ftHTTPCSPwd", childnode, TYPE_TXT)) != null) {
                        RcsSettings.getInstance().writeParameter(RcsSettingsData.FT_HTTP_PASSWORD, ftHttpCsPwd);
                        continue;
                    }
                }

                if (ftDefaultMech == null) {
                    if ((ftDefaultMech = getValueByParamName("ftDefaultMech", childnode, TYPE_TXT)) != null) {
                    	FileTransferProtocol protocol = FileTransferProtocol.valueOf(ftDefaultMech);
                        RcsSettings.getInstance().setFtProtocol(protocol);
                        continue;
                    }
                }

                if (imSessionStart == null) {
                    if ((imSessionStart = getValueByParamName("imSessionStart", childnode, TYPE_INT)) != null) {
                        RcsSettings.getInstance().writeParameter(RcsSettingsData.IM_SESSION_START,
                                imSessionStart);
                        continue;
                    }
                }

                if (ftWarnSize == null) {
                    if ((ftWarnSize = getValueByParamName("ftWarnSize", childnode, TYPE_INT)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.WARN_FILE_TRANSFER_SIZE, ftWarnSize);
                        continue;
                    }
                }

                if (chatAuth == null) {
					if ((chatAuth = getValueByParamName("ChatAuth", childnode, TYPE_INT)) != null) {
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.CAPABILITY_IM_SESSION, !chatAuth.equals("0"));
						continue;
					}
                }

                if (smsFallBackAuth == null) {
					if ((smsFallBackAuth = getValueByParamName("SmsFallBackAuth", childnode, TYPE_INT)) != null) {
						// Careful:
						// 0- Indicates authorization is ok
						// 1- Indicates authorization is non ok
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.SMS_FALLBACK_SERVICE, smsFallBackAuth.equals("0"));
						continue;
					}
                }

                if (autoAcceptChat == null) {
					if ((autoAcceptChat = getValueByParamName("AutAccept", childnode, TYPE_INT)) != null) {
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.AUTO_ACCEPT_CHAT, !autoAcceptChat.equals("0"));
						continue;
					}
                }

                if (autoAcceptGroupChat == null) {
					if ((autoAcceptGroupChat = getValueByParamName("AutAcceptGroupChat", childnode, TYPE_INT)) != null) {
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.AUTO_ACCEPT_GROUP_CHAT,
								!autoAcceptGroupChat.equals("0"));
						continue;
					}
                }

                if (maxSize1to1 == null) {
                    if ((maxSize1to1 = getValueByParamName("MaxSize1to1", childnode, TYPE_INT)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.MAX_CHAT_MSG_LENGTH, maxSize1to1);
                        continue;
                    }
                }

                if (maxSize1toM == null) {
                    if ((maxSize1toM = getValueByParamName("MaxSize1toM", childnode, TYPE_INT)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.MAX_GROUPCHAT_MSG_LENGTH, maxSize1toM);
                        continue;
                    }
                }

                if (timerIdle == null) {
                    if ((timerIdle = getValueByParamName("TimerIdle", childnode, TYPE_INT)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.CHAT_IDLE_DURATION, timerIdle);
                        continue;
                    }
                }

                if (maxSizeFileTransfer == null) {
                    if ((maxSizeFileTransfer = getValueByParamName("MaxSizeFileTr", childnode, TYPE_INT)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.MAX_FILE_TRANSFER_SIZE, maxSizeFileTransfer);
                        continue;
                    }
                }

                if (ftThumb == null) {
					if ((ftThumb = getValueByParamName("ftThumb", childnode, TYPE_INT)) != null) {
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.CAPABILITY_FILE_TRANSFER_THUMBNAIL,
								!ftThumb.equals("0"));
						continue;
					}
                }
                
                if (maxAdhocGroupSize == null) {
                    if ((maxAdhocGroupSize = getValueByParamName("max_adhoc_group_size", childnode, TYPE_INT)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.MAX_CHAT_PARTICIPANTS, maxAdhocGroupSize);
                        continue;
                    }
                }

                if (confFctyUri == null) {
                    if ((confFctyUri = getValueByParamName("conf-fcty-uri", childnode, TYPE_TXT)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.IM_CONF_URI, formatSipUri(confFctyUri));
                        continue;
                    }
                }

                // Not used for RCS: "pres-srv-cap"
                // Not used for RCS: "deferred-msg-func-uri"
                // Not used for RCS: "exploder-uri"
                
            } while((childnode = childnode.getNextSibling()) != null);
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
                    if ((pollingPeriod = getValueByParamName("pollingPeriod", childnode, TYPE_INT)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.CAPABILITY_POLLING_PERIOD, pollingPeriod);
                        continue;
                    }
                }

                if (capInfoExpiry == null) {
                    if ((capInfoExpiry = getValueByParamName("capInfoExpiry", childnode, TYPE_INT)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.CAPABILITY_EXPIRY_TIMEOUT, capInfoExpiry);
                        continue;
                    }
                }

                if (presenceDiscovery == null) {
					if ((presenceDiscovery = getValueByParamName("presenceDisc", childnode, TYPE_INT)) != null) {
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY,
								!presenceDiscovery.equals("0"));
						continue;
					}
                }
            } while((childnode = childnode.getNextSibling()) != null);
        }
    }

    /**
     * Parse APN
     *
     * @param node Node
     */
    private void parseAPN(Node node) {
        // Not supported: "rcseOnlyAPN"
        // Not supported: "enableRcseSwitch"
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
	                        RcsSettings.getInstance().writeParameter(
	                        		RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_MOBILE,
	                                ListeningPoint.UDP);
                        } else
                        if (psSignalling.equals("SIPoTCP")) {
	                        RcsSettings.getInstance().writeParameter(
	                        		RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_MOBILE,
	                                ListeningPoint.TCP);
                        } else
                        if (psSignalling.equals("SIPoTLS")) {
	                        RcsSettings.getInstance().writeParameter(
	                        		RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_MOBILE,
	                                ListeningPoint.TLS);
                        }
                        continue;
                    }
                }

                if (wifiSignalling == null) {
                    if ((wifiSignalling = getValueByParamName("wifiSignalling", childnode, TYPE_TXT)) != null) {
                        if (wifiSignalling.equals("SIPoUDP")) {
	                        RcsSettings.getInstance().writeParameter(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_WIFI,
	                                ListeningPoint.UDP);
                        } else
                        if (wifiSignalling.equals("SIPoTCP")) {
	                        RcsSettings.getInstance().writeParameter(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_WIFI,
	                                ListeningPoint.TCP);
                        } else
                        if (wifiSignalling.equals("SIPoTLS")) {
	                        RcsSettings.getInstance().writeParameter(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_WIFI,
	                                ListeningPoint.TLS);
                        }
                        continue;
                    }
                }

                if (wifiMedia == null) {
                    if ((wifiMedia = getValueByParamName("wifiMedia", childnode, TYPE_TXT)) != null) {
						if (wifiMedia.equals("MSRP")) {
							RcsSettings.getInstance().writeBoolean(RcsSettingsData.SECURE_MSRP_OVER_WIFI, false);
						} else if (wifiMedia.equals("MSRPoTLS")) {
							RcsSettings.getInstance().writeBoolean(RcsSettingsData.SECURE_MSRP_OVER_WIFI, true);
						}
                        continue;
                    }
                }

                if (wifiRtMedia == null) {
                    if ((wifiRtMedia = getValueByParamName("wifiRTMedia", childnode, TYPE_TXT)) != null) {
						if (wifiMedia.equals("RTP")) {
							RcsSettings.getInstance().writeBoolean(RcsSettingsData.SECURE_RTP_OVER_WIFI, false);
						} else if (wifiMedia.equals("SRTP")) {
							RcsSettings.getInstance().writeBoolean(RcsSettingsData.SECURE_RTP_OVER_WIFI, true);
						}
                        continue;
                    }
                }
                
                // Not supported: "psMedia"
                // Not supported: "psRTMedia"
                
            } while((childnode = childnode.getNextSibling()) != null);
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
                    if ((endUserConfReqId = getValueByParamName("endUserConfReqId", childnode, TYPE_TXT)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.ENDUSER_CONFIRMATION_URI, formatSipUri(endUserConfReqId));
                        continue;
                    }
                }

                if (deviceID == null) {
					if ((deviceID = getValueByParamName("deviceID", childnode, TYPE_INT)) != null) {
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.USE_IMEI_AS_DEVICE_ID, deviceID.equals("0"));
						continue;
					}
                }
                
                if (aaIPCallBreakOut == null) {
                    if ((aaIPCallBreakOut = getValueByParamName("IPCallBreakOut", childnode, TYPE_INT)) != null) {
                        RcsSettings.getInstance().writeBoolean(RcsSettingsData.IPVOICECALL_BREAKOUT_AA, aaIPCallBreakOut.equals("1"));
                        continue;
                    }
                }
                
                if (csIPCallBreakOut == null) {
                    if ((csIPCallBreakOut = getValueByParamName("IPCallBreakOutCS", childnode, TYPE_INT)) != null) {
                        RcsSettings.getInstance().writeBoolean(RcsSettingsData.IPVOICECALL_BREAKOUT_CS, csIPCallBreakOut.equals("1"));
                        continue;
                    }
                }
                
                if (rcsIPVideoCallUpgradeFromCS == null) {
					if ((rcsIPVideoCallUpgradeFromCS = getValueByParamName("rcsIPVideoCallUpgradeFromCS", childnode, TYPE_INT)) != null) {
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.IPVIDEOCALL_UPGRADE_FROM_CS,
								rcsIPVideoCallUpgradeFromCS.equals("1"));
						continue;
					}
                }
                
                if (rcsIPVideoCallUpgradeOnCapError == null) {
					if ((rcsIPVideoCallUpgradeOnCapError = getValueByParamName("rcsIPVideoCallUpgradeOnCapError", childnode,
							TYPE_INT)) != null) {
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.IPVIDEOCALL_UPGRADE_ON_CAPERROR,
								rcsIPVideoCallUpgradeOnCapError.equals("1"));
						continue;
					}
                }
                
                if (beIPVideoCallUpgradeAttemptEarly == null) {
					if ((beIPVideoCallUpgradeAttemptEarly = getValueByParamName("rcsIPVideoCallUpgradeAttemptEarly", childnode,
							TYPE_INT)) != null) {
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.IPVIDEOCALL_UPGRADE_ATTEMPT_EARLY,
								beIPVideoCallUpgradeAttemptEarly.equals("1"));
						continue;
					}
                }
                
                if (maxMsrpLengthExtensions == null) {
                	if ((maxMsrpLengthExtensions = getValueByParamName("extensionsMaxMSRPSize", childnode, TYPE_INT)) != null) {
                        RcsSettings.getInstance().writeParameter(RcsSettingsData.MAX_MSRP_SIZE_EXTENSIONS, maxMsrpLengthExtensions);
                        continue;
                    }
                }

                // Not supported: "WarnSizeImageShare"
                
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
                    if ((conRef = getValueByParamName("ConRef", childnode, TYPE_TXT)) != null) {
                        RcsSettings.getInstance().writeParameter(RcsSettingsData.RCS_APN, conRef);
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
                    if ((publicUserIdentity = getValueByParamName("Public_User_Identity", childnode, TYPE_TXT)) != null) {
                    	String username = extractUserNamePart(publicUserIdentity);
                    	RcsSettings.getInstance().setUserProfileImsUserName(username);
                        continue;
                    }
                }
            } while((childnode = childnode.getNextSibling()) != null);
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
        String voiceCall = null;
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
                if (chat == null) {
                    if ((chat = getValueByParamName("Chat", childnode, TYPE_INT)) != null) {
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

                if (fileTranfer == null) {
                    if ((fileTranfer = getValueByParamName("FileTranfer", childnode, TYPE_INT)) != null) {
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
                    if ((videoShare = getValueByParamName("VideoShare", childnode, TYPE_INT)) != null) {
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
                    if ((imageShare = getValueByParamName("ImageShare", childnode, TYPE_INT)) != null) {
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
                    if ((geolocPush = getValueByParamName("GeoLocPush", childnode, TYPE_INT)) != null) {
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
                
	        	// Not used for RCS: "SendSms"
	        	// Not used for RCS: "VoiceCall"
                
            } while ((childnode = childnode.getNextSibling()) != null);
        }*/
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
                            if (typenode.getNodeValue().equalsIgnoreCase("SecondaryDevicePar")) {
                                parseSecondaryDevicePar(childnode);
                            }
                        }
                    }
                }

                if (intUrlFmt == null) {
					if ((intUrlFmt = getValueByParamName("IntUrlFmt", childnode, TYPE_INT)) != null) {
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.TEL_URI_FORMAT, intUrlFmt.equals("0"));
						continue;
					}
                }

                if (maxSizeImageShare == null) {
                    if ((maxSizeImageShare = getValueByParamName("MaxSizeImageShare", childnode, TYPE_INT)) != null) {
                    	int kb = Integer.parseInt(maxSizeImageShare) / 1024;
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.MAX_IMAGE_SHARE_SIZE, ""+kb);
                        continue;
                    }
                }

                if (maxTimeVideoShare == null) {
                    if ((maxTimeVideoShare = getValueByParamName("MaxTimeVideoShare", childnode, TYPE_INT)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.MAX_VIDEO_SHARE_DURATION, maxTimeVideoShare);
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

                // Not used: "AddressType"
                
            } while((childnode = childnode.getNextSibling()) != null);
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
                            RcsSettings.getInstance().setImsAuthenticationProcedureForMobile(AuthenticationProcedure.GIBA);
                        } else {
                            RcsSettings.getInstance().setImsAuthenticationProcedureForMobile(AuthenticationProcedure.DIGEST);
                        }
                        continue;
                    }
                }

                if (realm == null) {
                    if ((realm = getValueByParamName("Realm", childnode, TYPE_TXT)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.USERPROFILE_IMS_REALM, realm);
                        continue;
                    }
                }

                if (userName == null) {
                    if ((userName = getValueByParamName("UserName", childnode, TYPE_TXT)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID,
                                userName);
                        continue;
                    }
                }

                if (userPwd == null) {
                    if ((userPwd = getValueByParamName("UserPwd", childnode, TYPE_TXT)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.USERPROFILE_IMS_PASSWORD, userPwd);
                        continue;
                    }
                }
            } while((childnode = childnode.getNextSibling()) != null);
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
                            if (typenode.getNodeValue().equalsIgnoreCase("IMS")) {
                                parseIMS(childnode);
                            } else
                            if (typenode.getNodeValue().equalsIgnoreCase("PRESENCE")) {
                                parsePresence(childnode);
                            } else
                            if (typenode.getNodeValue().equalsIgnoreCase("XDMS")) {
                                parseXDMS(childnode);
                            } else
                            if (typenode.getNodeValue().equalsIgnoreCase("IM")) {
                                parseIM(childnode);
                            } else
                            if (typenode.getNodeValue().equalsIgnoreCase("CAPDISCOVERY")) {
                                parseCapabilityDiscovery(childnode);
                            } else
                            if (typenode.getNodeValue().equalsIgnoreCase("APN")) {
                                parseAPN(childnode);
                            } else
                            if (typenode.getNodeValue().equalsIgnoreCase("OTHER")) {
                                parseOther(childnode);
                            } else
                            if (typenode.getNodeValue().equalsIgnoreCase("SERVICES")) {
                                parseServices(childnode);
                            } else
                            if (typenode.getNodeValue().equalsIgnoreCase("SUPL")) {
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
                            } else
                            if (typenode.getNodeValue().equalsIgnoreCase("Public_user_identity_List")) {
                                parsePublicUserIdentity(childnode);
                            } else
                            if (typenode.getNodeValue().equalsIgnoreCase("Ext")) {
                                parseExt(childnode);
                            } else
                            if (typenode.getNodeValue().equalsIgnoreCase("ICSI_List")) {
                                parseICSI(childnode);
                            } else
                            if (typenode.getNodeValue().equalsIgnoreCase("LBO_P-CSCF_Address")) {
                                parsePcscfAddress(childnode);
                            } else
                            if (typenode.getNodeValue().equalsIgnoreCase("PhoneContext_List")) {
                                parsePhoneContextList(childnode);
                            } else
                            if (typenode.getNodeValue().equalsIgnoreCase("APPAUTH")) {
                                parseAppAuthent(childnode);
                            }
                        }
                    }
                }

                if (timert1 == null) {
                    if ((timert1 = getValueByParamName("Timer_T1", childnode, TYPE_INT)) != null) {
                        RcsSettings.getInstance().writeParameter(RcsSettingsData.SIP_TIMER_T1,
                                timert1);
                        continue;
                    }
                }

                if (timert2 == null) {
                    if ((timert2 = getValueByParamName("Timer_T2", childnode, TYPE_INT)) != null) {
                        RcsSettings.getInstance().writeParameter(RcsSettingsData.SIP_TIMER_T2,
                                timert2);
                        continue;
                    }
                }

                if (timert4 == null) {
                    if ((timert4 = getValueByParamName("Timer_T4", childnode, TYPE_INT)) != null) {
                        RcsSettings.getInstance().writeParameter(RcsSettingsData.SIP_TIMER_T4,
                                timert4);
                        continue;
                    }
                }

                if (privateUserIdentity == null) {
                    if ((privateUserIdentity = getValueByParamName("Private_User_Identity", childnode, TYPE_TXT)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID,
                                privateUserIdentity);
                        continue;
                    }
                }

                if (homeDomain == null) {
                    if ((homeDomain = getValueByParamName("Home_network_domain_name", childnode, TYPE_TXT)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.USERPROFILE_IMS_HOME_DOMAIN,
                                homeDomain);
                        continue;
                    }
                }

                if (keepAliveEnabled == null) {
					if ((keepAliveEnabled = getValueByParamName("Keep_Alive_Enabled", childnode, TYPE_INT)) != null) {
						RcsSettings.getInstance().writeBoolean(RcsSettingsData.SIP_KEEP_ALIVE, keepAliveEnabled.equals("1"));
						continue;
					}
                }

                if (regRetryBasetime == null) {
                    if ((regRetryBasetime = getValueByParamName("RegRetryBaseTime", childnode, TYPE_INT)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.REGISTER_RETRY_BASE_TIME, regRetryBasetime);
                        continue;
                    }
                }

                if (regRetryMaxtime == null) {
                    if ((regRetryMaxtime = getValueByParamName("RegRetryMaxTime", childnode, TYPE_INT)) != null) {
                        RcsSettings.getInstance().writeParameter(
                                RcsSettingsData.REGISTER_RETRY_MAX_TIME, regRetryMaxtime);
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
        
        if (logger.isActivated()) {
            logger.debug("Get parameter " + paramName + ", node " + node);
        }

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
                    // logger.debug("Read parameter " + paramName + ": " + value);
                    logger.debug("Read parameter " + paramName);
                }
            	
            	// Check type
            	if (type == TYPE_INT) {
            		try {
            			Integer.parseInt(value);
            		} catch(NumberFormatException e) {
            			if (logger.isActivated()) {
            				logger.warn("Bad value for integer parameter " + paramName);
            			}
            			return null;
            		}
            	} 
            	
				return value;
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
