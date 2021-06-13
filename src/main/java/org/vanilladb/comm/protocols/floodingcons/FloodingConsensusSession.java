package org.vanilladb.comm.protocols.floodingcons;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vanilladb.comm.process.ProcessList;
import org.vanilladb.comm.process.ProcessState;
import org.vanilladb.comm.protocols.events.ProcessListInit;
import org.vanilladb.comm.protocols.tcpfd.FailureDetected;
import org.vanilladb.comm.protocols.tcpfd.ProcessConnected;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;

/**
 * This implements Flooding Consensus Protocol. Note that this can only be run once.
 * 
 * @author SLMT
 *
 */
public class FloodingConsensusSession extends Session {
	private static Logger logger = Logger.getLogger(FloodingConsensusSession.class.getName());
	
	private ProcessList processList;
	private int roundId = 1;
	private boolean hasDecided = false;
	private List<Set<Value>> proposalsPerRound = new ArrayList<Set<Value>>();
	private List<Set<Integer>> correctsPerRound = new ArrayList<Set<Integer>>();
	
	FloodingConsensusSession(Layer layer) {
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
		else if (event instanceof ConsensusRequest)
			handleConsensusRequest((ConsensusRequest) event);
		else if (event instanceof Propose)
			handlePropose((Propose) event);
		else if (event instanceof Decide)
			handleDecide((Decide) event);
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
		
		// Initialize the first round
		correctsPerRound.add(new HashSet<Integer>());
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
		
		// Add the correct processes to the list of initial round (round 0)
		correctsPerRound.get(0).add(event.getConnectedProcessId());
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
	
	private void handleConsensusRequest(ConsensusRequest event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received ConsensusRequest");
		
		// Note that we start from round one
		roundId = 1;
		
		// Create a new proposal
		Set<Value> proposal = new HashSet<Value>();
		proposal.add(event.getValue());
		
		// Propose
		propose(event.getChannel(), roundId, proposal);
	}
	
	// Note that we might receive a proposal from ourselves
	// since it is a broadcast.
	private void handlePropose(Propose event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received Propose");
		
		// Includes the proposal
		includePrposal(event.getRoundId(), event.getProposal());
		
		// Record who sent the proposal
		int senderPid = processList.getId((SocketAddress) event.source);
		setCorrect(event.getRoundId(), senderPid);
		
		// See if we can decide a value
		tryDecide(event.getChannel());
	}
	
	private void handleDecide(Decide event) {
		if (logger.isLoggable(Level.FINE))
			logger.fine("Received Decide");
		
		if (!hasDecided) {
			hasDecided = true;
			
			// Deliver the result
			deliverDecision(event.getChannel(), event.getValue());
			
			// Relay the decision
			try {
				event.setDir(Direction.DOWN);
				event.setSourceSession(this);
				event.init();
				event.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void propose(Channel channel, int roundId, Set<Value> proposal) {
		// Send a Propose broadcast
		try {
			Propose propose = new Propose(channel, this, roundId, proposal);
			propose.init();
			propose.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
	
	private void includePrposal(int roundId, Set<Value> receivedProposal) {
		// Extends the proposal list to match the round
		while (proposalsPerRound.size() < roundId + 1) {
			proposalsPerRound.add(new HashSet<Value>());
		}
		
		// Add the proposal to the list
		proposalsPerRound.get(roundId).addAll(receivedProposal);
	}
	
	private void setCorrect(int roundId, int pid) {
		// Extends the correct list to match the round
		while (correctsPerRound.size() < roundId + 1) {
			correctsPerRound.add(new HashSet<Integer>());
		}
		
		// Add the correct process to the list
		correctsPerRound.get(roundId).add(pid);
	}
	
	private void tryDecide(Channel channel) {
		// Check if we have received all the proposal in the current round
		if (!hasDecided && correctsPerRound.get(roundId).containsAll(processList.getCorrectProcessIds())) {
			// Check if the view changes in this round
			if (correctsPerRound.get(roundId).containsAll(correctsPerRound.get(roundId - 1))) {
				decide(channel);
			} else {
				// Cannot decide, start a new round
				roundId++;
				propose(channel, roundId, proposalsPerRound.get(roundId - 1));
			}
		}
	}

	
	private void decide(Channel channel) {
		// Make a decision
		Value decision = Collections.min(proposalsPerRound.get(roundId));
		
		// Deliver the decision
		hasDecided = true;
		deliverDecision(channel, decision);
		
		// Send a decide broadcast
		try {
			Decide decide = new Decide(channel, this, decision);
			decide.init();
			decide.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
	
	private void deliverDecision(Channel channel, Value decision) {
		try {
			ConsensusResult result = new ConsensusResult(channel, this, decision);
			result.init();
			result.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
}
