package org.vanilladb.comm.protocols.p2pappl;

import java.io.Serializable;

import net.sf.appia.core.Event;

/**
 * A point to point message.
 * 
 * @author yslin
 */
public class P2pMessage extends Event {
	
	private int receiverId;
	private Serializable message;
	
	public P2pMessage(Serializable message, int receiverId) {
		super();
		this.receiverId = receiverId;
		this.message = message;
	}
	
	public Serializable getMessage() {
		return message;
	}
	
	public int getReceiverId() {
		return receiverId;
	}
}
