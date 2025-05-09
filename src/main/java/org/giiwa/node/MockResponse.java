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

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.giiwa.dao.X;
import org.giiwa.json.JSON;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public class MockResponse implements HttpServletResponse, Closeable {

	public int status = 200;
	public String message;
	public JSON head = JSON.create();
	public ByteArrayOutputStream out = new ByteArrayOutputStream();

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(status).append(" ").append(message).append("\r\n");
		sb.append(head.toString()).append("\r\n");
		sb.append(new String(out.toByteArray())).append("\r\n");
		return sb.toString();
	}

	@Override
	public void flushBuffer() throws IOException {

	}

	@Override
	public int getBufferSize() {
		return 0;
	}

	@Override
	public String getCharacterEncoding() {
		return null;
	}

	@Override
	public String getContentType() {
		return null;
	}

	@Override
	public Locale getLocale() {
		return null;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return new ServletOutputStream() {

			@Override
			public void write(int b) throws IOException {
				out.write(b);
			}

			@Override
			public boolean isReady() {
				return true;
			}

			@Override
			public void setWriteListener(WriteListener arg0) {
				// TODO Auto-generated method stub

			}

		};
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		return new PrintWriter(out);
	}

	@Override
	public boolean isCommitted() {
		return false;
	}

	@Override
	public void reset() {

	}

	@Override
	public void resetBuffer() {

	}

	@Override
	public void setBufferSize(int arg0) {

	}

	@Override
	public void setCharacterEncoding(String arg0) {

	}

	@Override
	public void setContentLength(int arg0) {

	}

	@Override
	public void setContentType(String type) {
		head.append("Content-Type", type);
	}

	@Override
	public void setLocale(Locale arg0) {

	}

	@Override
	public void addCookie(Cookie arg0) {

	}

	@Override
	public void addDateHeader(String name, long v) {
		head.append(name, Long.toString(v));
	}

	@Override
	public void addHeader(String name, String v) {
		head.append(name, v);
	}

	@Override
	public void addIntHeader(String name, int v) {
		head.append(name, Integer.toString(v));
	}

	@Override
	public boolean containsHeader(String arg0) {
		return false;
	}

	@Override
	public String encodeRedirectURL(String arg0) {
		return null;
	}

	@Override
	public String encodeURL(String arg0) {
		return null;
	}

	@Override
	public void sendError(int arg0, String arg1) throws IOException {

	}

	@Override
	public void sendError(int arg0) throws IOException {

	}

	@Override
	public void sendRedirect(String url) throws IOException {
//		log.warn("url=" + url, new Exception("redirect 302"));
		head.append("location", url);
		status = 302;
	}

	@Override
	public void setDateHeader(String arg0, long arg1) {

	}

	@Override
	public void setHeader(String name, String v) {
		head.append(name, v);
	}

	@Override
	public void setIntHeader(String name, int v) {
		head.append(name, Integer.toString(v));
	}

//	@Override
//	public void setStatus(int status, String message) {
//		this.status = status;
//		this.message = message;
//
//	}

//	@Override
//	public void setStatus(int status) {
//		setStatus(status, X.EMPTY);
//	}

	public static MockResponse create() {
		return new MockResponse();
	}

	public void close() throws IOException {
		out.close();
	}

	@Override
	public void setContentLengthLong(long arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getHeader(String name) {
		return head.getString(name);
	}

	@Override
	public Collection<String> getHeaderNames() {
		return head.keySet();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Collection<String> getHeaders(String name) {
		Object o = head.get(name);
		if (X.isArray(o)) {
			return (Collection<String>) o;
		}
		return Arrays.asList(o.toString());
	}

	@Override
	public int getStatus() {
		return this.status;
	}

	@Override
	public void setStatus(int sc) {
		this.status = sc;
	}

}
