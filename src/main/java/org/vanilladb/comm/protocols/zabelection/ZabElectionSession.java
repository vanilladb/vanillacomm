package org.vanilladb.comm.protocols.zabelection;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.vanilladb.comm.process.ProcessList;
import org.vanilladb.comm.process.ProcessState;
import org.vanilladb.comm.protocols.events.ProcessListInit;
import org.vanilladb.comm.protocols.tcpfd.FailureDetected;
import org.vanilladb.comm.protocols.tcpfd.ProcessConnected;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;

public class ZabElectionSession extends Session {
	private static Logger logger = Logger.getLogger(ZabElectionSession.class.getName());
	
	private ProcessList processList;
	private int leaderId;
	private int epochId = 0;
	
	ZabElectionSession(Layer layer) {
		super(layer);
	}
	
	@Override
	public void handle(Event event) {
		if (event instanceof ProcessListInit)
			handleProcessListInit((ProcessListInit) event);
		else if (event instanceof ProcessConnected)
			handleProcessConnected((ProcessConnected) event);
		else if (event instanceof FailureDetected)
			handleFailureDetected((FailureDetected) event);
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
		
		// Initialize when all processes are ready
		if (processList.areAllCorrect())
			setFirstLeader(event.getChannel());
	}
	
	private void handleFailureDetected(FailureDetected event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received FailureDetected (failed id = " +
					event.getFailedProcessId() + ")");
		
		// Set the process state as failed
		processList.getProcess(event.getFailedProcessId()).setState(ProcessState.FAILED);
		
		// Let the event continue
		try {
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
		
		// If it is the leader, choose a new one.
		if (leaderId == event.getFailedProcessId()) {
			electNewLeader();
			
			try {
				LeaderChanged change = new LeaderChanged(event.getChannel(), this,
						leaderId, epochId);
				change.init();
				change.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void setFirstLeader(Channel channel) {
		// Set the first leader
		leaderId = processList.getSize() - 1;
		
		if (logger.isLoggable(Level.FINE))
			logger.fine("Initialize with leaderId = " + leaderId);
		
		// Send a leader init event
		try {
			LeaderInit init = new LeaderInit(channel, this, leaderId);
			init.init();
			init.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
	
	private void electNewLeader() {
		// A deterministic algorithm
		// Only works when there are no two processes failed at the same time
		for (int i = processList.getSize() - 1; i >= 0; i--) {
			if (processList.getProcess(i).isCorrect()) {
				leaderId = i;
				break;
			}
		}
		
		epochId++;

		if (logger.isLoggable(Level.INFO))
			logger.info("Elected a new leader: Process " + leaderId +
					" (epoch: " + epochId + ")");
	}
}
