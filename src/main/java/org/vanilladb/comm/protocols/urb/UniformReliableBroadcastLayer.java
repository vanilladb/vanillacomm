package org.vanilladb.comm.protocols.urb;

import org.vanilladb.comm.protocols.events.ProcessListInit;
import org.vanilladb.comm.protocols.tcpfd.FailureDetected;
import org.vanilladb.comm.protocols.tcpfd.ProcessConnected;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;

/**
 * All-Ack Uniform Reliable Broadcast.
 * 
 * @author nuno, DBN, yslin
 */
public class UniformReliableBroadcastLayer extends Layer {
	
	public UniformReliableBroadcastLayer() {
		// Events that the protocol will create
		evProvide = new Class[] {
		};
		
		// Events that the protocol requires to work
		// This is a subset of the accepted events
		evRequire = new Class[] {
			ProcessListInit.class,
			ProcessConnected.class,
			UniformReliableBroadcast.class
		};
		
		// Events that the protocol will accept
		evAccept = new Class[] {
			ProcessListInit.class,
			ProcessConnected.class,
			FailureDetected.class,
			UniformReliableBroadcast.class
		};
	}

	@Override
	public Session createSession() {
		return new UniformReliableBroadcastSession(this);
	}
}
