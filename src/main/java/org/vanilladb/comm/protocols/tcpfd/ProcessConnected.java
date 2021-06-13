package org.vanilladb.comm.protocols.tcpfd;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Session;

public class ProcessConnected extends Event {
	
	private int connectedProcessId;
	
	public ProcessConnected(Channel channel, Session src, int connectedProcessId)
			throws AppiaEventException {
		super(channel, Direction.UP, src);
		this.connectedProcessId = connectedProcessId;
	}
	
	public int getConnectedProcessId() {
		return connectedProcessId;
	}
}
