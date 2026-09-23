package org.giiwa.task;

import java.io.Serializable;

@FunctionalInterface
public interface TrConsumer<T, U, V> extends Serializable {

	void accept(T t, U u, V v);

}
