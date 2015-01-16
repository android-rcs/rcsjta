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
package com.orangelabs.rcs.core.ims.service.im.chat.geoloc;

import java.io.IOException;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.orangelabs.rcs.utils.DateUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Geolocation info parser
 *
 * @author vfml3370
 */
public class GeolocInfoParser extends DefaultHandler {
	
	/* Geoloc-Info SAMPLE:
	<?xml version="1.0" encoding="UTF-8"?>
	<rcsenvelope xmlns="urn:gsma:params:xml:ns:rcs:rcs:geolocation" xmlns:rpid="urn:ietf:params:xml:ns:pidf:rpid" xmlns:gp="urn:ietf:params:xml:ns:pidf:geopriv10" xmlns:gml="http://www.opengis.net/gml" xmlns:gs="http://www.opengis.net/pidflo/1.0" entity="tel:+12345678901">
	<rcspushlocation id="a123" label ="meeting location" >
	<rpid:place-type rpid:until="2012-03-15T21:00:00-05:00">
	</rpid:place-type>
	<rpid:time-offset rpid:until="2012-03-15T21:00:00-05:00"></rpid:time-offset>
	<gp:geopriv>
	<gp:location-info>
	<gs:Circle srsName="urn:ogc:def:crs:EPSG::4326">
	<gml:pos>48.731964 -3.45829</gml:pos>
	<gs:radius uom="urn:ogc:def:uom:EPSG::9001">10</gs:radius>
	</gs:Circle>
	</gp:location-info>
	<gp:usage-rules>
	<gp:retention-expiry>2012-03-15T21:00:00-05:00</gp:retention-expiry>
	</gp:usage-rules>
	</gp:geopriv>
	<timestamp>2012-03-15T16:09:44-05:00</timestamp>
	</rcspushlocation>
	</rcsenvelope>
   */
	
	private StringBuffer accumulator;
	private GeolocInfoDocument geoloc = null;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     *
     * @param inputSource Input source
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws Exception
     */
    public GeolocInfoParser(InputSource inputSource) throws ParserConfigurationException, SAXException, IOException {
    	SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(inputSource, this);
	}

	public GeolocInfoDocument getGeoLocInfo() {
		return geoloc;
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
		if (localName.equals("rcsenvelope")) {
			String entity = attr.getValue("entity").trim();			
			geoloc = new GeolocInfoDocument(entity);
		} else
		if (localName.equals("rcspushlocation")) {
			if (logger.isActivated()) {
				logger.debug("rcspushlocation");
			}
			if (geoloc != null) {
				String label = attr.getValue("label").trim();	
				geoloc.setLabel(label);
			}
		} 
	}

	public void endElement(String namespaceURL, String localName, String qname) {
		if (localName.equals("radius")) {
			if (geoloc != null) {
				geoloc.setRadius(Float.parseFloat(accumulator.toString().trim()));
			}
		} else
		if (localName.equals("retention-expiry")) {			                  
			if (geoloc != null) {
				geoloc.setExpiration(DateUtils.decodeDate(accumulator.toString().trim()));
			}
		} else		
		if (localName.equals("pos")) {
			if (geoloc != null) {
				StringTokenizer st = new StringTokenizer(accumulator.toString().trim());
				try {
					if (st.hasMoreTokens()) {
						geoloc.setLatitude(Double.parseDouble(st.nextToken()));
					}
					if (st.hasMoreTokens()) {
						geoloc.setLongitude(Double.parseDouble(st.nextToken()));
					}
				} catch(Exception e) {
					if (logger.isActivated()) {
						logger.error("Can't parse geoloc value", e);
					}
				}
			}
		}
	}

	public void endDocument() {
		if (logger.isActivated()) {
			logger.debug("End document");
		}
	}
}
