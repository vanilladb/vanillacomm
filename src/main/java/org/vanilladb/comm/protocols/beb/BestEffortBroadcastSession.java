package org.vanilladb.comm.protocols.beb;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.vanilladb.comm.process.CommProcess;
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

/**
 * Best Effort Broadcast.
 * 
 * @author yslin
 */
public class BestEffortBroadcastSession extends Session {
	private static Logger logger = Logger.getLogger(BestEffortBroadcastSession.class.getName());
	
	private ProcessList processList;
	
	BestEffortBroadcastSession(Layer layer) {
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
		else if (event instanceof Broadcast)
			handleBroadcast((Broadcast) event);
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
	
	private void handleBroadcast(Broadcast event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received a Broadcast request");

		try {
			if (event.getDir() == Direction.DOWN) {
				// Replicates the event to all other correct processes
				for (int i = 0; i < processList.getSize(); i++) {
					CommProcess process = processList.getProcess(i);
					if (process.isCorrect() && !process.isSelf()) {
						// Clone the event
						Broadcast clonedEvent = (Broadcast) event.cloneEvent();
						
						// Setup the address
						clonedEvent.setSourceSession(this);
						clonedEvent.source = processList.getSelfProcess().getAddress();
						clonedEvent.dest = process.getAddress();
						
						// GO!
						clonedEvent.init();
						clonedEvent.go();
					}
				}
				
				// Send the event back to upper layers
				event.setDir(Direction.UP);
				event.setSourceSession(this);
				event.source = processList.getSelfProcess().getAddress();
				event.dest = processList.getSelfProcess().getAddress();
				event.init();
				event.go();
			} else {
				// Come from other processes, let the event continue
				event.go();
			}
		} catch (AppiaEventException e) {
			e.printStackTrace();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
	}
}
