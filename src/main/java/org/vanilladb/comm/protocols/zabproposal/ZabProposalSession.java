package org.vanilladb.comm.protocols.zabproposal;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vanilladb.comm.process.ProcessList;
import org.vanilladb.comm.protocols.events.ProcessListInit;
import org.vanilladb.comm.protocols.totalorderappl.TotalOrderMessage;
import org.vanilladb.comm.protocols.totalorderappl.TotalOrderRequest;
import org.vanilladb.comm.protocols.zabacceptance.ZabCommit;
import org.vanilladb.comm.protocols.zabelection.LeaderChanged;
import org.vanilladb.comm.protocols.zabelection.LeaderInit;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;

public class ZabProposalSession extends Session {
	private static Logger logger = Logger.getLogger(ZabProposalSession.class.getName());

	private ProcessList processList;
	private int leaderId;
	private int epochId = 0;
	private int serialNumber = 0;
	private List<Serializable> cachedMessages;
	
	// For the leader
	private boolean hasOngoingProposal;
	private Queue<Serializable> messageQueue = new ArrayDeque<Serializable>();
	
	ZabProposalSession(Layer layer) {
		super(layer);
	}
	
	@Override
	public void handle(Event event) {
		if (event instanceof ProcessListInit)
			handleProcessListInit((ProcessListInit) event);
		else if (event instanceof LeaderInit)
			handleLeaderInit((LeaderInit) event);
		else if (event instanceof LeaderChanged)
			handleLeaderChanged((LeaderChanged) event);
		else if (event instanceof TotalOrderRequest)
			handleTotalOrderRequest((TotalOrderRequest) event);
		else if (event instanceof ZabPropose)
			handleZabPropose((ZabPropose) event);
		else if (event instanceof ZabCommit)
			handleZabCommit((ZabCommit) event);
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
	
	private void handleLeaderInit(LeaderInit event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received LeaderInit, the leader is " + event.getLeaderId());
		
		// Set the leader id
		leaderId = event.getLeaderId();
	}
	
	private void handleLeaderChanged(LeaderChanged event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received LeaderChanged, the new leader is " +
					event.getNewLeaderId() + ", new epoch " + event.getNewEpochId());
		
		// Set the leader id
		leaderId = event.getNewLeaderId();
		if (event.getNewEpochId() != epochId + 1) {
			if (logger.isLoggable(Level.SEVERE))
				logger.severe("The epoch id is not as we expected. Do we miss something?");
		}
		epochId = event.getNewEpochId();
	}
	
	private void handleTotalOrderRequest(TotalOrderRequest event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received TotalOrderRequest");
		
		if (processList.getSelfId() == leaderId) {
			messageQueue.add(event.getCarriedMessage());
			if (!hasOngoingProposal)
				propose(event.getChannel());
		} else {
			redirectToLeader(event);
		}
	}
	
	// For caching the message
	private void handleZabPropose(ZabPropose event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine(String.format("Received ZabPropose (epoch id: %d, serial #: %d)",
					event.getEpochId(), event.getSerialNumber()));
		
		// Note that since the leader has advanced its serialNumber
		// it will not cache its message here.
		if (event.getEpochId() == epochId && event.getSerialNumber() > serialNumber) {
			serialNumber = event.getSerialNumber();
			cachedMessages = event.getCarriedMessages();
		}
	}
	
	private void handleZabCommit(ZabCommit event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine(String.format("Received ZabCommit (epoch id: %d, serial #: %d)",
					event.getEpochId(), event.getSerialNumber()));
		
		if (event.getEpochId() == epochId && event.getSerialNumber() == serialNumber) {
			try {
				for (Serializable cachedMessage : cachedMessages) {
					TotalOrderMessage message = new TotalOrderMessage(event.getChannel(),
							this, cachedMessage, serialNumber);
					message.init();
					message.go();
				}
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
			
			if (processList.getSelfId() == leaderId) {
				hasOngoingProposal = false;
				if (!messageQueue.isEmpty())
					propose(event.getChannel());
			}
		}
	}
	
	private void propose(Channel channel) {
		// Build a message list
		List<Serializable> messageList = new ArrayList<Serializable>();
		Serializable message = messageQueue.poll();
		while (message != null) {
			messageList.add(message);
			message = messageQueue.poll();
		}
		
		if (messageList.isEmpty())
			return;

		if (logger.isLoggable(Level.FINE))
			logger.fine(String.format("Leader proposes (epoch id: %d, serial #: %d)",
					epochId, serialNumber + 1));
		
		try {
			serialNumber++;
			ZabPropose propose = new ZabPropose(channel, this,
					epochId, serialNumber, messageList);
			
			// Cache the message
			cachedMessages = messageList;
			
			propose.init();
			propose.go();
			
			hasOngoingProposal = true;
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
	
	private void redirectToLeader(TotalOrderRequest request) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Redirect the message to the leader (id = " + leaderId + ")");
		
		try {
			request.source = processList.getSelfProcess().getAddress();
			request.dest = processList.getProcess(leaderId).getAddress();
			request.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
}
