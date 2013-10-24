package com.orangelabs.rcs.core.ims.service.im.filetransfer.http;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.orangelabs.rcs.utils.logger.Logger;

public class FileTransferHttpResumeInfoParser extends DefaultHandler {
/*  File-Transfer HTTP SAMPLE:
	<?xml version="1.0" encoding=”UTF-8”?>
	<file-resume-info>
	<file-range start="[start-offset in bytes]" end="[end-offset in bytes]" / >
	<data url="[HTTP upload URL for the file]"/>
	</file-resume-info>
*/
	
    /**
     * File transfer over HTTP info document
     */
	private FileTransferHttpResumeInfo ftResumeInfo = null;
	
    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());
	
	public FileTransferHttpResumeInfoParser(InputSource ftHttpInput) throws ParserConfigurationException, SAXException, IOException {
	    	SAXParserFactory factory = SAXParserFactory.newInstance();
	        SAXParser parser = factory.newSAXParser();
	        parser.parse(ftHttpInput, this);
	}

	public FileTransferHttpResumeInfo getResumeInfo() {
		return ftResumeInfo;
	}
	
	/**
	 * Receive notification of the beginning of the document.
	 */
	public void startDocument() {
		if (logger.isActivated()) {
			logger.debug("Start document");
		}
	}
	
	/**
	 * Receive notification of the start of an element.
	 */
	public void startElement(String namespaceURL, String localName,	String qname, Attributes attr) {
		if (localName.equals("file-resume-info")) {		
			ftResumeInfo = new FileTransferHttpResumeInfo();
		} else
		if (localName.equals("file-range")) {
			if (ftResumeInfo != null) {
				String start = attr.getValue("start").trim();	
				ftResumeInfo.setStart(Integer.parseInt(start));
				String end = attr.getValue("end").trim();	
				ftResumeInfo.setEnd(Integer.parseInt(end));
				
			}
		} else		
		if (localName.equals("data")) {	
			if (ftResumeInfo != null) {
				String url = attr.getValue("url").trim();	
				ftResumeInfo.setUrl(url);
			}
		}
	}
	
	/**
	 * Receive notification of the end of the document.
	 */
	public void endDocument() {
		if (logger.isActivated()) {
			logger.debug("End document");
		}
	}
}
