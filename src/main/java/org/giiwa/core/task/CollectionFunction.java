package org.giiwa.core.task;

import java.io.Serializable;

/**
 * used to async call instead of runnable
 * 
 * @author joe
 *
 */
@FunctionalInterface
public interface CollectionFunction<V> extends Serializable {
	public void call(V e);
}
