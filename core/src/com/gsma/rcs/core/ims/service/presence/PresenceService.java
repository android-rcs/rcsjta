/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.core.ims.service.presence;

import static com.gsma.rcs.utils.StringUtils.UTF8_STR;

import com.gsma.rcs.addressbook.AddressBookEventListener;
import com.gsma.rcs.addressbook.AddressBookManager;
import com.gsma.rcs.core.FileAccessException;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.http.HttpResponse;
import com.gsma.rcs.core.ims.service.ContactInfo.RcsStatus;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsServiceSession;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.presence.pidf.PidfDocument;
import com.gsma.rcs.core.ims.service.presence.xdm.XdmManager;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.contact.ContactManagerException;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.StartService;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.DateUtils;
import com.gsma.rcs.utils.StringUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;

import java.util.Set;

/**
 * Presence service
 * 
 * @author Jean-Marc AUFFRET
 */
public class PresenceService extends ImsService implements AddressBookEventListener {

    private final RcsSettings mRcsSettings;

    private final ContactManager mContactManager;

    public final boolean mPermanentState;

    private PresenceInfo mPresenceInfo = new PresenceInfo();

    private final PublishManager mPublisher;

    private final XdmManager mXdm;

    private final SubscribeManager mWatcherInfoSubscriber;

    private final SubscribeManager mPresenceSubscriber;

    private static final Logger sLogger = Logger.getLogger(PresenceService.class.getSimpleName());

    private final AddressBookManager mAddressBookManager;

    private final Context mCtx;

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param ctx Context
     * @param rcsSettings RcsSettings
     * @param addressBookManager The address book manager
     * @param contactsManager ContactManager
     */
    public PresenceService(ImsModule parent, Context ctx, RcsSettings rcsSettings,
            ContactManager contactsManager, AddressBookManager addressBookManager) {
        super(parent, rcsSettings.isSocialPresenceSupported());
        mCtx = ctx;
        mRcsSettings = rcsSettings;
        mContactManager = contactsManager;
        mAddressBookManager = addressBookManager;

        // Set presence service options
        mPermanentState = mRcsSettings.isPermanentStateModeActivated();

        // Instantiate the XDM manager
        mXdm = new XdmManager(ctx);

        // Instantiate the publish manager
        mPublisher = new PublishManager(parent, mRcsSettings);

        // Instantiate the subscribe manager for watcher info
        mWatcherInfoSubscriber = new WatcherInfoSubscribeManager(parent, mRcsSettings);

        // Instantiate the subscribe manager for presence
        mPresenceSubscriber = new PresenceSubscribeManager(parent, mRcsSettings);

    }

    public void initialize() {
        mPublisher.initialize();
        mWatcherInfoSubscriber.initialize();
        mPresenceSubscriber.initialize();
    }

    @Override
    public synchronized void start() throws PayloadException, NetworkException {
        if (isServiceStarted()) {
            // Already started
            return;
        }
        setServiceStarted(true);

        mAddressBookManager.addAddressBookListener(this);

        // Restore the last presence info from the contacts database
        mPresenceInfo = mContactManager.getMyPresenceInfo();
        if (sLogger.isActivated()) {
            sLogger.debug("Last presence info:\n" + mPresenceInfo.toString());
        }

        // Initialize the XDM interface
        mXdm.initialize();

        // Add me in the granted set if necessary
        Set<ContactId> grantedContacts = mXdm.getGrantedContacts();

        ContactId me = ImsModule.getImsUserProfile().getUsername();

        if (!grantedContacts.contains(me)) {
            if (sLogger.isActivated()) {
                sLogger.debug("The enduser is not in the granted set: add it now");
            }
            mXdm.addContactToGrantedList(me);
        }

        // It may be necessary to initiate the address book first launch or account check procedure
        if (StartService.getNewUserAccount(mCtx)) {
            Set<ContactId> blockedContacts = mXdm.getBlockedContacts();
            firstLaunchOrAccountChangedCheck(grantedContacts, blockedContacts);
        }

        // Subscribe to watcher-info events
        if (mWatcherInfoSubscriber.subscribe()) {
            if (sLogger.isActivated()) {
                sLogger.debug("Subscribe manager is started with success for watcher-info");
            }
        } else {
            if (sLogger.isActivated()) {
                sLogger.debug("Subscribe manager can't be started for watcher-info");
            }
        }

        // Subscribe to presence events
        if (mPresenceSubscriber.subscribe()) {
            if (sLogger.isActivated()) {
                sLogger.debug("Subscribe manager is started with success for presence");
            }
        } else {
            if (sLogger.isActivated()) {
                sLogger.debug("Subscribe manager can't be started for presence");
            }
        }

        // Publish initial presence info
        String xml;
        if (mPermanentState) {
            xml = buildPartialPresenceInfoDocument(mPresenceInfo);
        } else {
            xml = buildPresenceInfoDocument(mPresenceInfo);
        }
        if (mPublisher.publish(xml)) {
            if (sLogger.isActivated()) {
                sLogger.debug("Publish manager is started with success");
            }
        } else {
            if (sLogger.isActivated()) {
                sLogger.debug("Publish manager can't be started");
            }
        }

        // Force a presence check
        handleAddressBookHasChanged();
    }

