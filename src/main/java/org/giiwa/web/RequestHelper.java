package org.giiwa.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.misc.Html;
import org.giiwa.web.Controller.NameValue;

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
		return ServletFileUpload.isMultipartContent(req);
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
	private Map<String, Object> uploads = null;

	public final String getHtml(String name) {

		try {
			String c1 = req.getContentType();

//			log.debug("get s, name=" + name + ", c1=" + c1);

			if (c1 != null && c1.indexOf("application/json") > -1) {

				if (uploads == null) {

					BufferedReader in = req.getReader();

					StringBuilder sb = new StringBuilder();
					char[] buff = new char[1024];
					int len;
					while ((len = in.read(buff)) != -1) {
						sb.append(buff, 0, len);
					}

					JSON jo = JSON.fromObject(sb.toString());
					if (jo != null) {
						uploads = new HashMap<String, Object>();
						uploads.putAll(jo);
					}
				}

				if (uploads != null) {
					Object v1 = uploads.get(name);

//					log.debug("get s=" + v1);

					if (v1 != null) {
						return v1.toString().trim();
					}
				}
			}

			if (this._multipart) {

//				_get_files();

				FileItem i = this.file(name);
//				log.debug("i=" + i);

				if (i != null && i.isFormField()) {
					InputStream in = i.getInputStream();
					byte[] bb = new byte[in.available()];
					in.read(bb);
					in.close();
					return new String(bb, ENCODING);
				}
				return null;

			}

			String[] ss = req.getParameterValues(name);
			if (ss == null || ss.length == 0) {
				return null;
			}

			String s = ss[ss.length - 1];
			s = _decode(s);

//			log.debug("get s = " + s);
			return s;

		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error("get request parameter " + name + " get exception.", e);
		}

		return null;

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
		String remote = this.head("X-Real-IP");
		if (remote == null) {
			remote = head("X-Forwarded-For");

			if (remote == null) {
				remote = req.getRemoteAddr();
			}
		}

		return remote;
	}

	public final String ipPath() {

		StringBuilder sb = new StringBuilder();

		String remote = head("X-Real-IP");
		if (!X.isEmpty(remote)) {
			sb.append(remote);
		}

		remote = this.head("X-Forwarded-For");
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

	@SuppressWarnings("unchecked")
	public final String[] getHtmls(String name) {
		try {
			if (this._multipart) {
				_get_files();

				Object o = uploads.get(name);
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
			} else {
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
			}
		} catch (Exception e) {
			if (log.isErrorEnabled())
				log.error(name, e);
		}
		return null;
	}

	public final List<String> names() {

		if (_names == null) {
			String c1 = req.getContentType();
			if (c1 != null && c1.indexOf("application/json") > -1) {
				this.getString(null);// initialize uploads
				if (uploads != null) {
					_names = new ArrayList<String>(uploads.keySet());
				}
			} else if (this._multipart) {
				_get_files();
				_names = new ArrayList<String>(uploads.keySet());
			}

			Enumeration<?> e = req.getParameterNames();
			if (e == null) {
				_names = Arrays.asList();
			}

			if (_names == null) {
				_names = new ArrayList<String>();

				while (e.hasMoreElements()) {
					_names.add(e.nextElement().toString());
				}
			}

		}

		return _names;

	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> _get_files() {

		if (uploads == null) {

			uploads = new HashMap<String, Object>();

			if (uploader == null) {
				DiskFileItemFactory factory = new DiskFileItemFactory();

				// Configure a repository (to ensure a secure temp location is used)
				File repository = (File) context().getAttribute("javax.servlet.context.tempdir");
				factory.setRepository(repository);

				// Create a new file upload handler
				uploader = new ServletFileUpload(factory);
			}

			// Parse the request
			try {
				List<FileItem> items = uploader.parseRequest(req);
				if (items != null && items.size() > 0) {
					for (FileItem f : items) {
						if (uploads.containsKey(f.getFieldName())) {
							Object o = uploads.get(f.getFieldName());
							if (o instanceof FileItem) {
								List<FileItem> list = new ArrayList<FileItem>();
								list.add((FileItem) o);
								list.add(f);
								uploads.put(f.getFieldName(), list);
							} else if (o instanceof List) {
								((List<FileItem>) o).add(f);
							}
						} else {
							uploads.put(f.getFieldName(), f);
						}
					}
				} else {
					if (log.isWarnEnabled())
						log.warn("nothing got!!!");
				}
			} catch (FileUploadException e) {
				// ignore
//				if (log.isErrorEnabled())
//					log.error(e.getMessage(), e);
			}
		}

		return uploads;
	}

	@SuppressWarnings("unchecked")
	public final FileItem file(String name) {

		_get_files();

		Object o = uploads.get(name);
		if (o instanceof FileItem) {
			return (FileItem) o;
		} else if (o instanceof List) {
			List<FileItem> list = (List<FileItem>) o;
			return list.get(list.size() - 1);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public final List<FileItem> files(String name) {

		_get_files();

		Object o = uploads.get(name);

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
		r.uploads = req._get_files();
		return r;

	}

	private List<String> _names = null;

	private static ServletFileUpload uploader;

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
				// content-type=application/x-www-form-urlencoded
				String charset = this.head("charset");
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

}
