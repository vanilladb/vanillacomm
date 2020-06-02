package org.vanilladb.comm.protocols.tcpfd;

import org.vanilladb.comm.protocols.events.ProcessListInit;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.protocols.common.RegisterSocketEvent;
import net.sf.appia.protocols.tcpcomplete.TcpUndeliveredEvent;

public class TcpFailureDetectionLayer extends Layer {
	
	public TcpFailureDetectionLayer() {
		// Events that the protocol will create
		evProvide = new Class[] {
			Heartbeat.class,
			NextHeartbeat.class,
			FailureDetected.class,
			ProcessConnected.class,
		};
		
		// Events that the protocol requires to work
		// This is a subset of the accepted events
		evRequire = new Class[] {
			ProcessListInit.class,
			RegisterSocketEvent.class,
			TcpUndeliveredEvent.class,
		};
		
		// Events that the protocol will accept
		evAccept = new Class[] {
			ProcessListInit.class,
			RegisterSocketEvent.class,
			Heartbeat.class,
			NextHeartbeat.class,
			TcpUndeliveredEvent.class,
		};
	}

	@Override
	public Session createSession() {
		return new TcpFailureDetectionSession(this);
	}
}

