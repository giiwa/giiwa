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
package org.giiwa.framework.web;

import java.lang.annotation.*;

import org.giiwa.core.bean.X;

/**
 * the {@code Path} annotation interface used to define a Web api, for each
 * annotated method, the framework will auto generate the web api mapping for
 * the method,
 * 
 * <br>
 * the whole web api uri should be=
 * <tt>http://[host]/[classname]/[method path]</tt>, method including: <br>
 * 
 * <pre>
 * path=X.NONE (no path defined)
 * method=Model.METHOD_GET|MOdel.METHOD_POST (handle all request method)
 * login=false (no required login)
 * access=X.NONE (not required access key name)
 * accesslog=true (record the accesslog if the run level is debug)
 * </pre>
 * 
 * @author joe
 * 
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Path {

	/**
	 * the URI path, default is "none".
	 *
	 * @return String
	 */
	String path() default X.NONE;

	/**
	 * the method of request, default for all.
	 *
	 * @return int
	 */
	int method() default Model.METHOD_GET | Model.METHOD_POST;

	/**
	 * login required, default is "false".
	 *
	 * @return boolean
	 */
	boolean login() default false;

	/**
	 * the access key that required, default is "none".
	 *
	 * @return String
	 */
	String access() default X.NONE;

	/**
	 * Log the data of request and response in oplog, default is 0 -$gt; none.
	 *
	 * @return int
	 */
	int log() default 0;

	/**
	 * log the access of client info, default is true.
	 *
	 * @return boolean
	 */
	boolean accesslog() default true;

}
