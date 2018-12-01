package org.giiwa.core.nio;

public interface IRequestHandler {

	/**
	 * process the request, and MUST release the request
	 * 
	 * @param r
	 * @param handler
	 */
	void process(Request r, IResponseHandler handler);

	void closed(String name);

}
