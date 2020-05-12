package org.vanilladb.comm.protocols.tcpfd;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Event;
import net.sf.appia.core.Session;

public class AllProcessesReady extends Event {
	
	public AllProcessesReady(Channel channel, int dir, Session src)
			throws AppiaEventException {
		super(channel, dir, src);
	}
	
}
