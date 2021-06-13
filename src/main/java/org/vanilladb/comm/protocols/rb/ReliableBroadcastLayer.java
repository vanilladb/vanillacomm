package org.vanilladb.comm.protocols.rb;

import org.vanilladb.comm.protocols.events.ProcessListInit;
import org.vanilladb.comm.protocols.tcpfd.FailureDetected;
import org.vanilladb.comm.protocols.tcpfd.ProcessConnected;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;

/**
 * Lazy Reliable Broadcast.
 * 
 * @author nuno, DBN, yslin
 */
public class ReliableBroadcastLayer extends Layer {
	
	public ReliableBroadcastLayer() {
		// Events that the protocol will create
		evProvide = new Class[] {
		};
		
		// Events that the protocol requires to work
		// This is a subset of the accepted events
		evRequire = new Class[] {
			ProcessListInit.class,
			ProcessConnected.class,
			ReliableBroadcast.class
		};
		
		// Events that the protocol will accept
		evAccept = new Class[] {
			ProcessListInit.class,
			ProcessConnected.class,
			FailureDetected.class,
			ReliableBroadcast.class
		};
	}

	@Override
	public Session createSession() {
		return new ReliableBroadcastSession(this);
	}
}
