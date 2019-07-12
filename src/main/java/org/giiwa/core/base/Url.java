package org.giiwa.core.base;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.X;
import org.giiwa.core.json.JSON;

public class Url {

	private static Log log = LogFactory.getLog(Url.class);

	String url;
	String protocol;
	String uri;
	String ip;
	String port;

	Map<String, String> query;

	public void setProtocol(String protocol) {
		this.protocol = protocol;
		this.url = null;
	}

	public void setUri(String uri) {
		this.uri = uri;
		this.url = null;
	}

	public void setIp(String ip) {
		this.ip = ip;
		this.url = null;
	}

	public void setPort(String port) {
		this.port = port;
		this.url = null;
	}

	public JSON toJson() {
		return JSON.create().append("url", url);
	}

	public static Url fromJson(JSON jo) {
		String url = jo.getString("url");
		return Url.create(url);
	}

	public String getUrl() {
		if (X.isEmpty(url)) {

			StringBuilder sb = new StringBuilder();
			if (!X.isEmpty(protocol)) {
				sb.append(protocol).append("://");
			}
			if (!X.isEmpty(ip)) {
				sb.append(ip);
			}
			if (!X.isEmpty(port)) {
				sb.append(":").append(port);
			}
			if (!X.isEmpty(uri)) {
				sb.append(uri);
			}
			if (query != null && !query.isEmpty()) {
				try {
					StringBuilder sb1 = new StringBuilder();
					for (String name : query.keySet()) {
						if (sb1.length() > 0) {
							sb1.append("&");
						}
						sb1.append(name).append("=");
						sb1.append(query.get(name));
					}
					sb.append("?").append(sb1.toString());
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
			url = sb.toString();

		}
		return url;
	}

	public String getProtocol() {
		return protocol;
	}

	public String getIp() {
		return ip;
	}

	public int getPort(int defaultPort) {
		int p = X.toInt(port);
		return p > 0 ? p : defaultPort;
	}

	public String getUri() {
		return uri;
	}

	public String getPort() {
		return port;
	}

	public Map<String, String> getQuery() {
		return query;
	}

	/**
	 * 
	 * @param url, protocol://ip:port?user=&passwd=
	 * @return the Url
	 */
	public static Url create(String url) {
		if (!X.isEmpty(url)) {
			Url u = new Url();
			u.url = url;
			try {
				if (u.parse()) {
					return u;
				}
			} catch (Exception e) {
				// e.printStackTrace();
				log.error(e.getMessage(), e);
			}
		}
		return null;
	}

	/**
	 * parse more parameter from original url according to the refer
	 * 
	 * @param refer e.g. "http://{ip}:{port}/{path}?
	 * @return
	 */
	public boolean parse(String refer) {
		if (X.isEmpty(refer))
			return false;

		Url u1 = Url.create(refer);

		if (u1 == null) {
			return false;
		}

		if (!X.isSame(this.protocol, u1.protocol)) {
			return false;
		}

		// ip
		String ip = u1.getIp();
		if (!X.isEmpty(ip)) {
			int i = ip.indexOf("{");
			if (i >= 0) {
				int j = ip.indexOf("}", i + 1);
				if (j > i) {
					String name = ip.substring(i + 1, j);
					int k = name.indexOf("=");
					if (k > 0) {
						name = name.substring(0, k);
					}
					String value = this.ip.substring(i, this.ip.length() - (ip.length() - j - 1));
					put(name, value);
				}
			}
		}

		// port
		String port = u1.getPort();
		if (!X.isEmpty(port)) {
			int i = port.indexOf("{");
			if (i >= 0) {
				int j = port.indexOf("}", i + 1);
				if (j > i) {
					String name = port.substring(i + 1, j);
					int k = name.indexOf("=");
					if (k > 0) {
						name = name.substring(0, k);
					}
					String value = X.isEmpty(this.port) ? X.EMPTY
							: (this.port.substring(i, this.port.length() - (port.length() - j - 1)));
					put(name, value);
				}
			}
		}

		// uri
		String uri = u1.getUri();
		if (!X.isEmpty(uri)) {
			int i = uri.indexOf("{");
			if (i >= 0) {
				int j = uri.indexOf("}", i + 1);
				if (j > i) {
					String name = uri.substring(i + 1, j);
					int k = name.indexOf("=");
					if (k > 0) {
						name = name.substring(0, k);
					}
					String value = this.uri.substring(i, this.uri.length() - (uri.length() - j - 1));
					put(name, value);
				}
			}
		}

		// query
		if (u1.query != null && !u1.query.isEmpty()) {
			for (String s : u1.query.keySet()) {
				String v = u1.query.get(s);

				int i = v.indexOf("{");
				if (i >= 0) {
					int j = v.indexOf("}", i + 1);
					if (j > i) {
						String name = v.substring(i + 1, j);
						int k = name.indexOf("=");
						if (k > 0) {
							name = name.substring(0, k);
						}
						String value = this.get(s);
						put(name, value);
					}
				}

			}
		}
		return true;
	}

	private boolean parse() throws Exception {
		String s = url;
		int i = s.indexOf("://");
		if (i > 0) {
			protocol = s.substring(0, i);
			s = s.substring(i + 3);
		}

		i = s.indexOf("?");
		if (i >= 0) {
			String query = (i >= (s.length() - 1)) ? null : s.substring(i + 1);
			s = i > 0 ? s.substring(0, i) : null;

			// parse query
			if (!X.isEmpty(query)) {
				String[] ss = query.split("&");
				for (String s1 : ss) {
					i = s1.indexOf("=");
					if (i > -1) {
						String n = s1.substring(0, i);
						String v = s1.substring(i + 1);
						this.put(n, URLDecoder.decode(v, "UTF8"));
					}
				}
			}
		}

		if (s != null) {
			i = s.indexOf("/");
			if (i >= 0) {
				uri = s.substring(i);
				s = i > 0 ? s.substring(0, i) : null;
			}
		}

		if (s != null) {
			i = s.indexOf(":");
			if (i >= 0) {
				port = (i >= (s.length() - 1)) ? null : s.substring(i + 1);
				ip = i > 0 ? s.substring(0, i) : null;
			} else {
				ip = s;
			}
		}

		return true;
	}

	@Override
	public String toString() {
		return "Url [url=" + url + ", protocol=" + protocol + ", ip=" + ip + ", port=" + port + ", uri=" + uri
				+ ", query=" + query + "]";
	}

	public void put(String name, String value) {
		if (query == null) {
			this.query = new TreeMap<String, String>();
		}
		this.query.put(name, value);
	}

	public String get(String name) {
		return query == null ? X.EMPTY : query.get(name);
	}

	public Url append(String name, String value) {
		if (query == null) {
			query = new TreeMap<String, String>();
		}
		query.put(name, value);
		this.url = null;

		return this;
	}

	public String encodedUrl() {
		StringBuilder sb = new StringBuilder();
		if (!X.isEmpty(protocol)) {
			sb.append(protocol).append("://");
		}
		if (!X.isEmpty(ip)) {
			sb.append(ip);
		}
		if (!X.isEmpty(port)) {
			sb.append(":").append(port);
		}
		if (!X.isEmpty(uri)) {
			sb.append(uri);
		}
		if (query != null && !query.isEmpty()) {
			try {
				StringBuilder sb1 = new StringBuilder();
				for (String name : query.keySet()) {
					if (sb1.length() > 0) {
						sb1.append("&");
					}
					sb1.append(name).append("=");
					sb1.append(URLEncoder.encode(query.get(name), "UTF8"));
				}
				sb.append("?").append(sb1.toString());
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return sb.toString();
	}

	public boolean isProtocol(String protocol) {
		return X.isEmpty(this.protocol) || X.isSame("*", this.protocol) || X.isSame(this.protocol, protocol);
	}

	public static String encode(String url) {
		if (X.isEmpty(url)) {
			return url;
		}

		try {
			return URLEncoder.encode(url, "UTF8");
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public static String decode(String url) {
		if (X.isEmpty(url)) {
			return url;
		}

		try {
			return URLDecoder.decode(url, "UTF8");
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public String getPath() {
		return path(this.getUrl());
	}

	public static String path(String url) {
		int i = url.lastIndexOf("/");
		if (i > 8) {
			return url.substring(0, i + 1);
		}
		return url + "/";
	}

}
