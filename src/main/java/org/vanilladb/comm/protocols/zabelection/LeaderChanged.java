package org.vanilladb.comm.protocols.zabelection;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Session;

public class LeaderChanged extends Event {
	
	private int newLeaderId;
	private int newEpochId;
	
	public LeaderChanged(Channel channel, Session src, int newLeaderId, int newEpochId)
			throws AppiaEventException {
		super(channel, Direction.UP, src);
		this.newLeaderId = newLeaderId;
		this.newEpochId = newEpochId;
	}
	
	public int getNewLeaderId() {
		return newLeaderId;
	}
	
	public int getNewEpochId() {
		return newEpochId;
	}
}
