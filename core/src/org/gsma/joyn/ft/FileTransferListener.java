package org.gsma.joyn.ft;

/**
 * File transfer event listener
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class FileTransferListener extends IFileTransferListener.Stub {
	/**
	 * Callback called when the file transfer is started
	 */
	public abstract void onTransferStarted();
	
	/**
	 * Callback called when the file transfer has been aborted
	 */
	public abstract void onTransferAborted();

	/**
	 * Callback called when the transfer has failed
	 * 
	 * @param error Error
	 * @see FileTransfer.Error
	 */
	public abstract void onTransferError(int error);
	
	/**
	 * Callback called during the transfer progress
	 * 
	 * @param currentSize Current transferred size in bytes
	 * @param totalSize Total size to transfer in bytes
	 */
	public abstract void onTransferProgress(long currentSize, long totalSize);

	/**
	 * Callback called when the file has been transferred
	 * 
	 * @param filename Filename including the path of the transferred file
	 */
	public abstract void onFileTransferred(String filename);
}
