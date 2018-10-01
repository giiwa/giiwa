package org.giiwa.core.task;

import java.io.Serializable;

/**
 * used to async call instead of runnable
 * 
 * @author joe
 *
 */
@FunctionalInterface
public interface MyFunc2 extends Serializable {
	public void call();
}
