package org.vanilladb.comm.protocols.tob;

import java.util.HashSet;
import java.util.Set;

import org.vanilladb.comm.protocols.floodingcons.Value;
import org.vanilladb.comm.protocols.rb.MessageId;

public class OrderProposal implements Value {
	
	private static final long serialVersionUID = 20200407001L;
	
	private Set<MessageId> messageIds;
	
	public OrderProposal(Set<MessageId> messageIds) {
		// Defensive copy
		this.messageIds = new HashSet<MessageId>(messageIds);
	}
	
	public Set<MessageId> getMessageIds() {
		return messageIds;
	}
	
	@Override
	public int compareTo(Value target) {
		return hashCode() - target.hashCode();
	}
	
	@Override
	public int hashCode() {
		return this.messageIds.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		
		if (!(obj instanceof OrderProposal))
			return false;
		
		OrderProposal target = (OrderProposal) obj;
		return this.messageIds.equals(target.messageIds);
	}
}
