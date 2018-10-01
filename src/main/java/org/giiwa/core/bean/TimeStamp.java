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
package org.giiwa.core.bean;

import java.io.Serializable;

// TODO: Auto-generated Javadoc
/**
 * The {@code TimeStamp} Class used to create a time stamp
 * 
 * @author joe
 *
 */
public class TimeStamp implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** The start. */
	long start;

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
	 * @param s
	 *            the s
	 * @return the time stamp
	 */
	public TimeStamp set(long s) {
		start = s;

		return this;
	}

	/**
	 * past ms
	 *
	 * @deprecated
	 * @return the long
	 */
	public long past() {
		return pastms();
	}

	/**
	 * past ms
	 * 
	 * @return
	 */
	public long pastms() {
		return (System.nanoTime() - start) / 1000000;
	}

	/**
	 * past us
	 * 
	 * @return
	 */
	public long pastus() {
		return (System.nanoTime() - start) / 1000;
	}

	/**
	 * past ns
	 * 
	 * @return
	 */
	public long pastns() {
		return System.nanoTime() - start;
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
		long r = past();
		start = System.nanoTime();
		return r;
	}

	public static void main(String[] args) {
		TimeStamp t = TimeStamp.create();
		System.out.println(t.past());
		System.out.println(t.pastns());
		System.out.println(t.past());
	}
}
