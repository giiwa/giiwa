package org.giiwa.app.web;

import java.io.File;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.catalina.util.XMLWriter;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.web.Controller;
import org.giiwa.framework.web.Path;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public class helo extends Controller {

	@Path(path = "test")
	public void test() {

		String name = this.getString("name");
		this.set("name", name);

		this.show("/helo.html");

	}

	@Path(path = "files", method = "*")
	public void files() {

		log.debug("method=" + method.name);

		if (method.is("OPTIONS")) {

			resp.addHeader("DAV", "1,2");
			resp.addHeader("Allow", determineMethodsAllowed(req));
			resp.addHeader("MS-Author-Via", "DAV");

		} else if (method.is("PROPFIND")) {

			propfind();

		}

	}

	private final Hashtable<String, Vector<String>> lockNullResources = new Hashtable<>();

	private void propfind() {

		try {
			String path = "/Users/joe/d/";

			// Properties which are to be displayed.
			Vector<String> properties = null;

			// Propfind depth
			int depth = 10;// maxDepth;
			// Propfind type
			int type = 1;// FIND_ALL_PROP;

			String depthStr = req.getHeader("Depth");

			if (depthStr == null) {
				depth = 10;// maxDepth;
			} else {
				if (depthStr.equals("0")) {
					depth = 0;
				} else if (depthStr.equals("1")) {
					depth = 1;
				} else if (depthStr.equals("infinity")) {
					depth = 10;// maxDepth;
				}
			}

			Node propNode = null;

			log.debug("req.length=" + req.getContentLengthLong());

			if (req.getContentLengthLong() > 0) {

				byte[] bb = new byte[req.getContentLength()];
				req.getInputStream().read(bb);
				String ss = new String(bb);

				log.debug("bb=" + ss);

				DocumentBuilder documentBuilder = getDocumentBuilder();

				try {

					JSON jo = JSON.fromXml(ss);
//					Document doc = documentBuilder.parse(ss);
//
					log.debug(jo.toPrettyString());

					Object o = jo.get("propfind");
					if (!o.getClass().isArray()) {
						o = Arrays.asList(o);
					}

					List<JSON> l1 = (List<JSON>) o;
					for (JSON j1 : l1) {
//						String type = 
					}

					// Get the root element of the document
//					Element rootElement = doc.getDocumentElement();
//					NodeList childList = rootElement.getChildNodes();

//					for (String name: jo.keySet()) {
//						Node currentNode = childList.item(i);
//						switch (currentNode.getNodeType()) {
//						case Node.TEXT_NODE:
//							break;
//						case Node.ELEMENT_NODE:
//							if (currentNode.getNodeName().endsWith("prop")) {
//								type = FIND_BY_PROPERTY;
//								propNode = currentNode;
//							}
//							if (currentNode.getNodeName().endsWith("propname")) {
//								type = FIND_PROPERTY_NAMES;
//							}
//							if (currentNode.getNodeName().endsWith("allprop")) {
//								type = FIND_ALL_PROP;
//							}
//							break;
//						}
//					}
				} catch (Exception e) {
					// Something went wrong - bad request
					resp.sendError(WebdavStatus.SC_BAD_REQUEST);
					log.error(e.getMessage(), e);
					return;
				}
			}

			log.debug("type=" + type);

			if (type == FIND_BY_PROPERTY) {
				properties = new Vector<>();
				// propNode must be non-null if type == FIND_BY_PROPERTY
				@SuppressWarnings("null")
				NodeList childList = propNode.getChildNodes();

				for (int i = 0; i < childList.getLength(); i++) {
					Node currentNode = childList.item(i);
					switch (currentNode.getNodeType()) {
					case Node.TEXT_NODE:
						break;
					case Node.ELEMENT_NODE:
						String nodeName = currentNode.getNodeName();
						String propertyName = null;
						if (nodeName.indexOf(':') != -1) {
							propertyName = nodeName.substring(nodeName.indexOf(':') + 1);
						} else {
							propertyName = nodeName;
						}
						// href is a live property which is handled differently
						properties.addElement(propertyName);
						break;
					}
				}

			}

			File res = new File(path);

			log.debug("res=" + res);

			if (res.exists()) {
				int slash = path.lastIndexOf('/');
				if (slash != -1) {
					String parentPath = path.substring(0, slash);
					Vector<String> currentLockNullResources = lockNullResources.get(parentPath);
					if (currentLockNullResources != null) {
						Enumeration<String> lockNullResourcesList = currentLockNullResources.elements();
						while (lockNullResourcesList.hasMoreElements()) {
							String lockNullPath = lockNullResourcesList.nextElement();
							if (lockNullPath.equals(path)) {
								resp.setStatus(WebdavStatus.SC_MULTI_STATUS);
								resp.setContentType("text/xml; charset=UTF-8");
								// Create multistatus object
								XMLWriter generatedXML = new XMLWriter(resp.getWriter());
								generatedXML.writeXMLHeader();
								generatedXML.writeElement("D", DEFAULT_NAMESPACE, "multistatus", XMLWriter.OPENING);
								parseLockNullProperties(req, generatedXML, lockNullPath, type, properties);
								generatedXML.writeElement("D", "multistatus", XMLWriter.CLOSING);
								generatedXML.sendData();

								log.debug(generatedXML.toString());

								return;
							}
						}
					}
				}
			}

			if (!res.exists()) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND, path);
				log.debug("resource not exists");

				return;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		log.debug("over");

	}

	protected DocumentBuilder getDocumentBuilder() throws ServletException {
		DocumentBuilder documentBuilder = null;
		DocumentBuilderFactory documentBuilderFactory = null;
		try {
			documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setNamespaceAware(true);
			documentBuilderFactory.setExpandEntityReferences(false);
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
			documentBuilder.setEntityResolver(new WebdavResolver(Controller.s️ervletContext));
		} catch (ParserConfigurationException e) {
			throw new ServletException(e);
		}
		return documentBuilder;
	}

	private void parseLockNullProperties(HttpServletRequest req, XMLWriter generatedXML, String path, int type,
			Vector<String> propertiesVector) {

// Exclude any resource in the /WEB-INF and /META-INF subdirectories
//		if (isSpecialPath(path))
//			return;

//// Retrieving the lock associated with the lock-null resource
//		LockInfo lock = resourceLocks.get(path);
//
//		if (lock == null)
//			return;
//
//		String absoluteUri = req.getRequestURI();
//		String relativePath = getRelativePath(req);
//		String toAppend = path.substring(relativePath.length());
//		if (!toAppend.startsWith("/"))
//			toAppend = "/" + toAppend;
//
//		String rewrittenUrl = rewriteUrl(RequestUtil.normalize(absoluteUri + toAppend));
//
//		generatePropFindResponse(generatedXML, rewrittenUrl, path, type, propertiesVector, true, true,
//				lock.creationDate.getTime(), lock.creationDate.getTime(), 0, "", "");
	}

	protected String determineMethodsAllowed(HttpServletRequest req) {

		// These methods are always allowed. They may return a 404 (not a 405)
		// if the resource does not exist.
		StringBuilder methodsAllowed = new StringBuilder("OPTIONS, GET, POST, HEAD");

		methodsAllowed.append(", DELETE"); // RW
		methodsAllowed.append(", PUT"); // dir

		// Trace - assume disabled unless we can prove otherwise

		// methodsAllowed.append(", LOCK, UNLOCK, PROPPATCH, COPY, MOVE");

		// methodsAllowed.append(", PROPFIND");

		// methodsAllowed.append(", MKCOL"); // not exists

		return methodsAllowed.toString();
	}

	private class WebdavResolver implements EntityResolver {
		private ServletContext context;

		public WebdavResolver(ServletContext theContext) {
			context = theContext;
		}

		@Override
		public InputSource resolveEntity(String publicId, String systemId) {
//            context.log(lang.getString("webdavservlet.enternalEntityIgnored",
//                    publicId, systemId));
			return new InputSource(new StringReader("Ignored external entity"));
		}
	}

	private static final String METHOD_PROPFIND = "PROPFIND";
	private static final String METHOD_PROPPATCH = "PROPPATCH";
	private static final String METHOD_MKCOL = "MKCOL";
	private static final String METHOD_COPY = "COPY";
	private static final String METHOD_MOVE = "MOVE";
	private static final String METHOD_LOCK = "LOCK";
	private static final String METHOD_UNLOCK = "UNLOCK";

	/**
	 * PROPFIND - Specify a property mask.
	 */
	private static final int FIND_BY_PROPERTY = 0;

	/**
	 * PROPFIND - Display all properties.
	 */
	private static final int FIND_ALL_PROP = 1;

	/**
	 * PROPFIND - Return property names.
	 */
	private static final int FIND_PROPERTY_NAMES = 2;

	/**
	 * Create a new lock.
	 */
	private static final int LOCK_CREATION = 0;

	/**
	 * Refresh lock.
	 */
	private static final int LOCK_REFRESH = 1;

	/**
	 * Default lock timeout value.
	 */
	private static final int DEFAULT_TIMEOUT = 3600;

	/**
	 * Maximum lock timeout.
	 */
	private static final int MAX_TIMEOUT = 604800;

	/**
	 * Default namespace.
	 */
	protected static final String DEFAULT_NAMESPACE = "DAV:";

}

