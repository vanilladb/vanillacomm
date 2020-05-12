package org.vanilladb.comm.protocols.zabelection;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Session;

public class LeaderInit extends Event {
	
	private int leaderId;
	
	public LeaderInit(Channel channel, Session src, int leaderId)
			throws AppiaEventException {
		super(channel, Direction.UP, src);
		this.leaderId = leaderId;
	}
	
	public int getLeaderId() {
		return leaderId;
	}
}
