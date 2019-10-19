package org.giiwa.core.task;

/**
 * using system thread queue to run this task
 * 
 * @author joe
 *
 */
public abstract class SysTask extends Task {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected final boolean isSys() {
		return true;
	}

}
