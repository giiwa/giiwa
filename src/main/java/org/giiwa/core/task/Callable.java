package org.giiwa.core.task;

/**
 * used to async call instead of runnable
 * 
 * @author joe
 *
 */
public interface Callable<V> {

	public void onCall(int state, V jo);

}
