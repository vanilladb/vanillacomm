package org.vanilladb.comm.protocols.floodingcons;

import org.vanilladb.comm.protocols.beb.Broadcast;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Session;

public class Decide extends Broadcast {
	
	private boolean isInitailized;
	
	private Value value;
	
	// We must provide a public constructor for TcpCompleteSession
	// in order to reconstruct this on the other side
	public Decide() {
		super();
		this.isInitailized = false;
	}
	
	public Decide(Channel channel, Session source, Value value)
			throws AppiaEventException {
		super(channel, Direction.DOWN, source);
		this.value = value;
		this.isInitailized = true;
		
		// Push the data to the message buffer in order to send
		// through network
		getMessage().pushObject(value);
	}
	
	public Value getValue() {
		if (!isInitailized)
			recoverData();
		return value;
	}
	
	private void recoverData() {
		// The data must be recovered from the message buffer
		// after it is sent through the network.
		value = (Value) getMessage().popObject();
		isInitailized = true;
	}
}