class WebdavStatus {

	// ----------------------------------------------------- Instance Variables

	/**
	 * This Hashtable contains the mapping of HTTP and WebDAV status codes to
	 * descriptive text. This is a static variable.
	 */
	private static final Hashtable<Integer, String> mapStatusCodes = new Hashtable<>();

	// ------------------------------------------------------ HTTP Status Codes

	/**
	 * Status code (200) indicating the request succeeded normally.
	 */
	public static final int SC_OK = HttpServletResponse.SC_OK;

	/**
	 * Status code (201) indicating the request succeeded and created a new resource
	 * on the server.
	 */
	public static final int SC_CREATED = HttpServletResponse.SC_CREATED;

	/**
	 * Status code (202) indicating that a request was accepted for processing, but
	 * was not completed.
	 */
	public static final int SC_ACCEPTED = HttpServletResponse.SC_ACCEPTED;

	/**
	 * Status code (204) indicating that the request succeeded but that there was no
	 * new information to return.
	 */
	public static final int SC_NO_CONTENT = HttpServletResponse.SC_NO_CONTENT;

	/**
	 * Status code (301) indicating that the resource has permanently moved to a new
	 * location, and that future references should use a new URI with their
	 * requests.
	 */
	public static final int SC_MOVED_PERMANENTLY = HttpServletResponse.SC_MOVED_PERMANENTLY;

