package org.vanilladb.comm.protocols.p2pcounting;

import org.vanilladb.comm.protocols.events.ProcessListInit;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;

public class P2pCountingLayer extends Layer {
	
	public P2pCountingLayer() {
		// Events that the protocol will create
		evProvide = new Class[] {
		};
		
		// Events that the protocol requires to work
		// This is a subset of the accepted events
		evRequire = new Class[] {
			ProcessListInit.class,
			SendableEvent.class
		};
		
		// Events that the protocol will accept
		evAccept = new Class[] {
			ProcessListInit.class,
			SendableEvent.class
		};
	}

	@Override
	public Session createSession() {
		return new P2pCountingSession(this);
	}
}
