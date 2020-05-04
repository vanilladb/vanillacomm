package org.vanilladb.comm.protocols.totalorderappl;

import java.io.Serializable;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Session;

public class TotalOrderMessages extends Event {
	
	private Serializable[] messages;
	private long messageSerialNumberStart;
	
	public TotalOrderMessages(Channel channel, Session src, Serializable[] messages,
			long messageSerialNumberStart) throws AppiaEventException {
		super(channel, Direction.UP, src);
		this.messages = messages;
		this.messageSerialNumberStart = messageSerialNumberStart;
	}
	
	public Serializable[] getMessages() {
		return messages;
	}
	
	public long getMessageSerialNumberStart() {
		return messageSerialNumberStart;
	}
}
