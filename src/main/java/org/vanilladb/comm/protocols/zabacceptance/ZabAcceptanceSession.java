package org.vanilladb.comm.protocols.zabacceptance;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.vanilladb.comm.process.ProcessList;
import org.vanilladb.comm.process.ProcessState;
import org.vanilladb.comm.protocols.events.ProcessListInit;
import org.vanilladb.comm.protocols.tcpfd.AllProcessesReady;
import org.vanilladb.comm.protocols.tcpfd.FailureDetected;
import org.vanilladb.comm.protocols.zabelection.LeaderChanged;
import org.vanilladb.comm.protocols.zabproposal.ZabPropose;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;

public class ZabAcceptanceSession extends Session {
	private static Logger logger = Logger.getLogger(ZabAcceptanceSession.class.getName());

	private int epochId = 0;
	private int serialNumber = 0;
	
	private ProcessList processList;
	private int voteCount = 0;
	private boolean isCommitted = false;
	
	ZabAcceptanceSession(Layer layer) {
		super(layer);
	}
	
	@Override
	public void handle(Event event) {
		if (event instanceof ProcessListInit)
			handleProcessListInit((ProcessListInit) event);
		if (event instanceof AllProcessesReady)
			handleAllProcessesReady((AllProcessesReady) event);
		else if (event instanceof FailureDetected)
			handleFailureDetected((FailureDetected) event);
		else if (event instanceof LeaderChanged)
			handleLeaderChanged((LeaderChanged) event);
		else if (event instanceof ZabPropose)
			handleZabPropose((ZabPropose) event);
		else if (event instanceof ZabAccept)
			handleZabAccept((ZabAccept) event);
		else if (event instanceof ZabDeny)
			handleZabDeny((ZabDeny) event);
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
	
	private void handleAllProcessesReady(AllProcessesReady event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received AllProcessesReady");
		
		// Set all process states to correct
		for (int i = 0; i < processList.getSize(); i++) {
			processList.getProcess(i).setState(ProcessState.CORRECT);
		}
		
		// Let the event continue
		try {
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
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
	}
	
	private void handleLeaderChanged(LeaderChanged event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received LeaderChanged, the new leader is " +
					event.getNewLeaderId() + ", new epoch " + event.getNewEpochId());
		
		// Set the new epoch
		if (event.getNewEpochId() != epochId + 1) {
			if (logger.isLoggable(Level.SEVERE))
				logger.severe("The epoch id is not as we expected. Do we miss something?");
		}
		epochId = event.getNewEpochId();
		
		// Let the event continue
		try {
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
	
	private void handleZabPropose(ZabPropose event) {
		
		try {
			if (event.getDir() == Direction.DOWN) { // Leader
				if (logger.isLoggable(Level.FINE))
					logger.fine(String.format("Received ZabPropose from application (epoch id: %d, serial #: %d)",
							event.getEpochId(), event.getSerialNumber()));
				
				// Reset voting
				voteCount = 0; // Leader has a vote
				isCommitted = false;
				
				// Note that the leader will also receive its broadcast message
				// so the leader also has to send Accept message.
				
				// Let the event continue to broadcast
				event.go();
			} else { // Broadcast messages from network
				if (logger.isLoggable(Level.FINE))
					logger.fine(String.format("Received ZabPropose from network (epoch id: %d, serial #: %d)",
							event.getEpochId(), event.getSerialNumber()));
				
				if (event.getEpochId() == epochId && event.getSerialNumber() > serialNumber) {
					serialNumber = event.getSerialNumber();
					
					// Let the event go continue going UP for caching the message
					event.go();
					
					// Accept the proposal
					ZabAccept accept = new ZabAccept(event.getChannel(), this,
							event.getEpochId(), event.getSerialNumber());
					accept.source = processList.getSelfProcess().getAddress();
					accept.dest = event.source;
					accept.init();
					accept.go();

					if (logger.isLoggable(Level.FINE))
						logger.fine(String.format("Accept proposal (epoch id: %d, serial #: %d)",
								event.getEpochId(), event.getSerialNumber()));
				} else {
					// Deny the proposal
					ZabDeny deny = new ZabDeny(event.getChannel(), this,
							event.getEpochId(), event.getSerialNumber());
					deny.source = processList.getSelfProcess().getAddress();
					deny.dest = event.source;
					deny.init();
					deny.go();

					if (logger.isLoggable(Level.FINE))
						logger.fine(String.format("Deny proposal (epoch id: %d, serial #: %d)",
								event.getEpochId(), event.getSerialNumber()));
				}
			}
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
	
	// Leader's method
	private void handleZabAccept(ZabAccept event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine(String.format("Received ZabAccept from %s (epoch id: %d, serial #: %d, vote #: %d)",
					event.source, event.getEpochId(), event.getSerialNumber(), voteCount));
		
		if (event.getEpochId() == epochId && event.getSerialNumber() == serialNumber) {
			voteCount++;
			if (voteCount > processList.getCorrectCount() / 2 && !isCommitted) {
				commit(event.getChannel());
			}
		}
	}

	// Leader's method
	private void handleZabDeny(ZabDeny event) {
		if (logger.isLoggable(Level.WARNING))
			logger.warning(String.format("Received ZabDeny from %s (epoch id: %d, serial #: %d, vote #: %d)",
					event.source, event.getEpochId(), event.getSerialNumber(), voteCount));
		
		// TODO: Retry with higher number?
	}
	
	private void commit(Channel chennel) {
		if (logger.isLoggable(Level.FINE))
			logger.fine(String.format("Commit the message (epoch id: %d, serial #: %d, vote #: %d)",
					epochId, serialNumber, voteCount));
		
		try {
			// Broadcast the result (note that this process will
			// also receive one since it is a broadcast)
			ZabCommit commit = new ZabCommit(chennel, Direction.DOWN,
					this, epochId, serialNumber);
			commit.init();
			commit.go();
			
			isCommitted = true;
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
}
