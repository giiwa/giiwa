package org.giiwa.task;

import java.io.Serializable;

/**
 * used to async call instead of runnable
 * 
 * @author joe
 *
 */
@FunctionalInterface
public interface Function<K, V> extends java.util.function.Function<K, V>, Serializable {

}
