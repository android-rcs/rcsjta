// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * A class that tries to locate name servers and the search path to
 * be appended to unqualified names.
 *
 * The following are attempted, in order, until one succeeds.
 * <UL>
 *   <LI>The properties 'dns.server' and 'dns.search' (comma delimited lists)
 *       are checked.  The servers can either be IP addresses or hostnames
 *       (which are resolved using Java's built in DNS support).
 *   <LI>The sun.net.dns.ResolverConfiguration class is queried.
 *   <LI>On Unix, /etc/resolv.conf is parsed.
 *   <LI>On Windows, ipconfig/winipcfg is called and its output parsed.  This
 *       may fail for non-English versions on Windows.
 *   <LI>"localhost" is used as the nameserver, and the search path is empty.
 * </UL>
 *
 * These routines will be called internally when creating Resolvers/Lookups
 * without explicitly specifying server names, and can also be called
 * directly if desired.
 *
 * @author Brian Wellington
 * @author <a href="mailto:yannick@meudal.net">Yannick Meudal</a>
 * @author <a href="mailto:arnt@gulbrandsen.priv.no">Arnt Gulbrandsen</a>
 */

public class ResolverConfig {

private String [] servers = null;
private Name [] searchlist = null;
private int ndots = -1;

private static ResolverConfig currentConfig;

static {
	refresh();
}

public
ResolverConfig() {
	findAndroid();
}

private void
addServer(String server, List list) {
	if (list.contains(server))
		return;
	if (Options.check("verbose"))
		System.out.println("adding server " + server);
	list.add(server);
}

private void
addSearch(String search, List list) {
	Name name;
	if (Options.check("verbose"))
		System.out.println("adding search " + search);
	try {
		name = Name.fromString(search, Name.root);
	}
	catch (TextParseException e) {
		return;
	}
	if (list.contains(name))
		return;
	list.add(name);
}

private int
parseNdots(String token) {
	token = token.substring(6);
	try {
		int ndots = Integer.parseInt(token);
		if (ndots >= 0) {
			if (Options.check("verbose"))
				System.out.println("setting ndots " + token);
			return ndots;
		}
	}
	catch (NumberFormatException e) {
	}
	return -1;
}

private void
configureFromLists(List lserver, List lsearch) {
	if (servers == null && lserver.size() > 0)
		servers = (String []) lserver.toArray(new String[0]);
	if (searchlist == null && lsearch.size() > 0)
		searchlist = (Name []) lsearch.toArray(new Name[0]);
}

private void
configureNdots(int lndots) {
	if (ndots < 0 && lndots > 0)
		ndots = lndots;
}

/**
 * Parses the output of getprop, which is the only way to get DNS
 * info on Android. getprop might disappear in future releases, so
 * this code comes with a use-by date.
 */
private void
findAndroid() {
	// This originally looked for all lines containing .dns; but
	// http://code.google.com/p/android/issues/detail?id=2207#c73
	// indicates that net.dns* should always be the active nameservers, so
	// we use those.
		final String re1 = "^\\d+(\\.\\d+){3}$";
		final String re2 = "^[0-9a-f]+(:[0-9a-f]*)+:[0-9a-f]+$";
		ArrayList lserver = new ArrayList();
		ArrayList lsearch = new ArrayList();
		try {
			Class SystemProperties = Class.forName("android.os.SystemProperties");
			Method method = SystemProperties.getMethod("get", new Class[] { String.class });
			final String[] netdns = new String[] { "net.dns1", "net.dns2", "net.dns3", "net.dns4" };
			for (int i = 0; i < netdns.length; i++) {
				Object[] args = new Object[] { netdns[i] };
				String v = (String) method.invoke(null, args);
				if (v != null && (v.matches(re1) || v.matches(re2)) &&

				!lserver.contains(v))
					lserver.add(v);
			}

		} catch (Exception e) {
			// ignore resolutely
		}
		configureFromLists(lserver, lsearch);
	}

/** Returns all located servers */
public String []
servers() {
	return servers;
}

/** Returns the first located server */
public String
server() {
	if (servers == null)
		return null;
	return servers[0];
}

/** Returns all entries in the located search path */
public Name []
searchPath() {
	return searchlist;
}

/**
 * Returns the located ndots value, or the default (1) if not configured.
 * Note that ndots can only be configured in a resolv.conf file, and will only
 * take effect if ResolverConfig uses resolv.conf directly (that is, if the
 * JVM does not include the sun.net.dns.ResolverConfiguration class).
 */
public int
ndots() {
	if (ndots < 0)
		return 1;
	return ndots;
}

/** Gets the current configuration */
public static synchronized ResolverConfig
getCurrentConfig() {
	return currentConfig;
}

/** Gets the current configuration */
public static void
refresh() {
	ResolverConfig newConfig = new ResolverConfig();
	synchronized (ResolverConfig.class) {
		currentConfig = newConfig;
	}
}

}
