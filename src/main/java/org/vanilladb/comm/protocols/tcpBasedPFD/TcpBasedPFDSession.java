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

package org.vanilladb.comm.protocols.tcpBasedPFD;

import net.sf.appia.core.*;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.protocols.tcpcomplete.TcpUndeliveredEvent;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.vanilladb.comm.protocols.events.Crash;
import org.vanilladb.comm.protocols.events.ProcessInitEvent;
import org.vanilladb.comm.protocols.utils.ProcessSet;
import org.vanilladb.comm.protocols.utils.SampleProcess;

/**
 * Session implementing the TCP-Based Perfect Failure Detector. <br>
 * When TCP signals a failed destination, a crash notification of implied
 * process is sent.
 * 
 * @author nuno
 */
public class TcpBasedPFDSession extends Session {

	private Channel channel;

	private Map<Channel, ProcessSet> cpMap;

	private ProcessSet processes;

	private boolean started;

	/**
	 * Constructor of the Session.
	 * 
	 * @param layer
	 *            parent layer.
	 */
	public TcpBasedPFDSession(Layer layer) {
		super(layer);
		started = false;
		cpMap = new HashMap<Channel, ProcessSet>();
	}

	public void handle(Event event) {
		if (event instanceof TcpUndeliveredEvent)
			notifyCrash((TcpUndeliveredEvent) event);
		else if (event instanceof ChannelInit)
			handleChannelInit((ChannelInit) event);
		else if (event instanceof ProcessInitEvent)
			handleProcessInit((ProcessInitEvent) event);
		else if (event instanceof PFDStartEvent)
			handlePFDStart((PFDStartEvent) event);
	}

	/**
	 * @param init
	 */
	private void handleChannelInit(ChannelInit init) {
		channel = init.getChannel();

		try {
			init.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param event
	 */
	private void handleProcessInit(ProcessInitEvent event) {
		processes = event.getProcessSet();
		cpMap.put(event.getChannel(), processes);
		try {
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param event
	 */
	private void handlePFDStart(PFDStartEvent event) {
		started = true;
		try {
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}

		// get an array of processes
		SampleProcess[] processArray = this.processes.getAllProcesses();
		// for each process...
		for (int i = 0; i < processArray.length; i++) {
			try {
				CreateChannelsEvent createChannels = new CreateChannelsEvent(
						channel, Direction.DOWN, this);
				// set source and destination of event message
				createChannels.source = processes.getSelfProcess()
						.getSocketAddress();
				createChannels.dest = processArray[i].getSocketAddress();
				// sets the session that created the event.
				// this is important when this session is sending a cloned
				// event
				createChannels.setSourceSession(this);
				// if it is the "self" process, send the event upwards
				if (i == processes.getSelfRank())
					continue;
				// initializes and sends the message event
				createChannels.init();
				createChannels.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
				return;
			}

		}

		// try {
		// CreateChannelsEvent createChannels = new CreateChannelsEvent(
		// channel, Direction.DOWN, this);
		// createChannels.go();
		// } catch (AppiaEventException e1) {
		// e1.printStackTrace();
		// }

	}

	/**
	 * 
	 * When this protocol receives a TcpUndelivered Event from a process that
	 * was correct, it creates a Crash event and sends it to above protocols.
	 * 
	 * @param event
	 */
	private void notifyCrash(TcpUndeliveredEvent event) {
		if (started) {
			for (Channel c : cpMap.keySet()) {
				SampleProcess p = cpMap.get(c).getProcess(
						(SocketAddress) event.getFailedAddress());
				if (p != null && p.isCorrect()) {
					Logger.getLogger(TcpBasedPFDSession.class.getName())
							.severe("Process "
									+ p.getProcessNumber()
									+ " in channel "
									+ c.getChannelID()
									+ " failed. Source session: "
									+ (event.getSourceSession() == null ? "null"
											: event.getSourceSession()
													.toString()));
					p.setCorrect(false);

					try {
						Crash crash = new Crash(c, Direction.UP, this,
								p.getProcessNumber());
						crash.go();
					} catch (AppiaEventException e) {
						e.printStackTrace();
					}

				}
			}
		}
	}
}