	/**
	 * Status code (302) indicating that the resource has temporarily moved to
	 * another location, but that future references should still use the original
	 * URI to access the resource.
	 */
	public static final int SC_MOVED_TEMPORARILY = HttpServletResponse.SC_MOVED_TEMPORARILY;

	/**
	 * Status code (304) indicating that a conditional GET operation found that the
	 * resource was available and not modified.
	 */
	public static final int SC_NOT_MODIFIED = HttpServletResponse.SC_NOT_MODIFIED;

	/**
	 * Status code (400) indicating the request sent by the client was syntactically
	 * incorrect.
	 */
	public static final int SC_BAD_REQUEST = HttpServletResponse.SC_BAD_REQUEST;

	/**
	 * Status code (401) indicating that the request requires HTTP authentication.
	 */
	public static final int SC_UNAUTHORIZED = HttpServletResponse.SC_UNAUTHORIZED;

	/**
	 * Status code (403) indicating the server understood the request but refused to
	 * fulfill it.
	 */
	public static final int SC_FORBIDDEN = HttpServletResponse.SC_FORBIDDEN;

	/**
	 * Status code (404) indicating that the requested resource is not available.
	 */
	public static final int SC_NOT_FOUND = HttpServletResponse.SC_NOT_FOUND;

	/**
	 * Status code (500) indicating an error inside the HTTP service which prevented
	 * it from fulfilling the request.
	 */
	public static final int SC_INTERNAL_SERVER_ERROR = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

	/**
	 * Status code (501) indicating the HTTP service does not support the
	 * functionality needed to fulfill the request.
	 */
	public static final int SC_NOT_IMPLEMENTED = HttpServletResponse.SC_NOT_IMPLEMENTED;

	/**
	 * Status code (502) indicating that the HTTP server received an invalid
	 * response from a server it consulted when acting as a proxy or gateway.
	 */
	public static final int SC_BAD_GATEWAY = HttpServletResponse.SC_BAD_GATEWAY;

	/**
	 * Status code (503) indicating that the HTTP service is temporarily overloaded,
	 * and unable to handle the request.
	 */
	public static final int SC_SERVICE_UNAVAILABLE = HttpServletResponse.SC_SERVICE_UNAVAILABLE;

	/**
	 * Status code (100) indicating the client may continue with its request. This
	 * interim response is used to inform the client that the initial part of the
	 * request has been received and has not yet been rejected by the server.
	 */
	public static final int SC_CONTINUE = 100;

	/**
	 * Status code (405) indicating the method specified is not allowed for the
	 * resource.
	 */
	public static final int SC_METHOD_NOT_ALLOWED = 405;

	/**
	 * Status code (409) indicating that the request could not be completed due to a
	 * conflict with the current state of the resource.
	 */
	public static final int SC_CONFLICT = 409;

	/**
	 * Status code (412) indicating the precondition given in one or more of the
	 * request-header fields evaluated to false when it was tested on the server.
	 */
	public static final int SC_PRECONDITION_FAILED = 412;

	/**
	 * Status code (413) indicating the server is refusing to process a request
	 * because the request entity is larger than the server is willing or able to
	 * process.
	 */
	public static final int SC_REQUEST_TOO_LONG = 413;

	/**
	 * Status code (415) indicating the server is refusing to service the request
	 * because the entity of the request is in a format not supported by the
	 * requested resource for the requested method.
	 */
	public static final int SC_UNSUPPORTED_MEDIA_TYPE = 415;

