package org.vanilladb.comm.protocols.totalorderappl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import net.sf.appia.core.events.SendableEvent;

public class TotalOrderRequest extends SendableEvent {
	
	private boolean isInitailized;
	
	private List<Serializable> messages;
	
	// We must provide a public constructor for TcpCompleteSession
	// in order to reconstruct this on the other side
	public TotalOrderRequest() {
		super();
		this.isInitailized = false;
	}
	
	public TotalOrderRequest(List<Serializable> messages) {
		super();
		this.messages = messages;
		this.isInitailized = true;
		
		// Push the data to the message buffer in order to send
		// through network
		for (int i = messages.size() - 1; i >= 0; i--) 
			getMessage().pushObject(messages.get(i));
		getMessage().pushInt(messages.size());
	}
	
	public List<Serializable> getCarriedMessages() {
		if (!isInitailized)
			recoverData();
		return messages;
	}
	
	private void recoverData() {
		// The data must be recovered from the message buffer
		// after it is sent through the network.
		int messageCount = getMessage().popInt();
		messages = new ArrayList<Serializable>(messageCount);
		for (int i = 0; i < messageCount; i++)
			messages.add((Serializable) getMessage().popObject());
		isInitailized = true;
	}
}
