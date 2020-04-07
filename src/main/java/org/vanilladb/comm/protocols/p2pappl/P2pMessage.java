package org.vanilladb.comm.protocols.p2pappl;

import java.io.Serializable;

import net.sf.appia.core.events.SendableEvent;

/**
 * A point to point message.
 * 
 * @author yslin
 */
public class P2pMessage extends SendableEvent {
	
	private boolean isInitailized;
	
	private int receiverId;
	private Serializable message;
	
	// We must provide a public constructor for TcpCompleteSession
	// in order to reconstruct this on the other side
	public P2pMessage() {
		super();
		this.isInitailized = false;
	}
	
	public P2pMessage(Serializable message, int receiverId) {
		super();
		this.receiverId = receiverId;
		this.message = message;
		this.isInitailized = true;
		
		// Push the data to the message buffer in order to send
		// through network
		getMessage().pushInt(receiverId);
		getMessage().pushObject(message);
	}
	
	public Serializable getCarriedMessage() {
		if (!isInitailized)
			recoverData();
		return message;
	}
	
	public int getReceiverId() {
		if (!isInitailized)
			recoverData();
		return receiverId;
	}
	
	private void recoverData() {
		// The data must be recovered from the message buffer
		// after it is sent through the network.
		message = (Serializable) getMessage().popObject();
		receiverId = getMessage().popInt();
		isInitailized = true;
	}
}
