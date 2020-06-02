package org.vanilladb.comm.protocols.zabacceptance;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;

public class ZabDeny extends SendableEvent {
	
	// We must provide a public constructor for TcpCompleteSession
	// in order to reconstruct this on the other side
	public ZabDeny() {
		super();
	}
	
	public ZabDeny(Channel channel, int dir, Session source)
			throws AppiaEventException {
		super(channel, dir, source);
	}
}
