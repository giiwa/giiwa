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
package org.giiwa.framework.bean;

import java.io.File;

import org.apache.commons.configuration.Configuration;
import org.giiwa.core.bean.UID;

/**
 * Create Temporary file, which can be accessed by web api, please refer
 * model("/temp")
 * 
 * @author joe
 *
 */
public class Temp {

  public static String ROOT;

  /**
   * Initialize the Temp object, this will be invoke in giiwa startup
   * 
   * @param conf
   *          the conf
   */
  public static void init(Configuration conf) {
    ROOT = conf.getString("temp.path", "/opt/temp/");
  }

  private String id   = null;
  private String name = null;
  private File   file = null;

  private Temp() {

  }

  /**
   * get the Temp file for name
   * 
   * @param name
   *          the file name
   * @return the Temp
   */
  public static Temp create(String name) {
    Temp t = new Temp();
    t.name = name;
    t.id = UID.id(System.currentTimeMillis(), UID.random());
    t.file = get(t.id, name);
    if (t.file.exists()) {
      t.file.delete();
    } else {
      t.file.getParentFile().mkdirs();
    }
    return t;
  }

  /**
   * get the Id
   * 
   * @return String of id
   */
  public String getId() {
    return id;
  }

  /**
   * get the File
   * 
   * @return File the file
   */
  public File getFile() {
    return file;
  }

  /**
   * get the web access uri directly
   * 
   * @return String of uri
   */
  public String getUri() {
    return "/temp/" + id + "/" + name + "?" + ((file == null || !file.exists()) ? 0 : file.lastModified());
  }

  /**
   * Gets the.
   * 
   * @param id
   *          the id
   * @param name
   *          the name
   * @return the file
   */
  public static File get(String id, String name) {
    return new File(path(id, name));
  }

  static private String path(String path, String name) {
    long id = Math.abs(UID.hash(path));
    char p1 = (char) (id % 23 + 'a');
    char p2 = (char) (id % 19 + 'A');
    char p3 = (char) (id % 17 + 'a');
    char p4 = (char) (id % 13 + 'A');

    StringBuilder sb = new StringBuilder(ROOT);

    sb.append("/").append(p1).append("/").append(p2).append("/").append(p3).append("/").append(p4).append("/")
        .append(id);

    if (name != null)
      sb.append("_").append(name);

    return sb.toString();
  }

}
