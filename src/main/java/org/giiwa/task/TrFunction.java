package org.giiwa.task;

import java.io.Serializable;

@FunctionalInterface
public interface TrFunction<T1, T2, T3, R> extends Serializable{

	R apply(T1 t1, T2 t2, T3 t3);
	
}
