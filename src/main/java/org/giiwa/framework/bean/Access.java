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
package org.giiwa.framework.bean;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.giiwa.core.bean.*;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;

// TODO: Auto-generated Javadoc
/**
 * access token class, it's Bean and mapping "gi_access" collection, it mapping
 * the "access" method in @Path interface. <br>
 * collection="gi_access"
 * 
 * @author yjiang
 * 
 */
@Table(name = "gi_access")
public class Access extends Bean {
  /**
  * 
  */
  private static final long serialVersionUID = 1L;

  @Column(name = X.ID)
  private String            name;

  /**
   * get the group name of the access name
   * 
   * @return the string
   */
  public String groupName() {
    int i = name.indexOf(".");
    if (i > 0) {
      int j = name.indexOf(".", i + 1);
      if (j > 0) {
        return name.substring(0, j);
      } else {
        return name.substring(0, i);
      }
    }
    return "access";
  }

  /**
   * get the access name
   * 
   * @return String
   */
  public String getName() {
    return name;
  }

  /**
   * Add a access name, the access name MUST fit with "access.[group].[name]" .
   * 
   * @param name
   *          the name
   */
  public static void set(String name) {
    if (X.isEmpty(name) || !name.startsWith("access.")) {
      log.error("error access.name: " + name, new Exception("error access name:" + name));
    } else if (!exists(name)) {
      String[] ss = name.split("[\\|｜]");
      for (String s : ss) {
        Helper.insert(V.create(X.ID, s), Access.class);
      }
    }
  }

  static private Set<String> cache = new HashSet<String>();

  /**
   * check exists of the name
   *
   * @param name
   *          the name
   * @return true, if successful
   */
  public static boolean exists(String name) {
    if (cache.contains(name)) {
      return true;
    }

    try {
      if (Helper.exists(name, Access.class)) {
        cache.add(name);
        return true;
      }
    } catch (Exception e1) {
      log.error(e1.getMessage(), e1);
    }
    return false;
  }

  /**
   * Load all access and group by [group] name.
   *
   * @return the map
   */
  public static Map<String, List<Access>> load() {
    Beans<Access> bs = Helper.load(W.create(), 0, Integer.MAX_VALUE, Access.class);
    List<Access> list = bs.getList();
    Map<String, List<Access>> r = new TreeMap<String, List<Access>>();
    String group = null;
    List<Access> last = null;
    for (Access a : list) {
      if (!X.isEmpty(a.name)) {
        String name = a.groupName();
        if (group == null || !name.equals(group)) {
          group = name;
          last = new ArrayList<Access>();
          r.put(group, last);
        }
        last.add(a);
      }
    }

    return r;
  }

}
