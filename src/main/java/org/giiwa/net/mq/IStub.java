/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.net.mq;

import java.util.HashMap;
import java.util.Map;

import org.giiwa.net.mq.MQ.Mode;
import org.giiwa.net.mq.MQ.Request;

/**
 * The IStub Class used to define the APIs of Message stub
 * 
 * @author joe
 *
 */
public abstract class IStub {

	/**
	 * the service name that will bind on ActiveMQ
	 */
	protected String name;
	protected Mode mode = Mode.QUEUE;

	private Map<String, Object> _attach = new HashMap<String, Object>();

	public void attach(String name, Object obj) {
		_attach.put(name, obj);
	}

	@SuppressWarnings("unchecked")
	public <T> T attach(String name) {
		return (T) _attach.get(name);
	}

	public String getName() {
		return name;
	}

	/**
	 * Instantiates a new i stub.
	 *
	 * @param name the name
	 */
	public IStub(String name) {
		this.name = name;
	}

	public IStub(String name, Mode mode) {
		this.name = name;
		this.mode = mode;
	}

	/**
	 * Bind the stub on the MQ as Queue
	 *
	 * @throws Exception the exception
	 */
	public void bind() throws Exception {
		bindAs(mode);
	}

	protected boolean bound = false;

	/**
	 * Bind the stub on the ActiveMQ with the mode
	 *
	 * @param m the mode
	 * @throws Exception the exception
	 */
	public void bindAs(Mode m) throws Exception {
		mode = m;
		MQ.bind(name, this, m);
		bound = true;
	}

	public void destroy() throws Exception {
		MQ.unbind(this);
		MQ.destroy0(name, mode);
		bound = false;
	}

	/**
	 * unbind the stub
	 * 
	 * @throws Exception throw Exception if unbind failed
	 */
//	final public void unbind() throws Exception {
//		MQ.unbind(this);
//	}

	/**
	 * Send message to destination
	 *
	 * @param to  the destination queue
	 * @param req the message
	 * @throws Exception the exception
	 */
	public void send(String to, Request req) throws Exception {
		req.from = name;
		MQ.send(to, req);
	}

	/**
	 * send a topic to the destination
	 * 
	 * @param to  the destination topic
	 * @param req the message
	 * @throws Exception the exception
	 */
	public void topic(String to, Request req) throws Exception {
		req.from = name;
		MQ.topic(to, req);
	}

	/**
	 * On request triggered when message arrive
	 *
	 * @param seq the sequence
	 * @param req the message
	 */
	public abstract void onRequest(long seq, Request req);

}