    /**
     * First launch or account changed check <br>
     * Check done at first launch of the service on the phone after install of the application or
     * when the user account changed <br>
     * We create a new contact with the adequate state for each RCS number in the XDM lists that is
     * not already existing on the phone
     * 
     * @param grantedContacts of granted contacts
     * @param blockedContacts of blocked contacts
     * @throws PayloadException
     */
    private void firstLaunchOrAccountChangedCheck(Set<ContactId> grantedContacts,
            Set<ContactId> blockedContacts) throws PayloadException {
        final String publicUri = ImsModule.getImsUserProfile().getPublicUri();
        try {
            boolean logActivated = sLogger.isActivated();
            if (logActivated) {
                sLogger.debug("First launch or account change check procedure");
            }
            mContactManager.flushRcsContactProvider();
            ContactId me = null;
            PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(publicUri);
            if (number == null) {
                if (logActivated) {
                    sLogger.error("Cannot parse user contact ".concat(publicUri));
                }
            } else {
                me = ContactUtil.createContactIdFromValidatedData(number);
            }

            for (ContactId contact : grantedContacts) {
                if (me != null && !contact.equals(me)) {
                    if (!PresenceUtils.isNumberInAddressBook(contact)) {
                        if (logActivated) {
                            sLogger.debug(new StringBuilder("The RCS number ").append(contact)
                                    .append(" was not found in the address book: add it")
                                    .toString());
                        }
                        PresenceUtils.createRcsContactIfNeeded(mCtx, contact);
                    }
                    mContactManager.updateRcsStatusOrCreateNewContact(contact,
                            RcsStatus.PENDING_OUT);
                }
            }

            for (ContactId contact : blockedContacts) {
                if (!PresenceUtils.isNumberInAddressBook(contact)) {
                    if (logActivated) {
                        sLogger.debug(new StringBuilder("The RCS number ").append(contact)
                                .append(" was not found in the address book: add it").toString());
                    }
                    PresenceUtils.createRcsContactIfNeeded(mCtx, contact);
                    mContactManager.blockContact(contact);
                    mContactManager.updateRcsStatusOrCreateNewContact(contact, RcsStatus.BLOCKED);
                }
            }
        } catch (OperationApplicationException e) {
            throw new PayloadException(new StringBuilder("Failed creating contact for URI : ")
                    .append(publicUri).toString(), e);

        } catch (ContactManagerException e) {
            throw new PayloadException(new StringBuilder("Failed creating contact for URI : ")
                    .append(publicUri).toString(), e);

        } catch (FileAccessException e) {
            throw new PayloadException(new StringBuilder("Failed creating contact for URI : ")
                    .append(publicUri).toString(), e);

        } catch (RemoteException e) {
            throw new PayloadException(new StringBuilder("Failed creating contact for URI : ")
                    .append(publicUri).toString(), e);
        }
    }

    @Override
    public synchronized void stop(ImsServiceSession.TerminationReason reasonCode)
            throws PayloadException, NetworkException {
        if (!isServiceStarted()) {
            // Already stopped
            return;
        }
        setServiceStarted(false);

        mAddressBookManager.removeAddressBookListener(this);

        if (!mPermanentState) {
            // If not permanent state mode: publish a last presence info before
            // to quit
            if ((getImsModule().getCurrentNetworkInterface() != null)
                    && getImsModule().getCurrentNetworkInterface().isRegistered()
                    && mPublisher.isPublished()) {
                String xml = buildPresenceInfoDocument(mPresenceInfo);
                mPublisher.publish(xml);
            }
        }

        // Stop publish
        mPublisher.terminate();

        // Stop subscriptions
        mWatcherInfoSubscriber.terminate();
        mPresenceSubscriber.terminate();
    }

