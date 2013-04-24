package org.gsma.joyn;

/**
 * Build infos
 * 
 * @author jexa7410
 */
public class Build {
	/**
	 * API release
	 */
	public static final String API_RELEASE = "1.0";

	/**
	 * API release implementor
	 */
	public static final String API_CODENAME = "GSMA";
	
	/**
	 * GSMA releases
	 */
	public static enum GsmaRelease {
		RCS_2, RCSE_HOTFIXES_1_2, RCSE_BLACKBIRD_BASE
    }
	
	/**
	 * Supported GSMA release
	 */
	public static final GsmaRelease GSMA_SUPPORTED_RELEASE = GsmaRelease.RCSE_HOTFIXES_1_2;
}
