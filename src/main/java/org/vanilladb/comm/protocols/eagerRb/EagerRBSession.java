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

package org.vanilladb.comm.protocols.eagerRb;

import net.sf.appia.core.*;
import net.sf.appia.core.events.channel.ChannelInit;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.vanilladb.comm.protocols.events.Crash;
import org.vanilladb.comm.protocols.events.ProcessInitEvent;
import org.vanilladb.comm.protocols.events.ReliableBroadcastEvent;
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
public class EagerRBSession extends Session {

	private ProcessSet processes;

	private long seqNumber;

	private long[] deliveredNumbers;

	private final int DELIVERED_SHRINK_SIZE = 100;

	// List of MessageID objects
	private Set<MessageID> delivered;

	/**
	 * @param layer
	 */
	public EagerRBSession(Layer layer) {
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
		if (event instanceof ReliableBroadcastEvent) {
			if (event.getDir() == Direction.DOWN)
				// UPON event from the above protocol (or application)
				rbBroadcast((ReliableBroadcastEvent) event);
			else
				// UPON event from the bottom protocol (or perfect point2point
				// links)
				bebDeliver((ReliableBroadcastEvent) event);
		} 
		else if (event instanceof Crash)
			handleCrash((Crash) event);
		else if (event instanceof ChannelInit)
			handleChannelInit((ChannelInit) event);
		else if (event instanceof ProcessInitEvent)
			handleProcessInitEvent((ProcessInitEvent) event);
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
		delivered = new HashSet<MessageID>();
	}

	/**
	 * @param event
	 */
	private void handleProcessInitEvent(ProcessInitEvent event) {
		processes = event.getProcessSet();
		try {
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}

		deliveredNumbers = new long[processes.getSize()];
		for (int i = 0; i < processes.getSize(); ++i) {
			deliveredNumbers[i] = -1;
		}
	}

	/**
	 * Called when the above protocol sends a message.
	 * 
	 * @param event
	 */
	private void rbBroadcast(ReliableBroadcastEvent event) {
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
	private void bebDeliver(ReliableBroadcastEvent event) {
		//Debug.print("RB: Received message from beb.");
		MessageID msgID = (MessageID) event.getMessage().peekObject();

		if (msgID.seqNumber > deliveredNumbers[msgID.process]
				&& !delivered.contains(msgID)) {
			Debug.print("RB: message is new.");
			delivered.add(msgID);
			// removes the header from the message (sender and seqNumber) and
			// delivers
			// it
			ReliableBroadcastEvent retransmission = null;
			try {
				retransmission = (ReliableBroadcastEvent) event.cloneEvent();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
				return;
			}
			event.getMessage().popObject();
			try {
				event.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
			// adds message to the "from" array

			/*
			 * resends the message if the source is no longer correct
			 */
			bebBroadcast(retransmission);

			if (delivered.size() < DELIVERED_SHRINK_SIZE) {
				shrinkDelivered(msgID);
				return;
			}
		}
	}

	/**
	 * Called by this protocol to send a message to the lower protocol.
	 * 
	 * @param event
	 */
	private void bebBroadcast(ReliableBroadcastEvent event) {
		//Debug.print("RB: sending message to beb.");
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
		Logger.getLogger(EagerRBSession.class.getName()).fine(
				"Process " + pi + " failed.");

		// changes the state of the process to "failed"
		processes.getProcess(pi).setCorrect(false);

		try {
			crash.go();
		} catch (AppiaEventException ex) {
			ex.printStackTrace();
		}
	}

	private void shrinkDelivered(MessageID mid) {
		int pid = mid.process;
		long sn = mid.seqNumber;
		long old_sn = deliveredNumbers[pid];

		MessageID tmid = new MessageID(pid, old_sn);
		long i;
		for (i = old_sn + 1; i <= sn; ++i) {
			tmid.seqNumber = i;
			if (!delivered.remove(tmid)) {
				break;
			}
		}
		deliveredNumbers[pid] = i - 1;
	}
}
