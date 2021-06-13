package org.vanilladb.comm.protocols.beb;

import org.vanilladb.comm.protocols.events.ProcessListInit;
import org.vanilladb.comm.protocols.tcpfd.FailureDetected;
import org.vanilladb.comm.protocols.tcpfd.ProcessConnected;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;

/**
 * Best Effort Broadcast.
 * 
 * @author yslin
 */
public class BestEffortBroadcastLayer extends Layer {
	
	public BestEffortBroadcastLayer() {
		// Events that the protocol will create
		evProvide = new Class[] {
		};
		
		// Events that the protocol requires to work
		// This is a subset of the accepted events
		evRequire = new Class[] {
			ProcessListInit.class,
			ProcessConnected.class,
			Broadcast.class
		};
		
		// Events that the protocol will accept
		evAccept = new Class[] {
			ProcessListInit.class,
			ProcessConnected.class,
			FailureDetected.class,
			Broadcast.class
		};
	}

	@Override
	public Session createSession() {
		return new BestEffortBroadcastSession(this);
	}
}
