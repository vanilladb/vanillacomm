package org.vanilladb.comm.protocols.tob;

import org.vanilladb.comm.protocols.events.ProcessListInit;
import org.vanilladb.comm.protocols.floodingcons.ConsensusRequest;
import org.vanilladb.comm.protocols.floodingcons.ConsensusResult;
import org.vanilladb.comm.protocols.tcpfd.AllProcessesReady;
import org.vanilladb.comm.protocols.tcpfd.FailureDetected;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;

/**
 * Total Order Broadcast.
 * 
 * @author yslin
 */
public class TotalOrderBroadcastLayer extends Layer {
	
	public TotalOrderBroadcastLayer() {
		// Events that the protocol will create
		evProvide = new Class[] {
			ConsensusRequest.class,
		};
		
		// Events that the protocol requires to work
		// This is a subset of the accepted events
		evRequire = new Class[] {
			ProcessListInit.class,
			AllProcessesReady.class,
			TotalOrderBroadcast.class
		};
		
		// Events that the protocol will accept
		evAccept = new Class[] {
			ProcessListInit.class,
			AllProcessesReady.class,
			FailureDetected.class,
			TotalOrderBroadcast.class,
			ConsensusResult.class
		};
	}

	@Override
	public Session createSession() {
		return new TotalOrderBroadcastSession(this);
	}
}
