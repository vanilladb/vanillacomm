package org.vanilladb.comm.protocols.urb;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vanilladb.comm.process.ProcessList;
import org.vanilladb.comm.process.ProcessState;
import org.vanilladb.comm.protocols.beb.Broadcast;
import org.vanilladb.comm.protocols.events.ProcessListInit;
import org.vanilladb.comm.protocols.rb.MessageId;
import org.vanilladb.comm.protocols.tcpfd.FailureDetected;
import org.vanilladb.comm.protocols.tcpfd.ProcessConnected;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;

public class UniformReliableBroadcastSession extends Session {
	private static Logger logger = Logger.getLogger(UniformReliableBroadcastSession.class.getName());
	
	private ProcessList processList;
	// <Source Process Id> -> <A set of delivered seq #>
	private Map<Integer, Set<Integer>> delivered = new HashMap<Integer, Set<Integer>>();
	// <Srouce Process Id> -> (<seq #> -> <message>)
	private Map<Integer, Map<Integer, UniformReliableBroadcast>> pending = new HashMap<Integer, Map<Integer, UniformReliableBroadcast>>();
	// <Srouce Process Id> -> (<seq #> -> <a set of ACKed processes>)
	private Map<Integer, Map<Integer, Set<Integer>>> acks = new HashMap<Integer, Map<Integer, Set<Integer>>>();
	private int sequenceNumber = 0;
	
	UniformReliableBroadcastSession(Layer layer) {
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
		else if (event instanceof UniformReliableBroadcast) {
			if (event.getDir() == Direction.DOWN)
				handleBroadcastRequest((UniformReliableBroadcast) event);
			else
				handleBroadcastDeliver((UniformReliableBroadcast) event);
		}
		
		// Try to deliver the pending messages when anything happens
		tryDeliver();
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
		
		// Initialize the data structures
		for (int pid = 0; pid < processList.getSize(); pid++) {
			delivered.put(pid, new HashSet<Integer>());
			pending.put(pid, new HashMap<Integer, UniformReliableBroadcast>());
			acks.put(pid, new HashMap<Integer, Set<Integer>>());
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
		
		try {
			// Let the event continue
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
	
	private void handleBroadcastRequest(UniformReliableBroadcast event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received a Broadcast request from a upper layer");
		
		// Append the message id
		int seqNum = sequenceNumber++;
		int selfPid = processList.getSelfId();
		MessageId id = new MessageId(selfPid, seqNum);
		event.getMessage().pushObject(id);
		
		// Add it to the pending list
		try {
			pending.get(selfPid).put(seqNum, (UniformReliableBroadcast) event.cloneEvent());
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		
		// Forward to Layer BestEffortBroadcast
		try {
			event.setSourceSession(this);
			event.setDir(Direction.DOWN);
			event.init();
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
	
	private void handleBroadcastDeliver(UniformReliableBroadcast event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received a delivered broadcast from a lower layer");
		
		// Retrieve the message id
		MessageId id = (MessageId) event.getMessage().popObject();
		int sourcePid = id.getSourceProcessId();
		int seqNum = id.getSequenceNumber();
		
		// Add the sender to the ACK set
		int senderPid = processList.getId((SocketAddress) event.source);
		receiveAck(id, senderPid);
		
		// Check if it is in the pending list
		if (pending.get(sourcePid).containsKey(seqNum)) {
			// Add it to the pending list
			try {
				pending.get(sourcePid).put(seqNum, (UniformReliableBroadcast) event.cloneEvent());
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			
			// Forward to Layer BestEffortBroadcast
			try {
				event.setSourceSession(this);
				event.setDir(Direction.DOWN);
				event.init();
				event.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void receiveAck(MessageId id, int senderPid) {
		Set<Integer> ackSet = acks.get(id.getSourceProcessId()).get(id.getSequenceNumber());
		if (ackSet == null) {
			ackSet = new HashSet<Integer>();
			acks.get(id.getSourceProcessId()).put(id.getSequenceNumber(), ackSet);
		}
		ackSet.add(senderPid);
	}
	
	private void tryDeliver() {
		List<Integer> deletedFromPending = new ArrayList<Integer>();
		
		// Check all pending messages
		for (Entry<Integer, Map<Integer, UniformReliableBroadcast>> entry : pending.entrySet()) {
			int sourcePid = entry.getKey();
			Map<Integer, UniformReliableBroadcast> messages = entry.getValue();
			deletedFromPending.clear();
			
			for (Entry<Integer, UniformReliableBroadcast> messageEntry : messages.entrySet()) {
				int seqNum = messageEntry.getKey();
				Broadcast message = messageEntry.getValue();
				
				if (canDeliver(sourcePid, seqNum)) {
					deliver(sourcePid, seqNum, message);
					deletedFromPending.add(seqNum);
				}
			}
			
			// Delete the delivered message from pending list
			for (Integer sn : deletedFromPending)
				messages.remove(sn);
		}
	}
	
	private boolean canDeliver(int sourcePid, int seqNum) {
		Set<Integer> ackProcesses = acks.get(sourcePid).get(seqNum);
		return ackProcesses != null && ackProcesses.containsAll(processList.getCorrectProcessIds());
	}
	
	private void deliver(int sourcePid, int seqNum, Broadcast message) {
		// Add to the delivered set
		delivered.get(sourcePid).add(seqNum);
	
		// Deliver to upper layers
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
