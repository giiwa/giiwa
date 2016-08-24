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
package org.giiwa.framework.web.view;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.framework.web.Model;

public abstract class View {

  static Log log = LogFactory.getLog(View.class);

  /**
   * parse the file with the model
   * 
   * @param file
   *          the file
   * @param m
   *          the model
   * @return true: successful,
   * @throws Exception
   *           if occur error
   */
  protected abstract boolean parse(File file, Model m) throws Exception;

  /**
   * init the views by config
   * 
   * @param config
   *          the config
   */
  public static void init(Map<String, String> config) {

    for (String name : config.keySet()) {
      if (name.startsWith(".")) {
        String value = config.get(name);
        try {
          View v = (View) Class.forName(value).newInstance();
          views.put(name, v);
        } catch (Exception e1) {
          log.error(value, e1);
        }
      }
    }

    log.debug("config=" + views);

  }

  /**
   * parse the file with the model
   * 
   * @param file
   *          the file
   * @param m
   *          the model
   * @throws Exception
   *           if occur error
   */
  public static void merge(File file, Model m) throws Exception {

    String name = file.getName();
    for (String suffix : views.keySet()) {
      if (name.endsWith(suffix)) {
        View v = views.get(suffix);
        v.parse(file, m);
        return;
      }
    }

    fileview.parse(file, m);
  }

  private static Map<String, View> views    = new HashMap<String, View>();
  private static View              fileview = new FileView();
}
