package org.vanilladb.comm.protocols.totalorderappl;

import java.io.Serializable;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Session;

public class TotalOrderMessage extends Event {
	
	private Serializable message;
	private int serialNumber;
	
	public TotalOrderMessage(Channel channel, Session src, Serializable message,
			int serialNumber) throws AppiaEventException {
		super(channel, Direction.UP, src);
		this.message = message;
		this.serialNumber = serialNumber;
	}
	
	public Serializable getMessage() {
		return message;
	}
	
	public int getSerialNumber() {
		return serialNumber;
	}
}
