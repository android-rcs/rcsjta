package org.gsma.joyn.ft;

/**
 * Intent broadcasted when a new file transfer invitation is received
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileTransferIntent {
    /**
     * Broadcast action: a new file transfer has been received.
     * <p>Intent includes the following extras:
     * <ul>
     * <li> {@link #EXTRA_CONTACT} containing the MSISDN of the contact
     *  sending the invitation.
     * <li> {@link #EXTRA_DISPLAY_NAME} containing the display name of the
     *  contact sending the invitation (extracted from the SIP address).
     * <li> {@link #EXTRA_SHARING_ID} containing the unique ID of the file transfer.
     * <li> {@link #EXTRA_FILENAME} containing the name of file to be transfered.
     * <li> {@link #EXTRA_FILESIZE} containing the size of the file to be transfered.
     * <li> {@link #EXTRA_FILETYPE} containing the MIME type of the file to be transfered.
     * </ul>
     */
	public final static String ACTION_NEW_FILE_TRANSFER = "org.gsma.joyn.ft.action.NEW_FILE_TRANSFER";

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
	 * MIME type of the file
	 */
	public final static String EXTRA_FILETYPE = "filetype";
}