	// -------------------------------------------- Extended WebDav status code

	/**
	 * Status code (207) indicating that the response requires providing status for
	 * multiple independent operations.
	 */
	public static final int SC_MULTI_STATUS = 207;
	// This one collides with HTTP 1.1
	// "207 Partial Update OK"

	/**
	 * Status code (418) indicating the entity body submitted with the PATCH method
	 * was not understood by the resource.
	 */
	public static final int SC_UNPROCESSABLE_ENTITY = 418;
	// This one collides with HTTP 1.1
	// "418 Reauthentication Required"

	/**
	 * Status code (419) indicating that the resource does not have sufficient space
	 * to record the state of the resource after the execution of this method.
	 */
	public static final int SC_INSUFFICIENT_SPACE_ON_RESOURCE = 419;
	// This one collides with HTTP 1.1
	// "419 Proxy Reauthentication Required"

	/**
	 * Status code (420) indicating the method was not executed on a particular
	 * resource within its scope because some part of the method's execution failed
	 * causing the entire method to be aborted.
	 */
	public static final int SC_METHOD_FAILURE = 420;

	/**
	 * Status code (423) indicating the destination resource of a method is locked,
	 * and either the request did not contain a valid Lock-Info header, or the
	 * Lock-Info header identifies a lock held by another principal.
	 */
	public static final int SC_LOCKED = 423;

	// ------------------------------------------------------------ Initializer

	static {
		// HTTP 1.0 status Code
		addStatusCodeMap(SC_OK, "OK");
		addStatusCodeMap(SC_CREATED, "Created");
		addStatusCodeMap(SC_ACCEPTED, "Accepted");
		addStatusCodeMap(SC_NO_CONTENT, "No Content");
		addStatusCodeMap(SC_MOVED_PERMANENTLY, "Moved Permanently");
		addStatusCodeMap(SC_MOVED_TEMPORARILY, "Moved Temporarily");
		addStatusCodeMap(SC_NOT_MODIFIED, "Not Modified");
		addStatusCodeMap(SC_BAD_REQUEST, "Bad Request");
		addStatusCodeMap(SC_UNAUTHORIZED, "Unauthorized");
		addStatusCodeMap(SC_FORBIDDEN, "Forbidden");
		addStatusCodeMap(SC_NOT_FOUND, "Not Found");
		addStatusCodeMap(SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
		addStatusCodeMap(SC_NOT_IMPLEMENTED, "Not Implemented");
		addStatusCodeMap(SC_BAD_GATEWAY, "Bad Gateway");
		addStatusCodeMap(SC_SERVICE_UNAVAILABLE, "Service Unavailable");
		addStatusCodeMap(SC_CONTINUE, "Continue");
		addStatusCodeMap(SC_METHOD_NOT_ALLOWED, "Method Not Allowed");
		addStatusCodeMap(SC_CONFLICT, "Conflict");
		addStatusCodeMap(SC_PRECONDITION_FAILED, "Precondition Failed");
		addStatusCodeMap(SC_REQUEST_TOO_LONG, "Request Too Long");
		addStatusCodeMap(SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type");
		// WebDav Status Codes
		addStatusCodeMap(SC_MULTI_STATUS, "Multi-Status");
		addStatusCodeMap(SC_UNPROCESSABLE_ENTITY, "Unprocessable Entity");
		addStatusCodeMap(SC_INSUFFICIENT_SPACE_ON_RESOURCE, "Insufficient Space On Resource");
		addStatusCodeMap(SC_METHOD_FAILURE, "Method Failure");
		addStatusCodeMap(SC_LOCKED, "Locked");
	}

	// --------------------------------------------------------- Public Methods

	/**
	 * Returns the HTTP status text for the HTTP or WebDav status code specified by
	 * looking it up in the static mapping. This is a static function.
	 *
	 * @param nHttpStatusCode [IN] HTTP or WebDAV status code
	 * @return A string with a short descriptive phrase for the HTTP status code
	 *         (e.g., "OK").
	 */
	public static String getStatusText(int nHttpStatusCode) {
		Integer intKey = Integer.valueOf(nHttpStatusCode);

		if (!mapStatusCodes.containsKey(intKey)) {
			return "";
		} else {
			return mapStatusCodes.get(intKey);
		}
	}

	// -------------------------------------------------------- Private Methods

	/**
	 * Adds a new status code -> status text mapping. This is a static method
	 * because the mapping is a static variable.
	 *
	 * @param nKey   [IN] HTTP or WebDAV status code
	 * @param strVal [IN] HTTP status text
	 */
	private static void addStatusCodeMap(int nKey, String strVal) {
		mapStatusCodes.put(Integer.valueOf(nKey), strVal);
	}

}
