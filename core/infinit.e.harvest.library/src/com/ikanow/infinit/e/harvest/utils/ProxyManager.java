/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.ikanow.infinit.e.harvest.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

public class ProxyManager {

	private static final Logger logger = Logger.getLogger(ProxyManager.class);
	
	private static boolean _bConfiguredSystem = false;

	private static class CustomProxyInfo {
		Proxy[] proxies;
		int nCurrProxy = -1;
	}
	
	private static int _nCurrSingletonProxies = -1;
	private static Proxy _singletonProxies[] = { Proxy.NO_PROXY };
	private static HashMap<String, CustomProxyInfo> _proxyOverrides = null;
	
	public static Proxy getProxy(URL url, String proxyOverride) throws IOException { // So you can have sourcr and URL-dependent proxies  in the future
		if (null == proxyOverride) { // use system default
			if (!_bConfiguredSystem) { // (configure system default first time)
				PropertiesManager props = new PropertiesManager();
				String proxyStr = props.getHarvestProxy();
				ArrayList<Proxy> tmp = new ArrayList<Proxy>();
				if ((null == proxyStr) || proxyStr.isEmpty()) { // (no proxy configured)
					return _singletonProxies[0];
				}
				initProxy(proxyStr, tmp);
				_singletonProxies = tmp.toArray(new Proxy[tmp.size()]);

				_bConfiguredSystem = true;
			}//TESTED
			if (0 == _singletonProxies.length) { // No proxies started:
				throw new IOException("No proxies available");
			}
			if (_singletonProxies.length > 1) {
				synchronized (ProxyManager.class) {
					_nCurrSingletonProxies++;
					if (_nCurrSingletonProxies >= _singletonProxies.length) {
						_nCurrSingletonProxies = 0;
					}
					//DEBUG
					//System.out.println("PROXY USE: " + _nCurrSingletonProxies + ": " + _singletonProxies[_nCurrSingletonProxies].toString());
					
					return _singletonProxies[_nCurrSingletonProxies];
				}
			}//TESTED
			//DEBUG
			//System.out.println("PROXY USE: (single): " + _singletonProxies[0].toString()); //TOTEST
			return _singletonProxies[0]; // (simplest case)
		}
		else if (proxyOverride.equalsIgnoreCase("direct") || proxyOverride.equalsIgnoreCase("no_proxy")) 
		{ 
			return Proxy.NO_PROXY; // override to direct
		}//TESTED
		else { // use override - basically identical logic but we store the info in a map
			synchronized (ProxyManager.class) {
				if (null == _proxyOverrides) {
					_proxyOverrides = new HashMap<String, CustomProxyInfo>();
				}
				CustomProxyInfo overriddenProxyInfo = _proxyOverrides.get(proxyOverride);
				if (null == overriddenProxyInfo) {
					ArrayList<Proxy> tmp = new ArrayList<Proxy>();
					initProxy(proxyOverride, tmp);
					overriddenProxyInfo = new CustomProxyInfo();
					overriddenProxyInfo.proxies = tmp.toArray(new Proxy[tmp.size()]);
					_proxyOverrides.put(proxyOverride, overriddenProxyInfo);
				}
				if (0 == overriddenProxyInfo.proxies.length) { // No proxies started:
					throw new IOException("No proxies available");
				}
				overriddenProxyInfo.nCurrProxy++;
				if (overriddenProxyInfo.nCurrProxy >= overriddenProxyInfo.proxies.length) {
					overriddenProxyInfo.nCurrProxy = 0;
				}
				//DEBUG
				//System.out.println("OVERRIDE PROXY USE: (" + url.toString() + ") " + overriddenProxyInfo.nCurrProxy + ": " + overriddenProxyInfo.proxies[overriddenProxyInfo.nCurrProxy].toString());
				
				return overriddenProxyInfo.proxies[overriddenProxyInfo.nCurrProxy];
			}
		}//TESTED
	}//TESTED
	
	private static void initProxy(String proxyStrList, ArrayList<Proxy> proxyList) {
		String[] proxyStrItems = proxyStrList.split("\\s*,\\s*");
		for (String proxyStr: proxyStrItems) {
			if (null != proxyStr) {
				if (proxyStr.startsWith("http://")) { 
					long nPortPair = 80;
					String[] hostPort = proxyStr.substring(7).split(":");
					if (hostPort.length > 1) {
						nPortPair = getPortPair(hostPort[1]);
					}
					addToProxyList(Proxy.Type.HTTP, hostPort[0], nPortPair, proxyList);
				}
				else if (proxyStr.startsWith("socks://")) {
					String[] hostPort = proxyStr.substring(8).split(":");				
					if (hostPort.length > 1) {
						long nPortPair = getPortPair(hostPort[1]);
						addToProxyList(Proxy.Type.SOCKS, hostPort[0], nPortPair, proxyList);
					}				
				}
				// HTTPS proxy not supported
			}
		}
	}//TESTED
	
	// Sticks the address:port-range into an array list
	
	private static void addToProxyList(Proxy.Type type, String addr, long nPortPair, ArrayList<Proxy> listOfProxies) {

		int nMinPort = (int)(nPortPair & 0xFFFF);
		int nMaxPort = (int)(nPortPair >> 32);
		if (nMaxPort < nMinPort) {
			nMaxPort = nMinPort;
		}
		for (int nPort = nMinPort; nPort <= nMaxPort; ++nPort) {
			InetSocketAddress endpoint = new InetSocketAddress(addr, nPort);
			
			try {
				InetSocketAddress client = new InetSocketAddress(0);
				Socket socket = new Socket();
				socket.bind(client);
				socket.connect(endpoint, 1000); // (1s timeout)
				socket.close();
				listOfProxies.add(new Proxy(type, endpoint));
			}
			catch (Exception e) { // (any exception)
				logger.warn("Failed to connect to: " + endpoint.toString() + ", skipping");
			}//TESTED			
		}
		
	}//TESTED 
	
	// Parses portmin[-portmax] into portmax << 32 | portmin
	
	private static long getPortPair(String port) {
		try {
			long nPortMin = Long.parseLong(port);
			return nPortMin;
		}
		catch (NumberFormatException e) {
			String[] ports = port.split("-");
			long nPortMin = Long.parseLong(ports[0]);
			long nPortMax = Long.parseLong(ports[1]);
			return nPortMin | (nPortMax << 32);
		}
	}//TESTED
}
