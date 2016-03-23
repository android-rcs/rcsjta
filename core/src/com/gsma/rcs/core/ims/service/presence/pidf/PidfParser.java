/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.core.ims.service.presence.pidf;

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.core.ims.service.presence.pidf.geoloc.Geopriv;
import com.gsma.rcs.utils.DateUtils;
import com.gsma.rcs.utils.StringUtils;
import com.gsma.rcs.utils.logger.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * PDIF parser
 * 
 * @author jexa7410
 */
public class PidfParser extends DefaultHandler {

    /*
     * PIDF SAMPLE: <?xml version="1.0" encoding="UTF-8"?> <presence
     * xmlns="urn:ietf:params:xml:ns:pidf" xmlns:op="urn:oma:xml:prs:pidf:oma-pres"
     * xmlns:rt="urn:ietf:params:xml:ns:pidf:rpid" xmlns:gp="urn:ietf:params:xml:ns:pidf:geopriv10"
     * xmlns:pdm="urn:ietf:params:xml:ns:pidf:data-model"
     * xmlns:rs="urn:ietf:params:xml:ns:pidf:status:rpid-status"
     * entity="sip:+33960810101@domain.com" version="1"> <ep:tuple
     * xmlns:ep="urn:ietf:params:xml:ns:pidf" id="id5"> <ep:status> <ep:basic>closed</ep:basic>
     * </ep:status> <op:service-description>
     * <op:service-id>org.3gpp.cs-videotelephony</op:service-id> <op:version>1.0</op:version>
     * </op:service-description> <ep:contact>tel:+33960810101</ep:contact>
     * <ep:timestamp>2009-04-24T16:58:32Z</ep:timestamp> </ep:tuple> <ep:tuple
     * xmlns:ep="urn:ietf:params:xml:ns:pidf" id="id2"> <ep:status> <ep:basic>open</ep:basic>
     * </ep:status> <op:service-description> <op:service-id>org.gsma.videoshare</op:service-id>
     * <op:version>1.0</op:version> </op:service-description>
     * <ep:contact>sip:+33960810101@domain.com</ep:contact>
     * <ep:timestamp>2009-04-24T16:58:32Z</ep:timestamp> </ep:tuple> <tuple id="geolocid"> <status>
     * <basic>automatic</basic> <geopriv> <location-info> <location> <Point srsDimension="3"\>
     * <pos>48.73 -3.53 62</pos> </Point> </location> </location-info> <method>GPS</method>
     * </geopriv> </status> <timestamp>2009-04-24T16:58:32Z</timestamp>
     * <contact>tel:+33960810101</contact> </tuple> <pdm:person id="p1"> <ci:display-name
     * xmlns:ci="urn:ietf:params:xml:ns:pidf:cipid">User BRUNE</ci:display-name>
     * <pdm:timestamp>2009-04-24T16:58:32Z</pdm:timestamp> <rpid:status-icon
     * opd:etag="26362">http://..../rcs_status_icon</rpid:status-icon> </pdm:person> </presence>
     */

    private StringBuffer mAccumulator;
    private PidfDocument mPresence;
    private Tuple mTuple;
    private Note mNote;
    private Contact mContact;
    private Basic mBasic;
    private OverridingWillingness mWillingness;
    private Status mStatus;
    private Service mService;
    private StatusIcon mIcon;
    private Person mPerson;
    private Geopriv mGeopriv;

    private final InputSource mInputSource;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param inputSource Input source
     */
    public PidfParser(InputSource inputSource) {
        mInputSource = inputSource;
    }