    /**
     * Check the IMS service
     */
    public void check() {
        if (sLogger.isActivated()) {
            sLogger.debug("Check presence service");
        }

        // Check subscribe manager status for watcher-info events
        if (!mWatcherInfoSubscriber.isSubscribed()) {
            if (sLogger.isActivated()) {
                sLogger.debug("Subscribe manager not yet started for watcher-info");
            }

            if (mWatcherInfoSubscriber.subscribe()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Subscribe manager is started with success for watcher-info");
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.debug("Subscribe manager can't be started for watcher-info");
                }
            }
        }

        // Check subscribe manager status for presence events
        if (!mPresenceSubscriber.isSubscribed()) {
            if (sLogger.isActivated()) {
                sLogger.debug("Subscribe manager not yet started for presence");
            }

            if (mPresenceSubscriber.subscribe()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Subscribe manager is started with success for presence");
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.debug("Subscribe manager can't be started for presence");
                }
            }
        }
    }

    /**
     * Is permanent state procedure
     * 
     * @return Boolean
     */
    public boolean isPermanentState() {
        return mPermanentState;
    }

    /**
     * Set the presence info
     * 
     * @param info Presence info
     */
    public void setPresenceInfo(PresenceInfo info) {
        mPresenceInfo = info;
    }

    /**
     * Returns the presence info
     * 
     * @return Presence info
     */
    public PresenceInfo getPresenceInfo() {
        return mPresenceInfo;
    }

    /**
     * Returns the publish manager
     * 
     * @return Publish manager
     */
    public PublishManager getPublishManager() {
        return mPublisher;
    }

    /**
     * Returns the watcher-info subscribe manager
     * 
     * @return Subscribe manager
     */
    public SubscribeManager getWatcherInfoSubscriber() {
        return mWatcherInfoSubscriber;
    }

    /**
     * Returns the presence subscribe manager
     * 
     * @return Subscribe manager
     */
    public SubscribeManager getPresenceSubscriber() {
        return mPresenceSubscriber;
    }

    /**
     * Returns the XDM manager
     * 
     * @return XDM manager
     */
    public XdmManager getXdmManager() {
        return mXdm;
    }

    /**
     * Build boolean status value
     * 
     * @param state Boolean state
     * @return String
     */
    private String buildBooleanStatus(boolean state) {
        if (state) {
            return "open";
        }
        return "closed";
    }

    /**
     * Build capabilities document
     * 
     * @param timestamp Timestamp
     * @param capabilities Capabilities
     * @return Document
     */
    private String buildCapabilities(String timestamp, Capabilities capabilities) {
        String publicUri = ImsModule.getImsUserProfile().getPublicUri();
        return new StringBuilder("<tuple id=\"t1\">").append(SipUtils.CRLF)
                .append("  <status><basic>")
                .append(buildBooleanStatus(capabilities.isFileTransferMsrpSupported()))
                .append("</basic></status>").append(SipUtils.CRLF)
                .append("  <op:service-description>").append(SipUtils.CRLF)
                .append("    <op:service-id>").append(PresenceUtils.FEATURE_RCS2_FT)
                .append("</op:service-id>").append(SipUtils.CRLF)
                .append("    <op:version>1.0</op:version>").append(SipUtils.CRLF)
                .append("  </op:service-description>").append(SipUtils.CRLF).append("  <contact>")
                .append(publicUri).append("</contact>").append(SipUtils.CRLF)
                .append("  <timestamp>").append(timestamp).append("</timestamp>")
                .append(SipUtils.CRLF).append("</tuple>").append(SipUtils.CRLF)
                .append("<tuple id=\"t2\">").append(SipUtils.CRLF).append("  <status><basic>")
                .append(buildBooleanStatus(capabilities.isImageSharingSupported()))
                .append("</basic></status>").append(SipUtils.CRLF)
                .append("  <op:service-description>").append(SipUtils.CRLF)
                .append("    <op:service-id>").append(PresenceUtils.FEATURE_RCS2_IMAGE_SHARE)
                .append("</op:service-id>").append(SipUtils.CRLF)
                .append("    <op:version>1.0</op:version>").append(SipUtils.CRLF)
                .append("  </op:service-description>").append(SipUtils.CRLF).append("  <contact>")
                .append(publicUri).append("</contact>").append(SipUtils.CRLF)
                .append("  <timestamp>").append(timestamp).append("</timestamp>")
                .append(SipUtils.CRLF).append("</tuple>").append(SipUtils.CRLF)
                .append("<tuple id=\"t3\">").append(SipUtils.CRLF).append("  <status><basic>")
                .append(buildBooleanStatus(capabilities.isVideoSharingSupported()))
                .append("</basic></status>").append(SipUtils.CRLF)
                .append("  <op:service-description>").append(SipUtils.CRLF)
                .append("    <op:service-id>").append(PresenceUtils.FEATURE_RCS2_VIDEO_SHARE)
                .append("</op:service-id>").append(SipUtils.CRLF)
                .append("    <op:version>1.0</op:version>").append(SipUtils.CRLF)
                .append("  </op:service-description>").append(SipUtils.CRLF).append("  <contact>")
                .append(publicUri).append("</contact>").append(SipUtils.CRLF)
                .append("  <timestamp>").append(timestamp).append("</timestamp>")
                .append(SipUtils.CRLF).append("</tuple>").append(SipUtils.CRLF)
                .append("<tuple id=\"t4\">").append(SipUtils.CRLF).append("  <status><basic>")
                .append(buildBooleanStatus(capabilities.isImSessionSupported()))
                .append("</basic></status>").append(SipUtils.CRLF)
                .append("  <op:service-description>").append(SipUtils.CRLF)
                .append("    <op:service-id>").append(PresenceUtils.FEATURE_RCS2_CHAT)
                .append("</op:service-id>").append(SipUtils.CRLF)
                .append("    <op:version>1.0</op:version>").append(SipUtils.CRLF)
                .append("  </op:service-description>").append(SipUtils.CRLF).append("  <contact>")
                .append(publicUri).append("</contact>").append(SipUtils.CRLF)
                .append("  <timestamp>").append(timestamp).append("</timestamp>")
                .append(SipUtils.CRLF).append("</tuple>").append(SipUtils.CRLF)
                .append("<tuple id=\"t5\">").append(SipUtils.CRLF).append("  <status><basic>")
                .append(buildBooleanStatus(capabilities.isCsVideoSupported()))
                .append("</basic></status>").append(SipUtils.CRLF)
                .append("  <op:service-description>").append(SipUtils.CRLF)
                .append("    <op:service-id>").append(PresenceUtils.FEATURE_RCS2_CS_VIDEO)
                .append("</op:service-id>").append(SipUtils.CRLF)
                .append("    <op:version>1.0</op:version>").append(SipUtils.CRLF)
                .append("  </op:service-description>").append(SipUtils.CRLF).append("  <contact>")
                .append(publicUri).append("</contact>").append(SipUtils.CRLF)
                .append("  <timestamp>").append(timestamp).append("</timestamp>")
                .append(SipUtils.CRLF).append("</tuple>").append(SipUtils.CRLF).toString();
    }

    /**
     * Build geoloc document
     * 
     * @param timestamp Timestamp
     * @param geolocInfo Geoloc info
     * @return Document
     */
    private String buildGeoloc(String timestamp, Geoloc geolocInfo) {
        String document = "";
        if (geolocInfo != null) {
            document += "<tuple id=\"g1\">" + SipUtils.CRLF
                    + "  <status><basic>open</basic></status>" + SipUtils.CRLF + "   <gp:geopriv>"
                    + SipUtils.CRLF + "    <gp:location-info><gml:location>" + SipUtils.CRLF
                    + "        <gml:Point srsDimension=\"3\"><gml:pos>" + geolocInfo.getLatitude()
                    + " " + geolocInfo.getLongitude() + " " + geolocInfo.getAltitude()
                    + "</gml:pos>" + SipUtils.CRLF + "        </gml:Point></gml:location>"
                    + SipUtils.CRLF + "    </gp:location-info>" + SipUtils.CRLF
                    + "    <gp:method>GPS</gp:method>" + SipUtils.CRLF + "   </gp:geopriv>"
                    + SipUtils.CRLF + "  <contact>" + ImsModule.getImsUserProfile().getPublicUri()
                    + "</contact>" + SipUtils.CRLF + "  <timestamp>" + timestamp + "</timestamp>"
                    + SipUtils.CRLF + "</tuple>" + SipUtils.CRLF;
        }
        return document;
    }

    /**
     * Build person info document
     * 
     * @param info Presence info
     * @return Document
     */
    private String buildPersonInfo(PresenceInfo info) {
        StringBuilder document = new StringBuilder("  <op:overriding-willingness>")
                .append(SipUtils.CRLF).append("    <op:basic>").append(info.getPresenceStatus())
                .append("</op:basic>").append(SipUtils.CRLF)
                .append("  </op:overriding-willingness>").append(SipUtils.CRLF);

        FavoriteLink favoriteLink = info.getFavoriteLink();
        if ((favoriteLink != null) && (favoriteLink.getLink() != null)) {
            document.append("  <ci:homepage>")
                    .append(StringUtils.encodeXML(favoriteLink.getLink())).append("</ci:homepage>")
                    .append(SipUtils.CRLF);
        }

        PhotoIcon photoIcon = info.getPhotoIcon();
        String eTag = (photoIcon != null) ? photoIcon.getEtag() : null;
        if (photoIcon != null && eTag != null) {
            document.append("  <rpid:status-icon opd:etag=\"").append(eTag)
                    .append("\" opd:fsize=\"").append(photoIcon.getSize())
                    .append("\" opd:contenttype=\"").append(photoIcon.getType())
                    .append("\" opd:resolution=\"").append(photoIcon.getResolution()).append("\">")
                    .append(mXdm.getEndUserPhotoIconUrl()).append("</rpid:status-icon>")
                    .append(SipUtils.CRLF);
        }

        String freetext = info.getFreetext();
        if (freetext != null) {
            document.append("  <pdm:note>").append(StringUtils.encodeXML(freetext))
                    .append("</pdm:note>").append(SipUtils.CRLF);
        }

        return document.toString();
    }

    /**
     * Build presence info document (RCS 1.0)
     * 
     * @param info Presence info
     * @return Document
     */
    private String buildPresenceInfoDocument(PresenceInfo info) {
        String document = new StringBuilder("<?xml version=\"1.0\" encoding=\"").append(UTF8_STR)
                .append("\"?>").append(SipUtils.CRLF)
                .append("<presence xmlns=\"urn:ietf:params:xml:ns:pidf\"")
                .append(" xmlns:op=\"urn:oma:xml:prs:pidf:oma-pres\"")
                .append(" xmlns:opd=\"urn:oma:xml:pde:pidf:ext\"")
                .append(" xmlns:pdm=\"urn:ietf:params:xml:ns:pidf:data-model\"")
                .append(" xmlns:ci=\"urn:ietf:params:xml:ns:pidf:cipid\"")
                .append(" xmlns:rpid=\"urn:ietf:params:xml:ns:pidf:rpid\"")
                .append(" xmlns:gp=\"urn:ietf:params:xml:ns:pidf:geopriv10\"")
                .append(" xmlns:gml=\"urn:opengis:specification:gml:schema-xsd:feature:v3.0\"")
                .append(" entity=\"").append(ImsModule.getImsUserProfile().getPublicUri())
                .append("\">").append(SipUtils.CRLF).toString();

        // Encode timestamp
        String timestamp = DateUtils.encodeDate(info.getTimestamp());

        // Build capabilities
        document += buildCapabilities(timestamp, mRcsSettings.getMyCapabilities());

        // Build geoloc
        document += buildGeoloc(timestamp, info.getGeoloc());

        // Build person info
        document += "<pdm:person id=\"p1\">" + SipUtils.CRLF + buildPersonInfo(info)
                + "  <pdm:timestamp>" + timestamp + "</pdm:timestamp>" + SipUtils.CRLF
                + "</pdm:person>" + SipUtils.CRLF;

        // Add last header
        document += "</presence>" + SipUtils.CRLF;

        return document;
    }

    /**
     * Build partial presence info document (all presence info except permanent state info)
     * 
     * @param info Presence info
     * @return Document
     */
    private String buildPartialPresenceInfoDocument(PresenceInfo info) {
        String document = new StringBuilder("<?xml version=\"1.0\" encoding=\"").append(UTF8_STR)
                .append("\"?>").append(SipUtils.CRLF)
                .append("<presence xmlns=\"urn:ietf:params:xml:ns:pidf\"")
                .append(" xmlns:op=\"urn:oma:xml:prs:pidf:oma-pres\"")
                .append(" xmlns:opd=\"urn:oma:xml:pde:pidf:ext\"")
                .append(" xmlns:pdm=\"urn:ietf:params:xml:ns:pidf:data-model\"")
                .append(" xmlns:ci=\"urn:ietf:params:xml:ns:pidf:cipid\"")
                .append(" xmlns:rpid=\"urn:ietf:params:xml:ns:pidf:rpid\"")
                .append(" xmlns:gp=\"urn:ietf:params:xml:ns:pidf:geopriv10\"")
                .append(" xmlns:gml=\"urn:opengis:specification:gml:schema-xsd:feature:v3.0\"")
                .append(" entity=\"").append(ImsModule.getImsUserProfile().getPublicUri())
                .append("\">").append(SipUtils.CRLF).toString();

        // Encode timestamp
        String timestamp = DateUtils.encodeDate(info.getTimestamp());

        // Build capabilities
        document += buildCapabilities(timestamp, mRcsSettings.getMyCapabilities());

        // Build geoloc
        document += buildGeoloc(timestamp, info.getGeoloc());

        // Add last header
        document += "</presence>" + SipUtils.CRLF;

        return document;
    }

    /**
     * Build permanent presence info document (RCS R2.0)
     * 
     * @param info Presence info
     * @return Document
     */
    private String buildPermanentPresenceInfoDocument(PresenceInfo info) {
        String document = new StringBuilder("<?xml version=\"1.0\" encoding=\"").append(UTF8_STR)
                .append("\"?>").append(SipUtils.CRLF)
                .append("<presence xmlns=\"urn:ietf:params:xml:ns:pidf\"")
                .append(" xmlns:op=\"urn:oma:xml:prs:pidf:oma-pres\"")
                .append(" xmlns:opd=\"urn:oma:xml:pde:pidf:ext\"")
                .append(" xmlns:pdm=\"urn:ietf:params:xml:ns:pidf:data-model\"")
                .append(" xmlns:ci=\"urn:ietf:params:xml:ns:pidf:cipid\"")
                .append(" xmlns:rpid=\"urn:ietf:params:xml:ns:pidf:rpid\"").append(" entity=\"")
                .append(ImsModule.getImsUserProfile().getPublicUri()).append("\">")
                .append(SipUtils.CRLF).toString();

        // Encode timestamp
        String timestamp = DateUtils.encodeDate(info.getTimestamp());

        // Build person info (freetext, favorite link and photo-icon)
        document += "<pdm:person id=\"p1\">" + SipUtils.CRLF + buildPersonInfo(info)
                + "  <pdm:timestamp>" + timestamp + "</pdm:timestamp>" + SipUtils.CRLF
                + "</pdm:person>" + SipUtils.CRLF;

        // Add last header
        document += "</presence>" + SipUtils.CRLF;

        return document;
    }

    /**
     * Update photo-icon
     * 
     * @param photoIcon Photo-icon
     * @return Boolean result
     * @throws NetworkException
     * @throws PayloadException
     */
    private boolean updatePhotoIcon(PhotoIcon photoIcon) throws PayloadException, NetworkException {
        boolean result = false;

        // Photo-icon management
        PhotoIcon currentPhoto = mPresenceInfo.getPhotoIcon();
        if ((photoIcon != null) && (photoIcon.getEtag() == null)) {
            // Test photo icon size
            long maxSize = mRcsSettings.getMaxPhotoIconSize();
            if ((maxSize != 0) && (photoIcon.getSize() > maxSize)) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Max photo size achieved");
                }
                return false;
            }

            // Upload the new photo-icon
            if (sLogger.isActivated()) {
                sLogger.info("Upload the photo-icon");
            }
            result = uploadPhotoIcon(photoIcon);
        } else if ((photoIcon == null) && (currentPhoto != null)) {
            // Delete the current photo-icon
            if (sLogger.isActivated()) {
                sLogger.info("Delete the photo-icon");
            }
            result = deletePhotoIcon();
        } else {
            // Nothing to do
            result = true;
        }

        return result;
    }

    /**
     * Publish presence info
     * 
     * @param info Presence info
     * @return true if the presence info has been publish with success, else returns false
     * @throws NetworkException
     * @throws PayloadException
     */
    public boolean publishPresenceInfo(PresenceInfo info) throws PayloadException, NetworkException {
        boolean result = false;

        // Photo-icon management
        result = updatePhotoIcon(info.getPhotoIcon());
        if (!result) {
            // Can't update the photo-icon in the XDM server
            return result;
        }

        // Reset timestamp
        info.resetTimestamp();

        // Publish presence info
        if (mPermanentState) {
            // Permanent state procedure: publish the new presence info via XCAP
            if (sLogger.isActivated()) {
                sLogger.info("Publish presence info via XDM request (permanent state)");
            }
            String xml = buildPermanentPresenceInfoDocument(info);
            HttpResponse response = mXdm.setPresenceInfo(xml);
            if ((response != null) && response.isSuccessfullResponse()) {
                result = true;
            } else {
                result = false;
            }
        } else {
            // SIP procedure: publish the new presence info via SIP
            if (sLogger.isActivated()) {
                sLogger.info("Publish presence info via SIP request");
            }
            String xml = buildPresenceInfoDocument(info);
            result = mPublisher.publish(xml);
        }

        // If server updated with success then update contact info cache
        if (result) {
            mPresenceInfo = info;
        }

        return result;
    }

    /**
     * Upload photo icon
     * 
     * @param photo Photo icon
     * @return Boolean result
     * @throws NetworkException
     * @throws PayloadException
     */
    public boolean uploadPhotoIcon(PhotoIcon photo) throws PayloadException, NetworkException {
        // Upload the photo to the XDM server
        HttpResponse response = mXdm.uploadEndUserPhoto(photo);
        if ((response != null) && response.isSuccessfullResponse()) {
            // Extract the Etag value in the 200 OK response
            String etag = response.getHeader("Etag");
            if (etag != null) {
                // Removed quotes
                etag = StringUtils.removeQuotes(etag);
            } else {
                etag = "" + System.currentTimeMillis();
            }

            // Set the Etag of the photo-icon
            photo.setEtag(etag);

            return true;
        }
        return false;
    }

    /**
     * Delete photo icon
     * 
     * @return Boolean result
     * @throws NetworkException
     * @throws PayloadException
     */
    public boolean deletePhotoIcon() throws PayloadException, NetworkException {
        // Delete the photo from the XDM server
        HttpResponse response = mXdm.deleteEndUserPhoto();
        if ((response != null)
                && (response.isSuccessfullResponse() || response.isNotFoundResponse())) {
            return true;
        }
        return false;
    }

    /**
     * Invite a contact to share its presence
     * 
     * @param contact Contact
     * @return Returns true if XDM request was successful, else false
     * @throws NetworkException
     * @throws PayloadException
     */
    public boolean inviteContactToSharePresence(ContactId contact) throws PayloadException,
            NetworkException {
        // Remove contact from the blocked contacts list
        mXdm.removeContactFromBlockedList(contact);

        // Remove contact from the revoked contacts list
        mXdm.removeContactFromRevokedList(contact);

        // Add contact in the granted contacts list
        HttpResponse response = mXdm.addContactToGrantedList(contact);
        if ((response != null) && response.isSuccessfullResponse()) {
            return true;
        }
        return false;
    }

    /**
     * Revoke a shared contact
     * 
     * @param contact Contact
     * @return Returns true if XDM request was successful, else false
     * @throws NetworkException
     * @throws PayloadException
     */
    public boolean revokeSharedContact(ContactId contact) throws PayloadException, NetworkException {
        // Add contact in the revoked contacts list
        HttpResponse response = mXdm.addContactToRevokedList(contact);
        if ((response == null) || (!response.isSuccessfullResponse())) {
            return false;
        }

        // Remove contact from the granted contacts list
        response = mXdm.removeContactFromGrantedList(contact);
        if ((response != null)
                && (response.isSuccessfullResponse() || response.isNotFoundResponse())) {
            return true;
        }
        return false;
    }

    /**
     * Remove a revoked contact
     * 
     * @param contact Contact
     * @return Returns true if XDM request was successful, else false
     * @throws NetworkException
     * @throws PayloadException
     */
    public boolean removeRevokedContact(ContactId contact) throws PayloadException,
            NetworkException {
        // Remove contact from the revoked contacts list
        HttpResponse response = mXdm.removeContactFromRevokedList(contact);
        if ((response != null)
                && (response.isSuccessfullResponse() || response.isNotFoundResponse())) {
            return true;
        }
        return false;
    }

    /**
     * Remove a blocked contact
     * 
     * @param contact Contact
     * @return Returns true if XDM request was successful, else false
     * @throws NetworkException
     * @throws PayloadException
     */
    public boolean removeBlockedContact(ContactId contact) throws PayloadException,
            NetworkException {
        // Remove contact from the blocked contacts list
        HttpResponse response = mXdm.removeContactFromBlockedList(contact);
        if ((response != null)
                && (response.isSuccessfullResponse() || response.isNotFoundResponse())) {
            return true;
        }
        return false;
    }

    /**
     * Address book content has changed
     * 
     * @throws NetworkException
     * @throws PayloadException
     */
    public void handleAddressBookHasChanged() throws PayloadException, NetworkException {
        // If a contact used to be in a RCS relationship with us but is not in the address book any
        // more, we may have to remove or
        // unblock it
        // Get a list of all RCS numbers
        Set<ContactId> rcsNumbers = mContactManager.getRcsContactsWithSocialPresence();
        // For each RCS number
        for (ContactId contact : rcsNumbers) {
            if (!PresenceUtils.isNumberInAddressBook(contact)) {
                // If it is not present in the address book
                if (sLogger.isActivated()) {
                    sLogger.debug("The RCS number " + contact
                            + " was not found in the address book any more.");
                }

                if (mContactManager.isNumberShared(contact)
                        || mContactManager.isNumberInvited(contact)) {
                    // Active or Invited
                    if (sLogger.isActivated()) {
                        sLogger.debug(contact + " is either active or invited");
                        sLogger.debug("We remove it from the buddy list");
                    }
                    // We revoke it
                    boolean result = revokeSharedContact(contact);
                    if (result) {
                        // The contact should be automatically unrevoked after a given timeout. Here
                        // the
                        // timeout period is 0, so the contact can receive invitations again now
                        result = removeRevokedContact(contact);
                        if (result) {
                            // Remove entry from rich address book provider
                            mContactManager.updateRcsStatusOrCreateNewContact(contact,
                                    RcsStatus.RCS_CAPABLE);
                        } else {
                            if (sLogger.isActivated()) {
                                sLogger.error("Something went wrong when revoking shared contact");
                            }
                        }
                    }
                } else if (mContactManager.isNumberBlocked(contact)) {
                    // Blocked
                    if (sLogger.isActivated()) {
                        sLogger.debug(contact + " is blocked");
                        sLogger.debug("We remove it from the blocked list");
                    }
                    // We unblock it
                    boolean result = removeBlockedContact(contact);
                    if (result) {
                        // Remove entry from rich address book provider
                        mContactManager.updateRcsStatusOrCreateNewContact(contact,
                                RcsStatus.RCS_CAPABLE);
                    } else {
                        if (sLogger.isActivated()) {
                            sLogger.error("Something went wrong when removing blocked contact");
                        }
                    }
                } else {
                    if (mContactManager.isNumberWilling(contact)) {
                        // Willing
                        if (sLogger.isActivated()) {
                            sLogger.debug(contact + " is willing");
                            sLogger.debug("Nothing to do");
                        }
                    } else {
                        if (mContactManager.isNumberCancelled(contact)) {
                            // Cancelled
                            if (sLogger.isActivated()) {
                                sLogger.debug(contact + " is cancelled");
                                sLogger.debug("We remove it from rich address book provider");
                            }
                            // Remove entry from rich address book provider
                            mContactManager.updateRcsStatusOrCreateNewContact(contact,
                                    RcsStatus.RCS_CAPABLE);
                        }
                    }
                }
            }
        }
    }

    /**
     * A new presence sharing notification has been received
     * 
     * @param contact Contact identifier
     * @param status Status
     * @param reason Reason
     */
    public void handlePresenceSharingNotification(ContactId contact, String status, String reason) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event presence sharing notification for " + contact + " ("
                    + status + ":" + reason + ")");
        }
        // Not used
    }

    /**
     * A new presence info notification has been received
     * 
     * @param contact Contact identifier
     * @param presence Presence info document
     */
    public void handlePresenceInfoNotification(ContactId contact, PidfDocument presence) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event presence info notification for " + contact);
        }
        // Not used
    }

    /**
     * A new presence sharing invitation has been received
     * 
     * @param contact Contact identifier
     */
    public void handlePresenceSharingInvitation(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug("Handle event presence sharing invitation");
        }
        // Not used
    }

}
