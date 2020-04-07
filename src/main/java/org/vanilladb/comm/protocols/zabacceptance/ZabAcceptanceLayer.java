package org.vanilladb.comm.protocols.zabacceptance;

import org.vanilladb.comm.protocols.events.ProcessListInit;
import org.vanilladb.comm.protocols.tcpfd.AllProcessesReady;
import org.vanilladb.comm.protocols.tcpfd.FailureDetected;
import org.vanilladb.comm.protocols.zabelection.LeaderChanged;
import org.vanilladb.comm.protocols.zabproposal.ZabPropose;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;

public class ZabAcceptanceLayer extends Layer {
	
	public ZabAcceptanceLayer() {
		// Events that the protocol will create
		evProvide = new Class[] {
			ZabAccept.class,
			ZabDeny.class,
			ZabCommit.class,
		};
		
		// Events that the protocol requires to work
		// This is a subset of the accepted events
		evRequire = new Class[] {
			ProcessListInit.class,
			AllProcessesReady.class,
			ZabPropose.class
		};
		
		// Events that the protocol will accept
		evAccept = new Class[] {
			ProcessListInit.class,
			AllProcessesReady.class,
			FailureDetected.class,
			LeaderChanged.class,
			ZabPropose.class,
			ZabAccept.class,
			ZabDeny.class,
		};
	}

	@Override
	public Session createSession() {
		return new ZabAcceptanceSession(this);
	}
}

