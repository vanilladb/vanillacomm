package org.vanilladb.comm.protocols.p2pappl;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vanilladb.comm.process.ProcessList;
import org.vanilladb.comm.protocols.events.ProcessListInit;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.protocols.common.RegisterSocketEvent;
import net.sf.appia.protocols.tcpcomplete.TcpUndeliveredEvent;

public class P2pApplicationSession extends Session {
	private static Logger logger = Logger.getLogger(P2pApplicationSession.class.getName());
	
	private P2pMessageListener listener;
	private ProcessList processList;
	private boolean willRegisterSocket;
	
	P2pApplicationSession(Layer layer, P2pMessageListener listener,
			ProcessList processList, boolean willRegisterSocket) {
		super(layer);
		this.listener = listener;
		this.processList = processList;
		this.willRegisterSocket = willRegisterSocket;
	}
	
	@Override
	public void handle(Event event) {
		if (event instanceof ChannelInit)
			handleChannelInit((ChannelInit) event);
		else if (event instanceof RegisterSocketEvent)
			handleRegisterSocket((RegisterSocketEvent) event);
		else if (event instanceof P2pMessage)
			handleP2pMessage((P2pMessage) event);
		else if (event instanceof SendableEvent)
			handleSendableEvent((SendableEvent) event);
		else if (event instanceof TcpUndeliveredEvent)
			handleTcpUndelivered((TcpUndeliveredEvent) event);
	}
	
	private void handleChannelInit(ChannelInit init) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received ChannelInit");
		
		try {
			// ChannelInit must go() before inserting other events
			init.go();

			if (willRegisterSocket) {
				// Register socket for TCP connection
				RegisterSocketEvent rse = new RegisterSocketEvent(init.getChannel(),
						Direction.DOWN, this);
				rse.localHost = processList.getSelfProcess().getAddress().getAddress();
				rse.port = processList.getSelfProcess().getAddress().getPort();
				rse.init();
				rse.go();
				
				if (logger.isLoggable(Level.INFO))
					logger.info("Socket registration request sent.");
			}
			
			// Send process init
			ProcessListInit processInit = new ProcessListInit(init.getChannel(),
					this, new ProcessList(processList));
			processInit.init();
			processInit.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
	
	private void handleRegisterSocket(RegisterSocketEvent event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received RegisterSocket");
		
		if (event.error) {
			if (logger.isLoggable(Level.SEVERE))
				logger.severe(event.getErrorDescription());
			System.exit(2);
		} else {
			if (logger.isLoggable(Level.INFO))
				logger.info(String.format("Socket registration completed. (%s:%d)",
						event.localHost, event.port));
		}
	}
	
	private void handleP2pMessage(P2pMessage p2pMsg) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received P2pMessage");
		
		// Convert it to a SendableEvent
		try {
			SendableEvent event = new SendableEvent();
			event.source = processList.getSelfProcess().getAddress();
			event.dest = processList.getProcess(p2pMsg.getReceiverId()).getAddress();
			event.getMessage().pushObject(p2pMsg.getMessage());
			event.setChannel(p2pMsg.getChannel());
			event.setDir(Direction.DOWN);
			event.setSourceSession(this);
			event.init();
		event.go();
		} catch (AppiaEventException ex) {
			ex.printStackTrace();
		}
	}
	
	private void handleSendableEvent(SendableEvent event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received SendableEvent");
		
		try {
			if (event.getDir() == Direction.UP) {
				int senderId = processList.getId((SocketAddress) event.source);
				Serializable message = (Serializable) event.getMessage().popObject();
				listener.onRecvP2pMessage(senderId, message);
			} else {
				event.go();
			}
		} catch (AppiaEventException ex) {
			ex.printStackTrace();
		}
	}
	
	private void handleTcpUndelivered(TcpUndeliveredEvent event) {
		int processId = processList.getId((InetSocketAddress) event.getFailedAddress());
		
		if (logger.isLoggable(Level.SEVERE))
			logger.severe(String.format("Failed to deliver message to process no.%d (%s)",
					processId, event.getFailedAddress()));
	}
}
