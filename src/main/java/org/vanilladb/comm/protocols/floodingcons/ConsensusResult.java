package org.vanilladb.comm.protocols.floodingcons;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Session;

public class ConsensusResult extends Event {
	
	private Value decision;
	
	public ConsensusResult(Channel channel, Session src, Value decision)
			  throws AppiaEventException {
		super(channel, Direction.UP, src);
		this.decision = decision;
	}
	
	public Value getDecision() {
		return decision;
	}
	
}
