package org.giiwa.dfile;

public interface IRequestHandler {

	/**
	 * process the request, and MUST release the request
	 * 
	 * @param r       the request
	 * @param handler the response handler
	 */
	void process(Request r, IResponseHandler handler);

	void closed(String name);

}
