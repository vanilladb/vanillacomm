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

package org.vanilladb.comm.protocols.basicPaxos;

import net.sf.appia.core.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.vanilladb.comm.protocols.consensusUtils.PaxosProposal;
import org.vanilladb.comm.protocols.consensusUtils.TimestampValue;
import org.vanilladb.comm.protocols.events.Nack;
import org.vanilladb.comm.protocols.events.PaxosPropose;
import org.vanilladb.comm.protocols.events.PaxosReturn;
import org.vanilladb.comm.protocols.events.ProcessInitEvent;
import org.vanilladb.comm.protocols.events.Read;
import org.vanilladb.comm.protocols.events.ReadAck;
import org.vanilladb.comm.protocols.events.Write;
import org.vanilladb.comm.protocols.events.WriteAck;
import org.vanilladb.comm.protocols.utils.ProcessSet;

/**
 * implements basic Paxos consensus
 */
public class BasicPaxosConsensusSession extends Session {

	public BasicPaxosConsensusSession(Layer layer) {
		super(layer);
	}

	private ProcessSet correct = null;
	private Set<TimestampValue> readSet = null;
	private PaxosProposal tempValue = null;
	private PaxosProposal val = null;
	private long tstamp = 0;
	private long rts = 0;
	private long wts = 0;
	private int wAcks = 0;

	/**
	 * 
	 * 
	 * @see net.sf.appia.core.Session#handle(net.sf.appia.core.Event)
	 */
	public void handle(Event event) {
		if (event instanceof ProcessInitEvent)
			handleProcessInit((ProcessInitEvent) event);
		else if (event instanceof PaxosPropose)
			handlePaxosPropose((PaxosPropose) event);
		else if (event instanceof Read)
			handleRead((Read) event);
		else if (event instanceof Nack)
			handleNack((Nack) event);
		else if (event instanceof ReadAck)
			handleReadAck((ReadAck) event);
		else if (event instanceof Write)
			handleWrite((Write) event);
		else if (event instanceof WriteAck)
			handleWriteAck((WriteAck) event);
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
		tempValue = new PaxosProposal();
		val = new PaxosProposal();
		tempValue.abort = val.abort = true;
		rts = wts = 0;
		wAcks = 0;

		tstamp = correct.getSelfRank();
		readSet = new HashSet<TimestampValue>();
	}

	/**
	 * 
	 * @param event
	 */
	private void handleProcessInit(ProcessInitEvent event) {
		correct = event.getProcessSet();
		init();
		try {
			event.go();
		} catch (AppiaEventException ex) {
			ex.printStackTrace();
		}
	}

	/*********** begin Prepare phase ***********/

	/**
	 * 
	 * @param ap
	 */
	private void handlePaxosPropose(PaxosPropose pp) {
		tstamp = tstamp + correct.getSize();
		tempValue = pp.value;

		// bebBroadcast
		try {
			Read ev = new Read(pp.getChannel(), Direction.DOWN, this);
			ev.getMessage().pushLong(tstamp);
			ev.go();
		} catch (AppiaEventException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * 
	 * @param read
	 */
	private void handleRead(Read read) {
		long ts = read.getMessage().popLong();
		if (rts >= ts || wts >= ts) {

			// pp2p
			try {
				Nack ev = new Nack(read.getChannel(), Direction.DOWN, this);
				ev.source = correct.getSelfProcess().getSocketAddress();
				ev.dest = read.source;
				ev.setSourceSession(this);
				ev.init();
				ev.go();
			} catch (AppiaEventException ex) {
				ex.printStackTrace();
			}
		} else {
			rts = ts;

			// pp2p
			try {
				ReadAck ev = new ReadAck(read.getChannel(), Direction.DOWN,
						this);

				ev.getMessage().pushLong(wts);
				ev.getMessage().pushObject(val);

				ev.source = correct.getSelfProcess().getSocketAddress();
				ev.dest = read.source;
				ev.setSourceSession(this);
				ev.init();
				ev.go();
			} catch (AppiaEventException ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Newer proposal acknowledged, propose abort
	 * 
	 * @param nack
	 */
	private void handleNack(Nack nack) {
		try {
			PaxosReturn ev = new PaxosReturn(nack.getChannel(), Direction.UP,
					this);
			tempValue.abort = true;
			ev.decision = tempValue;
			ev.go();
		} catch (AppiaEventException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Add received promises to readSet
	 * 
	 * @param ra
	 */
	private void handleReadAck(ReadAck ra) {
		PaxosProposal v = (PaxosProposal) ra.getMessage().popObject();
		long ts = ra.getMessage().popLong();
		TimestampValue tsv = new TimestampValue(ts, v);

		readSet.add(tsv);
		readDecide(ra.getChannel());
	}

	/**
	 * Proceed to the second phase when acquired a majority of promises
	 * 
	 * @param channel
	 */
	private void readDecide(Channel channel) {
		if (readSet.size() > correct.getSize() / 2) {
			TimestampValue highest = Collections.max(this.readSet);
			if (highest.getPaxosProposal().abort != true) {
				// proceed to write phase
				tempValue = highest.getPaxosProposal();
				try {
					Write ev = new Write(channel, Direction.DOWN, this);
					ev.getMessage().pushLong(tstamp);
					ev.getMessage().pushObject(tempValue);
					ev.go();
				} catch (AppiaEventException ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	/*********** end Prepare phase ***********/

	/*********** begin Accept phase ***********/
	/**
	 * 
	 * @param write
	 */
	private void handleWrite(Write write) {
		PaxosProposal v = (PaxosProposal) write.getMessage().popObject();
		long ts = write.getMessage().popLong();

		if (rts >= ts || wts >= ts) {

			// pp2p
			try {
				Nack ev = new Nack(write.getChannel(), Direction.DOWN, this);
				ev.source = correct.getSelfProcess().getSocketAddress();
				ev.dest = write.source;
				ev.setSourceSession(this);
				ev.init();
				ev.go();
			} catch (AppiaEventException ex) {
				ex.printStackTrace();
			}
		} else {
			wts = ts;
			val = v;

			// pp2p
			try {
				WriteAck ev = new WriteAck(write.getChannel(), Direction.DOWN,
						this);

				ev.source = correct.getSelfProcess().getSocketAddress();
				ev.dest = write.source;
				ev.setSourceSession(this);
				ev.init();
				ev.go();
			} catch (AppiaEventException ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * @param wa
	 */
	private void handleWriteAck(WriteAck wa) {
		wAcks += 1;
		writeDecide(wa.getChannel());
	}

	/**
	 * 
	 * @param channel
	 */
	private void writeDecide(Channel channel) {
		if (wAcks > correct.getSize() / 2) {
			readSet.clear();
			wAcks = 0;

			try {
				PaxosReturn ev = new PaxosReturn(channel, Direction.UP, this);
				tempValue.abort = false;
				ev.decision = tempValue;
				ev.go();
			} catch (AppiaEventException ex) {
				ex.printStackTrace();
			}
		}
	}
	/*********** end Accept phase ***********/
}
