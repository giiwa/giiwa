package org.giiwa.core.dfile;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.giiwa.core.bean.X;
import org.giiwa.core.json.JSON;

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
	public String encodeRedirectUrl(String arg0) {
		return null;
	}

	@Override
	public String encodeURL(String arg0) {
		return null;
	}

	@Override
	public String encodeUrl(String arg0) {
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

	@Override
	public void setStatus(int status, String message) {
		this.status = status;
		this.message = message;

	}

	@Override
	public void setStatus(int status) {
		setStatus(status, X.EMPTY);
	}

	public static MockResponse create() {
		return new MockResponse();
	}

	public void close() throws IOException {
		out.close();
	}
}
