package org.vanilladb.comm.protocols.beb;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;

public class Broadcast extends SendableEvent {
	
	// We must provide a public constructor for TcpCompleteSession
	// in order to reconstruct this on the other side
	public Broadcast() {
		super();
	}
	
	public Broadcast(Channel channel, int direction, Session source)
			throws AppiaEventException {
		super(channel, direction, source);
	}
}
