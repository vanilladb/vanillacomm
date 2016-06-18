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

package org.vanilladb.comm.protocols.consensusPaxos;

import net.sf.appia.core.*;

import org.vanilladb.comm.protocols.consensusUtils.PaxosProposal;
import org.vanilladb.comm.protocols.events.Decided;
import org.vanilladb.comm.protocols.events.PaxosPropose;
import org.vanilladb.comm.protocols.events.PaxosReturn;
import org.vanilladb.comm.protocols.events.ProcessInitEvent;
import org.vanilladb.comm.protocols.events.UcDecide;
import org.vanilladb.comm.protocols.events.UcPropose;
import org.vanilladb.comm.protocols.utils.ProcessSet;

/**
 * implements basic Paxos consensus
 */
public class ConsensusPaxosSession extends Session {

	public ConsensusPaxosSession(Layer layer) {
		super(layer);
	}

	private Channel channel;
	private ProcessSet correct = null;
	private PaxosProposal proposal = null;
	boolean proposed, decided;

	/**
	 * 
	 * 
	 * @see net.sf.appia.core.Session#handle(net.sf.appia.core.Event)
	 */
	public void handle(Event event) {
		if (event instanceof ProcessInitEvent)
			handleProcessInit((ProcessInitEvent) event);
		else if (event instanceof UcPropose)
			handleUcPropose((UcPropose) event);
		else if (event instanceof PaxosReturn)
			handleAcReturn((PaxosReturn) event);
		else if (event instanceof Decided)
			handleDecided((Decided) event);
		else {
			try {
				event.go();
			} catch (AppiaEventException ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
     * 
     */
	private void init() {
		proposed = false;
		decided = false;
	}

	/**
	 * 
	 * @param event
	 */
	private void handleProcessInit(ProcessInitEvent event) {
		correct = event.getProcessSet();
		channel = event.getChannel();
		init();
		try {
			event.go();
		} catch (AppiaEventException ex) {
			ex.printStackTrace();
		}
	}

	private void handleUcPropose(UcPropose p) {
		proposal = p.value;
		tryPropose();
	}

	private void tryPropose() {
		if (!proposed) {
			proposed = true;
			try {
				PaxosPropose event = new PaxosPropose(this.channel,
						Direction.DOWN, this);
				event.value = proposal;
				event.setSourceSession(this);
				event.init();
				event.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
		}
	}

	private void handleAcReturn(PaxosReturn result) {
		if (!result.decision.abort) {
			try {
				Decided event = new Decided();
				event.value = result.decision;
				event.setDir(Direction.DOWN);
				event.setSourceSession(this);
				event.init();
				event.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
		} else {
			proposed = false;
			tryPropose();
		}
	}

	private void handleDecided(Decided v) {
		decided = true;
		try {
			UcDecide event = new UcDecide(this.channel, Direction.UP, this);
			event.decision = v.value;
			event.setSourceSession(this);
			event.init();
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
}
