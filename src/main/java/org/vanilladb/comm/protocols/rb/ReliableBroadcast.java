package org.vanilladb.comm.protocols.rb;

import org.vanilladb.comm.protocols.beb.Broadcast;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Session;

public class ReliableBroadcast extends Broadcast {
	
	// We must provide a public constructor for TcpCompleteSession
	// in order to reconstruct this on the other side
	public ReliableBroadcast() {
		super();
	}
	
	public ReliableBroadcast(Channel channel, int direction, Session source)
			throws AppiaEventException {
		super(channel, direction, source);
	}

}