    /**
     * Parse the PIDF input
     * 
     * @return PidfParser
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws ParseFailureException
     */
    public PidfParser parse() throws ParserConfigurationException, SAXException,
            ParseFailureException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(mInputSource, this);
            return this;

        } catch (IOException e) {
            throw new ParseFailureException("Failed to parse input source!", e);
        }
    }

    public PidfDocument getPresence() {
        return mPresence;
    }

    public void startDocument() {
        if (logger.isActivated()) {
            logger.debug("Start document");
        }
        mAccumulator = new StringBuffer();
    }

    public void characters(char buffer[], int start, int length) {
        mAccumulator.append(buffer, start, length);
    }

    public void startElement(String namespaceURL, String localName, String qname, Attributes attr) {
        mAccumulator.setLength(0);

        if (localName.equals("presence")) {
            String entity = attr.getValue("entity").trim();
            mPresence = new PidfDocument(entity);
        } else if (localName.equals("person")) {
            String id = attr.getValue("id").trim();
            mPerson = new Person(id);
        } else if (localName.equals("tuple")) {
            String id = attr.getValue("id").trim();
            mTuple = new Tuple(id);
        } else if (localName.equals("note")) {
            mNote = new Note();
            String lang = attr.getValue("xml:lang");
            if (lang != null) {
                mNote.setLang(lang.trim());
            }
        } else if (localName.equals("contact")) {
            mContact = new Contact();
            String priority = attr.getValue("priority");
            if (priority != null) {
                mContact.setPriority(priority.trim());
            }
            String contactType = attr.getValue("contactType");

            if (contactType != null) {
                mContact.setContactType(contactType.trim());
            }
        } else if (localName.equals("overriding-willingness")) {
            mWillingness = new OverridingWillingness();
            String until = null;
            int indexUntilValue = attr.getIndex("urn:oma:xml:pde:pidf:ext", "until");
            if (indexUntilValue != -1) {
                until = attr.getValue(indexUntilValue);
            }
            if (until != null) {
                mWillingness.setUntilTimestamp(until);
            }
        } else if (localName.equals("status-icon")) {
            mIcon = new StatusIcon();
            String etag = null;
            int indexEtagValue = attr.getIndex("urn:oma:xml:pde:pidf:ext", "etag");
            if (indexEtagValue != -1) {
                etag = attr.getValue(indexEtagValue);
            }
            if (etag != null) {
                mIcon.setEtag(etag);
            }
        } else if (localName.equals("status")) {
            mStatus = new Status();
        } else if (localName.equals("service-description")) {
            mService = new Service();
        } else if (localName.equals("geopriv")) {
            mGeopriv = new Geopriv();
        }
    }

    public void endElement(String namespaceURL, String localName, String qname) {
        if (localName.equals("tuple")) {
            if (mPresence != null) {
                mPresence.addTuple(mTuple);
            }
            mTuple = null;
        } else if (localName.equals("person")) {
            if (mPerson != null) {
                mPresence.setPerson(mPerson);
                mPerson = null;
            }
        } else if (localName.equals("note")) {
            if ((mNote != null) && (mPerson != null)) {
                String value = StringUtils.decodeXML(mAccumulator.toString().trim());
                mNote.setValue(value);
                mPerson.setNote(mNote);
                mNote = null;
            }
        } else if (localName.equals("contact")) {
            if ((mContact != null) && (mTuple != null)) {
                mContact.setUri(mAccumulator.toString().trim());
                mTuple.addContact(mContact);
                mContact = null;
            }
        } else if (localName.equals("basic")) {
            mBasic = new Basic(mAccumulator.toString().trim());
            if (mStatus != null) {
                mStatus.setBasic(mBasic);
            } else if (mWillingness != null) {
                mWillingness.setBasic(mBasic);
            }
            mBasic = null;
        } else if (localName.equals("overriding-willingness")) {
            if ((mWillingness != null) && (mPerson != null)) {
                mPerson.setOverridingWillingness(mWillingness);
                mWillingness = null;
            }
        } else if (localName.equals("status-icon")) {
            String url = mAccumulator.toString();
            if ((mIcon != null) && (mPerson != null)) {
                mIcon.setUrl(url.trim());
                mPerson.setStatusIcon(mIcon);
                mIcon = null;
            }
        } else if (localName.equals("status")) {
            if ((mStatus != null) && (mTuple != null)) {
                mTuple.setStatus(mStatus);
                mStatus = null;
            }
        } else if (localName.equals("timestamp")) {
            String timestamp = mAccumulator.toString();
            if (timestamp != null) {
                long ts = DateUtils.decodeDate(timestamp.trim());
                if (mPerson != null) {
                    mPerson.setTimestamp(ts);
                } else if (mTuple != null) {
                    mTuple.setTimestamp(ts);
                }
            }
        } else if (localName.equals("service-description")) {
            if ((mService != null) && (mTuple != null)) {
                mTuple.setService(mService);
                mService = null;
            }
        } else if (localName.equals("service-id")) {
            if (mService != null) {
                mService.setId(mAccumulator.toString().trim());
            }
        } else if (localName.equals("homepage")) {
            String homepage = mAccumulator.toString();
            if ((homepage != null) && (mPerson != null)) {
                mPerson.setHomePage(homepage.trim());
                homepage = null;
            }
        } else if (localName.equals("geopriv")) {
            if ((mGeopriv != null) && (mPresence != null)) {
                mPresence.setGeopriv(mGeopriv);
                mGeopriv = null;
            }
        } else if (localName.equals("method")) {
            if (mGeopriv != null) {
                mGeopriv.setMethod(mAccumulator.toString().trim());
            }
        } else if (localName.equals("pos")) {
            if (mGeopriv != null) {
                StringTokenizer st = new StringTokenizer(mAccumulator.toString().trim());
                if (st.hasMoreTokens()) {
                    mGeopriv.setLatitude(Double.parseDouble(st.nextToken()));
                }
                if (st.hasMoreTokens()) {
                    mGeopriv.setLongitude(Double.parseDouble(st.nextToken()));
                }
                if (st.hasMoreTokens()) {
                    mGeopriv.setAltitude(Double.parseDouble(st.nextToken()));
                }
            }
        } else if (localName.equals("presence")) {
            if (logger.isActivated()) {
                logger.debug("Presence document complete");
            }
        }
    }

    public void endDocument() {
        if (logger.isActivated()) {
            logger.debug("End document");
        }
    }
}
