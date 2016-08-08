/*
 * Copyright 2015 Giiwa, Inc. and/or its affiliates.
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
package org.giiwa.core.vengine;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.giiwa.core.bean.X;

// TODO: Auto-generated Javadoc
/**
 * @author wujun
 *
 */
public class VEngine {

  static Log log = LogFactory.getLog(VEngine.class);

  // public static void main(String[] args) {
  //
  // String s = "{age}>10";
  //
  // TimeStamp t = TimeStamp.create();
  // for (int i = 0; i < 100000; i++) {
  // Map<String, Object> m = new HashMap<String, Object>();
  // m.put("age", i);
  // // VEngine.test(s, m);
  // // System.out.println(i);
  // }
  // System.out.println(t.past() + "ms");
  // }

  /**
   * test the velocity sentence is true or false by the data model.
   * 
   * @param s
   *          the sentence of the velocity, e.g. $age &lt; 10
   * @param m
   *          the data model
   * @return boolean of the test
   * @exception Exception
   *              throw exception when occur error
   */
  public static boolean test(String s, Map<String, Object> m) throws Exception {
    log.debug("vengine.test ...");
    if (X.isEmpty(s)) {
      return true;
    }

    s = M.replaceAll("sss", s).replaceAll("\\{", "\\$\\{");

    Map<String, Object> b = new HashMap<String, Object>();
    m.put("result", b);
    try {
      execute(s, m);
    } catch (Exception e) {
      throw new Exception("expression error, e.g {age}>10", e);
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
   * @param s
   *          the sentence of velocity, it can be a template file.
   * @param m
   *          the data model transfer to the sentence
   * @return boolean true it success executed, or throw the exception
   * @exception Exception
   *              throw exception when occur error
   */
  public static boolean execute(String s, Map<String, Object> m) throws Exception {
    log.debug("vengine.execute ...");

    try {
      VelocityContext context = new VelocityContext(m);
      StringWriter out = new StringWriter();
      // log.debug("s=\r\n" + s);
      Velocity.evaluate(context, out, "ve", s);
      log.debug("s=" + s + ", out=" + out);
      return true;
    } catch (Exception e) {
      log.error(s, e);
      throw e;
    }

  }

  private final static String M = "#set($result.bool=(sss))";
}
