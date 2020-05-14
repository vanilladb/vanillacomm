package org.vanilladb.comm.protocols.floodingcons;

import java.util.Set;

import org.vanilladb.comm.protocols.beb.Broadcast;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Session;

public class Propose extends Broadcast {
	
	private boolean isInitailized;
	
	private int roundId;
	private Set<Value> proposal;
	
	// We must provide a public constructor for TcpCompleteSession
	// in order to reconstruct this on the other side
	public Propose() {
		super();
		this.isInitailized = false;
	}
	
	public Propose(Channel channel, Session source,
			int roundId, Set<Value> proposal)
			throws AppiaEventException {
		super(channel, Direction.DOWN, source);
		this.roundId = roundId;
		this.proposal = proposal;
		this.isInitailized = true;
		
		// Push the data to the message buffer in order to send
		// through network
		getMessage().pushInt(roundId);
		getMessage().pushObject(proposal);
	}
	
	public int getRoundId() {
		if (!isInitailized)
			recoverData();
		return roundId;
	}
	
	public Set<Value> getProposal() {
		if (!isInitailized)
			recoverData();
		return proposal;
	}
	
	private void recoverData() {
		// The data must be recovered from the message buffer
		// after it is sent through the network.
		proposal = (Set<Value>) getMessage().popObject();
		roundId = getMessage().popInt();
		isInitailized = true;
	}
}
