package org.giiwa.core.task;

import java.io.Serializable;

/**
 * used to async call instead of runnable
 * 
 * @author joe
 *
 */
@FunctionalInterface
public interface Callable2<T, V> extends Serializable {
	public void call(T t, V e);
}
