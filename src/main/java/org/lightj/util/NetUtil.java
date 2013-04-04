package org.lightj.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class NetUtil {

	/**
	 * Validate an ip address (ipV4 only), every part within 0 - 255
	 * @param ipAddress
	 * @return
	 */
	public static boolean isValid(String ipAddress) {
		if (StringUtil.isNullOrEmpty(ipAddress)) return false;
		String[] tokens = ipAddress.split("\\.");
		if (tokens.length != 4) return false;
		for (int i = 0; i < tokens.length; i++) {
			if (StringUtil.isNullOrEmpty(tokens[i])) return false;
			try {
				int intToken = Integer.parseInt(tokens[i]);
				if (intToken < 0 || intToken > 255) return false;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Wether an ip is itself 127.0.0.1
	 * @param ipAddress
	 * @return
	 */
	public static boolean isSelf(String ipAddress) {
		try {
			return InetAddress.getByName(ipAddress).isSiteLocalAddress();
		} catch (UnknownHostException e) {
			return false;
		}
	}
	
	private static String myHostName;
	public static String getMyHostName() {
		if (myHostName==null) {
			synchronized (NetUtil.class) {
				if (myHostName==null) {
					try {
						myHostName = InetAddress.getLocalHost().getHostName();
					} catch (UnknownHostException e) {
						myHostName = "unknown-host";
					}
				}
			}
		}
		return myHostName;
	}
	
	/**
	 * Wether an ip is internal ip, that is
	 * 10.0.0.0 - 10.255.255.255
	 * 172.16.0.0 - 172.31.255.255
	 * 192.168.0.0 - 192.168.255.255
	 * @param ipAddress
	 * @return
	 */
	public static boolean isInternal(String ipAddress) {
		if (!isValid(ipAddress)) return false;
		String[] tokens = ipAddress.split("\\.");
		int firstToken = Integer.parseInt(tokens[0]);
		int secondToken = Integer.parseInt(tokens[1]);
		if (firstToken == 10 ||
				(firstToken == 172 && secondToken >= 16 && secondToken <= 31) ||
				(firstToken == 192 && secondToken == 168)) return true;
		return false;
	}
	
	/**
	 * convert a cidr type of string to standard start ip - end ip
	 * @param subnetStr
	 * @return
	 */
	public static String[] convertCidr(String subnetStr) {
		String[] rst = new String[2];
		if (!StringUtil.isNullOrEmpty(subnetStr) && subnetStr.indexOf('/') > 0) {
			int idx = subnetStr.indexOf('/');
			String startIp = subnetStr.substring(0, idx);
			int mask = Integer.parseInt(subnetStr.substring(idx+1));
			rst[0] = startIp;
			StringBuffer endIp = new StringBuffer();
			byte[] ipTokens = null;
			try {
				ipTokens = InetAddress.getByName(startIp).getAddress();
			} catch (UnknownHostException e) {
				// invalid address, ignore
				return null;
			}
			//for (String ipTokenString : startIp.split("\\.")) {
			for (byte ipToken : ipTokens) {
				int ipPortion = ipToken | (mask <= 0 ? 255 : (mask > 8 ? 0 : 255>>mask));
				mask -= 8;
				if (ipPortion < 0) {
					endIp.append(Integer.toString(ipPortion+256)).append('.');
				}
				else {
					endIp.append(Integer.toString(ipPortion)).append('.');
				}
			}
			rst[1] = endIp.substring(0, endIp.length()-1);
		}
		return rst;
	}
	
	/**
	 * get all ips in an ip range
	 * @param startIp
	 * @param endIp
	 * @return
	 */
	public static String[] getAllIpsInRange(String startIp, String endIp) {
		List<String> rst = new ArrayList<String>();
		try {
			byte[] start = InetAddress.getByName(startIp).getAddress();
			byte[] end = InetAddress.getByName(endIp).getAddress();
			for (int j = start[0]; j <= end[0]; j++) {
				for (int i = start[1]; i <= end[1]; i++) {
					for (int k = start[2]; k <= end[2]; k++) {
						int m = end[3]<0 ? end[3]+256 : end[3];
						for (int l = start[3]; l <= m; l++) {
							if (l > 1 && l < 255) {
								StringBuffer buf = new StringBuffer();
								buf.append(Integer.toString((j<0 ? j+256 : j))).append('.').append(Integer.toString((i<0 ? i+256 : i))).append('.');
								buf.append(Integer.toString((k<0 ? k+256 : k))).append('.').append(Integer.toString(l));
								rst.add(buf.toString());
							}
						}
					}
				}
			}
		} catch (UnknownHostException e) {
			// invalid ip range, ignore
		}
		return rst.toArray(new String[0]);
	}
	
	/**
	 * check wether an ip address is in the subnet
	 * @param ipAddress
	 * @param subnet
	 * @return
	 */
	public static final boolean isInRange(String ipAddress, String subnet) {
		String[] ipRange = convertCidr(subnet);
		try {
			byte[] ipTokens = InetAddress.getByName(ipAddress).getAddress();
			byte[] startTokens = InetAddress.getByName(ipRange[0]).getAddress();
			byte[] endTokens = InetAddress.getByName(ipRange[1]).getAddress();
			for (int i = 0; i < ipTokens.length; i++) {
				if (ipTokens[i] < startTokens[i] || ipTokens[i] > endTokens[i]) {
					return false;
				}
			}
			return true;
		} catch (UnknownHostException e) {
			// invalid address, ignore
			return false;
		}
	}
	
	/**
	 * ns lookup
	 * @param host
	 * @return
	 */
    public static String nslookup(String host) throws UnknownHostException {
        InetAddress inetaddress = null;
		inetaddress = InetAddress.getByName(host);
		byte abyte0[] = inetaddress.getAddress();
		StringBuffer s1 = new StringBuffer();
		for (int i = 0; i < abyte0.length; i++) {
			if (i > 0) {
				s1.append(".");
			}
			s1.append(abyte0[i] & 0xff);
		}
		return s1.toString();
    }

    /**
     * reverse ns lookup
     * @param ip
     * @return
     * @throws UnknownHostException
     */
    public static String reversLookup(String ip) throws UnknownHostException {
    	String host = InetAddress.getByName(ip).getHostName();
    	return host;
    }
    
    /**
     * port scan
     * @param host
     * @param ports
     * @return
     * @throws UnknownHostException
     */
    public static boolean portScan(String host, int... ports) throws UnknownHostException {
    	InetAddress remote = InetAddress.getByName(host);
		for (int port : ports) {
			try {
				Socket s = new Socket(remote, port);
				s.close();
				return true;
			} catch (IOException ex) {
				// The remote host is not listening on this port
			}
		}
		return false;
	}
    
    /**
     * test if two ip addresses are equal
     * @param ip1
     * @param ip2
     * @return
     */
    public static boolean isEqual(String ip1, String ip2) {
    	if (ip1 == null && ip2 == null) {
    		return true;
    	}else if (ip1 == null || ip2 == null){
    		return false;
    	}
    	else if (ip1.equals(ip2)) {
    		return true;
    	}
    	else {
        	try {
				byte[] addr1 = InetAddress.getByName(ip1).getAddress();
	        	byte[] addr2 = InetAddress.getByName(ip2).getAddress();
	        	for (int i = 0; i < addr1.length; i++) {
	        		if (addr1[i] != addr2[i]) {
	        			return false;
	        		}
	        	}
	        	return true;
			} catch (UnknownHostException e) {
				return false;
			}
    	}
    }

	/**
	 * Verifies if the given FQDN and IP address have matching forward
	 * and reverse DNS entries.
	 * 
	 * @param fqdn the fully qualified domain name
	 * @param ip the IP address
	 * @return boolean flag indicating true if matching forward and reverse
	 * DNS entries exist, otherwise, false.
	 */
    public static boolean isDNSSetUp(String fqdn, String ip) {
		boolean result = false;
		try {
			if (fqdn != null && ip != null) {
				result = ip.equalsIgnoreCase(NetUtil.nslookup(fqdn)) 
					&& fqdn.equalsIgnoreCase(NetUtil.reversLookup(ip));
			}
		} catch (UnknownHostException uhe) {
			//Do nothing - failure case
		}
		return result;
	}
	
}
