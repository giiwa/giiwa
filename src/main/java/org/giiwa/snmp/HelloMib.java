package org.giiwa.snmp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.MOServer;
import org.snmp4j.agent.mo.DefaultMOFactory;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOFactory;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;

public class HelloMib
//--AgentGen BEGIN=_EXTENDS
//--AgentGen END
		implements MOGroup
//--AgentGen BEGIN=_IMPLEMENTS
//--AgentGen END
{
	@SuppressWarnings("unused")
	private static Log log = LogFactory.getLog(HelloMib.class);

//--AgentGen BEGIN=_STATIC
//--AgentGen END

//Factory
	@SuppressWarnings("unused")
	private MOFactory moFactory = DefaultMOFactory.getInstance();

//Constants

	/**
	 * OID of this MIB module for usage which can be used for its identification.
	 */
	public static final OID oidHelloMib = new OID(new int[] {});

	public static final OID oidSysDescr = new OID(new int[] { 1, 3, 6, 1, 2, 1, 4, 1, 1949, 1, 0 });
	public static final OID oidSysTest = new OID(new int[] { 1, 3, 6, 1, 2, 1, 4, 1, 1949, 2, 0 });

//Enumerations

//TextualConventions

//Scalars
	@SuppressWarnings("rawtypes")
	private MOScalar sysDescr;
	@SuppressWarnings("rawtypes")
	private MOScalar sysTest;

//Tables

//--AgentGen BEGIN=_MEMBERS
//--AgentGen END

	/**
	 * Constructs a HelloMib instance without actually creating its
	 * <code>ManagedObject</code> instances. This has to be done in a sub-class
	 * constructor or after construction by calling
	 * {@link #createMO(MOFactory moFactory)}.
	 */
	protected HelloMib() {
//--AgentGen BEGIN=_DEFAULTCONSTRUCTOR
//--AgentGen END
	}

	/**
	 * Constructs a HelloMib instance and actually creates its
	 * <code>ManagedObject</code> instances using the supplied
	 * <code>MOFactory</code> (by calling {@link #createMO(MOFactory moFactory)}).
	 * 
	 * @param moFactory the <code>MOFactory</code> to be used to create the managed
	 *                  objects for this module.
	 */
	public HelloMib(MOFactory moFactory) {
		createMO(moFactory);
//--AgentGen BEGIN=_FACTORYCONSTRUCTOR
//--AgentGen END
	}

//--AgentGen BEGIN=_CONSTRUCTORS
//--AgentGen END

	/**
	 * Create the ManagedObjects defined for this MIB module using the specified
	 * {@link MOFactory}.
	 * 
	 * @param moFactory the <code>MOFactory</code> instance to use for object
	 *                  creation.
	 */
	protected void createMO(MOFactory moFactory) {
		addTCsToFactory(moFactory);
		sysDescr = moFactory.createScalar(oidSysDescr, moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_WRITE),
				new OctetString("2222222222222"));
		sysTest = moFactory.createScalar(oidSysTest, moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ_WRITE),
				new OctetString("test"));

	}

	@SuppressWarnings("rawtypes")
	public MOScalar getSysDescr() {
		return sysDescr;
	}

	@SuppressWarnings("rawtypes")
	public MOScalar getSysTest() {
		return sysTest;
	}

	public void registerMOs(MOServer server, OctetString context) throws DuplicateRegistrationException {
// Scalar Objects
		server.register(this.sysDescr, context);
		server.register(this.sysTest, context);
//--AgentGen BEGIN=_registerMOs
//--AgentGen END
	}

	public void unregisterMOs(MOServer server, OctetString context) {
// Scalar Objects
		server.unregister(this.sysDescr, context);
		server.unregister(this.sysTest, context);
//--AgentGen BEGIN=_unregisterMOs
//--AgentGen END
	}

//Notifications

//Scalars

//Value Validators

//Rows and Factories

//--AgentGen BEGIN=_METHODS
//--AgentGen END

//Textual Definitions of MIB module HelloMib
	protected void addTCsToFactory(MOFactory moFactory) {
	}

//--AgentGen BEGIN=_TC_CLASSES_IMPORTED_MODULES_BEGIN
//--AgentGen END

//Textual Definitions of other MIB modules
	public void addImportedTCsToFactory(MOFactory moFactory) {
	}

//--AgentGen BEGIN=_TC_CLASSES_IMPORTED_MODULES_END
//--AgentGen END

//--AgentGen BEGIN=_CLASSES
//--AgentGen END

//--AgentGen BEGIN=_END
//--AgentGen END
}
