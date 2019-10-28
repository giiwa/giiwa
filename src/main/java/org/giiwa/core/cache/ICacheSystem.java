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
package org.giiwa.core.cache;

interface ICacheSystem {

	/**
	 * Gets the.
	 *
	 * @param id the id
	 * @return the object
	 */
	Object get(String name);

	/**
	 * Delete.
	 *
	 * @param id the id
	 * @return true, if successful
	 */
	boolean delete(String name);

	/**
	 * Sets the.
	 *
	 * @param id   the id
	 * @param data the data
	 * @return true, if successful
	 */
	boolean set(String name, Object data, int expired);

	boolean trylock(String name);

	void expire(String name, long ms);

	boolean unlock(String name, String value);

}
