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
package org.giiwa.misc;

import java.util.*;
import java.util.function.Consumer;

import javax.swing.text.html.parser.ParserDelegator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.web.QueryString;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

/**
 * The {@code Html} Class used to string to html, html to plain, or get images
 * from the html string.
 * 
 * @author joe
 *
 */
public final class Html {

	/** The delegator. */
	transient ParserDelegator delegator = new ParserDelegator();

	/** The log. */
	private static Log log = LogFactory.getLog(Html.class);

	/** The d. */
	transient Document d = null;

	public String url;

	private Html(String html) {
		if (html != null) {
			d = Jsoup.parse(html);
		}
	}

	/**
	 * create a Html object by html string.
	 *
	 * @param html the html string
	 * @return Html object
	 */
	public static Html create(String html) {
		return create(html, null);
	}

	public static Html create(String html, String charset) {
		Html h = new Html(html);
		if (!X.isEmpty(charset)) {
			List<Element> l1 = h.find("meta");
			if (l1 != null && l1.size() > 0) {
				String c1 = charset;
				for (Element e : l1) {
					String c2 = e.attr("charset");
					if (!X.isEmpty(c2)) {
						c1 = c2;
						break;
					}
				}
				if (!X.isSame(c1, charset)) {
					try {
						h = new Html(new String(html.getBytes(charset), c1));
					} catch (Exception e1) {
						log.error(e1.getMessage(), e1);
					}
				}
			}
		}
		return h;
	}

	/**
	 * Removes the tag.
	 * 
	 * @param html the html
	 * @param tag  the tag
	 * @return the string
	 */
	public static String removeTag(String html, String tag) {
		if (html == null)
			return null;

		StringBuilder sb = new StringBuilder();
		int p = 0;
		int len = html.length();
		while (p < len) {
			int i = html.indexOf("<" + tag, p);
			if (i > -1) {
				sb.append(html.substring(p, i));
				i = html.indexOf("</" + tag + ">", i);
				if (i > -1) {
					p = i + tag.length() + 3;
				} else {
					break;
				}
			} else {
				sb.append(html.substring(p));
				break;
			}
		}

		return sb.toString();
	}

	/**
	 * get the title of the HTML.
	 *
	 * @return the string
	 */
	public String title() {
		if (d != null) {
			return d.title();
		}

		return null;
	}

	transient String body;

	/**
	 * Body.
	 * 
	 * @return the string
	 */
	public String body() {
		if (body == null && d != null) {
			d.select("iframe").remove();
			body = d.html();
		}

		return body;
	}

	/**
	 * Body.
	 *
	 * @param len the len
	 * @return the string
	 */
	public String body(int len) {
		body();

		if (body != null && body.length() > len) {
			body = body.substring(0, len - 3) + "...";
		}

		return body;
	}

	transient String text;

	/**
	 * Text.
	 * 
	 * @return the string
	 */
	public String text() {
		if (text == null && d != null) {
			String s = d.text();
			text = s.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
		}

		return text;
	}

	/**
	 * Text.
	 *
	 * @param len the len
	 * @return the string
	 */
	public String text(int len) {
		text();

		if (text != null && text.getBytes().length > len) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < text.length() && sb.toString().getBytes().length < len - 5; i++) {
				sb.append(text.charAt(i));
			}
			text = sb.append("...").toString();
		}

		return text;
	}

	/**
	 * please refers getTags().
	 *
	 * @param tag the html tag
	 * @return List of element
	 * @deprecated
	 */
	public List<Element> get(String tag) {
		return getTags(tag);
	}

	/**
	 * get the tags.
	 *
	 * @param tag the name of the tag
	 * @return List
	 */
	public List<Element> getTags(String tag) {
		if (d != null) {
			Elements es = d.getElementsByTag(tag);
			List<Element> list = new ArrayList<Element>();
			for (int i = 0; i < es.size(); i++) {
				Element e = es.get(i);
				list.add(e);
			}
			return list;

		}
		return null;
	}

	/**
	 * find the element by the selector <br>
	 * 
	 * @deprecated
	 *
	 * @param selector 1, .id for id element<br>
	 *                 e.g: find(".aaa") <br>
	 *                 2, .class for class attribute <br>
	 *                 e.g: find(".aaa") <br>
	 *                 3, tagname for tag directly <br>
	 *                 e.g: find("div") <br>
	 * @return the list of Elements
	 */
	public List<Element> find(String selector) {
		return select(selector);
	}

	public Elements select(String selector) {
		if (d == null)
			return null;

		return d.select(selector);
	}

	/**
	 * find the elements in the node.
	 * 
	 * @deprecated
	 * @param e        the element node
	 * @param selector the selector string
	 * @return the list of element or null if nothing found
	 */
	public static List<Element> find(Element e, String selector) {
		return e.select(selector);
	}

	/**
	 * find the elements in the elements by the selector.
	 *
	 * @deprecated
	 * @param list     the original elements
	 * @param selector the string of selector, .id, .class, tag
	 * @return the list of element or null nothing found
	 */
	public static List<Element> find(List<Element> list, String selector) {
		if (list == null || list.size() == 0) {
			return null;
		}

		List<Element> l1 = null;
		for (Element e : list) {
			List<Element> l2 = find(e, selector);
			if (l2 != null && l2.size() > 0) {
				if (l1 == null) {
					l1 = l2;
				} else {
					l1.addAll(l2);
				}
			}
		}

		return l1;
	}

	public StringFinder finder() {
		return StringFinder.create(body);
	}

	public JSON json() {
		return JSON.fromObject(body);
	}

	/**
	 * Removes the.
	 * 
	 * @param q the q
	 * @return the html
	 */
	public Html remove(String q) {
		if (d != null) {
			d.select(q).remove();
		}
		return this;
	}

	private String _path(String url) {
		int i = url.lastIndexOf("/");
		if (i > 8) {
			return url.substring(0, i + 1);
		}
		return url + "/";
	}

	private String _path2(String url) {
		int i = url.indexOf("?");
		if (i > 8) {
			return url.substring(0, i);
		}
		return url;
	}

	/**
	 * @deprecated
	 * @param regex
	 * @return
	 * @throws Exception
	 */
	public List<JSON> href(String... regex) throws Exception {
		return a(regex);
	}

	/**
	 * 
	 * find all a tag
	 * 
	 * @return
	 * @throws Exception
	 */
	public List<JSON> a(Object regex) throws Exception {
		return link("a", "href", regex, (Consumer<Url>) null);
	}

	public List<JSON> a(Object regex, Consumer<Url> func) throws Exception {
		return link("a", "href", regex, func);
	}

