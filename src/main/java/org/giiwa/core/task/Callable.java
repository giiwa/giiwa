package org.giiwa.core.task;

import org.giiwa.core.json.JSON;

/**
 * used to async call instead of runnable
 * 
 * @author joe
 *
 */
public interface Callable {

	public void onCall(int state, JSON jo);

}
