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
package org.giiwa.dao;

import java.io.Serializable;

/**
 * The {@code TimeStamp} Class used to create a time stamp
 * 
 * @author joe
 *
 */
public final class TimeStamp implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** The start. */
	long start;

	// 初始值
	long init;

	/**
	 * Creates the.
	 *
	 * @return the time stamp
	 */
	public static TimeStamp create() {
		return new TimeStamp();
	}

	/**
	 * Instantiates a new time stamp.
	 */
	public TimeStamp() {
		start = System.nanoTime();
	}

	/**
	 * Sets the.
	 *
	 * @param s the s
	 * @return the time stamp
	 */
	public TimeStamp set(long s) {
		start = s;
		return this;
	}

	/**
	 * past ms
	 *
	 * @return the long
	 */
	public String past() {
		long s = pastns();
		if (s < 1000) {
			return s + "ns";
		}
		s /= 1000;
		if (s < 1000) {
			return s + "us";
		}
		s /= 1000;
		if (s < 1000) {
			return s + "ms";
		}
		s /= 1000;
		return s + "s";
	}

	/**
	 * past ms
	 * 
	 * 
	 * @return the ms of past
	 */
	public long pastms() {
		return pastus() / 1000;
	}

	/**
	 * past us
	 * 
	 * @return
	 */
	public long pastus() {
		return pastns() / 1000;
	}

	/**
	 * past ns
	 * 
	 * @return
	 */
	public long pastns() {
		if (start > -1) {
			return System.nanoTime() - start + init;
		} else {
			return init;
		}
	}

	/**
	 * Gets the.
	 *
	 * @return the long
	 */
	public long get() {
		return start;
	}

	/**
	 * Reset.
	 *
	 * @return the long
	 */
	public long reset() {
		long r = pastms();
		start = System.nanoTime();
		init = 0;
		return r;
	}

	public void pause() {
		if (start > -1) {
			init += System.nanoTime() - start;
			start = -1;
		}
	}

	public void resume() {
		if (start == -1) {
			start = System.nanoTime();
		}
	}

}
