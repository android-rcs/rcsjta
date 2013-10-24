package com.orangelabs.rcs.core.ims.service.im.filetransfer.http;

import org.xml.sax.helpers.DefaultHandler;

/**
 * File transfer over HTTP info document
 *
 * @author hhff3235
 */
public class FileTransferHttpResumeInfo extends DefaultHandler{
	/**
	 * start-offset in bytes
	 */
	private int start = 0;

	/**
	 * end-offset in bytes
	 */
	private int end = 0;

	/**
	 * HTTP upload URL for the file
	 */
	private String url = null;


	/**
	 * @return the start
	 */
	protected int getStart() {
		return start;
	}

	/**
	 * @param start the start to set
	 */
	protected void setStart(int start) {
		this.start = start;
	}

	/**
	 * @return the end
	 */
	protected int getEnd() {
		return end;
	}

	/**
	 * @param end the end to set
	 */
	protected void setEnd(int end) {
		this.end = end;
	}

	/**
	 * @return the url
	 */
	protected String getUrl() {
		return url;
	}

	/**
	 * @param url the url to set
	 */
	protected void setUrl(String url) {
		this.url = url;
	}

}
