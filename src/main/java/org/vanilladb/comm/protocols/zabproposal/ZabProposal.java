package org.vanilladb.comm.protocols.zabproposal;

import java.io.Serializable;

/**
 * A Zab proposal
 */
public class ZabProposal implements Serializable {
	
	private static final long serialVersionUID = 20200501001L;
	
	private ZabProposalId proposalId;
	private long messageStartId;
	private Serializable[] messages;
	
	public ZabProposal(ZabProposalId proposalId, long messageStartId, Serializable[] messages) {
		this.proposalId = proposalId;
		this.messageStartId = messageStartId;
		this.messages = messages;
	}
	
	public ZabProposalId getId() {
		return proposalId;
	}
	
	public long getMessageStartId() {
		return messageStartId;
	}
	
	public Serializable[] getMessages() {
		return messages;
	}
}
