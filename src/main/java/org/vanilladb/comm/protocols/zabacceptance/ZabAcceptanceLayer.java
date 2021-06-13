package org.vanilladb.comm.protocols.zabacceptance;

import org.vanilladb.comm.protocols.events.ProcessListInit;
import org.vanilladb.comm.protocols.tcpfd.FailureDetected;
import org.vanilladb.comm.protocols.tcpfd.ProcessConnected;
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
			ZabCacheProposal.class,
		};
		
		// Events that the protocol requires to work
		// This is a subset of the accepted events
		evRequire = new Class[] {
			ProcessListInit.class,
			ProcessConnected.class,
			ZabPropose.class
		};
		
		// Events that the protocol will accept
		evAccept = new Class[] {
			ProcessListInit.class,
			ProcessConnected.class,
			FailureDetected.class,
			LeaderChanged.class,
			ZabPropose.class,
		};
	}

	@Override
	public Session createSession() {
		return new ZabAcceptanceSession(this);
	}
}

