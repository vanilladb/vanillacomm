package org.vanilladb.comm.protocols.zabproposal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.vanilladb.comm.protocols.beb.Broadcast;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Session;

public class ZabPropose extends Broadcast {
	
	private boolean isInitailized;
	
	private int epochId;
	private int proposalSerialNumber;
	private int messageSerialNumberStart;
	private List<Serializable> messages;
	
	// We must provide a public constructor for TcpCompleteSession
	// in order to reconstruct this on the other side
	public ZabPropose() {
		super();
		this.isInitailized = false;
	}
	
	public ZabPropose(Channel channel, Session source, int epochId,
			int proposalSerialNumber, int messageSerialNumberStart, List<Serializable> messages)
			throws AppiaEventException {
		super(channel, Direction.DOWN, source);
		this.epochId = epochId;
		this.proposalSerialNumber = proposalSerialNumber;
		this.messageSerialNumberStart = messageSerialNumberStart;
		this.messages = messages;
		this.isInitailized = true;
		
		// Push the data to the message buffer in order to send
		// through network
		getMessage().pushInt(epochId);
		getMessage().pushInt(proposalSerialNumber);
		getMessage().pushInt(messageSerialNumberStart);
		for (int i = messages.size() - 1; i >= 0; i--) 
			getMessage().pushObject(messages.get(i));
		getMessage().pushInt(messages.size());
	}
	
	public int getEpochId() {
		if (!isInitailized)
			recoverData();
		return epochId;
	}
	
	public int getProposalSerialNumber() {
		if (!isInitailized)
			recoverData();
		return proposalSerialNumber;
	}
	
	public int getMessageSerialNumberStart() {
		if (!isInitailized)
			recoverData();
		return messageSerialNumberStart;
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
		messageSerialNumberStart = getMessage().popInt();
		proposalSerialNumber = getMessage().popInt();
		epochId = getMessage().popInt();
		isInitailized = true;
	}
}
