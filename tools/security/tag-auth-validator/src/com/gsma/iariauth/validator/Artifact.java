package com.gsma.iariauth.validator;

/**
 * A class encapsulating a processing artifact from package processing or validation
 */
public class Artifact {
	
	/************************************
	 * Error/warning conditions
	 ************************************/

	/* Malformed packaging or data:
	 * the application package or its metadata is malformed or might have been corrupted.
	 * Details: none */
	public static final int CODE_MALFORMED = 1000;

	/* Permission denied:
	 * the application is not permitted to run based on the configured policy.
	 */
	public static final int CODE_BLOCKED_DENIED = 1001;
	
	/* Revoked application:
	 * the application has been revoked and is no longer allowed to be installed.
	 * Details:
	 * 0: String IARI: the revoked IARI */
	public static final int CODE_BLOCKED_REVOKED = 1002;

	/* The trust certificate on the IARI authorisation has expired, so it might be dangerous to run.
	 * Details:
	 * 0: ICertificateInfo: expired certificate */
	public static final int CODE_BLOCKED_EXPIRED = 1003;

	/* Unable to obtain the verification status of the trust certificate,
	 * so it might be dangerous to run; processing as untrusted.
	 * Details:
	 * 0: ICertificateInfo[]: certificates whose status is unknown */
	public static final int CODE_BLOCKED_CERT_UNKNOWN = 1004;

	/************************************
	 * Public API
	 ************************************/

	/*
	 * The nature of the problem as an error code
	 * STATUS_INVALID: widget package or associated assets not valid
	 * STATUS_DENIED: the widget does not have permission for the requested functionality
	 * STATUS_CAPABILITY: the runtime does not have the requested functionality
	 * others ...
	 */
	public int status;

	/*
	 * The reason identifier
	 */
	public int code;

	/*
	 * Details that may be used in constructing a useful error message.
	 * Each reason will define the set of expected details.
	 */
	public Object[] details;
	
	/*
	 * The specific reason - an internally understood string
	 * to pinpoint the assertion or issue
	 */
	public String reason;

	/*
	 * Simplistic constructor
	 */
	public Artifact(String reason) {
		this.code = CODE_MALFORMED;
		this.reason = reason;
		status = ProcessingResult.STATUS_INVALID;
	}
	
	/*
	 * Constructor
	 */
	public Artifact(int status, int code, String reason, Object[] details) {
		this.status = status;
		this.code = code;
		this.reason = reason;
		this.details = details;
	}

	/*
	 * Simplistic status text
	 */
	public String getStatusText() {
		String statusText;
		switch(status) {
		case ProcessingResult.STATUS_INVALID:
			statusText = "STATUS_INVALID";
			break;
		case ProcessingResult.STATUS_DENIED:
			statusText = "STATUS_DENIED";
			break;
		default:
			statusText = "STATUS_UNKNOWN";
		}
		return statusText;
	}
	
	/*
	 * Get details text for error or warning dialogs
	 */
	public String[] getDetailsText() {
		String[] detailsText = null;
		if(details != null) {
			detailsText = new String[details.length];
			for(int i=0; i < details.length; i++) {
				detailsText[i] = details[i].toString();
			}
		}
		return detailsText;
	}

	public String toString() {
		StringBuffer result = new StringBuffer(getStatusText() + '\n');
		result.append("code=");
		result.append(code);
		result.append('\n');
		if(reason != null) {
			result.append("reason=");
			result.append(reason);
			result.append('\n');
		}
		if(details != null) {
			for(String detail : getDetailsText()) {
				result.append(detail);
			}
		}

		return result.toString();
	}
}
