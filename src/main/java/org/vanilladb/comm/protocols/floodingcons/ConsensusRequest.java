package org.vanilladb.comm.protocols.floodingcons;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Session;

public class ConsensusRequest extends Event {
	
	private Value value;
	
	public ConsensusRequest(Channel channel, Session src, Value value)
			  throws AppiaEventException {
		super(channel, Direction.DOWN, src);
		this.value = value;
	}
	
	public Value getValue() {
		return value;
	}
	
}
