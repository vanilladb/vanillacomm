package org.vanilladb.comm.protocols.urb;

import org.vanilladb.comm.protocols.beb.Broadcast;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Session;

public class UniformReliableBroadcast extends Broadcast {
	
	// We must provide a public constructor for TcpCompleteSession
	// in order to reconstruct this on the other side
	public UniformReliableBroadcast() {
		super();
	}
	
	public UniformReliableBroadcast(Channel channel, int direction, Session source)
			throws AppiaEventException {
		super(channel, direction, source);
	}

}
