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
package org.giiwa.node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.web.RequestHelper;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;

public class MockRequest implements HttpServletRequest {

	private static Log log = LogFactory.getLog(MockRequest.class);

	String uri;
	public JSON head;
	public JSON body;

	@Override
	public Object getAttribute(String arg0) {
		return null;
	}

	@Override
	public String getCharacterEncoding() {
		return null;
	}

	@Override
	public int getContentLength() {
		return 0;
	}

	@Override
	public String getContentType() {
		return "application/json";
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public String getLocalAddr() {
		return null;
	}

	@Override
	public String getLocalName() {
		return null;
	}

	@Override
	public int getLocalPort() {
		return 0;
	}

	@Override
	public Locale getLocale() {
		return null;
	}

	@Override
	public String getParameter(String name) {
		if (!X.isEmpty(name)) {
			name = name.toLowerCase();
		}
		return body.getString(name);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		HashMap<String, String[]> mm = new HashMap<String, String[]>();
		for (String name : body.keySet()) {
			if (X.isEmpty(name)) {
				continue;
			}
			Object o = body.get(name);
			if (o != null) {
				if (X.isArray(o)) {
					List<String> l1 = X.asList(o, s -> s.toString());
					mm.put(name, l1.toArray(new String[l1.size()]));
				} else {
					mm.put(name, new String[] { o.toString() });
				}
			}
		}
		return mm;
	}

	@Override
	public Enumeration<String> getParameterNames() {
		Vector<String> l1 = new Vector<String>(body.keySet());
		return l1.elements();
	}

	@Override
	public String[] getParameterValues(String name) {

		if (!X.isEmpty(name)) {
			name = name.toLowerCase();
		}

		if (log.isDebugEnabled()) {
			log.debug("name=" + body.getString(name));
		}

		String s = body.getString(name);
		String[] ss = X.split(s, "\\$\\$\\$");
		return ss;

	}

	@Override
	public String getProtocol() {
		return null;
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return null;
	}

	@Override
	public String getRemoteAddr() {
		return null;
	}

	@Override
	public String getRemoteHost() {
		return null;
	}

	@Override
	public int getRemotePort() {
		return 0;
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String arg0) {
		return null;
	}

	@Override
	public String getScheme() {
		return null;
	}

	@Override
	public String getServerName() {
		return null;
	}

	@Override
	public int getServerPort() {
		return 0;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public void removeAttribute(String arg0) {

	}

	@Override
	public void setAttribute(String arg0, Object arg1) {

	}

	@Override
	public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {

	}

	@Override
	public String getAuthType() {
		return null;
	}

	@Override
	public String getContextPath() {
		return null;
	}

	@Override
	public Cookie[] getCookies() {
		String[] ss = X.split(head.getString("cookie"), "[; ]");
		if (ss != null && ss.length > 0) {
			Cookie[] cc = new Cookie[ss.length];
			for (int i = 0; i < ss.length; i++) {
				String[] s1 = X.split(ss[i], "[= ]");
				log.info("get cookie, [" + s1[0] + "]=" + s1[1]);
				cc[i] = new Cookie(s1[0], s1[1]);
			}
			return cc;
		}
		return null;
	}

	@Override
	public long getDateHeader(String arg0) {
		return 0;
	}

	@Override
	public String getHeader(String name) {
		if (!X.isEmpty(name)) {
			name = name.toLowerCase();
		}
		return head.getString(name);
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		Vector<String> l1 = new Vector<String>(head.keySet());
		return l1.elements();
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		if (!X.isEmpty(name)) {
			name = name.toLowerCase();
		}
		Object o = head.get(name);
		if (o != null) {
			Vector<String> l1 = new Vector<String>();
			if (X.isArray(o)) {
				List<String> l2 = X.asList(o, s -> s.toString());
				l1.addAll(l2);
			} else {
				l1.add(o.toString());
			}
			return l1.elements();
		}
		return null;
	}

	@Override
	public int getIntHeader(String arg0) {
		return 0;
	}

	@Override
	public String getMethod() {
		return null;
	}

	@Override
	public String getPathInfo() {
		return null;
	}

	@Override
	public String getPathTranslated() {
		return null;
	}

	@Override
	public String getQueryString() {
		return null;
	}

	@Override
	public String getRemoteUser() {
		return null;
	}

	@Override
	public String getRequestURI() {
		return uri;
	}

	@Override
	public StringBuffer getRequestURL() {
		return null;
	}

	@Override
	public String getRequestedSessionId() {
		return null;
	}

	@Override
	public String getServletPath() {
		return null;
	}

	@Override
	public HttpSession getSession() {
		return null;
	}

	@Override
	public HttpSession getSession(boolean arg0) {
		return null;
	}

	@Override
	public Principal getUserPrincipal() {
		return null;
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return false;
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		return false;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		return false;
	}

	@Override
	public boolean isUserInRole(String arg0) {
		return false;
	}

	public static RequestHelper create(String uri, JSON head, JSON body) {
		MockRequest r = new MockRequest();
		r.uri = uri;
		r.head = JSON.create();
		for (String name : head.keySet()) {
			if (!X.isEmpty(name)) {
				r.head.put(name.toLowerCase(), head.get(name));
			}
		}
		r.body = JSON.create();
		for (String name : body.keySet()) {
			if (!X.isEmpty(name)) {
				r.body.put(name.toLowerCase(), body.get(name));
			}
		}
		return RequestHelper.create(r);
	}

	@Override
	public AsyncContext getAsyncContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getContentLengthLong() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public DispatcherType getDispatcherType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServletContext getServletContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAsyncStarted() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isAsyncSupported() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AsyncContext startAsync(ServletRequest arg0, ServletResponse arg1) throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean authenticate(HttpServletResponse arg0) throws IOException, ServletException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String changeSessionId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Part getPart(String arg0) throws IOException, ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void login(String arg0, String arg1) throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public void logout() throws ServletException {
		// TODO Auto-generated method stub

	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> arg0) throws IOException, ServletException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Enumeration<Locale> getLocales() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRequestId() {
		return null;
	}

	@Override
	public String getProtocolRequestId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ServletConnection getServletConnection() {
		// TODO Auto-generated method stub
		return null;
	}

}
