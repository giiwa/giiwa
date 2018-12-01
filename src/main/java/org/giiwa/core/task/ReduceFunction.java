package org.giiwa.core.task;

import java.io.Serializable;

/**
 * used to async call instead of runnable
 * 
 * @author joe
 *
 */
@FunctionalInterface
public interface ReduceFunction<T, V> extends Serializable {
	public T call(V e);
}
