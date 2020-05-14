package org.vanilladb.comm.protocols.zabproposal;

import java.io.Serializable;

public class ZabProposalId implements Serializable {
	
	private static final long serialVersionUID = 20200501002L;
	
	private int epochId;
	private long serialNumber;
	
	public ZabProposalId(int epochId, long serialNumber) {
		this.epochId = epochId;
		this.serialNumber = serialNumber;
	}
	
	public int getEpochId() {
		return epochId;
	}
	
	public long getSerialNumber() {
		return serialNumber;
	}
}
