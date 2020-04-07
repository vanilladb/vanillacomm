package org.vanilladb.comm.protocols.totalorderappl;

import java.io.Serializable;

import net.sf.appia.core.events.SendableEvent;

public class TotalOrderRequest extends SendableEvent {
	
	private boolean isInitailized;
	
	private Serializable message;
	
	// We must provide a public constructor for TcpCompleteSession
	// in order to reconstruct this on the other side
	public TotalOrderRequest() {
		super();
		this.isInitailized = false;
	}
	
	public TotalOrderRequest(Serializable message) {
		super();
		this.message = message;
		this.isInitailized = true;
		
		// Push the data to the message buffer in order to send
		// through network
		getMessage().pushObject(message);
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
		isInitailized = true;
	}
}
