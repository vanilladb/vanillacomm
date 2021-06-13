package org.vanilladb.comm.protocols.zabacceptance;

import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vanilladb.comm.process.ProcessList;
import org.vanilladb.comm.process.ProcessState;
import org.vanilladb.comm.protocols.events.ProcessListInit;
import org.vanilladb.comm.protocols.tcpfd.FailureDetected;
import org.vanilladb.comm.protocols.tcpfd.ProcessConnected;
import org.vanilladb.comm.protocols.zabelection.LeaderChanged;
import org.vanilladb.comm.protocols.zabproposal.ZabProposal;
import org.vanilladb.comm.protocols.zabproposal.ZabProposalId;
import org.vanilladb.comm.protocols.zabproposal.ZabPropose;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;

public class ZabAcceptanceSession extends Session {
	private static Logger logger = Logger.getLogger(ZabAcceptanceSession.class.getName());

	private int epochId = 0;
	private long lastReceivedProposalSerial = 0;
	
	private ProcessList processList;
	
	ZabAcceptanceSession(Layer layer) {
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
		else if (event instanceof LeaderChanged)
			handleLeaderChanged((LeaderChanged) event);
		else if (event instanceof ZabPropose)
			handleZabPropose((ZabPropose) event);
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
					logger.fine(String.format("Received ZabPropose from application"));
				
				// Note that the leader will also receive its broadcast message
				// so the leader also has to send Accept message.
				
				// Let the event continue to broadcast
				event.go();
			} else { // Broadcast messages from network
				ZabProposal proposal = (ZabProposal) event.getMessage().popObject();
				ZabProposalId id = proposal.getId();
				int senderId = processList.getId((SocketAddress) event.source);
				int direction = (processList.getSelfId() == senderId)? Direction.UP : Direction.DOWN;
				
				if (logger.isLoggable(Level.FINE))
					logger.fine(String.format("Received ZabPropose from network (epoch id: %d, proposal serial #: %d)",
							id.getEpochId(), id.getSerialNumber()));
				
				if (id.getEpochId() == epochId && id.getSerialNumber() > lastReceivedProposalSerial) {
					lastReceivedProposalSerial = id.getSerialNumber();
					
					// Send a event to ZabProposalLayer for caching the message
					ZabCacheProposal cache = new ZabCacheProposal(event.getChannel(), this, proposal);
					cache.init();
					cache.go();
					
					// Accept the proposal
					ZabAccept accept = new ZabAccept(event.getChannel(), direction, this);
					accept.getMessage().pushObject(id);
					accept.source = processList.getSelfProcess().getAddress();
					accept.dest = event.source;
					accept.init();
					accept.go();

					if (logger.isLoggable(Level.FINE))
						logger.fine(String.format("Accept proposal (epoch id: %d, proposal serial #: %d)",
								id.getEpochId(), id.getSerialNumber()));
				} else {
					// Deny the proposal
					ZabDeny deny = new ZabDeny(event.getChannel(), direction, this);
					deny.getMessage().pushObject(id);
					deny.source = processList.getSelfProcess().getAddress();
					deny.dest = event.source;
					deny.init();
					deny.go();

					if (logger.isLoggable(Level.FINE))
						logger.fine(String.format("Deny proposal (epoch id: %d, proposal serial #: %d)",
								id.getEpochId(), id.getSerialNumber()));
				}
			}
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
}
