package org.vanilladb.comm.protocols.tcpfd;

import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	
	private static final int RETRY_PERIOD = 5000; // in milliseconds
	
	private ProcessList processList;
	
	TcpFailureDetectionSession(Layer layer) {
		super(layer);
	}
	
	@Override
	public void handle(Event event) {
		if (event instanceof ProcessListInit)
			handleProcessListInit((ProcessListInit) event);
		else if (event instanceof RegisterSocketEvent)
			handleRegisterSocket((RegisterSocketEvent) event);
		else if (event instanceof FdHello)
			handleFdHello((FdHello) event);
		else if (event instanceof FdHelloAck)
			handleFdHelloAck((FdHelloAck) event);
		else if (event instanceof TcpUndeliveredEvent)
			handleTcpUndelivered((TcpUndeliveredEvent) event);
		else if (event instanceof FdHelloRetry)
			handleFdHelloRetry((FdHelloRetry) event);
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
	
	private void handleRegisterSocket(RegisterSocketEvent event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received RegisterSocketEvent");
		
		try {
			// Let the event keep going
			event.go();
			
			if (event.getDir() == Direction.UP && !event.error) {
				if (logger.isLoggable(Level.FINE))
					logger.fine("Sending FdHello to all other nodes");
				
				for (int id = 0; id < processList.getSize(); id++) {
					if (id != processList.getSelfId()) {
						FdHello hello = new FdHello(event.getChannel(), this);
						hello.source = processList.getSelfProcess().getAddress();
						hello.dest = processList.getProcess(id).getAddress();
						hello.init();
						hello.go();
					}
				}
				
				// Retry later, in case that other processes are still initializing 
				scheduleHelloRetry(event.getChannel());
			}
		} catch (AppiaEventException e) {
			e.printStackTrace();
		} catch (AppiaException e) {
			e.printStackTrace();
		}
	}
	
	private void handleFdHello(FdHello event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received FdHello from " + event.source);
		
		try {
			FdHelloAck ack = new FdHelloAck(event.getChannel(), this);
			ack.source = processList.getSelfProcess().getAddress();
			ack.dest = event.source;
			ack.init();
			ack.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
	
	private void handleFdHelloAck(FdHelloAck event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received FdHelloAck from " + event.source);
		
		try {
			processList.getProcess((SocketAddress) event.source)
					.setState(ProcessState.CORRECT);
			
			// Check if all processes are correct
			if (processList.areAllCorrect()) {
				AllProcessesReady ready = new AllProcessesReady(event.getChannel(),
						Direction.UP, this);
				ready.init();
				ready.go();
			}
		} catch (AppiaEventException e) {
			e.printStackTrace();
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
					logger.severe("Detected failed processs " + event.getFailedAddress());
				
				// Mark failed
				processList.getProcess(processId).setState(ProcessState.FAILED);
				
				// Notify upper layers of the failed process
				FailureDetected fd = new FailureDetected(event.getChannel(), this, processId);
				fd.init();
				fd.go();
			}
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
	
	private void handleFdHelloRetry(FdHelloRetry event) {
		try {
			boolean needToRetryLater = false;
			
			for (int id = 0; id < processList.getSize(); id++) {
				if (id != processList.getSelfId() &&
						!processList.getProcess(id).isInitialized()) {
					if (logger.isLoggable(Level.FINE))
						logger.fine("Sending FdHello again to " +
								processList.getProcess(id).getAddress());
					
					FdHello hello = new FdHello(event.getChannel(), this);
					hello.source = processList.getSelfProcess().getAddress();
					hello.dest = processList.getProcess(id).getAddress();
					hello.init();
					hello.go();
					
					needToRetryLater = true;
				}
			}
			
			if (needToRetryLater)
				scheduleHelloRetry(event.getChannel());
		} catch (AppiaEventException e) {
			e.printStackTrace();
		} catch (AppiaException e) {
			e.printStackTrace();
		}
	}
	
	private void scheduleHelloRetry(Channel channel) throws AppiaEventException, AppiaException {
		FdHelloRetry retry = new FdHelloRetry(RETRY_PERIOD, "FdHelloRetry",
				channel, this);
		retry.init();
		retry.go();
	}
}
