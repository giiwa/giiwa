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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.giiwa.framework.web.Module;

public class VelocityTemplateLoader extends ClasspathResourceLoader {

  static Log               log   = LogFactory.getLog(VelocityTemplateLoader.class);

  /**
   * cache the file
   */
  static Map<String, File> cache = new HashMap<String, File>();

  /**
   * Clean.
   */
  public static void clean() {
    cache.clear();
  }

  @Override
  public boolean isCachingOn() {
    return true;
  }

  /**
   * load template from the setting module, this make the template will be
   * overload in child
   * 
   * @param resource
   * @return File
   */
  private File getFile(String resource) {
    File f = cache.get(resource);
    try {
      if (f == null) {
        f = new File(resource);
        if (!f.exists()) {
          f = Module.home.getFile(resource);
        }

        if (f != null) {
          cache.put(resource, f);
          // log.info(resource + "=>" + f.getCanonicalPath());
        } else if (log.isDebugEnabled()) {
          // not found the file
          log.debug("not exists, resource=" + resource);
        }
      }

    } catch (Exception e) {
      cache.remove(resource);
    }
    return f;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader#
   * getLastModified(org.apache.velocity.runtime.resource.Resource)
   */
  @Override
  public long getLastModified(Resource resource) {
    File f = getFile(resource.getName());
    if (f != null) {
      return f.lastModified();
    }
    return 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader#
   * getResourceStream(java.lang.String)
   */
  @Override
  public InputStream getResourceStream(String name) throws ResourceNotFoundException {
    File f = getFile(name);
    if (f != null) {
      try {
        return new FileInputStream(f);
      } catch (FileNotFoundException e) {
        log.error(e);
      }
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader#init
   * (org.apache.commons.collections.ExtendedProperties)
   */
  @Override
  public void init(ExtendedProperties configuration) {
	log.debug("VelocityTemplateLoader init..."+this.getClassName());
    super.init(configuration);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader#
   * isSourceModified(org.apache.velocity.runtime.resource.Resource)
   */
  @Override
  public boolean isSourceModified(Resource resource) {
    if (resource == null)
      return true;

    File f = getFile(resource.getName());

    return f == null || f.lastModified() != resource.getLastModified();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.velocity.runtime.resource.loader.ResourceLoader#resourceExists
   * (java.lang.String)
   */
  @Override
  public boolean resourceExists(String name) {
    // if (log.isDebugEnabled())
    // log.debug("exists? name=" + name);
    File f = getFile(name);
    return f != null;
  }
}
