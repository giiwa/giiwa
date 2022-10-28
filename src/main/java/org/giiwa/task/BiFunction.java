package org.giiwa.task;

import java.io.Serializable;

/**
 * used to async call instead of runnable
 * 
 * @author joe
 *
 */
@FunctionalInterface
public interface BiFunction<K, V1, V2> extends java.util.function.BiFunction<K, V1, V2>, Serializable {

}
