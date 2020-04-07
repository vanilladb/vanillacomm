package org.vanilladb.comm.protocols.zabacceptance;

import org.vanilladb.comm.protocols.beb.Broadcast;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Session;

public class ZabCommit extends Broadcast {
	
	private boolean isInitailized;
	
	private int epochId;
	private int serialNumber;
	
	// We must provide a public constructor for TcpCompleteSession
	// in order to reconstruct this on the other side
	public ZabCommit() {
		super();
		this.isInitailized = false;
	}
	
	public ZabCommit(Channel channel, int direction, Session source,
			int epochId, int serialNumber) throws AppiaEventException {
		super(channel, direction, source);
		this.epochId = epochId;
		this.serialNumber = serialNumber;
		this.isInitailized = true;
		
		// Push the data to the message buffer in order to send
		// through network
		getMessage().pushInt(epochId);
		getMessage().pushInt(serialNumber);
	}
	
	public int getEpochId() {
		if (!isInitailized)
			recoverData();
		return epochId;
	}
	
	public int getSerialNumber() {
		if (!isInitailized)
			recoverData();
		return serialNumber;
	}
	
	private void recoverData() {
		// The data must be recovered from the message buffer
		// after it is sent through the network.
		serialNumber = getMessage().popInt();
		epochId = getMessage().popInt();
		isInitailized = true;
	}
}
