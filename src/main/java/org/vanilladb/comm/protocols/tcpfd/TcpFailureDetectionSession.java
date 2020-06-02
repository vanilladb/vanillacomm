package org.vanilladb.comm.protocols.tcpfd;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vanilladb.comm.process.CommProcess;
import org.vanilladb.comm.process.ProcessList;
import org.vanilladb.comm.process.ProcessState;
import org.vanilladb.comm.protocols.events.ProcessListInit;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.protocols.common.RegisterSocketEvent;
import net.sf.appia.protocols.tcpcomplete.TcpUndeliveredEvent;

public class TcpFailureDetectionSession extends Session {
	private static Logger logger = Logger.getLogger(TcpFailureDetectionSession.class.getName());
	
	private static final int HEARTBEAT_PERIOD = 10_000; // in milliseconds
	private static final int HEARTBEAT_TIMEOUT = 30_000; // in milliseconds
	
	private ProcessList processList;
	private Map<Integer, Long> lastReceived = new HashMap<Integer, Long>();
	private boolean allReadyIsSent = false;
	
	TcpFailureDetectionSession(Layer layer) {
		super(layer);
	}
	
	@Override
	public void handle(Event event) {
		if (event instanceof ProcessListInit)
			handleProcessListInit((ProcessListInit) event);
		else if (event instanceof RegisterSocketEvent)
			handleRegisterSocket((RegisterSocketEvent) event);
		else if (event instanceof Heartbeat)
			handleHeartbeat((Heartbeat) event);
		else if (event instanceof TcpUndeliveredEvent)
			handleTcpUndelivered((TcpUndeliveredEvent) event);
		else if (event instanceof NextHeartbeat)
			handleNextHeartbeat((NextHeartbeat) event);
	}
	
	private void handleProcessListInit(ProcessListInit event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received ProcessListInit");
		
		// Save the list
		this.processList = event.copyProcessList();
		
		// Let the event continue
		try {
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
		
		// Set the start time
		long startTime = System.currentTimeMillis();
		for (int id = 0; id < processList.getSize(); id++)
			lastReceived.put(id, startTime);
	}
	
	private void handleRegisterSocket(RegisterSocketEvent event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received RegisterSocketEvent");
		
		try {
			// Let the event keep going
			event.go();
			
			if (event.getDir() == Direction.UP && !event.error) {
				if (logger.isLoggable(Level.FINE))
					logger.fine("Sending heartbeats to all other nodes");
				
				// Send heartbeats
				sendHeartbeatSignal(event.getChannel());
				
				// Retry later, in case that other processes are still initializing 
				scheduleNextHeartbeat(event.getChannel());
			}
		} catch (AppiaEventException e) {
			e.printStackTrace();
		} catch (AppiaException e) {
			e.printStackTrace();
		}
	}
	
	private void handleHeartbeat(Heartbeat event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received Heartbeat from " + event.source);
		
		// Set the state as CORRECT
		CommProcess process = processList.getProcess((SocketAddress) event.source);
		process.setState(ProcessState.CORRECT);
		lastReceived.put(process.getId(), System.currentTimeMillis());
		
		// Check if all processes are correct
		if (!allReadyIsSent && processList.areAllCorrect()) {
			try {
				AllProcessesReady ready = new AllProcessesReady(event.getChannel(),
						Direction.UP, this);
				ready.init();
				ready.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
			allReadyIsSent = true;
		}
	}
	
	private void handleTcpUndelivered(TcpUndeliveredEvent event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received TcpUndelivered for " + event.getFailedAddress());
		
		try {
			int processId = processList.getId((SocketAddress) event.getFailedAddress());
			
			// Not on the list, which means not my concern
			if (processId == -1)
				return;
			
			if (!processList.getProcess(processId).isInitialized()) {
				if (logger.isLoggable(Level.SEVERE))
					logger.severe("Cannot deliver messages to processs " + event.getFailedAddress()
							+ ". Retry later.");
			} else if (processList.getProcess(processId).isCorrect()) {
				if (logger.isLoggable(Level.SEVERE))
					logger.severe("Detected failed processs " + event.getFailedAddress() + " due to TCP undelivered.");
				detectFailedProcess(event.getChannel(), processId);
			}
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
	
	private void handleNextHeartbeat(NextHeartbeat event) {
		try {
			// Check if there is any heartbeat that is not received
			for (int id = 0; id < processList.getSize(); id++) {
				if (id != processList.getSelfId() &&
						processList.getProcess(id).isInitialized()) {
					long lastTime = lastReceived.get(id);
					if (System.currentTimeMillis() - lastTime > HEARTBEAT_TIMEOUT) {
						if (logger.isLoggable(Level.SEVERE)) {
							CommProcess process = processList.getProcess(id);
							logger.severe("Detected failed processs " +
									process.getAddress() + " due to heartbeat timeout.");
						}
						detectFailedProcess(event.getChannel(), id);
					}
				}
			}
			
			// Send more heartbeats
			sendHeartbeatSignal(event.getChannel());
			scheduleNextHeartbeat(event.getChannel());
		} catch (AppiaEventException e) {
			e.printStackTrace();
		} catch (AppiaException e) {
			e.printStackTrace();
		}
	}
	
	private void sendHeartbeatSignal(Channel channel) throws AppiaEventException {
		for (int id = 0; id < processList.getSize(); id++) {
			if (id != processList.getSelfId()) {
				Heartbeat heartbeat = new Heartbeat(channel, this);
				heartbeat.source = processList.getSelfProcess().getAddress();
				heartbeat.dest = processList.getProcess(id).getAddress();
				heartbeat.init();
				heartbeat.go();
			}
		}
	}
	
	private void scheduleNextHeartbeat(Channel channel) throws AppiaEventException, AppiaException {
		NextHeartbeat next = new NextHeartbeat(HEARTBEAT_PERIOD, "NextHeartbeat",
				channel, this);
		next.init();
		next.go();
	}
	
	private void detectFailedProcess(Channel channel, int failedId) throws AppiaEventException {
		// Mark failed
		processList.getProcess(failedId).setState(ProcessState.FAILED);
		
		// Notify upper layers of the failed process
		FailureDetected fd = new FailureDetected(channel, this, failedId);
		fd.init();
		fd.go();
	}
}
