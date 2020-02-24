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

/**
 * the Module Filter for the url <br>
 * the filter is configured in module.xml
 * 
 * <pre>
&lt;?xml version="1.0" encoding="UTF-8"?&gt;
&lt;module version="1.0"&gt;
  ...
 &lt;filter&gt;
   &lt;pattern&gt;/user/login&lt;/pattern&gt;
   &lt;class&gt;org.giiwa.forum.web.UserFilter&lt;/class&gt;
 &lt;/filter&gt;
 ...
&lt;/module&gt;
 * </pre>
 * 
 * @author wujun
 *
 */
public interface IFilter {

	/**
	 * before dispatch to the model
	 * 
	 * @param m
	 *            the model
	 * @return boolean, true = pass to the model, pass to the next "filter"; false =
	 *         stop pass to the model, and stop pass to the next "filter"
	 */
	default boolean before(Controller m) {
		return true;
	};

	/**
	 * after dispatched to the model
	 * 
	 * @param m
	 *            the model
	 * @return boolean, true = pass to the next "filter", false = stop the pass to
	 *         the next "filter"
	 */
	default boolean after(Controller m) {
		return true;
	};

}
