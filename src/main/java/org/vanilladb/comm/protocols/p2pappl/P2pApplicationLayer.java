package org.vanilladb.comm.protocols.p2pappl;

import org.vanilladb.comm.process.ProcessList;
import org.vanilladb.comm.protocols.events.ProcessListInit;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.protocols.common.RegisterSocketEvent;
import net.sf.appia.protocols.tcpcomplete.TcpUndeliveredEvent;

public class P2pApplicationLayer extends Layer {
	
	private P2pMessageListener listener;
	private ProcessList processList;
	private boolean willRegisterSocket;
	
	public P2pApplicationLayer(P2pMessageListener listener, ProcessList processList,
			boolean willRegisterSocket) {
		this.listener = listener;
		this.processList = processList;
		this.willRegisterSocket = willRegisterSocket;
		
		// Events that the protocol will create
		evProvide = new Class[] {
			RegisterSocketEvent.class,
			P2pMessage.class,
			SendableEvent.class,
			ProcessListInit.class
		};
		
		// Events that the protocol requires to work
		// This is a subset of the accepted events
		evRequire = new Class[] {
			ChannelInit.class
		};
		
		// Events that the protocol will accept
		evAccept = new Class[] {
			ChannelInit.class,
			RegisterSocketEvent.class,
			P2pMessage.class,
			SendableEvent.class,
			TcpUndeliveredEvent.class
		};
	}

	@Override
	public Session createSession() {
		return new P2pApplicationSession(this, listener, processList, willRegisterSocket);
	}
}