//	public List<JSON> a(Object regex, JSObject func) throws Exception {
//		return link("a", "href", regex, func);
//	}

	public List<JSON> link(String selector, String attr, Object regex) throws Exception {
		return link(selector, attr, regex, null);
	}

	public List<JSON> link(String selector, String attr, Object regex, Consumer<Url> func) throws Exception {
		if (X.isEmpty(url))
			throw new Exception("url is not setting");

		List<String> r1 = X.asList(regex, s -> s.toString());

		Set<String> hh = new HashSet<String>();
		if (X.isEmpty(r1) || X.isEmpty(r1.get(0))) {
			hh.add(_server(url) + ".*");
		} else {

			for (String s : r1) {
				String[] ss = X.split(s, "[;]");
				for (String s1 : ss) {
					hh.add(s1);
				}
			}
		}

//		Map<String, JSON> l2 = new HashMap<String, JSON>();
		List<JSON> l2 = JSON.createList();
		List<Element> l1 = select(selector);

		if (l1 != null && l1.size() > 0) {
			for (Element e1 : l1) {

				String href = e1.attr(attr);
				href = href.trim();

				if (href.startsWith("#") || href.toLowerCase().startsWith("javascript:")
						|| href.toLowerCase().startsWith("tel:") || href.toLowerCase().startsWith("mail:")) {
					continue;
				} else if (href.startsWith("//")) {
					href = _protocol(url) + href;
				} else if (href.startsWith("/")) {
					href = _server(url) + href;
				} else if (href.startsWith("?")) {
					href = _path2(url) + href;
				} else if (!href.startsWith("http")) {
					href = _path(url) + href;
				}
				int i = href.indexOf("#");
				if (i > 0) {
					href = href.substring(0, i);
				}

				href = format(href);

				if (func != null) {
					Url u1 = Url.create(href);
					func.accept(u1);
					href = u1.encodedUrl();
				}

				if (_match(href, hh)) {
					JSON a = JSON.create();
					List<Attribute> l3 = e1.attributes().asList();
					l3.forEach(e -> {
						a.put(e.getKey(), e.getValue());
					});
					a.append("href", href).append("url", href).append("text", e1.text());
					l2.add(a);
				}
			}
		}

		return l2;
	}

	/**
	 * 格式化链接
	 * 
	 * @param href
	 * @return
	 */
	public String format(String href) {
		if (href.startsWith("//")) {
			href = _protocol(url) + href;
		} else if (href.startsWith("/")) {
			href = _server(url) + href;
		} else if (href.startsWith("?")) {
			href = _path2(url) + href;
		} else if (!href.startsWith("http")) {
			href = _path(url) + href;
		}
		int i = href.indexOf("#");
		if (i > 0) {
			href = href.substring(0, i);
		}

		if (X.isEmpty(href))
			return null;

		QueryString qs = new QueryString(href);
//		qs.remove(removals);

		return qs.toString();

	}

	/**
	 * find links
	 * 
	 * @param selector the html tag selector
	 * @param attr     the tag's attribute
	 * @param regex    the link's regex
	 * @param func     callback function
	 * @return
	 * @throws Exception
	 */
//	@SuppressWarnings({ "unchecked", "restriction", "rawtypes" })
//	public List<JSON> link(String selector, String attr, Object regex, JSObject func) throws Exception {
//		return link(selector, attr, regex, u -> {
//			func.call(this, u);
//		});
//	}

	private boolean _match(String href, Set<String> domains) {

		if (domains == null || domains.isEmpty())
			return true;

		for (String s : domains) {
//			log.debug("href=" + href + ", s=" + s);
//
//			System.out.println("href=" + href + ", s=" + s + ", matches?=" + href.matches(s));

			if (href.matches(s)) {
				return true;
			}
		}
		return false;
	}

	private String _server(String url) {
		int i = url.indexOf("/", 8);
		if (i > 0) {
			return url.substring(0, i);
		}
		return url;
	}

	private String _protocol(String url) {
		int i = url.indexOf("/");
		if (i > 0) {
			return url.substring(0, i);
		}
		return url;
	}

}
