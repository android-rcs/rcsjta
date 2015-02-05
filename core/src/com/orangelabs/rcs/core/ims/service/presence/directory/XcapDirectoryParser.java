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

package com.orangelabs.rcs.core.ims.service.presence.directory;

import java.util.Hashtable;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import com.orangelabs.rcs.utils.DateUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * XCAP directory parser
 * 
 * @author jexa7410
 */
public class XcapDirectoryParser extends DefaultHandler {

    private StringBuffer accumulator;
    private Folder folder = null;
    private Entry entry = null;

    private Hashtable<String, Folder> docs = new Hashtable<String, Folder>();

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
    public XcapDirectoryParser(InputSource inputSource) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(inputSource, this);
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

    public void startElement(String namespaceURL, String localName, String qname, Attributes attr) {
        accumulator.setLength(0);

        if (localName.equals("folder")) {
            String auid = attr.getValue("auid").trim();
            folder = new Folder(auid);
        } else if (localName.equals("entry")) {
            String uri = attr.getValue("uri").trim();
            entry = new Entry(uri);

            String etag = attr.getValue("etag");
            if (etag != null) {
                entry.setEtag(etag.trim());
            }

            String lastModified = attr.getValue("last-modified");
            if (lastModified != null) {
                long ts = DateUtils.decodeDate(lastModified.trim());
                entry.setLastModified(ts);
            }
        }
    }

    public void endElement(String namespaceURL, String localName, String qname) {
        if (localName.equals("folder")) {
            if (folder != null) {
                docs.put(folder.getAuid(), folder);
            }
            folder = null;
        } else if (localName.equals("entry")) {
            if ((folder != null) && (entry != null)) {
                folder.setEntry(entry);
            }
            entry = null;
        } else if (localName.equals("xcap-directory")) {
            if (logger.isActivated()) {
                logger.debug("XCAP directory document is complete");
            }
        }
    }

    public void endDocument() {
        if (logger.isActivated()) {
            logger.debug("End document");
        }
    }

    public Hashtable<String, Folder> getDocuments() {
        return docs;
    }
}
