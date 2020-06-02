package org.vanilladb.comm.protocols.rb;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vanilladb.comm.process.ProcessList;
import org.vanilladb.comm.process.ProcessState;
import org.vanilladb.comm.protocols.events.ProcessListInit;
import org.vanilladb.comm.protocols.tcpfd.FailureDetected;
import org.vanilladb.comm.protocols.tcpfd.ProcessConnected;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;

public class ReliableBroadcastSession extends Session {
	private static Logger logger = Logger.getLogger(ReliableBroadcastSession.class.getName());
	
	private ProcessList processList;
	private Set<MessageId> delivered = new HashSet<MessageId>();
	// <Srouce Process Id> -> (<seq #> -> <message>)
	private Map<Integer, Map<Integer, ReliableBroadcast>> logs = new HashMap<Integer, Map<Integer, ReliableBroadcast>>();
	private int sequenceNumber = 0;
	
	ReliableBroadcastSession(Layer layer) {
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
		else if (event instanceof ReliableBroadcast) {
			if (event.getDir() == Direction.DOWN)
				handleBroadcastRequest((ReliableBroadcast) event);
			else
				handleBroadcastDeliver((ReliableBroadcast) event);
		}
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
			logs.put(pid, new HashMap<Integer, ReliableBroadcast>());
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
		
			// Relay all its messages to other processes
			for (Map.Entry<Integer, ReliableBroadcast> entry : logs.get(event.getFailedProcessId()).entrySet()) {
				int seqNum = entry.getKey();
				ReliableBroadcast broadcast = entry.getValue();
				
				// Append the message id
				MessageId id = new MessageId(event.getFailedProcessId(), seqNum);
				broadcast.getMessage().pushObject(id);
				
				// Go
				broadcast.setSourceSession(this);
				broadcast.setDir(Direction.DOWN);
				broadcast.init();
				broadcast.go();
			}

		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
	
	private void handleBroadcastRequest(ReliableBroadcast event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received a Broadcast request from a upper layer");
		
		// Append the message id
		int seqNum = sequenceNumber++;
		MessageId id = new MessageId(processList.getSelfId(), seqNum);
		event.getMessage().pushObject(id);
		
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
	
	private void handleBroadcastDeliver(ReliableBroadcast event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received a delivered broadcast from a lower layer");
		
		// Retrieve the message id
		MessageId id = (MessageId) event.getMessage().popObject();
		
		// Check if it is delivered since it might be a duplicated message
		if (!delivered.contains(id)) {
			try {
				// Add to the delivered set
				delivered.add(id);
			
				// Log the message
				logs.get(id.getSourceProcessId())
					.put(id.getSequenceNumber(), (ReliableBroadcast) event.cloneEvent());
			
				// If the source has crashed, relay this message
				if (!processList.getProcess(id.getSourceProcessId()).isCorrect()) {
					ReliableBroadcast cloned = (ReliableBroadcast) event.cloneEvent();
					cloned.setSourceSession(this);
					cloned.setDir(Direction.DOWN);
					cloned.init();
					cloned.go();
				}
			
				// Deliver to upper layers
				event.setSourceSession(this);
				event.setDir(Direction.UP);
				event.init();
				event.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
	}
}
