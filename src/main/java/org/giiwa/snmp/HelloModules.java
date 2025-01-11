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
package org.giiwa.snmp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.MOServer;
import org.snmp4j.agent.mo.MOFactory;
import org.snmp4j.smi.OctetString;

public class HelloModules implements MOGroup {
	
	@SuppressWarnings("unused")
	private static Log log = LogFactory.getLog(HelloModules.class);

	private HelloMib helloMib;

	@SuppressWarnings("unused")
	private MOFactory factory;

	// --AgentGen BEGIN=_MEMBERS
	// --AgentGen END

	public HelloModules() {
		helloMib = new HelloMib();
		// --AgentGen BEGIN=_DEFAULTCONSTRUCTOR
		// --AgentGen END
	}

	public HelloModules(MOFactory factory) {
		helloMib = new HelloMib(factory);
		// --AgentGen BEGIN=_CONSTRUCTOR
		// --AgentGen END
	}

	public void registerMOs(MOServer server, OctetString context) throws DuplicateRegistrationException {
		helloMib.registerMOs(server, context);
		// --AgentGen BEGIN=_registerMOs
		// --AgentGen END
	}

	public void unregisterMOs(MOServer server, OctetString context) {
		helloMib.unregisterMOs(server, context);
		// --AgentGen BEGIN=_unregisterMOs
		// --AgentGen END
	}

	public HelloMib getSnmp4jDemoMib() {
		return helloMib;
	}

	// --AgentGen BEGIN=_METHODS
	// --AgentGen END

	// --AgentGen BEGIN=_CLASSES
	// --AgentGen END

	// --AgentGen BEGIN=_END
	// --AgentGen END

}
