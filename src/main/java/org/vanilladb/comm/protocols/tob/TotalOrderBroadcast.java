package org.vanilladb.comm.protocols.tob;

import org.vanilladb.comm.protocols.rb.ReliableBroadcast;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Session;

public class TotalOrderBroadcast extends ReliableBroadcast {
	
	// We must provide a public constructor for TcpCompleteSession
	// in order to reconstruct this on the other side
	public TotalOrderBroadcast() {
		super();
	}
	
	public TotalOrderBroadcast(Channel channel, int direction, Session source)
			throws AppiaEventException {
		super(channel, direction, source);
	}

}
