package org.vanilladb.comm.protocols.totalorderappl;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vanilladb.comm.process.ProcessList;
import org.vanilladb.comm.process.ProcessState;
import org.vanilladb.comm.process.ProcessStateListener;
import org.vanilladb.comm.protocols.events.ProcessListInit;
import org.vanilladb.comm.protocols.tcpfd.FailureDetected;
import org.vanilladb.comm.protocols.tcpfd.ProcessConnected;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.protocols.common.RegisterSocketEvent;

public class TotalOrderApplicationSession extends Session {
	private static Logger logger = Logger.getLogger(TotalOrderApplicationSession.class.getName());
	
	private ProcessStateListener procListener;
	private TotalOrderMessageListener totalMsgListener;
	private ProcessList processList;
	private boolean willRegisterSocket;
	
	TotalOrderApplicationSession(Layer layer,
			ProcessStateListener procListener,
			TotalOrderMessageListener totalMsgListener,
			ProcessList processList, boolean willRegisterSocket) {
		super(layer);
		
		this.procListener = procListener;
		this.totalMsgListener = totalMsgListener;
		this.processList = processList;
		this.willRegisterSocket = willRegisterSocket;
	}
	
	@Override
	public void handle(Event event) {
		if (event instanceof ChannelInit)
			handleChannelInit((ChannelInit) event);
		else if (event instanceof ProcessConnected)
			handleProcessConnected((ProcessConnected) event);
		else if (event instanceof RegisterSocketEvent)
			handleRegisterSocketEvent((RegisterSocketEvent) event);
		else if (event instanceof FailureDetected)
			handleFailureDetected((FailureDetected) event);
		else if (event instanceof TotalOrderMessages)
			handleTotalOrderMessage((TotalOrderMessages) event);
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
	
	private void handleProcessConnected(ProcessConnected event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received ProcessConnected");
		
		// Let the event continue
		try {
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
		
		// Set the connected process ready
		processList.getProcess(event.getConnectedProcessId())
				.setState(ProcessState.CORRECT);
		
		// Notify the listener when all the processes are ready
		if (processList.areAllCorrect())
			procListener.onAllProcessesReady();
	}
	
	private void handleRegisterSocketEvent(RegisterSocketEvent event) {
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
	
	private void handleFailureDetected(FailureDetected event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received FailureDetected (failed id = " +
					event.getFailedProcessId() + ")");
		
		// Set the process state as failed
		processList.getProcess(event.getFailedProcessId()).setState(ProcessState.FAILED);
		
		// Notify the listener
		procListener.onProcessFailed(event.getFailedProcessId());
	}
	
	private void handleTotalOrderMessage(TotalOrderMessages event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine(String.format("Received TotalOrderMessage (serial number start at: %d, length: %d)",
					event.getMessageSerialNumberStart(), event.getMessages().length));
			
		// Notify the listener
		long startId = event.getMessageSerialNumberStart();
		Serializable[] messages = event.getMessages();
		for (int id = 0; id < messages.length; id++) {
			totalMsgListener.onRecvTotalOrderMessage(startId + id, messages[id]);
		}
	}
}
