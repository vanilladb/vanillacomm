package org.vanilladb.comm.protocols.zabelection;

import org.vanilladb.comm.protocols.events.ProcessListInit;
import org.vanilladb.comm.protocols.tcpfd.FailureDetected;
import org.vanilladb.comm.protocols.tcpfd.ProcessConnected;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;

public class ZabElectionLayer extends Layer {
	
	public ZabElectionLayer() {
		// Events that the protocol will create
		evProvide = new Class[] {
			LeaderInit.class,
			LeaderChanged.class
		};
		
		// Events that the protocol requires to work
		// This is a subset of the accepted events
		evRequire = new Class[] {
			ProcessListInit.class,
			ProcessConnected.class,
			FailureDetected.class,
		};
		
		// Events that the protocol will accept
		evAccept = new Class[] {
			ProcessListInit.class,
			ProcessConnected.class,
			FailureDetected.class
		};
	}

	@Override
	public Session createSession() {
		return new ZabElectionSession(this);
	}
}
