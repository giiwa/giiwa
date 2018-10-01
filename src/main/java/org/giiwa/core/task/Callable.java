package org.giiwa.core.task;

import java.io.Serializable;

/**
 * used to async call instead of runnable
 * 
 * @author joe
 *
 */
@FunctionalInterface
public interface Callable<T, V> extends Serializable {
	public T call(int state, V e);
}
