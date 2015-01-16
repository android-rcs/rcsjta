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

package com.orangelabs.rcs.core.ims.service.presence.pidf;

import java.util.StringTokenizer;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import com.orangelabs.rcs.core.ims.service.presence.pidf.geoloc.Geopriv;
import com.orangelabs.rcs.utils.DateUtils;
import com.orangelabs.rcs.utils.StringUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * PDIF parser
 * 
 * @author jexa7410
 */
public class PidfParser extends DefaultHandler {
	
	/* PIDF SAMPLE:
	<?xml version="1.0" encoding="UTF-8"?>
	<presence xmlns="urn:ietf:params:xml:ns:pidf" xmlns:op="urn:oma:xml:prs:pidf:oma-pres" xmlns:rt="urn:ietf:params:xml:ns:pidf:rpid" xmlns:gp="urn:ietf:params:xml:ns:pidf:geopriv10" xmlns:pdm="urn:ietf:params:xml:ns:pidf:data-model" xmlns:rs="urn:ietf:params:xml:ns:pidf:status:rpid-status" entity="sip:+33960810101@domain.com" version="1">
	 <ep:tuple xmlns:ep="urn:ietf:params:xml:ns:pidf" id="id5">
	   <ep:status>
	   <ep:basic>closed</ep:basic>
	   </ep:status>
	   <op:service-description>
	     <op:service-id>org.3gpp.cs-videotelephony</op:service-id>
	     <op:version>1.0</op:version>
	   </op:service-description>
	   <ep:contact>tel:+33960810101</ep:contact>
	   <ep:timestamp>2009-04-24T16:58:32Z</ep:timestamp>
	 </ep:tuple>
	 <ep:tuple xmlns:ep="urn:ietf:params:xml:ns:pidf" id="id2">
	   <ep:status>
	   <ep:basic>open</ep:basic>
	   </ep:status>
	   <op:service-description>
	     <op:service-id>org.gsma.videoshare</op:service-id>
	     <op:version>1.0</op:version>
	   </op:service-description>
	   <ep:contact>sip:+33960810101@domain.com</ep:contact>
	   <ep:timestamp>2009-04-24T16:58:32Z</ep:timestamp>
	 </ep:tuple>
	 <tuple id="geolocid">
	   <status>
	    <basic>automatic</basic>
	    <geopriv>
	     <location-info>
		  <location>
		   <Point srsDimension="3"\>
		    <pos>48.73 -3.53 62</pos>
		   </Point>
		  </location>
		 </location-info>
		 <method>GPS</method>
	    </geopriv>
	   </status>
	   <timestamp>2009-04-24T16:58:32Z</timestamp>
	   <contact>tel:+33960810101</contact>
	 </tuple>		 
	 <pdm:person id="p1">
	   <ci:display-name xmlns:ci="urn:ietf:params:xml:ns:pidf:cipid">User BRUNE</ci:display-name>
	   <pdm:timestamp>2009-04-24T16:58:32Z</pdm:timestamp>
	   <rpid:status-icon opd:etag="26362">http://..../rcs_status_icon</rpid:status-icon>
	 </pdm:person>
	 </presence>
	 */
	
	private StringBuffer accumulator;
	private PidfDocument presence = null;
	private Tuple tuple = null;
	private Note note = null;
	private Contact contact = null;
	private Basic basic = null;
	private OverridingWillingness willingness = null;
	private Status status = null;
	private Service service = null;
	private StatusIcon icon = null;
	private Person person = null;
	private Geopriv geopriv = null;
	
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param inputSource Input source
     * @throws Exception
     */
    public PidfParser(InputSource inputSource) throws Exception {
    	SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(inputSource, this);
	}

	public PidfDocument getPresence() {
		return presence;
	}

	public void startDocument() {
		if (logger.isActivated()) {
			logger.debug("Start document");
		}
		accumulator = new StringBuffer();
	}

	public void characters(char buffer[], int start, int length) {
		accumulator.append(buffer, start, length);
	}

