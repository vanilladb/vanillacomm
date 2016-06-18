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

package org.vanilladb.comm.protocols.zabTotalOrder;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.ChannelInit;

import org.vanilladb.comm.messages.TotalOrderMessage;
import org.vanilladb.comm.protocols.consensusUtils.PaxosObjectProposal;
import org.vanilladb.comm.protocols.consensusUtils.PaxosProposal;
import org.vanilladb.comm.protocols.events.Crash;
import org.vanilladb.comm.protocols.events.PaxosPropose;
import org.vanilladb.comm.protocols.events.PaxosReturn;
import org.vanilladb.comm.protocols.events.ProcessInitEvent;
import org.vanilladb.comm.protocols.events.ZabCacheTom;
import org.vanilladb.comm.protocols.events.ZabCommit;
import org.vanilladb.comm.protocols.events.ZabRequest;
import org.vanilladb.comm.protocols.events.ZabTomResult;
import org.vanilladb.comm.protocols.utils.ProcessSet;

/**
 * Session implementing the Basic Broadcast protocol.
 * 
 * @author nuno
 * 
 */
public class ZabTOBSession extends Session {

	private ProcessSet processes;

	private Queue<Object> msg_queue;

	private int leader;

	private boolean proposed;

	private int epoch;

	private int sn;

	private long toid = 0;
	private long tosn = 0;

	private Channel channel;

	private TotalOrderMessage cachedTom;
	private long lastTime = 0;

	/**
	 * Builds a new ZabTOBSession.
	 * 
	 * @param layer
	 */
	public ZabTOBSession(Layer layer) {
		super(layer);
	}

	/**
	 * Handles incoming events.
	 * 
	 * @see appia.Session#handle(appia.Event)
	 */
	public void handle(Event event) {
		// Init events. Channel Init is from Appia and ProcessInitEvent is to
		// know
		// the elements of the group

		if (event instanceof ZabRequest)
			handleZabRequest((ZabRequest) event);
		else if (event instanceof ZabTOBEvent)
			handleZabTOBEvent((ZabTOBEvent) event);
		else if (event instanceof PaxosReturn)
			handleConsensusDecide((PaxosReturn) event);
		else if (event instanceof ChannelInit)
			handleChannelInit((ChannelInit) event);
		else if (event instanceof ProcessInitEvent)
			handleProcessInitEvent((ProcessInitEvent) event);
		else if (event instanceof ZabCommit)
			handleZabCommit((ZabCommit) event);
		else if (event instanceof ZabCacheTom)
			handleZabCacheTom((ZabCacheTom)event);
		else if (event instanceof Crash)
			handleCrash((Crash) event);
	}

	/**
	 * Gets the process set and forwards the event to other layers.
	 * 
	 * @param event
	 */
	private void handleProcessInitEvent(ProcessInitEvent event) {
		processes = event.getProcessSet();
		msg_queue = new ArrayDeque<Object>();
		// XXX
		// standalone leader
		leader = event.getProcessSet().getSize() - 1;

		proposed = false;

		epoch = 0;
		sn = 0;

		try {
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Handles the first event that arrives to the protocol session. In this
	 * case, just forwards it.
	 * 
	 * @param init
	 */
	private void handleChannelInit(ChannelInit init) {
		this.channel = init.getChannel();
		try {
			init.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}

	private void handleZabRequest(ZabRequest event) {
		// check if self is the leader
		if (leader == processes.getSelfRank()) {
			// do zab propose
			msg_queue.add(event.getObject());
			zabPropose();
		} else {
			// send the zab request to leader
			try {
				ZabTOBEvent ev = new ZabTOBEvent(this.channel, Direction.DOWN,
						this);
				ev.getMessage().pushObject(event.getObject());
				ev.source = processes.getSelfProcess().getSocketAddress();
				ev.dest = processes.getProcess(leader).getSocketAddress();
				ev.init();
				ev.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Broadcasts a message.
	 * 
	 * @param event
	 */
	private void handleZabTOBEvent(ZabTOBEvent event) {
		msg_queue.add(event.getMessage().popObject());
		zabPropose();
	}

	/**
	 * Consensus result obtained. Deliver the next total ordered message.
	 * 
	 * @param event
	 */
	private void handleConsensusDecide(PaxosReturn pr) {
		proposed = false;
		PaxosProposal pp = pr.decision;
		PaxosObjectProposal pop = (PaxosObjectProposal) pp;
		if (!pp.abort) {
			if (Logger.getLogger(ZabTOBSession.class.getName()).isLoggable(
					Level.FINE)) {
				Logger.getLogger(ZabTOBSession.class.getName()).fine(
						"consensus decided " + tosn);
				Logger.getLogger(ZabTOBSession.class.getName()).fine(
						"TOB Queue size = " + this.msg_queue.size());
			}
			try {
				ZabCommit zc = new ZabCommit(this.channel, Direction.DOWN, this);
				zc.init();
				zc.go();

			} catch (AppiaEventException ex) {
				ex.printStackTrace();
			}

		} else {
			msg_queue.add(pop.obj);
		}

		zabPropose();
	}


	/**
	 * <p>
	 * Leader proposes a new message to be the next total ordered message.
	 * </p>
	 * If there is no propose being processed, and there are messages in the
	 * queue, do propose in a batch (all queued message are combined into a
	 * queue
	 */
	private void zabPropose() {
		if (!proposed && !msg_queue.isEmpty()) {
			proposed = true;
			List<Object> list = new LinkedList<Object>();
			while (!msg_queue.isEmpty()) {
				for (Object o : ((TotalOrderMessage) msg_queue.poll())
						.getMessages()) {
					list.add(o);
				}
			}

			TotalOrderMessage tom = new TotalOrderMessage(list.toArray(new Object[0]));
			tom.setTotalOrderIdStart(toid);
			tom.setTotalOrderSequenceNumber(tosn++);
			toid += tom.getMessages().length;
			PaxosObjectProposal proposal = new PaxosObjectProposal(tom);
			try {
				PaxosPropose event = new PaxosPropose(this.channel,
						Direction.DOWN, this);
				event.value = proposal;
				event.epoch = this.epoch;
				event.sn = ++this.sn;
				event.init();
				event.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Every node will receive this event to commit this ZAB round and send the TOM to the ZabAppl
	 * @param zc
	 */
	private void handleZabCommit(ZabCommit zc){
		// take the cached TOM
		
		// deliver to zabAppl layer
		try {
			ZabTomResult ztr = new ZabTomResult(this.channel, Direction.UP, this, cachedTom);
			ztr.init();
			ztr.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Cache TOM from delivered by ZabAcceptLayer
	 * @param zct
	 */
	private void handleZabCacheTom(ZabCacheTom zct){
		cachedTom = zct.getTom();
	}

	/**
	 * Called when some process crashed.
	 * 
	 * @param crash
	 */
	private void handleCrash(Crash crash) {
		int crashedProcess = crash.getCrashedProcess();
		Logger.getLogger(ZabTOBSession.class.getName()).fine(
				"Process " + crashedProcess + " failed.");
		// changes the state of the process to "failed"
		synchronized (this) {
			processes.getProcess(crashedProcess).setCorrect(false);
			if (crashedProcess == leader) {
				for (int i = 0; i < processes.getSize(); ++i) {
					if (processes.getProcess(i).isCorrect()) {
						leader = i;
					}
				}
				++epoch;
				sn = 0;
			}
			if (processes.getSelfRank() == leader) {
				proposed = false;
				msg_queue.clear();
			}
		}
		try {
			crash.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
}
