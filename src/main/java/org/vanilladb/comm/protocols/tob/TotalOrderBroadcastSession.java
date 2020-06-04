package org.vanilladb.comm.protocols.tob;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vanilladb.comm.process.ProcessList;
import org.vanilladb.comm.process.ProcessState;
import org.vanilladb.comm.protocols.events.ProcessListInit;
import org.vanilladb.comm.protocols.floodingcons.ConsensusRequest;
import org.vanilladb.comm.protocols.floodingcons.ConsensusResult;
import org.vanilladb.comm.protocols.rb.MessageId;
import org.vanilladb.comm.protocols.tcpfd.FailureDetected;
import org.vanilladb.comm.protocols.tcpfd.ProcessConnected;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;

public class TotalOrderBroadcastSession extends Session {
	private static Logger logger = Logger.getLogger(TotalOrderBroadcastSession.class.getName());
	
	private ProcessList processList;
	
	private int sequenceNumber = 1;
	
	private Set<MessageId> delivered = new HashSet<MessageId>();

	// <Message Id> -> <message>
	private Map<MessageId, TotalOrderBroadcast> unordered =
			new HashMap<MessageId, TotalOrderBroadcast>();
	
	private List<MessageId> waitForDeliverIds = new ArrayList<MessageId>();
	private Map<MessageId, TotalOrderBroadcast> waitForDeliverMessages
		= new HashMap<MessageId, TotalOrderBroadcast>();
	
	private boolean wait = false;
	
	TotalOrderBroadcastSession(Layer layer) {
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
		else if (event instanceof TotalOrderBroadcast) {
			if (event.getDir() == Direction.DOWN) {
				handleBroadcastRequest((TotalOrderBroadcast) event);
			} else {
				handleBroadcastDeliver((TotalOrderBroadcast) event);
			}
		} else if (event instanceof ConsensusResult)
			handleConsensusResult((ConsensusResult) event);
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
		
		processList.getProcess(event.getFailedProcessId()).setState(ProcessState.FAILED);
		
		// Let the event continue
		try {
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
	
	private void handleBroadcastRequest(TotalOrderBroadcast event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received a TotalOrderBroadcast request");
		
		// Append a sequence number
		MessageId id = new MessageId(processList.getSelfId(), sequenceNumber);
		sequenceNumber++;
		event.getMessage().pushObject(id);

		// Let the event continue.
		// ReliableBroadcast layer will handle it.
		try {
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
	
	private void handleBroadcastDeliver(TotalOrderBroadcast event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received a delivered Broadcast message");
		
		// Take out the message id
		MessageId id = (MessageId) event.getMessage().popObject();
		
		// Check if it is delivered
		if (!delivered.contains(id)) {
			if (waitForDeliverIds.contains(id)) {
				waitForDeliverMessages.put(id, event);
				tryDeliver(event.getChannel());
			} else {
				// Cache the message
				unordered.put(id, event);
				tryOrder(event.getChannel());
			}
			
		}
	}
	
	private void handleConsensusResult(ConsensusResult event) {
		OrderProposal decision = (OrderProposal) event.getDecision();
		
		// Sort the ids
		List<MessageId> orderedIds = new ArrayList<MessageId>(decision.getMessageIds());
		Collections.sort(orderedIds);
		
		// Put the decided ids and corresponding messages to the deliver queue
		for (MessageId id : orderedIds) {
			TotalOrderBroadcast message = unordered.remove(id);
			waitForDeliverIds.add(id);
			if (message != null)
				waitForDeliverMessages.put(id, message);
		}
		
		tryDeliver(event.getChannel());
		
		// Ready for the next consensus
		wait = false;
		
		tryOrder(event.getChannel());
	}
	
	private void tryOrder(Channel channel) {
		if (!wait && !unordered.isEmpty()) {
			wait = true;
			
			// Create a order proposal
			OrderProposal proposal = new OrderProposal(unordered.keySet());
			
			// Send a consensus request
			try {
				ConsensusRequest request = new ConsensusRequest(channel, this, proposal);
				request.init();
				request.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void tryDeliver(Channel channel) {
		Set<MessageId> delivered = new HashSet<MessageId>();
		for (MessageId id : waitForDeliverIds) {
			TotalOrderBroadcast message = waitForDeliverMessages.remove(id);
			if (message != null) {
				delivered.add(id);
				try {
					message.setSourceSession(this);
					message.setDir(Direction.UP);
					message.init();
					message.go();
				} catch (AppiaEventException e) {
					e.printStackTrace();
				}
			}
		}
		
		// Remove the delivered ids form the queue
		waitForDeliverIds.removeAll(delivered);
		this.delivered.addAll(delivered);
	}
}
