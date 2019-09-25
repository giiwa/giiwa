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
package org.giiwa.app.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.X;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.bean.*;
import org.giiwa.framework.web.*;

/**
 * web api： <a href='/menu' target='_blank'>/menu</a> <br>
 * used to get menu
 * 
 * @author joe
 * 
 */
public class menu extends Controller {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Override
	public void onGet() {
		onPost();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onPost()
	 */
	public void onPost() {
		User me = this.getUser();

		long id = this.getLong("root");
		String name = this.getString("name");

		Beans<Menu> bs = null;
		Menu m = null;
		if (!X.isEmpty(name)) {
			/**
			 * load the menu by id and name
			 */
			m = Menu.load(id, name);

			if (m != null) {

				/**
				 * load the submenu of the menu
				 */
				bs = m.submenu();
			}
		} else {
			/**
			 * load the submenu by id
			 */
			bs = Menu.submenu(id);

		}
		List<Menu> list = bs;

		/**
		 * filter out the item which no access
		 */
		Collection<Menu> ll = Menu.filterAccess(list, me);

		if (log.isDebugEnabled())
			log.debug("load menu: id=" + id + ", size=" + (list == null ? 0 : list.size()) + ", filtered="
					+ (ll == null ? 0 : ll.size()));

		/**
		 * convert the list to json array
		 */
		List<JSON> arr = new ArrayList<JSON>();

		if (ll != null) {
			Iterator<Menu> it = ll.iterator();

			while (it.hasNext()) {
				JSON jo = new JSON();
				m = it.next();

				/**
				 * set the text width language
				 */
				jo.put("text", lang.get(m.getName()));
				jo.put("id", m.getId());
				if (!X.isEmpty(m.getClasses())) {
					jo.put("classes", m.getClasses());
				}

				if (!X.isEmpty(m.getStyle())) {
					jo.put("style", m.getStyle());
				}

				/**
				 * set the url
				 */
				if (!X.isEmpty(m.getUrl())) {
					jo.put(X.URL, m.getUrl());
				}

				/**
				 * set children
				 */
				if (m.getChilds() > 0) {
					jo.put("hasChildren", true);
				}

				jo.put("seq", m.getSeq());
				jo.put("tag", m.getTag());
				if (!X.isEmpty(m.getLoad1())) {
					jo.put("load", m.getLoad1() + "?__node=" + this.getString("__node"));
				}

				if (!X.isEmpty(m.getClick())) {
					jo.put("click", m.getClick());
				}

				if (!X.isEmpty(m.getContent())) {
					jo.put("content", m.getContent());
				}

				arr.add(jo);
			}
		}

		this.response(arr);
	}
}
