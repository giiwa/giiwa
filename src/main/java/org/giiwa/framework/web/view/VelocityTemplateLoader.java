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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.util.ExtProperties;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Local;
import org.giiwa.framework.bean.Disk;
import org.giiwa.framework.web.Module;

public class VelocityTemplateLoader extends ClasspathResourceLoader {

	static Log log = LogFactory.getLog(VelocityTemplateLoader.class);

	/**
	 * cache the file
	 */
	static Map<String, Object> cache = new HashMap<String, Object>();

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
	private Object getFile(String resource) {

		Object f = cache.get(resource);
		try {
			if (f == null) {
				if (X.isSame("VM_global_library.vm", resource)) {
					f = Module.home.getFile("/notfound.html");
				} else {
					f = new File(resource);
					if (!((File) f).exists()) {
						f = Module.home.getFile(resource);
					}
				}

				if (f == null || !((File) f).exists()) {
					f = Disk.seek(resource);
				}

				if (f != null) {

					if (log.isDebugEnabled())
						log.debug("got resource=" + resource);

					if (Local.getInt("web.debug", 0) == 0) {
						// not debug
						cache.put(resource, f);
					}

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
	 * @see org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader.
	 * getLastModified(org.apache.velocity.runtime.resource.Resource)
	 */
	@Override
	public long getLastModified(Resource resource) {
		Object f = getFile(resource.getName());
		if (f != null) {
			return View.lastModified(f);
		}
		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader.
	 * isSourceModified(org.apache.velocity.runtime.resource.Resource)
	 */
	@Override
	public boolean isSourceModified(Resource resource) {
		if (resource == null)
			return true;

		Object f = getFile(resource.getName());

		return f == null || View.lastModified(f) != resource.getLastModified();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.velocity.runtime.resource.loader.ResourceLoader.resourceExists
	 * (java.lang.String)
	 */
	@Override
	public boolean resourceExists(String name) {
		// if (log.isDebugEnabled())
		// log.debug("exists? name=" + name);
		Object f = getFile(name);
		if (f != null) {
			return true;
		} else {
			return super.resourceExists(name);
		}
	}

	@Override
	public Reader getResourceReader(String name, String encoding) throws ResourceNotFoundException {
		Object f = getFile(name);
		if (f != null) {
			try {
				return new InputStreamReader(View.getInputStream(f), encoding);
			} catch (IOException e) {
				log.error(e);
			}
		}
		return super.getResourceReader(name, encoding);
	}

	@Override
	public void init(ExtProperties configuration) {
		if (log.isDebugEnabled())
			log.debug("VelocityTemplateLoader init..." + this.getClassName());
		try {
			super.init(configuration);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
	}

}
