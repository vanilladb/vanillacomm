package org.vanilladb.comm.protocols.zabacceptance;

import org.vanilladb.comm.protocols.zabproposal.ZabProposal;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Session;

public class ZabCacheProposal extends Event {
	
	private ZabProposal proposal;
	
	public ZabCacheProposal(Channel channel, Session source, ZabProposal proposal)
			throws AppiaEventException {
		super(channel, Direction.UP, source);
		this.proposal = proposal;
	}
	
	public ZabProposal getProposal() {
		return proposal;
	}
}
