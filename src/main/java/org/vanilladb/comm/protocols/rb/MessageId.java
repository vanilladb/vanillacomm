package org.vanilladb.comm.protocols.rb;

import java.io.Serializable;

public class MessageId implements Serializable, Comparable<MessageId> {
	
	private static final long serialVersionUID = 20200406001L;
	
	private int sourceProcessId;
	private int sequenceNumber;
	
	public MessageId(int sourceProcessId, int sequenceNumber) {
		this.sourceProcessId = sourceProcessId;
		this.sequenceNumber = sequenceNumber;
	}
	
	public int getSourceProcessId() {
		return sourceProcessId;
	}
	
	public int getSequenceNumber() {
		return sequenceNumber;
	}

	@Override
	public int compareTo(MessageId id) {
		int result = sourceProcessId - id.sourceProcessId;
		if (result != 0)
			return result;
		
		return sequenceNumber - id.sourceProcessId;
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + sourceProcessId;
		result = 31 * result + sequenceNumber;
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		
		if (!(obj instanceof MessageId))
			return false;
		
		MessageId target = (MessageId) obj;
		return this.sourceProcessId == target.sourceProcessId &&
				this.sequenceNumber == target.sequenceNumber;
	}
}
