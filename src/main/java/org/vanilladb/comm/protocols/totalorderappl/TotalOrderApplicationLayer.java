package org.vanilladb.comm.protocols.totalorderappl;

import org.vanilladb.comm.process.ProcessList;
import org.vanilladb.comm.process.ProcessStateListener;
import org.vanilladb.comm.protocols.events.ProcessListInit;
import org.vanilladb.comm.protocols.tcpfd.FailureDetected;
import org.vanilladb.comm.protocols.tcpfd.ProcessConnected;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.protocols.common.RegisterSocketEvent;

public class TotalOrderApplicationLayer extends Layer {
	
	private ProcessStateListener procListener;
	private TotalOrderMessageListener totalMsgListener;
	private ProcessList processList;
	private boolean willRegisterSocket;
	
	public TotalOrderApplicationLayer(ProcessStateListener procListener,
			TotalOrderMessageListener totalMsgListener,
			ProcessList processList, boolean willRegisterSocket) {
		this.procListener = procListener;
		this.totalMsgListener = totalMsgListener;
		this.processList = processList;
		this.willRegisterSocket = willRegisterSocket;
		
		// Events that the protocol will create
		evProvide = new Class[] {
			TotalOrderRequest.class,
			ProcessListInit.class,
			RegisterSocketEvent.class
		};
		
		// Events that the protocol requires to work
		// This is a subset of the accepted events
		evRequire = new Class[] {
			ChannelInit.class,
			ProcessConnected.class,
		};
		
		// Events that the protocol will accept
		evAccept = new Class[] {
			ChannelInit.class,
			ProcessConnected.class,
			RegisterSocketEvent.class,
			FailureDetected.class,
			TotalOrderMessages.class
		};
	}

	@Override
	public Session createSession() {
		return new TotalOrderApplicationSession(this, procListener,
				totalMsgListener, processList, willRegisterSocket);
	}
}