	public void startElement(String namespaceURL, String localName,	String qname, Attributes attr) {
		accumulator.setLength(0);

		if (localName.equals("presence")) {
			String entity = attr.getValue("entity").trim();
			presence = new PidfDocument(entity);
		} else
		if (localName.equals("person")) {
			String id = attr.getValue("id").trim();
			person = new Person(id);
		} else
		if (localName.equals("tuple")) {
			String id = attr.getValue("id").trim();
			tuple = new Tuple(id);
		} else
		if (localName.equals("note")) {
			note = new Note();
			String lang = attr.getValue("xml:lang");
			if (lang != null) {
				note.setLang(lang.trim());
			}
		} else
		if (localName.equals("contact")) {
			contact = new Contact();
			String priority = attr.getValue("priority");
			if (priority != null) {
				contact.setPriority(priority.trim());
			}
			String contactType = attr.getValue("contactType");

			if (contactType != null) {
				contact.setContactType(contactType.trim());
			}
		} else
		if (localName.equals("overriding-willingness")) {
			willingness = new OverridingWillingness();
			String until = null;
			int indexUntilValue = attr.getIndex("urn:oma:xml:pde:pidf:ext", "until");
			if (indexUntilValue != -1) {
				until = attr.getValue(indexUntilValue);
			}
			if (until != null) {
				willingness.setUntilTimestamp(until);
			}
		} else
		if (localName.equals("status-icon")) {
			icon = new StatusIcon();
			String etag = null;
			int indexEtagValue = attr.getIndex("urn:oma:xml:pde:pidf:ext", "etag");
			if (indexEtagValue!=-1) {
				etag = attr.getValue(indexEtagValue);
			}
			if (etag != null) {
				icon.setEtag(etag);
			}
		} else
		if (localName.equals("status")) {
			status = new Status();
		} else
		if (localName.equals("service-description")) {
			service = new Service();
		} else
		if (localName.equals("geopriv")) {
			geopriv = new Geopriv();
		}
	}

	public void endElement(String namespaceURL, String localName, String qname) {
		if (localName.equals("tuple")) {
			if (presence != null) {
				presence.addTuple(tuple);
			}
			tuple = null;
		} else
		if (localName.equals("person")) {
			if (person != null) {
				presence.setPerson(person);
				person = null;
			}
		} else
		if (localName.equals("note")) {
			if ((note != null) && (person != null)) {
				String value = StringUtils.decodeXML(accumulator.toString().trim());
				note.setValue(value);
				person.setNote(note);
				note = null;
			}			
		} else
		if (localName.equals("contact")) {
			if ((contact != null) && (tuple != null)) {
				contact.setUri(accumulator.toString().trim());
				tuple.addContact(contact);
				contact = null;
			}
		} else
		if (localName.equals("basic")) {
			basic = new Basic(accumulator.toString().trim());
			if (status != null) {
				status.setBasic(basic);
			} else
			if (willingness != null) {
				willingness.setBasic(basic);
			}
			basic = null;
		} else
		if (localName.equals("overriding-willingness")) {
			if ((willingness != null) && (person != null)) {
				person.setOverridingWillingness(willingness);
				willingness = null;
			}
		} else
		if (localName.equals("status-icon")) {
			String url = accumulator.toString();
			if ((icon != null) && (person != null)) {
				icon.setUrl(url.trim());
				person.setStatusIcon(icon);
				icon = null;
			}
		} else
		if (localName.equals("status")) {
			if ((status != null) && (tuple != null)) {
				tuple.setStatus(status);
				status = null;
			}
		} else
		if (localName.equals("timestamp")) {
			String timestamp = accumulator.toString();
			if (timestamp != null) {
				long ts = DateUtils.decodeDate(timestamp.trim());
				if (person != null) {
					person.setTimestamp(ts);
				} else
				if (tuple != null) {
					tuple.setTimestamp(ts);
				}
			}
		} else
		if (localName.equals("service-description")) {
			if ((service != null) && (tuple != null)) {
				tuple.setService(service);
				service = null;
			}
		} else
		if (localName.equals("service-id")) {
			if (service != null) {
				service.setId(accumulator.toString().trim());
			}
		} else
		if (localName.equals("homepage")){
			String homepage = accumulator.toString(); 
			if ((homepage != null) && (person != null)) {
				person.setHomePage(homepage.trim());
				homepage = null;
			}
		} else
		if (localName.equals("geopriv")) {
			if ((geopriv != null) && (presence != null)) {
				presence.setGeopriv(geopriv);
				geopriv = null;
			}
		} else
		if (localName.equals("method")) {
			if (geopriv != null) {
				geopriv.setMethod(accumulator.toString().trim());
			}
		} else
		if (localName.equals("pos")) {
			if (geopriv != null) {
				StringTokenizer st = new StringTokenizer(accumulator.toString().trim());
				try {
				    if (st.hasMoreTokens()) {
				    	geopriv.setLatitude(Double.parseDouble(st.nextToken()));
			    	}
				    if (st.hasMoreTokens()) {
				    	geopriv.setLongitude(Double.parseDouble(st.nextToken()));
			        }
				    if (st.hasMoreTokens()) {
				    	geopriv.setAltitude(Double.parseDouble(st.nextToken()));
			        }
				} catch(Exception e) {
					if (logger.isActivated()) {
						logger.error("Can't parse geoloc value", e);
					}
				}
			}
		} else
		if (localName.equals("presence")) {
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
