package org.gsma.joyn.ft;

/**
 * Intent broadcasted when a new file transfer invitation is received
 * 
 * @author jexa7410
 */
public class FileTransferIntent {
    /**
     * Intent
     */
	public final static String FILE_TRANSFER_INVITATION = "org.gsma.joyn.ft.FILE_TRANSFER_INVITATION";

	/**
	 * MSISDN of the contact sending the invitation
	 */
	public final static String EXTRA_CONTACT = "contact";
	
	/**
	 * Display name of the contact sending the invitation (extracted from the SIP address)
	 */
	public final static String EXTRA_DISPLAY_NAME = "contactDisplayname";

	/**
	 * Unique ID of the file transfer
	 */
	public final static String EXTRA_TRANSFER_ID = "transferId";

	/**
	 * Name of the file
	 */
	public final static String EXTRA_FILENAME = "filename";
	
	/**
	 * Size of the file in byte
	 */
	public final static String EXTRA_FILESIZE = "filesize";
	
	/**
	 * MIME type of the file.
	 */
	public final static String EXTRA_FILETYPE = "filetype";
}
