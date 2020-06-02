package org.vanilladb.comm.protocols.tcpfd;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	private Map<Integer, List<Channel>> processIdsToChannels = new HashMap<Integer, List<Channel>>();
	private Channel largerChannel; // The channel which assigns a larger process list
	
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
			logger.fine("Received ProcessListInit from Channel "
					+ event.getChannel().getChannelID());
		
		// Save the list (if it is larger)
		ProcessList list = event.copyProcessList();
		if (this.processList == null || list.getSize() > this.processList.getSize()) {
			this.processList = list;
			this.largerChannel = event.getChannel();
		}
		
		// Let the event continue
		try {
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
		
		// Add the channel to the notify list
		for (int pid = 0; pid < list.getSize(); pid++) {
			List<Channel> channels = processIdsToChannels.get(pid);
			if (channels == null) {
				channels = new ArrayList<Channel>();
				channels.add(event.getChannel());
				processIdsToChannels.put(pid, channels);
				
				// Set the start time
				long startTime = System.currentTimeMillis();
				lastReceived.put(pid, startTime);
			} else {
				channels.add(event.getChannel());
			}
		}
			
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
				sendHeartbeatSignal();
				
				// Retry later, in case that other processes are still initializing 
				scheduleNextHeartbeat();
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
		
		// Record when the heartbeat is received
		CommProcess process = processList.getProcess((SocketAddress) event.source);
		lastReceived.put(process.getId(), System.currentTimeMillis());
		
		// If the process has not been initialized
		if (!process.isInitialized()) {
			// Set the state as CORRECT
			process.setState(ProcessState.CORRECT);
			for (Channel channel : processIdsToChannels.get(process.getId())) {
				try {
					ProcessConnected conn = new ProcessConnected(channel, this, process.getId());
					conn.init();
					conn.go();
				} catch (AppiaEventException e) {
					e.printStackTrace();
				}
			}
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
				detectFailedProcess(processId);
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
						processList.getProcess(id).isCorrect()) {
					long lastTime = lastReceived.get(id);
					if (System.currentTimeMillis() - lastTime > HEARTBEAT_TIMEOUT) {
						if (logger.isLoggable(Level.SEVERE)) {
							CommProcess process = processList.getProcess(id);
							logger.severe("Detected failed processs " +
									process.getAddress() + " due to heartbeat timeout.");
						}
						detectFailedProcess(id);
					}
				}
			}
			
			// Send more heartbeats
			sendHeartbeatSignal();
			scheduleNextHeartbeat();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		} catch (AppiaException e) {
			e.printStackTrace();
		}
	}
	
	private void sendHeartbeatSignal() throws AppiaEventException {
		for (int id = 0; id < processList.getSize(); id++) {
			if (id != processList.getSelfId()) {
				Heartbeat heartbeat = new Heartbeat(largerChannel, this);
				heartbeat.source = processList.getSelfProcess().getAddress();
				heartbeat.dest = processList.getProcess(id).getAddress();
				heartbeat.init();
				heartbeat.go();
			}
		}
	}
	
	private void scheduleNextHeartbeat() throws AppiaEventException, AppiaException {
		NextHeartbeat next = new NextHeartbeat(HEARTBEAT_PERIOD, "NextHeartbeat",
				largerChannel, this);
		next.init();
		next.go();
	}
	
	private void detectFailedProcess(int failedId) throws AppiaEventException {
		// Mark failed
		processList.getProcess(failedId).setState(ProcessState.FAILED);
		
		// Notify upper layers of the failed process
		for (Channel channel : processIdsToChannels.get(failedId)) {
			FailureDetected fd = new FailureDetected(channel, this, failedId);
			fd.init();
			fd.go();
		}
	}
}
