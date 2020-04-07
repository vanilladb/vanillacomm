package org.vanilladb.comm.protocols.zabproposal;

import java.io.Serializable;

import org.vanilladb.comm.protocols.beb.Broadcast;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Session;

public class ZabPropose extends Broadcast {
	
	private boolean isInitailized;
	
	private int epochId;
	private int serialNumber;
	private Serializable message;
	
	// We must provide a public constructor for TcpCompleteSession
	// in order to reconstruct this on the other side
	public ZabPropose() {
		super();
		this.isInitailized = false;
	}
	
	public ZabPropose(Channel channel, Session source, int epochId,
			int serialNumber, Serializable message)
			throws AppiaEventException {
		super(channel, Direction.DOWN, source);
		this.epochId = epochId;
		this.serialNumber = serialNumber;
		this.message = message;
		this.isInitailized = true;
		
		// Push the data to the message buffer in order to send
		// through network
		getMessage().pushInt(epochId);
		getMessage().pushInt(serialNumber);
		getMessage().pushObject(message);
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
	
	public Serializable getCarriedMessage() {
		if (!isInitailized)
			recoverData();
		return message;
	}
	
	private void recoverData() {
		// The data must be recovered from the message buffer
		// after it is sent through the network.
		message = (Serializable) getMessage().popObject();
		serialNumber = getMessage().popInt();
		epochId = getMessage().popInt();
		isInitailized = true;
	}
}
