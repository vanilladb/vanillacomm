package org.vanilladb.comm.protocols.zabacceptance;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;

public class ZabAccept extends SendableEvent {
	
	private boolean isInitailized;
	
	private int epochId;
	private int serialNumber;
	
	// We must provide a public constructor for TcpCompleteSession
	// in order to reconstruct this on the other side
	public ZabAccept() {
		super();
		this.isInitailized = false;
	}
	
	public ZabAccept(Channel channel, Session source, int epochId,
			int serialNumber)
			throws AppiaEventException {
		super(channel, Direction.DOWN, source);
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
