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
package org.giiwa.core.cache;

// TODO: Auto-generated Javadoc
public interface ICacheSystem {

	/**
   * Gets the.
   *
   * @param id
   *          the id
   * @return the cachable
   */
	Cachable get(String id);

	/**
   * Delete.
   *
   * @param id
   *          the id
   * @return true, if successful
   */
	boolean delete(String id);

	/**
   * Sets the.
   *
   * @param id
   *          the id
   * @param data
   *          the data
   * @return true, if successful
   */
	boolean set(String id, Cachable data);

}
