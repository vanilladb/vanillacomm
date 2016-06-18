/*
 *
 * Hands-On code of the book Introduction to Reliable Distributed Programming
 * by Christian Cachin, Rachid Guerraoui and Luis Rodrigues
 * Copyright (C) 2005-2011 Luis Rodrigues
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 *
 * Contact
 * 	Address:
 *		Rua Alves Redol 9, Office 605
 *		1000-029 Lisboa
 *		PORTUGAL
 * 	Email:
 * 		ler@ist.utl.pt
 * 	Web:
 *		http://homepages.gsd.inesc-id.pt/~ler/
 * 
 */

package org.vanilladb.comm.protocols.causalEagerRb;

import net.sf.appia.core.*;
import net.sf.appia.core.events.channel.ChannelInit;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.vanilladb.comm.protocols.events.BroadcastEvent;
import org.vanilladb.comm.protocols.events.Crash;
import org.vanilladb.comm.protocols.events.ProcessInitEvent;
import org.vanilladb.comm.protocols.utils.Debug;
import org.vanilladb.comm.protocols.utils.MessageID;
import org.vanilladb.comm.protocols.utils.ProcessSet;
import org.vanilladb.comm.protocols.utils.SampleProcess;

/**
 * Session implementing the Eager Reliable Broadcast protocol.
 * 
 * @author DBN
 * 
 */
public class CausalEagerRBSession extends Session {

	private ProcessSet processes;

	private long seqNumber;

	private long[] expectedDeliverNumbers;


	// List of MessageID objects
	private Map<MessageID, BroadcastEvent> received;

	/**
	 * @param layer
	 */
	public CausalEagerRBSession(Layer layer) {
		super(layer);
		seqNumber = 0;
	}

	/**
	 * Main event handler
	 */
	public void handle(Event event) {
		// Init events. Channel Init is from Appia and ProcessInitEvent is to
		// know
		// the elements of the group
		if (event instanceof ChannelInit)
			handleChannelInit((ChannelInit) event);
		else if (event instanceof ProcessInitEvent)
			handleProcessInitEvent((ProcessInitEvent) event);
		else if (event instanceof BroadcastEvent) {
			if (event.getDir() == Direction.DOWN)
				// UPON event from the above protocol (or application)
				rbBroadcast((BroadcastEvent) event);
			else
				// UPON event from the bottom protocol (or perfect point2point
				// links)
				bebDeliver((BroadcastEvent) event);
		} else if (event instanceof Crash)
			handleCrash((Crash) event);
	}

	/**
	 * @param init
	 */
	private void handleChannelInit(ChannelInit init) {
		try {
			init.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
		received = new HashMap<MessageID, BroadcastEvent>();
	}

	/**
	 * @param event
	 */
	@SuppressWarnings("unchecked")
	private void handleProcessInitEvent(ProcessInitEvent event) {
		processes = event.getProcessSet();
		try {
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}

		expectedDeliverNumbers = new long[processes.getSize()];
		for (int i = 0; i < processes.getSize(); ++i) {
			expectedDeliverNumbers[i] = 0;
		}
	}

	/**
	 * Called when the above protocol sends a message.
	 * 
	 * @param event
	 */
	private void rbBroadcast(BroadcastEvent event) {
		// first we take care of the header of the message
		SampleProcess self = processes.getSelfProcess();
		MessageID msgID = new MessageID(self.getProcessNumber(), seqNumber);
		seqNumber++;
		Debug.print("RB: broadcasting message.");
		event.getMessage().pushObject(msgID);
		// broadcast the message
		bebBroadcast(event);

	}

	/**
	 * Called when the lower protocol delivers a message.
	 * 
	 * @param event
	 */
	private void bebDeliver(BroadcastEvent event) {
		Debug.print("RB: Received message from beb.");
		MessageID msgID = (MessageID) event.getMessage().peekObject();

		if (msgID.seqNumber >= expectedDeliverNumbers[msgID.process]) {
			Debug.print("RB: message is new.");

			BroadcastEvent retransmission = null;

			try {
				retransmission = (BroadcastEvent) event.cloneEvent();
			} catch (CloneNotSupportedException e1) {
				e1.printStackTrace();
			}
			
			event.getMessage().popObject();
			received.put(msgID, event);
			
						
			
			
			if(msgID.seqNumber == expectedDeliverNumbers[msgID.process]){
				while(true){
					BroadcastEvent ev = received.remove(msgID);
					if(ev == null){
						break;
					}
					try {
						ev.go();
					} catch (AppiaEventException e) {
						e.printStackTrace();
					}
					++msgID.seqNumber;
				}
				expectedDeliverNumbers[msgID.process] = msgID.seqNumber;
			}
			
			bebBroadcast(retransmission);
		}
		
	}

	
	/**
	 * Called by this protocol to send a message to the lower protocol.
	 * 
	 * @param event
	 */
	private void bebBroadcast(BroadcastEvent event) {
		Debug.print("RB: sending message to beb.");
		try {
			event.setDir(Direction.DOWN);
			event.setSourceSession(this);
			event.init();
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Called when some process crashed.
	 * 
	 * @param crash
	 */
	private void handleCrash(Crash crash) {
		int pi = crash.getCrashedProcess();
		Logger.getLogger(CausalEagerRBSession.class.getName()).fine(
				"Process " + pi + " failed.");

		// changes the state of the process to "failed"
		processes.getProcess(pi).setCorrect(false);

		try {
			crash.go();
		} catch (AppiaEventException ex) {
			ex.printStackTrace();
		}
	}
}
