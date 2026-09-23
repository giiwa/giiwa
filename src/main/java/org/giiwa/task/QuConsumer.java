package org.giiwa.task;

import java.io.Serializable;

@FunctionalInterface
public interface QuConsumer<T, U, V, W> extends Serializable {

	void accept(T t, U u, V v, W w);
	
}
