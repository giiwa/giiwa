package org.giiwa.core.dfile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.nio.IResponseHandler;
import org.giiwa.core.nio.Request;

public interface ICommand {

	final static Log log = LogFactory.getLog(ICommand.class);

	public static final byte CMD_INFO = 1;
	public static final byte CMD_GET = 2;
	public static final byte CMD_PUT = 3;
	public static final byte CMD_DELETE = 4;
	public static final byte CMD_LIST = 5;
	public static final byte CMD_MKDIRS = 6;
	public static final byte CMD_MOVE = 7;
	public static final byte CMD_HTTP = 8;
	public static final byte CMD_NOTIFY = 9;

	public void process(Request in, IResponseHandler handler);

}
