package org.vanilladb.comm.protocols.floodingcons;

import org.vanilladb.comm.protocols.events.ProcessListInit;
import org.vanilladb.comm.protocols.tcpfd.FailureDetected;
import org.vanilladb.comm.protocols.tcpfd.ProcessConnected;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;

/**
 * Flooding consensus.
 * 
 * @author alexp, yslin
 */
public class FloodingConsensusLayer extends Layer {
	
	public FloodingConsensusLayer() {
		// Events that the protocol will create
		evProvide = new Class[] {
			Propose.class,
			Decide.class,
			ConsensusResult.class
		};
		
		// Events that the protocol requires to work
		// This is a subset of the accepted events
		evRequire = new Class[] {
			ProcessListInit.class,
			ProcessConnected.class,
			ConsensusRequest.class
		};
		
		// Events that the protocol will accept
		evAccept = new Class[] {
			ProcessListInit.class,
			ProcessConnected.class,
			FailureDetected.class,
			ConsensusRequest.class,
			Propose.class,
			Decide.class
		};
	}

	@Override
	public Session createSession() {
		return new FloodingConsensusSession(this);
	}
}
