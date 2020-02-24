package org.giiwa.task;

import java.io.Serializable;

/**
 * used to async call instead of runnable
 * 
 * @author joe
 *
 */
@FunctionalInterface
public interface TaskFunction extends Serializable {
	public void call();
}
