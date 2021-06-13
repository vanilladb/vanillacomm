package org.vanilladb.comm.protocols.zabproposal;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vanilladb.comm.process.ProcessList;
import org.vanilladb.comm.process.ProcessState;
import org.vanilladb.comm.protocols.events.ProcessListInit;
import org.vanilladb.comm.protocols.tcpfd.ProcessConnected;
import org.vanilladb.comm.protocols.totalorderappl.TotalOrderMessages;
import org.vanilladb.comm.protocols.totalorderappl.TotalOrderRequest;
import org.vanilladb.comm.protocols.zabacceptance.ZabAccept;
import org.vanilladb.comm.protocols.zabacceptance.ZabCacheProposal;
import org.vanilladb.comm.protocols.zabacceptance.ZabCommit;
import org.vanilladb.comm.protocols.zabacceptance.ZabDeny;
import org.vanilladb.comm.protocols.zabelection.LeaderChanged;
import org.vanilladb.comm.protocols.zabelection.LeaderInit;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;

public class ZabProposalSession extends Session {
	private static Logger logger = Logger.getLogger(ZabProposalSession.class.getName());
	
	// For all processes
	private ProcessList processList;
	private int leaderId;
	private int epochId = 0;
	private long lastReceivedProposalSerial = 0;
	private ZabProposal cachedProposal;
	
	// For the leader
	private boolean hasOngoingProposal;
	private Queue<Serializable> messageQueue = new ArrayDeque<Serializable>();
	private long nextProposalSerial = 1;
	private long nextMessageStart = 1;
	private long currentProposingSerial = 1;
	private int voteCount = 0;
	
	ZabProposalSession(Layer layer) {
		super(layer);
	}
	
	@Override
	public void handle(Event event) {
		if (event instanceof ProcessListInit)
			handleProcessListInit((ProcessListInit) event);
		else if (event instanceof ProcessConnected)
			handleProcessConnected((ProcessConnected) event);
		else if (event instanceof LeaderInit)
			handleLeaderInit((LeaderInit) event);
		else if (event instanceof LeaderChanged)
			handleLeaderChanged((LeaderChanged) event);
		else if (event instanceof TotalOrderRequest)
			handleTotalOrderRequest((TotalOrderRequest) event);
		else if (event instanceof ZabCacheProposal)
			handleZabCacheProposal((ZabCacheProposal) event);
		else if (event instanceof ZabAccept)
			handleZabAccept((ZabAccept) event);
		else if (event instanceof ZabDeny)
			handleZabDeny((ZabDeny) event);
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
			messageQueue.addAll(event.getCarriedMessages());
			if (!hasOngoingProposal)
				propose(event.getChannel());
		} else {
			redirectToLeader(event);
		}
	}
	
	// For caching the message
	private void handleZabCacheProposal(ZabCacheProposal event) {
		ZabProposal proposal = event.getProposal();
		ZabProposalId id = proposal.getId();
		
		if (logger.isLoggable(Level.FINE))
			logger.fine(String.format("Received ZabCacheProposal (epoch id: %d, proposal serial #: %d)",
					id.getEpochId(), id.getSerialNumber()));
		
		// Note that since the leader has advanced its serialNumber
		// it will not cache its message here.
		if (id.getEpochId() == epochId && id.getSerialNumber() > lastReceivedProposalSerial) {
			lastReceivedProposalSerial = id.getSerialNumber();
			cachedProposal = proposal;
		}
	}
	
	private void handleZabAccept(ZabAccept event) {
		ZabProposalId id = (ZabProposalId) event.getMessage().popObject();
		
		if (logger.isLoggable(Level.FINE))
			logger.fine(String.format("Received ZabAccept from %s (epoch id: %d, proposal serial #: %d, vote #: %d)",
					event.source, id.getEpochId(), id.getSerialNumber(), voteCount));
		
		if (id.getEpochId() == epochId && id.getSerialNumber() == currentProposingSerial) {
			voteCount++;
			if (voteCount > processList.getCorrectCount() / 2 && hasOngoingProposal) {
				commit(event.getChannel());
				
				// Start the next proposal
				if (!messageQueue.isEmpty())
					propose(event.getChannel());
			}
		}
	}
	
	private void handleZabDeny(ZabDeny event) {
		ZabProposalId id = (ZabProposalId) event.getMessage().popObject();
		
		if (logger.isLoggable(Level.WARNING))
			logger.warning(String.format("Received ZabDeny from %s (epoch id: %d, proposal serial #: %d, vote #: %d)",
					event.source, id.getEpochId(), id.getSerialNumber(), voteCount));
		
		// TODO: Retry with higher number?
	}
	
	private void handleZabCommit(ZabCommit event) {
		ZabProposalId id = (ZabProposalId) event.getMessage().popObject();
		
		if (logger.isLoggable(Level.FINE))
			logger.fine(String.format("Received ZabCommit (epoch id: %d, serial #: %d)",
					id.getEpochId(), id.getSerialNumber()));
		
		if (id.getEpochId() == epochId && id.getSerialNumber() == lastReceivedProposalSerial) {
			try {
				TotalOrderMessages messages = new TotalOrderMessages(event.getChannel(),
						this, cachedProposal.getMessages(), cachedProposal.getMessageStartId());
				messages.init();
				messages.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
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
					epochId, nextMessageStart));
		
		try {
			// Create a proposal
			ZabProposalId id = new ZabProposalId(epochId, nextProposalSerial);
			ZabProposal proposal = new ZabProposal(id, nextMessageStart,
					messageList.toArray(new Serializable[messageList.size()]));
			
			// Record the information for voting
			currentProposingSerial = nextProposalSerial;
			voteCount = 0;
			
			// Advances the ids
			nextProposalSerial++;
			nextMessageStart += messageList.size();
			
			// Create a event for sending the proposal
			ZabPropose propose = new ZabPropose(channel, this);
			// Note: there are two ways to send a proposal via a SendableEvent.
			// The first way is to push each proposed value and the corresponding
			// information such as epoch id and proposal id one by one.
			// Another way is to package them into a single object and pushObject
			// it once.
			// We chose the second way not only for higher readability
			// but also for efficiency.
			// According to our research, calling pushObject multiple times
			// for a large proposal dramatically reduces scalability of this module.
			propose.getMessage().pushObject(proposal);
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
	
	private void commit(Channel chennel) {
		if (logger.isLoggable(Level.FINE))
			logger.fine(String.format("Commit the message (epoch id: %d, proposal serial #: %d, vote #: %d)",
					epochId, currentProposingSerial, voteCount));
		
		try {
			// Broadcast the result (note that this process will
			// also receive one since it is a broadcast)
			ZabCommit commit = new ZabCommit(chennel, this);
			commit.getMessage().pushObject(new ZabProposalId(epochId, currentProposingSerial));
			commit.init();
			commit.go();
			
			hasOngoingProposal = false;
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
}
