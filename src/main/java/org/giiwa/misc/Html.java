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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.task.Consumer;
import org.giiwa.web.QueryString;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
import org.giiwa.dao.Comment;

/**
 * The {@code Html} Class used to string to html, html to plain, or get images
 * from the html string.
 * 
 * @author joe
 *
 */
@Comment(text = "HTML")
public final class Html {

	/** The log. */
	private static Log log = LogFactory.getLog(Html.class);

	/** The d. */
	transient Document d = null;
	transient String html;

	public String url;

	private Html(String html) {
		if (html != null) {
			this.html = html;
			d = Jsoup.parse(html);
		}
	}

	@Override
	public String toString() {
		return body;
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
	@Comment(text = "title")
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
	@Comment(text = "body")
	public String body() {
		return body(false);
	}

	public String body(boolean fullbody) {
		if (body == null && d != null) {
			if (!fullbody) {
				d.select("iframe").remove();
			}
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
	 * 纯文本
	 * 
	 * @return the string
	 */
	@Comment(text = "text")
	public String text() {
		if (text == null && d != null) {
			text = d.text();
		}

		return text;
	}

	/**
	 * 带基本样式的纯文本
	 * 
	 * @return
	 */
	@Comment(text = "text2")
	public String text2() {
		if (text == null && d != null) {
			text = Jsoup.clean(html, Whitelist.basicWithImages());
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
	 * please refer to getTags().
	 *
	 * @param tag the html tag
	 * @return List of element
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
	 * @Deprecated please refer to select
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

	@Comment(text = "select")
	public Elements select(@Comment(text = "selector") String selector) {
		if (d == null)
			return null;

		return d.select(selector);
	}

	/**
	 * find the elements in the node.
	 * 
	 * @Deprecated
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
	 * @Deprecated
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

	@Comment(text = "finder")
	public StringFinder finder() {
		return StringFinder.create(body);
	}

	@Comment(text = "json")
	public JSON json() {
		try {
			return JSON.fromObject(body);
		} catch (Exception e) {
			throw new RuntimeException(body);
		}
	}

	@Comment(text = "jsons")
	public List<JSON> jsons() {
		try {
			return JSON.fromObjects(body);
		} catch (Exception e) {
			throw new RuntimeException(body);
		}
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
	 * @Deprecated
	 * @param regex
	 * @return
	 * @throws Exception
	 */
	@Comment(text = "href")
	public List<JSON> href(@Comment(text = "regex") String... regex) throws Exception {
		return a(regex);
	}

	/**
	 * 
	 * find all a tag
	 * 
	 * @return
	 * @throws Exception
	 */
	@Comment(text = "a")
	public List<JSON> a(@Comment(text = "regex") Object regex) throws Exception {
		return link("a", "href", regex, (Consumer<Url>) null);
	}

	@Comment(text = "a")
	public List<JSON> a(@Comment(text = "regex") Object regex, @Comment(text = "func") Consumer<Url> func)
			throws Exception {
		return link("a", "href", regex, func);
	}

//	public List<JSON> a(Object regex, JSObject func) throws Exception {
//		return link("a", "href", regex, func);
//	}

	@Comment(text = "link")
	public List<JSON> link(@Comment(text = "selector") String selector, @Comment(text = "attr") String attr,
			@Comment(text = "regex") Object regex) throws Exception {
		return link(selector, attr, regex, null);
	}

	@Comment(text = "link")
	public List<JSON> link(@Comment(text = "selector") String selector, @Comment(text = "attr") String attr,
			@Comment(text = "regex") Object regex, @Comment(text = "func") Consumer<Url> func) throws Exception {

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
//					href = u1.encodedUrl();
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
	@Comment(text = "format")
	public String format(@Comment(text = "href") String href) {
		return format(href, true);
	}

	@Comment(text = "format")
	public String format(@Comment(text = "href") String href, @Comment(text = "encode") boolean encode) {
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

		i = href.indexOf("//", 10);
		while (i > 0) {
			href = href.substring(0, i) + href.substring(i + 1);
			i = href.indexOf("//", 10);
		}

		i = href.indexOf("/../");
		while (i > 0) {
			int j = href.lastIndexOf("/", i - 1);
			if (j > 0) {
				href = href.substring(0, j + 1) + href.substring(i + 4);
			}
			i = href.indexOf("/../");
		}

		if (encode) {
			QueryString qs = new QueryString(href);
			return qs.toString();
		} else {
			return href;
		}

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
