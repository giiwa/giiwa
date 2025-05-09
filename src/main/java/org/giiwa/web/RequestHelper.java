/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.web;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Config;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.misc.Html;
import org.giiwa.misc.Url;
import org.giiwa.web.Controller.NameValue;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public class RequestHelper {

	private static Log log = LogFactory.getLog(RequestHelper.class);

	/**
	 * the request
	 */
	public HttpServletRequest req;

	private static String ENCODING = "UTF-8";

	private boolean _multipart;

	public static RequestHelper create(HttpServletRequest req) {

		RequestHelper r = new RequestHelper();
		r.req = req;

		try {
			r._multipart = r.isMultipartContent();
			r.setCharacterEncoding(ENCODING);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return r;
	}

	public boolean isMultipartContent() {
		return JakartaServletFileUpload.isMultipartContent(req);
	}

	public void setCharacterEncoding(String ENCODING) throws UnsupportedEncodingException {
		req.setCharacterEncoding(ENCODING);
	}

	public Object getAttribute(String name) {
		return req.getAttribute(name);
	}

	public void setAttribute(String name, Object o) {
		req.setAttribute(name, o);
	}

	public final String getString(String name) {
		String s = this.getHtml(name);
		if (s != null) {
			return s.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
		}
		return null;
	}

	/**
	 * uploaded file
	 */
	private JSON _files = null;
	private JSON _jsons = null;

	@SuppressWarnings("rawtypes")
	public final synchronized String getHtml(final String name) {

		if (X.isEmpty(name)) {
			return null;
		}

		if (rewrite != null && rewrite.containsKey(name)) {
			return rewrite.get(name);
		}

		String c1 = req.getContentType();

		try {
			if (c1 != null && c1.indexOf("application/json") > -1) {
				if (_jsons == null) {
					String s = X.IO.read(req.getReader());
					_jsons = JSON.fromObject(s);
				}
				if (_jsons != null) {
					Object v1 = _get(name);
					if (v1 != null) {
						return v1.toString().trim();
					}
				}
			}

			if (this._multipart) {
				FileItem i = this.file(name);
				if (i != null && i.isFormField()) {
					InputStream in = i.getInputStream();
					byte[] bb = new byte[in.available()];
					in.read(bb);
					in.close();
					return new String(bb, ENCODING);
				}
			}
		} catch (Exception e) {
			log.error(this.getRequestURI(), e);
		}

		// get from body
		String[] ss = req.getParameterValues(name);

		if (ss == null || ss.length == 0) {
			// get from query
			String s = req.getQueryString();

			if (!X.isEmpty(s)) {
				JSON j1 = JSON.fromObject(s);
				String o = j1.getString(name);
				if (o != null) {
					// 模糊大小写
					for (String key : j1.keySet()) {
						if (X.isSame(key, name)) {
							o = j1.getString(key);
							break;
						}
					}
				}
				if (o != null) {
					s = Url.decode(o);
					if (s != null) {
						return s;
					}
				}
			}
		}

		if (ss == null || ss.length == 0) {
			return null;
		}

		String s = ss[ss.length - 1];
		s = _decode(s);

		return s;

	}

	public final String get(String name, int maxlength) {
		String s = getString(name);
		if (!X.isEmpty(s)) {
			if (s.getBytes().length > maxlength) {
				s = Html.create(s).text(maxlength);
			}
		}

		return s;
	}

	public final String ip() {

		String s = Config.getConf().getString("ip.head");
		if (!X.isEmpty(s)) {
			String[] ss = X.split(s, "[,;]");
			for (String s1 : ss) {
				String remote = this.head(s1);
				if (!X.isEmpty(remote)) {
					return remote;
				}
			}
		}

		// compatible old
		String remote = this.head("X-Forwarded-For");
		if (remote == null) {
			remote = head("X-Real-IP");

			if (remote == null) {
				remote = req.getRemoteAddr();
			}
		}

		return remote;
	}

	public final String ipPath() {

		StringBuilder sb = new StringBuilder();

		String remote = head("X-Forwarded-For");
		if (!X.isEmpty(remote)) {
			sb.append(remote);
		}

		remote = this.head("X-Real-IP");
		if (!X.isEmpty(remote)) {
			if (sb.length() > 0)
				sb.append("->");
			sb.append(remote);
		}

		remote = req.getRemoteAddr();
		if (!X.isEmpty(remote)) {
			if (sb.length() > 0)
				sb.append("->");
			sb.append(remote);
		}

		return sb.toString();
	}

	public String getRequestURI() {
		return req.getRequestURI();
	}

	public final String head(String name) {
		try {
			return req.getHeader(name);
		} catch (Exception e) {
			return null;
		}
	}

	public final Cookie[] cookies() {
		return req.getCookies();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public final String[] getHtmls(String name) {

		try {
			if (this._multipart) {

				_parse_files();

				Object o = _get_file(name);
				if (o instanceof FileItem) {
					return new String[] { getString(name) };
				} else if (o instanceof List) {
					List<FileItem> list = (List<FileItem>) o;
					String[] ss = new String[list.size()];
					for (int i = 0; i < ss.length; i++) {
						FileItem ii = list.get(i);
						if (ii.isFormField()) {
							InputStream in = ii.getInputStream();
							byte[] bb = new byte[in.available()];
							in.read(bb);
							in.close();
							ss[i] = new String(bb, "UTF8").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
						}
					}
					return ss;
				}
			}

			String[] ss = req.getParameterValues(name);
			if (ss != null && ss.length > 0) {
				for (int i = 0; i < ss.length; i++) {
					if (ss[i] == null)
						continue;

					ss[i] = _decode(ss[i]);

					ss[i] = ss[i].replaceAll("<", "&lt").replaceAll(">", "&gt").trim();
				}
			}
			return ss;

		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(name, e);
		}
		return null;
	}

	private Object _get(String name) {
		Object o = _gets(name, _jsons);
		if (o != null) {
			if (X.isArray(o)) {
				try {
					if (o instanceof List) {
						return o;
					}
					String[] ss = (String[]) o;
					if (ss.length > 0) {
						return ss[0];
					}
				} catch (Exception e) {
					log.error(Arrays.asList(o), e);
				}
			}
		}
		return o;
	}

	Object _gets(String name, JSON json) {
		Object o = null;
		if (json != null) {
			o = json.get(name);
			if (o == null) {
				for (String key : json.keySet()) {
					if (X.isSame(name, key)) {
						o = json.get(key);
						break;
					}
				}
			}
		}
		return o;
	}

	private Object _get_file(String name) {
		Object o = _gets(name, _files);
		if (o != null) {
			if (o instanceof List) {
				return o;
			} else if (X.isArray(o)) {
				String[] ss = (String[]) o;
				if (ss.length > 0) {
					return ss[0];
				}
			}
		}
		return o;
	}

	public final List<String> names() {

		if (_names == null) {
			_names = new ArrayList<String>();
			String c1 = req.getContentType();
			if (c1 != null && c1.indexOf("application/json") > -1) {
				this.getString("1");// initialize uploads
				if (_jsons != null && !_jsons.isEmpty()) {
					_names.addAll(_jsons.keySet());
				}
			}
			if (this._multipart) {
				_parse_files();
				if (_files != null && !_files.isEmpty()) {
					_names.addAll(_files.keySet());
				}
			}

			Map<String, String[]> e = req.getParameterMap();
			if (e != null && !e.isEmpty()) {
				_names.addAll(e.keySet());
			} else {
				String s = req.getQueryString();
				if (!X.isEmpty(s)) {
					JSON j1 = JSON.fromObject(s);
					if (j1 != null && !j1.isEmpty()) {
						_names.addAll(j1.keySet());
					}
				}
			}

			if (rewrite != null) {
				for (String s : rewrite.keySet()) {
					if (_names.contains(s)) {
						_names.add(s);
					}
				}
			}
		}

		return _names;

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private JSON _parse_files() {

		if (_files == null) {

			_files = JSON.create();

			// Parse the request
			try {

				if (uploader == null) {
					DiskFileItemFactory factory = DiskFileItemFactory.builder().setPath("/data/temp/upload/").get();
					uploader = new JakartaServletFileUpload(factory);
				}

				List<FileItem> items = uploader.parseRequest(req);
				if (items != null && items.size() > 0) {
					for (FileItem f : items) {
						if (_files.containsKey(f.getFieldName())) {
							// 多个
							Object o = _files.get(f.getFieldName());
							if (o instanceof FileItem) {
								List<FileItem> list = new ArrayList<FileItem>();
								list.add((FileItem) o);
								list.add(f);
								_files.put(f.getFieldName(), list);
							} else if (o instanceof List) {
								((List<FileItem>) o).add(f);
							}
						} else {
							_files.put(f.getFieldName(), f);
						}
					}
				}
			} catch (FileUploadException e) {
				// ignore
			}
		}

		return _files;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public final FileItem file(String name) {

		_parse_files();

		Object o = _get_file(name);
		if (o instanceof FileItem) {
			return (FileItem) o;
		} else if (o instanceof List) {
			List<FileItem> list = (List<FileItem>) o;
			return list.get(list.size() - 1);
		}
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public final List<FileItem> files(String name) {

		_parse_files();

		Object o = _files.get(name);

		if (o instanceof FileItem) {
			return Arrays.asList((FileItem) o);
		} else if (o instanceof List) {
			List<FileItem> list = (List<FileItem>) o;
			return list;
		}
		return null;
	}

	public final NameValue[] heads() {
		Enumeration<String> e = req.getHeaderNames();
		if (e != null) {
			List<NameValue> list = new ArrayList<NameValue>();
			while (e.hasMoreElements()) {
				String n = e.nextElement();
				list.add(NameValue.create(n, req.getHeader(n)));
			}

			return list.toArray(new NameValue[list.size()]);
		}

		return null;
	}

	public RequestHelper copy(RequestHelper req) {

		RequestHelper r = new RequestHelper();
		r.req = req.req;
		r._files = req._parse_files();
		return r;

	}

	private List<String> _names = null;

	@SuppressWarnings("rawtypes")
	private static JakartaServletFileUpload uploader;

	private String _decode(String s) {
		try {
			String t = this.head("Content-Type");
			if (t == null) {
				// do nothing
				// log.debug("get s=" + s);

			} else if (t.indexOf("UTF-8") > -1) {
				// velocity bug fix

			} else if (t.indexOf("urlencoded") > -1) {
				// do nothing
				// content-type=application/x-www-form-urlencoded; charset=utf-8
				int i = t.indexOf("charset");
				String charset = null;
				if (i > 0) {
					i = t.indexOf("=", i + 1);
					if (i > 0) {
						charset = t.substring(i + 1).trim();
					}
				}
				if (X.isEmpty(charset)) {
					charset = "ISO-8859-1";
				}
				s = new String(s.getBytes(charset), ENCODING);

			} else if (t.indexOf("application/json") > -1) {
//				log.debug("get s=" + s);
				if (X.isIn(this.getMethod(), "POST")) {
					String charset = this.head("charset");
					if (X.isEmpty(charset)) {
						charset = "ISO-8859-1";
					}
					s = new String(s.getBytes(charset), ENCODING);
				}
			} else {
//				log.debug("get s=" + s);
			}
		} catch (Exception e) {
			log.error(s, e);
		}
		return s;
	}

	/**
	 * get the ServletContext
	 * 
	 * @return
	 */
	public final ServletContext context() {
		return GiiwaServlet.s️ervletContext;
	}

	private JSON _json;

	public final JSON json() {
		if (_json == null) {
			_json = JSON.create();
			for (String name : this.names()) {

				String s = this.getHtml(name);
				_json.put(name, s);
			}
//			_json.put("ip", this.ip());
//			_json.put("useragent", this.browser());

		}
		return _json;
	}

	public String getMethod() {
		return req.getMethod();
	}

	public RequestDispatcher getRequestDispatcher(String name) {
		return req.getRequestDispatcher(name);
	}

	private Map<String, String> rewrite = null;

	public void rewrite(String name, String value) {
		if (rewrite == null) {
			rewrite = new HashMap<String, String>();
		}
		rewrite.put(name, value);
	}

}
