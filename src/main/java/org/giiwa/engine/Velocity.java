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
package org.giiwa.engine;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.giiwa.dao.X;

/**
 * Velocity utility
 * 
 * @author wujun
 *
 */
public class Velocity {

	static Log log = LogFactory.getLog(Velocity.class);

	/**
	 * test the velocity sentence is true or false by the data model.
	 * 
	 * @param s the sentence of the velocity, e.g. $age &lt; 10
	 * @param m the data model
	 * @return boolean of the test
	 * @exception Exception throw exception when occur error
	 */
	public static boolean test(String s, Map<String, Object> m) throws Exception {
		if (log.isDebugEnabled())
			log.debug("vengine.test ...");
		if (X.isEmpty(s)) {
			return true;
		}
//    System.out.println(s);
		s = s.replaceAll("\\$", "\\\\\\$");
//    System.out.println(s);
		s = M.replaceAll("sss", s);

		Map<String, Object> b = new HashMap<String, Object>();
		m.put("result", b);
		try {
			execute(s, m);
		} catch (Exception e) {
			throw new Exception("expression error, e.g ${age}>10", e);
		}
		m.remove("result");

		if (b.containsKey("bool")) {
			Object o = b.get("bool");
			if (o instanceof Boolean) {
				return (Boolean) o;
			}
			return !X.isEmpty(o);
		}
		return false;
	}

	/**
	 * execute the velocity sentence.
	 * 
	 * @param s the sentence of velocity, it can be a template file.
	 * @param m the data model transfer to the sentence
	 * @return boolean true it success executed, or throw the exception
	 * @exception Exception throw exception when occur error
	 */
	public static boolean execute(String s, Map<String, Object> m) throws Exception {
		if (log.isDebugEnabled())
			log.debug("vengine.execute ...");

		try {
			VelocityContext context = new VelocityContext(m);
			StringWriter out = new StringWriter();
			// log.debug("s=\r\n" + s);
			org.apache.velocity.app.Velocity.evaluate(context, out, "ve", s);
			if (log.isDebugEnabled())
				log.debug("s=" + s + ", out=" + out);
			return true;
		} catch (Exception e) {
			log.error(s, e);
			throw e;
		}

	}

	private final static String M = ".set($result.bool=(sss))";

	/**
	 * Parses the string with the model
	 *
	 * @param s the string
	 * @param m the model
	 * @return the string
	 * @throws Exception the exception
	 */
	public static String parse(String s, Map<String, Object> m) throws Exception {
		if (log.isDebugEnabled())
			log.debug("vengine.parse ...");

		if (X.isEmpty(s)) {
			return s;
		}

		try {
			VelocityContext context = new VelocityContext(m);
			StringWriter out = new StringWriter();
			org.apache.velocity.app.Velocity.evaluate(context, out, "ve", s);
			if (log.isDebugEnabled())
				log.debug("s=" + s + ", out=" + out);
			return out.toString();
		} catch (Exception e) {
			log.error(s, e);
			throw e;
		}
	}

}
