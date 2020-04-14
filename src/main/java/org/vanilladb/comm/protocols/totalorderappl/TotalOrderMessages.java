package org.vanilladb.comm.protocols.totalorderappl;

import java.io.Serializable;
import java.util.List;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Session;

public class TotalOrderMessages extends Event {
	
	private List<Serializable> messages;
	private int messageSerialNumberStart;
	
	public TotalOrderMessages(Channel channel, Session src, List<Serializable> messages,
			int messageSerialNumberStart) throws AppiaEventException {
		super(channel, Direction.UP, src);
		this.messages = messages;
		this.messageSerialNumberStart = messageSerialNumberStart;
	}
	
	public List<Serializable> getMessages() {
		return messages;
	}
	
	public int getMessageSerialNumberStart() {
		return messageSerialNumberStart;
	}
}
